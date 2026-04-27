package com.jian.nemo.core.domain.usecase.grammar

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.GrammarExample
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarUsage
import com.jian.nemo.core.domain.repository.GrammarRepository
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
 * 单元测试: ToggleGrammarFavoriteUseCase
 *
 * 验证收藏/取消收藏语法的业务逻辑
 */
class ToggleGrammarFavoriteUseCaseTest {

    private lateinit var grammarRepository: GrammarRepository
    private lateinit var useCase: ToggleGrammarFavoriteUseCase

    @Before
    fun setup() {
        grammarRepository = mockk()
        useCase = ToggleGrammarFavoriteUseCase(grammarRepository)
    }

    @Test
    fun `should toggle favorite from false to true`() = runTest {
        // Given: 未收藏的语法
        val grammar = createTestGrammar(isFavorite = false)
        every { grammarRepository.getGrammarById(1) } returns flowOf(grammar)
        coEvery { grammarRepository.updateFavoriteStatus(1, true) } returns Result.Success(Unit)

        // When
        val result = useCase(grammarId = 1)

        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { grammarRepository.updateFavoriteStatus(1, true) }
    }

    @Test
    fun `should toggle favorite from true to false`() = runTest {
        // Given: 已收藏的语法
        val grammar = createTestGrammar(isFavorite = true)
        every { grammarRepository.getGrammarById(1) } returns flowOf(grammar)
        coEvery { grammarRepository.updateFavoriteStatus(1, false) } returns Result.Success(Unit)

        // When
        val result = useCase(grammarId = 1)

        // Then
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { grammarRepository.updateFavoriteStatus(1, false) }
    }

    @Test
    fun `should return error when grammar not found`() = runTest {
        // Given
        every { grammarRepository.getGrammarById(999) } returns flowOf(null)

        // When
        val result = useCase(grammarId = 999)

        // Then
        assertTrue(result is Result.Error)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { grammarRepository.updateFavoriteStatus(any(), any()) }
    }

    @Test
    fun `should handle repository error when updating favorite status`() = runTest {
        // Given
        val grammar = createTestGrammar(isFavorite = false)
        every { grammarRepository.getGrammarById(1) } returns flowOf(grammar)
        val exception = RuntimeException("Database error")
        coEvery { grammarRepository.updateFavoriteStatus(1, true) } returns Result.Error(exception)

        // When
        val result = useCase(grammarId = 1)

        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, result.exceptionOrNull())
    }

    private fun createTestGrammar(
        id: Int = 1,
        isFavorite: Boolean = false
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
