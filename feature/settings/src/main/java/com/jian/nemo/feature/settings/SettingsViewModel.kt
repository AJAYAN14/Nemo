package com.jian.nemo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.jian.nemo.core.domain.repository.SyncRepository
import android.util.Log
import javax.inject.Inject

/**
 * 设置界面ViewModel
 *
 * 职责:
 * - 从SettingsRepository读取配置
 * - 处理用户设置变更
 * - 更新DataStore
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: com.jian.nemo.core.domain.repository.AuthRepository,
    private val syncRepository: SyncRepository,
    private val restoreDataUseCase: com.jian.nemo.core.domain.usecase.sync.RestoreDataUseCase,
    private val exportDataUseCase: com.jian.nemo.core.domain.usecase.settings.ExportDataUseCase,
    private val importDataUseCase: com.jian.nemo.core.domain.usecase.settings.ImportDataUseCase,
    private val resetProgressUseCase: com.jian.nemo.core.domain.usecase.settings.ResetProgressUseCase,
    private val repairDataUseCase: com.jian.nemo.core.domain.usecase.settings.RepairDataUseCase,
    private val playTtsUseCase: com.jian.nemo.core.domain.usecase.audio.PlayTtsUseCase,
    private val audioRepository: com.jian.nemo.core.domain.repository.AudioRepository,
    private val application: android.app.Application
) : ViewModel() {

    // UI状态
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeUser()
        observeTtsEvents()
    }

    private fun observeTtsEvents() {
        viewModelScope.launch {
            audioRepository.ttsEvents.collect { event ->
                val id = when (event) {
                    is com.jian.nemo.core.domain.repository.TtsEvent.OnStart -> event.id
                    is com.jian.nemo.core.domain.repository.TtsEvent.OnDone,
                    is com.jian.nemo.core.domain.repository.TtsEvent.OnError,
                    com.jian.nemo.core.domain.repository.TtsEvent.GoogleTtsMissing -> null
                }

                if (id?.startsWith("preview-") == true) {
                    when (event) {
                        is com.jian.nemo.core.domain.repository.TtsEvent.OnStart -> {
                            // 开始播放时，状态已由 onEvent 提前设置
                        }
                        is com.jian.nemo.core.domain.repository.TtsEvent.OnDone,
                        is com.jian.nemo.core.domain.repository.TtsEvent.OnError,
                        com.jian.nemo.core.domain.repository.TtsEvent.GoogleTtsMissing -> {
                            // 结束或错误时清除预览状态
                            _uiState.update { it.copy(previewingVoiceName = null) }
                        }
                    }
                }
            }
        }
    }

    private fun observeUser() {
        viewModelScope.launch {
            authRepository.getUserFlow().collect { user ->
                 _uiState.update {
                     it.copy(
                         isLoggedIn = user != null,
                         user = user
                     )
                 }
                 if (user?.avatarUrl != null) {
                     _uiState.update { it.copy(avatarPath = user.avatarUrl) }
                 }
            }
        }

         viewModelScope.launch {
            settingsRepository.userAvatarPathFlow.collect { path ->
                _uiState.update { it.copy(avatarPath = path) }
            }
        }

        // 加载可用语音列表
        viewModelScope.launch {
            val voices = audioRepository.getAvailableVoices()
            _uiState.update { it.copy(availableVoices = voices) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val appearanceFlow = combine(
                settingsRepository.isDarkModeFlow,
                settingsRepository.isDynamicColorEnabledFlow,
                settingsRepository.darkModeStrategyFlow,
                settingsRepository.darkModeStartTimeFlow,
                settingsRepository.darkModeEndTimeFlow
            ) { darkMode, dynamicColor, strategy, startTime, endTime ->
                ThemeSettings(darkMode, dynamicColor, strategy, startTime, endTime)
            }.combine(settingsRepository.themeColorFlow) { theme, themeColor ->
                theme.copy(themeColor = themeColor)
            }.combine(settingsRepository.appIconFlow) { theme, appIcon ->
                 _uiState.update { it.copy(appIcon = appIcon) }
                 theme // return theme for next combined
            }

            val goalsFlow = combine(
                settingsRepository.dailyGoalFlow,
                settingsRepository.grammarDailyGoalFlow,
                settingsRepository.learningDayResetHourFlow,
                settingsRepository.isRandomNewContentEnabledFlow,
                settingsRepository.isRestoringFlow
            ) { dailyGoal, grammarDailyGoal, resetHour, isRandom, isRestoring ->
                Quintuple(dailyGoal, grammarDailyGoal, resetHour, isRandom, isRestoring)
            }

            val syncFlow = combine(
                settingsRepository.lastSyncTimeFlow,
                settingsRepository.isAutoSyncEnabledFlow,
                settingsRepository.lastSyncConflictCountFlow
            ) { lastSyncTime, isAutoSyncEnabled, conflictCount ->
                Triple(lastSyncTime, isAutoSyncEnabled, conflictCount)
            }

            val advancedFlow = combine(
                 settingsRepository.learningStepsFlow,
                 settingsRepository.relearningStepsFlow,
                 settingsRepository.learnAheadLimitFlow,
                 settingsRepository.leechThresholdFlow,
                 settingsRepository.leechActionFlow
            ) { steps, relearningSteps, limit, leechThreshold, leechAction ->
                AdvancedSettings(
                    learningSteps = steps,
                    relearningSteps = relearningSteps,
                    learnAheadLimit = limit,
                    leechThreshold = leechThreshold,
                    leechAction = leechAction
                )
            }

            val ttsFlow = combine(
                settingsRepository.ttsSpeechRateFlow,
                settingsRepository.ttsPitchFlow,
                settingsRepository.ttsVoiceNameFlow
            ) { rate, pitch, voiceName -> Triple(rate, pitch, voiceName) }

            combine(
                appearanceFlow,
                goalsFlow,
                syncFlow,
                advancedFlow,
                ttsFlow
            ) { theme, (dailyGoal, grammarDailyGoal, resetHour, isRandom, isRestoring), (lastSyncTime, isAutoSyncEnabled, conflictCount), advanced, (rate, pitch, voiceName) ->
                _uiState.update { state ->
                    state.copy(
                        darkMode = when (theme.darkMode) {
                            null -> DarkModeOption.AUTO
                            true -> DarkModeOption.DARK
                            false -> DarkModeOption.LIGHT
                        },
                        darkModeStrategy = when (theme.strategy) {
                            "scheduled" -> DarkModeStrategy.SCHEDULED
                            else -> DarkModeStrategy.FOLLOW_SYSTEM
                        },
                        darkModeStartTime = theme.startTime,
                        darkModeEndTime = theme.endTime,
                        isDynamicColorEnabled = theme.dynamicColor,
                        themeColor = theme.themeColor,
                        dailyGoal = dailyGoal,
                        grammarDailyGoal = grammarDailyGoal,
                        learningDayResetHour = resetHour,
                        isRandomNewContentEnabled = isRandom,
                        lastSyncTime = lastSyncTime,
                        isAutoSyncEnabled = isAutoSyncEnabled,
                        lastSyncConflictCount = conflictCount,
                        learningSteps = advanced.learningSteps,
                        relearningSteps = advanced.relearningSteps,
                        learnAheadLimit = advanced.learnAheadLimit,
                        leechThreshold = advanced.leechThreshold,
                        leechAction = advanced.leechAction,
                        ttsSpeechRate = rate,
                        ttsPitch = pitch,
                        ttsVoiceName = voiceName,
                        isRestoring = isRestoring,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetDarkMode -> setDarkMode(event.option)
            is SettingsEvent.SetDarkModeStrategy -> setDarkModeStrategy(event.strategy)
            is SettingsEvent.SetDarkModeStartTime -> setDarkModeStartTime(event.time)
            is SettingsEvent.SetDarkModeEndTime -> setDarkModeEndTime(event.time)
            is SettingsEvent.SetDynamicColor -> setDynamicColor(event.enabled)
            is SettingsEvent.SetThemeColor -> setThemeColor(event.colorArgb)
            is SettingsEvent.SetDailyGoal -> setDailyGoal(event.goal)
            is SettingsEvent.SetAppIcon -> setAppIcon(event.iconName)
            is SettingsEvent.SetGrammarDailyGoal -> setGrammarDailyGoal(event.goal)
            is SettingsEvent.SetLearningDayResetHour -> setLearningDayResetHour(event.hour)
            is SettingsEvent.SetRandomNewContentEnabled -> setRandomNewContentEnabled(event.enabled)
            is SettingsEvent.ShowDailyGoalDialog -> _uiState.update { it.copy(showDailyGoalDialog = event.show) }
            is SettingsEvent.ShowGrammarDailyGoalDialog -> _uiState.update { it.copy(showGrammarDailyGoalDialog = event.show) }
            is SettingsEvent.ShowLearningDayResetHourDialog -> _uiState.update { it.copy(showLearningDayResetHourDialog = event.show) }
            is SettingsEvent.SetLearningSteps -> setLearningSteps(event.steps)
            is SettingsEvent.SetRelearningSteps -> setRelearningSteps(event.steps)
            is SettingsEvent.SetLearnAheadLimit -> setLearnAheadLimit(event.limit)
            is SettingsEvent.SetLeechThreshold -> setLeechThreshold(event.threshold)
            is SettingsEvent.SetLeechAction -> setLeechAction(event.action)
            is SettingsEvent.SaveAdvancedLearningSettings -> saveAdvancedLearningSettings(
                event.learningSteps,
                event.relearningSteps,
                event.learnAheadLimit,
                event.leechThreshold,
                event.leechAction
            )

            is SettingsEvent.SetTtsSpeechRate -> setTtsSpeechRate(event.rate)
            is SettingsEvent.SetTtsPitch -> setTtsPitch(event.pitch)
            is SettingsEvent.SetTtsVoiceName -> setTtsVoiceName(event.voiceName)
            is SettingsEvent.ShowVoiceSelectionDialog -> _uiState.update { it.copy(showVoiceSelectionDialog = event.show) }
            is SettingsEvent.PreviewTts -> previewTts(event.text)
            is SettingsEvent.PreviewVoice -> previewVoiceWithName(event.voiceName, event.text)

            is SettingsEvent.SyncData -> syncData()
            is SettingsEvent.RestoreData -> restoreData()
            is SettingsEvent.SetAutoSyncEnabled -> setAutoSyncEnabled(event.enabled)
            is SettingsEvent.ResolveConflict -> resolveConflict(event.option)
            is SettingsEvent.ExportData -> exportData(event.uri)
            is SettingsEvent.ImportData -> importData(event.uri)
            is SettingsEvent.ResetProgress -> resetProgress(event.includeCloud)
            is SettingsEvent.RepairLocalData -> repairData()
        }
    }

    private fun updateStatusMessage(message: String?, delayMs: Long = 5000) {
        viewModelScope.launch {
            _uiState.update { it.copy(syncMessage = message) }
            if (message != null) {
                kotlinx.coroutines.delay(delayMs)
                _uiState.update { it.copy(syncMessage = null) }
            }
        }
    }

    private fun exportData(uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = exportDataUseCase(uri.toString())
                _uiState.update { it.copy(isLoading = false) }
                updateStatusMessage(if (success) "导出成功" else "导出失败", 5000)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                updateStatusMessage("导出出错: ${e.message}", 5000)
            }
        }
    }

    private fun importData(uri: android.net.Uri) {
         viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val message = importDataUseCase(uri.toString())
                _uiState.update { it.copy(isLoading = false) }
                updateStatusMessage(message, 5000)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                updateStatusMessage("导入出错: ${e.message}", 5000)
            }
        }
    }

    private fun resolveConflict(option: ConflictResolutionOption) {
        when (option) {
            ConflictResolutionOption.FORCE_CLOUD -> restoreData()
            ConflictResolutionOption.FORCE_LOCAL -> syncData(force = true)
        }
    }

    private fun syncData(force: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val userId = _uiState.value.user?.id ?: return@launch
            syncRepository.performSync(userId, force).collect { progress ->
                when(progress) {
                    is com.jian.nemo.core.domain.model.SyncProgress.Running -> {
                         val msg = "${progress.section} (${progress.current}/${progress.total})"
                         _uiState.update { it.copy(syncMessage = msg) }
                    }
                    is com.jian.nemo.core.domain.model.SyncProgress.Completed -> {
                        val stats = progress.report.stats
                        val detail = buildString {
                            append("同步成功: ")
                            if (stats.addedItems > 0) append("新增 ${stats.addedItems} 条, ")
                            if (stats.updatedItems > 0) append("更新 ${stats.updatedItems} 条")
                        }.removeSuffix(", ")

                        _uiState.update { it.copy(isLoading = false, syncMessage = null) }
                        updateStatusMessage(detail)
                    }
                    is com.jian.nemo.core.domain.model.SyncProgress.Failed -> {
                        _uiState.update { it.copy(isLoading = false, syncMessage = null) }
                        updateStatusMessage("同步失败: ${progress.error}", 5000)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun restoreData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            restoreDataUseCase().collect { progress ->
                when(progress) {
                    is com.jian.nemo.core.domain.model.SyncProgress.Running -> {
                         _uiState.update { it.copy(syncMessage = "正在从云端恢复: ${progress.section}") }
                    }
                    is com.jian.nemo.core.domain.model.SyncProgress.Completed -> {
                        _uiState.update { it.copy(isLoading = false, syncMessage = null) }
                        updateStatusMessage("镜像恢复成功，本地数据已更新")
                    }
                    is com.jian.nemo.core.domain.model.SyncProgress.Failed -> {
                        _uiState.update { it.copy(isLoading = false, syncMessage = null) }
                        updateStatusMessage("恢复失败: ${progress.error}", 5000)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSyncEnabled(enabled)
            // 如果开启了自动同步，可以考虑立即触发一次 worker 检查或者 update worker constraint
            // 但 AutoSyncWorker 是基于 PeriodicWorkRequest 的，
            // 通常由系统调度。这里只需保存配置，Worker 会读取该配置决定是否执行实际同步逻辑。
        }
    }

    /**
     * 设置深色模式
     */
    private fun setDarkMode(option: DarkModeOption) {
        viewModelScope.launch {
            val value = when (option) {
                DarkModeOption.AUTO -> null
                DarkModeOption.LIGHT -> false
                DarkModeOption.DARK -> true
            }
            settingsRepository.setDarkMode(value)
        }
    }

    private fun setDarkModeStrategy(strategy: DarkModeStrategy) {
        viewModelScope.launch {
            val value = when (strategy) {
                DarkModeStrategy.FOLLOW_SYSTEM -> "system"
                DarkModeStrategy.SCHEDULED -> "scheduled"
            }
            settingsRepository.setDarkModeStrategy(value)
        }
    }

    private fun setDarkModeStartTime(time: String) {
        viewModelScope.launch {
            settingsRepository.setDarkModeStartTime(time)
        }
    }

    private fun setDarkModeEndTime(time: String) {
        viewModelScope.launch {
            settingsRepository.setDarkModeEndTime(time)
        }
    }

    /**
     * 设置动态颜色
     */
    private fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(enabled)
        }
    }

    /**
     * 设置主题色
     */
    private fun setThemeColor(colorArgb: Long?) {
        viewModelScope.launch {
            settingsRepository.setThemeColor(colorArgb)
        }
    }

    /**
     * 设置每日目标
     */
    private fun setDailyGoal(goal: Int) {
        viewModelScope.launch {
            settingsRepository.setDailyGoal(goal)
            _uiState.update { it.copy(showDailyGoalDialog = false) }

            // 🎯 动态提示次日生效时间
            val resetHour = _uiState.value.learningDayResetHour
            updateStatusMessage("目标设置成功，将于明天凌晨${resetHour}:00后生效", 5000)
        }
    }

    /**
     * 设置每日语法目标
     */
    private fun setGrammarDailyGoal(goal: Int) {
        viewModelScope.launch {
            settingsRepository.setGrammarDailyGoal(goal)
            _uiState.update { it.copy(showGrammarDailyGoalDialog = false) }

            // 🎯 动态提示次日生效时间
            val resetHour = _uiState.value.learningDayResetHour
            updateStatusMessage("目标设置成功，将于明天凌晨${resetHour}:00后生效", 5000)
        }
    }

    /**
     * 设置应用图标
     */
    private fun setAppIcon(iconName: String) {
        viewModelScope.launch {
            // 1. 保存到 DataStore
            settingsRepository.setAppIcon(iconName)
            
            // 2. 执行物理切换 (PackageManager)
            try {
                val packageManager = application.packageManager
                val packageName = application.packageName
                
                // 定义所有的图标组件别名 (必须与 AndroidManifest.xml 一致)
                val icons = listOf("Nemo", "Gold", "Daruma", "Zen")
                
                icons.forEach { name ->
                    val componentName = android.content.ComponentName(
                        packageName,
                        "$packageName.MainActivity$name"
                    )
                    
                    val newState = if (name == iconName) {
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    }
                    
                    packageManager.setComponentEnabledSetting(
                        componentName,
                        newState,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                }
                
                Log.i("SettingsViewModel", "App icon changed to: $iconName")
                updateStatusMessage("应用图标已切换为 [$iconName]，桌面图标更新可能需要几秒钟")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to change app icon", e)
                updateStatusMessage("切换图标失败: ${e.message}")
            }
        }
    }

    /**
     * 设置学习日重置时间
     */
    private fun setLearningDayResetHour(hour: Int) {
        viewModelScope.launch {
            settingsRepository.setLearningDayResetHour(hour)
            _uiState.update { it.copy(showLearningDayResetHourDialog = false) }
        }
    }

    /**
     * 设置是否开启新内容随机抽取
     */
    private fun setRandomNewContentEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRandomNewContentEnabled(enabled)
            val message = if (enabled) {
                "下次开始学习时，新单词将随机出现"
            } else {
                "下次开始学习时，新单词将按顺序出现"
            }
            updateStatusMessage(message, 5000)
        }
    }

    private fun setLearningSteps(steps: String) {
        // Validate? For now simple
        viewModelScope.launch {
            settingsRepository.setLearningSteps(steps)
        }
    }

    private fun setRelearningSteps(steps: String) {
        viewModelScope.launch {
            settingsRepository.setRelearningSteps(steps)
        }
    }

    private fun setLearnAheadLimit(limit: Int) {
        viewModelScope.launch {
            settingsRepository.setLearnAheadLimit(limit)
        }
    }

    private fun setLeechThreshold(threshold: Int) {
        viewModelScope.launch {
            settingsRepository.setLeechThreshold(threshold.coerceAtLeast(1))
        }
    }

    private fun setLeechAction(action: String) {
        viewModelScope.launch {
            val normalized = if (action == "bury_today") "bury_today" else "skip"
            settingsRepository.setLeechAction(normalized)
        }
    }

    private fun saveAdvancedLearningSettings(
        learningSteps: String,
        relearningSteps: String,
        learnAheadLimit: Int,
        leechThreshold: Int,
        leechAction: String
    ) {
        viewModelScope.launch {
            settingsRepository.saveAdvancedLearningSettings(
                learningSteps,
                relearningSteps,
                learnAheadLimit,
                leechThreshold,
                leechAction
            )
            // 可选：在保存成功后给出一个小提示
            // updateStatusMessage("高级学习设置已保存", 3000)
        }
    }

    private fun setTtsSpeechRate(rate: Float) {
        viewModelScope.launch {
            settingsRepository.setTtsSpeechRate(rate)
        }
    }

    private fun setTtsPitch(pitch: Float) {
        viewModelScope.launch {
            settingsRepository.setTtsPitch(pitch)
        }
    }

    private fun setTtsVoiceName(voiceName: String) {
        viewModelScope.launch {
            settingsRepository.setTtsVoiceName(voiceName)
            _uiState.update { it.copy(showVoiceSelectionDialog = false) }
        }
    }

    private fun previewTts(text: String) {
        val id = System.currentTimeMillis().toString()
        playTtsUseCase(text, "ja-JP", id)
    }

    /**
     * 预览指定语音（不保存设置）
     */
    private fun previewVoiceWithName(voiceName: String, text: String) {
        // Use audio repository to temporarily set voice and play
        viewModelScope.launch {
            _uiState.update { it.copy(previewingVoiceName = voiceName) }
            audioRepository.previewVoice(voiceName, text)
        }
    }

    /**
     * 重置所有学习进度
     * @param includeCloud 是否同时删除云端同步数据
     */
    private fun resetProgress(includeCloud: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = resetProgressUseCase(includeCloud)) {
                is com.jian.nemo.core.common.Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    updateStatusMessage("学习进度已重置，所有学习数据已清除", 5000)
                }
                is com.jian.nemo.core.common.Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    updateStatusMessage("重置失败: ${result.exception.message}", 5000)
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun repairData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = repairDataUseCase()) {
                is com.jian.nemo.core.common.Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    updateStatusMessage(result.data, 5000)
                }
                is com.jian.nemo.core.common.Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    updateStatusMessage("修复失败: ${result.exception.message}", 5000)
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}

private data class ThemeSettings(
    val darkMode: Boolean?,
    val dynamicColor: Boolean,
    val strategy: String,
    val startTime: String,
    val endTime: String,
    val themeColor: Long? = null
)

data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

private data class AdvancedSettings(
    val learningSteps: String,
    val relearningSteps: String,
    val learnAheadLimit: Int,
    val leechThreshold: Int,
    val leechAction: String
)
