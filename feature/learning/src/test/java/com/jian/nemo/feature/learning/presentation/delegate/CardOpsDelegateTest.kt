package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.feature.learning.presentation.LearningItem
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.LearningStatus
import com.jian.nemo.feature.learning.presentation.LearningUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * CardOpsDelegate 独立单元测试 (系统设计 §8.1.2 / UserStory US-6 / F6)
 *
 * 覆盖：
 * 1. suspendCurrentItem: 手动暂停当前卡片 (持久化 isSkipped=true, 清除内存状态, 切卡)
 * 2. buryCurrentItem: 今日暂缓 (持久化 buriedUntilDay=today, 清除内存状态, 切卡)
 * 3. handleLeech: bury_today 分支 vs skip 分支
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardOpsDelegateTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val stateFlow = MutableStateFlow(LearningUiState())
    private val testStateUpdater = object : StateUpdater {
        override fun update(transform: (LearningUiState) -> LearningUiState) {
            stateFlow.update(transform)
        }
        override val currentState: LearningUiState get() = stateFlow.value
    }

    private val sessionState = LearningSessionState()
    private val updateWordUseCase = mockk<UpdateWordUseCase>(relaxed = true)
    private val updateGrammarUseCase = mockk<UpdateGrammarUseCase>(relaxed = true)
    private val queueNavigationDelegate = mockk<QueueNavigationDelegate>(relaxed = true)

    private lateinit var cardOpsDelegate: CardOpsDelegate

    private fun createTestWord(id: Int, japanese: String = "言葉$id"): Word {
        return Word(
            id = id,
            rawId = "test_$id",
            japanese = japanese,
            hiragana = "ことば",
            chinese = "词汇",
            level = "N1",
            repetitionCount = 1,
            interval = 1,
            type = 2
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { updateWordUseCase(any()) } returns Result.Success(Unit)
        coEvery { updateGrammarUseCase(any()) } returns Result.Success(Unit)

        cardOpsDelegate = CardOpsDelegate(
            updateWordUseCase = updateWordUseCase,
            updateGrammarUseCase = updateGrammarUseCase,
            sessionState = sessionState,
            stateUpdater = testStateUpdater,
            scope = testScope,
            queueNavigationDelegate = queueNavigationDelegate
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `suspendCurrentItem - marks item skipped, removes memory state and moves next`() = testScope.runTest {
        val word = createTestWord(1)
        sessionState.learningSteps[word.id] = 1
        sessionState.learningDueTimes[word.id] = 12345L

        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                currentIndex = 0,
                currentWord = word,
                status = LearningStatus.Learning
            )
        }

        cardOpsDelegate.suspendCurrentItem()
        advanceUntilIdle()

        // 验证持久化了 isSkipped = true
        coVerify(exactly = 1) {
            updateWordUseCase(match { it.id == 1 && it.isSkipped })
        }
        // 验证内存状态清除
        assertNull(sessionState.learningSteps[word.id])
        assertNull(sessionState.learningDueTimes[word.id])
        // 验证切卡
        verify(exactly = 1) { queueNavigationDelegate.removeCurrentAndMoveNext() }
        // 验证提示文案
        assertTrue(testStateUpdater.currentState.error?.contains("已暂停") == true)
    }

    @Test
    fun `buryCurrentItem - sets buriedUntilDay, removes memory state and moves next`() = testScope.runTest {
        val word = createTestWord(2)
        sessionState.learningSteps[word.id] = 2
        sessionState.learningDueTimes[word.id] = 67890L

        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                currentIndex = 0,
                currentWord = word,
                status = LearningStatus.Learning
            )
        }

        cardOpsDelegate.buryCurrentItem()
        advanceUntilIdle()

        // 验证持久化设置了 buriedUntilDay
        coVerify(exactly = 1) {
            updateWordUseCase(match { it.id == 2 && it.buriedUntilDay > 0L })
        }
        // 验证内存状态清除
        assertNull(sessionState.learningSteps[word.id])
        assertNull(sessionState.learningDueTimes[word.id])
        // 验证切卡
        verify(exactly = 1) { queueNavigationDelegate.removeCurrentAndMoveNext() }
        // 验证提示文案
        assertTrue(testStateUpdater.currentState.error?.contains("已暂缓") == true)
    }

    @Test
    fun `handleLeech - bury_today action buries item for today`() = testScope.runTest {
        val word = createTestWord(3)
        sessionState.leechAction = "bury_today"
        val item = LearningItem.WordItem(word)

        cardOpsDelegate.handleLeech(item, totalLapses = 5)
        advanceUntilIdle()

        // 验证持久化设置了 buriedUntilDay
        coVerify(exactly = 1) {
            updateWordUseCase(match { it.id == 3 && it.buriedUntilDay > 0L })
        }
        // 验证提示包含顽固卡片埋藏
        assertTrue(testStateUpdater.currentState.error?.contains("已埋藏顽固卡片") == true)
        verify(exactly = 1) { queueNavigationDelegate.removeCurrentAndMoveNext() }
    }

    @Test
    fun `handleLeech - skip action freezes item with isSkipped true`() = testScope.runTest {
        val word = createTestWord(4)
        sessionState.leechAction = "skip"
        val item = LearningItem.WordItem(word)

        cardOpsDelegate.handleLeech(item, totalLapses = 6)
        advanceUntilIdle()

        // 验证持久化设置了 isSkipped = true
        coVerify(exactly = 1) {
            updateWordUseCase(match { it.id == 4 && it.isSkipped })
        }
        // 验证提示包含冻结顽固卡片
        assertTrue(testStateUpdater.currentState.error?.contains("已冻结顽固卡片") == true)
        verify(exactly = 1) { queueNavigationDelegate.removeCurrentAndMoveNext() }
    }
}
