package com.jian.nemo.core.domain.usecase.grammar

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import javax.inject.Inject

/**
 * 更新语法 UseCase
 * 主要用于更新 SRS 状态（间隔、重复次数、下次复习时间等）
 */
class UpdateGrammarUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository
) {
    suspend operator fun invoke(grammar: Grammar): Result<Unit> {
        return grammarRepository.updateGrammar(grammar)
    }
}
