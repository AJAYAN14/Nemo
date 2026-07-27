package com.jian.nemo.core.domain.usecase.grammar

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.ext.asResult
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 获取处于学习中/重学中（未毕业）的语法 Use Case
 *
 * 业务规则:
 * 1. 筛选状态为 Learning (type=1) 或 Relearning (type=3) 的语法
 * 2. 排除今日被搁置的卡片 (buriedUntilDay == today)
 */
class GetLearningGrammarsUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Result<List<Grammar>>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val today = DateTimeUtils.getLearningDay(resetHour)
            grammarRepository.getLearningGrammars()
                .map { grammars ->
                    grammars.filter { it.buriedUntilDay != today }
                }
        }.asResult()
    }
}
