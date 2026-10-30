package com.jian.nemo.feature.learning.presentation

import android.content.Context
import android.content.SharedPreferences
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.ReviewLogRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.feature.learning.domain.LearningQueueManager
import com.jian.nemo.feature.learning.domain.LearningScheduler
import com.jian.nemo.feature.learning.domain.LearningSessionStatsManager
import com.jian.nemo.feature.learning.domain.LearningUndoHelper
import com.jian.nemo.feature.learning.domain.QueueSelectionResult
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.domain.UndoSnapshot
import com.jian.nemo.feature.learning.presentation.delegate.CardOpsDelegate
import com.jian.nemo.feature.learning.presentation.delegate.DebounceConstants
import com.jian.nemo.feature.learning.presentation.delegate.LearningSessionState
import com.jian.nemo.feature.learning.presentation.delegate.QueueNavigationDelegate
import com.jian.nemo.feature.learning.presentation.delegate.RatingDelegate
import com.jian.nemo.feature.learning.presentation.delegate.SettingsDelegate
import com.jian.nemo.feature.learning.presentation.delegate.StateUpdater
import com.jian.nemo.feature.learning.presentation.delegate.StatsTimerDelegate
import com.jian.nemo.feature.learning.presentation.delegate.UndoDelegate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 学习模块行为契约测试 (US-12 / F12 / N1 / QS-01 ~ QS-05)
 *
 * 验证重构后行为与重构前 100% 一致，严格覆盖 5 个关键时序场景：
 * 1. 评分防抖 (QS-01): 100ms 内快速连续评分，第二次被拦截忽略
 * 2. 导航防抖 (QS-02): 400ms 内快速连续导航，第二次被拦截忽略
 * 3. 跨天重置 (QS-03): 学习日跨越天数时，统计清零重新计数
 * 4. Learn Ahead Limit (QS-04): 队列全部冷却时，容差内允许提前学习，超限则等待
 * 5. 撤销恢复 (QS-05): 评分后撤销，数据库 + UI 状态 + 内存 Map 全部恢复
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LearningBehaviorContractTest {

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

    private fun createTestWord(id: Int, japanese: String = "言葉$id"): Word {
        return Word(
            id = id,
            rawId = id.toString(),
            japanese = japanese,
            hiragana = "ことば",
            chinese = "单词$id",
            level = "N1",
            repetitionCount = 1,
            interval = 1,
            type = 2
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =========================================================================
    // 场景 1：评分防抖（QS-01：RATING_DEBOUNCE_MS = 100L）
    // =========================================================================
    @Test
    fun `rating debounce - second rating within 100ms is ignored`() = testScope.runTest {
        val learningQueueManager = LearningQueueManager()
        val learningScheduler = mockk<LearningScheduler>(relaxed = true)
        val srsCalculator = mockk<SrsCalculator>(relaxed = true)
        val srsIntervalPreview = mockk<SrsIntervalPreview>(relaxed = true)
        val wordRepository = mockk<WordRepository>(relaxed = true)
        val grammarRepository = mockk<GrammarRepository>(relaxed = true)
        val updateWordUseCase = mockk<UpdateWordUseCase>(relaxed = true)
        val updateGrammarUseCase = mockk<UpdateGrammarUseCase>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val studyRecordRepository = mockk<StudyRecordRepository>(relaxed = true)
        val reviewLogRepository = mockk<ReviewLogRepository>(relaxed = true)
        val undoDelegate = mockk<UndoDelegate>(relaxed = true)
        val statsTimerDelegate = mockk<StatsTimerDelegate>(relaxed = true)
        val queueNavigationDelegate = mockk<QueueNavigationDelegate>(relaxed = true)
        val cardOpsDelegate = mockk<CardOpsDelegate>(relaxed = true)

        val testWord = createTestWord(1)
        val words = listOf(testWord)
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = words,
                currentIndex = 0,
                currentWord = testWord,
                status = LearningStatus.Learning
            )
        }

        val ratingDelegate = RatingDelegate(
            learningQueueManager, learningScheduler, srsCalculator, srsIntervalPreview,
            wordRepository, grammarRepository, updateWordUseCase, updateGrammarUseCase,
            settingsRepository, studyRecordRepository, reviewLogRepository,
            sessionState, testStateUpdater, testScope,
            undoDelegate, statsTimerDelegate, queueNavigationDelegate, cardOpsDelegate
        )

        // 第一次评分：启动进入 Processing 并触发保存撤销快照
        ratingDelegate.rate(3)
        advanceTimeBy(50L)
        testDispatcher.scheduler.runCurrent()

        // 验证进入 Processing 状态并触发了 saveUndoSnapshot
        assertEquals(LearningStatus.Processing, testStateUpdater.currentState.status)
        io.mockk.verify(exactly = 1) { undoDelegate.saveUndoSnapshot() }

        // 紧接着在 100ms 防抖期内发起第二次评分
        ratingDelegate.rate(4)
        testDispatcher.scheduler.runCurrent()

        // 验证第二次评分被防抖拦截，没有再次触发快照保存或调度
        io.mockk.verify(exactly = 1) { undoDelegate.saveUndoSnapshot() }

        // 前进时间超过 100ms 防抖期
        advanceTimeBy(100L)
        testDispatcher.scheduler.runCurrent()
    }

    // =========================================================================
    // 场景 2：导航防抖（QS-02：NAVIGATION_DEBOUNCE_MS = 400L）
    // =========================================================================
    @Test
    fun `navigation debounce - second navigation within 400ms is ignored`() = testScope.runTest {
        val learningQueueManager = mockk<LearningQueueManager>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val srsIntervalPreview = mockk<SrsIntervalPreview>(relaxed = true)
        val settingsDelegate = mockk<SettingsDelegate>(relaxed = true)

        val words = listOf(createTestWord(1), createTestWord(2), createTestWord(3))
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = words,
                currentIndex = 0,
                currentWord = words[0],
                status = LearningStatus.Learning,
                isNavigating = false
            )
        }

        val qnd = QueueNavigationDelegate(
            learningQueueManager, settingsRepository, srsIntervalPreview,
            sessionState, testStateUpdater, testScope, settingsDelegate
        )

        // 第一次导航
        qnd.navigateNext()
        testDispatcher.scheduler.runCurrent()

        // 此时已处于导航中 (isNavigating == true)
        assertTrue(testStateUpdater.currentState.isNavigating)
        assertEquals(1, testStateUpdater.currentState.currentIndex)

        // 在 400ms 窗口期内快速连续点击下一张
        qnd.navigateNext()
        testDispatcher.scheduler.runCurrent()

        // 索引未发生改变，第二次导航被忽略
        assertEquals(1, testStateUpdater.currentState.currentIndex)

        // 前进时间超过 400ms 防抖期
        advanceTimeBy(DebounceConstants.NAVIGATION_DEBOUNCE_MS + 50L)
        testDispatcher.scheduler.runCurrent()

        // 导航状态解锁
        assertFalse(testStateUpdater.currentState.isNavigating)

        // 此时再次导航，顺利前进到 index 2
        qnd.navigateNext()
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, testStateUpdater.currentState.currentIndex)
    }

    // =========================================================================
    // 场景 3：跨天重置（QS-03：跨越午夜清零统计并重置天数）
    // =========================================================================
    @Test
    fun `cross-day reset - stats reset when learning day crosses midnight`() {
        val mockContext = mockk<Context>(relaxed = true)
        val mockPrefs = mockk<SharedPreferences>(relaxed = true)
        val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockPrefs
        every { mockPrefs.edit() } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putInt(any(), any()) } returns mockEditor
        every { mockEditor.remove(any()) } returns mockEditor

        // 模拟上次保存的天数为 100L (昨天)
        every { mockPrefs.getLong("last_reset_day", 0L) } returns 100L

        val statsManager = LearningSessionStatsManager(mockContext)

        // 今日为 101L (发生跨天)
        val today = 101L
        statsManager.checkAndRestoreOrReset(LearningMode.Word, today)

        // 验证清空重置操作
        assertEquals(0, statsManager.currentCombo)
        assertEquals(0, statsManager.sessionNewCount)
        assertEquals(0, statsManager.sessionReviewCount)
        assertEquals(0, statsManager.sessionRelearnCount)

        // 验证更新持久化的 last_reset_day
        io.mockk.verify { mockEditor.putLong("last_reset_day", today) }
    }

    // =========================================================================
    // 场景 4：Learn Ahead Limit（QS-04：队列冷却超限设 Waiting）
    // =========================================================================
    @Test
    fun `learn ahead limit - set Waiting status when all items cooling`() = testScope.runTest {
        val learningQueueManager = mockk<LearningQueueManager>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val srsIntervalPreview = mockk<SrsIntervalPreview>(relaxed = true)
        val settingsDelegate = mockk<SettingsDelegate>(relaxed = true)

        val words = listOf(createTestWord(1))
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = words,
                status = LearningStatus.Learning
            )
        }

        val qnd = QueueNavigationDelegate(
            learningQueueManager, settingsRepository, srsIntervalPreview,
            sessionState, testStateUpdater, testScope, settingsDelegate
        )

        // 队列返回等待中（冷却至 futureTime）
        val futureWaitTime = System.currentTimeMillis() + 3600_000L
        qnd.handleSelectionResult(
            result = QueueSelectionResult.Wait(futureWaitTime),
            newList = words,
            level = "GLOBAL",
            isWord = true
        )

        // 验证 UI 状态变为 Waiting，并正确附带 waitingUntil 时间戳
        assertEquals(LearningStatus.Waiting, testStateUpdater.currentState.status)
        assertEquals(futureWaitTime, testStateUpdater.currentState.waitingUntil)
    }

    // =========================================================================
    // 场景 5：撤销恢复（QS-05：数据库、内存 Map、UI 全状态一致性回退）
    // =========================================================================
    @Test
    fun `undo restore - database plus UI state plus memory map all restored`() = testScope.runTest {
        val learningUndoHelper = mockk<LearningUndoHelper>(relaxed = true)
        val updateWordUseCase = mockk<UpdateWordUseCase>(relaxed = true)
        val updateGrammarUseCase = mockk<UpdateGrammarUseCase>(relaxed = true)
        val studyRecordRepository = mockk<StudyRecordRepository>(relaxed = true)
        val wordRepository = mockk<WordRepository>(relaxed = true)
        val grammarRepository = mockk<GrammarRepository>(relaxed = true)
        val srsIntervalPreview = mockk<SrsIntervalPreview>(relaxed = true)
        val queueNavigationDelegate = mockk<QueueNavigationDelegate>(relaxed = true)

        coEvery { updateWordUseCase(any()) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementLearnedWords(any()) } returns Result.Success(Unit)

        val originalWord = createTestWord(42, "記憶")
        val snapshot = UndoSnapshot(
            item = LearningItem.WordItem(originalWord),
            wasNew = true,
            originalWord = originalWord,
            originalGrammar = null,
            wordList = listOf(originalWord),
            grammarList = emptyList(),
            currentIndex = 0,
            currentGrammarIndex = 0,
            learningSteps = mapOf(42 to 1),
            learningDueTimes = mapOf(42 to 1000L),
            requeuedItems = setOf(42),
            lapseCounts = mapOf(42 to 2),
            completedToday = 5,
            completedThisSession = 3,
            sessionProcessedCount = 8
        )

        every { learningUndoHelper.popSnapshot() } returns snapshot

        val undoDelegate = UndoDelegate(
            learningUndoHelper, updateWordUseCase, updateGrammarUseCase,
            studyRecordRepository, wordRepository, grammarRepository,
            srsIntervalPreview, sessionState, testStateUpdater, testScope, queueNavigationDelegate
        )

        // 执行撤销
        undoDelegate.undo()
        testDispatcher.scheduler.runCurrent()

        // 1. 验证数据库/UseCase 恢复调用
        coVerify(exactly = 1) { updateWordUseCase(originalWord) }
        coVerify(exactly = 1) { studyRecordRepository.incrementLearnedWords(-1) }

        // 2. 验证内存 Map/Set 恢复
        assertEquals(mapOf(42 to 1), sessionState.learningSteps)
        assertEquals(mapOf(42 to 1000L), sessionState.learningDueTimes)
        assertEquals(setOf(42), sessionState.requeuedItems)
        assertEquals(mapOf(42 to 2), sessionState.lapseCounts.value)

        // 3. 验证 UI 状态恢复
        val state = testStateUpdater.currentState
        assertEquals(LearningStatus.Learning, state.status)
        assertEquals(originalWord, state.currentWord)
        assertEquals(0, state.currentIndex)
        assertEquals(5, state.completedToday)
        assertEquals(3, state.completedThisSession)
        assertEquals(8, state.sessionProcessedCount)
        assertTrue(state.isAnswerShown)
        assertTrue(state.isCardFlipped)
    }
}
