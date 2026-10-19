package com.jian.nemo.feature.learning.domain

import com.jian.nemo.core.domain.model.SrsItem
import java.time.LocalDate
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
 * 负责统一处理 Word 和 Grammar 会话的加载与微会话（Micro-session）规划逻辑：
 * 1. 尝试恢复已保存的会话
 * 2. 获取到期复习项并执行 FSRS 四级客观切片
 * 3. 隔离 P3 沉睡池，避免断更心理压迫
 * 4. 依据单组容量（Session Batch Size）构建 3~5 分钟保底微会话
 * 5. 智能穿插混合 (Smart Interleaving)
 *
 * 遵循原则:
 * 1. 纯逻辑，不包含 Android 依赖
 * 2. 泛型支持，统一用于 Word 和 Grammar (<T : SrsItem>)
 * 3. 零破坏架构：纯内存计算与切片
 */
class SessionLoader @Inject constructor(
    private val learningSessionPolicy: LearningSessionPolicy
) {
    /**
     * 加载学习会话
     *
     * @param T 项目类型 (Word 或 Grammar, 必须实现 SrsItem)
     * @param level 当前学习等级
     * @param dailyGoal 每日目标
     * @param completedToday 今日已完成数量
     * @param savedSession 已保存的会话（如果有）
     * @param getItemsByIds 根据 ID 列表获取项目
     * @param getDueItems 获取到期复习项
     * @param getNewItems 获取新项目
     * @param getLearningItems 获取跨天学习中项
     * @param getItemId 获取项目 ID
     * @param filterByLevel 按等级过滤
     * @param isItemNew 判断是否为新项目
     * @param sessionBatchSize 单组微会话容量 (默认 20，范围 10..30)
     * @param backlogStrategy 积压期新词策略 ("taste" 微量尝鲜 vs "review_only" 纯复习)
     * @param maxDailyReviews 每日复习上限 (默认 0 表示不限制)
     * @param todayReviewedCount 今日已完成复习数量
     * @param todayEpochDay 当前 Epoch Day (默认基于系统日期)
     */
    suspend fun <T : SrsItem> loadSession(
        level: String,
        dailyGoal: Int,
        completedToday: Int,
        savedSession: SavedSession?,
        getItemsByIds: suspend (List<Int>) -> List<T>,
        getDueItems: suspend () -> List<T>,
        getNewItems: suspend () -> List<T>,
        getLearningItems: (suspend () -> List<T>)? = null,
        getItemId: (T) -> Int,
        filterByLevel: (T) -> Boolean,
        isItemNew: (T) -> Boolean,
        sessionBatchSize: Int = 20,
        backlogStrategy: String = "taste",
        maxDailyReviews: Int = 0,
        todayReviewedCount: Int = 0,
        todayEpochDay: Long = LocalDate.now().toEpochDay()
    ): SessionLoadResult<T> {

        // 1. 尝试恢复会话
        if (savedSession != null && savedSession.level == level) {
            val (ids, index, _, steps) = savedSession
            val allItems = getItemsByIds(ids)

            // 按照 ID 列表的顺序重建 Session 列表 (保持原有的穿插顺序)
            val itemMap = allItems.associateBy { getItemId(it) }
            val restoredItems = ids.mapNotNull { itemMap[it] }

            // 恢复已保存的会话队列：全量保留已保存的卡片，不受中途目标修改干扰
            val prunedItems = restoredItems.toMutableList()

            // 确保恢复后的列表不为空，且索引有效
            if (prunedItems.isNotEmpty() && index < prunedItems.size) {
                // 计算今日剩余复习额度与新学额度
                val remainingReviewQuota = if (maxDailyReviews > 0) {
                    (maxDailyReviews - todayReviewedCount).coerceAtLeast(0)
                } else {
                    Int.MAX_VALUE
                }
                val rawRemainingQuota = (dailyGoal - completedToday).coerceAtLeast(0)
                val effectiveBatchSize = sessionBatchSize.coerceIn(10, 30)

                val remainingItemsInSession = prunedItems.subList(index, prunedItems.size)

                // 依据复习上限配额与微会话单组容量（Session Batch Size）对残留卡片进行智能切片
                var reviewQuotaLeft = remainingReviewQuota
                var newQuotaLeft = rawRemainingQuota
                val curatedRemainingItems = mutableListOf<T>()

                for (item in remainingItemsInSession) {
                    if (curatedRemainingItems.size >= effectiveBatchSize) {
                        break
                    }
                    val isNew = isItemNew(item)
                    if (isNew) {
                        if (newQuotaLeft > 0) {
                            curatedRemainingItems.add(item)
                            newQuotaLeft--
                        }
                    } else {
                        if (reviewQuotaLeft > 0) {
                            curatedRemainingItems.add(item)
                            reviewQuotaLeft--
                        }
                    }
                }

                // 检查：如果清洗截断后没有可学习项目（例如今日复习已达上限且今日新词已达成/无新词配额），
                // 说明超额复习项已达上限，应当尊重配额直接呈现结算
                if (curatedRemainingItems.isEmpty()) {
                    println("🛑 [Quota & Micro-Session] 今日复习已达上限且新词已达成（或无剩余配额），废止残留超额微会话，直接结算")
                    return SessionLoadResult.Completed(
                        dailyGoal = dailyGoal,
                        completedToday = completedToday
                    )
                }

                val learnedItems = prunedItems.subList(0, index)
                val finalItems = learnedItems + curatedRemainingItems

                println("✅ 恢复微会话: Index $index / ${finalItems.size} (本组待学 ${curatedRemainingItems.size} 词, 历史总计 ${prunedItems.size} 词, 单组容量 $effectiveBatchSize)")
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
                println("🏁 会话已完成")
                return SessionLoadResult.Completed(
                    dailyGoal = dailyGoal,
                    completedToday = completedToday
                )
            }
        }

        // 2. 获取到期复习项与跨天学习中项
        val learningItems = getLearningItems?.invoke() ?: emptyList()
        val allDueItems = getDueItems()

        // 3. 执行 FSRS 四级互斥切片与休眠隔离 (方案 2.4 核心)
        val partition = learningSessionPolicy.partitionOverdueItems(allDueItems, todayEpochDay)
        val p3StaleCount = partition.p3Stale.size
        if (p3StaleCount > 0) {
            println("😴 [Backlog] 纯内存隔离休眠条目: $p3StaleCount 词进入沉睡池，不参与日常高压排队")
        }

        // 组织活跃复习条目：抢救级 P0 优先救 -> 当期级 P2 FIFO 自然排队 -> 深度逾期级 P1 稳步推进
        val prioritizedDueItems = partition.p0UrgentRescue + partition.p2RegularCurrent + partition.p1DeepOverdue
        val activeDueItems = learningItems + prioritizedDueItems
        val activeDueCount = activeDueItems.size

        // 4. 微会话容量（Session Batch Size）与每日复习上限配额调度
        val effectiveBatchSize = sessionBatchSize.coerceIn(10, 30)
        val rawRemainingQuota = (dailyGoal - completedToday).coerceAtLeast(0)

        // 计算今日剩余复习额度上限约束 (0 表示不设限制)
        val remainingReviewQuota = if (maxDailyReviews > 0) {
            (maxDailyReviews - todayReviewedCount).coerceAtLeast(0)
        } else {
            Int.MAX_VALUE
        }

        // 判定是否处于积压还债期 (活跃复习项已超过或达到单组容量，且仍有复习额度)
        val isBacklogState = activeDueCount >= effectiveBatchSize && remainingReviewQuota > 0

        // 根据积压状态与新词策略计算本组新词配额
        val newItemsQuota = when {
            remainingReviewQuota <= 0 -> {
                // 复习上限已满：如果新词目标未完成，剩余微会话容量全部给新词；否则 0
                minOf(effectiveBatchSize, rawRemainingQuota)
            }
            !isBacklogState -> {
                // 无积压状态：剩余容量提供给新词，且不超过用户今日未完成目标
                val remainingCapacity = (effectiveBatchSize - activeDueCount).coerceAtLeast(0)
                minOf(remainingCapacity, rawRemainingQuota)
            }
            backlogStrategy == "review_only" -> {
                // 积压期【纯复习优先】策略：0 新词，整组全部投入清积压
                0
            }
            else -> {
                // 积压期【微量尝鲜】策略 (默认)：允许 2~3 个新词调剂口味，不超过剩余目标
                minOf(3, rawRemainingQuota)
            }
        }

        // 截取本组微会话复习条目，严格受单组容量与每日复习上限约束
        val curatedDueItems = if (remainingReviewQuota <= 0) {
            emptyList()
        } else if (isBacklogState) {
            val dueCapacity = minOf((effectiveBatchSize - newItemsQuota).coerceAtLeast(0), remainingReviewQuota)
            activeDueItems.take(dueCapacity)
        } else {
            activeDueItems.take(remainingReviewQuota)
        }

        // 5. 如果没有可复习条目且无新词配额，则今日常规学习会话完成，直接呈现结算
        if (curatedDueItems.isEmpty() && newItemsQuota == 0) {
            println("🎉 今日任务全部达成或达到复习上限 (今日新词完成: $completedToday/$dailyGoal, 今日复习: $todayReviewedCount, 上限: $maxDailyReviews)")
            return SessionLoadResult.Completed(
                dailyGoal = dailyGoal,
                completedToday = completedToday
            )
        }

        // 6. 获取新项目并构建混合微会话
        val newItems = if (newItemsQuota > 0) getNewItems() else emptyList()
        val sessionNewItems = newItems.take(newItemsQuota)
        val sessionItems = learningSessionPolicy.mixSessionItems(curatedDueItems, sessionNewItems)

        if (sessionItems.isEmpty()) {
            return SessionLoadResult.Completed(
                dailyGoal = dailyGoal,
                completedToday = completedToday
            )
        }

        println("✅ 学习微会话启动成功: 本组共 ${sessionItems.size} 个项目 (本组复习: ${curatedDueItems.size}, 本组新词: ${sessionNewItems.size}, 待复习池总数: $activeDueCount, 今日复习上限: $maxDailyReviews, 今日已复习: $todayReviewedCount)")
        println("   -> 微会话策略: 单组容量=$effectiveBatchSize, 积压状态=$isBacklogState, 新词策略=$backlogStrategy")

        return SessionLoadResult.NewSession(
            items = sessionItems,
            dueCount = activeDueCount,
            newCount = sessionNewItems.size,
            dailyGoal = dailyGoal,
            completedToday = completedToday
        )
    }
}
