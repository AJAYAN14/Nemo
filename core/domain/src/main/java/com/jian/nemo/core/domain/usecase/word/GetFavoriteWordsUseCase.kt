package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.ext.asResult
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取收藏单词 Use Case
 *
 * 业务规则:
 * 1. 返回所有收藏的单词 (isFavorite = true)
 */
class GetFavoriteWordsUseCase @Inject constructor(
    private val wordRepository: WordRepository
) {
    /**
     * 获取收藏的单词列表
     *
     * @return Flow<Result<List<Word>>> 收藏单词列表
     */
    operator fun invoke(): Flow<Result<List<Word>>> {
        return wordRepository.getFavoriteWords()
            .asResult()
    }
}
