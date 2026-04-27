package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * 恢复封禁(跳过)的单词 Use Case
 *
 * 逻辑:
 * 1. 将 isSkipped 设为 false
 * 2. 重置 Settings 中的 Lapse 计数
 */
class RecoverLeechWordUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(wordId: Int): Result<Unit> {
        return try {
            val word = wordRepository.getWordById(wordId).firstOrNull()
                ?: return Result.Error(Exception("单词不存在: $wordId"))

            val updatedWord = word.copy(isSkipped = false)
            wordRepository.updateWord(updatedWord)

            // 重置错误计数，给它一个新机会
            settingsRepository.resetWordLapse(wordId)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
