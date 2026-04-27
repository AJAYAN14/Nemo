package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取指定日期学习的语法
 */
class GetLearnedGrammarsForDateUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository
) {
    operator fun invoke(date: Long): Flow<List<Grammar>> {
        return grammarRepository.getTodayLearnedGrammars(date)
    }
}
