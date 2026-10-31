package com.jian.nemo.feature.test.data

import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.domain.model.Word

/**
 * 手打题生成器
 * 负责将单词数据转化为打字评测题型
 *

 *
 * 支持6种题型组合：
 *   1: 题目=释义, 答案=假名
 *   2: 题目=释义, 答案=汉字
 *   3: 题目=假名, 答案=汉字
 *   4: 题目=汉字, 答案=假名
 *   5: 题目=假名, 答案=释义
 *   6: 题目=汉字, 答案=释义
 */
class TypingQuestionGenerator {

    /**
     * 生成手打题列表
     * @param words 单词列表
     * @param count 生成题目数量
     * @return 手打题列表
     */
    fun generateQuestions(
        words: List<Word>,
        count: Int
    ): List<TestQuestion.Typing> {
        val questions = mutableListOf<TestQuestion.Typing>()
        val questionTypes = listOf(1, 2, 3, 4, 5, 6)

        // 打乱单词列表并取指定数量
        val shuffledWords = words.shuffled()

        for (word in shuffledWords.take(count)) {
            // 随机选择题型
            val questionType = questionTypes.random()

            // 根据题型获取题目文本和正确答案
            val (questionText, correctAnswer) = getQuestionData(word, questionType)

            questions.add(
                TestQuestion.Typing(
                    id = word.id,
                    word = word,
                    questionText = questionText,
                    correctAnswer = correctAnswer,
                    questionType = questionType
                )
            )
        }

        return questions
    }

    /**
     * 根据题型获取题目文本和正确答案

     *
     * @param word 单词对象
     * @param questionType 题型（1-6）
     * @return Pair<题目文本, 正确答案>
     */
    private fun getQuestionData(word: Word, questionType: Int): Pair<String, String> {
        return when (questionType) {
            1 -> {
                // 1. 题目：释义 要求输入：假名
                // 题目显示：chinese（释义）
                // 正确答案：hiragana（假名）
                word.chinese to word.hiragana
            }
            2 -> {
                // 2. 题目：释义 要求输入：汉字
                // 题目显示：chinese（释义）
                // 正确答案：japanese（汉字）
                word.chinese to word.japanese
            }
            3 -> {
                // 3. 题目：假名 要求输入：汉字
                // 题目显示：hiragana（假名）
                // 正确答案：japanese（汉字）
                word.hiragana to word.japanese
            }
            4 -> {
                // 4. 题目：汉字 要求输入：假名
                // 题目显示：japanese（汉字）
                // 正确答案：hiragana（假名）
                word.japanese to word.hiragana
            }
            5 -> {
                // 5. 题目：假名 要求输入：释义
                // 题目显示：hiragana（假名）
                // 正确答案：chinese（释义）
                word.hiragana to word.chinese
            }
            6 -> {
                // 6. 题目：汉字 要求输入：释义
                // 题目显示：japanese（汉字）
                // 正确答案：chinese（释义）
                word.japanese to word.chinese
            }
            else -> {
                // 默认使用题型1
                word.chinese to word.hiragana
            }
        }
    }
}
