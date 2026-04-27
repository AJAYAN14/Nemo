package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有已学习的语法 (不包含跳过的)
 */
class GetAllLearnedGrammarsUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository
) {
    operator fun invoke(): Flow<List<Grammar>> {
        return grammarRepository.getAllLearnedGrammars()
    }
}
