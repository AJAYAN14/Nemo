package com.jian.nemo.feature.learning.domain

import com.jian.nemo.feature.learning.presentation.LearningItem
import javax.inject.Inject

/**
 * 调度结果
 */
sealed class ScheduleResult {
    /**
     * 重新入队 (Review/Learning)
     * 对应: Again, Hard, Good (Intermediate Step)
     */
    data class Requeue(
        val updatedItem: LearningItem,
        val nextStepIndex: Int,
        val dueTime: Long,
        val isLapse: Boolean // 是否为失败导致
    ) : ScheduleResult()

    /**
     * 毕业 (Graduate)
     * 对应: Good (Last Step), Easy
     */
    data class Graduate(
        val item: LearningItem,
        val quality: Int
    ) : ScheduleResult()

    /**
     * 钉子户 (Leech)
     * 对应: 失败次数过多
     */
    data class Leech(
        val item: LearningItem,
        val totalLapses: Int
    ) : ScheduleResult()
}

/**
 * 学习调度器
 *
 * 负责处理卡片评分后的流转逻辑 (Anki 算法的核心状态机)。
 * 不涉及数据库操作，只进行纯逻辑计算。
 */
class LearningScheduler @Inject constructor() {

    companion object {
        private const val LEECH_THRESHOLD = 5
    }

    /**
     * 处理失败 (评分 < 3)
     */
    /**
     * 处理失败 (评分 < 3)
     */
    fun scheduleFailure(
        item: LearningItem,
        currentLapseCount: Int,
        stepConfig: List<Int>,
        leechThreshold: Int = LEECH_THRESHOLD
    ): ScheduleResult {
        val newLapseCount = currentLapseCount + 1

        // 1. 钉子户检测
        if (newLapseCount >= leechThreshold.coerceAtLeast(1)) {
            return ScheduleResult.Leech(item, newLapseCount)
        }

        // 2. Anki Logic: Again -> Reset to Step 0
        val nextStep = 0
        // Use the first step from config (e.g. 1 min for relearning steps)
        val firstStepMin = stepConfig.firstOrNull() ?: 1
        val dueTime = System.currentTimeMillis() + firstStepMin * 60 * 1000L

        val updatedItem = when (item) {
            is LearningItem.WordItem -> item.copy(step = nextStep, dueTime = dueTime)
            is LearningItem.GrammarItem -> item.copy(step = nextStep, dueTime = dueTime)
        }

        return ScheduleResult.Requeue(
            updatedItem = updatedItem,
            nextStepIndex = nextStep,
            dueTime = dueTime,
            isLapse = true
        )
    }

    /**
     * 处理通过 (评分 >= 3)
     */
    fun schedulePass(
        item: LearningItem,
        quality: Int,
        currentStep: Int,
        stepConfig: List<Int>
    ): ScheduleResult {
        // Hard (3): 对齐 Anki 逻辑
        if (quality == 3) {
            val hardDelayMillis = if (currentStep == 0) {
                val againSecs = (stepConfig.getOrNull(0) ?: 1) * 60L
                if (stepConfig.size > 1) {
                    val nextSecs = (stepConfig.getOrNull(1) ?: 10) * 60L
                    (againSecs + nextSecs) / 2 * 1000L
                } else {
                    // 只有一步时，取 1.5 倍，最高不超过 1 天
                    (againSecs * 1.5 * 1000L).toLong().coerceAtMost((againSecs + 86400L) * 1000L)
                }
            } else {
                stepConfig.getOrElse(currentStep) { 1 }.toLong() * 60 * 1000L
            }
            val dueTime = System.currentTimeMillis() + hardDelayMillis

            val updatedItem = when (item) {
                is LearningItem.WordItem -> item.copy(step = currentStep, dueTime = dueTime)
                is LearningItem.GrammarItem -> item.copy(step = currentStep, dueTime = dueTime)
            }

            return ScheduleResult.Requeue(
                updatedItem = updatedItem,
                nextStepIndex = currentStep,
                dueTime = dueTime,
                isLapse = false
            )
        }

        // 如果是 Good (4)，判断是否还有下一步
        if (quality == 4 && currentStep < stepConfig.size - 1) {
            val nextStep = currentStep + 1
            val nextStepMin = stepConfig.getOrElse(nextStep) { 10 }
            val dueTime = System.currentTimeMillis() + nextStepMin * 60 * 1000L

            val updatedItem = when (item) {
                is LearningItem.WordItem -> item.copy(step = nextStep, dueTime = dueTime)
                is LearningItem.GrammarItem -> item.copy(step = nextStep, dueTime = dueTime)
            }

            return ScheduleResult.Requeue(
                updatedItem = updatedItem,
                nextStepIndex = nextStep,
                dueTime = dueTime,
                isLapse = false
            )
        }

        // 毕业 (Graduate):
        // 1. 评分是 Easy (5)
        // 2. 评分是 Good (4) 且已经是最后一个台阶
        return ScheduleResult.Graduate(item, quality)
    }
}
