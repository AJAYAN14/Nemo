package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.SrsUpdateResult
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 单元测试: MasterWordUseCase
 *
 * 验证掌握单词的复杂业务逻辑:
 * - SRS算法调用
 * - 学习记录统计
 * - 首次学习 vs 复习区分
 *
 * 参考: 旧项目 MasterWordUseCase.kt 测试要求
 */
class MasterWordUseCaseTest {

    private lateinit var wordRepository: WordRepository
    private lateinit var srsCalculator: SrsCalculator
    private lateinit var studyRecordRepository: StudyRecordRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: MasterWordUseCase

    @Before
    fun setup() {
        wordRepository = mockk()
        srsCalculator = mockk()
        studyRecordRepository = mockk()
        settingsRepository = mockk()
        useCase = MasterWordUseCase(wordRepository, srsCalculator, studyRecordRepository, settingsRepository)

        // Mock DateTimeUtils
        mockkObject(DateTimeUtils)
        every { settingsRepository.learningDayResetHourFlow } returns flowOf(4)
        every { DateTimeUtils.getLearningDay(4) } returns 100L
    }

    @Test
    fun `first time learning should increment learned words count`() = runTest {
        // Given: 未学习的单词 (repetitionCount = 0)
        val word = createTestWord(repetitionCount = 0)
        every { wordRepository.getWordById(1) } returns flowOf(word)

        val srsResult = SrsUpdateResult(
            repetitionCount = 1,
            stability = 2.5f,
            difficulty = 5.0f,
            interval = 1,
            nextReviewDate = 101L,
            lastReviewedDate = 100L,
            firstLearnedDate = 100L
        )
        every { srsCalculator.calculate(any(), 3, 100L) } returns srsResult

        coEvery { wordRepository.updateWord(any()) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementLearnedWords(any()) } returns Result.Success(Unit)

        // When
        val result = useCase(wordId = 1, quality = 3)

        // Then
        assertTrue(result is Result.Success)

        // 验证: 首次学习调用incrementLearnedWords
        coVerify(exactly = 1) { studyRecordRepository.incrementLearnedWords(any()) }
        coVerify(exactly = 0) { studyRecordRepository.incrementReviewedWords(any()) }
    }

    @Test
    fun `reviewing learned word should increment reviewed words count`() = runTest {
        // Given: 已学习的单词 (repetitionCount > 0)
        val word = createTestWord(repetitionCount = 2)
        every { wordRepository.getWordById(1) } returns flowOf(word)

        val srsResult = SrsUpdateResult(
            repetitionCount = 3,
            stability = 2.6f,
            difficulty = 5.0f,
            interval = 7,
            nextReviewDate = 107L,
            lastReviewedDate = 100L,
            firstLearnedDate = 90L
        )
        every { srsCalculator.calculate(any(), 3, 100L) } returns srsResult

        coEvery { wordRepository.updateWord(any()) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementReviewedWords(any()) } returns Result.Success(Unit)

        // When
        val result = useCase(wordId = 1, quality = 3)

        // Then
        assertTrue(result is Result.Success)

        // 验证: 复习调用incrementReviewedWords
        coVerify(exactly = 0) { studyRecordRepository.incrementLearnedWords(any()) }
        coVerify(exactly = 1) { studyRecordRepository.incrementReviewedWords(any()) }
    }

    @Test
    fun `should remove skip flag when master word`() = runTest {
        // Given: 跳过的单词
        val skippedWord = createTestWord(isSkipped = true, repetitionCount = 0)
        every { wordRepository.getWordById(1) } returns flowOf(skippedWord)

        val srsResult = SrsUpdateResult(
            repetitionCount = 1,
            stability = 2.5f,
            difficulty = 5.0f,
            interval = 1,
            nextReviewDate = 101L,
            lastReviewedDate = 100L,
            firstLearnedDate = 100L
        )
        every { srsCalculator.calculate(any(), 3, 100L) } returns srsResult

        val updatedWordSlot = slot<Word>()
        coEvery { wordRepository.updateWord(capture(updatedWordSlot)) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementLearnedWords(any()) } returns Result.Success(Unit)

        // When
        useCase(wordId = 1, quality = 3)

        // Then: 验证isSkipped被设置为false
        assertFalse(updatedWordSlot.captured.isSkipped)
    }

    @Test
    fun `should return error when word not found`() = runTest {
        // Given: 单词不存在
        every { wordRepository.getWordById(999) } returns flowOf(null)

        // When
        val result = useCase(wordId = 999, quality = 3)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `should pass quality parameter to SRS calculator`() = runTest {
        // Given
        val word = createTestWord()
        every { wordRepository.getWordById(1) } returns flowOf(word)

        val srsResult = SrsUpdateResult(
            repetitionCount = 1,
            stability = 2.7f,
            difficulty = 4.0f,
            interval = 1,
            nextReviewDate = 101L,
            lastReviewedDate = 100L,
            firstLearnedDate = 100L
        )

        val qualitySlot = slot<Int>()
        every {
            srsCalculator.calculate(any(), capture(qualitySlot), 100L)
        } returns srsResult

        coEvery { wordRepository.updateWord(any()) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementLearnedWords(any()) } returns Result.Success(Unit)

        // When: 使用quality=5(完全掌握)
        useCase(wordId = 1, quality = 5)

        // Then: 验证传递了正确的quality
        assertEquals(5, qualitySlot.captured)
    }

    private fun createTestWord(
        id: Int = 1,
        repetitionCount: Int = 0,
        isSkipped: Boolean = false
    ) = Word(
        id = id,
        japanese = "テスト",
        hiragana = "てすと",
        chinese = "测试",
        level = "n5",
        pos = null,
        example1 = null,
        gloss1 = null,
        example2 = null,
        gloss2 = null,
        example3 = null,
        gloss3 = null,
        repetitionCount = repetitionCount,
        stability = 2.5f,
        difficulty = 5.0f,
        interval = 0,
        nextReviewDate = 0,
        lastReviewedDate = null,
        firstLearnedDate = null,
        isFavorite = false,
        isSkipped = isSkipped,
        lastModifiedTime = System.currentTimeMillis()
    )
}
