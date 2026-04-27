package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 启动学习会话 Use Case
 *
 * 业务逻辑:
 * 1. 获取指定等级的新单词
 * 2. 计算今日已学习数量
 * 3. 根据每日目标返回应学习的单词列表
 *
 * 简化版本:不包含会话持久化(SessionRepository)
 * 会话管理在ViewModel层处理
 *
 * 参考: 旧项目StartLearningSessionUseCase (第31-34行, 第113行)
 */
class StartLearningSessionUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val settingsRepository: com.jian.nemo.core.domain.repository.SettingsRepository
) {
    /**
     * 启动学习会话
     *
     * @param level JLPT等级
     * @param dailyGoal 每日学习目标
     * @return LearningSessionResult 会话结果
     */
    suspend operator fun invoke(
        level: String,
        dailyGoal: Int
    ): LearningSessionResult {
        return try {
            // 1. 获取今日已学习数量
            val resetHour = settingsRepository.learningDayResetHourFlow.first()
            val today = DateTimeUtils.getLearningDay(resetHour)
            val todayLearned = wordRepository.getTodayLearnedWords(today).first()
            val todayLearnedCount = todayLearned.size

            // 2. 计算剩余目标
            val remaining = (dailyGoal - todayLearnedCount).coerceAtLeast(0)

            if (remaining == 0) {
                return LearningSessionResult.GoalCompleted(
                    learnedToday = todayLearnedCount
                )
            }

            // 3. 获取新单词
            val newWords = wordRepository.getNewWords(level).first()

            if (newWords.isEmpty()) {
                return LearningSessionResult.NoMoreWords(
                    level = level
                )
            }

            // 4. 返回应学习的单词列表(限制数量)
            val sessionWords = newWords.take(remaining)

            LearningSessionResult.Success(
                words = sessionWords,
                currentIndex = 0,
                totalCount = sessionWords.size,
                todayLearnedCount = todayLearnedCount,
                dailyGoal = dailyGoal
            )
        } catch (e: Exception) {
            LearningSessionResult.Error(e.message ?: "Unknown error")
        }
    }
}

/**
 * 学习会话结果
 */
sealed interface LearningSessionResult {
    /**
     * 成功启动会话
     */
    data class Success(
        val words: List<Word>,
        val currentIndex: Int,
        val totalCount: Int,
        val todayLearnedCount: Int,
        val dailyGoal: Int
    ) : LearningSessionResult

    /**
     * 今日目标已完成
     */
    data class GoalCompleted(
        val learnedToday: Int
    ) : LearningSessionResult

    /**
     * 无更多单词
     */
    data class NoMoreWords(
        val level: String
    ) : LearningSessionResult

    /**
     * 错误
     */
    data class Error(
        val error: String
    ) : LearningSessionResult
}
