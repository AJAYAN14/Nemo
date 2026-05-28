package com.jian.nemo.feature.test.domain.usecase

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.feature.test.domain.model.QuestionSource
import com.jian.nemo.feature.test.domain.model.TestContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 查询可用等级 UseCase
 *
 * 对应阶段二：UseCase 抽取
 * 将 ViewModel 中分散的 6 个查询方法合并为单一 UseCase
 */
class QueryAvailableLevelsUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val grammarWrongAnswerRepository: GrammarWrongAnswerRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 查询可用等级及其数量
     *
     * @param source 题目来源
     * @param contentType 内容类型
     * @return Pair(单词等级列表, 语法等级列表)，每个列表元素为 (等级名, 数量)
     */
    suspend operator fun invoke(
        source: QuestionSource,
        contentType: TestContentType
    ): Pair<List<Pair<String, Int>>, List<Pair<String, Int>>> = withContext(Dispatchers.IO) {
        val wordLevels = mutableListOf<Pair<String, Int>>()
        val grammarLevels = mutableListOf<Pair<String, Int>>()
        val allLevels = listOf("N5", "N4", "N3", "N2", "N1")

        // 1. 查询单词等级
        if (contentType != TestContentType.GRAMMAR) {
            when (source) {
                QuestionSource.TODAY -> {
                    val resetHour = settingsRepository.learningDayResetHourFlow.first()
                    val today = DateTimeUtils.getLearningDay(resetHour)
                    val words = wordRepository.getTodayLearnedWords(today).first()
                    // 按等级分组统计
                    words.groupBy { it.level.uppercase() }.forEach { (level, list) ->
                        if (level in allLevels) wordLevels.add(level to list.size)
                    }
                }
                QuestionSource.WRONG -> {
                    val ids = wrongAnswerRepository.getAllWrongWordIds()
                    if (ids.isNotEmpty()) {
                        val words = wordRepository.getWordsByIds(ids)
                        words.groupBy { it.level.uppercase() }.forEach { (level, list) ->
                            if (level in allLevels) wordLevels.add(level to list.size)
                        }
                    }
                }
                QuestionSource.FAVORITE -> {
                    val words = wordRepository.getFavoriteWords().first()
                    words.groupBy { it.level.uppercase() }.forEach { (level, list) ->
                        if (level in allLevels) wordLevels.add(level to list.size)
                    }
                }
                QuestionSource.LEARNED -> {
                    val words = wordRepository.getAllLearnedWords().first()
                        .filter { !it.isSkipped }
                    words.groupBy { it.level.uppercase() }.forEach { (level, list) ->
                        if (level in allLevels) wordLevels.add(level to list.size)
                    }
                }
                QuestionSource.TODAY_REVIEWED -> {
                    val resetHour = settingsRepository.learningDayResetHourFlow.first()
                    val today = DateTimeUtils.getLearningDay(resetHour)
                    val words = wordRepository.getTodayReviewedWords(today).first()
                    words.groupBy { it.level.uppercase() }.forEach { (level, list) ->
                        if (level in allLevels) wordLevels.add(level to list.size)
                    }
                }
                QuestionSource.ALL -> {
                    // 单词只能逐级查询
                    for (level in allLevels) {
                        val count = wordRepository.getAllWordsByLevel(level).first().size
                        if (count > 0) wordLevels.add(level to count)
                    }
                }
            }
        }

        // 2. 查询语法等级
        if (contentType != TestContentType.WORDS) {
            when (source) {
                QuestionSource.TODAY -> {
                    val resetHour = settingsRepository.learningDayResetHourFlow.first()
                    val today = DateTimeUtils.getLearningDay(resetHour)
                    val grammars = grammarRepository.getTodayLearnedGrammars(today).first()
                    grammars.groupBy { it.grammarLevel.uppercase() }.forEach { (level, list) ->
                        if (level in allLevels) grammarLevels.add(level to list.size)
                    }
                }
                QuestionSource.WRONG -> {
                    val ids = grammarWrongAnswerRepository.getAllWrongGrammarIds()
                    if (ids.isNotEmpty()) {
                        val grammars = grammarRepository.getGrammarsByIds(ids)
                        grammars.groupBy { it.grammarLevel.uppercase() }.forEach { (level, list) ->
                            if (level in allLevels) grammarLevels.add(level to list.size)
                        }
                    }
                }
                QuestionSource.FAVORITE -> {
                    val grammars = grammarRepository.getFavoriteGrammars().first()
                    grammars.groupBy { it.grammarLevel.uppercase() }.forEach { (level, list) ->
                        if (level in allLevels) grammarLevels.add(level to list.size)
                    }
                }
                QuestionSource.LEARNED -> {
                    val grammars = grammarRepository.getAllLearnedGrammars().first()
                    grammars.groupBy { it.grammarLevel.uppercase() }.forEach { (level, list) ->
                        if (level in allLevels) grammarLevels.add(level to list.size)
                    }
                }
                QuestionSource.TODAY_REVIEWED -> {
                    val resetHour = settingsRepository.learningDayResetHourFlow.first()
                    val today = DateTimeUtils.getLearningDay(resetHour)
                    val grammars = grammarRepository.getTodayReviewedGrammars(today).first()
                    grammars.groupBy { it.grammarLevel.uppercase() }.forEach { (level, list) ->
                        if (level in allLevels) grammarLevels.add(level to list.size)
                    }
                }
                QuestionSource.ALL -> {
                    // 语法批量查询优化
                    val allGrammars = grammarRepository.getGrammarsByLevels(allLevels).first()
                    val grouped = allGrammars.groupBy { it.grammarLevel.uppercase() }
                    for (level in allLevels) {
                        val list = grouped[level]
                        if (!list.isNullOrEmpty()) {
                            grammarLevels.add(level to list.size)
                        }
                    }
                }
            }
        }

        return@withContext wordLevels to grammarLevels
    }
}
