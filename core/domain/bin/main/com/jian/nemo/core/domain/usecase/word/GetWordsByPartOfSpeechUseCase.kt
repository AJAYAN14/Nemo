package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import javax.inject.Inject

/**
 * 根据词性获取单词列表
 *
 * 用于词性分类浏览功能
 */
class GetWordsByPartOfSpeechUseCase @Inject constructor(
    private val wordRepository: WordRepository
) {
    /**
     * 获取指定词性的所有单词
     *
     * @param partOfSpeech 词性类型
     * @return 单词列表（按ID排序）
     */
    suspend operator fun invoke(partOfSpeech: PartOfSpeech): List<Word> {
        return wordRepository.getWordsByPartOfSpeech(partOfSpeech)
    }
}
