package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.feature.learning.domain.LearningQueueManager
import com.jian.nemo.feature.learning.domain.QueueSelectionResult
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.presentation.LearningItem
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.LearningStatus
import com.jian.nemo.feature.learning.presentation.LearningUiState
import com.jian.nemo.feature.learning.presentation.SlideDirection
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
import org.junit.Before
import org.junit.Test

/**
 * QueueNavigationDelegate 独立单元测试 (系统设计 §8.1.2 / UserStory US-4 / F4)
 *
 * 覆盖：
 * 1. handleSelectionResult: Empty(完成会话), Wait(等待状态), Next(平滑切卡)
 * 2. resumeFromWaiting: 解除倒计时与内存冷却，恢复出题
 * 3. navigateNext / navigatePrev / navigateTo: 自由浏览与边界拦截
 * 4. updateQueueBadgeCounts: 四色队列角标统计
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QueueNavigationDelegateTest {

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
    private val learningQueueManager = LearningQueueManager()
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val srsIntervalPreview = mockk<SrsIntervalPreview>(relaxed = true)
    private val settingsDelegate = mockk<SettingsDelegate>(relaxed = true)
    private val statsTimerDelegate = mockk<StatsTimerDelegate>(relaxed = true)

    private lateinit var queueNavigationDelegate: QueueNavigationDelegate

    private fun createTestWord(id: Int, isNew: Boolean = false): Word {
        return Word(
            id = id,
            rawId = "test_$id",
            japanese = "言葉$id",
            hiragana = "ことば",
            chinese = "词汇",
            level = "N1",
            repetitionCount = if (isNew) 0 else 1,
            interval = if (isNew) 0 else 1,
            type = if (isNew) 0 else 2
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        queueNavigationDelegate = QueueNavigationDelegate(
            learningQueueManager = learningQueueManager,
            settingsRepository = settingsRepository,
            srsIntervalPreview = srsIntervalPreview,
            sessionState = sessionState,
            stateUpdater = testStateUpdater,
            scope = testScope,
            settingsDelegate = settingsDelegate
        )
        queueNavigationDelegate.statsTimerDelegate = statsTimerDelegate
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `handleSelectionResult - Empty triggers completeSession on statsTimerDelegate`() {
        queueNavigationDelegate.handleSelectionResult(
            result = QueueSelectionResult.Empty,
            newList = emptyList<Word>(),
            level = "GLOBAL",
            isWord = true
        )

        verify(exactly = 1) { statsTimerDelegate.completeSession() }
    }

    @Test
    fun `handleSelectionResult - Wait updates status to Waiting and records waitingUntil`() {
        val word = createTestWord(1)
        queueNavigationDelegate.handleSelectionResult(
            result = QueueSelectionResult.Wait(waitingUntil = 50000L),
            newList = listOf(word),
            level = "GLOBAL",
            isWord = true
        )

        val state = testStateUpdater.currentState
        assertEquals(LearningStatus.Waiting, state.status)
        assertEquals(50000L, state.waitingUntil)
        assertEquals(1, state.wordList.size)
    }

    @Test
    fun `handleSelectionResult - Next updates state with next card and calls armShowAnswerDelay`() {
        val word = createTestWord(1)
        queueNavigationDelegate.handleSelectionResult(
            result = QueueSelectionResult.Next(index = 0, item = word),
            newList = listOf(word),
            level = "GLOBAL",
            isWord = true
        )

        val state = testStateUpdater.currentState
        assertEquals(LearningStatus.Learning, state.status)
        assertEquals(word, state.currentWord)
        assertEquals(0, state.currentIndex)
        assertEquals(SlideDirection.FORWARD, state.slideDirection)
        verify(exactly = 1) { settingsDelegate.armShowAnswerDelay() }
    }

    @Test
    fun `resumeFromWaiting - clears waitingUntil and dueTimes and selects first card`() {
        val word = createTestWord(1)
        sessionState.learningDueTimes[word.id] = 123456L
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                waitingUntil = 123456L,
                status = LearningStatus.Waiting
            )
        }

        queueNavigationDelegate.resumeFromWaiting()

        val state = testStateUpdater.currentState
        assertEquals(0L, state.waitingUntil)
        assertNull(sessionState.learningDueTimes[word.id])
        assertEquals(LearningStatus.Learning, state.status)
        assertEquals(word, state.currentWord)
    }

    @Test
    fun `navigateNext and navigatePrev - navigates smoothly within bounds`() = testScope.runTest {
        val w1 = createTestWord(1)
        val w2 = createTestWord(2)
        val w3 = createTestWord(3)
        val list = listOf(w1, w2, w3)

        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = list,
                currentIndex = 0,
                currentWord = w1,
                status = LearningStatus.Learning
            )
        }

        // 向前导航到第 1 项
        queueNavigationDelegate.navigateNext()
        advanceUntilIdle()

        assertEquals(1, testStateUpdater.currentState.currentIndex)
        assertEquals(w2, testStateUpdater.currentState.currentWord)
        assertEquals(SlideDirection.FORWARD, testStateUpdater.currentState.slideDirection)

        // 向后导航回第 0 项
        queueNavigationDelegate.navigatePrev()
        advanceUntilIdle()

        assertEquals(0, testStateUpdater.currentState.currentIndex)
        assertEquals(w1, testStateUpdater.currentState.currentWord)
        assertEquals(SlideDirection.BACKWARD, testStateUpdater.currentState.slideDirection)
    }

    @Test
    fun `navigatePrev at index 0 does nothing`() = testScope.runTest {
        val w1 = createTestWord(1)
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(w1),
                currentIndex = 0,
                currentWord = w1,
                status = LearningStatus.Learning
            )
        }

        queueNavigationDelegate.navigatePrev()
        advanceUntilIdle()

        assertEquals(0, testStateUpdater.currentState.currentIndex)
    }

    @Test
    fun `updateQueueBadgeCounts - correctly calculates card badges`() {
        val newWord = createTestWord(1, isNew = true)
        val reviewWord = createTestWord(2, isNew = false)

        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(newWord, reviewWord)
            )
        }

        queueNavigationDelegate.updateQueueBadgeCounts()

        val state = testStateUpdater.currentState
        assertEquals(1, state.queueNewCount)
        assertEquals(1, state.queueReviewCount)
        assertEquals(0, state.queueLearningCount)
        assertEquals(0, state.queueRelearnCount)
    }
}
