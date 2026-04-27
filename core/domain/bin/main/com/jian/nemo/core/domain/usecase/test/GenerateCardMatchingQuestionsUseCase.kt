package com.jian.nemo.core.domain.usecase.test

import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import javax.inject.Inject

/**
 * 生成卡片题UseCase
 *
 * 参考: 旧项目 TestManager.kt 行1108-1123
 *
 * 逻辑要点:
 * 1. 每5个单词为一组生成一道卡片题
 * 2. 如果配置要求打乱，先shuffled()再chunked(5)
 * 3. 返回 TestQuestion.CardMatching 列表
 */
class GenerateCardMatchingQuestionsUseCase @Inject constructor(
    private val wordRepository: WordRepository
) {
    /**
     * 生成卡片题
     *
     * @param words 单词列表
     * @param shuffle 是否打乱顺序
     * @return 卡片题列表
     */
    suspend operator fun invoke(
        words: List<Word>,
        shuffle: Boolean = true
    ): List<TestQuestion.CardMatching> {
        // 根据配置决定是否打乱顺序（参考旧项目行1110-1115）
        val processedWords = if (shuffle) {
            words.shuffled()
        } else {
            words
        }

        // 每5个单词为一组（参考旧项目行1117-1122）
        return processedWords.chunked(5).mapIndexed { index, wordGroup ->
            TestQuestion.CardMatching(
                id = index,
                pairs = wordGroup
            )
        }
    }
}
