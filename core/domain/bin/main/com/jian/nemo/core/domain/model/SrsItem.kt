package com.jian.nemo.core.domain.model

/**
 * SRS (Spaced Repetition System) 项目接口
 *
 * Word 和 Grammar 实体都实现这个接口，抽象出 FSRS 算法所需的字段
 *
 * 基于 FSRS 6 算法:
 * - stability: 记忆稳定性 (天数) — 多少天后回忆概率降至 90%
 * - difficulty: 难度 (1-10)
 */
interface SrsItem {
    /**
     * 复习次数（已成功复习的次数）
     * - 0: 未学习
     * - 1+: 已复习次数
     */
    val repetitionCount: Int

    /**
     * 记忆稳定性 (FSRS)
     * - 0: 未学习 (新卡)
     * - >0: 稳定性 (天)
     */
    val stability: Float

    /**
     * 难度 (FSRS)
     * - 0: 未学习 (新卡)
     * - 1-10: 难度值
     */
    val difficulty: Float

    /**
     * 下次复习间隔（天数）
     */
    val interval: Int

    /**
     * 下次复习日期（Epoch Day）
     * - 0: 未学习或需要立即复习
     */
    val nextReviewDate: Long

    /**
     * 最后复习日期（Epoch Day）
     */
    val lastReviewedDate: Long?

    /**
     * 首次学习日期（Epoch Day）
     */
    val firstLearnedDate: Long?
}

/**
 * SRS 更新结果
 */
data class SrsUpdateResult(
    val repetitionCount: Int,
    val stability: Float,
    val difficulty: Float,
    val interval: Int,
    val nextReviewDate: Long,
    val lastReviewedDate: Long?,
    val firstLearnedDate: Long?
)
