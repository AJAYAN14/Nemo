package com.jian.nemo.core.domain.usecase.review

import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 处理复习结果 UseCase
 *
 * 负责：
 * 1. 计算 SRS 算法 (SrsCalculator)
 * 2. 更新数据库实体 (Word/Grammar)
 * 3. 更新今日统计 (SettingsRepository)
 * 4. 更新学习记录 (StudyRecordRepository)
 */
class ProcessReviewResultUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository,
    private val studyRecordRepository: StudyRecordRepository,
    private val reviewLogRepository: com.jian.nemo.core.domain.repository.ReviewLogRepository, // [NEW]
    private val srsCalculator: SrsCalculator
) {

    companion object {
        private const val TAG = "ProcessReviewResult"
    }

    suspend fun processWord(word: Word, quality: Int) {
        val resetHour = settingsRepository.learningDayResetHourFlow.first()
        val today = com.jian.nemo.core.common.util.DateTimeUtils.getLearningDay(resetHour)

        // 1. 记录日志 (在由于计算更新前记录，以捕获"本次复习时的状态")
        // 计算实际间隔 (Actual Interval) = 今天 - 上次复习时间
        val lastDate = word.lastReviewedDate
        val actualInterval = if (lastDate != null && lastDate > 0L) {
            (today - lastDate).toInt()
        } else {
            0 // 首次学习或数据缺失
        }

        try {
            reviewLogRepository.insertLog(
                com.jian.nemo.core.domain.model.ReviewLog(
                    itemId = word.id,
                    itemType = "word",
                    reviewDate = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis(), // 精确时间戳
                    intervalDays = actualInterval,
                    rating = quality
                )
            )
        } catch (e: Exception) {
            println("$TAG: 记录日志失败 - ${e.message}")
        }

        // 2. SRS 计算
        val result = srsCalculator.calculate(word, quality, today)
        val updatedWord = word.copy(
            repetitionCount = result.repetitionCount,
            stability = result.stability,
            difficulty = result.difficulty,
            interval = result.interval,
            nextReviewDate = result.nextReviewDate,
            lastReviewedDate = result.lastReviewedDate,
            firstLearnedDate = result.firstLearnedDate
        )

        wordRepository.updateWord(updatedWord)

        try {
            // 记录今日复习ID
            settingsRepository.addTodayTestedWordId(word.id)

            // 低质量视为错误
            if (quality <= 2) {
                settingsRepository.addTodayWrongWordId(word.id)
            }

            // 更新学习记录 (复习计数)
            studyRecordRepository.incrementReviewedWords(1)
        } catch (e: Exception) {
            println("$TAG: 更新复习记录失败 - ${e.message}")
        }
    }

    suspend fun processGrammar(grammar: Grammar, quality: Int) {
        val resetHour = settingsRepository.learningDayResetHourFlow.first()
        val today = com.jian.nemo.core.common.util.DateTimeUtils.getLearningDay(resetHour)

        // 1. 记录日志
        val lastDate = grammar.lastReviewedDate
        val actualInterval = if (lastDate != null && lastDate > 0L) {
            (today - lastDate).toInt()
        } else {
            0
        }

        try {
            reviewLogRepository.insertLog(
                com.jian.nemo.core.domain.model.ReviewLog(
                    itemId = grammar.id,
                    itemType = "grammar",
                    reviewDate = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis(),
                    intervalDays = actualInterval,
                    rating = quality
                )
            )
        } catch (e: Exception) {
            println("$TAG: 记录日志失败 - ${e.message}")
        }

        // 2. SRS 计算
        val result = srsCalculator.calculate(grammar, quality, today)
        val updatedGrammar = grammar.copy(
            repetitionCount = result.repetitionCount,
            stability = result.stability,
            difficulty = result.difficulty,
            interval = result.interval,
            nextReviewDate = result.nextReviewDate,
            lastReviewedDate = result.lastReviewedDate,
            firstLearnedDate = result.firstLearnedDate
        )

        grammarRepository.updateGrammar(updatedGrammar)

        try {
            // 记录今日复习ID
            settingsRepository.addTodayTestedGrammarId(grammar.id)

            if (quality <= 2) {
                settingsRepository.addTodayWrongGrammarId(grammar.id)
            }

            // 更新学习记录 (复习计数)
            studyRecordRepository.incrementReviewedGrammars(1)
        } catch (e: Exception) {
            println("$TAG: 更新复习记录失败 - ${e.message}")
        }
    }
}
