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
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 获取到期复习单词 Use Case
 *
 * 业务规则:
 * 1. 筛选已学习的单词 (repetitionCount > 0)
 * 2. 筛选到期复习的单词 (nextReviewDate <= today)
 * 3. 按下次复习日期排序（最早的优先）
 * 4. 这里的“今日”遵循设置中的重置时间
 */
class GetDueWordsUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 获取到期复习的单词
     *
     * @return Flow<Result<List<Word>>> 到期复习单词列表,按复习日期排序
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Result<List<Word>>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val today = DateTimeUtils.getLearningDay(resetHour)
            wordRepository.getDueWords(today)
                .map { words ->
                    // 业务规则: 按下次复习日期排序（最早的优先）
                    // 排除今日被搁置的卡片 (buriedUntilDay != today)
                    words.filter { it.buriedUntilDay != today }
                        .sortedBy { it.nextReviewDate }
                }
        }.asResult()
    }
}
