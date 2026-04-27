package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * 获取今日学习的单词
 */
class GetTodayLearnedWordsUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Word>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val learningDay = DateTimeUtils.getLearningDay(resetHour)
            wordRepository.getTodayLearnedWords(learningDay)
        }
    }
}
