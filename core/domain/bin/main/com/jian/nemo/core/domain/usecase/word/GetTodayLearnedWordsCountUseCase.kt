package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.ext.asResult
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 获取今日学习单词数量 Use Case
 *
 * 业务规则:
 * 1. 统计今日首次学习的单词数量
 * 2. 这里的“今日”遵循设置中的重置时间（例如凌晨4点之前仍算作前一天）
 *
 * 参考:
 * - 旧项目: 在MasterWordUseCase.kt中使用 getTodayLearnedWordsCount() (第63行)
 * - 实施计划: 06-单词Domain层.md Use Cases列表
 */
class GetTodayLearnedWordsCountUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 获取今日学习单词数量
     *
     * @return Flow<Result<Int>> 今日学习数量
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Result<Int>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val learningDay = DateTimeUtils.getLearningDay(resetHour)
            wordRepository.getTodayLearnedWords(learningDay)
                .map { words -> words.size }
        }.asResult()
    }
}
