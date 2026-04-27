package com.jian.nemo.core.domain.usecase.grammar

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * 恢复封禁(跳过)的语法 Use Case
 */
class RecoverLeechGrammarUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(grammarId: Int): Result<Unit> {
        return try {
            val grammar = grammarRepository.getGrammarById(grammarId).firstOrNull()
                ?: return Result.Error(Exception("语法不存在: $grammarId"))

            val updatedGrammar = grammar.copy(isSkipped = false)
            grammarRepository.updateGrammar(updatedGrammar)

            // 重置错误计数，给它一个新机会
            settingsRepository.resetGrammarLapse(grammarId)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
