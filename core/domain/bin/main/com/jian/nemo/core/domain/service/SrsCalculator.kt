package com.jian.nemo.core.domain.service

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.SrsItem
import com.jian.nemo.core.domain.model.SrsUpdateResult

/**
 * SRS (Spaced Repetition System) 算法计算器接口
 *
 * 当前实现: FSRS 6 (Free Spaced Repetition Scheduler)
 */
interface SrsCalculator {
    /**
     * 计算 SRS 更新结果
     *
     * @param item 当前 SRS 项目状态（Word 或 Grammar）
     * @param quality 回答质量 (0-5)
     *   - 0-2: 忘记 → FSRS Again
     *   - 3: 困难 → FSRS Hard
     *   - 4: 良好 → FSRS Good
     *   - 5: 简单 → FSRS Easy
     * @param today 今天的 Epoch Day
     * @return SRS 更新后的状态（stability, difficulty, interval 等）
     */
    fun calculate(
        item: SrsItem,
        quality: Int,
        today: Long = DateTimeUtils.getCurrentEpochDay()
    ): SrsUpdateResult
}
