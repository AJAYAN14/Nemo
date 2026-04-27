package com.jian.nemo.core.domain.usecase.grammar

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.ext.asResult
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 获取今日学习语法数量 Use Case
 *
 * 这里的“今日”遵循设置中的重置时间（例如凌晨4点之前仍算作前一天）
 */
class GetTodayLearnedGrammarsCountUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 获取今日学习语法数量
     *
     * @return Flow<Result<Int>> 今日学习数量
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Result<Int>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val learningDay = DateTimeUtils.getLearningDay(resetHour)
            grammarRepository.getTodayLearnedGrammars(learningDay)
                .map { grammars -> grammars.size }
        }.asResult()
    }
}
