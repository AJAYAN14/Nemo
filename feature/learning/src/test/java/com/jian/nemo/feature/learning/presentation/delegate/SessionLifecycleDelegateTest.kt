package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.usecase.grammar.GetDueGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetLearningGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetNewGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetTodayLearnedGrammarsCountUseCase
import com.jian.nemo.core.domain.usecase.word.GetDueWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetLearningWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetNewWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetTodayLearnedWordsCountUseCase
import com.jian.nemo.core.data.manager.CloudBackupManager
import com.jian.nemo.feature.learning.domain.LearningSessionPolicy
import com.jian.nemo.feature.learning.domain.SessionLoadResult
import com.jian.nemo.feature.learning.domain.SessionLoader
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.LearningStatus
import com.jian.nemo.feature.learning.presentation.LearningUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * SessionLifecycleDelegate 独立单元测试 (系统设计 §8.1.2 / UserStory US-2 / F2)
 *
 * 覆盖：
 * 1. exitLearning: 清空快照、清空计时、彻底重置 UI 状态
 * 2. changeLearningMode: 切换模式并触发新模式会话启动
 * 3. handleStart: 设置 Loading 状态，清空快照，加载新会话
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionLifecycleDelegateTest {

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
    private val sessionLoader = mockk<SessionLoader>(relaxed = true)
    private val learningSessionPolicy = mockk<LearningSessionPolicy>(relaxed = true)
    private val wordRepository = mockk<WordRepository>(relaxed = true)
    private val grammarRepository = mockk<GrammarRepository>(relaxed = true)
    private val getNewWordsUseCase = mockk<GetNewWordsUseCase>(relaxed = true)
    private val getDueWordsUseCase = mockk<GetDueWordsUseCase>(relaxed = true)
    private val getLearningWordsUseCase = mockk<GetLearningWordsUseCase>(relaxed = true)
    private val getTodayLearnedWordsCountUseCase = mockk<GetTodayLearnedWordsCountUseCase>(relaxed = true)
    private val getNewGrammarsUseCase = mockk<GetNewGrammarsUseCase>(relaxed = true)
    private val getDueGrammarsUseCase = mockk<GetDueGrammarsUseCase>(relaxed = true)
    private val getLearningGrammarsUseCase = mockk<GetLearningGrammarsUseCase>(relaxed = true)
    private val getTodayLearnedGrammarsCountUseCase = mockk<GetTodayLearnedGrammarsCountUseCase>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val cloudBackupManager = mockk<CloudBackupManager>(relaxed = true)
    private val settingsDelegate = mockk<SettingsDelegate>(relaxed = true)
    private val undoDelegate = mockk<UndoDelegate>(relaxed = true)
    private val statsTimerDelegate = mockk<StatsTimerDelegate>(relaxed = true)
    private val queueNavigationDelegate = mockk<QueueNavigationDelegate>(relaxed = true)
    private val srsIntervalPreview = mockk<SrsIntervalPreview>(relaxed = true)

    private lateinit var sessionLifecycleDelegate: SessionLifecycleDelegate

    private fun createTestWord(id: Int): Word {
        return Word(
            id = id,
            rawId = "test_$id",
            japanese = "言葉$id",
            hiragana = "ことば",
            chinese = "词汇",
            level = "N1",
            repetitionCount = 0,
            interval = 0,
            type = 0
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { settingsRepository.dailyGoalFlow } returns flowOf(20)
        every { settingsRepository.grammarDailyGoalFlow } returns flowOf(20)
        every { settingsRepository.learningDayResetHourFlow } returns flowOf(4)
        every { settingsRepository.wordLapsesFlow } returns flowOf(emptyMap())
        every { settingsRepository.grammarLapsesFlow } returns flowOf(emptyMap())
        every { settingsRepository.sessionBatchSizeFlow } returns flowOf(20)
        every { settingsRepository.backlogNewItemStrategyFlow } returns flowOf("FIFO")
        every { settingsRepository.maxDailyReviewsFlow } returns flowOf(50)
        every { settingsRepository.isRandomNewContentEnabledFlow } returns flowOf(false)
        coEvery { settingsRepository.getWordSession() } returns flowOf(null)
        coEvery { settingsRepository.getGrammarSession() } returns flowOf(null)

        every { getTodayLearnedWordsCountUseCase() } returns flowOf(Result.Success(5))
        every { getTodayLearnedGrammarsCountUseCase() } returns flowOf(Result.Success(3))
        every { wordRepository.getDueWordsCount(any()) } returns flowOf(10)
        every { grammarRepository.getDueGrammarsCount(any()) } returns flowOf(8)

        sessionLifecycleDelegate = SessionLifecycleDelegate(
            sessionLoader = sessionLoader,
            learningSessionPolicy = learningSessionPolicy,
            wordRepository = wordRepository,
            grammarRepository = grammarRepository,
            getNewWordsUseCase = getNewWordsUseCase,
            getDueWordsUseCase = getDueWordsUseCase,
            getLearningWordsUseCase = getLearningWordsUseCase,
            getTodayLearnedWordsCountUseCase = getTodayLearnedWordsCountUseCase,
            getNewGrammarsUseCase = getNewGrammarsUseCase,
            getDueGrammarsUseCase = getDueGrammarsUseCase,
            getLearningGrammarsUseCase = getLearningGrammarsUseCase,
            getTodayLearnedGrammarsCountUseCase = getTodayLearnedGrammarsCountUseCase,
            settingsRepository = settingsRepository,
            cloudBackupManager = cloudBackupManager,
            sessionState = sessionState,
            stateUpdater = testStateUpdater,
            scope = testScope,
            settingsDelegate = settingsDelegate,
            undoDelegate = undoDelegate,
            statsTimerDelegate = statsTimerDelegate,
            queueNavigationDelegate = queueNavigationDelegate,
            srsIntervalPreview = srsIntervalPreview
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exitLearning - resets UI state, clears snapshots and clears timer`() {
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                completedToday = 15,
                status = LearningStatus.Learning
            )
        }

        sessionLifecycleDelegate.exitLearning()

        verify(exactly = 1) { undoDelegate.clearSnapshots() }
        verify(exactly = 1) { statsTimerDelegate.clearSessionTimer() }
        assertEquals(LearningUiState(), testStateUpdater.currentState)
    }

    @Test
    fun `changeLearningMode - updates mode and triggers handleStart`() = testScope.runTest {
        testStateUpdater.update { it.copy(learningMode = LearningMode.Word) }

        sessionLifecycleDelegate.changeLearningMode(LearningMode.Grammar)
        testScheduler.runCurrent()

        assertEquals(LearningMode.Grammar, testStateUpdater.currentState.learningMode)
        verify(atLeast = 1) { undoDelegate.clearSnapshots() }
    }

    @Test
    fun `handleStart - sets status to Loading and transitions to Learning when new session loaded`() = testScope.runTest {
        val word = createTestWord(1)
        coEvery {
            sessionLoader.loadSession<Word>(
                level = any(),
                dailyGoal = any(),
                completedToday = any(),
                savedSession = any(),
                getItemsByIds = any(),
                getDueItems = any(),
                getNewItems = any(),
                getLearningItems = any(),
                getItemId = any(),
                filterByLevel = any(),
                isItemNew = any(),
                sessionBatchSize = any(),
                backlogStrategy = any(),
                maxDailyReviews = any(),
                todayReviewedCount = any(),
                todayEpochDay = any()
            )
        } returns SessionLoadResult.NewSession(
            items = listOf(word),
            dueCount = 0,
            newCount = 1,
            dailyGoal = 20,
            completedToday = 5
        )

        testStateUpdater.update { it.copy(status = LearningStatus.Idle) }

        sessionLifecycleDelegate.handleStart()

        // 验证执行完毕后进入 Learning 状态，且各属性正确装填
        advanceUntilIdle()
        assertEquals(LearningStatus.Learning, testStateUpdater.currentState.status)
        assertEquals("GLOBAL", testStateUpdater.currentState.selectedLevel)
        assertEquals(word, testStateUpdater.currentState.currentWord)
        assertEquals(20, testStateUpdater.currentState.dailyGoal)
        assertEquals(5, testStateUpdater.currentState.completedToday)
        verify(exactly = 1) { undoDelegate.clearSnapshots() }
    }
}
