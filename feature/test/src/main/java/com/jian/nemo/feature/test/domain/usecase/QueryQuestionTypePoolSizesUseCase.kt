package com.jian.nemo.feature.test.domain.usecase

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 查询各题型的可用池大小
 *
 * 根据测试配置（内容类型、来源、等级）查询每种题型在当前条件下可生成的题目数量上限。
 *
 * 注意：
 * - 卡片题需要 5 个单词生成 1 题，所以池大小 = 单词数 / 5
 * - 语法只支持选择题，其他题型池大小为 0
 * - 手打、匹配、排序题仅支持单词，不支持语法
 */
class QueryQuestionTypePoolSizesUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val grammarWrongAnswerRepository: GrammarWrongAnswerRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 查询各题型的池大小
     *
     * @param contentType 内容类型: "words", "grammar", "mixed"
     * @param source 题目来源: "today", "wrong", "favorite", "learned", "today_reviewed", "all"
     * @param selectedWordLevels 选中的单词等级
     * @param selectedGrammarLevels 选中的语法等级
     * @return Map<题型ID, 池大小>，题型ID: "multiple_choice", "typing", "card_matching", "sorting"
     */
    suspend operator fun invoke(
        contentType: String,
        source: String,
        selectedWordLevels: List<String>,
        selectedGrammarLevels: List<String>
    ): Map<String, Int> {
        val limit = 1000 // 查询上限，避免性能问题
        val resetHour = settingsRepository.learningDayResetHourFlow.first()
        val today = DateTimeUtils.getLearningDay(resetHour)

        // 1. 查询单词池大小（用于手打、匹配、排序、单词选择题）
        val wordPoolSize = if (contentType != "grammar") {
            queryWordPoolSize(source, selectedWordLevels, today, limit)
        } else {
            0
        }

        // 2. 查询语法池大小（仅用于选择题）
        val grammarPoolSize = if (contentType != "words") {
            queryGrammarPoolSize(source, selectedGrammarLevels, today, limit)
        } else {
            0
        }

        // 3. 计算各题型的池大小
        return when (contentType) {
            "grammar" -> {
                // 语法只支持选择题
                mapOf(
                    "multiple_choice" to grammarPoolSize,
                    "typing" to 0,
                    "card_matching" to 0,
                    "sorting" to 0
                )
            }
            "words" -> {
                // 单词支持所有题型
                mapOf(
                    "multiple_choice" to wordPoolSize,
                    "typing" to wordPoolSize,
                    "card_matching" to (wordPoolSize / 5).coerceAtLeast(0), // 卡片题：5个单词 = 1题
                    "sorting" to wordPoolSize
                )
            }
            "mixed" -> {
                // 混合模式：
                // - 选择题：单词 + 语法
                // - 手打/匹配/排序：仅单词
                val mcFromWords = wordPoolSize
                val mcFromGrammar = grammarPoolSize
                val totalMcPool = mcFromWords + mcFromGrammar

                mapOf(
                    "multiple_choice" to totalMcPool,
                    "typing" to wordPoolSize,
                    "card_matching" to (wordPoolSize / 5).coerceAtLeast(0),
                    "sorting" to wordPoolSize
                )
            }
            else -> mapOf(
                "multiple_choice" to 0,
                "typing" to 0,
                "card_matching" to 0,
                "sorting" to 0
            )
        }
    }

    private suspend fun queryWordPoolSize(
        source: String,
        selectedLevels: List<String>,
        today: Long,
        limit: Int
    ): Int {
        return when (source) {
            "today" -> {
                wordRepository.getTodayLearnedWords(today)
                    .first()
                    .filter { it.level in selectedLevels }
                    .take(limit)
                    .size
            }
            "wrong" -> {
                val ids = wrongAnswerRepository.getAllWrongWordIds()
                if (ids.isEmpty()) 0
                else {
                    wordRepository.getWordsByIds(ids)
                        .filter { it.level in selectedLevels }
                        .take(limit)
                        .size
                }
            }
            "favorite" -> {
                wordRepository.getFavoriteWords()
                    .first()
                    .filter { it.level in selectedLevels }
                    .take(limit)
                    .size
            }
            "learned" -> {
                wordRepository.getAllLearnedWords()
                    .first()
                    .filter { !it.isSkipped && it.level in selectedLevels }
                    .take(limit)
                    .size
            }
            "today_reviewed" -> {
                wordRepository.getTodayReviewedWords(today)
                    .first()
                    .filter { it.level in selectedLevels }
                    .take(limit)
                    .size
            }
            "all" -> {
                var total = 0
                for (level in selectedLevels) {
                    if (total >= limit) break
                    val count = wordRepository.getAllWordsByLevel(level)
                        .first()
                        .take(limit - total)
                        .size
                    total += count
                }
                total
            }
            else -> 0
        }
    }

    private suspend fun queryGrammarPoolSize(
        source: String,
        selectedLevels: List<String>,
        today: Long,
        limit: Int
    ): Int {
        return when (source) {
            "today" -> {
                grammarRepository.getTodayLearnedGrammars(today)
                    .first()
                    .filter { it.grammarLevel in selectedLevels }
                    .take(limit)
                    .size
            }
            "wrong" -> {
                val ids = grammarWrongAnswerRepository.getAllWrongGrammarIds()
                if (ids.isEmpty()) 0
                else {
                    grammarRepository.getGrammarsByIds(ids)
                        .filter { it.grammarLevel in selectedLevels }
                        .take(limit)
                        .size
                }
            }
            "favorite" -> {
                grammarRepository.getFavoriteGrammars()
                    .first()
                    .filter { it.grammarLevel in selectedLevels }
                    .take(limit)
                    .size
            }
            "learned" -> {
                grammarRepository.getAllLearnedGrammars()
                    .first()
                    .filter { it.grammarLevel in selectedLevels }
                    .take(limit)
                    .size
            }
            "today_reviewed" -> {
                grammarRepository.getTodayReviewedGrammars(today)
                    .first()
                    .filter { it.grammarLevel in selectedLevels }
                    .take(limit)
                    .size
            }
            "all" -> {
                val grammars = grammarRepository.getGrammarsByLevels(selectedLevels)
                    .first()
                    .take(limit)
                grammars.size
            }
            else -> 0
        }
    }
}
