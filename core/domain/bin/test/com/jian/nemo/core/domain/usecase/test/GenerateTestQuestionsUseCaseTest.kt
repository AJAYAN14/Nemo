package com.jian.nemo.core.domain.usecase.test

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.factory.TestQuestionFactory
import com.jian.nemo.core.domain.model.QuestionType
import com.jian.nemo.core.domain.model.TestMode
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.domain.model.ExplanationPayload
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarUsage
import com.jian.nemo.core.domain.model.GrammarExample
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.GrammarTestRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 单元测试: GenerateTestQuestionsUseCase
 *
 * 验证测试题目生成的业务逻辑
 */
class GenerateTestQuestionsUseCaseTest {

    private lateinit var wordRepository: WordRepository
    private lateinit var grammarRepository: com.jian.nemo.core.domain.repository.GrammarRepository
    private lateinit var grammarTestRepository: GrammarTestRepository
    private lateinit var generateCardMatchingQuestionsUseCase: GenerateCardMatchingQuestionsUseCase
    private lateinit var wrongAnswerRepository: WrongAnswerRepository
    private lateinit var grammarWrongAnswerRepository: GrammarWrongAnswerRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var testQuestionFactory: TestQuestionFactory
    private lateinit var useCase: GenerateTestQuestionsUseCase

    @Before
    fun setup() {
        wordRepository = mockk()
        grammarRepository = mockk()
        grammarTestRepository = mockk()
        wrongAnswerRepository = mockk()
        grammarWrongAnswerRepository = mockk()
        settingsRepository = mockk()
        testQuestionFactory = TestQuestionFactory()
        generateCardMatchingQuestionsUseCase = GenerateCardMatchingQuestionsUseCase(wordRepository)
        useCase = GenerateTestQuestionsUseCase(
            wordRepository = wordRepository,
            grammarRepository = grammarRepository,
            grammarTestRepository = grammarTestRepository,
            generateCardMatchingQuestionsUseCase = generateCardMatchingQuestionsUseCase,
            wrongAnswerRepository = wrongAnswerRepository,
            grammarWrongAnswerRepository = grammarWrongAnswerRepository,
            settingsRepository = settingsRepository,
            testQuestionFactory = testQuestionFactory
        )

        // Mock DateTimeUtils
        mockkObject(DateTimeUtils)
        every { settingsRepository.learningDayResetHourFlow } returns flowOf(4)
        every { DateTimeUtils.getLearningDay(4) } returns 100L
        every { wordRepository.getAllWordsByLevel(any()) } answers {
            flowOf(createTestWords(20))
        }
        coEvery { grammarTestRepository.loadQuestionsByLevel(any()) } returns Result.Success(emptyList())
        coEvery { wrongAnswerRepository.getAllWrongWordIds() } returns emptyList()
        coEvery { grammarWrongAnswerRepository.getAllWrongGrammarIds() } returns emptyList()
    }

    @Test
    fun `should generate correct number of multiple choice questions`() = runTest {
        // Given: 今天学习了10个单词
        val learnedWords = createTestWords(10)
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(learnedWords)

        // When: 生成5道题
        val questions = useCase(
            level = "n5",
            mode = TestMode.JP_TO_CN,
            count = 5,
            questionType = QuestionType.MULTIPLE_CHOICE
        )

        // Then
        assertEquals(5, questions.size)
        assertTrue(questions.all { it is TestQuestion.MultipleChoice })
    }

    @Test
    fun `should return empty list when no learned words available`() = runTest {
        // Given: 没有学习过的单词
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(emptyList())

        // When
        val questions = useCase(
            level = "n5",
            mode = TestMode.JP_TO_CN,
            count = 10
        )

        // Then
        assertTrue(questions.isEmpty())
    }

    @Test
    fun `should limit questions to available words count`() = runTest {
        // Given: 只有3个学习过的单词
        val learnedWords = createTestWords(3)
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(learnedWords)

        // When: 请求10道题
        val questions = useCase(
            level = "n5",
            mode = TestMode.CN_TO_JP,
            count = 10
        )

        // Then: 只能生成3道题
        assertEquals(3, questions.size)
    }

    @Test
    fun `multiple choice question should have 4 options`() = runTest {
        // Given: 足够的单词生成选项
        val learnedWords = createTestWords(20)
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(learnedWords)

        // When
        val questions = useCase(
            level = "n5",
            mode = TestMode.JP_TO_CN,
            count = 5,
            questionType = QuestionType.MULTIPLE_CHOICE
        )

        // Then: 每道题应该有4个选项
        questions.forEach { question ->
            assertTrue(question is TestQuestion.MultipleChoice)
            val mcQuestion = question as TestQuestion.MultipleChoice
            assertEquals(4, mcQuestion.options.size)
            assertTrue(mcQuestion.options.contains(mcQuestion.correctAnswer))
        }
    }

    @Test
    fun `should generate typing questions with correct format`() = runTest {
        // Given
        val learnedWords = createTestWords(5)
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(learnedWords)

        // When: 生成打字题
        val questions = useCase(
            level = "n5",
            mode = TestMode.KANA,
            count = 5,
            questionType = QuestionType.TYPING
        )

        // Then
        assertEquals(5, questions.size)
        assertTrue(questions.all { it is TestQuestion.Typing })

        questions.forEach { question ->
            val typingQuestion = question as TestQuestion.Typing
            assertTrue(
                typingQuestion.questionText in setOf(
                    typingQuestion.word.chinese,
                    typingQuestion.word.japanese,
                    typingQuestion.word.hiragana
                )
            )
            assertTrue(
                typingQuestion.correctAnswer in setOf(
                    typingQuestion.word.chinese,
                    typingQuestion.word.japanese,
                    typingQuestion.word.hiragana
                )
            )
        }
    }

    @Test
    fun `JP_TO_CN mode should show japanese and answer chinese`() = runTest {
        // Given
        val word = createTestWord(1, "日本語", "にほんご", "日语")
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(listOf(word))

        // When
        val questions = useCase(
            level = "n5",
            mode = TestMode.JP_TO_CN,
            count = 1,
            questionType = QuestionType.MULTIPLE_CHOICE
        )

        // Then
        val question = questions.first() as TestQuestion.MultipleChoice
        assertEquals("日本語", question.questionText)
        assertEquals("日语", question.correctAnswer)
    }

    @Test
    fun `CN_TO_JP mode should show chinese and answer japanese`() = runTest {
        // Given
        val word = createTestWord(1, "単語", "たんご", "单词")
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(listOf(word))

        // When
        val questions = useCase(
            level = "n5",
            mode = TestMode.CN_TO_JP,
            count = 1,
            questionType = QuestionType.MULTIPLE_CHOICE
        )

        // Then
        val question = questions.first() as TestQuestion.MultipleChoice
        assertEquals("单词", question.questionText)
        assertEquals("単語", question.correctAnswer)
    }

    @Test
    fun `KANA mode should show japanese and answer hiragana`() = runTest {
        // Given
        val word = createTestWord(1, "勉強", "べんきょう", "学习")
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(listOf(word))

        // When
        val questions = useCase(
            level = "n5",
            mode = TestMode.KANA,
            count = 1,
            questionType = QuestionType.MULTIPLE_CHOICE
        )

        // Then
        val question = questions.first() as TestQuestion.MultipleChoice
        assertEquals("勉強", question.questionText)
        assertEquals("べんきょう", question.correctAnswer)
    }

    @Test
    fun `should not include duplicate words in questions`() = runTest {
        // Given
        val learnedWords = createTestWords(10)
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(learnedWords)

        // When
        val questions = useCase(
            level = "n5",
            mode = TestMode.JP_TO_CN,
            count = 5
        )

        // Then: 每道题的单词ID应该唯一
        val wordIds = questions.mapNotNull { it.word?.id }
        assertEquals(wordIds.distinct().size, wordIds.size)
    }

    @Test
    fun `word multiple choice should include unified word explanation payload`() = runTest {
        // Given
        val word = createTestWord(1, "日本語", "にほんご", "日语")
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(listOf(word))

        // When
        val questions = useCase(
            level = "n5",
            mode = TestMode.JP_TO_CN,
            count = 1,
            questionType = QuestionType.MULTIPLE_CHOICE
        )

        // Then
        val question = questions.first() as TestQuestion.MultipleChoice
        val payload = question.explanationPayload
        assertTrue(payload is ExplanationPayload.WordSummary)

        val wordPayload = payload as ExplanationPayload.WordSummary
        assertEquals("日本語", wordPayload.japanese)
        assertEquals("にほんご", wordPayload.hiragana)
        assertEquals("日语", wordPayload.meaning)
    }

    @Test
    fun `grammar multiple choice should include unified grammar explanation payload`() = runTest {
        // Given
        val grammar = createTestGrammar(
            id = 1,
            grammarText = "〜ている",
            explanation = "表示动作正在进行"
        )
        every { grammarRepository.getTodayLearnedGrammars(100L) } returns flowOf(listOf(grammar))
        every { grammarRepository.getGrammarsByLevels(any()) } returns flowOf(listOf(grammar))

        // When
        val questions = useCase(
            level = "N5",
            mode = TestMode.JP_TO_CN,
            count = 1,
            questionType = QuestionType.MULTIPLE_CHOICE,
            contentType = "grammar",
            source = "today"
        )

        // Then
        val question = questions.first() as TestQuestion.MultipleChoice
        val payload = question.explanationPayload
        assertTrue(payload is ExplanationPayload.GrammarText)
        assertEquals("表示动作正在进行", (payload as ExplanationPayload.GrammarText).text)
    }

    private fun createTestGrammar(
        id: Int,
        grammarText: String,
        explanation: String
    ): Grammar {
        return Grammar(
            id = id,
            grammar = grammarText,
            grammarLevel = "N5",
            usages = listOf(
                GrammarUsage(
                    subtype = null,
                    connection = "动词て形",
                    explanation = explanation,
                    notes = null,
                    examples = listOf(
                        GrammarExample(
                            sentence = "ご飯を食べています。",
                            translation = "正在吃饭。",
                            source = null,
                            isDialog = false
                        )
                    )
                )
            )
        )
    }

    private fun createTestWords(count: Int): List<Word> {
        return (1..count).map { id ->
            createTestWord(
                id = id,
                japanese = "単語$id",
                hiragana = "たんご$id",
                chinese = "单词$id"
            )
        }
    }

    private fun createTestWord(
        id: Int,
        japanese: String = "テスト",
        hiragana: String = "てすと",
        chinese: String = "测试"
    ) = Word(
        id = id,
        japanese = japanese,
        hiragana = hiragana,
        chinese = chinese,
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
