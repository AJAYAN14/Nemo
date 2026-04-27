package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * 切换收藏状态 Use Case
 *
 * 业务逻辑:
 * 1. 获取当前收藏状态
 * 2. 切换状态
 * 3. 更新Repository
 * 4. 返回新状态
 */
class ToggleWordFavoriteUseCase @Inject constructor(
    private val wordRepository: WordRepository
) {
    /**
     * 切换单词收藏状态
     *
     * @param wordId 单词ID
     * @return Result<Boolean> 新的收藏状态(true=已收藏, false=未收藏)
     */
    suspend operator fun invoke(wordId: Int): Result<Boolean> {
        return try {
            // 1. 获取单词当前收藏状态
            val word = wordRepository.getWordById(wordId).firstOrNull()
                ?: return Result.Error(
                    IllegalArgumentException("单词不存在: wordId=$wordId")
                )

            // 2. 切换收藏状态
            val newStatus = !word.isFavorite

            // 3. 更新数据库
            wordRepository.updateFavoriteStatus(wordId, newStatus)

            // 4. 返回新状态
            Result.Success(newStatus)

        } catch (e: Exception) {
            // TODO: 使用结构化日志框架记录异常
            Result.Error(e)
        }
    }
}
