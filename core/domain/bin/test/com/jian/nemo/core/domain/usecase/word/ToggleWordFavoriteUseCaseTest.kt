package com.jian.nemo.core.domain.usecase.word

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 单元测试: ToggleWordFavoriteUseCase
 *
 * 验证切换收藏状态的业务逻辑
 */
class ToggleWordFavoriteUseCaseTest {

    private lateinit var wordRepository: WordRepository
    private lateinit var useCase: ToggleWordFavoriteUseCase

    @Before
    fun setup() {
        wordRepository = mockk()
        useCase = ToggleWordFavoriteUseCase(wordRepository)
    }

    @Test
    fun `should toggle favorite from false to true`() = runTest {
        // Given: 未收藏的单词
        val word = createTestWord(isFavorite = false)
        every { wordRepository.getWordById(1) } returns flowOf(word)
        coEvery { wordRepository.updateFavoriteStatus(1, true) } returns Result.Success(Unit)

        // When
        val result = useCase(wordId = 1)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(true, result.getOrNull()) // 返回新状态true
        coVerify(exactly = 1) { wordRepository.updateFavoriteStatus(1, true) }
    }

    @Test
    fun `should toggle favorite from true to false`() = runTest {
        // Given: 已收藏的单词
        val word = createTestWord(isFavorite = true)
        every { wordRepository.getWordById(1) } returns flowOf(word)
        coEvery { wordRepository.updateFavoriteStatus(1, false) } returns Result.Success(Unit)

        // When
        val result = useCase(wordId = 1)

        // Then
        assertTrue(result is Result.Success)
        assertEquals(false, result.getOrNull()) // 返回新状态false
        coVerify(exactly = 1) { wordRepository.updateFavoriteStatus(1, false) }
    }

    @Test
    fun `should return error when word not found`() = runTest {
        // Given
        every { wordRepository.getWordById(999) } returns flowOf(null)

        // When
        val result = useCase(wordId = 999)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    private fun createTestWord(isFavorite: Boolean = false) = Word(
        id = 1,
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
        repetitionCount = 0,
        stability = 2.5f,
        difficulty = 5.0f,
        interval = 0,
        nextReviewDate = 0,
        lastReviewedDate = null,
        firstLearnedDate = null,
        isFavorite = isFavorite,
        isSkipped = false,
        lastModifiedTime = System.currentTimeMillis()
    )
}
