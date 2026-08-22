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
        val isLapse: Boolean, // 是否为失败导致
        val relativeOffset: Int = 3 // 相对卡片插入偏移量 (张数)
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
 * 负责处理卡片评分后的流转逻辑 (结合相对步长与 SRS 状态机)。
 * 不涉及数据库操作，只进行纯逻辑计算。
 */
class LearningScheduler @Inject constructor() {

    /**
     * 处理失败 (评分 < 3)
     */
    fun scheduleFailure(
        item: LearningItem,
        currentLapseCount: Int,
        stepConfig: List<Int>,
        leechThreshold: Int
    ): ScheduleResult {
        val newLapseCount = currentLapseCount + 1

        // 1. 钉子户检测
        if (newLapseCount >= leechThreshold.coerceAtLeast(1)) {
            return ScheduleResult.Leech(item, newLapseCount)
        }

        // 2. Again -> Reset to Step 0
        val nextStep = 0
        val firstStepMin = stepConfig.firstOrNull() ?: 1
        val dueTime = System.currentTimeMillis() + firstStepMin * 60 * 1000L

        // 计算新类型
        val newType = if (item.repetitionCount > 0) 3 else 1 // 3: Relearning, 1: Learning

        val updatedItem = when (item) {
            is LearningItem.WordItem -> item.copy(step = nextStep, dueTime = dueTime, type = newType)
            is LearningItem.GrammarItem -> item.copy(step = nextStep, dueTime = dueTime, type = newType)
        }

        // 生疏词：相对当前位置后移 3~5 张卡片（根据 lapse 次数错峰微扰，避免连错扎堆）
        val fuzz = (newLapseCount % 3)
        val relativeOffset = 3 + fuzz

        return ScheduleResult.Requeue(
            updatedItem = updatedItem,
            nextStepIndex = nextStep,
            dueTime = dueTime,
            isLapse = true,
            relativeOffset = relativeOffset
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
        // Hard (3): 保持当前 Step，中度间隔后移 (5~7 张)
        if (quality == 3) {
            val hardDelayMillis = if (currentStep == 0) {
                val againSecs = (stepConfig.getOrNull(0) ?: 1) * 60L
                if (stepConfig.size > 1) {
                    val nextSecs = (stepConfig.getOrNull(1) ?: 10) * 60L
                    (againSecs + nextSecs) / 2 * 1000L
                } else {
                    (againSecs * 1.5 * 1000L).toLong().coerceAtMost((againSecs + 86400L) * 1000L)
                }
            } else {
                stepConfig.getOrElse(currentStep) { 1 }.toLong() * 60 * 1000L
            }
            val dueTime = System.currentTimeMillis() + hardDelayMillis
            
            val newType = if (item.type == 0) 1 else item.type

            val updatedItem = when (item) {
                is LearningItem.WordItem -> item.copy(step = currentStep, dueTime = dueTime, type = newType)
                is LearningItem.GrammarItem -> item.copy(step = currentStep, dueTime = dueTime, type = newType)
            }

            return ScheduleResult.Requeue(
                updatedItem = updatedItem,
                nextStepIndex = currentStep,
                dueTime = dueTime,
                isLapse = false,
                relativeOffset = 6 // Hard 相对后移 6 张
            )
        }

        // 如果是 Good (4)，判断是否还有下一步
        if (quality == 4 && currentStep < stepConfig.size - 1) {
            val nextStep = currentStep + 1
            val nextStepMin = stepConfig.getOrElse(nextStep) { 10 }
            val dueTime = System.currentTimeMillis() + nextStepMin * 60 * 1000L

            val updatedItem = when (item) {
                is LearningItem.WordItem -> item.copy(step = nextStep, dueTime = dueTime, type = if (item.type == 0) 1 else item.type)
                is LearningItem.GrammarItem -> item.copy(step = nextStep, dueTime = dueTime, type = if (item.type == 0) 1 else item.type)
            }

            return ScheduleResult.Requeue(
                updatedItem = updatedItem,
                nextStepIndex = nextStep,
                dueTime = dueTime,
                isLapse = false,
                relativeOffset = 10 // Good 中间步：相对后移 10 张
            )
        }

        // 毕业 (Graduate):
        // 1. 评分是 Easy (5)
        // 2. 评分是 Good (4) 且已经是最后一个台阶
        val graduatedItem = when (item) {
            is LearningItem.WordItem -> item.copy(type = 2) // 2: Review
            is LearningItem.GrammarItem -> item.copy(type = 2)
        }
        return ScheduleResult.Graduate(graduatedItem, quality)
    }
}
