package com.jian.nemo.core.domain.usecase.grammar

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.onSuccess
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * 复习语法 Use Case
 */
class ReviewGrammarUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository,
    private val srsCalculator: SrsCalculator,
    private val studyRecordRepository: StudyRecordRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 复习语法并更新SRS状态
     *
     * @param grammarId 语法ID
     * @param quality 回忆质量(0-5)
     * @return Result<Grammar> 更新后的语法
     */
    suspend operator fun invoke(
        grammarId: Int,
        quality: Int
    ): Result<Grammar> {
        require(quality in 0..5) { "Quality must be 0-5, got $quality" }

        return try {
            // 1. 获取语法
            val grammar = grammarRepository.getGrammarById(grammarId).firstOrNull()
                ?: return Result.Error(IllegalArgumentException("语法不存在: grammarId=$grammarId"))

            // 2. 计算SRS新状态
            val resetHour = settingsRepository.learningDayResetHourFlow.first()
            val today = DateTimeUtils.getLearningDay(resetHour)
            val srsResult = srsCalculator.calculate(grammar, quality, today)

            // 3. 更新语法状态
            val updatedGrammar = grammar.copy(
                repetitionCount = srsResult.repetitionCount,
                stability = srsResult.stability,
                difficulty = srsResult.difficulty,
                interval = srsResult.interval,
                nextReviewDate = srsResult.nextReviewDate,
                lastReviewedDate = srsResult.lastReviewedDate,
                firstLearnedDate = srsResult.firstLearnedDate ?: grammar.firstLearnedDate,
                lastModifiedTime = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            )

            grammarRepository.updateGrammar(updatedGrammar).onSuccess {
                // 4. 记录为复习
                studyRecordRepository.incrementReviewedGrammars()
            }

            Result.Success(updatedGrammar)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
