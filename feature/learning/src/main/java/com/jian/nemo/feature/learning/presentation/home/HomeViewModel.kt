package com.jian.nemo.feature.learning.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.data.repository.NotificationRepositoryImpl
import com.jian.nemo.core.domain.model.AppNotification
import com.jian.nemo.core.domain.model.User
import com.jian.nemo.core.domain.repository.ConfigRepository
import com.jian.nemo.core.domain.usecase.auth.GetUserFlowUseCase
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import java.time.LocalTime
import javax.inject.Inject

/**
 * 首页ViewModel（简化版，显示统计数据）
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getLearningStatsUseCase: com.jian.nemo.core.domain.usecase.statistics.GetLearningStatsUseCase,
    private val settingsRepository: com.jian.nemo.core.domain.repository.SettingsRepository,
    private val notificationRepository: NotificationRepositoryImpl,
    private val configRepository: ConfigRepository,
    private val getUserFlowUseCase: GetUserFlowUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // 获取用户信息
        viewModelScope.launch {
            getUserFlowUseCase().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }

        // 恢复上次学习模式
        viewModelScope.launch {
            settingsRepository.lastLearningModeFlow.collect { modeStr ->
                val mode = if (modeStr == "grammar") LearningMode.Grammar else LearningMode.Word
                // 使用内部更新逻辑，但不调用 save (避免循环)
                updateLearningModeState(mode)
            }
        }

        // 恢复上次选择的等级 (单词)
        viewModelScope.launch {
            settingsRepository.preferredWordLevelFlow.collect { level ->
                _uiState.update { it.copy(wordSelectedLevel = level) }
            }
        }

        // 恢复上次选择的等级 (语法)
        viewModelScope.launch {
            settingsRepository.preferredGrammarLevelFlow.collect { level ->
                _uiState.update { it.copy(grammarSelectedLevel = level) }
            }
        }

        // Monitor Active Sessions (SRS Optimization)
        viewModelScope.launch {
            settingsRepository.getWordSession().collect { session ->
                _uiState.update { it.copy(hasActiveWordSession = session != null) }
            }
        }

        viewModelScope.launch {
            settingsRepository.getGrammarSession().collect { session ->
                _uiState.update { it.copy(hasActiveGrammarSession = session != null) }
            }
        }

        // Collect learning stats and update UI state
        viewModelScope.launch {
            getLearningStatsUseCase().collect { stats ->
                val todayStats = TodayStats(
                    learnedWords = stats.todayLearnedWords,
                    learnedGrammars = stats.todayLearnedGrammars,
                    reviewedWords = stats.todayReviewedWords,
                    reviewedGrammars = stats.todayReviewedGrammars,
                    dueWords = stats.dueWords,
                    dueGrammars = stats.dueGrammars,
                    wordDailyGoal = stats.wordDailyGoal,
                    grammarDailyGoal = stats.grammarDailyGoal
                )

                _uiState.update { currentState ->
                    // Calculate derived values based on current mode
                    val isWord = currentState.learningMode == LearningMode.Word
                    val progress = if (isWord) todayStats.learnedWords else todayStats.learnedGrammars
                    val goal = if (isWord) todayStats.wordDailyGoal else todayStats.grammarDailyGoal

                    currentState.copy(
                        stats = todayStats,
                        currentProgress = progress,
                        dailyGoal = goal
                    )
                }
            }
        }

        // 查询远程通知 (增加优先级逻辑：如果已经有更新弹窗显示，则不显示通知)
        viewModelScope.launch {
            try {
                // 1. 检查是否有应用更新
                val updateConfig = configRepository.getUpdateConfig()
                val currentVersion = configRepository.getCurrentVersionCode()
                val hasUpdate = updateConfig != null && updateConfig.versionCode > currentVersion
                
                // 2. 如果没有更新，再尝试获取通知
                if (!hasUpdate) {
                    val notification = notificationRepository.getActiveNotification()
                    _uiState.update { it.copy(activeNotification = notification) }
                }
            } catch (e: Exception) {
                // 如果检查更新失败，仍可以尝试显示通知（降级策略）
                val notification = notificationRepository.getActiveNotification()
                _uiState.update { it.copy(activeNotification = notification) }
            }
        }
    }

    fun setLearningMode(mode: LearningMode) {
        viewModelScope.launch {
            val modeStr = if (mode == LearningMode.Word) "word" else "grammar"
            settingsRepository.setLastLearningMode(modeStr)
            // UI更新会通过 Flow 收集自动触发，或者我们可以立即更新以防UI延迟
            updateLearningModeState(mode)
        }
    }

    private fun updateLearningModeState(mode: LearningMode) {
        _uiState.update { currentState ->
            val isWord = mode == LearningMode.Word
            currentState.copy(
                learningMode = mode,
                bgTextResId = if (isWord) R.string.bg_text_word else R.string.bg_text_grammar,
                titleResId = if (isWord) R.string.title_word_learning else R.string.title_grammar_learning,
                currentProgress = if (isWord) currentState.stats.learnedWords else currentState.stats.learnedGrammars,
                dailyGoal = if (isWord) currentState.stats.wordDailyGoal else currentState.stats.grammarDailyGoal
            )
        }
    }

    fun selectLevel(level: String) {
        _uiState.update { 
            if (it.learningMode == LearningMode.Word) {
                it.copy(wordSelectedLevel = level)
            } else {
                it.copy(grammarSelectedLevel = level)
            }
        }
        
        viewModelScope.launch {
            if (_uiState.value.learningMode == LearningMode.Word) {
                settingsRepository.setPreferredWordLevel(level)
            } else {
                settingsRepository.setPreferredGrammarLevel(level)
            }
        }
    }

    fun toggleLevelSheet(show: Boolean) {
        _uiState.update { it.copy(showLevelSheet = show) }
    }

    fun dismissNotification(id: String) {
        viewModelScope.launch {
            notificationRepository.dismiss(id)
            _uiState.update { it.copy(activeNotification = null) }
        }
    }
}

data class HomeUiState(
    val learningMode: LearningMode = LearningMode.Word,
    val wordSelectedLevel: String = "N5",
    val grammarSelectedLevel: String = "N5",
    val levels: List<String> = listOf("N1", "N2", "N3", "N4", "N5"),
    val stats: TodayStats = TodayStats(),
    val showLevelSheet: Boolean = false,

    // [SRS Optimization] Session Awareness
    val hasActiveWordSession: Boolean = false,
    val hasActiveGrammarSession: Boolean = false,

    // 通知状态
    val activeNotification: AppNotification? = null,

    // UI Resources (Derived)
    val bgTextResId: Int = R.string.bg_text_word,
    val titleResId: Int = R.string.title_word_learning,
    val currentProgress: Int = 0,
    val dailyGoal: Int = 0,
    
    // User Context
    val user: User? = null
) {
    val selectedLevel: String
        get() = if (learningMode == LearningMode.Word) wordSelectedLevel else grammarSelectedLevel

    val progressFraction: Float
        get() = if (dailyGoal > 0) (currentProgress.toFloat() / dailyGoal).coerceIn(0f, 1f) else 0f

    val hasCurrentModeSession: Boolean
        get() = if (learningMode == LearningMode.Word) hasActiveWordSession else hasActiveGrammarSession

    val itemsDue: Int
        get() = if (learningMode == LearningMode.Word) stats.dueWords else stats.dueGrammars

    val reviewedToday: Int
        get() = if (learningMode == LearningMode.Word) stats.reviewedWords else stats.reviewedGrammars

    val dailyCompletionRate: Int
        get() = if (dailyGoal > 0) ((currentProgress.toFloat() / dailyGoal) * 100).toInt().coerceIn(0, 100) else 0
}

/**
 * 今日统计数据
 */
data class TodayStats(
    val learnedWords: Int = 0,
    val learnedGrammars: Int = 0,
    val reviewedWords: Int = 0,
    val reviewedGrammars: Int = 0,
    val dueWords: Int = 0,
    val dueGrammars: Int = 0,
    val wordDailyGoal: Int = 50, // Default
    val grammarDailyGoal: Int = 10 // Default
) {
    val totalLearned: Int
        get() = learnedWords + learnedGrammars

    val totalReviewed: Int
        get() = reviewedWords + reviewedGrammars

    val totalDue: Int
        get() = dueWords + dueGrammars
}
