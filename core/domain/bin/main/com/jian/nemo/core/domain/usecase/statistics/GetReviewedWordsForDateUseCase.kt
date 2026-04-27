package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取指定日期复习过的单词
 */
class GetReviewedWordsForDateUseCase @Inject constructor(
    private val wordRepository: WordRepository
) {
    operator fun invoke(date: Long): Flow<List<Word>> {
        return wordRepository.getTodayReviewedWords(date)
    }
}
