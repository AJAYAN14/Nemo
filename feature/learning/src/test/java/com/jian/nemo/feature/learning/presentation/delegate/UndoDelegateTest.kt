package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.feature.learning.domain.LearningUndoHelper
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.presentation.LearningItem
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.LearningStatus
import com.jian.nemo.feature.learning.presentation.LearningUiState
import com.jian.nemo.feature.learning.presentation.SlideDirection
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * UndoDelegate 独立单元测试 (系统设计 §8.1.2 / UserStory US-5 / F5)
 *
 * 覆盖：
 * 1. saveUndoSnapshot: 完整捕获当前项目、数据库状态、内存 Map 并同步 canUndo
 * 2. undo: 恢复数据库状态 (updateWord + studyRecord 递减)、恢复内部 Map、恢复 UI 状态 (BACKWARD + 翻面)
 * 3. Processing 状态保护与空快照保护
 * 4. clearSnapshots: 清空快照与同步
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UndoDelegateTest {

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
    private val learningUndoHelper = LearningUndoHelper()
    private val updateWordUseCase = mockk<UpdateWordUseCase>(relaxed = true)
    private val updateGrammarUseCase = mockk<UpdateGrammarUseCase>(relaxed = true)
    private val studyRecordRepository = mockk<StudyRecordRepository>(relaxed = true)
    private val wordRepository = mockk<WordRepository>(relaxed = true)
    private val grammarRepository = mockk<GrammarRepository>(relaxed = true)
    private val srsIntervalPreview = mockk<SrsIntervalPreview>(relaxed = true)
    private val queueNavigationDelegate = mockk<QueueNavigationDelegate>(relaxed = true)

    private lateinit var undoDelegate: UndoDelegate

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
        coEvery { updateWordUseCase(any()) } returns Result.Success(Unit)
        coEvery { updateGrammarUseCase(any()) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementLearnedWords(any()) } returns Result.Success(Unit)
        coEvery { studyRecordRepository.incrementReviewedWords(any()) } returns Result.Success(Unit)

        undoDelegate = UndoDelegate(
            learningUndoHelper = learningUndoHelper,
            updateWordUseCase = updateWordUseCase,
            updateGrammarUseCase = updateGrammarUseCase,
            studyRecordRepository = studyRecordRepository,
            wordRepository = wordRepository,
            grammarRepository = grammarRepository,
            srsIntervalPreview = srsIntervalPreview,
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
    fun `saveUndoSnapshot - saves snapshot and sets canUndo to true`() {
        val word = createTestWord(1)
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                currentIndex = 0,
                currentWord = word
            )
        }
        sessionState.learningSteps[word.id] = 2

        undoDelegate.saveUndoSnapshot()

        assertTrue(learningUndoHelper.canUndo())
        assertTrue(testStateUpdater.currentState.canUndo)
    }

    @Test
    fun `undo - restores word from snapshot including database, memory and UI`() = testScope.runTest {
        val originalWord = createTestWord(10, isNew = true)
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(originalWord),
                currentIndex = 0,
                currentWord = originalWord,
                completedToday = 5,
                completedThisSession = 2,
                status = LearningStatus.Learning
            )
        }
        sessionState.learningSteps[originalWord.id] = 1

        // 1. 保存快照
        undoDelegate.saveUndoSnapshot()

        // 2. 模拟评分后状态改变
        val nextWord = createTestWord(11)
        testStateUpdater.update {
            it.copy(
                wordList = listOf(nextWord),
                currentIndex = 0,
                currentWord = nextWord,
                completedToday = 6,
                completedThisSession = 3,
                isAnswerShown = false,
                isCardFlipped = false
            )
        }
        sessionState.learningSteps.remove(originalWord.id)

        // 3. 执行撤销
        undoDelegate.undo()
        advanceUntilIdle()

        // 验证数据库回滚 (还原 originalWord，递减新学计数)
        coVerify(exactly = 1) { updateWordUseCase(originalWord) }
        coVerify(exactly = 1) { studyRecordRepository.incrementLearnedWords(-1) }

        // 验证内存 Map 恢复
        assertEquals(1, sessionState.learningSteps[originalWord.id])

        // 验证 UI 状态恢复
        val state = testStateUpdater.currentState
        assertEquals(originalWord.id, state.currentWord?.id)
        assertEquals(5, state.completedToday)
        assertEquals(2, state.completedThisSession)
        assertTrue(state.isAnswerShown)
        assertTrue(state.isCardFlipped)
        assertEquals(SlideDirection.BACKWARD, state.slideDirection)
        verify(exactly = 1) { queueNavigationDelegate.updateQueueBadgeCounts() }
        verify(exactly = 1) { queueNavigationDelegate.saveCurrentSessionState() }
    }

    @Test
    fun `undo - ignores when status is Processing`() = testScope.runTest {
        val word = createTestWord(1)
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                currentIndex = 0,
                currentWord = word
            )
        }
        undoDelegate.saveUndoSnapshot()

        // 设置为 Processing
        testStateUpdater.update { it.copy(status = LearningStatus.Processing) }

        undoDelegate.undo()
        advanceUntilIdle()

        // 验证没有执行更新数据库
        coVerify(exactly = 0) { updateWordUseCase(any()) }
    }

    @Test
    fun `clearSnapshots - empties helper and resets canUndo to false`() {
        val word = createTestWord(1)
        testStateUpdater.update {
            it.copy(
                learningMode = LearningMode.Word,
                wordList = listOf(word),
                currentIndex = 0,
                currentWord = word
            )
        }
        undoDelegate.saveUndoSnapshot()
        assertTrue(testStateUpdater.currentState.canUndo)

        undoDelegate.clearSnapshots()

        assertFalse(learningUndoHelper.canUndo())
        assertFalse(testStateUpdater.currentState.canUndo)
    }
}
