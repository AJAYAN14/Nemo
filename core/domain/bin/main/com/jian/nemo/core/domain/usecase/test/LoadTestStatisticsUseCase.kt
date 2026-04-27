package com.jian.nemo.core.domain.usecase.test

import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * 测试统计数据
 */
data class TestStatistics(
    val todayTestCount: Int = 0,
    val todayAccuracy: Float = 0f,
    val wrongWordsCount: Int = 0,
    val favoriteWordsCount: Int = 0,
    val consecutiveTestDays: Int = 0,
    val maxTestStreak: Int = 0,
    val totalTestCount: Int = 0,
    val overallAccuracy: Float = 0f
)

/**
 * 加载测试统计数据 Use Case
 *
 * 职责：聚合多个数据源的统计信息
 * 提取自：TestViewModel.kt 行67-120
 */
class LoadTestStatisticsUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val settingsRepository: SettingsRepository,
    private val favoriteQuestionRepository: com.jian.nemo.core.domain.repository.FavoriteQuestionRepository,
    private val grammarWrongAnswerRepository: com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
) {

    /**
     * 获取今日测试次数 Flow
     */
    fun getTodayTestCountFlow(today: Long): Flow<Int> = wordRepository.getTodayTestCount(today)

    /**
     * 获取今日正确率 Flow
     */
    fun getTodayAccuracyFlow(today: Long): Flow<Float> = wordRepository.getTodayTestAccuracy(today)

    /**
     * 获取错题数量 Flow（包含单词和语法）
     */
    fun getWrongWordsCountFlow(): Flow<Int> =
        combine(
            wrongAnswerRepository.getAllWrongAnswers(),
            grammarWrongAnswerRepository.getAllWrongAnswers()
        ) { wrongWords, wrongGrammars ->
            wrongWords.size + wrongGrammars.size
        }

    /**
     * 获取收藏数量 Flow（包含单词和题目）
     */
    fun getFavoriteWordsCountFlow(): Flow<Int> =
        combine(
            wordRepository.getFavoriteWords(),
            favoriteQuestionRepository.getAllFavoriteQuestions()
        ) { favoriteWords, favoriteQuestions ->
            favoriteWords.size + favoriteQuestions.size
        }

    /**
     * 获取连续测试天数 Flow
     */
    fun getConsecutiveTestDaysFlow(): Flow<Int> = settingsRepository.testStreakFlow

    /**
     * 获取最大连续天数 Flow
     */
    fun getMaxTestStreakFlow(): Flow<Int> = settingsRepository.maxTestStreakFlow

    /**
     * 获取总测试次数 Flow
     */
    fun getTotalTestCountFlow(): Flow<Int> = wordRepository.getTotalTestCount()

    /**
     * 获取总体正确率 Flow
     */
    fun getOverallAccuracyFlow(): Flow<Float> = wordRepository.getOverallAccuracy()
}
