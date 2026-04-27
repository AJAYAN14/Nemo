package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * 获取今日学习的语法
 */
class GetTodayLearnedGrammarsUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Grammar>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val learningDay = DateTimeUtils.getLearningDay(resetHour)
            grammarRepository.getTodayLearnedGrammars(learningDay)
        }
    }
}
