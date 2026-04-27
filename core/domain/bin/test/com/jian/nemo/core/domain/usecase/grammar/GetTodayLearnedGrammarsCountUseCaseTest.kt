package com.jian.nemo.core.domain.usecase.grammar

import app.cash.turbine.test
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.GrammarExample
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
 * 单元测试: GetTodayLearnedGrammarsCountUseCase
 *
 * 验证获取今日学习语法数量的业务逻辑
 */
class GetTodayLearnedGrammarsCountUseCaseTest {

    private lateinit var grammarRepository: GrammarRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: GetTodayLearnedGrammarsCountUseCase

    @Before
    fun setup() {
        grammarRepository = mockk()
        settingsRepository = mockk()
        useCase = GetTodayLearnedGrammarsCountUseCase(grammarRepository, settingsRepository)

        // Mock DateTimeUtils
        mockkObject(DateTimeUtils)
        every { settingsRepository.learningDayResetHourFlow } returns flowOf(4)
        every { DateTimeUtils.getLearningDay(4) } returns 100L
    }

    @Test
    fun `invoke should return count of grammars learned today`() = runTest {
        // Given: 今天学习了5个语法
        val learnedGrammars = listOf(
            createTestGrammar(1),
            createTestGrammar(2),
            createTestGrammar(3),
            createTestGrammar(4),
            createTestGrammar(5)
        )
        every { grammarRepository.getTodayLearnedGrammars(100L) } returns flowOf(learnedGrammars)

        //When & Then
        useCase().test {
            awaitItem() // Loading

            val successResult = awaitItem()
            assertTrue(successResult is Result.Success)
            assertEquals(5, successResult.getOrNull())

            awaitComplete()
        }
    }

    @Test
    fun `invoke should return zero when no grammars learned today`() = runTest {
        // Given
        every { grammarRepository.getTodayLearnedGrammars(100L) } returns flowOf(emptyList())

        // When & Then
        useCase().test {
            awaitItem() // Loading

            val successResult = awaitItem()
            assertTrue(successResult is Result.Success)
            assertEquals(0, successResult.getOrNull())

            awaitComplete()
        }
    }

    @Test
    fun `invoke should handle repository errors`() = runTest {
        // Given
        val exception = RuntimeException("Database error")
        every { grammarRepository.getTodayLearnedGrammars(100L) } returns kotlinx.coroutines.flow.flow {
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

    private fun createTestGrammar(id: Int) = com.jian.nemo.core.domain.model.Grammar(
        id = id,
        grammar = "～について$id",
        grammarLevel = "n5",
        usages = listOf(
            GrammarUsage(
                subtype = null,
                connection = "名詞＋について",
                explanation = "关于～",
                notes = null,
                examples = listOf(
                    GrammarExample(
                        sentence = "例句$id",
                        translation = "译文$id",
                        source = null,
                        isDialog = false
                    )
                )
            )
        ),
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

