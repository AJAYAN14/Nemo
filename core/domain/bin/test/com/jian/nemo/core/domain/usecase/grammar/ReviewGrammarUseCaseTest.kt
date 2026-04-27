package com.jian.nemo.core.domain.usecase.grammar

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.GrammarExample
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarUsage
import com.jian.nemo.core.domain.model.SrsUpdateResult
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 单元测试: ReviewGrammarUseCase
 *
 * 验证复习语法的业务逻辑
 */
class ReviewGrammarUseCaseTest {

    private lateinit var grammarRepository: GrammarRepository
    private lateinit var srsCalculator: SrsCalculator
    private lateinit var studyRecordRepository: StudyRecordRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: ReviewGrammarUseCase

    @Before
    fun setup() {
        grammarRepository = mockk()
        srsCalculator = mockk()
        studyRecordRepository = mockk()
        settingsRepository = mockk()
        useCase = ReviewGrammarUseCase(grammarRepository, srsCalculator, studyRecordRepository, settingsRepository)

        // Mock DateTimeUtils
        mockkObject(DateTimeUtils)
        every { settingsRepository.learningDayResetHourFlow } returns flowOf(4)
        every { DateTimeUtils.getLearningDay(4) } returns 100L
    }

    @Test
    fun `should update grammar with SRS calculation result`() = runTest {
        // Given: 已学习的语法
        val grammar = createTestGrammar(repetitionCount = 2, stability = 2.5f)
        every { grammarRepository.getGrammarById(1) } returns flowOf(grammar)

        val srsResult = SrsUpdateResult(
            repetitionCount = 3,
            stability = 2.6f,
            difficulty = 5.0f,
            interval = 7,
            nextReviewDate = 107L,
            lastReviewedDate = 100L,
            firstLearnedDate = 90L
        )
        every { srsCalculator.calculate(any(), 4, 100L) } returns srsResult

        val updatedGrammarSlot = slot<Grammar>()
        coEvery { grammarRepository.updateGrammar(capture(updatedGrammarSlot)) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementReviewedGrammars() } returns Result.Success(Unit)

        // When
        val result = useCase(grammarId = 1, quality = 4)

        // Then
        assertTrue(result is Result.Success)
        val updatedGrammar = updatedGrammarSlot.captured
        assertEquals(3, updatedGrammar.repetitionCount)
        assertEquals(2.6f, updatedGrammar.stability, 0.01f)
        assertEquals(5.0f, updatedGrammar.difficulty, 0.01f)
        assertEquals(7, updatedGrammar.interval)
        assertEquals(107L, updatedGrammar.nextReviewDate)
    }

    @Test
    fun `should increment reviewed grammars count`() = runTest {
        // Given
        val grammar = createTestGrammar(repetitionCount = 1)
        every { grammarRepository.getGrammarById(1) } returns flowOf(grammar)

        val srsResult = SrsUpdateResult(
            repetitionCount = 2,
            stability = 2.5f,
            difficulty = 5.0f,
            interval = 3,
            nextReviewDate = 103L,
            lastReviewedDate = 100L,
            firstLearnedDate = 95L
        )
        every { srsCalculator.calculate(any(), 3, 100L) } returns srsResult

        coEvery { grammarRepository.updateGrammar(any()) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementReviewedGrammars() } returns Result.Success(Unit)

        // When
        useCase(grammarId = 1, quality = 3)

        // Then: 验证复习计数增加
        coVerify(exactly = 1) { studyRecordRepository.incrementReviewedGrammars() }
        coVerify(exactly = 0) { studyRecordRepository.incrementLearnedGrammars() }
    }

    @Test
    fun `should return error when grammar not found`() = runTest {
        // Given
        every { grammarRepository.getGrammarById(999) } returns flowOf(null)

        // When
        val result = useCase(grammarId = 999, quality = 3)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should handle quality parameter correctly`() = runTest {
        // Given
        val grammar = createTestGrammar(repetitionCount = 1)
        every { grammarRepository.getGrammarById(1) } returns flowOf(grammar)

        val qualitySlot = slot<Int>()
        val srsResult = SrsUpdateResult(
            repetitionCount = 2,
            stability = 2.7f,
            difficulty = 4.0f,
            interval = 3,
            nextReviewDate = 103L,
            lastReviewedDate = 100L,
            firstLearnedDate = 95L
        )
        every { srsCalculator.calculate(any(), capture(qualitySlot), 100L) } returns srsResult

        coEvery { grammarRepository.updateGrammar(any()) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementReviewedGrammars() } returns Result.Success(Unit)

        // When: 使用高质量评分
        useCase(grammarId = 1, quality = 5)

        // Then
        assertEquals(5, qualitySlot.captured)
    }

    private fun createTestGrammar(
        id: Int = 1,
        repetitionCount: Int = 1,
        stability: Float = 2.5f,
        difficulty: Float = 5.0f
    ) = Grammar(
        id = id,
        grammar = "～について",
        grammarLevel = "n5",
        usages = listOf(
            GrammarUsage(
                subtype = null,
                connection = "名詞＋について",
                explanation = "关于～",
                notes = null,
                examples = listOf(
                    GrammarExample(
                        sentence = "日本の文化について勉強します。",
                        translation = "学习关于日本的文化。",
                        source = null,
                        isDialog = false
                    )
                )
            )
        ),
        repetitionCount = repetitionCount,
        stability = stability,
        difficulty = difficulty,
        interval = 1,
        nextReviewDate = 100L,
        lastReviewedDate = 95L,
        firstLearnedDate = 90L,
        isFavorite = false,
        isSkipped = false,
        lastModifiedTime = System.currentTimeMillis()
    )
}
