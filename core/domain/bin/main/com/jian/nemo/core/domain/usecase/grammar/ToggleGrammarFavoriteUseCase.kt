package com.jian.nemo.core.domain.usecase.grammar

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.GrammarRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 切换语法收藏状态 Use Case
 *
 * 参考: ToggleWordFavoriteUseCase
 */
class ToggleGrammarFavoriteUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository
) {
    suspend operator fun invoke(grammarId: Int): Result<Unit> {
        return try {
            val grammar = grammarRepository.getGrammarById(grammarId).first()
                ?: return Result.Error(IllegalArgumentException("语法不存在: grammarId=$grammarId"))

            grammarRepository.updateFavoriteStatus(
                grammarId = grammarId,
                isFavorite = !grammar.isFavorite
            )
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
