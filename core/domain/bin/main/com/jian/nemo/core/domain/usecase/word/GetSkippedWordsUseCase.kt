package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有封禁(跳过)的单词 Use Case
 */
class GetSkippedWordsUseCase @Inject constructor(
    private val wordRepository: WordRepository
) {
    operator fun invoke(limit: Int = Int.MAX_VALUE): Flow<List<Word>> {
        return wordRepository.getSkippedWords(limit)
    }
}
