package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取所有已学习的单词 (不包含跳过的)
 */
class GetAllLearnedWordsUseCase @Inject constructor(
    private val wordRepository: WordRepository
) {
    operator fun invoke(): Flow<List<Word>> {
        return wordRepository.getAllLearnedWords()
    }
}
