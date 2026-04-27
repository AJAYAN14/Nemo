package com.jian.nemo.core.domain.usecase.test

import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.domain.model.TestResult
import com.jian.nemo.core.domain.model.WrongAnswer
import com.jian.nemo.core.domain.model.GrammarWrongAnswer
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.common.util.DateTimeUtils
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * 提交测试结果 Use Case
 *
 * 将错题记录到错题本
 *
 * 参考: 旧项目 QuestionLogic.kt submitAnswer()
 * 参考: 阶段11文档
 */
class SubmitTestResultUseCase @Inject constructor(
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val grammarWrongAnswerRepository: GrammarWrongAnswerRepository,
    private val settingsRepository: com.jian.nemo.core.domain.repository.SettingsRepository
) {
    /**
     * 提交测试结果
     *
     * @param result 测试结果
     */
    suspend operator fun invoke(result: TestResult) {
        val resetHour = settingsRepository.learningDayResetHourFlow.firstOrNull() ?: 4
        val today = DateTimeUtils.getLearningDay(resetHour)

        // 获取错题移除阈值
        val threshold = settingsRepository.testWrongAnswerRemovalThresholdFlow.firstOrNull() ?: 0

        // 遍历所有题目，分别处理正确和错误的题目
        // TestResult.questions 包含了所有的题目（无论对错）
        result.questions.forEach { question ->
            val userAnswerText = when (question) {
                is TestQuestion.MultipleChoice -> question.userAnswer ?: ""
                is TestQuestion.Typing -> question.userAnswer
                is TestQuestion.CardMatching -> "" // 卡片题没有用户输入文本
                is TestQuestion.Sorting -> question.userAnswer.joinToString("") { it.char.toString() }
            }

            val testMode = when (question) {
                is TestQuestion.MultipleChoice -> "multiple_choice"
                is TestQuestion.Typing -> "typing"
                is TestQuestion.CardMatching -> "card_matching"
                is TestQuestion.Sorting -> "sorting"
            }

            // 区分单词和语法
            if (question.grammar != null) {
                val grammarId = question.grammar!!.id

                if (question.isCorrect) {
                    // 答对: 检查是否在错题本中
                    val existingMistake = grammarWrongAnswerRepository.getWrongAnswerByGrammarIdSync(grammarId)
                    if (existingMistake != null) {
                        // 在错题本中
                        if (threshold > 0) {
                            val newCount = existingMistake.consecutiveCorrectCount + 1
                            if (newCount >= threshold) {
                                // 达到阈值，移除
                                grammarWrongAnswerRepository.deleteByGrammarId(grammarId)
                                println("✅ 语法错题已移除: grammarId=$grammarId (连续答对 $newCount 次)")
                            } else {
                                // 未达阈值，更新计数
                                val updatedMistake = existingMistake.copy(
                                    consecutiveCorrectCount = newCount,
                                    timestamp = today // 更新时间戳
                                )
                                grammarWrongAnswerRepository.insertWrongAnswer(updatedMistake)
                                println("📝 语法错题更新: grammarId=$grammarId (连续答对 $newCount/$threshold 次)")
                            }
                        }
                    }
                } else {
                    // 答错: 插入或更新 (重置计数为0)

                    // 获取题目快照信息
                    val qText = when (question) {
                        is TestQuestion.MultipleChoice -> question.questionText
                        is TestQuestion.Sorting -> question.word.hiragana // 排序题通常是拼写题目
                        else -> ""
                    }
                    val options = when (question) {
                        is TestQuestion.MultipleChoice -> question.options
                        is TestQuestion.Sorting -> question.options.map { it.char.toString() }
                        else -> emptyList()
                    }
                    val explanation = when (question) {
                        is TestQuestion.MultipleChoice -> question.explanation
                        else -> null
                    }

                    val grammarWrongAnswer = GrammarWrongAnswer(
                        id = 0, // Insert 会自动处理 ID
                        grammarId = grammarId,
                        grammar = null,
                        testMode = testMode,
                        userAnswer = userAnswerText,
                        correctAnswer = question.correctAnswer,
                        questionType = testMode,
                        questionText = qText,
                        options = options,
                        explanation = explanation,
                        timestamp = today,
                        consecutiveCorrectCount = 0 // 重置
                    )
                    grammarWrongAnswerRepository.insertWrongAnswer(grammarWrongAnswer)
                }

            } else if (question.word != null) {
                val wordId = question.word!!.id

                if (question.isCorrect) {
                    // 答对: 检查是否在错题本中
                    val existingMistake = wrongAnswerRepository.getWrongAnswerByWordIdSync(wordId)
                    if (existingMistake != null) {
                        // 在错题本中
                        if (threshold > 0) {
                            val newCount = existingMistake.consecutiveCorrectCount + 1
                            if (newCount >= threshold) {
                                // 达到阈值，移除
                                wrongAnswerRepository.deleteByWordId(wordId)
                                println("✅ 单词错题已移除: wordId=$wordId (连续答对 $newCount 次)")
                            } else {
                                // 未达阈值，更新计数
                                val updatedMistake = existingMistake.copy(
                                    consecutiveCorrectCount = newCount,
                                    timestamp = today
                                )
                                wrongAnswerRepository.insertWrongAnswer(updatedMistake)
                                println("📝 单词错题更新: wordId=$wordId (连续答对 $newCount/$threshold 次)")
                            }
                        }
                    }
                } else {
                    // 答错: 插入或更新 (重置计数为0)
                    val wrongAnswer = WrongAnswer(
                        id = 0,
                        wordId = wordId,
                        word = null,
                        testMode = testMode,
                        userAnswer = userAnswerText,
                        correctAnswer = question.correctAnswer,
                        timestamp = today,
                        consecutiveCorrectCount = 0 // 重置
                    )
                    wrongAnswerRepository.insertWrongAnswer(wrongAnswer)
                }
            }
        }
    }
}
