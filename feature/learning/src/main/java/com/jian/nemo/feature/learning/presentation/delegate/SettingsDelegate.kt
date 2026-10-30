package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.usecase.statistics.GetLearningStatsUseCase
import com.jian.nemo.feature.learning.domain.LearningTtsManager
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.LearningStatus
import com.jian.nemo.feature.learning.presentation.parseSteps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 设置配置域 delegate (系统设计 §3.2.M2 / UserStory US-1 / F1)
 *
 * 承接原 LearningViewModel 的：
 * - init 块 18+ Flow collector（设置监听 + TTS 播放状态监听）
 * - toggle/cycle/set 系列设置写入方法
 * - armShowAnswerDelay 显示答案延迟装填
 * - 深色模式设置
 *
 * 配置变量（resetHour/leechThreshold/leechAction/stepsConfig 等）
 * 统一存放在共享 LearningSessionState，供其他 delegate 读取。
 */
class SettingsDelegate(
    private val settingsRepository: SettingsRepository,
    private val getLearningStatsUseCase: GetLearningStatsUseCase,
    private val learningTtsManager: LearningTtsManager,
    private val sessionState: LearningSessionState,
) {
    private lateinit var stateUpdater: StateUpdater
    private lateinit var scope: CoroutineScope

    fun init(stateUpdater: StateUpdater, scope: CoroutineScope) {
        this.stateUpdater = stateUpdater
        this.scope = scope
        collectSettings()
    }

    private fun collectSettings() {
        // 0. 监听显示模式设置 (深色/浅色)
        scope.launch {
            settingsRepository.isDarkModeFlow.collect { isDark ->
                stateUpdater.update { it.copy(isDarkMode = isDark) }
            }
        }

        // 监听图标风格设置 (Plump Duo 等)
        scope.launch {
            settingsRepository.settingIconStyleFlow.collect { iconStyle ->
                stateUpdater.update { it.copy(settingIconStyle = iconStyle) }
            }
        }

        // 监听打卡统计数据以获取真实连续打卡天数 (Streak)
        scope.launch {
            getLearningStatsUseCase().collect { stats ->
                stateUpdater.update { it.copy(dailyStreak = stats.dailyStreak) }
            }
        }

        // 监听 Leech 阈值设置
        scope.launch {
            settingsRepository.leechThresholdFlow.collect { threshold ->
                sessionState.leechThreshold = threshold.coerceAtLeast(1)
            }
        }

        // 监听 Leech 处置动作设置 (bury_today / skip)
        scope.launch {
            settingsRepository.leechActionFlow.collect { action ->
                sessionState.leechAction = action
            }
        }

        // 1. 监听 TTS 事件以更新播放状态 (必须在 uiState 初始化后)
        learningTtsManager.observeTtsEvents(scope)
        scope.launch {
            learningTtsManager.playingAudioId.collect { id ->
                stateUpdater.update { it.copy(playingAudioId = id) }
            }
        }

        // 监听设置变化
        scope.launch {
            // 监听每日目标变化 (新目标将在下个学习日新会话生效，中途修改不干预当前会话)
            launch {
                settingsRepository.dailyGoalFlow.distinctUntilChanged().collect { newGoal ->
                    val state = stateUpdater.currentState
                    if (state.learningMode == LearningMode.Word && state.status != LearningStatus.Learning) {
                        stateUpdater.update { it.copy(dailyGoal = newGoal) }
                    }
                }
            }

            launch {
                settingsRepository.grammarDailyGoalFlow.distinctUntilChanged().collect { newGoal ->
                    val state = stateUpdater.currentState
                    if (state.learningMode == LearningMode.Grammar && state.status != LearningStatus.Learning) {
                        stateUpdater.update { it.copy(dailyGoal = newGoal) }
                    }
                }
            }

            // 监听失败次数
            launch {
                settingsRepository.wordLapsesFlow.collect { lapses ->
                    if (stateUpdater.currentState.learningMode == LearningMode.Word) {
                        sessionState.lapseCounts.value = lapses
                    }
                }
            }

            launch {
                settingsRepository.grammarLapsesFlow.collect { lapses ->
                    if (stateUpdater.currentState.learningMode == LearningMode.Grammar) {
                        sessionState.lapseCounts.value = lapses
                    }
                }
            }

            // 监听学习配置
            launch {
                settingsRepository.learningStepsFlow.collect { stepsStr ->
                    sessionState.learningStepsConfig = parseSteps(stepsStr)
                    println("学习步进更新: ${sessionState.learningStepsConfig} (mins)")
                }
            }

            launch {
                settingsRepository.relearningStepsFlow.collect { stepsStr ->
                    sessionState.relearningStepsConfig = parseSteps(stepsStr)
                    println("重学步进更新: ${sessionState.relearningStepsConfig} (mins)")
                }
            }

            launch {
                settingsRepository.learnAheadLimitFlow.collect { limit ->
                    sessionState.learnAheadLimitMs = limit * 60 * 1000L
                    println("提前学习限制更新: $limit mins")
                }
            }

            // 监听自动朗读设置
            launch {
                settingsRepository.isAutoPlayAudioEnabledFlow.collect { enabled ->
                    stateUpdater.update { it.copy(isAutoAudioEnabled = enabled) }
                }
            }

            // 监听显示答案等待开关 (Word)
            launch {
                settingsRepository.isShowAnswerDelayEnabledFlow.collect { enabled ->
                    if (stateUpdater.currentState.learningMode == LearningMode.Word) {
                        stateUpdater.update { it.copy(isShowAnswerDelayEnabled = enabled) }
                        armShowAnswerDelay()
                    }
                }
            }

            // 监听显示答案等待开关 (Grammar)
            launch {
                settingsRepository.isGrammarShowAnswerDelayEnabledFlow.collect { enabled ->
                    if (stateUpdater.currentState.learningMode == LearningMode.Grammar) {
                        stateUpdater.update { it.copy(isShowAnswerDelayEnabled = enabled) }
                        armShowAnswerDelay()
                    }
                }
            }

            // 监听显示答案等待时长 (Word)
            launch {
                settingsRepository.showAnswerDelayMsFlow.collect { delayMs ->
                    if (stateUpdater.currentState.learningMode == LearningMode.Word) {
                        sessionState.showAnswerDelayMs = delayMs
                        stateUpdater.update { it.copy(showAnswerDelayMs = delayMs) }
                        armShowAnswerDelay()
                    }
                }
            }

            // 监听显示答案等待时长 (Grammar)
            launch {
                settingsRepository.grammarShowAnswerDelayMsFlow.collect { delayMs ->
                    if (stateUpdater.currentState.learningMode == LearningMode.Grammar) {
                        sessionState.showAnswerDelayMs = delayMs
                        stateUpdater.update { it.copy(showAnswerDelayMs = delayMs) }
                        armShowAnswerDelay()
                    }
                }
            }

            // 监听自动翻面开关 (Word)
            launch {
                settingsRepository.isAutoRevealAnswerEnabledFlow.collect { enabled ->
                    stateUpdater.update { it.copy(isAutoRevealAnswerEnabled = enabled) }
                }
            }

            // 监听自动翻面时长 (Word)
            launch {
                settingsRepository.autoRevealAnswerMsFlow.collect { delayMs ->
                    stateUpdater.update { it.copy(autoRevealAnswerMs = delayMs) }
                }
            }

            // 监听自动翻面开关 (Grammar)
            launch {
                settingsRepository.isGrammarAutoRevealAnswerEnabledFlow.collect { enabled ->
                    stateUpdater.update { it.copy(isGrammarAutoRevealAnswerEnabled = enabled) }
                }
            }

            // 监听自动翻面时长 (Grammar)
            launch {
                settingsRepository.grammarAutoRevealAnswerMsFlow.collect { delayMs ->
                    stateUpdater.update { it.copy(grammarAutoRevealAnswerMs = delayMs) }
                }
            }
        }
    }

    // ===== 设置写入 (toggle / cycle / set 系列) =====

    fun toggleAutoPlayAudio(enabled: Boolean) {
        scope.launch {
            settingsRepository.setAutoPlayAudioEnabled(enabled)
        }
    }

    fun toggleShowAnswerDelay(enabled: Boolean) {
        scope.launch {
            val isGrammar = stateUpdater.currentState.learningMode == LearningMode.Grammar
            settingsRepository.setShowAnswerDelayEnabled(enabled, isGrammar = isGrammar)
        }
    }

    fun cycleShowAnswerDelayDuration() {
        val next = when (sessionState.showAnswerDelayMs) {
            2000L -> 3000L
            3000L -> 4000L
            4000L -> 5000L
            else -> 2000L
        }
        scope.launch {
            val isGrammar = stateUpdater.currentState.learningMode == LearningMode.Grammar
            settingsRepository.setShowAnswerDelayMs(next, isGrammar = isGrammar)
        }
    }

    fun setShowAnswerDelayMs(ms: Long) {
        scope.launch {
            val isGrammar = stateUpdater.currentState.learningMode == LearningMode.Grammar
            settingsRepository.setShowAnswerDelayMs(ms, isGrammar = isGrammar)
        }
    }

    fun toggleAutoRevealAnswer(enabled: Boolean) {
        scope.launch {
            settingsRepository.setAutoRevealAnswerEnabled(enabled)
        }
    }

    fun cycleAutoRevealAnswerDuration() {
        val current = stateUpdater.currentState.autoRevealAnswerMs
        val next = when (current) {
            5000L -> 6000L
            6000L -> 7000L
            7000L -> 8000L
            8000L -> 9000L
            9000L -> 10000L
            else -> 5000L
        }
        scope.launch {
            settingsRepository.setAutoRevealAnswerMs(next)
        }
    }

    fun setAutoRevealAnswerMs(ms: Long) {
        scope.launch {
            settingsRepository.setAutoRevealAnswerMs(ms)
        }
    }

    fun toggleGrammarAutoRevealAnswer(enabled: Boolean) {
        scope.launch {
            settingsRepository.setGrammarAutoRevealAnswerEnabled(enabled)
        }
    }

    fun cycleGrammarAutoRevealAnswerDuration() {
        val current = stateUpdater.currentState.grammarAutoRevealAnswerMs
        val next = when (current) {
            10000L -> 11000L
            11000L -> 12000L
            12000L -> 13000L
            13000L -> 14000L
            14000L -> 15000L
            else -> 10000L
        }
        scope.launch {
            settingsRepository.setGrammarAutoRevealAnswerMs(next)
        }
    }

    fun setGrammarAutoRevealAnswerMs(ms: Long) {
        scope.launch {
            settingsRepository.setGrammarAutoRevealAnswerMs(ms)
        }
    }

    // ===== 深色模式 =====

    fun setDarkMode(enabled: Boolean?) {
        scope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }

    fun cycleDarkMode() {
        val nextMode = when (stateUpdater.currentState.isDarkMode) {
            null -> false  // 系统 -> 浅色
            false -> true  // 浅色 -> 深色
            true -> null   // 深色 -> 系统
        }
        scope.launch {
            settingsRepository.setDarkMode(nextMode)
        }
    }

    // ===== 显示答案延迟装填 =====

    /**
     * 装填显示答案延迟计时：
     * 取「显式延迟目标」与「冷却锁定期目标」的最大值。
     */
    fun armShowAnswerDelay() {
        val state = stateUpdater.currentState
        if (state.isAnswerShown || !state.hasPendingItems || state.status != LearningStatus.Learning) {
            stateUpdater.update { it.copy(showAnswerAvailableAt = 0L) }
            return
        }

        val now = System.currentTimeMillis()
        val currentItem = state.currentLearningItem()
        val dueTime = currentItem?.let { sessionState.learningDueTimes[it.id] } ?: 0L

        // 判定当前卡片是否处于冷却锁定期（例如手动滑动翻阅至尚未到期的卡片）
        val waitMs = dueTime - now
        val isCooling = waitMs > 0L && (sessionState.learnAheadLimitMs <= 0L || waitMs > sessionState.learnAheadLimitMs)

        val delayTarget = if (state.isShowAnswerDelayEnabled) now + sessionState.showAnswerDelayMs else 0L
        val coolingTarget = if (isCooling) dueTime else 0L

        val availableAt = maxOf(delayTarget, coolingTarget)
        stateUpdater.update { it.copy(showAnswerAvailableAt = availableAt) }
    }

    /**
     * 模式切换后刷新显示答案等待配置（原 changeLearningMode 内联逻辑，
     * 供 SessionLifecycleDelegate 切换模式时调用）
     */
    suspend fun refreshAnswerDelayConfigForMode(mode: LearningMode) {
        val isGrammar = mode == LearningMode.Grammar
        val enabled = if (isGrammar) {
            settingsRepository.isGrammarShowAnswerDelayEnabledFlow.first()
        } else {
            settingsRepository.isShowAnswerDelayEnabledFlow.first()
        }
        val delayMs = if (isGrammar) {
            settingsRepository.grammarShowAnswerDelayMsFlow.first()
        } else {
            settingsRepository.showAnswerDelayMsFlow.first()
        }
        sessionState.showAnswerDelayMs = delayMs
        stateUpdater.update {
            it.copy(
                isShowAnswerDelayEnabled = enabled,
                showAnswerDelayMs = delayMs
            )
        }
        armShowAnswerDelay()
    }
}
