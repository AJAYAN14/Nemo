package com.jian.nemo.feature.test.domain.usecase

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.feature.test.domain.model.QuestionSource
import com.jian.nemo.feature.test.domain.model.TestConfig
import com.jian.nemo.feature.test.domain.model.TestContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 查询可用数据量 UseCase
 *
 * 对应阶段二：UseCase 抽取
 * 从 TestSettingsViewModel 中提取数据量查询逻辑，消除重复代码
 */
class QueryAvailableDataCountUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val grammarWrongAnswerRepository: GrammarWrongAnswerRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 查询可用数据量 (词数, 语法数)
     *
     * @param config 测试配置
     * @return Pair(可用单词数, 可用语法数)
     */
    suspend operator fun invoke(config: TestConfig): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val limit = 1000 // 使用合理的上限值避免性能问题
        val resetHour = settingsRepository.learningDayResetHourFlow.first()
        val today = DateTimeUtils.getLearningDay(resetHour)

        val wordCount = if (config.testContentType != TestContentType.GRAMMAR) {
            when (config.questionSource) {
                QuestionSource.TODAY -> {
                    wordRepository.getTodayLearnedWords(today)
                        .first()
                        .filter { it.level in config.selectedWordLevels }
                        .take(limit)
                        .size
                }
                QuestionSource.WRONG -> {
                    val ids = wrongAnswerRepository.getAllWrongWordIds()
                    if (ids.isEmpty()) 0
                    else {
                        wordRepository.getWordsByIds(ids)
                            .filter { it.level in config.selectedWordLevels }
                            .take(limit)
                            .size
                    }
                }
                QuestionSource.FAVORITE -> {
                    wordRepository.getFavoriteWords()
                        .first()
                        .filter { it.level in config.selectedWordLevels }
                        .take(limit)
                        .size
                }
                QuestionSource.LEARNED -> {
                    wordRepository.getAllLearnedWords()
                        .first()
                        .filter { it.level in config.selectedWordLevels }
                        .take(limit)
                        .size
                }
                QuestionSource.TODAY_REVIEWED -> {
                    // 今日复习内容
                    wordRepository.getTodayReviewedWords(today)
                        .first()
                        .filter { it.level in config.selectedWordLevels }
                        .take(limit)
                        .size
                }
                QuestionSource.ALL -> {
                    // 查询选中等级的单词总数
                    // 优化：与其逐个查全量再 take，不如按等级查
                    var total = 0
                    for (level in config.selectedWordLevels) {
                        if (total >= limit) break
                        val count = wordRepository.getAllWordsByLevel(level)
                            .first()
                            .take(limit - total)
                            .size
                        total += count
                    }
                    total
                }
            }
        } else 0

        val grammarCount = if (config.testContentType != TestContentType.WORDS) {
            when (config.questionSource) {
                QuestionSource.TODAY -> {
                    grammarRepository.getTodayLearnedGrammars(today)
                        .first()
                        .filter { it.grammarLevel in config.selectedGrammarLevels }
                        .take(limit)
                        .size
                }
                QuestionSource.WRONG -> {
                    val ids = grammarWrongAnswerRepository.getAllWrongGrammarIds()
                    if (ids.isEmpty()) 0
                    else {
                        grammarRepository.getGrammarsByIds(ids)
                            .filter { it.grammarLevel in config.selectedGrammarLevels }
                            .take(limit)
                            .size
                    }
                }
                QuestionSource.FAVORITE -> {
                    grammarRepository.getFavoriteGrammars()
                        .first()
                        .filter { it.grammarLevel in config.selectedGrammarLevels }
                        .take(limit)
                        .size
                }
                QuestionSource.LEARNED -> {
                    grammarRepository.getAllLearnedGrammars()
                        .first()
                        .filter { it.grammarLevel in config.selectedGrammarLevels }
                        .take(limit)
                        .size
                }
                QuestionSource.TODAY_REVIEWED -> {
                    grammarRepository.getTodayReviewedGrammars(today)
                        .first()
                        .filter { it.grammarLevel in config.selectedGrammarLevels }
                        .take(limit)
                        .size
                }
                QuestionSource.ALL -> {
                    // 查询选中等级的语法总数
                    val grammars = grammarRepository.getGrammarsByLevels(config.selectedGrammarLevels)
                        .first()
                        .take(limit)
                    grammars.size
                }
            }
        } else 0

        return@withContext wordCount to grammarCount
    }
}
