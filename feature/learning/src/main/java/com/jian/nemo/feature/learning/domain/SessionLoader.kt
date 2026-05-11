package com.jian.nemo.feature.learning.domain

import javax.inject.Inject

/**
 * 会话加载结果
 * 封装 Session 加载的三种可能结果
 */
sealed class SessionLoadResult<T> {
    /**
     * 恢复了之前未完成的会话
     */
    data class Restored<T>(
        val items: List<T>,
        val index: Int,
        val steps: Map<Int, Int>,
        val dailyGoal: Int,
        val completedToday: Int,
        val waitingUntil: Long = 0L,
        val isAnswerShown: Boolean = false // 新增
    ) : SessionLoadResult<T>()

    /**
     * 创建了新会话
     */
    data class NewSession<T>(
        val items: List<T>,
        val dueCount: Int,
        val newCount: Int,
        val dailyGoal: Int,
        val completedToday: Int
    ) : SessionLoadResult<T>()

    /**
     * 会话已完成（无更多项目可学习）
     */
    data class Completed<T>(
        val dailyGoal: Int,
        val completedToday: Int
    ) : SessionLoadResult<T>()
}

/**
 * 已保存的会话数据
 */
data class SavedSession(
    val ids: List<Int>,
    val index: Int,
    val level: String,
    val steps: Map<Int, Int>,
    val waitingUntil: Long = 0L,
    val isAnswerShown: Boolean = false // 新增
)

/**
 * 会话加载器
 *
 * 负责统一处理 Word 和 Grammar 会话的加载逻辑：
 * 1. 尝试恢复已保存的会话
 * 2. 获取到期复习项
 * 3. 计算新项配额
 * 4. 智能混合排序
 *
 * 遵循原则:
 * 1. 纯逻辑，不包含 Android 依赖
 * 2. 泛型支持，统一用于 Word 和 Grammar
 */
class SessionLoader @Inject constructor(
    private val learningSessionPolicy: LearningSessionPolicy
) {
    /**
     * 加载学习会话
     *
     * @param T 项目类型 (Word 或 Grammar)
     * @param level 当前学习等级
     * @param dailyGoal 每日目标
     * @param completedToday 今日已完成数量
     * @param savedSession 已保存的会话（如果有）
     * @param getItemsByIds 根据 ID 列表获取项目
     * @param getDueItems 获取到期复习项
     * @param getNewItems 获取新项目
     * @param getItemId 获取项目 ID
     * @param filterByLevel 按等级过滤
     */
    suspend fun <T> loadSession(
        level: String,
        dailyGoal: Int,
        completedToday: Int,
        savedSession: SavedSession?,
        getItemsByIds: suspend (List<Int>) -> List<T>,
        getDueItems: suspend () -> List<T>,
        getNewItems: suspend () -> List<T>,
        getItemId: (T) -> Int,
        filterByLevel: (T) -> Boolean,
        isItemNew: (T) -> Boolean // 新增：判断是否为新词
    ): SessionLoadResult<T> {

        // 1. 尝试恢复会话
        if (savedSession != null && savedSession.level == level) {
            val (ids, index, _, steps) = savedSession
            val allItems = getItemsByIds(ids)

            // 按照 ID 列表的顺序重建 Session 列表 (保持原有的穿插顺序)
            val itemMap = allItems.associateBy { getItemId(it) }
            val restoredItems = ids.mapNotNull { itemMap[it] }

            // [逻辑调整] 如果是恢复会话，我们需要根据新的 dailyGoal 重新检查剩余新词配额
            // 计算当前还允许的新词数量
            val remainingNewQuota = (dailyGoal - completedToday).coerceAtLeast(0)

            // 对 currentIndex 之后的项目进行裁剪
            // 【重要原则】：
            // 1. “剩余数量”由 prunedItems 的长度决定。
            // 2. 在 Pager 模式下，currentIndex 之前的词虽已滑过，但只要未评分就仍留在队列中。
            // 3. 因此在恢复会话时，i < index 的新词也必须扣除配额，以保持队列总数稳定。
            val prunedItems = mutableListOf<T>()
            var newItemsRemaining = remainingNewQuota

            restoredItems.forEachIndexed { i, item ->
                val isNew = isItemNew(item)
                if (i < index) {
                    // 索引之前的词，直接保留（支持 Pager 回划）
                    prunedItems.add(item)
                    // [关键修正]：索引之前的词如果是新词，也必须占用配额，否则会导致重复补货
                    if (isNew) {
                        newItemsRemaining--
                    }
                } else if (i == index) {
                    // 当前正在学的这一张，必须保留以防止 UI 崩溃
                    prunedItems.add(item)
                    // 如果当前这张是新词，它还没计入已完成，必须扣掉 1 个配额
                    if (isNew) {
                        newItemsRemaining--
                    }
                } else {
                    // 索引之后的词（还没见过的词）
                    if (isNew) {
                        // 如果是新词，检查配额
                        if (newItemsRemaining > 0) {
                            prunedItems.add(item)
                            newItemsRemaining--
                        } else {
                            // 配额用尽，该新词被移除
                            println("✂️ 热重载裁剪: 移除超出配额的新词 ID=${getItemId(item)}")
                        }
                    } else {
                        // 如果是复习词/步进词，无论如何都要保留
                        prunedItems.add(item)
                    }
                }
            }

            // 确保恢复后的列表不为空，且索引有效
            if (prunedItems.isNotEmpty() && index < prunedItems.size) {
                
                // [新增逻辑] 如果配额还有剩余（newItemsRemaining > 0），说明目标调大了，需要补货
                var finalItems = prunedItems.toList()
                if (newItemsRemaining > 0) {
                    println("📦 热重载补货: 发现配额缺口 $newItemsRemaining，正在从词库抓取新词...")
                    val supplementalNewItems = getNewItems()
                    val existingIds = finalItems.map { getItemId(it) }.toSet()
                    
                    // 过滤掉已经在队列里的，取前 N 个
                    val newSupplement = supplementalNewItems
                        .filter { getItemId(it) !in existingIds }
                        .take(newItemsRemaining)
                    
                    if (newSupplement.isNotEmpty()) {
                        finalItems = finalItems + newSupplement
                        println("✅ 补货成功: 增加了 ${newSupplement.size} 个新词")
                    }
                }

                println("✅ 恢复并同步学习会话: Index $index / ${finalItems.size} (目标: $dailyGoal)")
                return SessionLoadResult.Restored(
                    items = finalItems,
                    index = index,
                    steps = steps,
                    dailyGoal = dailyGoal,
                    completedToday = completedToday,
                    waitingUntil = savedSession.waitingUntil,
                    isAnswerShown = savedSession.isAnswerShown
                )
            } else if (prunedItems.isNotEmpty() && index >= prunedItems.size) {
                // 边界情况：如果当前索引因为裁剪而变为了最后一个之后，说明任务已完成
                println("🏁 目标缩减导致会话提前完成")
                return SessionLoadResult.Completed(
                    dailyGoal = dailyGoal,
                    completedToday = completedToday
                )
            }
        }

        // 2. 获取到期复习项
        val allDueItems = getDueItems()
        val dueItems = allDueItems // 不再按等级过滤复习项，全量复习
        val dueCount = dueItems.size

        // 3. 计算新项配额（当前策略：不减载）
        val rawRemainingQuota = (dailyGoal - completedToday).coerceAtLeast(0)
        val adjustedQuota = learningSessionPolicy.calculateAdjustedNewQuota(rawRemainingQuota, dueCount)

        // [Debug Log]
        println("📊 会话规划: 目标=$dailyGoal, 已学=$completedToday, 复习堆积=$dueCount")
        println("   -> 新词配额=$adjustedQuota")

        // 4. 如果调整后配额为0且无复习项，则会话完成
        if (adjustedQuota == 0 && dueItems.isEmpty()) {
            return SessionLoadResult.Completed(
                dailyGoal = dailyGoal,
                completedToday = completedToday
            )
        }

        // 5. 获取新项目
        val newItems = getNewItems()

        // 6. 组装会话列表: 智能混合 (Smart Interleaving)
        val sessionNewItems = newItems.take(adjustedQuota)
        val sessionItems = learningSessionPolicy.mixSessionItems(dueItems, sessionNewItems)

        if (sessionItems.isEmpty()) {
            return SessionLoadResult.Completed(
                dailyGoal = dailyGoal,
                completedToday = completedToday
            )
        }

        println("✅ 学习会话启动成功: ${sessionItems.size} 个项目 (复习: ${dueItems.size}, 新: ${sessionNewItems.size})")
        println("   -> 混合策略: 全量配额 $adjustedQuota + 穿插排序")

        return SessionLoadResult.NewSession(
            items = sessionItems,
            dueCount = dueItems.size,
            newCount = sessionNewItems.size,
            dailyGoal = dailyGoal,
            completedToday = completedToday
        )
    }
}
