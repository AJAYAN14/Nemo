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
 * 获取新单词 Use Case
 *
 * 业务规则:
 * 1. 只返回未学习（repetitionCount = 0）的单词
 * 2. 排除已跳过的单词
 * 3. 排除今日被搁置的单词（这里的“今日”遵循设置重置时间）
 * 4. 按 ID 排序（确保顺序一致）
 */
class GetNewWordsUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 获取指定等级的新单词
     *
     * @param level JLPT等级 (n1, n2, n3, n4, n5)
     * @return Flow<Result<List<Word>>> 新单词列表(Loading -> Success/Error)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(level: String, isRandom: Boolean = false): Flow<Result<List<Word>>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val today = DateTimeUtils.getLearningDay(resetHour)
            wordRepository.getNewWords(level, isRandom)
                .map { words ->
                    // 排除今日被搁置的卡片 (buriedUntilDay != today)
                    words.filter { it.buriedUntilDay != today }
                }
        }.asResult()
    }
}
