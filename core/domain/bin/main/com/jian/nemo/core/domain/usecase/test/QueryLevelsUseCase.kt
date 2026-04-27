package com.jian.nemo.core.domain.usecase.test

import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 查询测试等级分布 Use Case
 *
 * 职责：根据内容类型和来源查询可用的等级列表
 * 提取自：TestViewModel.kt 行207-318
 */
class QueryLevelsUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository
) {

    private val allLevels = listOf("N5", "N4", "N3", "N2", "N1")

    /**
     * 查询今日学习内容的等级分布
     * @param contentType 内容类型：words, grammar, mixed
     * @return Pair<单词等级列表, 语法等级列表>
     */
    suspend fun queryTodayLearnedLevels(contentType: String): Pair<List<String>, List<String>> {
        val resetHour = settingsRepository.learningDayResetHourFlow.first()
        val today = com.jian.nemo.core.common.util.DateTimeUtils.getLearningDay(resetHour)
        return when (contentType) {
            "words" -> {
                val levels = wordRepository.getTodayLearnedLevels(today).first()
                Pair(levels, emptyList())
            }
            "grammar" -> {
                val levels = grammarRepository.getTodayLearnedGrammarLevels(today).first()
                Pair(emptyList(), levels)
            }
            "mixed" -> {
                val wordLevels = wordRepository.getTodayLearnedLevels(today).first()
                val grammarLevels = grammarRepository.getTodayLearnedGrammarLevels(today).first()
                Pair(wordLevels, grammarLevels)
            }
            else -> Pair(allLevels, allLevels)
        }
    }

    /**
     * 查询错题的等级分布
     */
    suspend fun queryWrongAnswerLevels(contentType: String): Pair<List<String>, List<String>> {
        return when (contentType) {
            "words" -> {
                val levels = wordRepository.getWrongAnswerLevels().first()
                Pair(levels, emptyList())
            }
            "grammar" -> {
                val levels = grammarRepository.getWrongAnswerGrammarLevels().first()
                Pair(emptyList(), levels)
            }
            "mixed" -> {
                val wordLevels = wordRepository.getWrongAnswerLevels().first()
                val grammarLevels = grammarRepository.getWrongAnswerGrammarLevels().first()
                Pair(wordLevels, grammarLevels)
            }
            else -> Pair(allLevels, allLevels)
        }
    }

    /**
     * 查询收藏的等级分布
     */
    suspend fun queryFavoriteLevels(contentType: String): Pair<List<String>, List<String>> {
        return when (contentType) {
            "words" -> {
                val levels = wordRepository.getFavoriteLevels().first()
                Pair(levels, emptyList())
            }
            "grammar" -> {
                val levels = grammarRepository.getFavoriteGrammarLevels().first()
                Pair(emptyList(), levels)
            }
            "mixed" -> {
                val wordLevels = wordRepository.getFavoriteLevels().first()
                val grammarLevels = grammarRepository.getFavoriteGrammarLevels().first()
                Pair(wordLevels, grammarLevels)
            }
            else -> Pair(allLevels, allLevels)
        }
    }

    /**
     * 查询已学习内容的等级分布
     */
    suspend fun queryLearnedLevels(contentType: String): Pair<List<String>, List<String>> {
        return when (contentType) {
            "words" -> {
                val levels = wordRepository.getLearnedLevels().first()
                Pair(levels, emptyList())
            }
            "grammar" -> {
                val levels = grammarRepository.getLearnedGrammarLevels().first()
                Pair(emptyList(), levels)
            }
            "mixed" -> {
                val wordLevels = wordRepository.getLearnedLevels().first()
                val grammarLevels = grammarRepository.getLearnedGrammarLevels().first()
                Pair(wordLevels, grammarLevels)
            }
            else -> Pair(allLevels, allLevels)
        }
    }

    /**
     * 查询今日复习内容的等级分布
     */
    suspend fun queryTodayReviewedLevels(contentType: String): Pair<List<String>, List<String>> {
        val resetHour = settingsRepository.learningDayResetHourFlow.first()
        val today = com.jian.nemo.core.common.util.DateTimeUtils.getLearningDay(resetHour)
        return when (contentType) {
            "words" -> {
                val levels = wordRepository.getTodayReviewedLevels(today).first()
                Pair(levels, emptyList())
            }
            "grammar" -> {
                val levels = grammarRepository.getTodayReviewedGrammarLevels(today).first()
                Pair(emptyList(), levels)
            }
            "mixed" -> {
                val wordLevels = wordRepository.getTodayReviewedLevels(today).first()
                val grammarLevels = grammarRepository.getTodayReviewedGrammarLevels(today).first()
                Pair(wordLevels, grammarLevels)
            }
            else -> Pair(allLevels, allLevels)
        }
    }
}
