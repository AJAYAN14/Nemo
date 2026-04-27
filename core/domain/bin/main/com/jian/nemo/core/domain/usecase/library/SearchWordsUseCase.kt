package com.jian.nemo.core.domain.usecase.library

import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchWordsUseCase @Inject constructor(
    private val wordRepository: WordRepository
) {
    operator fun invoke(query: String): Flow<List<Word>> {
        return wordRepository.searchWords(query)
    }
}
