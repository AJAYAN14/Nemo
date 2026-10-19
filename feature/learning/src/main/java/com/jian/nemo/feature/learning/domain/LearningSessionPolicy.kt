package com.jian.nemo.feature.learning.domain

import com.jian.nemo.core.domain.algorithm.FsrsAlgorithm
import com.jian.nemo.core.domain.model.SrsItem
import javax.inject.Inject

/**
 * 四级互斥切片分流结果容器
 * 100% 穷尽覆盖所有待复习项目，各分类之间严格互斥
 */
data class OverduePartition<T : SrsItem>(
    /** P0 临界抢救级: 0.6 <= R < 0.9, 处于遗忘边缘但尚未完全遗忘，按回忆率 R 升序 (越危险越优先) */
    val p0UrgentRescue: List<T>,
    /** P1 深度逾期级: R < 0.6, 遗忘严重，按相对逾期率 RO 降序 */
    val p1DeepOverdue: List<T>,
    /** P2 常规当期级: R >= 0.9, 今天刚到期或长周期平稳词，按 nextReviewDate 升序 (FIFO) */
    val p2RegularCurrent: List<T>,
    /** P3 休眠隔离池 (Stale): 纯内存识别长期断更沉睡项，不参与日常高压排队 */
    val p3Stale: List<T>
) {
    /** 活跃待复习项（排除休眠池） */
    val activeItems: List<T> get() = p0UrgentRescue + p1DeepOverdue + p2RegularCurrent

    /** 待复习总数 */
    val totalCount: Int get() = p0UrgentRescue.size + p1DeepOverdue.size + p2RegularCurrent.size + p3Stale.size
}

/**
 * 学习会话策略
 * 负责计算配额、穿插混合、FSRS 客观切片等纯逻辑算法
 *
 * 遵循原则:
 * 1. 纯逻辑，不包含 Android 依赖
 * 2. 泛型支持，统用于 Word 和 Grammar (<T : SrsItem>)
 * 3. 零破坏架构：纯内存计算，不改动数据库表结构
 */
class LearningSessionPolicy(
    private val fsrsAlgorithm: FsrsAlgorithm
) {
    @Inject
    constructor() : this(FsrsAlgorithm())

    /**
     * 计算客观回忆率 R(t, S)
     * 基于 FSRS 6 官方幂律遗忘曲线: R = (1 + factor * (t / S)) ^ (-decay)
     *
     * @param item SRS 项目 (泛型 T : SrsItem)
     * @param today 当前日期 (Epoch Day)
     * @return 客观回忆率 (范围 0.0f .. 1.0f)
     */
    fun <T : SrsItem> calculateRetrievability(item: T, today: Long): Float {
        val lastDate = item.lastReviewedDate ?: item.firstLearnedDate ?: return 0f
        val elapsedDays = (today - lastDate).coerceAtLeast(0).toFloat()
        val stability = item.stability

        if (stability <= 0f) return 0f
        if (elapsedDays <= 0f) return 1f

        return fsrsAlgorithm.forgettingCurve(elapsedDays, stability).coerceIn(0f, 1f)
    }

    /**
     * 计算相对逾期率 RO (Relative Overdue)
     * RO = (实际经过天数 t - 原计划间隔 I) / 原计划间隔 I = 逾期天数 / I
     *
     * @param item SRS 项目
     * @param today 当前日期 (Epoch Day)
     * @return 相对逾期比率 (例如 2.0 表示逾期程度为原间隔的 200%)
     */
    fun <T : SrsItem> calculateRelativeOverdue(item: T, today: Long): Float {
        val interval = item.interval.coerceAtLeast(1).toFloat()
        val overdueDays = (today - item.nextReviewDate).coerceAtLeast(0).toFloat()
        return overdueDays / interval
    }

    /**
     * 判定卡片是否属于长期断更沉睡项 (Stale)
     * 准入条件：逾期天数 >= 30 天 且 实际经过天数 t >= 3 * 原计划间隔 I
     *
     * @param item SRS 项目
     * @param today 当前日期 (Epoch Day)
     */
    fun <T : SrsItem> isItemStale(item: T, today: Long): Boolean {
        val lastDate = item.lastReviewedDate ?: item.firstLearnedDate ?: return false
        val overdueDays = today - item.nextReviewDate
        val elapsedDays = today - lastDate
        val interval = item.interval.coerceAtLeast(1)

        return overdueDays >= 30 && elapsedDays >= (3 * interval)
    }

    /**
     * 四级全域互斥穷尽切片分流
     * 彻底消灭逻辑真空，各等级严格互斥，总和等于原始列表数量
     *
     * @param items 待复习项目列表 (NextReviewDate <= today)
     * @param today 当前日期 (Epoch Day)
     */
    fun <T : SrsItem> partitionOverdueItems(items: List<T>, today: Long): OverduePartition<T> {
        val p3List = mutableListOf<T>()
        val p0List = mutableListOf<Pair<T, Float>>() // Pair(item, R)
        val p1List = mutableListOf<Pair<T, Float>>() // Pair(item, RO)
        val p2List = mutableListOf<T>()

        for (item in items) {
            if (isItemStale(item, today)) {
                p3List.add(item)
            } else {
                val r = calculateRetrievability(item, today)
                when {
                    r in 0.6f..<0.9f -> {
                        p0List.add(item to r)
                    }
                    r < 0.6f -> {
                        val ro = calculateRelativeOverdue(item, today)
                        p1List.add(item to ro)
                    }
                    else -> { // r >= 0.9f
                        p2List.add(item)
                    }
                }
            }
        }

        // P0: 按回忆率 R 升序 (越接近 0.6 遗忘边缘越优先拯救)
        val sortedP0 = p0List.sortedBy { it.second }.map { it.first }

        // P1: 按相对逾期率 RO 降序 (偏离原计划倍数越大越优先)
        val sortedP1 = p1List.sortedByDescending { it.second }.map { it.first }

        // P2: 按 nextReviewDate 升序 (FIFO 队列，自然排队)
        val sortedP2 = p2List.sortedBy { it.nextReviewDate }

        return OverduePartition(
            p0UrgentRescue = sortedP0,
            p1DeepOverdue = sortedP1,
            p2RegularCurrent = sortedP2,
            p3Stale = p3List
        )
    }

    /**
     * 计算调整后的新词配额
     *
     * 当前策略：不对新词做减载，始终返回目标配额。
     * 即“有多少学多少”，复习数量不会压缩新词配额。
     *
     * @param targetQuota 原始目标配额
     * @param dueCount 到期复习数量（当前策略未使用）
     */
    fun calculateAdjustedNewQuota(targetQuota: Int, dueCount: Int): Int {
        return targetQuota
    }

    /**
     * 智能穿插排序 (Sandwich Mix)
     * 结构：[高危复习] -> [新词均匀分散在普通复习中]
     *
     * 算法：动态比例分散
     * - 根据复习/新词的实际数量比例计算插入间隔
     * - 新词均匀分布在整个 session 中，避免堆积
     * - 例如：10复习+4新词 → 每隔约2-3个复习插入1个新词
     *
     * @param dueItems 复习项列表 (假设已按优先级/DueDate排序)
     * @param newItems 新项列表
     */
    fun <T> mixSessionItems(dueItems: List<T>, newItems: List<T>): List<T> {
        if (dueItems.isEmpty()) return newItems
        if (newItems.isEmpty()) return dueItems

        // 1. 提取高危复习项 (Top 20%, 最少3个, 最多全部)
        //    这些项会被放在最前面优先复习
        val urgentCount = (dueItems.size * 0.2).toInt().coerceAtLeast(3).coerceAtMost(dueItems.size)
        val urgentReviews = dueItems.take(urgentCount)
        val normalReviews = dueItems.drop(urgentCount)

        // 2. 如果没有普通复习项，直接返回 紧急复习 + 新词
        if (normalReviews.isEmpty()) {
            return urgentReviews + newItems
        }

        // 3. 动态比例混合：将新词均匀分散到普通复习中
        val mixed = mutableListOf<T>()
        val reviewCount = normalReviews.size
        val newCount = newItems.size

        // 计算插入位置：将新词均匀分布
        // 例如：10个复习位置，4个新词 → 在位置 2, 5, 7, 10 后插入新词
        val insertPositions = if (newCount > 0) {
            val step = (reviewCount + 1).toDouble() / newCount
            (0 until newCount).map { i ->
                ((i + 1) * step).toInt().coerceAtMost(reviewCount)
            }.toSet()
        } else {
            emptySet()
        }

        var newItemIndex = 0
        normalReviews.forEachIndexed { index, review ->
            mixed.add(review)
            // 在特定位置后插入新词
            if ((index + 1) in insertPositions && newItemIndex < newItems.size) {
                mixed.add(newItems[newItemIndex])
                newItemIndex++
            }
        }

        // 4. 添加剩余的新词（如果有的话）
        while (newItemIndex < newItems.size) {
            mixed.add(newItems[newItemIndex])
            newItemIndex++
        }

        // 5. 组合最终列表
        return urgentReviews + mixed
    }
}
