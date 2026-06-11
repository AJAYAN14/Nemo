package com.jian.nemo.feature.learning.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.service.SyncService
import com.jian.nemo.core.domain.service.SrsCalculator
import com.jian.nemo.core.domain.usecase.grammar.GetDueGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetNewGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetTodayLearnedGrammarsCountUseCase
import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.word.GetDueWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetNewWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetTodayLearnedWordsCountUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.core.domain.repository.ContentReportRepository
import com.jian.nemo.feature.learning.domain.LearningSessionPolicy
import com.jian.nemo.feature.learning.domain.LearningSessionStatsManager
import com.jian.nemo.feature.learning.domain.LearningQueueManager
import com.jian.nemo.feature.learning.domain.LearningScheduler
import com.jian.nemo.feature.learning.domain.LearningUndoHelper
import com.jian.nemo.feature.learning.domain.QueueSelectionResult
import com.jian.nemo.feature.learning.domain.SavedSession
import com.jian.nemo.feature.learning.domain.ScheduleResult
import com.jian.nemo.feature.learning.domain.UndoSnapshot
import com.jian.nemo.feature.learning.domain.SessionLoadResult
import com.jian.nemo.feature.learning.domain.SessionLoader
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.domain.LearningTtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import javax.inject.Inject



/**
 * 核心设计原则：
 * 1. 统一处理 Word 和 Grammar
 * 2. 自由导航（允许前后浏览）
 * 3. 简化评分流转逻辑
 * 4. 钉子户保护机制
 */
@HiltViewModel
class LearningViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    // Word UseCases
    private val getNewWordsUseCase: GetNewWordsUseCase,
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val getTodayLearnedWordsCountUseCase: GetTodayLearnedWordsCountUseCase,
    // Grammar UseCases
    private val getNewGrammarsUseCase: GetNewGrammarsUseCase,
    private val getDueGrammarsUseCase: GetDueGrammarsUseCase,
    private val updateGrammarUseCase: UpdateGrammarUseCase,
    private val getTodayLearnedGrammarsCountUseCase: GetTodayLearnedGrammarsCountUseCase,
    // Repositories
    private val settingsRepository: SettingsRepository,
    private val studyRecordRepository: StudyRecordRepository,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    // Services
    private val srsCalculator: SrsCalculator,
    private val syncService: SyncService,
    // Domain
    private val learningSessionPolicy: LearningSessionPolicy,

    private val srsIntervalPreview: SrsIntervalPreview,
    private val sessionLoader: SessionLoader,
    private val learningQueueManager: LearningQueueManager,
    private val learningScheduler: LearningScheduler,
    private val learningUndoHelper: LearningUndoHelper,
    private val learningTtsManager: LearningTtsManager,
    private val contentReportRepository: ContentReportRepository
) : ViewModel() {
    companion object {
        /** 导航防抖延迟 (ms) */
        private const val NAVIGATION_DEBOUNCE_MS = 400L

        /** 评分防抖延迟 (ms) */
        private const val RATING_DEBOUNCE_MS = 300L

        /** 点击边界容差，避免倒计时到点仍被拦截 */
        private const val SHOW_ANSWER_GRACE_MS = 150L
    }

    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    /** 学习阶段追踪 (ItemId -> StepIndex) */
    private val _learningSteps = mutableMapOf<Int, Int>()

    /** Anki-Mode: 到期时间记录 (ID -> DueTime Epoch Millis) */
    private val _learningDueTimes = mutableMapOf<Int, Long>()

    /** 失败次数追踪 (用于钉子户检测)，使用 StateFlow 确保并发安全 */
    private val _lapseCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())

    /** 重入队标记 (用于红标显示) */
    private val _requeuedItems = mutableSetOf<Int>()

    /** 会话锁定的学习日 (用于零点跨天保护) */
    private var _sessionLockedDay: Long? = null

    /** 结算战报与持久化逻辑管理器 */
    private val sessionStatsManager = LearningSessionStatsManager(context)

    /** 学习日重置时间 (小时) */
    private var _resetHour: Int = 4
    private var _leechThreshold: Int = 5

    /** 学习步进配置 (分钟列表) */
    private var _learningStepsConfig: List<Int> = listOf(1, 10)

    /** 重学步进配置 (分钟列表) */
    private var _relearningStepsConfig: List<Int> = listOf(1, 10)

    /** 提前学习限制 (毫秒) */
    private var _learnAheadLimitMs: Long = 20 * 60 * 1000L

    /** 显示答案等待时长 (毫秒) */
    private var _showAnswerDelayMs: Long = 5000L

    private var ratingJob: Job? = null
    private var navigationJob: Job? = null
    private var loadingJob: Job? = null

    init {
        // 0. 监听显示模式设置 (深色/浅色)
        viewModelScope.launch {
            settingsRepository.isDarkModeFlow.collect { isDark ->
                _uiState.update { it.copy(isDarkMode = isDark) }
            }
        }

        // 监听 Leech 阈值设置
        viewModelScope.launch {
            settingsRepository.leechThresholdFlow.collect { threshold ->
                _leechThreshold = threshold.coerceAtLeast(1)
            }
        }

        // 1. 监听 TTS 事件以更新播放状态 (必须在 _uiState 初始化后)
        learningTtsManager.observeTtsEvents(viewModelScope)
        viewModelScope.launch {
            learningTtsManager.playingAudioId.collect { id ->
                _uiState.update { it.copy(playingAudioId = id) }
            }
        }


        // 监听设置变化
        viewModelScope.launch {
            // 监听每日目标变化 - 豁免机制 (Hot-swap)
            // 设计文档 7.4: 学习中途改了每日目标会怎样？
            // 不强制结束当前会话，只更新目标并提示
            launch {
                settingsRepository.dailyGoalFlow.distinctUntilChanged().collect { newGoal ->
                    val state = _uiState.value
                    if (state.learningMode == LearningMode.Word && state.status == LearningStatus.Learning) {
                        handleDailyGoalChange(newGoal)
                    }
                }
            }

            launch {
                settingsRepository.grammarDailyGoalFlow.distinctUntilChanged().collect { newGoal ->
                    val state = _uiState.value
                    if (state.learningMode == LearningMode.Grammar && state.status == LearningStatus.Learning) {
                        handleDailyGoalChange(newGoal)
                    }
                }
            }

            // 监听失败次数
            launch {
                settingsRepository.wordLapsesFlow.collect { lapses ->
                    if (_uiState.value.learningMode == LearningMode.Word) {
                        _lapseCounts.value = lapses
                    }
                }
            }

            launch {
                settingsRepository.grammarLapsesFlow.collect { lapses ->
                    if (_uiState.value.learningMode == LearningMode.Grammar) {
                        _lapseCounts.value = lapses
                    }
                }
            }

            // 监听学习配置
            launch {
                settingsRepository.learningStepsFlow.collect { stepsStr ->
                    _learningStepsConfig = parseSteps(stepsStr)
                    println("学习步进更新: $_learningStepsConfig (mins)")
                }
            }

            launch {
                settingsRepository.relearningStepsFlow.collect { stepsStr ->
                    _relearningStepsConfig = parseSteps(stepsStr)
                    println("重学步进更新: $_relearningStepsConfig (mins)")
                }
            }

            launch {
                settingsRepository.learnAheadLimitFlow.collect { limit ->
                    _learnAheadLimitMs = limit * 60 * 1000L
                    println("提前学习限制更新: $limit mins")
                }
            }

            // 监听自动朗读设置
            launch {
                settingsRepository.isAutoPlayAudioEnabledFlow.collect { enabled ->
                    _uiState.update { it.copy(isAutoAudioEnabled = enabled) }
                }
            }

            // 监听显示答案等待开关
            launch {
                settingsRepository.isShowAnswerDelayEnabledFlow.collect { enabled ->
                    _uiState.update { it.copy(isShowAnswerDelayEnabled = enabled) }
                    armShowAnswerDelay()
                }
            }

            // 监听显示答案等待时长
            launch {
                settingsRepository.showAnswerDelayMsFlow.collect { delayMs ->
                    _showAnswerDelayMs = delayMs
                    _uiState.update { it.copy(showAnswerDelayMs = delayMs) }
                    armShowAnswerDelay()
                }
            }

            // 监听自动翻面开关 (Word)
            launch {
                settingsRepository.isAutoRevealAnswerEnabledFlow.collect { enabled ->
                    _uiState.update { it.copy(isAutoRevealAnswerEnabled = enabled) }
                }
            }

            // 监听自动翻面时长 (Word)
            launch {
                settingsRepository.autoRevealAnswerMsFlow.collect { delayMs ->
                    _uiState.update { it.copy(autoRevealAnswerMs = delayMs) }
                }
            }

            // 监听自动翻面开关 (Grammar)
            launch {
                settingsRepository.isGrammarAutoRevealAnswerEnabledFlow.collect { enabled ->
                    _uiState.update { it.copy(isGrammarAutoRevealAnswerEnabled = enabled) }
                }
            }

            // 监听自动翻面时长 (Grammar)
            launch {
                settingsRepository.grammarAutoRevealAnswerMsFlow.collect { delayMs ->
                    _uiState.update { it.copy(grammarAutoRevealAnswerMs = delayMs) }
                }
            }
        }
    }

    /**
     * 处理每日目标变化 - 豁免机制 (Hot-swap)
     */
    private fun handleDailyGoalChange(newGoal: Int) {
        val state = _uiState.value
        val completedToday = state.completedToday

        _uiState.update { it.copy(dailyGoal = newGoal) }

        if (state.status == LearningStatus.Learning || state.status == LearningStatus.Waiting) {
            println("每日目标变更 ($newGoal)，正在触发热重载以同步队列...")
            
            // 1. 立即保存当前进度到 DataStore，确保重载时能恢复到当前位置
            saveSessionState(
                ids = if (state.learningMode == LearningMode.Word) state.wordList.map { it.id } else state.grammarList.map { it.id },
                index = state.currentIndex,
                level = state.selectedLevel
            )

            // 2. 重新触发启动逻辑（利用 SessionLoader 的恢复模式进行动态裁剪）
            startLearning()
        }

        if (completedToday >= newGoal) {
            println("豁免机制: 目标改为 $newGoal，已学 $completedToday，今日已达标")
            _uiState.update {
                it.copy(error = "今日目标已达标！可继续学习或退出")
            }
        }
    }

    /**
     * 处理所有 UI 事件
     */
    fun onEvent(event: LearningEvent) {
        when (event) {
            is LearningEvent.StartLearning -> {
                _uiState.update { it.copy(learningMode = event.mode) }
                startLearning()
            }
            is LearningEvent.ChangeLearningMode -> changeLearningMode(event.mode)


            // 卡片交互
            is LearningEvent.FlipCard -> showAnswer()
            is LearningEvent.ShowAnswer -> showAnswer()

            // 评分 (统一处理)
            is LearningEvent.RateWord -> rate(event.quality)
            is LearningEvent.RateGrammar -> rate(event.quality)

            // 导航
            is LearningEvent.NavigateNext -> navigateNext()
            is LearningEvent.NavigatePrev -> navigatePrev()
            is LearningEvent.GoToIndex -> navigateTo(event.index)

            // 其他
            is LearningEvent.ShowTypingPractice -> _uiState.update { it.copy(showTypingPractice = true) }
            is LearningEvent.HideTypingPractice -> _uiState.update { it.copy(showTypingPractice = false) }
            is LearningEvent.Retry -> startLearning()
            is LearningEvent.Undo -> undo()
            is LearningEvent.DismissUndo -> {
                // 仅关闭提示，不清空撤销快照。
                // 这样用户即使错过 Snackbar，也能从菜单继续撤销上一次评分。
            }
            is LearningEvent.ResumeFromWaiting -> resumeFromWaiting()
            is LearningEvent.SuspendCurrent -> suspendCurrentItem()
            is LearningEvent.BuryCurrent -> buryCurrentItem()
            is LearningEvent.ExitLearning -> exitLearning()
            is LearningEvent.PauseTimer -> pauseTimer()
            is LearningEvent.ResumeTimer -> resumeTimer()

            // 语法专用
            is LearningEvent.ToggleGrammarDetail -> toggleGrammarDetail()
            is LearningEvent.LoadPreviousGrammar -> navigatePrev()
            is LearningEvent.LoadNextGrammar -> navigateNext()
            is LearningEvent.MasterGrammar -> rate(5) // 掌握 = 评分5

            // TTS 朗读
            is LearningEvent.SpeakWord -> learningTtsManager.speakWord(event.text, event.chinese)
            is LearningEvent.SpeakExample -> learningTtsManager.speakExample(event.japanese, event.chinese, event.id)
            is LearningEvent.ToggleAutoPlayAudio -> toggleAutoPlayAudio(event.enabled)
            is LearningEvent.ToggleShowAnswerDelay -> toggleShowAnswerDelay(event.enabled)
            is LearningEvent.CycleShowAnswerDelayDuration -> cycleShowAnswerDelayDuration()
            is LearningEvent.ToggleAutoRevealAnswer -> toggleAutoRevealAnswer(event.enabled)
            is LearningEvent.CycleAutoRevealAnswerDuration -> cycleAutoRevealAnswerDuration()
            is LearningEvent.ToggleGrammarAutoRevealAnswer -> toggleGrammarAutoRevealAnswer(event.enabled)
            is LearningEvent.CycleGrammarAutoRevealAnswerDuration -> cycleGrammarAutoRevealAnswerDuration()
            is LearningEvent.ReportContentError -> handleReportError(event.errorType, event.description)
            is LearningEvent.OpenReportErrorDialog -> _uiState.update { it.copy(showReportErrorDialog = true) }
            is LearningEvent.CancelReportErrorDialog -> _uiState.update { it.copy(showReportErrorDialog = false) }
            is LearningEvent.ClearSuccessMessage -> _uiState.update { it.copy(successMessage = null) }
            is LearningEvent.ClearErrorMessage -> _uiState.update { it.copy(error = null) }
            is LearningEvent.SetDarkMode -> {
                viewModelScope.launch {
                    settingsRepository.setDarkMode(event.enabled)
                }
            }
            is LearningEvent.CycleDarkMode -> {
                val nextMode = when (uiState.value.isDarkMode) {
                    null -> false  // 系统 -> 浅色
                    false -> true  // 浅色 -> 深色
                    true -> null   // 深色 -> 系统
                }
                viewModelScope.launch {
                    settingsRepository.setDarkMode(nextMode)
                }
            }
            is LearningEvent.ToggleWhiteboard -> {
                _uiState.update { it.copy(isWhiteboardEnabled = event.enabled) }
            }
        }
    }

    private fun handleReportError(errorType: String, description: String?) {
        // 关闭对话框
        _uiState.update { it.copy(showReportErrorDialog = false) }

        val state = _uiState.value
        val itemId = if (state.learningMode == LearningMode.Word) {
            state.currentWord?.id
        } else {
            state.currentGrammar?.id
        } ?: return
        
        val itemType = if (state.learningMode == LearningMode.Word) "word" else "grammar"

        viewModelScope.launch {
            val result = contentReportRepository.reportContentError(itemId, itemType, errorType, description)
            if (result is Result.Success) {
                // 使用通用的成功消息提示
                _uiState.update { it.copy(successMessage = "反馈成功，感谢您的反馈！") }
            } else if (result is Result.Error) {
                val message = result.exception.message ?: ""
                if (message.contains("unique_user_item_report")) {
                    _uiState.update { it.copy(error = "您已经提交过对此类型问题的反馈了，请勿重复提交。") }
                } else {
                    _uiState.update { it.copy(error = "反馈失败: $message") }
                }
            }
        }
    }

    /**
     * 启动学习会话
     */
    private fun startLearning() {
        // 清空撤销快照
        learningUndoHelper.clear()
        syncUndoAvailability()

        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    status = LearningStatus.Loading,
                    selectedLevel = "GLOBAL",
                    error = null
                )
            }

            try {
                val mode = _uiState.value.learningMode

                // 获取配置
                val dailyGoal = when (mode) {
                    LearningMode.Word -> settingsRepository.dailyGoalFlow.first()
                    LearningMode.Grammar -> settingsRepository.grammarDailyGoalFlow.first()
                }

                // 获取学习日重置时间并锁定当前学习日 (零点跨天保护)
                _resetHour = settingsRepository.learningDayResetHourFlow.first()
                _sessionLockedDay = DateTimeUtils.getLearningDay(_resetHour)
                println("会话锁定学习日: $_sessionLockedDay (重置时间: $_resetHour:00)")

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
                _lapseCounts.value = when (mode) {
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
                _uiState.update {
                    it.copy(
                        status = LearningStatus.Error,
                        error = "启动失败: ${e.message}"
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

    private suspend fun loadWordSession(
        dailyGoal: Int,
        completedToday: Int,
        savedSession: SavedSession?
    ): SessionLoadResult<Word> {
        return sessionLoader.loadSession(
            level = "GLOBAL",
            dailyGoal = dailyGoal,
            completedToday = completedToday,
            savedSession = savedSession,
            getItemsByIds = { ids -> wordRepository.getWordsByIds(ids) },
            getDueItems = {
                val r = getDueWordsUseCase().first { it !is Result.Loading }
                if (r is Result.Success) r.data else emptyList()
            },
            getNewItems = {
                val isRandom = settingsRepository.isRandomNewContentEnabledFlow.first()
                val r = getNewWordsUseCase(isRandom).first { it !is Result.Loading }
                if (r is Result.Success) r.data else emptyList()
            },
            getItemId = { it.id },
            filterByLevel = { true }, // 全局进阶不再按等级过滤
            isItemNew = { it.repetitionCount == 0 }
        )
    }

    private suspend fun loadGrammarSession(
        dailyGoal: Int,
        completedToday: Int,
        savedSession: SavedSession?
    ): SessionLoadResult<Grammar> {
        return sessionLoader.loadSession(
            level = "GLOBAL",
            dailyGoal = dailyGoal,
            completedToday = completedToday,
            savedSession = savedSession,
            getItemsByIds = { ids -> grammarRepository.getGrammarsByIds(ids) },
            getDueItems = {
                val r = getDueGrammarsUseCase().first { it !is Result.Loading }
                if (r is Result.Success) r.data else emptyList()
            },
            getNewItems = {
                val isRandom = settingsRepository.isRandomNewContentEnabledFlow.first()
                val r = getNewGrammarsUseCase(isRandom).first { it !is Result.Loading }
                if (r is Result.Success) r.data else emptyList()
            },
            getItemId = { it.id },
            filterByLevel = { true }, // 全局进阶不再按等级过滤
            isItemNew = { it.repetitionCount == 0 }
        )
    }

    private fun <T> handleSessionLoadResult(
        result: SessionLoadResult<T>,
        dailyGoal: Int,
        completedToday: Int
    ) {
        val mode = _uiState.value.learningMode

        when (result) {
            is SessionLoadResult.Restored -> {
                _learningSteps.clear()
                _learningSteps.putAll(result.steps)
                _learningDueTimes.clear() // 清除旧模式或旧会话的到期时间缓存
                _requeuedItems.clear()

                // 恢复本次学习统计
                checkAndRestoreOrReset()
                sessionStatsManager.resumeTimer()

                val items = result.items.toLearningItems()
                val currentItem = items.getOrNull(result.index)

                _uiState.update {
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
                        ratingIntervals = calculateRatingIntervals(currentItem),
                        sessionProcessedCount = result.index,
                        status = if (result.waitingUntil > System.currentTimeMillis()) LearningStatus.Waiting else LearningStatus.Learning,
                        waitingUntil = result.waitingUntil,
                        // 恢复本地持久化及展示UI上的战报统计
                        sessionDurationSeconds = 0L,
                        sessionMaxCombo = sessionStatsManager.maxCombo,
                        sessionNewCount = sessionStatsManager.sessionNewCount,
                        sessionReviewCount = sessionStatsManager.sessionReviewCount,
                        sessionRelearnCount = sessionStatsManager.sessionRelearnCount
                    )
                }
                armShowAnswerDelay()
                updateQueueBadgeCounts()

                println("恢复学习会话: ${items.size} 个项目, 索引 ${result.index}")
            }

            is SessionLoadResult.NewSession -> {
                _learningSteps.clear()
                _learningDueTimes.clear() // 清除旧模式或旧会话的到期时间缓存
                _requeuedItems.clear()

                // 恢复/重置学习统计并启动计时
                checkAndRestoreOrReset()
                sessionStatsManager.resumeTimer()

                val items = result.items.toLearningItems()
                val firstItem = items.firstOrNull()

                _uiState.update {
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
                        ratingIntervals = calculateRatingIntervals(firstItem),
                        sessionProcessedCount = 0,
                        waitingUntil = 0L,
                        // 恢复本地持久化及展示UI上的战报统计
                        sessionDurationSeconds = 0L,
                        sessionMaxCombo = sessionStatsManager.maxCombo,
                        sessionNewCount = sessionStatsManager.sessionNewCount,
                        sessionReviewCount = sessionStatsManager.sessionReviewCount,
                        sessionRelearnCount = sessionStatsManager.sessionRelearnCount
                    )
                }
                armShowAnswerDelay()
                updateQueueBadgeCounts()

                // 保存初始会话
                saveSessionState(items.map { it.id }, 0, "GLOBAL")

                println("新学习会话: ${items.size} 个项目 (复习: ${result.dueCount}, 新: ${result.newCount})")
            }

            is SessionLoadResult.Completed -> {
                clearSession()
                // 🔧 修复：先从本地缓存恢复今日战报数据，再结算
                checkAndRestoreOrReset()
                val todayEpoch = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
                val totalDuration = sessionStatsManager.completeSession(mode, todayEpoch)
                _uiState.update {
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
                        // 🔧 修复：恢复战报统计数据到 UI
                        sessionMaxCombo = sessionStatsManager.maxCombo,
                        sessionNewCount = sessionStatsManager.sessionNewCount,
                        sessionReviewCount = sessionStatsManager.sessionReviewCount,
                        sessionRelearnCount = sessionStatsManager.sessionRelearnCount
                    )
                }

                println("今日学习已完成")
            }
        }

        // 异步加载明日到期复习卡片预测
        val todayEpoch = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
        val tomorrowEpoch = todayEpoch + 1
        viewModelScope.launch {
            if (mode == LearningMode.Word) {
                wordRepository.getDueWordsCount(tomorrowEpoch).collect { count ->
                    _uiState.update { it.copy(tomorrowReviewForecastCount = count) }
                }
            } else {
                grammarRepository.getDueGrammarsCount(tomorrowEpoch).collect { count ->
                    _uiState.update { it.copy(tomorrowReviewForecastCount = count) }
                }
            }
        }
    }



    /**
     * 显示答案
     * 点击"显示答案"后，卡片翻转或展开
     */
    private fun showAnswer() {
        if (_uiState.value.status == LearningStatus.Processing) return

        val state = _uiState.value
        if (state.isShowAnswerDelayEnabled && System.currentTimeMillis() + SHOW_ANSWER_GRACE_MS < state.showAnswerAvailableAt) {
            return
        }

        _uiState.update { it.copy(isAnswerShown = true, isCardFlipped = true, showAnswerAvailableAt = 0L) }

        // 自动朗读逻辑
        if (_uiState.value.isAutoAudioEnabled) {
             val state = _uiState.value
             // 仅在单词模式下自动朗读
             if (state.learningMode == LearningMode.Word) {
                 state.currentWord?.let { word ->
                     learningTtsManager.speakWord(word.hiragana, word.chinese) // 朗读假名/发音 + 中文含义
                 }

             }
        }

        // 保存当前会话状态（主要是为了记录 isAnswerShown = true）
        saveCurrentSessionState()
    }

    private fun toggleAutoPlayAudio(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoPlayAudioEnabled(enabled)
        }
    }

    private fun toggleShowAnswerDelay(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowAnswerDelayEnabled(enabled)
        }
    }

    private fun cycleShowAnswerDelayDuration() {
        val next = when (_showAnswerDelayMs) {
            2000L -> 3000L
            3000L -> 4000L
            4000L -> 5000L
            else -> 2000L
        }
        viewModelScope.launch {
            settingsRepository.setShowAnswerDelayMs(next)
        }
    }

    private fun toggleAutoRevealAnswer(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoRevealAnswerEnabled(enabled)
        }
    }

    private fun cycleAutoRevealAnswerDuration() {
        val current = _uiState.value.autoRevealAnswerMs
        val next = when (current) {
            5000L -> 6000L
            6000L -> 7000L
            7000L -> 8000L
            8000L -> 9000L
            9000L -> 10000L
            else -> 5000L
        }
        viewModelScope.launch {
            settingsRepository.setAutoRevealAnswerMs(next)
        }
    }

    private fun toggleGrammarAutoRevealAnswer(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGrammarAutoRevealAnswerEnabled(enabled)
        }
    }

    private fun cycleGrammarAutoRevealAnswerDuration() {
        val current = _uiState.value.grammarAutoRevealAnswerMs
        val next = when (current) {
            10000L -> 11000L
            11000L -> 12000L
            12000L -> 13000L
            13000L -> 14000L
            14000L -> 15000L
            else -> 10000L
        }
        viewModelScope.launch {
            settingsRepository.setGrammarAutoRevealAnswerMs(next)
        }
    }

    private fun armShowAnswerDelay() {
        val state = _uiState.value
        if (!state.isShowAnswerDelayEnabled || state.isAnswerShown || !state.hasPendingItems || state.status != LearningStatus.Learning) {
            _uiState.update { it.copy(showAnswerAvailableAt = 0L) }
            return
        }

        _uiState.update { it.copy(showAnswerAvailableAt = System.currentTimeMillis() + _showAnswerDelayMs) }
    }

    /**
     * 保存撤销快照
     */
    private fun saveUndoSnapshot() {
        val state = _uiState.value
        val currentItem = getCurrentItem() ?: return

        val snapshot = UndoSnapshot(
            item = currentItem,
            originalWord = (currentItem as? LearningItem.WordItem)?.word,
            originalGrammar = (currentItem as? LearningItem.GrammarItem)?.grammar,
            wordList = state.wordList.toList(),
            grammarList = state.grammarList.toList(),
            currentIndex = state.currentIndex,
            currentGrammarIndex = state.currentGrammarIndex,
            learningSteps = _learningSteps.toMap(),
            lapseCounts = _lapseCounts.value.toMap(),
            requeuedItems = _requeuedItems.toSet(),
            learningDueTimes = _learningDueTimes.toMap(),
            completedToday = state.completedToday,
            completedThisSession = state.completedThisSession,
            sessionProcessedCount = state.sessionProcessedCount,
            wasNew = currentItem.isNew
        )

        learningUndoHelper.saveSnapshot(snapshot)
        syncUndoAvailability()

        println("快照已保存: ${currentItem.displayName}")
    }

    /**
     * 同步撤销可用性 (单一真相源: LearningUndoHelper)
     */
    private fun syncUndoAvailability() {
        val canUndo = learningUndoHelper.canUndo()
        _uiState.update { it.copy(canUndo = canUndo, undoEventTrigger = it.undoEventTrigger + 1) }
    }

    /**
     * 撤销上一次评分
     */
    private fun undo() {
        if (_uiState.value.status == LearningStatus.Processing) return

        val snapshot = learningUndoHelper.popSnapshot() ?: run {
            syncUndoAvailability()
            return
        }
        // 快照已被消费，立即刷新可撤销状态，避免快速重复点击。
        syncUndoAvailability()

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(status = LearningStatus.Processing) }

                // 恢复数据库状态
                when (snapshot.item) {
                    is LearningItem.WordItem -> {
                        snapshot.originalWord?.let { updateWordUseCase(it) }
                        if (snapshot.wasNew) {
                            studyRecordRepository.incrementLearnedWords(-1)
                        } else {
                            studyRecordRepository.incrementReviewedWords(-1)
                        }
                    }
                    is LearningItem.GrammarItem -> {
                        snapshot.originalGrammar?.let { updateGrammarUseCase(it) }
                        if (snapshot.wasNew) {
                            studyRecordRepository.incrementLearnedGrammars(-1)
                        } else {
                            studyRecordRepository.incrementReviewedGrammars(-1)
                        }
                    }
                }

                // 恢复 ViewModel 内部状态
                _learningSteps.clear()
                _learningSteps.putAll(snapshot.learningSteps)
                _lapseCounts.value = snapshot.lapseCounts
                _requeuedItems.clear()
                _requeuedItems.addAll(snapshot.requeuedItems)
                _learningDueTimes.clear()
                _learningDueTimes.putAll(snapshot.learningDueTimes)

                // 恢复 UI 状态
                val mode = _uiState.value.learningMode
                _uiState.update {
                    it.copy(
                        status = LearningStatus.Learning,
                        wordList = snapshot.wordList,
                        grammarList = snapshot.grammarList,
                        currentIndex = snapshot.currentIndex,
                        currentGrammarIndex = snapshot.currentGrammarIndex,
                        currentWord = snapshot.originalWord,
                        currentGrammar = snapshot.originalGrammar,
                        completedToday = snapshot.completedToday,
                        completedThisSession = snapshot.completedThisSession,
                        sessionProcessedCount = snapshot.sessionProcessedCount,
                        isAnswerShown = true,  // 撤销后显示答案面
                        isCardFlipped = true,
                        ratingIntervals = calculateRatingIntervals(snapshot.item),
                        slideDirection = SlideDirection.BACKWARD
                    )
                }
                updateQueueBadgeCounts()
                syncUndoAvailability()

                // 保存会话状态
                val ids = if (mode == LearningMode.Word)
                    snapshot.wordList.map { it.id }
                else
                    snapshot.grammarList.map { it.id }
                saveSessionState(ids, snapshot.currentIndex, _uiState.value.selectedLevel)

                println("撤销成功: ${snapshot.item.displayName}")

            } catch (e: Exception) {
                println("撤销失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        status = LearningStatus.Learning,
                        error = "撤销失败: ${e.message}"
                    )
                }
                syncUndoAvailability()
            }
        }
    }

    /**
     * 评分 (统一处理 Word 和 Grammar)
     *
     * @param quality 评分 (0-5)
     */
    private fun rate(quality: Int) {
        // 防抖
        if (_uiState.value.status == LearningStatus.Processing) return

        ratingJob?.cancel()
        ratingJob = viewModelScope.launch {
            // 保存撤销快照
            saveUndoSnapshot()

            _uiState.update { it.copy(status = LearningStatus.Processing) }

            try {
                val currentItem = getCurrentItem() ?: run {
                    _uiState.update { it.copy(status = LearningStatus.Learning) }
                    return@launch
                }

                // 委托给 StatsManager 进行计数统计与实时持久化
                val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
                sessionStatsManager.onItemRated(_uiState.value.learningMode, quality, currentItem.cardBadge, today)

                _uiState.update {
                    it.copy(
                        sessionNewCount = sessionStatsManager.sessionNewCount,
                        sessionReviewCount = sessionStatsManager.sessionReviewCount,
                        sessionRelearnCount = sessionStatsManager.sessionRelearnCount,
                        sessionMaxCombo = sessionStatsManager.maxCombo
                    )
                }

                val isNew = currentItem.isNew
                val currentStep = _learningSteps[currentItem.id]
                val isLearning = isNew || currentStep != null

                when {
                    // 熟词速通 (评分 = 5)
                    quality == 5 -> {
                        println("熟词速通: ${currentItem.displayName}")
                        _learningSteps.remove(currentItem.id)
                        processSrsUpdate(currentItem, quality, isLapse = false)
                    }

                        // 忘记 (评分 < 3)
                        quality < 3 -> {
                            // 1. 先应用惩罚 (更新数据库中的 Interval/EF)，确保下次毕业时基于惩罚后的值计算
                            val penalizedItem = applyLapsePenalty(currentItem, quality) ?: currentItem

                            // 2. 调度失败流程 (进入 Learning Queue)
                            val currentLapseCount = _lapseCounts.value[currentItem.id] ?: 0
                            
                            // 新卡 Again 应该使用学习步长，复习卡 Again 使用重学步长
                            val config = if (currentItem.isNew) _learningStepsConfig else _relearningStepsConfig
                            
                            val result = learningScheduler.scheduleFailure(
                                penalizedItem, // 使用更新后的 item
                                currentLapseCount,
                                config,
                                _leechThreshold
                            )
                            handleScheduleResult(result)
                        }

                    // 掌握 (评分 3-4)
                    else -> {
                        if (isLearning) {
                             // Use Relearning Config if it's NOT a New Card (i.e. has Repetitions)
                             val config = if(isNew) _learningStepsConfig else _relearningStepsConfig
                            val result = learningScheduler.schedulePass(
                                currentItem,
                                quality,
                                currentStep ?: 0,
                                config
                            )
                            handleScheduleResult(result)
                        } else {
                            processSrsUpdate(currentItem, quality, isLapse = false)
                        }
                    }
                }

                // 评分防抖
                delay(RATING_DEBOUNCE_MS)

            } catch (e: Exception) {
                println("评分异常: ${e.message}")
                _uiState.update {
                    it.copy(
                        status = LearningStatus.Error,
                        error = "评分异常: ${e.message}"
                    )
                }
            }
        }
    }


    /**
     * 应用失败惩罚 (Lapse Penalty)
     *
     * 当用户点击"重来"时，立即更新数据库中的 Interval 和 EF。
     * 这样做的目的是：当卡片在重学阶段结束后再次毕业时，
     * 它的下一次间隔会基于这个"打折后"的值来计算，而不是基于原来的长间隔。
     */
    private suspend fun applyLapsePenalty(item: LearningItem, quality: Int): LearningItem? {
        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)

        return when (item) {
            is LearningItem.WordItem -> {
                val srsResult = srsCalculator.calculate(item.word, quality, today)
                val updatedWord = item.word.copy(
                    type = 3, // 🎯 标记为重学 (Relearn)
                    interval = srsResult.interval,
                    repetitionCount = srsResult.repetitionCount,
                    stability = srsResult.stability,
                    difficulty = srsResult.difficulty,
                    nextReviewDate = srsResult.nextReviewDate,
                    lastReviewedDate = srsResult.lastReviewedDate,
                    firstLearnedDate = srsResult.firstLearnedDate,
                    lastModifiedTime = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
                )
                // 更新数据库
                updateWordUseCase(updatedWord)
                // 返回更新后的 Item
                item.copy(word = updatedWord)
            }
            is LearningItem.GrammarItem -> {
                val srsResult = srsCalculator.calculate(item.grammar, quality, today)
                val updatedGrammar = item.grammar.copy(
                    type = 3, // 🎯 标记为重学 (Relearn)
                    interval = srsResult.interval,
                    repetitionCount = srsResult.repetitionCount,
                    stability = srsResult.stability,
                    difficulty = srsResult.difficulty,
                    nextReviewDate = srsResult.nextReviewDate,
                    lastReviewedDate = srsResult.lastReviewedDate,
                    firstLearnedDate = srsResult.firstLearnedDate,
                    lastModifiedTime = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
                )
                // 更新数据库
                updateGrammarUseCase(updatedGrammar)
                // 返回更新后的 Item
                item.copy(grammar = updatedGrammar)
            }
        }
    }

    /**
     * 处理重学毕业 (Re-learning Graduation)
     *
     * 此时卡片的 Interval 已经在 Lapse 时被惩罚过了 (例如 100 -> 45)。
     * 毕业时，只需将其移回复习队列，并设定下次复习日期为 Today + 45。
     * 不需要再次乘以 EF。
     * 但我们会应用轻微的 Fuzzing (模糊) 以防止复习尖峰。
     */
    private suspend fun processRelearningGraduation(item: LearningItem) {
        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
        val isNew = item.isNew

        when (item) {
            is LearningItem.WordItem -> {
                val word = item.word
                // 说明：此时 word.interval 已经在 applyLapsePenalty 时由 FSRS 算法计算并更新过了。
                // 毕业时只需将其“激活”为下一次复习日期，不再进行二次 Fuzz 或手动计算。
                val interval = word.interval.coerceAtLeast(1)

                val updatedWord = word.copy(
                    type = 2, // 🎯 毕业进入复习 (Review)
                    interval = interval,
                    repetitionCount = word.repetitionCount + 1,
                    lastReviewedDate = today,
                    nextReviewDate = today + interval,
                    lastModifiedTime = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
                )
                println("重学毕业: id=${updatedWord.id}, stability=${updatedWord.stability}, next_interval=${updatedWord.interval}d")
                val result = updateWordUseCase(updatedWord)
                handleSrsUpdateResult(result, isNew, isLapse = false)
            }
            is LearningItem.GrammarItem -> {
                val grammar = item.grammar
                val interval = grammar.interval.coerceAtLeast(1)

                val updatedGrammar = grammar.copy(
                    type = 2, // 🎯 毕业进入复习 (Review)
                    interval = interval,
                    repetitionCount = grammar.repetitionCount + 1,
                    lastReviewedDate = today,
                    nextReviewDate = today + interval,
                    lastModifiedTime = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
                )
                println("重学毕业: id=${updatedGrammar.id}, stability=${updatedGrammar.stability}, next_interval=${updatedGrammar.interval}d")
                val result = updateGrammarUseCase(updatedGrammar)
                handleSrsUpdateResult(result, isNew, isLapse = false)
            }
        }
    }

    /**
     * 处理调度结果
     */
    private suspend fun handleScheduleResult(result: ScheduleResult) {
        when (result) {
            is ScheduleResult.Leech -> handleLeech(result.item, result.totalLapses)
            is ScheduleResult.Graduate -> {
                println("毕业 (Graduated): ${result.item.displayName}")
                _learningSteps.remove(result.item.id)

                // 区分: 新卡毕业 vs 重学毕业 (Re-learning Graduation)
                // 重学毕业时，不应再次乘以 EF (因为 Lapse 时已经重置了间隔)，只需恢复到复习队列
                if (!result.item.isNew && result.quality < 5) {
                    processRelearningGraduation(result.item)
                } else {
                    processSrsUpdate(result.item, result.quality, isLapse = false)
                }
            }
            is ScheduleResult.Requeue -> {
                var item = result.updatedItem
                _learningSteps[item.id] = result.nextStepIndex

                // 🎯 核心修复：同步更新底层模型的 type 字段并持久化
                val mode = _uiState.value.learningMode
                when (mode) {
                    LearningMode.Word -> {
                        if (item is LearningItem.WordItem) {
                            val updatedWord = item.word.copy(type = item.type)
                            item = item.copy(word = updatedWord)
                            updateWordUseCase(updatedWord)
                            if (result.isLapse) settingsRepository.incrementWordLapse(item.id)
                        }
                    }
                    LearningMode.Grammar -> {
                        if (item is LearningItem.GrammarItem) {
                            val updatedGrammar = item.grammar.copy(type = item.type)
                            item = item.copy(grammar = updatedGrammar)
                            updateGrammarUseCase(updatedGrammar)
                            if (result.isLapse) settingsRepository.incrementGrammarLapse(item.id)
                        }
                    }
                }

                if (result.isLapse) {
                    println("失败 (Again): ${item.displayName}, Step ${result.nextStepIndex}, Due in ${(result.dueTime - System.currentTimeMillis())/1000}s")
                } else {
                    if (result.nextStepIndex == _learningSteps[item.id]) {
                        println("困难 (Hard): ${item.displayName}, Keep Step ${result.nextStepIndex}, Due in ${(result.dueTime - System.currentTimeMillis())/1000/60}m")
                    } else {
                        println("进阶 (Good): ${item.displayName} (Step -> ${result.nextStepIndex}), Due in ${(result.dueTime - System.currentTimeMillis())/1000/60}m")
                    }
                }

                reQueueToEnd(item)
            }
        }
    }

    /**
     * SRS 更新
     *
     * 使用会话锁定的学习日 (零点跨天保护)
     */
    private suspend fun processSrsUpdate(item: LearningItem, quality: Int, isLapse: Boolean) {
        // 使用会话锁定的学习日，如果未设置则使用当前学习日
        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
        val isNew = item.isNew

        when (item) {
            is LearningItem.WordItem -> {
                val word = item.word
                val srsResult = srsCalculator.calculate(word, quality, today)
                val updatedWord = word.copy(
                    type = 2, // 🎯 毕业进入复习 (Review)
                    interval = srsResult.interval,
                    repetitionCount = srsResult.repetitionCount,
                    stability = srsResult.stability,
                    difficulty = srsResult.difficulty,
                    nextReviewDate = srsResult.nextReviewDate,
                    lastReviewedDate = srsResult.lastReviewedDate,
                    firstLearnedDate = srsResult.firstLearnedDate,
                    lastModifiedTime = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
                )

                println("更新单词: id=${updatedWord.id}, rep=${updatedWord.repetitionCount}, interval=${updatedWord.interval}d")
                val result = updateWordUseCase(updatedWord)
                handleSrsUpdateResult(result, isNew, isLapse)
            }

            is LearningItem.GrammarItem -> {
                val grammar = item.grammar
                val srsResult = srsCalculator.calculate(grammar, quality, today)
                val updatedGrammar = grammar.copy(
                    type = 2, // 🎯 毕业进入复习 (Review)
                    interval = srsResult.interval,
                    repetitionCount = srsResult.repetitionCount,
                    stability = srsResult.stability,
                    difficulty = srsResult.difficulty,
                    nextReviewDate = srsResult.nextReviewDate,
                    lastReviewedDate = srsResult.lastReviewedDate,
                    firstLearnedDate = srsResult.firstLearnedDate,
                    lastModifiedTime = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
                )

                println("更新语法: id=${updatedGrammar.id}, rep=${updatedGrammar.repetitionCount}, interval=${updatedGrammar.interval}d")
                val result = updateGrammarUseCase(updatedGrammar)
                handleSrsUpdateResult(result, isNew, isLapse)
            }
        }
    }

    private suspend fun handleSrsUpdateResult(result: Result<Unit>, isNew: Boolean, isLapse: Boolean) {
        when (result) {
            is Result.Success -> {
                if (isLapse) {
                    // Lapse: 已在 handleFailure 中处理
                } else {
                    // 更新统计
                    val mode = _uiState.value.learningMode
                    when (mode) {
                        LearningMode.Word -> {
                            if (isNew) studyRecordRepository.incrementLearnedWords(1)
                            else studyRecordRepository.incrementReviewedWords(1)
                        }
                        LearningMode.Grammar -> {
                            if (isNew) studyRecordRepository.incrementLearnedGrammars(1)
                            else studyRecordRepository.incrementReviewedGrammars(1)
                        }
                    }

                    _uiState.update {
                        it.copy(
                            completedThisSession = it.completedThisSession + 1,
                            completedToday = if (isNew) it.completedToday + 1 else it.completedToday,
                            sessionProcessedCount = it.sessionProcessedCount + 1
                        )
                    }

                    // 移出队列并前进
                    removeCurrentAndMoveNext()
                }
            }
            is Result.Error -> {
                _uiState.update {
                    it.copy(
                        status = LearningStatus.Error,
                        error = "更新失败: ${result.exception.message}"
                    )
                }
            }
            else -> {}
        }
    }

    /**
     * 重新入队到末尾
     */
    private fun reQueueToEnd(item: LearningItem) {
        val state = _uiState.value
        val mode = state.learningMode

        // 保存 Anki 状态 (Step & DueTime)
        _learningSteps[item.id] = item.step
        _learningDueTimes[item.id] = item.dueTime

        println("重入队: ${item.displayName}, Step=${item.step}")

        // 注意: Requeue 不增加 sessionProcessedCount，
        // 因为卡片尚未完成学习，后续毕业时才计数 (在 handleSrsUpdateResult 中)
        when (mode) {
            LearningMode.Word -> {
                if (item is LearningItem.WordItem) {
                    val newList = state.wordList.toMutableList().apply {
                        add(item.word)
                    }
                    _uiState.update {
                        it.copy(wordList = newList)
                    }
                }
            }
            LearningMode.Grammar -> {
                if (item is LearningItem.GrammarItem) {
                    val newList = state.grammarList.toMutableList().apply {
                        add(item.grammar)
                    }
                    _uiState.update {
                        it.copy(grammarList = newList)
                    }
                }
            }
        }

        // 移除当前项并前进
        removeCurrentAndMoveNext()
    }

    /**
     * 从等待状态恢复
     * 忽略时间限制，强制显示下一个卡片
     */
    private fun resumeFromWaiting() {
        val state = _uiState.value
        val mode = state.learningMode
        val now = System.currentTimeMillis()

        // 重置状态
        _uiState.update { it.copy(waitingUntil = 0L) }

        val selection = when (mode) {
            LearningMode.Word -> {
                learningQueueManager.selectNextItem(
                    items = state.wordList,
                    getDueTime = { _learningDueTimes[it.id] ?: 0L },
                    now = now,
                    learnAheadLimitMs = Long.MAX_VALUE, // 强制恢复，忽略限制
                    preferredIndex = if (mode == LearningMode.Word) state.currentIndex else state.currentGrammarIndex
                )
            }
            LearningMode.Grammar -> {
                learningQueueManager.selectNextItem(
                    items = state.grammarList,
                    getDueTime = { _learningDueTimes[it.id] ?: 0L },
                    now = now,
                    learnAheadLimitMs = Long.MAX_VALUE,
                    preferredIndex = state.currentGrammarIndex
                )
            }
        }

        // 复用 handleSelectionResult，但此时不应该再返回 Wait
        // 因为我们传了 MAX_VALUE，除非队列为空
        val list = if (mode == LearningMode.Word) state.wordList else state.grammarList
        handleSelectionResult(selection, list, state.selectedLevel, isWord = mode == LearningMode.Word)
    }

    /**
     * 移除当前项并移动到下一个
     *
     * Anki Logic: Priority Queue Selection + Learn Ahead Limit
     * - 移除当前项
     * - 扫描剩余项，优先选最早到期 (min dueTime) 的
     * - Learn Ahead Check:
     *   - 如果 dueTime <= now: 立即显示
     *   - 如果 dueTime <= now + 20min: 提前显示 (Learn Ahead)
     *   - 如果 dueTime > now + 20min: 理论上应等待 (但为防止数据丢失目前强制显示，未来可优化为 Waiting 状态)
     */
    private fun removeCurrentAndMoveNext() {
        val state = _uiState.value
        val mode = state.learningMode
        val now = System.currentTimeMillis()

        when (mode) {
            LearningMode.Word -> {
                val currentIndex = state.currentIndex
                val newList = state.wordList.toMutableList().apply {
                    if (currentIndex in indices) removeAt(currentIndex)
                }

                val selection = learningQueueManager.selectNextItem(
                    items = newList,
                    getDueTime = { _learningDueTimes[it.id] ?: 0L },
                    now = now,
                    learnAheadLimitMs = _learnAheadLimitMs,
                    preferredIndex = currentIndex
                )

                handleSelectionResult(selection, newList, state.selectedLevel, isWord = true)
            }

            LearningMode.Grammar -> {
                val currentIndex = state.currentGrammarIndex
                val newList = state.grammarList.toMutableList().apply {
                    if (currentIndex in indices) removeAt(currentIndex)
                }

                val selection = learningQueueManager.selectNextItem(
                    items = newList,
                    getDueTime = { _learningDueTimes[it.id] ?: 0L },
                    now = now,
                    learnAheadLimitMs = _learnAheadLimitMs,
                    preferredIndex = currentIndex
                )

                handleSelectionResult(selection, newList, state.selectedLevel, isWord = false)
            }
        }
    }

    private fun <T> handleSelectionResult(
        result: QueueSelectionResult<T>,
        newList: List<T>,
        level: String,
        isWord: Boolean
    ) {
        when (result) {
            is QueueSelectionResult.Empty -> {
                completeSession()
            }
            is QueueSelectionResult.Wait -> {
                println("等待中: Waiting until ${result.waitingUntil}")
                _uiState.update {
                    if (isWord) {
                        @Suppress("UNCHECKED_CAST")
                        it.copy(
                            status = LearningStatus.Waiting,
                            wordList = newList as List<Word>,
                            waitingUntil = result.waitingUntil
                        )
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        it.copy(
                            status = LearningStatus.Waiting,
                            grammarList = newList as List<Grammar>,
                            waitingUntil = result.waitingUntil
                        )
                    }
                }
                // 保存当前进度位置，而非硬编码为 0
                // 恢复会话时会从此位置开始寻找下一个可用的卡片
                val currentIdx = if (isWord) _uiState.value.currentIndex else _uiState.value.currentGrammarIndex
                @Suppress("UNCHECKED_CAST")
                val ids = if (isWord) (newList as List<Word>).map { it.id } else (newList as List<Grammar>).map { it.id }
                saveSessionState(ids, currentIdx, level)
            }
            is QueueSelectionResult.Next -> {
                val nextIndex = result.index
                val nextItemObj = result.item

                if (isWord) {
                    val nextWord = nextItemObj as Word
                    // Anki Learn Ahead check was already done in manager
                    val due = _learningDueTimes[nextWord.id] ?: 0L
                    if (due > System.currentTimeMillis()) {
                         println("提前学习 (Learn Ahead)")
                    }

                    val nextItem = LearningItem.WordItem(
                        nextWord,
                        step = _learningSteps[nextWord.id] ?: 0,
                        dueTime = due
                    )

                    _uiState.update {
                        @Suppress("UNCHECKED_CAST")
                        it.copy(
                            status = LearningStatus.Learning,
                            wordList = newList as List<Word>,
                            currentIndex = nextIndex,
                            currentWord = nextWord,
                            isAnswerShown = false,
                            isCardFlipped = false,
                            ratingIntervals = calculateRatingIntervals(nextItem),
                            slideDirection = SlideDirection.FORWARD
                        )
                    }
                    armShowAnswerDelay()
                    @Suppress("UNCHECKED_CAST")
                    saveSessionState((newList as List<Word>).map { it.id }, nextIndex, level)
                } else {
                    val nextGrammar = nextItemObj as Grammar
                    val due = _learningDueTimes[nextGrammar.id] ?: 0L
                    if (due > System.currentTimeMillis()) {
                         println("提前学习 (Learn Ahead)")
                    }

                    val nextItem = LearningItem.GrammarItem(
                        nextGrammar,
                        step = _learningSteps[nextGrammar.id] ?: 0,
                        dueTime = due
                    )

                    _uiState.update {
                        @Suppress("UNCHECKED_CAST")
                        it.copy(
                            status = LearningStatus.Learning,
                            grammarList = newList as List<Grammar>,
                            currentGrammarIndex = nextIndex,
                            currentGrammar = nextGrammar,
                            isAnswerShown = false,
                            isGrammarDetailVisible = false,
                            ratingIntervals = calculateRatingIntervals(nextItem),
                            slideDirection = SlideDirection.FORWARD
                        )
                    }
                    armShowAnswerDelay()
                    @Suppress("UNCHECKED_CAST")
                    saveSessionState((newList as List<Grammar>).map { it.id }, nextIndex, level)
                }
            }
        }
        updateQueueBadgeCounts()
    }

    /**
     * 完成会话
     */
    private fun completeSession() {
        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
        val totalDuration = sessionStatsManager.completeSession(_uiState.value.learningMode, today)
        
        _uiState.update {
            it.copy(
                status = LearningStatus.SessionCompleted,
                currentWord = null,
                currentGrammar = null,
                wordList = emptyList(),
                grammarList = emptyList(),
                currentIndex = 0,
                currentGrammarIndex = 0,
                showAnswerAvailableAt = 0L,
                waitingUntil = 0L,
                sessionDurationSeconds = totalDuration
            )
        }

        clearSession()
        syncService.onLearningCompleted()
        syncUndoAvailability()

        println("会话完成！")
    }

    /**
     * 暂停学习计时器：将本次经过的秒数累加到对应模式的耗时中，持久化写入本地并清空开始时间戳
     */
    private fun pauseTimer() {
        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
        sessionStatsManager.pauseTimer(_uiState.value.learningMode, today)
    }

    /**
     * 恢复学习计时器：重新记录开始时间戳（启动前先核对并刷新天数重置）
     */
    private fun resumeTimer() {
        // 仅在被暂停了，且当前页面处于学习状态时才恢复
        if (sessionStatsManager.sessionStartTime == 0L && _uiState.value.status == LearningStatus.Learning) {
            checkAndRestoreOrReset()
            sessionStatsManager.resumeTimer()
        }
    }

    /**
     * 检查当前学习日天数，用于执行今日学习时间持久化恢复或跨天重置清零
     */
    private fun checkAndRestoreOrReset() {
        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
        sessionStatsManager.checkAndRestoreOrReset(_uiState.value.learningMode, today)
    }

    /**
     * 导航到下一个
     */
    private fun navigateNext() {
        val state = _uiState.value
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

    /**
     * 导航到上一个
     */
    private fun navigatePrev() {
        val state = _uiState.value
        if (state.isNavigating || state.status == LearningStatus.Processing) return

        val currentIndex = when (state.learningMode) {
            LearningMode.Word -> state.currentIndex
            LearningMode.Grammar -> state.currentGrammarIndex
        }

        val prevIndex = currentIndex - 1
        if (prevIndex < 0) return

        navigateTo(prevIndex)
    }

    /**
     * 导航到指定索引
     */
    private fun navigateTo(index: Int) {
        val state = _uiState.value
        if (state.isNavigating || state.status == LearningStatus.Processing) return

        navigationJob?.cancel()
        navigationJob = viewModelScope.launch {
            _uiState.update { it.copy(isNavigating = true) }

            when (state.learningMode) {
                LearningMode.Word -> {
                    if (index !in state.wordList.indices) {
                        _uiState.update { it.copy(isNavigating = false) }
                        return@launch
                    }

                    val direction = if (index > state.currentIndex) SlideDirection.FORWARD else SlideDirection.BACKWARD
                    val targetWord = state.wordList[index]
                    val targetItem = LearningItem.WordItem(targetWord)

                    _uiState.update {
                        it.copy(
                            currentIndex = index,
                            currentWord = targetWord,
                            isAnswerShown = false,
                            isCardFlipped = false,
                            ratingIntervals = calculateRatingIntervals(targetItem),
                            slideDirection = direction
                        )
                    }
                    armShowAnswerDelay()

                    saveSessionState(state.wordList.map { it.id }, index, state.selectedLevel)
                }

                LearningMode.Grammar -> {
                    if (index !in state.grammarList.indices) {
                        _uiState.update { it.copy(isNavigating = false) }
                        return@launch
                    }

                    val direction = if (index > state.currentGrammarIndex) SlideDirection.FORWARD else SlideDirection.BACKWARD
                    val targetGrammar = state.grammarList[index]
                    val targetItem = LearningItem.GrammarItem(targetGrammar)

                    _uiState.update {
                        it.copy(
                            currentGrammarIndex = index,
                            currentGrammar = targetGrammar,
                            isAnswerShown = false,
                            isGrammarDetailVisible = false,
                            ratingIntervals = calculateRatingIntervals(targetItem),
                            slideDirection = direction
                        )
                    }
                    armShowAnswerDelay()

                    saveSessionState(state.grammarList.map { it.id }, index, state.selectedLevel)
                }
            }

            delay(NAVIGATION_DEBOUNCE_MS)
            _uiState.update { it.copy(isNavigating = false) }
        }
    }

    private fun getCurrentItem(): LearningItem? {
        val state = _uiState.value
        return when (state.learningMode) {
            LearningMode.Word -> state.currentWord?.let { LearningItem.WordItem(it) }
            LearningMode.Grammar -> state.currentGrammar?.let { LearningItem.GrammarItem(it) }
        }
    }

    private fun calculateRatingIntervals(item: LearningItem?): Map<Int, String> {
        if (item == null) return emptyMap()

        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)

        return when (item) {
            is LearningItem.WordItem -> srsIntervalPreview.calculate(item.word, item.id, _learningSteps, _learningStepsConfig, _relearningStepsConfig, today)
            is LearningItem.GrammarItem -> srsIntervalPreview.calculate(item.grammar, item.id, _learningSteps, _learningStepsConfig, _relearningStepsConfig, today)
        }
    }



    /**
     * 统计当前队列中各状态的数量并更新 UI State
     */
    private fun updateQueueBadgeCounts() {
        val state = _uiState.value
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

        _uiState.update {
            it.copy(
                queueNewCount = newCount,
                queueLearningCount = learningCount,
                queueReviewCount = reviewCount,
                queueRelearnCount = relearnCount
            )
        }
    }

    private fun saveSessionState(ids: List<Int>, index: Int, level: String) {
        val uiState = _uiState.value
        val waitingUntil = uiState.waitingUntil
        val isAnswerShown = uiState.isAnswerShown
        viewModelScope.launch {
            when (uiState.learningMode) {
                LearningMode.Word -> settingsRepository.saveWordSession(ids, index, level, _learningSteps, waitingUntil, isAnswerShown)
                LearningMode.Grammar -> settingsRepository.saveGrammarSession(ids, index, level, _learningSteps, waitingUntil, isAnswerShown)
            }
        }
    }

    /**
     * 快捷保存当前会话状态
     */
    private fun saveCurrentSessionState() {
        val state = _uiState.value
        val isWord = state.learningMode == LearningMode.Word
        val ids = if (isWord) state.wordList.map { it.id } else state.grammarList.map { it.id }
        val index = if (isWord) state.currentIndex else state.currentGrammarIndex
        saveSessionState(ids, index, state.selectedLevel)
    }

    private fun clearSession() {
        viewModelScope.launch {
            when (_uiState.value.learningMode) {
                LearningMode.Word -> settingsRepository.clearWordSession()
                LearningMode.Grammar -> settingsRepository.clearGrammarSession()
            }
        }
    }

    private fun changeLearningMode(mode: LearningMode) {
        _uiState.update { it.copy(learningMode = mode) }
        // 模式切换时，立即触发新模式的会话加载
        startLearning()
    }



    private fun toggleGrammarDetail() {
        if (_uiState.value.status != LearningStatus.Learning) return
        _uiState.update { it.copy(isGrammarDetailVisible = !it.isGrammarDetailVisible) }
    }

    private fun exitLearning() {
        // 清空撤销快照
        learningUndoHelper.clear()
        syncUndoAvailability()

        sessionStatsManager.clearSessionTimer() // 退出时仅把本次会话开始时间置空，不抹掉今日已存的 word/grammar 累计记录

        _uiState.value = LearningUiState()
    }



    /**
     * 处理钉子户 (Leech)
     */
    private suspend fun handleLeech(item: LearningItem, totalLapses: Int) {
        println("钉子户 (Leech): ${item.displayName} (Lapses: $totalLapses)")

        // 提示用户
        _uiState.update {
            it.copy(error = "已暂停钉子户: ${item.displayName} (累计失败 $totalLapses 次)")
        }

        // 执行暂停逻辑
        suspendItem(item)
    }

    /**
     * 暂停当前项 (用户手动)
     */
    private fun suspendCurrentItem() {
        val currentItem = getCurrentItem() ?: return
        println("手动暂停: ${currentItem.displayName}")

        viewModelScope.launch {
            suspendItem(currentItem)

            // 提示
            _uiState.update {
                it.copy(error = "已暂停: ${currentItem.displayName}")
            }
        }
    }

    /**
     * 今日暂缓此项 (Bury)
     * 持久化到数据库，今日不再出现 (直到 resetHour)
     */
    private fun buryCurrentItem() {
        val currentItem = getCurrentItem() ?: return
        println("今日暂缓 (Bury): ${currentItem.displayName}")

        viewModelScope.launch {
            // 1. 持久化暂停状态 (buriedUntilDay)
            val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
            try {
                when (currentItem) {
                    is LearningItem.WordItem -> {
                        val updatedWord = currentItem.word.copy(buriedUntilDay = today)
                        updateWordUseCase(updatedWord)
                        println("[持久化] 单词已Bury: ${currentItem.displayName} (Until: $today)")
                    }
                    is LearningItem.GrammarItem -> {
                        val updatedGrammar = currentItem.grammar.copy(buriedUntilDay = today)
                        updateGrammarUseCase(updatedGrammar)
                        println("[持久化] 语法已Bury: ${currentItem.displayName} (Until: $today)")
                    }
                }
            } catch (e: Exception) {
                println("[持久化] Bury失败: ${e.message}")
            }

            // 2. 从各类内存状态中移除
            _learningSteps.remove(currentItem.id)
            _learningDueTimes.remove(currentItem.id)

            // 3. 提示
            _uiState.update {
                it.copy(error = "已暂缓: ${currentItem.displayName} (今日不再出现)")
            }

            // 4. 移出队列并前进
            removeCurrentAndMoveNext()
        }
    }

    /**
     * 执行暂停逻辑
     * - 更新 DB 状态为 Suspended (isSkipped = true)
     * - 移出当前会话队列
     */
    private suspend fun suspendItem(item: LearningItem) {
        // 1. 持久化暂停状态
        try {
            when (item) {
                is LearningItem.WordItem -> {
                    val updatedWord = item.word.copy(isSkipped = true)
                    updateWordUseCase(updatedWord)
                    println("[持久化] 单词已暂停: ${item.displayName}")
                }
                is LearningItem.GrammarItem -> {
                    val updatedGrammar = item.grammar.copy(isSkipped = true)
                    updateGrammarUseCase(updatedGrammar)
                    println("[持久化] 语法已暂停: ${item.displayName}")
                }
            }
        } catch (e: Exception) {
            println("[持久化] 暂停失败: ${e.message}")
            // 即使持久化失败，也继续在内存中移除，避免阻塞用户
        }

        // 2. 从各类状态中移除
        _learningSteps.remove(item.id)
        _learningDueTimes.remove(item.id)

        // 3. 移出队列并前进
        removeCurrentAndMoveNext()
    }



    override fun onCleared() {
        super.onCleared()
        learningTtsManager.stop()
    }
}
