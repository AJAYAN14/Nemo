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
 * 获取新语法 Use Case
 *
 * 业务规则:
 * 1. 只返回未学习（repetitionCount = 0）的语法
 * 2. 排除已跳过的语法
 * 3. 排除今日被搁置的语法（这里的“今日”遵循设置重置时间）
 * 4. 按 ID 排序
 */
class GetNewGrammarsUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(level: String, isRandom: Boolean = false): Flow<Result<List<Grammar>>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val today = DateTimeUtils.getLearningDay(resetHour)
            grammarRepository.getNewGrammars(level, isRandom)
                .map { grammars ->
                    // 排除今日被搁置的卡片 (buriedUntilDay != today)
                    grammars.filter { it.buriedUntilDay != today }
                }
        }.asResult()
    }
}
