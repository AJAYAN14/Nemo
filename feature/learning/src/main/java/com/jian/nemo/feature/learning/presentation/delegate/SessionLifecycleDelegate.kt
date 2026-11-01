package com.jian.nemo.feature.learning.presentation.delegate

import android.content.Context
import androidx.annotation.StringRes
import com.jian.nemo.feature.learning.R
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.data.manager.CloudBackupManager
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
import com.jian.nemo.core.ui.component.dialog.BonusMode
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.SrsItem
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.feature.learning.domain.LearningSessionPolicy
import com.jian.nemo.feature.learning.domain.SavedSession
import com.jian.nemo.feature.learning.domain.SessionLoadResult
import com.jian.nemo.feature.learning.domain.SessionLoader
import com.jian.nemo.feature.learning.presentation.LearningItem
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.LearningStatus
import com.jian.nemo.feature.learning.presentation.LearningUiState
import com.jian.nemo.feature.learning.presentation.toLearningItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 会话生命周期编排域 delegate (系统设计 §3.2.M3 / US-2 / F2)
 *
 * 承接原 LearningViewModel：
 * - startLearning / startBonusLearning
 * - loadSavedSession / loadWordSession / loadGrammarSession
 * - handleSessionLoadResult（Restored / NewSession / Completed 三分支）
 * - changeLearningMode / exitLearning
 *
 * 第二阶段合并 X6（loadWordSession / loadGrammarSession 泛型统一）。
 */
class SessionLifecycleDelegate(
    private val sessionLoader: SessionLoader,
    private val learningSessionPolicy: LearningSessionPolicy,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val getNewWordsUseCase: GetNewWordsUseCase,
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val getLearningWordsUseCase: GetLearningWordsUseCase,
    private val getTodayLearnedWordsCountUseCase: GetTodayLearnedWordsCountUseCase,
    private val getNewGrammarsUseCase: GetNewGrammarsUseCase,
    private val getDueGrammarsUseCase: GetDueGrammarsUseCase,
    private val getLearningGrammarsUseCase: GetLearningGrammarsUseCase,
    private val getTodayLearnedGrammarsCountUseCase: GetTodayLearnedGrammarsCountUseCase,
    private val settingsRepository: SettingsRepository,
    private val cloudBackupManager: CloudBackupManager,
    private val sessionState: LearningSessionState,
    private val stateUpdater: StateUpdater,
    private val scope: CoroutineScope,
    private val settingsDelegate: SettingsDelegate,
    private val undoDelegate: UndoDelegate,
    private val statsTimerDelegate: StatsTimerDelegate,
    private val queueNavigationDelegate: QueueNavigationDelegate,
    private val srsIntervalPreview: com.jian.nemo.feature.learning.domain.SrsIntervalPreview,
    private val context: Context? = null,
) {
    private fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return context?.getString(resId, *formatArgs) ?: when (resId) {
            R.string.learning_session_err_start_failed -> "\u542f\u52a8\u5931\u8d25: ${formatArgs[0]}"
            R.string.learning_session_unit_word -> "\u8bcd"
            R.string.learning_session_unit_grammar -> "\u8bed\u6cd5"
            R.string.learning_session_bonus_empty_review_backlog -> "\u592a\u68d2\u4e86\uff01\u5f53\u524d\u5f85\u590d\u4e60\u79ef\u538b\u5df2\u5168\u90e8\u51fa\u6e05"
            R.string.learning_session_bonus_empty_new_items -> "\u6682\u65e0\u66f4\u591a\u672a\u5b66\u65b0${formatArgs[0]}"
            R.string.learning_session_bonus_label_review -> "\u5f85\u590d\u4e60"
            R.string.learning_session_bonus_label_new -> "\u65b0"
            R.string.learning_session_bonus_loaded_remaining -> "\u5df2\u4e3a\u4f60\u52a0\u8f7d\u5269\u4f59\u5168\u90e8 ${formatArgs[0]} \u4e2a${formatArgs[1]}${formatArgs[2]}"
            R.string.learning_session_err_bonus_start_failed -> "\u542f\u52a8\u52a0\u9910\u5931\u8d25: ${formatArgs[0]}"
            else -> ""
        }
    }
    private var loadingJob
        get() = sessionState.loadingJob
        set(value) { sessionState.loadingJob = value }

    /** 启动学习会话（原 VM.startLearning） */
    fun handleStart() {
        // 清空撤销快照
        undoDelegate.clearSnapshots()

        loadingJob?.cancel()
        loadingJob = scope.launch {
            stateUpdater.update {
                it.copy(
                    status = LearningStatus.Loading,
                    selectedLevel = "GLOBAL",
                    error = null
                )
            }

            try {
                val mode = stateUpdater.currentState.learningMode

                // 获取配置
                val dailyGoal = when (mode) {
                    LearningMode.Word -> settingsRepository.dailyGoalFlow.first()
                    LearningMode.Grammar -> settingsRepository.grammarDailyGoalFlow.first()
                }

                // 获取学习日重置时间并锁定当前学习日 (零点跨天保护)
                sessionState.resetHour = settingsRepository.learningDayResetHourFlow.first()
                sessionState.sessionLockedDay = DateTimeUtils.getLearningDay(sessionState.resetHour)
                println("会话锁定学习日: ${sessionState.sessionLockedDay} (重置时间: ${sessionState.resetHour}:00)")

                val completedToday = when (mode) {
                    LearningMode.Word -> {
                        getTodayLearnedWordsCountUseCase()
                            .first { it !is Result.Loading }
                            .let { if (it is Result.Success) it.data else 0 }
                    }
                    LearningMode.Grammar -> {
                        getTodayLearnedGrammarsCountUseCase()
                            .first { it !is Result.Loading }
                            .let { if (it is Result.Success) it.data else 0 }
                    }
                }

                // 获取已保存的会话
                val savedSession = loadSavedSession(mode)

                // 加载失败计数
                sessionState.lapseCounts.value = when (mode) {
                    LearningMode.Word -> settingsRepository.wordLapsesFlow.first()
                    LearningMode.Grammar -> settingsRepository.grammarLapsesFlow.first()
                }

                // 使用 SessionLoader 加载会话
                val result = when (mode) {
                    LearningMode.Word -> loadWordSession(dailyGoal, completedToday, savedSession)
                    LearningMode.Grammar -> loadGrammarSession(dailyGoal, completedToday, savedSession)
                }

                // 处理加载结果
                handleSessionLoadResult(result, dailyGoal, completedToday)

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                println("启动学习会话失败: ${e.message}")
                stateUpdater.update {
                    it.copy(
                        status = LearningStatus.Error,
                        error = getString(R.string.learning_session_err_start_failed, e.message ?: "")
                    )
                }
            }
        }
    }

    /** 启动加餐学习会话（原 VM.startBonusLearning） */
    fun handleBonusStart(count: Int, bonusMode: BonusMode = BonusMode.ReviewBacklog) {
        undoDelegate.clearSnapshots()

        loadingJob?.cancel()
        loadingJob = scope.launch {
            stateUpdater.update {
                it.copy(
                    status = LearningStatus.Loading,
                    selectedLevel = "GLOBAL",
                    error = null
                )
            }

            try {
                val mode = stateUpdater.currentState.learningMode
                val isRandom = settingsRepository.isRandomNewContentEnabledFlow.first()

                val completedToday = when (mode) {
                    LearningMode.Word -> {
                        getTodayLearnedWordsCountUseCase()
                            .first { it !is Result.Loading }
                            .let { if (it is Result.Success) it.data else 0 }
                    }
                    LearningMode.Grammar -> {
                        getTodayLearnedGrammarsCountUseCase()
                            .first { it !is Result.Loading }
                            .let { if (it is Result.Success) it.data else 0 }
                    }
                }

                val dailyGoal = when (mode) {
                    LearningMode.Word -> settingsRepository.dailyGoalFlow.first()
                    LearningMode.Grammar -> settingsRepository.grammarDailyGoalFlow.first()
                }

                val unit = if (mode == LearningMode.Word) getString(R.string.learning_session_unit_word) else getString(R.string.learning_session_unit_grammar)
                val bonusItems: List<Any> = when (bonusMode) {
                    BonusMode.ReviewBacklog -> {
                        val today = java.time.LocalDate.now().toEpochDay()
                        when (mode) {
                            LearningMode.Word -> {
                                val dueR = getDueWordsUseCase().first { it !is Result.Loading }
                                val dueList = if (dueR is Result.Success) dueR.data else emptyList()
                                val partition = learningSessionPolicy.partitionOverdueItems(dueList, today)
                                partition.activeItems.take(count)
                            }
                            LearningMode.Grammar -> {
                                val dueR = getDueGrammarsUseCase().first { it !is Result.Loading }
                                val dueList = if (dueR is Result.Success) dueR.data else emptyList()
                                val partition = learningSessionPolicy.partitionOverdueItems(dueList, today)
                                partition.activeItems.take(count)
                            }
                        }
                    }
                    BonusMode.NewItems -> {
                        when (mode) {
                            LearningMode.Word -> {
                                val r = getNewWordsUseCase(isRandom).first { it !is Result.Loading }
                                val list = if (r is Result.Success) r.data else emptyList()
                                list.take(count)
                            }
                            LearningMode.Grammar -> {
                                val r = getNewGrammarsUseCase(isRandom).first { it !is Result.Loading }
                                val list = if (r is Result.Success) r.data else emptyList()
                                list.take(count)
                            }
                        }
                    }
                }

                if (bonusItems.isEmpty()) {
                    val emptyMsg = when (bonusMode) {
                        BonusMode.ReviewBacklog -> getString(R.string.learning_session_bonus_empty_review_backlog)
                        BonusMode.NewItems -> getString(R.string.learning_session_bonus_empty_new_items, unit)
                    }
                    stateUpdater.update {
                        it.copy(
                            status = LearningStatus.SessionCompleted,
                            error = emptyMsg
                        )
                    }
                    return@launch
                }

                if (bonusItems.size < count) {
                    val modeLabel = if (bonusMode == BonusMode.ReviewBacklog) getString(R.string.learning_session_bonus_label_review) else getString(R.string.learning_session_bonus_label_new)
                    stateUpdater.update {
                        it.copy(successMessage = getString(R.string.learning_session_bonus_loaded_remaining, bonusItems.size, modeLabel, unit))
                    }
                }

                sessionState.learningSteps.clear()
                sessionState.learningDueTimes.clear()
                sessionState.requeuedItems.clear()

                statsTimerDelegate.checkAndRestoreOrReset()
                statsTimerDelegate.resumeStatsTimer()

                val learningItems = bonusItems.toLearningItems()
                val firstItem = learningItems.firstOrNull()

                stateUpdater.update {
                    it.copy(
                        status = LearningStatus.Learning,
                        wordList = if (mode == LearningMode.Word) bonusItems.filterIsInstance<Word>() else emptyList(),
                        grammarList = if (mode == LearningMode.Grammar) bonusItems.filterIsInstance<Grammar>() else emptyList(),
                        currentIndex = 0,
                        currentWord = (firstItem as? LearningItem.WordItem)?.word,
                        currentGrammar = (firstItem as? LearningItem.GrammarItem)?.grammar,
                        currentGrammarIndex = 0,
                        dailyGoal = dailyGoal,
                        completedToday = completedToday,
                        isAnswerShown = false,
                        isCardFlipped = false,
                        isGrammarDetailVisible = false,
                        error = null,
                        sessionInitialSize = learningItems.size,
                        ratingIntervals = calculateRatingIntervals(firstItem, srsIntervalPreview, sessionState),
                        sessionProcessedCount = 0,
                        waitingUntil = 0L,
                        sessionDurationSeconds = 0L,
                        sessionMaxCombo = statsTimerDelegate.maxCombo,
                        sessionNewCount = statsTimerDelegate.sessionNewCount,
                        sessionReviewCount = statsTimerDelegate.sessionReviewCount,
                        sessionRelearnCount = statsTimerDelegate.sessionRelearnCount
                    )
                }
                settingsDelegate.armShowAnswerDelay()
                queueNavigationDelegate.updateQueueBadgeCounts()

                queueNavigationDelegate.saveSessionState(learningItems.map { it.id }, 0, "GLOBAL")
                println("加餐会话成功启动: ${learningItems.size} 个新项目")

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                println("启动加餐会话失败: ${e.message}")
                stateUpdater.update {
                    it.copy(
                        status = LearningStatus.Error,
                        error = getString(R.string.learning_session_err_bonus_start_failed, e.message ?: "")
                    )
                }
            }
        }
    }

    private suspend fun loadSavedSession(mode: LearningMode): SavedSession? {
        val restored = when (mode) {
            LearningMode.Word -> settingsRepository.getWordSession().first()
            LearningMode.Grammar -> settingsRepository.getGrammarSession().first()
        }
        return restored?.let {
            SavedSession(
                ids = it.ids,
                index = it.currentIndex,
                level = it.level,
                steps = it.steps,
                waitingUntil = it.waitingUntil,
                isAnswerShown = it.isAnswerShown
            )
        }
    }

    /** 统一泛型会话加载（F13 / US-13 / R3 / X6 泛型合并） */
    private suspend fun <T : SrsItem> loadSession(
        dailyGoal: Int,
        completedToday: Int,
        savedSession: SavedSession?,
        getItemId: (T) -> Int,
        getTodayReviewedCount: suspend (Long) -> Int,
        getItemsByIds: suspend (List<Int>) -> List<T>,
        getDueItems: suspend () -> List<T>,
        getNewItems: suspend (Boolean) -> List<T>,
        getLearningItems: suspend () -> List<T>,
    ): SessionLoadResult<T> {
        val todayEpoch = sessionState.lockedDayOrToday()
        val sessionBatchSize = settingsRepository.sessionBatchSizeFlow.first()
        val backlogStrategy = settingsRepository.backlogNewItemStrategyFlow.first()
        val maxDailyReviews = settingsRepository.maxDailyReviewsFlow.first()
        val isRandom = settingsRepository.isRandomNewContentEnabledFlow.first()
        val todayReviewedCount = try {
            getTodayReviewedCount(todayEpoch)
        } catch (e: Exception) {
            0
        }

        return sessionLoader.loadSession(
            level = "GLOBAL",
            dailyGoal = dailyGoal,
            completedToday = completedToday,
            savedSession = savedSession,
            getItemsByIds = getItemsByIds,
            getDueItems = getDueItems,
            getNewItems = { getNewItems(isRandom) },
            getLearningItems = getLearningItems,
            getItemId = getItemId,
            filterByLevel = { true },
            isItemNew = { it.repetitionCount == 0 },
            sessionBatchSize = sessionBatchSize,
            backlogStrategy = backlogStrategy,
            maxDailyReviews = maxDailyReviews,
            todayReviewedCount = todayReviewedCount,
            todayEpochDay = todayEpoch
        )
    }

    private suspend fun loadWordSession(
        dailyGoal: Int,
        completedToday: Int,
        savedSession: SavedSession?
    ): SessionLoadResult<Word> = loadSession(
        dailyGoal = dailyGoal,
        completedToday = completedToday,
        savedSession = savedSession,
        getItemId = { it.id },
        getTodayReviewedCount = { todayEpoch -> wordRepository.getTodayReviewedWords(todayEpoch).first().size },
        getItemsByIds = { ids -> wordRepository.getWordsByIds(ids) },
        getDueItems = {
            val r = getDueWordsUseCase().first { it !is Result.Loading }
            if (r is Result.Success) r.data else emptyList()
        },
        getNewItems = { isRandom ->
            val r = getNewWordsUseCase(isRandom).first { it !is Result.Loading }
            if (r is Result.Success) r.data else emptyList()
        },
        getLearningItems = {
            val r = getLearningWordsUseCase().first { it !is Result.Loading }
            if (r is Result.Success) r.data else emptyList()
        }
    )

    private suspend fun loadGrammarSession(
        dailyGoal: Int,
        completedToday: Int,
        savedSession: SavedSession?
    ): SessionLoadResult<Grammar> = loadSession(
        dailyGoal = dailyGoal,
        completedToday = completedToday,
        savedSession = savedSession,
        getItemId = { it.id },
        getTodayReviewedCount = { todayEpoch -> grammarRepository.getTodayReviewedGrammars(todayEpoch).first().size },
        getItemsByIds = { ids -> grammarRepository.getGrammarsByIds(ids) },
        getDueItems = {
            val r = getDueGrammarsUseCase().first { it !is Result.Loading }
            if (r is Result.Success) r.data else emptyList()
        },
        getNewItems = { isRandom ->
            val r = getNewGrammarsUseCase(isRandom).first { it !is Result.Loading }
            if (r is Result.Success) r.data else emptyList()
        },
        getLearningItems = {
            val r = getLearningGrammarsUseCase().first { it !is Result.Loading }
            if (r is Result.Success) r.data else emptyList()
        }
    )

    private fun <T> handleSessionLoadResult(
        result: SessionLoadResult<T>,
        dailyGoal: Int,
        completedToday: Int
    ) {
        val mode = stateUpdater.currentState.learningMode

        when (result) {
            is SessionLoadResult.Restored -> {
                sessionState.learningSteps.clear()
                sessionState.learningSteps.putAll(result.steps)
                sessionState.learningDueTimes.clear()
                sessionState.requeuedItems.clear()

                // 恢复本次学习统计
                statsTimerDelegate.checkAndRestoreOrReset()
                statsTimerDelegate.resumeStatsTimer()

                val items = result.items.toLearningItems()
                val currentItem = items.getOrNull(result.index)

                stateUpdater.update {
                    it.copy(
                        wordList = if (mode == LearningMode.Word) result.items.filterIsInstance<Word>() else emptyList(),
                        grammarList = if (mode == LearningMode.Grammar) result.items.filterIsInstance<Grammar>() else emptyList(),
                        currentIndex = result.index,
                        currentWord = (currentItem as? LearningItem.WordItem)?.word,
                        currentGrammar = (currentItem as? LearningItem.GrammarItem)?.grammar,
                        currentGrammarIndex = if (mode == LearningMode.Grammar) result.index else 0,
                        dailyGoal = result.dailyGoal,
                        completedToday = result.completedToday,
                        isAnswerShown = result.isAnswerShown,
                        isCardFlipped = result.isAnswerShown,
                        isGrammarDetailVisible = false,
                        error = null,
                        sessionInitialSize = items.size,
                        ratingIntervals = calculateRatingIntervals(currentItem, srsIntervalPreview, sessionState),
                        sessionProcessedCount = result.index,
                        status = if (result.waitingUntil > System.currentTimeMillis()) LearningStatus.Waiting else LearningStatus.Learning,
                        waitingUntil = result.waitingUntil,
                        sessionDurationSeconds = 0L,
                        sessionMaxCombo = statsTimerDelegate.maxCombo,
                        sessionNewCount = statsTimerDelegate.sessionNewCount,
                        sessionReviewCount = statsTimerDelegate.sessionReviewCount,
                        sessionRelearnCount = statsTimerDelegate.sessionRelearnCount
                    )
                }
                settingsDelegate.armShowAnswerDelay()
                queueNavigationDelegate.updateQueueBadgeCounts()

                println("恢复学习会话: ${items.size} 个项目, 索引 ${result.index}")
            }

            is SessionLoadResult.NewSession -> {
                sessionState.learningSteps.clear()
                sessionState.learningDueTimes.clear()
                sessionState.requeuedItems.clear()

                statsTimerDelegate.checkAndRestoreOrReset()
                statsTimerDelegate.resumeStatsTimer()

                val items = result.items.toLearningItems()
                val firstItem = items.firstOrNull()

                stateUpdater.update {
                    it.copy(
                        status = LearningStatus.Learning,
                        wordList = if (mode == LearningMode.Word) result.items.filterIsInstance<Word>() else emptyList(),
                        grammarList = if (mode == LearningMode.Grammar) result.items.filterIsInstance<Grammar>() else emptyList(),
                        currentIndex = 0,
                        currentWord = (firstItem as? LearningItem.WordItem)?.word,
                        currentGrammar = (firstItem as? LearningItem.GrammarItem)?.grammar,
                        currentGrammarIndex = 0,
                        dailyGoal = result.dailyGoal,
                        completedToday = result.completedToday,
                        isAnswerShown = false,
                        isCardFlipped = false,
                        isGrammarDetailVisible = false,
                        error = null,
                        sessionInitialSize = items.size,
                        ratingIntervals = calculateRatingIntervals(firstItem, srsIntervalPreview, sessionState),
                        sessionProcessedCount = 0,
                        waitingUntil = 0L,
                        sessionDurationSeconds = 0L,
                        sessionMaxCombo = statsTimerDelegate.maxCombo,
                        sessionNewCount = statsTimerDelegate.sessionNewCount,
                        sessionReviewCount = statsTimerDelegate.sessionReviewCount,
                        sessionRelearnCount = statsTimerDelegate.sessionRelearnCount
                    )
                }
                settingsDelegate.armShowAnswerDelay()
                queueNavigationDelegate.updateQueueBadgeCounts()

                queueNavigationDelegate.saveSessionState(items.map { it.id }, 0, "GLOBAL")

                println("新学习会话: ${items.size} 个项目 (复习: ${result.dueCount}, 新: ${result.newCount})")
            }

            is SessionLoadResult.Completed -> {
                queueNavigationDelegate.clearSession()
                // 🔧 修复：先从本地缓存恢复今日战报数据，再结算
                statsTimerDelegate.checkAndRestoreOrReset()
                val todayEpoch = sessionState.lockedDayOrToday()
                val totalDuration = statsTimerDelegate.completeSessionStats(mode, todayEpoch)
                // 触发即时自动备份
                statsTimerDelegate.triggerCloudBackup()
                statsTimerDelegate.syncTodayCounts()
                stateUpdater.update {
                    it.copy(
                        status = LearningStatus.SessionCompleted,
                        dailyGoal = result.dailyGoal,
                        completedToday = result.completedToday,
                        wordList = emptyList(),
                        grammarList = emptyList(),
                        currentWord = null,
                        currentGrammar = null,
                        showAnswerAvailableAt = 0L,
                        waitingUntil = 0L,
                        sessionDurationSeconds = totalDuration,
                        sessionMaxCombo = statsTimerDelegate.maxCombo,
                        sessionNewCount = statsTimerDelegate.sessionNewCount,
                        sessionReviewCount = statsTimerDelegate.sessionReviewCount,
                        sessionRelearnCount = statsTimerDelegate.sessionRelearnCount
                    )
                }

                println("今日学习已完成")
            }
        }

        // 异步加载今日待复习与明日到期复习卡片预测
        val todayEpoch = sessionState.lockedDayOrToday()
        val tomorrowEpoch = todayEpoch + 1
        scope.launch {
            if (mode == LearningMode.Word) {
                launch {
                    wordRepository.getDueWordsCount(todayEpoch).collect { count ->
                        stateUpdater.update { it.copy(todayDueReviewCount = count) }
                    }
                }
                launch {
                    wordRepository.getDueWordsCount(tomorrowEpoch).collect { count ->
                        stateUpdater.update { it.copy(tomorrowReviewForecastCount = count) }
                    }
                }
            } else {
                launch {
                    grammarRepository.getDueGrammarsCount(todayEpoch).collect { count ->
                        stateUpdater.update { it.copy(todayDueReviewCount = count) }
                    }
                }
                launch {
                    grammarRepository.getDueGrammarsCount(tomorrowEpoch).collect { count ->
                        stateUpdater.update { it.copy(tomorrowReviewForecastCount = count) }
                    }
                }
            }
        }
    }

    /** 切换学习模式（原 VM.changeLearningMode） */
    fun changeLearningMode(mode: LearningMode) {
        stateUpdater.update { it.copy(learningMode = mode) }
        scope.launch { settingsDelegate.refreshAnswerDelayConfigForMode(mode) }
        // 模式切换时，立即触发新模式的会话加载
        handleStart()
    }

    /** 退出学习（原 VM.exitLearning） */
    fun exitLearning() {
        // 清空撤销快照
        undoDelegate.clearSnapshots()

        statsTimerDelegate.clearSessionTimer() // 退出时仅把本次会话开始时间置空，不抹掉今日已存的 word/grammar 累计记录

        stateUpdater.update { LearningUiState() }
    }
}
