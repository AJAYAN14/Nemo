package com.jian.nemo.feature.settings

/**
 * 设置界面UI状态
 */
data class SettingsUiState(
    // 外观设置
    val darkMode: DarkModeOption = DarkModeOption.AUTO,
    val themeColor: Long? = null,  // null = 默认品牌蓝
    val darkModeStrategy: DarkModeStrategy = DarkModeStrategy.FOLLOW_SYSTEM,
    val darkModeStartTime: String = "07:00",
    val darkModeEndTime: String = "19:00",
    val isDynamicColorEnabled: Boolean = true,
    val appIcon: String = "Nemo", // 当前应用图标名称

    // 学习设置
    val dailyGoal: Int = 20,
    val grammarDailyGoal: Int = 10,
    val learningDayResetHour: Int = 4, // 学习日重置时间 (0-23)
    val isRandomNewContentEnabled: Boolean = true, // 默认开启随机抽取
    val learningSteps: String = "1 10", // 学习步进
    val relearningSteps: String = "1 10", // 重学步进
    val learnAheadLimit: Int = 20, // 提前学习限制 (分钟)
    val leechThreshold: Int = 5, // Leech 判定阈值（累计失败次数）
    val leechAction: String = "skip", // Leech 行为: skip | bury_today

    // 弹窗状态
    val showDailyGoalDialog: Boolean = false,
    val showGrammarDailyGoalDialog: Boolean = false,
    val showLearningDayResetHourDialog: Boolean = false,



    // 应用信息
    val appVersion: String = "1.0.0",

    // 同步状态
    val isAutoSyncEnabled: Boolean = true,
    val lastSyncTime: Long = 0,
    val lastSyncConflictCount: Int = 0,
    val showRestoreConfirmDialog: Boolean = false,
    val isRestoring: Boolean = false, // 恢复被中断的标识
    val syncMessage: String? = null,

    // TTS 设置
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsVoiceName: String? = null,
    val availableVoices: List<com.jian.nemo.core.domain.model.TtsVoice> = emptyList(),
    val showVoiceSelectionDialog: Boolean = false,
    val isLoadingVoices: Boolean = false,       // 语音列表加载中
    val previewingVoiceName: String? = null,    // 当前正在试听的语音名称

    // 加载状态
    val isLoading: Boolean = false,

    // 用户状态 (从 AuthRepository 观察)
    val isLoggedIn: Boolean = false,
    val user: com.jian.nemo.core.domain.model.User? = null,
    val avatarPath: String? = null
)

/**
 * 深色模式选项
 */
enum class DarkModeOption {
    AUTO,           // 自动
    LIGHT,          // 浅色
    DARK            // 深色
}

/**
 * 自动模式下的策略
 */
enum class DarkModeStrategy {
    FOLLOW_SYSTEM,  // 跟随系统
    SCHEDULED       // 定时切换
}

/**
 * 冲突解决选项
 */
enum class ConflictResolutionOption {
    FORCE_CLOUD, // 强制以云端为准
    FORCE_LOCAL  // 强制以本地为准
}

/**
 * 设置界面事件
 */
sealed interface SettingsEvent {
    data class SetDarkMode(
        val option: DarkModeOption,
        val triggerX: Float = 0f,
        val triggerY: Float = 0f
    ) : SettingsEvent
    data class SetDarkModeStrategy(val strategy: DarkModeStrategy) : SettingsEvent
    data class SetDarkModeStartTime(val time: String) : SettingsEvent
    data class SetDarkModeEndTime(val time: String) : SettingsEvent
    data class SetDynamicColor(val enabled: Boolean) : SettingsEvent
    data class SetThemeColor(val colorArgb: Long?) : SettingsEvent

    // 学习设置
    data class SetDailyGoal(val goal: Int) : SettingsEvent
    data class SetAppIcon(val iconName: String) : SettingsEvent
    data class SetGrammarDailyGoal(val goal: Int) : SettingsEvent
    data class SetLearningDayResetHour(val hour: Int) : SettingsEvent
    data class SetRandomNewContentEnabled(val enabled: Boolean) : SettingsEvent
    data class SetLearningSteps(val steps: String) : SettingsEvent
    data class SetRelearningSteps(val steps: String) : SettingsEvent
    data class SetLearnAheadLimit(val limit: Int) : SettingsEvent
    data class SetLeechThreshold(val threshold: Int) : SettingsEvent
    data class SetLeechAction(val action: String) : SettingsEvent

    /**
     * 原子保存高级学习设置
     */
    data class SaveAdvancedLearningSettings(
        val learningSteps: String,
        val relearningSteps: String,
        val learnAheadLimit: Int,
        val leechThreshold: Int,
        val leechAction: String
    ) : SettingsEvent

    // TTS 设置
    data class SetTtsSpeechRate(val rate: Float) : SettingsEvent
    data class SetTtsPitch(val pitch: Float) : SettingsEvent
    data class SetTtsVoiceName(val voiceName: String) : SettingsEvent
    data class ShowVoiceSelectionDialog(val show: Boolean) : SettingsEvent
    data class PreviewTts(val text: String) : SettingsEvent
    data class PreviewVoice(val voiceName: String, val text: String) : SettingsEvent

    // 弹窗控制
    data class ShowDailyGoalDialog(val show: Boolean) : SettingsEvent
    data class ShowGrammarDailyGoalDialog(val show: Boolean) : SettingsEvent
    data class ShowLearningDayResetHourDialog(val show: Boolean) : SettingsEvent

    // 同步
    data object SyncData : SettingsEvent
    data object RestoreData : SettingsEvent
    data class SetAutoSyncEnabled(val enabled: Boolean) : SettingsEvent
    data class ResolveConflict(val option: ConflictResolutionOption) : SettingsEvent

    // 文件导出导入
    data class ExportData(val uri: android.net.Uri) : SettingsEvent
    data class ImportData(val uri: android.net.Uri) : SettingsEvent

    // 重置进度
    data class ResetProgress(val includeCloud: Boolean) : SettingsEvent

    // 修复数据 (清理重复)
    data object RepairLocalData : SettingsEvent
}
