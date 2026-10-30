package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.feature.learning.domain.LearningQueueManager
import com.jian.nemo.feature.learning.domain.QueueSelectionResult
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.presentation.CardBadge
import com.jian.nemo.feature.learning.presentation.LearningItem
import com.jian.nemo.feature.learning.presentation.cardBadge
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.LearningStatus
import com.jian.nemo.feature.learning.presentation.SlideDirection
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 队列调度与导航域 delegate (系统设计 §3.2.M5 / US-4 / F4)
 *
 * 承接原 LearningViewModel：
 * - reQueueAtRelativeOffset / resumeFromWaiting / removeCurrentAndMoveNext
 * - handleSelectionResult（Empty/Wait/Next 三分支 + Learn Ahead 检测）
 * - navigateNext / navigatePrev / navigateTo（导航防抖 400ms）
 * - updateQueueBadgeCounts
 * - saveSessionState / saveCurrentSessionState / clearSession
 *
 * 与 StatsTimerDelegate 存在运行时环（QND.handleSelectionResult.Empty → STD.completeSession；
 * STD.completeSession → QND.clearSession），通过 lateinit 打破构造期环。
 */
class QueueNavigationDelegate(
    private val learningQueueManager: LearningQueueManager,
    private val settingsRepository: SettingsRepository,
    private val srsIntervalPreview: SrsIntervalPreview,
    private val sessionState: LearningSessionState,
    private val stateUpdater: StateUpdater,
    private val scope: CoroutineScope,
    private val settingsDelegate: SettingsDelegate,
) {
    // 打破 QND↔STD 构造环：STD 构造晚于 QND，由 ViewModel 在构造完成后注入
    internal lateinit var statsTimerDelegate: StatsTimerDelegate

    private var navigationJob
        get() = sessionState.navigationJob
        set(value) { sessionState.navigationJob = value }

    /** 对齐 Anki 的卡片重入队（基于时间戳与队列优先级重排，原 VM.reQueueAtRelativeOffset） */
    fun reQueueAtRelativeOffset(item: LearningItem, offset: Int = 0) {
        val state = stateUpdater.currentState
        val mode = state.learningMode

        // 保存状态 (Step & DueTime)
        sessionState.learningSteps[item.id] = item.step
        sessionState.learningDueTimes[item.id] = item.dueTime

        val now = System.currentTimeMillis()

        when (mode) {
            LearningMode.Word -> {
                if (item is LearningItem.WordItem) {
                    val currentIndex = state.currentIndex
                    val newList = learningQueueManager.reorderQueueAnkiStyle(
                        items = state.wordList,
                        currentIndex = currentIndex,
                        itemToRequeue = item.word,
                        getDueTime = { sessionState.learningDueTimes[it.id] ?: 0L },
                        now = now
                    )
                    val selection = learningQueueManager.selectNextItem(
                        items = newList,
                        getDueTime = { sessionState.learningDueTimes[it.id] ?: 0L },
                        now = now,
                        learnAheadLimitMs = sessionState.learnAheadLimitMs,
                        preferredIndex = 0
                    )
                    handleSelectionResult(selection, newList, state.selectedLevel, isWord = true)
                }
            }
            LearningMode.Grammar -> {
                if (item is LearningItem.GrammarItem) {
                    val currentIndex = state.currentGrammarIndex
                    val newList = learningQueueManager.reorderQueueAnkiStyle(
                        items = state.grammarList,
                        currentIndex = currentIndex,
                        itemToRequeue = item.grammar,
                        getDueTime = { sessionState.learningDueTimes[it.id] ?: 0L },
                        now = now
                    )
                    val selection = learningQueueManager.selectNextItem(
                        items = newList,
                        getDueTime = { sessionState.learningDueTimes[it.id] ?: 0L },
                        now = now,
                        learnAheadLimitMs = sessionState.learnAheadLimitMs,
                        preferredIndex = 0
                    )
                    handleSelectionResult(selection, newList, state.selectedLevel, isWord = false)
                }
            }
        }
    }

    /** 从等待状态恢复（原 VM.resumeFromWaiting） */
    fun resumeFromWaiting() {
        val state = stateUpdater.currentState
        val mode = state.learningMode

        stateUpdater.update { it.copy(waitingUntil = 0L) }

        // 用户主动点击"提前复习/立即学习"或倒计时结束：
        // 1. 先将整组卡片严格按原定到期时间戳升序排队（最早到期的排在最前）；
        // 2. 一次性解除当前列表所有卡片的内存冷却倒计时，避免背一个单词就弹一次等待打断用户；
        // 3. 从第 0 项开始如流水线般连贯出题。
        when (mode) {
            LearningMode.Word -> {
                val sortedList = learningQueueManager.sortAnkiStyle(
                    items = state.wordList,
                    getDueTime = { sessionState.learningDueTimes[it.id] ?: 0L },
                    now = Long.MAX_VALUE
                )
                for (item in sortedList) {
                    sessionState.learningDueTimes.remove(item.id)
                }
                val selection = if (sortedList.isNotEmpty()) {
                    QueueSelectionResult.Next(0, sortedList[0])
                } else {
                    QueueSelectionResult.Empty
                }
                handleSelectionResult(selection, sortedList, state.selectedLevel, isWord = true)
            }
            LearningMode.Grammar -> {
                val sortedList = learningQueueManager.sortAnkiStyle(
                    items = state.grammarList,
                    getDueTime = { sessionState.learningDueTimes[it.id] ?: 0L },
                    now = Long.MAX_VALUE
                )
                for (item in sortedList) {
                    sessionState.learningDueTimes.remove(item.id)
                }
                val selection = if (sortedList.isNotEmpty()) {
                    QueueSelectionResult.Next(0, sortedList[0])
                } else {
                    QueueSelectionResult.Empty
                }
                handleSelectionResult(selection, sortedList, state.selectedLevel, isWord = false)
            }
        }
    }

    /** 移出当前已毕业卡片并按 Anki 优先级平滑移动到下一个（原 VM.removeCurrentAndMoveNext） */
    fun removeCurrentAndMoveNext() {
        val state = stateUpdater.currentState
        val mode = state.learningMode
        val now = System.currentTimeMillis()

        when (mode) {
            LearningMode.Word -> {
                val currentIndex = state.currentIndex
                val remainingList = learningQueueManager.removeCurrent(state.wordList, currentIndex)
                val newList = learningQueueManager.sortAnkiStyle(
                    items = remainingList,
                    getDueTime = { sessionState.learningDueTimes[it.id] ?: 0L },
                    now = now
                )
                val selection = learningQueueManager.selectNextItem(
                    items = newList,
                    getDueTime = { sessionState.learningDueTimes[it.id] ?: 0L },
                    now = now,
                    learnAheadLimitMs = sessionState.learnAheadLimitMs,
                    preferredIndex = 0
                )
                handleSelectionResult(selection, newList, state.selectedLevel, isWord = true)
            }

            LearningMode.Grammar -> {
                val currentIndex = state.currentGrammarIndex
                val remainingList = learningQueueManager.removeCurrent(state.grammarList, currentIndex)
                val newList = learningQueueManager.sortAnkiStyle(
                    items = remainingList,
                    getDueTime = { sessionState.learningDueTimes[it.id] ?: 0L },
                    now = now
                )
                val selection = learningQueueManager.selectNextItem(
                    items = newList,
                    getDueTime = { sessionState.learningDueTimes[it.id] ?: 0L },
                    now = now,
                    learnAheadLimitMs = sessionState.learnAheadLimitMs,
                    preferredIndex = 0
                )
                handleSelectionResult(selection, newList, state.selectedLevel, isWord = false)
            }
        }
    }

    /** 处理队列选择结果（Empty/Wait/Next 三分支，原 VM.handleSelectionResult） */
    @Suppress("UNCHECKED_CAST")
    fun <T> handleSelectionResult(
        result: QueueSelectionResult<T>,
        newList: List<T>,
        level: String,
        isWord: Boolean
    ) {
        when (result) {
            is QueueSelectionResult.Empty -> {
                statsTimerDelegate.completeSession()
            }
            is QueueSelectionResult.Wait -> {
                println("等待中: Waiting until ${result.waitingUntil}")
                stateUpdater.update {
                    if (isWord) {
                        it.copy(
                            status = LearningStatus.Waiting,
                            wordList = newList as List<Word>,
                            waitingUntil = result.waitingUntil
                        )
                    } else {
                        it.copy(
                            status = LearningStatus.Waiting,
                            grammarList = newList as List<Grammar>,
                            waitingUntil = result.waitingUntil
                        )
                    }
                }
                // 保存当前进度位置，而非硬编码为 0
                // 恢复会话时会从此位置开始寻找下一个可用的卡片
                val currentIdx = if (isWord) stateUpdater.currentState.currentIndex else stateUpdater.currentState.currentGrammarIndex
                val ids = if (isWord) (newList as List<Word>).map { it.id } else (newList as List<Grammar>).map { it.id }
                saveSessionState(ids, currentIdx, level)
            }
            is QueueSelectionResult.Next -> {
                val nextIndex = result.index
                val nextItemObj = result.item

                if (isWord) {
                    val nextWord = nextItemObj as Word
                    // Anki Learn Ahead check was already done in manager
                    val due = sessionState.learningDueTimes[nextWord.id] ?: 0L
                    val isLearnAhead = due > System.currentTimeMillis()
                    if (isLearnAhead) {
                        println("提前学习 (Learn Ahead)")
                        sessionState.learningDueTimes.remove(nextWord.id)
                    }

                    val nextItem = LearningItem.WordItem(
                        nextWord,
                        step = sessionState.learningSteps[nextWord.id] ?: 0,
                        dueTime = if (isLearnAhead) 0L else due
                    )

                    stateUpdater.update {
                        it.copy(
                            status = LearningStatus.Learning,
                            wordList = newList as List<Word>,
                            currentIndex = nextIndex,
                            currentWord = nextWord,
                            isAnswerShown = false,
                            isCardFlipped = false,
                            ratingIntervals = calculateRatingIntervals(nextItem, srsIntervalPreview, sessionState),
                            slideDirection = SlideDirection.FORWARD
                        )
                    }
                    settingsDelegate.armShowAnswerDelay()
                    saveSessionState((newList as List<Word>).map { it.id }, nextIndex, level)
                } else {
                    val nextGrammar = nextItemObj as Grammar
                    val due = sessionState.learningDueTimes[nextGrammar.id] ?: 0L
                    val isLearnAhead = due > System.currentTimeMillis()
                    if (isLearnAhead) {
                        println("提前学习 (Learn Ahead)")
                        sessionState.learningDueTimes.remove(nextGrammar.id)
                    }

                    val nextItem = LearningItem.GrammarItem(
                        nextGrammar,
                        step = sessionState.learningSteps[nextGrammar.id] ?: 0,
                        dueTime = if (isLearnAhead) 0L else due
                    )

                    stateUpdater.update {
                        it.copy(
                            status = LearningStatus.Learning,
                            grammarList = newList as List<Grammar>,
                            currentGrammarIndex = nextIndex,
                            currentGrammar = nextGrammar,
                            isAnswerShown = false,
                            isGrammarDetailVisible = false,
                            ratingIntervals = calculateRatingIntervals(nextItem, srsIntervalPreview, sessionState),
                            slideDirection = SlideDirection.FORWARD
                        )
                    }
                    settingsDelegate.armShowAnswerDelay()
                    saveSessionState((newList as List<Grammar>).map { it.id }, nextIndex, level)
                }
            }
        }
        updateQueueBadgeCounts()
    }

    /** 导航到下一个（原 VM.navigateNext） */
    fun navigateNext() {
        val state = stateUpdater.currentState
        if (state.isNavigating || state.status == LearningStatus.Processing) return

        val listSize = when (state.learningMode) {
            LearningMode.Word -> state.wordList.size
            LearningMode.Grammar -> state.grammarList.size
        }
        val currentIndex = when (state.learningMode) {
            LearningMode.Word -> state.currentIndex
            LearningMode.Grammar -> state.currentGrammarIndex
        }

        val nextIndex = currentIndex + 1
        if (nextIndex >= listSize) return

        navigateTo(nextIndex)
    }

    /** 导航到上一个（原 VM.navigatePrev） */
    fun navigatePrev() {
        val state = stateUpdater.currentState
        if (state.isNavigating || state.status == LearningStatus.Processing) return

        val currentIndex = when (state.learningMode) {
            LearningMode.Word -> state.currentIndex
            LearningMode.Grammar -> state.currentGrammarIndex
        }

        val prevIndex = currentIndex - 1
        if (prevIndex < 0) return

        navigateTo(prevIndex)
    }

    /** 导航到指定索引（原 VM.navigateTo） */
    fun navigateTo(index: Int) {
        val state = stateUpdater.currentState
        if (state.isNavigating || state.status == LearningStatus.Processing) return

        navigationJob?.cancel()
        navigationJob = scope.launch {
            stateUpdater.update { it.copy(isNavigating = true) }

            when (state.learningMode) {
                LearningMode.Word -> {
                    if (index !in state.wordList.indices) {
                        stateUpdater.update { it.copy(isNavigating = false) }
                        return@launch
                    }

                    val direction = if (index > state.currentIndex) SlideDirection.FORWARD else SlideDirection.BACKWARD
                    val targetWord = state.wordList[index]
                    val targetItem = LearningItem.WordItem(targetWord)

                    stateUpdater.update {
                        it.copy(
                            currentIndex = index,
                            currentWord = targetWord,
                            isAnswerShown = false,
                            isCardFlipped = false,
                            ratingIntervals = calculateRatingIntervals(targetItem, srsIntervalPreview, sessionState),
                            slideDirection = direction
                        )
                    }
                    settingsDelegate.armShowAnswerDelay()

                    saveSessionState(state.wordList.map { it.id }, index, state.selectedLevel)
                }

                LearningMode.Grammar -> {
                    if (index !in state.grammarList.indices) {
                        stateUpdater.update { it.copy(isNavigating = false) }
                        return@launch
                    }

                    val direction = if (index > state.currentGrammarIndex) SlideDirection.FORWARD else SlideDirection.BACKWARD
                    val targetGrammar = state.grammarList[index]
                    val targetItem = LearningItem.GrammarItem(targetGrammar)

                    stateUpdater.update {
                        it.copy(
                            currentGrammarIndex = index,
                            currentGrammar = targetGrammar,
                            isAnswerShown = false,
                            isGrammarDetailVisible = false,
                            ratingIntervals = calculateRatingIntervals(targetItem, srsIntervalPreview, sessionState),
                            slideDirection = direction
                        )
                    }
                    settingsDelegate.armShowAnswerDelay()

                    saveSessionState(state.grammarList.map { it.id }, index, state.selectedLevel)
                }
            }

            delay(DebounceConstants.NAVIGATION_DEBOUNCE_MS)
            stateUpdater.update { it.copy(isNavigating = false) }
        }
    }

    /** 统计当前队列中各状态的数量并更新 UI State（原 VM.updateQueueBadgeCounts） */
    fun updateQueueBadgeCounts() {
        val state = stateUpdater.currentState
        val items: List<LearningItem> = when (state.learningMode) {
            LearningMode.Word -> state.wordList.map { LearningItem.WordItem(it) }
            LearningMode.Grammar -> state.grammarList.map { LearningItem.GrammarItem(it) }
        }

        var newCount = 0
        var learningCount = 0
        var reviewCount = 0
        var relearnCount = 0

        for (item in items) {
            when (item.cardBadge) {
                CardBadge.NEW -> newCount++
                CardBadge.LEARNING -> learningCount++
                CardBadge.REVIEW -> reviewCount++
                CardBadge.RELEARN -> relearnCount++
            }
        }

        stateUpdater.update {
            it.copy(
                queueNewCount = newCount,
                queueLearningCount = learningCount,
                queueReviewCount = reviewCount,
                queueRelearnCount = relearnCount
            )
        }
    }

    /** 保存会话状态到设置仓库（原 VM.saveSessionState） */
    fun saveSessionState(ids: List<Int>, index: Int, level: String) {
        val uiState = stateUpdater.currentState
        val waitingUntil = uiState.waitingUntil
        val isAnswerShown = uiState.isAnswerShown
        scope.launch {
            when (uiState.learningMode) {
                LearningMode.Word -> settingsRepository.saveWordSession(ids, index, level, sessionState.learningSteps, waitingUntil, isAnswerShown)
                LearningMode.Grammar -> settingsRepository.saveGrammarSession(ids, index, level, sessionState.learningSteps, waitingUntil, isAnswerShown)
            }
        }
    }

    /** 快捷保存当前会话状态（原 VM.saveCurrentSessionState） */
    fun saveCurrentSessionState() {
        val state = stateUpdater.currentState
        val isWord = state.learningMode == LearningMode.Word
        val ids = if (isWord) state.wordList.map { it.id } else state.grammarList.map { it.id }
        val index = if (isWord) state.currentIndex else state.currentGrammarIndex
        saveSessionState(ids, index, state.selectedLevel)
    }

    /** 清除会话持久化（原 VM.clearSession） */
    fun clearSession() {
        scope.launch {
            when (stateUpdater.currentState.learningMode) {
                LearningMode.Word -> settingsRepository.clearWordSession()
                LearningMode.Grammar -> settingsRepository.clearGrammarSession()
            }
        }
    }
}
