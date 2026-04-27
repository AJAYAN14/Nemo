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
 * 复习单词 Use Case
 *
 * 与MasterWordUseCase的区别:
 * - MasterWordUseCase: 首次学习，quality固定为3
 * - ReviewWordUseCase: 复习，quality由用户选择(0-5)
 *
 * 参考: 阶段09文档第394-442行
 */
class ReviewWordUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val srsCalculator: SrsCalculator,
    private val studyRecordRepository: StudyRecordRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 复习单词并更新SRS状态
     *
     * @param wordId 单词ID
     * @param quality 回忆质量(0-5)
     *   - 0: 完全忘记
     *   - 1: 基本忘记
     *   - 2: 勉强想起
     *   - 3: 想起但犹豫
     *   - 4: 想起较容易
     *   - 5: 完美记忆
     * @return Result<Word> 更新后的单词
     */
    suspend operator fun invoke(
        wordId: Int,
        quality: Int
    ): Result<Word> {
        require(quality in 0..5) { "Quality must be 0-5, got $quality" }

        return try {
            // 1. 获取单词
            val word = wordRepository.getWordById(wordId).firstOrNull()
                ?: return Result.Error(IllegalArgumentException("单词不存在: wordId=$wordId"))

            // 2. 计算SRS新状态
            val resetHour = settingsRepository.learningDayResetHourFlow.first()
            val today = DateTimeUtils.getLearningDay(resetHour)
            val srsResult = srsCalculator.calculate(word, quality, today)

            // 3. 更新单词状态
            val updatedWord = word.copy(
                repetitionCount = srsResult.repetitionCount,
                stability = srsResult.stability,
                difficulty = srsResult.difficulty,
                interval = srsResult.interval,
                nextReviewDate = srsResult.nextReviewDate,
                lastReviewedDate = srsResult.lastReviewedDate,
                firstLearnedDate = srsResult.firstLearnedDate ?: word.firstLearnedDate,
                lastModifiedTime = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            )

            wordRepository.updateWord(updatedWord).onSuccess {
                // 4. 记录为复习（不是首次学习）
                studyRecordRepository.incrementReviewedWords()
            }

            Result.Success(updatedWord)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
