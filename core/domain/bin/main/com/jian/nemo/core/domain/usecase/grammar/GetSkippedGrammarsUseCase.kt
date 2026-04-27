package com.jian.nemo.core.domain.usecase.grammar

import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有封禁(跳过)的语法 Use Case
 */
class GetSkippedGrammarsUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository
) {
    operator fun invoke(limit: Int = Int.MAX_VALUE): Flow<List<Grammar>> {
        return grammarRepository.getSkippedGrammars(limit)
    }
}
