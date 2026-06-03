package com.jian.nemo.core.domain.usecase.test

import com.jian.nemo.core.domain.model.SrsItem
import kotlin.math.pow

/**
 * 基于 FSRS 状态的加权随机抽样器
 *
 * 根据每个 SrsItem 的 difficulty（难度）、stability（记忆稳定性）和 lastReviewedDate（最后复习时间）
 * 三个维度计算抽取权重，使用 Efraimidis-Spirakis 加权水库抽样算法实现无放回加权随机抽样。
 *
 * 核心原理：
 * - 难度越高 → 权重越大 → 更容易被抽中
 * - 记忆稳定性越低 → 权重越大 → 更容易被抽中（容易忘的词优先）
 * - 久未复习的词有轻微提权（防止遗忘盲区）
 */
object WeightedSampler {

    // ========== 常量定义 ==========

    /** 新词（repetitionCount == 0）的默认权重 */
    const val DEFAULT_NEW_ITEM_WEIGHT = 1.0

    /** 开启 BOOST_NEW 模式时新词的极高权重 */
    private const val HIGH_NEW_ITEM_WEIGHT = 10.0

    /** 权重保底下限，确保已掌握的词仍有极低概率被抽到 */
    private const val MIN_WEIGHT = 0.01

    /** 默认难度指数：控制高难度词的权重放大程度 */
    private const val DEFAULT_DIFFICULTY_EXPONENT = 1.5

    /** BOOST_DIFFICULT 模式下的难度指数：极端放大 */
    private const val BOOSTED_DIFFICULTY_EXPONENT = 3.0

    /** 稳定性衰减率：控制 stability 对权重的影响曲线 */
    private const val STABILITY_DECAY_RATE = 0.8

    /** 时间衰减因子：控制 lastReviewedDate 对权重的影响强度 */
    private const val RECENCY_WEIGHT = 5.0

    /** 从未有 lastReviewedDate 但 repetitionCount > 0 的异常情况的默认天数 */
    private const val DEFAULT_DAYS_SINCE_REVIEW = 30L

    // ========== 权重模式 ==========

    /**
     * 加权模式枚举
     *
     * - ADAPTIVE: 默认智能模式，综合考虑三个维度
     * - BOOST_DIFFICULT: 对应 prioritizeWrong 开关，极端放大难度因子
     * - BOOST_NEW: 对应 prioritizeNew 开关，给予新词极高权重
     */
    enum class WeightMode {
        ADAPTIVE,
        BOOST_DIFFICULT,
        BOOST_NEW
    }

    // ========== 核心方法 ==========

    /**
     * 计算单个 SrsItem 的抽取权重
     *
     * @param item 待计算的 SrsItem（Word 或 Grammar）
     * @param today 今天的 Epoch Day
     * @param mode 加权模式
     * @return 抽取权重值（>= MIN_WEIGHT）
     */
    fun calculateWeight(item: SrsItem, today: Long, mode: WeightMode): Double {
        // 1. 新词特判：未学习过的词没有 FSRS 数据，赋予固定权重
        if (item.repetitionCount == 0) {
            return when (mode) {
                WeightMode.BOOST_NEW -> HIGH_NEW_ITEM_WEIGHT
                else -> DEFAULT_NEW_ITEM_WEIGHT
            }
        }

        // 2. 难度因子 (difficultyFactor)
        // difficulty 范围 1-10，归一化后取幂次放大
        // 如果 difficulty <= 0（异常数据），按最低正常值 1.0 处理
        val effectiveDifficulty = if (item.difficulty <= 0f) 1.0 else item.difficulty.toDouble()
        val normalizedDifficulty = effectiveDifficulty / 10.0
        val currentDifficultyExponent = when (mode) {
            WeightMode.BOOST_DIFFICULT -> BOOSTED_DIFFICULTY_EXPONENT
            else -> DEFAULT_DIFFICULTY_EXPONENT
        }
        val difficultyFactor = normalizedDifficulty.pow(currentDifficultyExponent)

        // 3. 稳定性因子 (stabilityFactor)
        // stability 越低（用户越容易忘），因子越大
        // 如果 stability <= 0（异常数据），按 0.5 处理（代表极不稳定）
        val effectiveStability = if (item.stability <= 0f) 0.5 else item.stability.toDouble()
        val stabilityFactor = 1.0 / (1.0 + effectiveStability.pow(STABILITY_DECAY_RATE))

        // 4. 时间衰减因子 (recencyBonus)
        // 距离上次复习的天数越多，bonus 略有下降（但不会主导权重）
        val daysSinceLastReview = if (item.lastReviewedDate != null) {
            (today - item.lastReviewedDate!!).coerceAtLeast(0)
        } else {
            // repetitionCount > 0 但无 lastReviewedDate 的异常情况
            DEFAULT_DAYS_SINCE_REVIEW
        }
        val recencyBonus = 1.0 + RECENCY_WEIGHT / (daysSinceLastReview.toDouble() + 1.0)

        // 5. 组合三个因子
        val weight = difficultyFactor * stabilityFactor * recencyBonus

        // 6. 保底：确保已掌握的词仍有极低概率被抽到
        return weight.coerceAtLeast(MIN_WEIGHT)
    }

    /**
     * 对候选池执行无放回加权随机抽样
     *
     * 使用 Efraimidis-Spirakis 算法：
     * 对每个候选项生成 key = random(0,1) ^ (1/weight)，取 key 最大的 count 个。
     * 权重越高的项生成大 key 值的概率越高，从而被选中的概率越高。
     *
     * @param items 候选池
     * @param count 需要抽取的数量
     * @param today 今天的 Epoch Day
     * @param mode 加权模式
     * @return 抽取结果列表（无重复）
     */
    fun <T : SrsItem> weightedSample(
        items: List<T>,
        count: Int,
        today: Long,
        mode: WeightMode
    ): List<T> {
        // 边界：池子不足时直接全量返回并打乱
        if (items.size <= count) return items.shuffled()

        // 边界：请求数量 <= 0
        if (count <= 0) return emptyList()

        val random = java.util.Random()
        return items
            .map { item ->
                val weight = calculateWeight(item, today, mode)
                // Efraimidis-Spirakis key: random(0,1) ^ (1/weight)
                // random.nextDouble() 返回 [0, 1)，权重越高 → 1/weight 越小 → 指数越小 → 结果越大
                val key = random.nextDouble().pow(1.0 / weight)
                item to key
            }
            .sortedByDescending { it.second }
            .take(count)
            .map { it.first }
    }
}
