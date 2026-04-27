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
        val waitingUntil: Long = 0L // 新增
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
    val waitingUntil: Long = 0L // 新增：保存等待状态
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
        filterByLevel: (T) -> Boolean
    ): SessionLoadResult<T> {

        // 1. 尝试恢复会话
        if (savedSession != null && savedSession.level == level) {
            val (ids, index, _, steps) = savedSession
            val allItems = getItemsByIds(ids)

            // 按照 ID 列表的顺序重建 Session 列表 (保持原有的穿插顺序)
            val itemMap = allItems.associateBy { getItemId(it) }
            val restoredItems = ids.mapNotNull { itemMap[it] }

            // 确保恢复后的列表不为空，且索引有效
            if (restoredItems.isNotEmpty() && index < restoredItems.size) {
                println("✅ 恢复上次学习会话: Index $index / ${restoredItems.size}")
                return SessionLoadResult.Restored(
                    items = restoredItems,
                    index = index,
                    steps = steps,
                    dailyGoal = dailyGoal,
                    completedToday = completedToday,
                    waitingUntil = savedSession.waitingUntil // 传递恢复的等待状态
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
