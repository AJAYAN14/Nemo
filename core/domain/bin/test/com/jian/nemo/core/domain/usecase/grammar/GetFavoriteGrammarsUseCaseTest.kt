package com.jian.nemo.core.domain.usecase.grammar

import app.cash.turbine.test
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.GrammarExample
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarUsage
import com.jian.nemo.core.domain.repository.GrammarRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 单元测试: GetFavoriteGrammarsUseCase
 *
 * 验证获取收藏语法的业务逻辑
 */
class GetFavoriteGrammarsUseCaseTest {

    private lateinit var grammarRepository: GrammarRepository
    private lateinit var useCase: GetFavoriteGrammarsUseCase

    @Before
    fun setup() {
        grammarRepository = mockk()
        useCase = GetFavoriteGrammarsUseCase(grammarRepository)
    }

    @Test
    fun `invoke should return favorite grammars`() = runTest {
        // Given
        val favoriteGrammars = listOf(
            createTestGrammar(id = 1, isFavorite = true),
            createTestGrammar(id = 2, isFavorite = true)
        )
        every { grammarRepository.getFavoriteGrammars() } returns flowOf(favoriteGrammars)

        // When & Then
        useCase().test {
            awaitItem() // Loading

            val successResult = awaitItem()
            assertTrue(successResult is Result.Success)
            assertEquals(2, successResult.getOrNull()?.size)
            assertTrue(successResult.getOrNull()?.all { it.isFavorite } == true)

            awaitComplete()
        }
    }

    @Test
    fun `invoke should return empty list when no favorites`() = runTest {
        // Given
        every { grammarRepository.getFavoriteGrammars() } returns flowOf(emptyList())

        // When & Then
        useCase().test {
            awaitItem() // Loading

            val successResult = awaitItem()
            assertTrue(successResult is Result.Success)
            assertTrue(successResult.getOrNull()?.isEmpty() == true)

            awaitComplete()
        }
    }

    @Test
    fun `invoke should handle repository errors`() = runTest {
        // Given
        val exception = RuntimeException("Database error")
        every { grammarRepository.getFavoriteGrammars() } returns kotlinx.coroutines.flow.flow {
            throw exception
        }

        // When & Then
        useCase().test {
            awaitItem() // Loading

            val result = awaitItem()
            assertTrue(result is Result.Error)
            assertEquals(exception, result.exceptionOrNull())

            awaitComplete()
        }
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
