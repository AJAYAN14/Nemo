package com.jian.nemo.core.domain.usecase.word

import app.cash.turbine.test
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 单元测试: GetDueWordsUseCase
 *
 * 验证到期复习单词的业务逻辑
 */
class GetDueWordsUseCaseTest {

    private lateinit var wordRepository: WordRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: GetDueWordsUseCase

    @Before
    fun setup() {
        wordRepository = mockk()
        settingsRepository = mockk()
        useCase = GetDueWordsUseCase(wordRepository, settingsRepository)

        // Mock DateTimeUtils
        mockkObject(DateTimeUtils)
        every { settingsRepository.learningDayResetHourFlow } returns flowOf(4)
        every { DateTimeUtils.getLearningDay(4) } returns 100L
    }

    @Test
    fun `invoke should return words sorted by nextReviewDate`() = runTest {
        // Given: 创建3个到期单词,但顺序是乱的
        val word1 = createTestWord(id = 1, nextReviewDate = 95L) // 最早
        val word2 = createTestWord(id = 2, nextReviewDate = 98L) // 中间
        val word3 = createTestWord(id = 3, nextReviewDate = 100L) // 今天到期
        val unsortedWords = listOf(word2, word3, word1) // 乱序

        every { wordRepository.getDueWords(100L) } returns flowOf(unsortedWords)

        // When & Then
        useCase().test {
            awaitItem() // Loading

            val result = awaitItem()
            assertTrue(result is Result.Success)

            val sortedWords = result.getOrNull()!!
            // 验证排序：按nextReviewDate升序
            assertEquals(word1.id, sortedWords[0].id)
            assertEquals(word2.id, sortedWords[1].id)
            assertEquals(word3.id, sortedWords[2].id)

            awaitComplete()
        }
    }

    @Test
    fun `invoke should return empty list when no due words`() = runTest {
        // Given
        every { wordRepository.getDueWords(100L) } returns flowOf(emptyList())

        // When & Then
        useCase().test {
            awaitItem() // Loading

            val result = awaitItem()
            assertTrue(result is Result.Success)
            assertTrue(result.getOrNull()?.isEmpty() == true)

            awaitComplete()
        }
    }

    private fun createTestWord(
        id: Int = 1,
        nextReviewDate: Long = 100L,
        repetitionCount: Int = 1
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
        interval = 7,
        nextReviewDate = nextReviewDate,
        lastReviewedDate = 93L,
        firstLearnedDate = 90L,
        isFavorite = false,
        isSkipped = false,
        lastModifiedTime = System.currentTimeMillis()
    )
}
