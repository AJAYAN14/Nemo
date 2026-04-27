package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.ext.asResult
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * 获取今日学习单词 Use Case
 *
 * 业务规则:
 * 1. 返回今日首次学习的单词 (firstLearnedDate = today)
 * 2. 这里的“今日”遵循设置重置时间
 */
class GetTodayLearnedWordsUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 获取今日首次学习的单词
     *
     * @return Flow<Result<List<Word>>> 今日学习单词列表
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Result<List<Word>>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val today = DateTimeUtils.getLearningDay(resetHour)
            wordRepository.getTodayLearnedWords(today)
        }.asResult()
    }
}
