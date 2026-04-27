package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.onSuccess
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * 掌握单词 Use Case
 *
 * 业务流程:
 * 1. 获取单词当前状态
 * 2. 调用 SRS 算法计算新状态（基于重置时间确定的“学习日”）
 * 3. 更新数据库
 * 4. 更新今日学习记录
 *
 * 参考:
 * - 旧项目: MasterWordUseCase.kt (第16-55行)
 * - 实施计划: 06-单词Domain层.md (第291-351行)
 * - 架构规范: rules.md (第248-267行)
 */
class MasterWordUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val srsCalculator: SrsCalculator,
    private val studyRecordRepository: StudyRecordRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 执行掌握单词操作
     *
     * @param wordId 单词ID
     * @param quality SRS质量评分 (0-5), 默认3表示掌握
     * @return Result<Word> 更新后的单词
     */
    suspend operator fun invoke(
        wordId: Int,
        quality: Int = 3
    ): Result<Word> {
        return try {
            // 1. 获取单词
            val word = wordRepository.getWordById(wordId).firstOrNull()
                ?: return Result.Error(
                    IllegalArgumentException("单词不存在: wordId=$wordId")
                )

            // 2. 计算 SRS 新状态
            val resetHour = settingsRepository.learningDayResetHourFlow.first()
            val today = DateTimeUtils.getLearningDay(resetHour)
            val srsResult = srsCalculator.calculate(word, quality, today)

            // 3. 更新单词状态
            // 参考: 旧项目 MasterWordUseCase.kt 第29-32行 - 移除跳过标记
            val updatedWord = word.copy(
                repetitionCount = srsResult.repetitionCount,
                stability = srsResult.stability,
                difficulty = srsResult.difficulty,
                interval = srsResult.interval,
                nextReviewDate = srsResult.nextReviewDate,
                lastReviewedDate = srsResult.lastReviewedDate,
                firstLearnedDate = srsResult.firstLearnedDate ?: word.firstLearnedDate,
                isSkipped = false,  // 掌握后取消跳过标记
                lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
            )

            // 4. 保存到数据库
            wordRepository.updateWord(updatedWord).onSuccess {
                // 5. 更新学习记录（这里 repository 内部已经处理了日期统一）
                // 参考: 旧项目 MasterWordUseCase.kt 第38行
                if (word.repetitionCount == 0 && updatedWord.repetitionCount > 0) {
                    // 首次学习
                    studyRecordRepository.incrementLearnedWords()
                } else {
                    // 复习
                    studyRecordRepository.incrementReviewedWords()
                }
            }

            Result.Success(updatedWord)

        } catch (e: Exception) {
            // TODO: 使用结构化日志框架记录异常
            Result.Error(e)
        }
    }
}
