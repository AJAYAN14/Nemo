package com.jian.nemo.feature.learning.domain

import com.jian.nemo.core.common.util.SrsTimeFormatter
import com.jian.nemo.core.domain.model.SrsItem
import com.jian.nemo.core.domain.service.SrsCalculator
import javax.inject.Inject

/**
 * SRS 间隔预览计算器
 * 负责计算并在 UI 上显示 0-5 分对应的下次复习间隔
 *
 * 适用于 Word 和 Grammar
 */
class SrsIntervalPreview @Inject constructor(
    private val srsCalculator: SrsCalculator
) {
    /**
     * 计算间隔预览文本
     *
     * @param item SRS 项目 (Word 或 Grammar)
     * @param itemId 项目 ID (用于在 steps 中查找)
     * @param steps 当前学习阶段映射表 (WordSteps 或 GrammarSteps).
     * @param learningStepsConfig 新词学习步骤配置
     * @param relearningStepsConfig 重学步骤配置
     * @param today 当前日期
     */
    fun calculate(
        item: SrsItem?,
        itemId: Int,
        steps: Map<Int, Int>?,
        learningStepsConfig: List<Int>,
        relearningStepsConfig: List<Int>,
        today: Long
    ): Map<Int, String> {
        if (item == null) return emptyMap()

        val intervals = mutableMapOf<Int, String>()

        val currentStep = steps?.get(itemId)
        val isNew = item.repetitionCount == 0
        // Currently Learning: New Card OR Relearning Card
        val isLearning = isNew || currentStep != null

        for (q in 0..5) {
            // 1. Fail (Again): Show first relearning step (or learning step if new)
            if (q < 3) {
                val firstStepMin = if (isNew) {
                    learningStepsConfig.firstOrNull() ?: 1
                } else {
                    relearningStepsConfig.firstOrNull() ?: 1
                }

                intervals[q] = SrsTimeFormatter.formatSrsInterval(firstStepMin * 60L)
                continue
            }

            // 2. Pass (Hard/Good/Easy) while in Learning Mode
            if (isLearning) {
                // Decide which config to use
                val config = if (isNew) learningStepsConfig else relearningStepsConfig
                val stepIndex = currentStep ?: 0

                // Good (4) Logic:
                if (q == 4) {
                    if (stepIndex < config.size - 1) {
                        // Move to next step
                        val nextStepMin = config.getOrElse(stepIndex + 1) { 10 }
                        intervals[q] = SrsTimeFormatter.formatSrsInterval(nextStepMin * 60L)
                        continue
                    } else {
                        // Graduate
                        // 特殊处理: 重学毕业 (Relearning Graduate)
                        if (!isNew) {
                            intervals[q] = SrsTimeFormatter.formatSrsInterval(item.interval * 86400L)
                            continue
                        }
                        // 新卡毕业 -> Fall through to calculator
                    }
                } else if (q == 3) {
                    // Hard (3) Logic: 对齐 Anki
                    val hardSecs = if (stepIndex == 0) {
                        val againSecs = (config.getOrNull(0) ?: 1) * 60L
                        if (config.size > 1) {
                            val nextSecs = (config.getOrNull(1) ?: 10) * 60L
                            (againSecs + nextSecs) / 2
                        } else {
                            // 只有一步时，取 1.5 倍，最高不超过 1 天
                            (againSecs * 1.5).toLong().coerceAtMost(againSecs + 86400L)
                        }
                    } else {
                        (config.getOrElse(stepIndex) { 1 } * 60L)
                    }
                    intervals[q] = SrsTimeFormatter.formatSrsInterval(hardSecs)
                    continue
                } else if (q == 5) {
                    // Easy (5): Instant Graduate
                    // Fall through to calculator
                }
            }

            // 3. SRS Calculator / Graduation
            val result = srsCalculator.calculate(item, q, today)
            intervals[q] = SrsTimeFormatter.formatSrsInterval(result.interval * 86400L)
        }
        return intervals
    }
}
