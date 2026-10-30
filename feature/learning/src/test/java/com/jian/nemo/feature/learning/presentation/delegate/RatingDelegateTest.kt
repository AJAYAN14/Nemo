package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.SrsUpdateResult
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
import com.jian.nemo.feature.learning.domain.ScheduleResult
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.presentation.LearningItem
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.LearningStatus
import com.jian.nemo.feature.learning.presentation.LearningUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
 * RatingDelegate 独立单元测试 (系统设计 §8.1.2 / UserStory US-3 / N2 / Q3)
 *
 * 覆盖：
 * 1. quality == 5 熟词速通分支
 * 2. quality < 3 忘记与失败调度 (Lapse)
 * 3. quality 3-4 掌握分支 (新词学习中 vs 既有复习卡)
 * 4. handleScheduleResult 三分支 (Leech, Graduate, Requeue)
 * 5. 防抖保护 (Processing 状态拦截)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RatingDelegateTest {

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
    private val learningScheduler = mockk<LearningScheduler>(relaxed = true)
    private val srsCalculator = mockk<SrsCalculator>(relaxed = true)
    private val srsIntervalPreview = mockk<SrsIntervalPreview>(relaxed = true)
    private val wordRepository = mockk<WordRepository>(relaxed = true)
    private val grammarRepository = mockk<GrammarRepository>(relaxed = true)
    private val updateWordUseCase = mockk<UpdateWordUseCase>(relaxed = true)
    private val updateGrammarUseCase = mockk<UpdateGrammarUseCase>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val studyRecordRepository = mockk<StudyRecordRepository>(relaxed = true)
    private val reviewLogRepository = mockk<ReviewLogRepository>(relaxed = true)
    private val undoDelegate = mockk<UndoDelegate>(relaxed = true)
    private val statsTimerDelegate = mockk<StatsTimerDelegate>(relaxed = true)
    private val queueNavigationDelegate = mockk<QueueNavigationDelegate>(relaxed = true)
    private val cardOpsDelegate = mockk<CardOpsDelegate>(relaxed = true)

    private lateinit var ratingDelegate: RatingDelegate

    private fun createTestWord(
        id: Int,
        japanese: String = "言葉$id",
        isNew: Boolean = false
    ): Word {
        return Word(
            id = id,
            rawId = "test_$id",
            japanese = japanese,
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

        coEvery { updateWordUseCase(any()) } returns Result.Success(Unit)
        coEvery { updateGrammarUseCase(any()) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementLearnedWords(any()) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementReviewedWords(any()) } returns Result.Success(Unit)

        every { srsCalculator.calculate(any(), any(), any()) } returns SrsUpdateResult(
            repetitionCount = 2,
            stability = 3.5f,
            difficulty = 4.0f,
            interval = 3,
            nextReviewDate = 1003L,
            lastReviewedDate = 1000L,
            firstLearnedDate = 900L,
            type = 2
        )

        ratingDelegate = RatingDelegate(
            learningQueueManager = learningQueueManager,
            learningScheduler = learningScheduler,
            srsCalculator = srsCalculator,
            srsIntervalPreview = srsIntervalPreview,
            wordRepository = wordRepository,
            grammarRepository = grammarRepository,
            updateWordUseCase = updateWordUseCase,
            updateGrammarUseCase = updateGrammarUseCase,
            settingsRepository = settingsRepository,
            studyRecordRepository = studyRecordRepository,
            reviewLogRepository = reviewLogRepository,
            sessionState = sessionState,
            stateUpdater = testStateUpdater,
            scope = testScope,
            undoDelegate = undoDelegate,
            statsTimerDelegate = statsTimerDelegate,
            queueNavigationDelegate = queueNavigationDelegate,
            cardOpsDelegate = cardOpsDelegate
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `rate - quality 5 easy pass removes step, updates SRS and moves next`() = testScope.runTest {
        val word = createTestWord(1, isNew = false)
        sessionState.learningSteps[word.id] = 1

        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                currentIndex = 0,
                currentWord = word,
                status = LearningStatus.Learning
            )
        }

        ratingDelegate.rate(5)
        advanceUntilIdle()

        // 验证撤销快照已保存
        verify(exactly = 1) { undoDelegate.saveUndoSnapshot() }
        // 验证 learningSteps 已移除该卡片
        assertNull(sessionState.learningSteps[word.id])
        // 验证 SRS 计算器被调用 (quality = 5)
        verify { srsCalculator.calculate(any(), 5, any()) }
        // 验证切卡方法被调用
        verify(exactly = 1) { queueNavigationDelegate.removeCurrentAndMoveNext() }
        // 验证 UI 状态乐观更新
        assertEquals(1, testStateUpdater.currentState.completedThisSession)
    }

    @Test
    fun `rate - quality less than 3 triggers lapse penalty and scheduleFailure`() = testScope.runTest {
        val word = createTestWord(1, isNew = false)
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                currentIndex = 0,
                currentWord = word,
                status = LearningStatus.Learning
            )
        }

        val penalizedWordItem = LearningItem.WordItem(word.copy(type = 3), step = 0, dueTime = 0L)
        every {
            learningScheduler.scheduleFailure(any(), any(), any(), any())
        } returns ScheduleResult.Requeue(
            updatedItem = penalizedWordItem,
            nextStepIndex = 0,
            dueTime = 60000L,
            isLapse = true
        )

        ratingDelegate.rate(1)
        advanceUntilIdle()

        // 验证调度失败方法被调用
        verify(exactly = 1) {
            learningScheduler.scheduleFailure(any(), 0, any(), sessionState.leechThreshold)
        }
        // 验证卡片被重新入队
        verify(exactly = 1) { queueNavigationDelegate.reQueueAtRelativeOffset(any()) }
        // 验证 step 记录为 0
        assertEquals(0, sessionState.learningSteps[word.id])
    }

    @Test
    fun `rate - quality 3-4 for learning item triggers schedulePass`() = testScope.runTest {
        val word = createTestWord(1, isNew = true)
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                currentIndex = 0,
                currentWord = word,
                status = LearningStatus.Learning
            )
        }

        every {
            learningScheduler.schedulePass(any(), any(), any(), any())
        } returns ScheduleResult.Graduate(
            item = LearningItem.WordItem(word),
            quality = 4
        )

        ratingDelegate.rate(4)
        advanceUntilIdle()

        // 验证 schedulePass 被调用
        verify(exactly = 1) { learningScheduler.schedulePass(any(), 4, 0, any()) }
        // 验证卡片毕业移除切卡
        verify(exactly = 1) { queueNavigationDelegate.removeCurrentAndMoveNext() }
    }

    @Test
    fun `rate - schedule failure returning Leech calls cardOpsDelegate handleLeech`() = testScope.runTest {
        val word = createTestWord(1, isNew = false)
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                currentIndex = 0,
                currentWord = word,
                status = LearningStatus.Learning
            )
        }

        val wordItem = LearningItem.WordItem(word)
        every {
            learningScheduler.scheduleFailure(any(), any(), any(), any())
        } returns ScheduleResult.Leech(item = wordItem, totalLapses = 5)

        ratingDelegate.rate(1)
        advanceUntilIdle()

        // 验证钉子户处理方法被触发
        coVerify(exactly = 1) { cardOpsDelegate.handleLeech(any(), 5) }
    }

    @Test
    fun `rate - debounce ignores rating when status is Processing`() = testScope.runTest {
        val word = createTestWord(1)
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                currentIndex = 0,
                currentWord = word,
                status = LearningStatus.Processing
            )
        }

        ratingDelegate.rate(4)
        advanceUntilIdle()

        // 由于防抖处于 Processing，不执行任何保存快照或调度
        verify(exactly = 0) { undoDelegate.saveUndoSnapshot() }
    }
}
