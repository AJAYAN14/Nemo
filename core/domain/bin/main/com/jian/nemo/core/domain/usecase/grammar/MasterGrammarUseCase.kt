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
 * 掌握语法 Use Case
 *
 * 业务流程:
 * 1. 获取语法当前状态
 * 2. 调用 SRS 算法计算新状态
 * 3. 更新数据库
 * 4. 更新今日学习记录
 *
 * 参考: MasterWordUseCase
 */
class MasterGrammarUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository,
    private val srsCalculator: SrsCalculator,
    private val studyRecordRepository: StudyRecordRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(
        grammarId: Int,
        quality: Int = 3
    ): Result<Grammar> {
        return try {
            val grammar = grammarRepository.getGrammarById(grammarId).firstOrNull()
                ?: return Result.Error(IllegalArgumentException("语法不存在: grammarId=$grammarId"))

            val resetHour = settingsRepository.learningDayResetHourFlow.first()
            val today = DateTimeUtils.getLearningDay(resetHour)
            val srsResult = srsCalculator.calculate(grammar, quality, today)

            val updatedGrammar = grammar.copy(
                repetitionCount = srsResult.repetitionCount,
                stability = srsResult.stability,
                difficulty = srsResult.difficulty,
                interval = srsResult.interval,
                nextReviewDate = srsResult.nextReviewDate,
                lastReviewedDate = srsResult.lastReviewedDate,
                firstLearnedDate = srsResult.firstLearnedDate ?: grammar.firstLearnedDate,
                isSkipped = false,
                lastModifiedTime = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            )

            grammarRepository.updateGrammar(updatedGrammar).onSuccess {
                if (grammar.repetitionCount == 0 && updatedGrammar.repetitionCount > 0) {
                    studyRecordRepository.incrementLearnedGrammars()
                } else {
                    studyRecordRepository.incrementReviewedGrammars()
                }
            }

            Result.Success(updatedGrammar)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
