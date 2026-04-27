package com.jian.nemo.core.domain.usecase.grammar

import app.cash.turbine.test
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.GrammarExample
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarUsage
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 单元测试: GetDueGrammarsUseCase
 *
 * 验证获取到期复习语法的业务逻辑
 */
class GetDueGrammarsUseCaseTest {

    private lateinit var grammarRepository: GrammarRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: GetDueGrammarsUseCase

    @Before
    fun setup() {
        grammarRepository = mockk()
        settingsRepository = mockk()
        useCase = GetDueGrammarsUseCase(grammarRepository, settingsRepository)

        // Mock DateTimeUtils
        mockkObject(DateTimeUtils)
        every { settingsRepository.learningDayResetHourFlow } returns flowOf(4)
        every { DateTimeUtils.getLearningDay(4) } returns 100L
    }

    @Test
    fun `invoke should return due grammars for today`() = runTest {
        // Given: 到期的语法
        val dueGrammars = listOf(
            createTestGrammar(id = 1, nextReviewDate = 99L, repetitionCount = 1),
            createTestGrammar(id = 2, nextReviewDate = 100L, repetitionCount = 2)
        )
        every { grammarRepository.getDueGrammars(100L) } returns flowOf(dueGrammars)

        // When & Then
        useCase().test {
            awaitItem() // Loading

            val successResult = awaitItem()
            assertTrue(successResult is Result.Success)
            assertEquals(2, successResult.getOrNull()?.size)

            awaitComplete()
        }
    }

    @Test
    fun `invoke should return empty list when no due grammars`() = runTest {
        // Given
        every { grammarRepository.getDueGrammars(100L) } returns flowOf(emptyList())

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
        every { grammarRepository.getDueGrammars(100L) } returns kotlinx.coroutines.flow.flow {
            throw exception
        }

        // When & Then
        useCase().test {
            awaitItem() // Loading

            val result = awaitItem()
            assertTrue(result is Result.Error)
            assertNotNull(result.exceptionOrNull())
            assertEquals("Database error", result.exceptionOrNull()?.message)

            awaitComplete()
        }
    }

    private fun createTestGrammar(
        id: Int = 1,
        grammar: String = "～について",
        nextReviewDate: Long = 0,
        repetitionCount: Int = 0
    ) = Grammar(
        id = id,
        grammar = grammar,
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
        stability = 2.5f,
        difficulty = 5.0f,
        interval = 0,
        nextReviewDate = nextReviewDate,
        lastReviewedDate = null,
        firstLearnedDate = if (repetitionCount > 0) 90L else null,
        isFavorite = false,
        isSkipped = false,
        lastModifiedTime = System.currentTimeMillis()
    )
}
