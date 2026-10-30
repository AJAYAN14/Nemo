
package com.jian.nemo.feature.learning.presentation

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
import com.jian.nemo.core.domain.service.SrsCalculator
import com.jian.nemo.core.domain.usecase.grammar.GetDueGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetLearningGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetNewGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetTodayLearnedGrammarsCountUseCase
import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.word.GetDueWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetLearningWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetNewWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetTodayLearnedWordsCountUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.core.domain.repository.ContentReportRepository
import com.jian.nemo.core.data.manager.CloudBackupManager
import com.jian.nemo.core.ui.component.dialog.BonusMode
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
import com.jian.nemo.feature.learning.presentation.delegate.CardOpsDelegate
import com.jian.nemo.feature.learning.presentation.delegate.LearningSessionState
import com.jian.nemo.feature.learning.presentation.delegate.QueueNavigationDelegate
import com.jian.nemo.feature.learning.presentation.delegate.RatingDelegate
import com.jian.nemo.feature.learning.presentation.delegate.SessionLifecycleDelegate
import com.jian.nemo.feature.learning.presentation.delegate.SettingsDelegate
import com.jian.nemo.feature.learning.presentation.delegate.StatsTimerDelegate
import com.jian.nemo.feature.learning.presentation.delegate.StateUpdater
import com.jian.nemo.feature.learning.presentation.delegate.UndoDelegate
import com.jian.nemo.feature.learning.presentation.delegate.calculateRatingIntervals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    // Word UseCases
    private val getNewWordsUseCase: GetNewWordsUseCase,
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val getLearningWordsUseCase: GetLearningWordsUseCase,
    private val updateWordUseCase: UpdateWordUseCase,
    private val getTodayLearnedWordsCountUseCase: GetTodayLearnedWordsCountUseCase,
    // Grammar UseCases
    private val getNewGrammarsUseCase: GetNewGrammarsUseCase,
    private val getDueGrammarsUseCase: GetDueGrammarsUseCase,
    private val getLearningGrammarsUseCase: GetLearningGrammarsUseCase,
    private val updateGrammarUseCase: UpdateGrammarUseCase,
    private val getTodayLearnedGrammarsCountUseCase: GetTodayLearnedGrammarsCountUseCase,
    // Repositories
    private val settingsRepository: SettingsRepository,
    private val studyRecordRepository: StudyRecordRepository,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    // Services
    private val srsCalculator: SrsCalculator,
    // Domain
    private val learningSessionPolicy: LearningSessionPolicy,

    private val srsIntervalPreview: SrsIntervalPreview,
    private val sessionLoader: SessionLoader,
    private val learningQueueManager: LearningQueueManager,
    private val learningScheduler: LearningScheduler,
    private val learningUndoHelper: LearningUndoHelper,
    private val learningTtsManager: LearningTtsManager,
    private val contentReportRepository: ContentReportRepository,
    private val cloudBackupManager: CloudBackupManager,
    private val reviewLogRepository: com.jian.nemo.core.domain.repository.ReviewLogRepository,
    private val getLearningStatsUseCase: com.jian.nemo.core.domain.usecase.statistics.GetLearningStatsUseCase,
    private val sessionStatsManager: LearningSessionStatsManager,
) : ViewModel(), StateUpdater {
    private val _uiState = MutableStateFlow(LearningUiState())
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    // StateUpdater 实现——delegate 通过此接口更新 _uiState（系统设计 §3.2.M9）
    override fun update(transform: (LearningUiState) -> LearningUiState) {
        _uiState.update(transform)
    }

    override val currentState: LearningUiState get() = _uiState.value

    /** 跨 delegate 共享的学习会话内部状态（系统设计 §4.2.2） */
    val sessionState = LearningSessionState()

    /** 设置配置域 delegate（承接原 init 块 18+ Flow collector + toggle/cycle/set + armShowAnswerDelay + 深色模式） */
    private val settingsDelegate = SettingsDelegate(
        settingsRepository = settingsRepository,
        getLearningStatsUseCase = getLearningStatsUseCase,
        learningTtsManager = learningTtsManager,
        sessionState = sessionState,
    ).also { it.init(this, viewModelScope) }
    /** 队列调度与导航域 delegate（reQueue/resumeFromWaiting/removeCurrentAndMoveNext/handleSelectionResult/navigate/updateQueueBadgeCounts/saveSessionState/clearSession） */
    private val queueNavigationDelegate = QueueNavigationDelegate(
        learningQueueManager, settingsRepository, srsIntervalPreview,
        sessionState, this, viewModelScope, settingsDelegate,
    )
    /** 撤销管理域 delegate（承接 saveUndoSnapshot/syncUndoAvailability/undo/clearSnapshots） */
    private val undoDelegate = UndoDelegate(
        learningUndoHelper, updateWordUseCase, updateGrammarUseCase,
        studyRecordRepository, wordRepository, grammarRepository,
        srsIntervalPreview, sessionState, this, viewModelScope, queueNavigationDelegate,
    )
    /** 计时与会话结算域 delegate（承接 completeSession/syncTodayCounts/pauseTimer/resumeTimer/checkAndRestoreOrReset/showAnswer/toggleGrammarDetail） */
    private val statsTimerDelegate = StatsTimerDelegate(
        wordRepository, grammarRepository,
        getTodayLearnedWordsCountUseCase, getTodayLearnedGrammarsCountUseCase,
        cloudBackupManager, learningTtsManager, sessionState, this, viewModelScope,
        undoDelegate, queueNavigationDelegate, sessionStatsManager,
    )
    init {
        // 打破 QueueNavigation↔StatsTimer 构造环：STD 构造晚于 QND，构造完成后注入
        queueNavigationDelegate.statsTimerDelegate = statsTimerDelegate
    }
    /** 卡片操作域 delegate（handleLeech/suspendCurrentItem/buryCurrentItem/suspendItem） */
    private val cardOpsDelegate = CardOpsDelegate(
        updateWordUseCase, updateGrammarUseCase,
        sessionState, this, viewModelScope, queueNavigationDelegate,
    )
    /** 评分管线域 delegate（rate/applyLapsePenalty/processRelearningGraduation/handleScheduleResult/processSrsUpdate/persistRatingItemAsync） */
    private val ratingDelegate = RatingDelegate(
        learningQueueManager, learningScheduler, srsCalculator, srsIntervalPreview,
        wordRepository, grammarRepository, updateWordUseCase, updateGrammarUseCase,
        settingsRepository, studyRecordRepository, reviewLogRepository,
        sessionState, this, viewModelScope,
        undoDelegate, statsTimerDelegate, queueNavigationDelegate, cardOpsDelegate,
    )
    /** 会话生命周期编排域 delegate（handleStart/handleBonusStart/loadSession/handleSessionLoadResult/changeLearningMode/exitLearning） */
    private val sessionLifecycleDelegate = SessionLifecycleDelegate(
        sessionLoader, learningSessionPolicy, wordRepository, grammarRepository,
        getNewWordsUseCase, getDueWordsUseCase, getLearningWordsUseCase, getTodayLearnedWordsCountUseCase,
        getNewGrammarsUseCase, getDueGrammarsUseCase, getLearningGrammarsUseCase, getTodayLearnedGrammarsCountUseCase,
        settingsRepository, cloudBackupManager,
        sessionState, this, viewModelScope,
        settingsDelegate, undoDelegate, statsTimerDelegate, queueNavigationDelegate, srsIntervalPreview,
    )

    /**
     * 处理所有 UI 事件
     */
    fun onEvent(event: LearningEvent) {
        when (event) {
            is LearningEvent.StartLearning -> {
                _uiState.update { it.copy(learningMode = event.mode) }
                sessionLifecycleDelegate.handleStart()
            }
            is LearningEvent.StartBonusLearning -> {
                _uiState.update { it.copy(learningMode = event.mode) }
                sessionLifecycleDelegate.handleBonusStart(event.count, event.bonusMode)
            }
            is LearningEvent.ChangeLearningMode -> sessionLifecycleDelegate.changeLearningMode(event.mode)

            // 卡片交互
            is LearningEvent.FlipCard -> statsTimerDelegate.showAnswer()
            is LearningEvent.ShowAnswer -> statsTimerDelegate.showAnswer()

            // 评分 (统一处理)
            is LearningEvent.RateWord -> ratingDelegate.rate(event.quality)
            is LearningEvent.RateGrammar -> ratingDelegate.rate(event.quality)

            // 导航
            is LearningEvent.NavigateNext -> queueNavigationDelegate.navigateNext()
            is LearningEvent.NavigatePrev -> queueNavigationDelegate.navigatePrev()
            is LearningEvent.GoToIndex -> queueNavigationDelegate.navigateTo(event.index)

            // 其他
            is LearningEvent.ShowTypingPractice -> _uiState.update { it.copy(showTypingPractice = true) }
            is LearningEvent.HideTypingPractice -> _uiState.update { it.copy(showTypingPractice = false) }
            is LearningEvent.Retry -> sessionLifecycleDelegate.handleStart()
            is LearningEvent.Undo -> undoDelegate.undo()
            is LearningEvent.ResumeFromWaiting -> queueNavigationDelegate.resumeFromWaiting()
            is LearningEvent.SuspendCurrent -> cardOpsDelegate.suspendCurrentItem()
            is LearningEvent.BuryCurrent -> cardOpsDelegate.buryCurrentItem()
            is LearningEvent.ExitLearning -> sessionLifecycleDelegate.exitLearning()
            is LearningEvent.PauseTimer -> statsTimerDelegate.pauseTimer()
            is LearningEvent.ResumeTimer -> statsTimerDelegate.resumeTimer()

            // 语法专用
            is LearningEvent.ToggleGrammarDetail -> statsTimerDelegate.toggleGrammarDetail()
            is LearningEvent.LoadPreviousGrammar -> queueNavigationDelegate.navigatePrev()
            is LearningEvent.LoadNextGrammar -> queueNavigationDelegate.navigateNext()
            is LearningEvent.MasterGrammar -> ratingDelegate.rate(5) // 掌握 = 评分5

            // TTS 朗读
            is LearningEvent.SpeakWord -> learningTtsManager.speakWord(event.text, event.chinese)
            is LearningEvent.SpeakExample -> learningTtsManager.speakExample(event.japanese, event.chinese, event.id)
            is LearningEvent.ToggleAutoPlayAudio -> settingsDelegate.toggleAutoPlayAudio(event.enabled)
            is LearningEvent.ToggleShowAnswerDelay -> settingsDelegate.toggleShowAnswerDelay(event.enabled)
            is LearningEvent.CycleShowAnswerDelayDuration -> settingsDelegate.cycleShowAnswerDelayDuration()
            is LearningEvent.SetShowAnswerDelayMs -> settingsDelegate.setShowAnswerDelayMs(event.ms)
            is LearningEvent.ToggleAutoRevealAnswer -> settingsDelegate.toggleAutoRevealAnswer(event.enabled)
            is LearningEvent.CycleAutoRevealAnswerDuration -> settingsDelegate.cycleAutoRevealAnswerDuration()
            is LearningEvent.SetAutoRevealAnswerMs -> settingsDelegate.setAutoRevealAnswerMs(event.ms)
            is LearningEvent.ToggleGrammarAutoRevealAnswer -> settingsDelegate.toggleGrammarAutoRevealAnswer(event.enabled)
            is LearningEvent.CycleGrammarAutoRevealAnswerDuration -> settingsDelegate.cycleGrammarAutoRevealAnswerDuration()
            is LearningEvent.SetGrammarAutoRevealAnswerMs -> settingsDelegate.setGrammarAutoRevealAnswerMs(event.ms)
            is LearningEvent.ReportContentError -> handleReportError(event.errorType, event.description)
            is LearningEvent.OpenReportErrorDialog -> _uiState.update { it.copy(showReportErrorDialog = true) }
            is LearningEvent.CancelReportErrorDialog -> _uiState.update { it.copy(showReportErrorDialog = false) }
            is LearningEvent.ClearSuccessMessage -> _uiState.update { it.copy(successMessage = null) }
            is LearningEvent.ClearErrorMessage -> _uiState.update { it.copy(error = null) }
            is LearningEvent.SetDarkMode -> settingsDelegate.setDarkMode(event.enabled)
            is LearningEvent.CycleDarkMode -> settingsDelegate.cycleDarkMode()
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
                val friendlyError = when {
                    message.contains("unique_user_item_report") -> "您已经提交过对此类型问题的反馈了，请勿重复提交。"
                    message.contains("connection closed", ignoreCase = true) ||
                    message.contains("Unable to resolve host", ignoreCase = true) ||
                    message.contains("timeout", ignoreCase = true) ||
                    message.contains("HTTP request to", ignoreCase = true) -> "网络连接失败，请检查网络后重试。"
                    else -> "反馈失败: $message"
                }
                _uiState.update { it.copy(error = friendlyError) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        learningTtsManager.stop()
    }
}
