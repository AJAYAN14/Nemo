package com.jian.nemo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject
import com.jian.nemo.core.data.manager.CloudBackupManager

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
    private val exportDataUseCase: com.jian.nemo.core.domain.usecase.settings.ExportDataUseCase,
    private val importDataUseCase: com.jian.nemo.core.domain.usecase.settings.ImportDataUseCase,
    private val resetProgressUseCase: com.jian.nemo.core.domain.usecase.settings.ResetProgressUseCase,
    private val repairDataUseCase: com.jian.nemo.core.domain.usecase.settings.RepairDataUseCase,
    private val playTtsUseCase: com.jian.nemo.core.domain.usecase.audio.PlayTtsUseCase,
    private val audioRepository: com.jian.nemo.core.domain.repository.AudioRepository,
    private val cloudBackupManager: CloudBackupManager,
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
            val chineseVoices = audioRepository.getAvailableChineseVoices()
            _uiState.update { it.copy(availableVoices = voices, availableChineseVoices = chineseVoices) }
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
                settingsRepository.defaultBonusBatchSizeFlow,
                settingsRepository.learningDayResetHourFlow,
                settingsRepository.isRandomNewContentEnabledFlow
            ) { dailyGoal, grammarDailyGoal, defaultBonusBatchSize, resetHour, isRandom ->
                GoalSettings(dailyGoal, grammarDailyGoal, defaultBonusBatchSize, resetHour, isRandom)
            }


            val advancedFlow = combine(
                 settingsRepository.learningStepsFlow,
                 settingsRepository.relearningStepsFlow,
                 settingsRepository.learnAheadLimitFlow,
                 settingsRepository.leechThresholdFlow,
                 settingsRepository.leechActionFlow,
                 settingsRepository.targetRetentionFlow
            ) { args ->
                AdvancedSettings(
                    learningSteps = args[0] as String,
                    relearningSteps = args[1] as String,
                    learnAheadLimit = args[2] as Int,
                    leechThreshold = args[3] as Int,
                    leechAction = args[4] as String,
                    targetRetention = args[5] as Float
                )
            }

            val ttsFlow = combine(
                settingsRepository.ttsSpeechRateFlow,
                settingsRepository.ttsPitchFlow,
                settingsRepository.ttsVoiceNameFlow,
                settingsRepository.ttsChineseVoiceNameFlow
            ) { rate, pitch, voiceName, chineseVoiceName ->
                data class TtsSettingsSnapshot(val rate: Float, val pitch: Float, val voiceName: String?, val chineseVoiceName: String?)
                TtsSettingsSnapshot(rate, pitch, voiceName, chineseVoiceName)
            }

            combine(
                appearanceFlow,
                goalsFlow,
                advancedFlow,
                ttsFlow
            ) { theme, goals, advanced, tts ->
                val (dailyGoal, grammarDailyGoal, defaultBonusBatchSize, resetHour, isRandom) = goals
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
                        defaultBonusBatchSize = defaultBonusBatchSize,
                        learningDayResetHour = resetHour,
                        isRandomNewContentEnabled = isRandom,
                        learningSteps = advanced.learningSteps,
                        relearningSteps = advanced.relearningSteps,
                        learnAheadLimit = advanced.learnAheadLimit,
                        leechThreshold = advanced.leechThreshold,
                        leechAction = advanced.leechAction,
                        targetRetention = advanced.targetRetention,
                        ttsSpeechRate = tts.rate,
                        ttsPitch = tts.pitch,
                        ttsVoiceName = tts.voiceName,
                        ttsChineseVoiceName = tts.chineseVoiceName,
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
            is SettingsEvent.SetDefaultBonusBatchSize -> setDefaultBonusBatchSize(event.size)
            is SettingsEvent.SetLearningDayResetHour -> setLearningDayResetHour(event.hour)
            is SettingsEvent.SetRandomNewContentEnabled -> setRandomNewContentEnabled(event.enabled)
            is SettingsEvent.ShowDailyGoalDialog -> _uiState.update { it.copy(showDailyGoalDialog = event.show) }
            is SettingsEvent.ShowGrammarDailyGoalDialog -> _uiState.update { it.copy(showGrammarDailyGoalDialog = event.show) }
            is SettingsEvent.ShowBonusBatchSizeDialog -> _uiState.update { it.copy(showBonusBatchSizeDialog = event.show) }
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
                event.leechAction,
                event.targetRetention
            )

            is SettingsEvent.SetTtsSpeechRate -> setTtsSpeechRate(event.rate)
            is SettingsEvent.SetTtsPitch -> setTtsPitch(event.pitch)
            is SettingsEvent.SetTtsVoiceName -> setTtsVoiceName(event.voiceName)
            is SettingsEvent.ShowVoiceSelectionDialog -> _uiState.update { it.copy(showVoiceSelectionDialog = event.show) }
            is SettingsEvent.SetTtsChineseVoiceName -> setTtsChineseVoiceName(event.voiceName)
            is SettingsEvent.ShowChineseVoiceSelectionDialog -> _uiState.update { it.copy(showChineseVoiceSelectionDialog = event.show) }
            is SettingsEvent.PreviewTts -> previewTts(event.text)
            is SettingsEvent.PreviewVoice -> previewVoiceWithName(event.voiceName, event.text, java.util.Locale.JAPAN)
            is SettingsEvent.PreviewChineseVoice -> previewVoiceWithName(event.voiceName, event.text, java.util.Locale.CHINA)


            is SettingsEvent.RequestExport -> requestExport()
            is SettingsEvent.ConfirmExport -> confirmExport(event.uri, event.isCompressed)
            is SettingsEvent.CancelExport -> cancelExport()
            is SettingsEvent.ImportData -> importData(event.uri)
            is SettingsEvent.ResetProgress -> resetProgress()
            is SettingsEvent.RepairLocalData -> repairData()
            is SettingsEvent.ClearToast -> _uiState.update { it.copy(toastMessage = null) }
            
            // 云端备份事件
            is SettingsEvent.BackupToCloud -> backupToCloud()
            is SettingsEvent.ShowCloudBackupList -> loadCloudBackups()
            is SettingsEvent.SelectRestoreFile -> _uiState.update { it.copy(showRestoreStrategyDialog = true, pendingRestoreFileName = event.fileName) }
            is SettingsEvent.CancelRestore -> _uiState.update { it.copy(showRestoreStrategyDialog = false, pendingRestoreFileName = null) }
            is SettingsEvent.RestoreFromCloud -> restoreFromCloud(event.fileName, event.strategy)
            is SettingsEvent.ConfirmRestore -> confirmRestore()
            is SettingsEvent.CancelRestorePreview -> cancelRestorePreview()

        }
    }


    private fun requestExport() {
        _uiState.update { it.copy(showExportOptionsDialog = true) }
    }

    private fun cancelExport() {
        _uiState.update { it.copy(showExportOptionsDialog = false) }
    }

    private fun confirmExport(uri: android.net.Uri, isCompressed: Boolean) {
        cancelExport() // 关闭弹窗并清理临时变量
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val success = exportDataUseCase(uri.toString(), isCompressed)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Export failed", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun importData(uri: android.net.Uri) {
         viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val message = importDataUseCase(uri.toString())
                _uiState.update { it.copy(toastMessage = message) }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Import failed", e)
                _uiState.update { it.copy(toastMessage = "导入过程中发生异常: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private var lastSuccessfulBackupTime = 0L
    private val BACKUP_DEBOUNCE_INTERVAL = 60_000L

    private fun backupToCloud() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSuccessfulBackupTime < BACKUP_DEBOUNCE_INTERVAL) {
            _uiState.update { it.copy(toastMessage = "备份过于频繁，请稍后再试") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCloudSyncing = true) }
            try {
                cloudBackupManager.uploadBackup()
                lastSuccessfulBackupTime = System.currentTimeMillis()
                _uiState.update { it.copy(toastMessage = "云端备份成功") }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Backup to cloud failed", e)
                _uiState.update { it.copy(toastMessage = "备份失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isCloudSyncing = false) }
                // 备份后刷新列表
                loadCloudBackups()
            }
        }
    }

    private fun loadCloudBackups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCloudSyncing = true) }
            try {
                val backups = cloudBackupManager.listBackups()
                _uiState.update { 
                    it.copy(
                        cloudBackupList = backups,
                        toastMessage = "备份列表已刷新"
                    ) 
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Load cloud backups failed", e)
                _uiState.update { it.copy(toastMessage = "获取备份列表失败: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isCloudSyncing = false) }
            }
        }
    }

    private fun restoreFromCloud(fileName: String, strategy: com.jian.nemo.core.data.manager.ImportStrategy) {
        viewModelScope.launch {
            _uiState.update { it.copy(showRestoreStrategyDialog = false, isCloudSyncing = true) }
            try {
                // 下载并预览
                val preview = cloudBackupManager.previewRestore(fileName, strategy)
                val content = cloudBackupManager.downloadBackup(fileName)
                _uiState.update { 
                    it.copy(
                        restorePreview = preview,
                        pendingRestoreContent = content
                    ) 
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Preview restore failed", e)
                _uiState.update { it.copy(toastMessage = "预览失败: ${e.message}", pendingRestoreFileName = null) }
            } finally {
                _uiState.update { it.copy(isCloudSyncing = false) }
            }
        }
    }

    private fun confirmRestore() {
        val content = _uiState.value.pendingRestoreContent
        val preview = _uiState.value.restorePreview
        if (content == null || preview == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(restorePreview = null, isCloudSyncing = true) }
            try {
                val message = cloudBackupManager.executeRestore(content, preview.strategy)
                _uiState.update { it.copy(toastMessage = message) }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Confirm restore failed", e)
                _uiState.update { it.copy(toastMessage = "恢复失败: ${e.message}") }
            } finally {
                _uiState.update { 
                    it.copy(
                        isCloudSyncing = false,
                        pendingRestoreFileName = null,
                        pendingRestoreContent = null
                    ) 
                }
            }
        }
    }

    private fun cancelRestorePreview() {
        _uiState.update { 
            it.copy(
                restorePreview = null,
                pendingRestoreContent = null,
                pendingRestoreFileName = null
            ) 
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
            _uiState.update { 
                it.copy(
                    showDailyGoalDialog = false,
                    toastMessage = "每日目标已更新，将在下一个学习日生效"
                ) 
            }
        }
    }

    /**
     * 设置每日语法目标
     */
    private fun setGrammarDailyGoal(goal: Int) {
        viewModelScope.launch {
            settingsRepository.setGrammarDailyGoal(goal)
            _uiState.update { 
                it.copy(
                    showGrammarDailyGoalDialog = false,
                    toastMessage = "每日语法目标已更新，将在下一个学习日生效"
                ) 
            }
        }
    }

    /**
     * 设置默认加餐单组数量
     */
    private fun setDefaultBonusBatchSize(size: Int) {
        viewModelScope.launch {
            settingsRepository.setDefaultBonusBatchSize(size)
            _uiState.update { it.copy(showBonusBatchSizeDialog = false) }
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
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to change app icon", e)
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
        leechAction: String,
        targetRetention: Float
    ) {
        viewModelScope.launch {
            settingsRepository.saveAdvancedLearningSettings(
                learningSteps,
                relearningSteps,
                learnAheadLimit,
                leechThreshold,
                leechAction,
                targetRetention
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

    private fun setTtsChineseVoiceName(voiceName: String) {
        viewModelScope.launch {
            settingsRepository.setTtsChineseVoiceName(voiceName)
            _uiState.update { it.copy(showChineseVoiceSelectionDialog = false) }
        }
    }

    private fun previewTts(text: String) {
        val id = System.currentTimeMillis().toString()
        playTtsUseCase(text, "ja-JP", id)
    }

    /**
     * 预览指定语音（不保存设置）
     */
    private fun previewVoiceWithName(voiceName: String, text: String, language: java.util.Locale = java.util.Locale.JAPAN) {
        // Use audio repository to temporarily set voice and play
        viewModelScope.launch {
            if (language == java.util.Locale.CHINA) {
                _uiState.update { it.copy(previewingChineseVoiceName = voiceName) }
            } else {
                _uiState.update { it.copy(previewingVoiceName = voiceName) }
            }
            audioRepository.previewVoice(voiceName, text, language)
        }
    }

    /**
     * 重置所有学习进度
     */
    private fun resetProgress() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = resetProgressUseCase()) {
                is com.jian.nemo.core.common.Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
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

private data class GoalSettings(
    val dailyGoal: Int,
    val grammarDailyGoal: Int,
    val defaultBonusBatchSize: Int,
    val resetHour: Int,
    val isRandom: Boolean
)

data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

private data class AdvancedSettings(
    val learningSteps: String,
    val relearningSteps: String,
    val learnAheadLimit: Int,
    val leechThreshold: Int,
    val leechAction: String,
    val targetRetention: Float
)


