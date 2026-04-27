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
 * 获取到期复习语法 Use Case
 *
 * 业务规则:
 * 1. 筛选已学习的语法 (repetitionCount > 0)
 * 2. 筛选到期复习的语法 (nextReviewDate <= today)
 * 3. 按下次复习日期排序（最早的优先）
 * 4. 这里的“今日”遵循设置中的重置时间
 */
class GetDueGrammarsUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository
) {
    /**
     * 获取到期复习的语法
     *
     * @return Flow<Result<List<Grammar>>> 到期复习语法列表,按复习日期排序
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Result<List<Grammar>>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val today = DateTimeUtils.getLearningDay(resetHour)
            grammarRepository.getDueGrammars(today)
                .map { grammars ->
                    // 业务规则: 按下次复习日期排序（最早的优先）
                    // 排除今日被搁置的卡片 (buriedUntilDay != today)
                    grammars.filter { it.buriedUntilDay != today }
                        .sortedBy { it.nextReviewDate }
                }
        }.asResult()
    }
}
