package com.jian.nemo.core.domain.usecase.test

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.TestMode
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.domain.model.TestResult
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.model.WrongAnswer
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 单元测试: SubmitTestResultUseCase
 *
 * 验证提交测试结果的业务逻辑
 */
class SubmitTestResultUseCaseTest {

    private lateinit var wrongAnswerRepository: WrongAnswerRepository
    private lateinit var grammarWrongAnswerRepository: GrammarWrongAnswerRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: SubmitTestResultUseCase

    @Before
    fun setup() {
        wrongAnswerRepository = mockk(relaxed = true)
        grammarWrongAnswerRepository = mockk<GrammarWrongAnswerRepository>(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        useCase = SubmitTestResultUseCase(wrongAnswerRepository, grammarWrongAnswerRepository, settingsRepository)

        // Mock DateTimeUtils
        mockkObject(DateTimeUtils)
        every { settingsRepository.learningDayResetHourFlow } returns kotlinx.coroutines.flow.flowOf(4)
        every { settingsRepository.testWrongAnswerRemovalThresholdFlow } returns kotlinx.coroutines.flow.flowOf(0)
        every { DateTimeUtils.getLearningDay(4) } returns 100L
    }

    @Test
    fun `should save all wrong answers when test has mistakes`() = runTest {
        // Given: 测试结果包含2个错题
        val wrongQuestion1 = createMultipleChoiceQuestion(
            id = 1,
            correctAnswer = "测试1",
            userAnswerIndex = 1,
            isCorrect = false
        )
        val wrongQuestion2 = createMultipleChoiceQuestion(
            id = 2,
            correctAnswer = "测试2",
            userAnswerIndex = 2,
            isCorrect = false
        )

        val testResult = TestResult(
            totalQuestions = 5,
            correctCount = 3,
            wrongCount = 2,
            accuracy = 0.6f,
            score = 60,
            timeSpentMs = 60000,
            wrongQuestions = listOf(wrongQuestion1, wrongQuestion2),
            questions = listOf(wrongQuestion1, wrongQuestion2) // Added questions including the wrong ones
        )

        // When
        useCase(testResult)

        // Then: 应该保存2个错题记录
        coVerify(exactly = 2) { wrongAnswerRepository.insertWrongAnswer(any()) }
    }

    @Test
    fun `should not save any wrong answers when test is perfect`() = runTest {
        // Given: 完美测试结果,没有错题
        val testResult = TestResult(
            totalQuestions = 10,
            correctCount = 10,
            wrongCount = 0,
            accuracy = 1.0f,
            score = 100,
            timeSpentMs = 120000,
            wrongQuestions = emptyList(),
            questions = List(10) {
                createMultipleChoiceQuestion(it, "Answer", null, true)
            }
        )

        // When
        useCase(testResult)

        // Then: 不应该保存任何错题
        coVerify(exactly = 0) { wrongAnswerRepository.insertWrongAnswer(any()) }
    }

    @Test
    fun `should save wrong answer with correct word id and user answer for multiple choice`() = runTest {
        // Given: 选择题错题
        val options = listOf("选项0", "选项1", "选项2", "正确答案")  // 不打乱,使用固定顺序
        val wrongQuestion = TestQuestion.MultipleChoice(
            id = 5,
            word = createTestWord(5),
            mode = TestMode.JP_TO_CN,
            questionText = "题目5",
            correctAnswer = "正确答案",
            options = options,
            isAnswered = true,
            isCorrect = false,
            userAnswerIndex = 2  // 用户选择了选项2(索引2)
        )

        val testResult = TestResult(
            totalQuestions = 1,
            correctCount = 0,
            wrongCount = 1,
            accuracy = 0f,
            score = 0,
            timeSpentMs = 10000,
            wrongQuestions = listOf(wrongQuestion),
            questions = listOf(wrongQuestion)
        )

        val savedWrongAnswer = slot<WrongAnswer>()
        coEvery { wrongAnswerRepository.insertWrongAnswer(capture(savedWrongAnswer)) } returns com.jian.nemo.core.common.Result.Success(Unit)

        // When
        useCase(testResult)

        // Then
        val captured = savedWrongAnswer.captured
        assertEquals(5, captured.wordId)
        assertEquals("选项2", captured.userAnswer)  // 用户答案应该是选项2
        assertEquals("正确答案", captured.correctAnswer)
        assertEquals("multiple_choice", captured.testMode)
        assertEquals(100L, captured.timestamp)
    }

    @Test
    fun `should save wrong answer for typing question`() = runTest {
        // Given: 打字题错题
        val wrongQuestion = TestQuestion.Typing(
            id = 3,
            word = createTestWord(3),
            questionText = "测试",
            correctAnswer = "てすと",
            isAnswered = true,
            isCorrect = false,
            userAnswer = "てすど" // 用户输错了
        )

        val testResult = TestResult(
            totalQuestions = 1,
            correctCount = 0,
            wrongCount = 1,
            accuracy = 0f,
            score = 0,
            timeSpentMs = 5000,
            wrongQuestions = listOf(wrongQuestion),
            questions = listOf(wrongQuestion)
        )

        val savedWrongAnswer = slot<WrongAnswer>()
        coEvery { wrongAnswerRepository.insertWrongAnswer(capture(savedWrongAnswer)) } returns com.jian.nemo.core.common.Result.Success(Unit)

        // When
        useCase(testResult)

        // Then
        val captured = savedWrongAnswer.captured
        assertEquals(3, captured.wordId)
        assertEquals("てすど", captured.userAnswer)
        assertEquals("てすと", captured.correctAnswer)
        assertEquals("typing", captured.testMode)
    }

    @Test
    fun `should handle multiple wrong questions with different types`() = runTest {
        // Given: 混合类型的错题
        val mcQuestion = createMultipleChoiceQuestion(
            id = 1,
            correctAnswer = "答案1",
            userAnswerIndex = 0,
            isCorrect = false
        )
        val typingQuestion = TestQuestion.Typing(
            id = 2,
            word = createTestWord(2),
            questionText = "题目2",
            correctAnswer = "正确2",
            isAnswered = true,
            isCorrect = false,
            userAnswer = "错误2"
        )

        val testResult = TestResult(
            totalQuestions = 5,
            correctCount = 3,
            wrongCount = 2,
            accuracy = 0.6f,
            score = 60,
            timeSpentMs = 30000,
            wrongQuestions = listOf(mcQuestion, typingQuestion),
            questions = listOf(mcQuestion, typingQuestion) // Should include correct questions too but for this test only wrong ones matter for verification
        )

        // When
        useCase(testResult)

        // Then: 两种类型的错题都应该被保存
        coVerify(exactly = 2) { wrongAnswerRepository.insertWrongAnswer(any()) }
    }

    @Test
    fun `should use today epoch day as timestamp`() = runTest {
        // Given
        every { DateTimeUtils.getLearningDay(4) } returns 200L

        val wrongQuestion = createMultipleChoiceQuestion(
            id = 1,
            correctAnswer = "答案",
            userAnswerIndex = 1,
            isCorrect = false
        )

        val testResult = TestResult(
            totalQuestions = 1,
            correctCount = 0,
            wrongCount = 1,
            accuracy = 0f,
            score = 0,
            timeSpentMs = 10000,
            wrongQuestions = listOf(wrongQuestion),
            questions = listOf(wrongQuestion)
        )

        val savedWrongAnswer = slot<WrongAnswer>()
        coEvery { wrongAnswerRepository.insertWrongAnswer(capture(savedWrongAnswer)) } returns com.jian.nemo.core.common.Result.Success(Unit)

        // When
        useCase(testResult)

        // Then: 应该使用当天的epoch day
        assertEquals(200L, savedWrongAnswer.captured.timestamp)
    }

    private fun createMultipleChoiceQuestion(
        id: Int,
        correctAnswer: String,
        userAnswerIndex: Int?,
        isCorrect: Boolean
    ): TestQuestion.MultipleChoice {
        val options = listOf("错误选项0", "错误选项1", "错误选项2", correctAnswer).shuffled()

        return TestQuestion.MultipleChoice(
            id = id,
            word = createTestWord(id),
            mode = TestMode.JP_TO_CN,
            questionText = "题目$id",
            correctAnswer = correctAnswer,
            options = options,
            isAnswered = true,
            isCorrect = isCorrect,
            userAnswerIndex = userAnswerIndex
        )
    }

    private fun createTestWord(id: Int) = Word(
        id = id,
        japanese = "単語$id",
        hiragana = "たんご$id",
        chinese = "单词$id",
        level = "n5",
        pos = null,
        example1 = null,
        gloss1 = null,
        example2 = null,
        gloss2 = null,
        example3 = null,
        gloss3 = null,
        repetitionCount = 1,
        stability = 2.5f,
        difficulty = 5.0f,
        interval = 1,
        nextReviewDate = 101L,
        lastReviewedDate = 100L,
        firstLearnedDate = 100L,
        isFavorite = false,
        isSkipped = false,
        lastModifiedTime = System.currentTimeMillis()
    )
}
