package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.TestPreferences
import kotlinx.coroutines.flow.Flow

/**
 * 设置Repository接口
 *


 */
interface SettingsRepository {

    // ========== 测试配置 (原子化 Flow) ==========
    val testPreferencesFlow: Flow<TestPreferences>

    companion object {
        const val DEFAULT_LAST_SYNC_TIME = 0L
    }

    // ========== 用户设置 ==========

    /**
     * 用户头像路径Flow
     */
    val userAvatarPathFlow: Flow<String>

    /**
     * 设置用户头像路径
     */
    suspend fun setUserAvatarPath(path: String)

    /**
     * 清除用户头像
     */
    suspend fun clearUserAvatar()

    /**
     * 每日学习目标Flow
     * 默认值: 50
     */
    val dailyGoalFlow: Flow<Int>

    /**
     * 设置每日学习目标
     */
    suspend fun setDailyGoal(goal: Int)

    /**
     * 每日语法学习目标Flow
     * 默认值: 10
     */
    val grammarDailyGoalFlow: Flow<Int>

    /**
     * 设置每日语法学习目标
     */
    suspend fun setGrammarDailyGoal(goal: Int)

    /**
     * 主题色 Flow (ARGB Long, null = 默认品牌蓝)
     */
    val themeColorFlow: Flow<Long?>

    /**
     * 设置主题色
     * @param colorArgb ARGB Long 值，null 表示恢复默认
     */
    suspend fun setThemeColor(colorArgb: Long?)

    /**
     * 深色模式Flow
     * null = 跟随系统, true = 深色, false = 浅色
     */
    val isDarkModeFlow: Flow<Boolean?>

    /**
     * 设置深色模式
     */
    suspend fun setDarkMode(enabled: Boolean?)

    /**
     * 深色模式策略Flow
     * "system" = 跟随系统, "scheduled" = 定时切换
     */
    val darkModeStrategyFlow: Flow<String>

    /**
     * 设置深色模式策略
     */
    suspend fun setDarkModeStrategy(strategy: String)

    /**
     * 浅色模式开始时间Flow (HH:mm)
     */
    val darkModeStartTimeFlow: Flow<String>

    /**
     * 设置浅色模式开始时间
     */
    suspend fun setDarkModeStartTime(time: String)

    /**
     * 深色模式开始时间Flow (HH:mm)
     */
    val darkModeEndTimeFlow: Flow<String>

    /**
     * 设置深色模式开始时间
     */
    suspend fun setDarkModeEndTime(time: String)

    /**
     * 动态颜色开关Flow
     * 默认值: true
     */
    val isDynamicColorEnabledFlow: Flow<Boolean>

    /**
     * 设置动态颜色
     */
    suspend fun setDynamicColorEnabled(enabled: Boolean)

    // ========== 学习日重置时间 ==========

    /**
     * 学习日重置时间 (小时, 0-23)
     * 默认值: 4 (凌晨4:00)
     *
     * 用于零点跨天处理：学习日在此时间刷新，而非凌晨0:00
     */
    val learningDayResetHourFlow: Flow<Int>

    /**
     * 设置学习日重置时间
     */
    suspend fun setLearningDayResetHour(hour: Int)

    // ========== 学习统计 ==========

    /**
     * 连续学习天数Flow
     * 默认值: 0
     */
    val dailyStreakFlow: Flow<Int>

    /**
     * 最后学习日期Flow（Epoch Day）
     * 默认值: 0
     */
    val lastStudyDateFlow: Flow<Long>

    /**
     * 累计学习天数Flow
     * 默认值: 0
     */
    val totalStudyDaysFlow: Flow<Int>

    /**
     * 更新连续学习天数
     *
     * 逻辑:
     * 1. 如果今天已经更新过，不重复更新
     * 2. 如果昨天有学习，streak +1
     * 3. 如果超过1天没学习，streak重置为1
     */
    suspend fun updateDailyStreak()

    /**
     * 连续测试天数Flow
     * 默认值: 0
     */
    val testStreakFlow: Flow<Int>

    /**
     * 最高连续测试天数Flow
     * 默认值: 0
     */
    val maxTestStreakFlow: Flow<Int>

    /**
     * 更新连续测试天数
     */
    suspend fun updateTestStreak()

    /**
     * 检查日期是否变化
     *
     * @return true表示日期已变化，需要清空今日进度
     */
    suspend fun isDateChanged(): Boolean

    /**
     * 恢复学习统计数据 (用于同步恢复)
     */
    suspend fun restoreStudyStats(totalStudyDays: Int, dailyStreak: Int, lastStudyDate: Long, maxTestStreak: Int, testStreak: Int)

    // ========== 应用配置 ==========

    /**
     * 是否首次启动Flow
     * 默认值: true
     */
    val isFirstLaunchFlow: Flow<Boolean>

    /**
     * 标记首次启动完成
     */
    /**
     * 标记首次启动完成
     */
    suspend fun setFirstLaunchCompleted()

    // ========== 今日复习记录 (临时统计) ==========

    suspend fun addTodayTestedWordId(id: Int)
    suspend fun addTodayWrongWordId(id: Int)
    suspend fun addTodayTestedGrammarId(id: Int)
    suspend fun addTodayWrongGrammarId(id: Int)

    // P3修复: 获取今日测试的单词或语法ID
    suspend fun getTodayTestedWordIds(): Set<Int>
    suspend fun getTodayTestedGrammarIds(): Set<Int>
    suspend fun clearTodayTestedIds()

    // ========== 测试配置 ==========

    /** 测试题目数量Flow 默认: 20 */
    val testQuestionCountFlow: Flow<Int>
    suspend fun setTestQuestionCount(count: Int)

    /** 测试时间限制(分钟)Flow 默认: 0 */
    val testTimeLimitMinutesFlow: Flow<Int>
    suspend fun setTestTimeLimitMinutes(minutes: Int)

    /** 题目乱序Flow 默认: true */
    val testShuffleQuestionsFlow: Flow<Boolean>
    suspend fun setTestShuffleQuestions(enabled: Boolean)

    /** 选项乱序Flow 默认: true */
    val testShuffleOptionsFlow: Flow<Boolean>
    suspend fun setTestShuffleOptions(enabled: Boolean)

    /** 自动跳转Flow 默认: true */
    val testAutoAdvanceFlow: Flow<Boolean>
    suspend fun setTestAutoAdvance(enabled: Boolean)

    /** 错题优先Flow 默认: false */
    val testPrioritizeWrongFlow: Flow<Boolean>
    suspend fun setTestPrioritizeWrong(enabled: Boolean)

    /** 未学优先Flow 默认: false */
    val testPrioritizeNewFlow: Flow<Boolean>
    suspend fun setTestPrioritizeNew(enabled: Boolean)

    /** 题目来源Flow 默认: "all" */
    val testQuestionSourceFlow: Flow<String>
    suspend fun setTestQuestionSource(source: String)

    /** 错题移除阈值Flow 默认: 1 */
    val testWrongAnswerRemovalThresholdFlow: Flow<Int>
    suspend fun setTestWrongAnswerRemovalThreshold(threshold: Int)

    /** 测试内容类型Flow 默认: "words" */
    val testContentTypeFlow: Flow<String>
    suspend fun setTestContentType(type: String)

    /** 选中的单词等级Flow 默认: setOf("N5", "N4", "N3", "N2", "N1") */
    val testSelectedWordLevelsFlow: Flow<Set<String>>
    suspend fun setTestSelectedWordLevels(levels: Set<String>)

    /** 选中的语法等级Flow 默认: setOf("N5", "N4", "N3", "N2", "N1") */
    val testSelectedGrammarLevelsFlow: Flow<Set<String>>
    suspend fun setTestSelectedGrammarLevels(levels: Set<String>)

    /**
     * 设置当前测试模式上下文
     *
     * 调用此方法后，所有 test...Flow 将自动切换为对应模式的配置数据流
     * @param mode 测试模式ID (null 表示默认/通用模式)
     */
    fun setContextTestMode(mode: String?)

    /** 保存完整的测试配置（含综合题型计数，单次原子写入） */
    suspend fun saveTestConfig(
        questionCount: Int,
        timeLimitMinutes: Int,
        shuffleQuestions: Boolean,
        shuffleOptions: Boolean,
        autoAdvance: Boolean,
        prioritizeWrong: Boolean,
        prioritizeNew: Boolean,
        questionSource: String,
        wrongAnswerRemovalThreshold: Int,
        testContentType: String,
        selectedWordLevels: Set<String>,
        selectedGrammarLevels: Set<String>,
        comprehensiveMultipleChoiceCount: Int,
        comprehensiveTypingCount: Int,
        comprehensiveCardMatchingCount: Int,
        comprehensiveSortingCount: Int
    )

    // ========== 新内容策略 ==========

    /**
     * 新内容随机抽取开关Flow
     * 默认值: true
     */
    val isRandomNewContentEnabledFlow: Flow<Boolean>

    /**
     * 设置新内容随机抽取开关
     */
    suspend fun setRandomNewContentEnabled(enabled: Boolean)

    // ========== 综合测试各个题型数量 ==========

    val comprehensiveTestMultipleChoiceCountFlow: Flow<Int>
    suspend fun saveComprehensiveTestMultipleChoiceCount(count: Int)

    val comprehensiveTestTypingCountFlow: Flow<Int>
    suspend fun saveComprehensiveTestTypingCount(count: Int)

    val comprehensiveTestCardMatchingCountFlow: Flow<Int>
    suspend fun saveComprehensiveTestCardMatchingCount(count: Int)

    val comprehensiveTestSortingCountFlow: Flow<Int>
    suspend fun saveComprehensiveTestSortingCount(count: Int)

    // ========== 学习状态持久化 ==========

    /**
     * 上次选择的学习模式Flow
     * 默认: "word" (对应 LearningMode.Word)
     */
    val lastLearningModeFlow: Flow<String>

    /**
     * 保存上次选择的学习模式
     */
    suspend fun setLastLearningMode(mode: String)

    /**
     * 偏好单词等级Flow
     * 默认: "N5"
     */
    val preferredWordLevelFlow: Flow<String>

    /**
     * 设置偏好单词等级
     */
    suspend fun setPreferredWordLevel(level: String)

    /**
     * 偏好语法等级Flow
     * 默认: "N5"
     */
    val preferredGrammarLevelFlow: Flow<String>

    /**
     * 设置偏好语法等级
     */
    suspend fun setPreferredGrammarLevel(level: String)

    // ========== 学习会话持久化 (单词) ==========

    /**
     * 保存单词学习会话
     * @param ids 单词ID列表
     * @param currentIndex 当前索引
     * @param level 当前等级
     * @param steps 学习步长状态 (WordId -> Step)
     * @param waitingUntil 等待结束时间 (Epoch Millis)
     */
    suspend fun saveWordSession(ids: List<Int>, currentIndex: Int, level: String, steps: Map<Int, Int>, waitingUntil: Long = 0L)

    /**
     * 获取单词学习会话
     * @return Triple(IDs, CurrentIndex, Level, Steps) or null if invalid/empty
     * Note: Returning Data Class would be better but keeping signature simple for now.
     * Actually, let's use a Quadruple or return a Helper Class.
     * Since Kotlin doesn't have Quadruple, let's return a data class `WordSession`.
     * Or just Map as 4th element.
     * `Flow<SessionState?>` where SessionState holds all.
     * To minimize changes, let's use `Flow<SessionData?>`.
     */
    // For now, let's keep it simple and just return the Steps map in a wrapper or just change return type.
    // Flow<SessionData?> is cleaner. Let's define SessionData in this file.
    suspend fun getWordSession(): Flow<SessionData?>

    /**
     * 清除单词学习会话
     */
    suspend fun clearWordSession()

    // ========== 学习会话持久化 (语法) ==========

    /**
     * 保存语法学习会话
     * @param ids 语法ID列表
     * @param currentIndex 当前索引
     * @param level 当前等级
     * @param steps 学习步长状态 (GrammarId -> Step)
     * @param waitingUntil 等待结束时间 (Epoch Millis)
     */
    suspend fun saveGrammarSession(ids: List<Int>, currentIndex: Int, level: String, steps: Map<Int, Int>, waitingUntil: Long = 0L)

    /**
     * 获取语法学习会话
     */
    suspend fun getGrammarSession(): Flow<SessionData?>

    /**
     * 清除语法学习会话
     */
    suspend fun clearGrammarSession()

    /** 记忆算法参数 (Lapse) */
    val wordLapsesFlow: Flow<Map<Int, Int>>
    val grammarLapsesFlow: Flow<Map<Int, Int>>

    suspend fun incrementWordLapse(wordId: Int)
    suspend fun incrementGrammarLapse(grammarId: Int)

    suspend fun resetWordLapse(wordId: Int)
    suspend fun resetGrammarLapse(grammarId: Int)

    // ========== 恢复状态管理 ==========

    /** 是否正在恢复中（用于识别被异常中断的任务） */
    val isRestoringFlow: Flow<Boolean>
    suspend fun setIsRestoring(isRestoring: Boolean)

    // ========== 自动同步配置 ==========

    /** 自动同步开关Flow 默认: true */
    val isAutoSyncEnabledFlow: Flow<Boolean>
    suspend fun setAutoSyncEnabled(enabled: Boolean)

    /** 上t次同步时间Flow 默认: 0 */
    val lastSyncTimeFlow: Flow<Long>
    suspend fun setLastSyncTime(time: Long)

    /** 上t次同步是否成功Flow 默认: true */
    val lastSyncSuccessFlow: Flow<Boolean>
    suspend fun setLastSyncSuccess(success: Boolean)

    /** 上t次同步错误信息Flow 默认: "" */
    val lastSyncErrorFlow: Flow<String>
    suspend fun setLastSyncError(error: String)

    /** 上次恢复时间Flow */
    val lastRestoreTimeFlow: Flow<Long>
    suspend fun setLastRestoreTime(time: Long)

    /** 词库云更新：上次已应用的内容版本号（0 = 未更新过） */
    suspend fun getLastContentVersion(): Int
    suspend fun setLastContentVersion(version: Int)

    /** 上次同步冲突数量Flow */
    val lastSyncConflictCountFlow: Flow<Int>
    suspend fun setLastSyncConflictCount(count: Int)

    /** 学习完成后是否自动同步Flow 默认: true */
    val isSyncOnLearningCompleteFlow: Flow<Boolean>
    suspend fun setSyncOnLearningComplete(enabled: Boolean)

    /** 测试完成后同步Flow 默认: true */
    val isSyncOnTestCompleteFlow: Flow<Boolean>
    suspend fun setSyncOnTestComplete(enabled: Boolean)

    // ========== 学习高级设置 ==========

    /** 学习步进Flow (由于是UI输入，使用String) @see PreferencesKeys.LEARNING_STEPS */
    val learningStepsFlow: Flow<String>
    suspend fun setLearningSteps(steps: String)

    /** 提前学习限制Flow (分钟) @see PreferencesKeys.LEARN_AHEAD_LIMIT */
    val learnAheadLimitFlow: Flow<Int>
    suspend fun setLearnAheadLimit(minutes: Int)

    /** Leech 阈值Flow (累计 lapse 次数) 默认: 5 */
    val leechThresholdFlow: Flow<Int>
    suspend fun setLeechThreshold(threshold: Int)

    /** Leech 行为Flow 默认: "skip" (skip | bury_today) */
    val leechActionFlow: Flow<String>
    suspend fun setLeechAction(action: String)

    /** 重学步进Flow (例如 "1 10") @see PreferencesKeys.RELEARNING_STEPS */
    val relearningStepsFlow: Flow<String>
    suspend fun setRelearningSteps(steps: String)

    /** 原子化保存所有高级学习设置，更新修改时间戳 */
    suspend fun saveAdvancedLearningSettings(
        learningSteps: String,
        relearningSteps: String,
        learnAheadLimit: Int,
        leechThreshold: Int,
        leechAction: String
    )

    // ========== TTS 设置 ==========

    /** 朗读语速Flow 默认: 1.0f */
    val ttsSpeechRateFlow: Flow<Float>
    suspend fun setTtsSpeechRate(rate: Float)

    /** 朗读音调Flow 默认: 1.0f */
    val ttsPitchFlow: Flow<Float>
    suspend fun setTtsPitch(pitch: Float)

    /** 朗读语音名称 (Locale + Name) 默认: null */
    val ttsVoiceNameFlow: Flow<String?>
    suspend fun setTtsVoiceName(voiceName: String?)

    /** 翻面自动朗读开关Flow 默认: false */
    val isAutoPlayAudioEnabledFlow: Flow<Boolean>
    suspend fun setAutoPlayAudioEnabled(enabled: Boolean)

    /** 显示答案等待开关 Flow 默认: false */
    val isShowAnswerDelayEnabledFlow: Flow<Boolean>
    suspend fun setShowAnswerDelayEnabled(enabled: Boolean)

    /** 显示答案等待时长 (ms) Flow 默认: 5000，可选: 2000/3000/4000/5000 */
    val showAnswerDelayMsFlow: Flow<Long>
    suspend fun setShowAnswerDelayMs(ms: Long)

    /**
     * 清除用户相关数据 (保留设备配置)
     * 用于单用户模式登出清理
     */
    suspend fun clearUserData()

    /**
     * 重置学习统计数据 (用于重置学习进度)
     * 包含:
     * - 连续/累计学习天数
     * - 连续/最高测试连胜
     * - 记忆算法参数 (Lapse)
     * - 今日临时数据
     */
    suspend fun resetLearningStats()

    // ========== 数据修复 ==========

    /**
     * 修复本地数据 (清理重复数据)
     * @return 清理的记录数量
     */
    suspend fun repairLocalData(): Int

    // ========== 恢复断点续传 ==========

    /**
     * 设置恢复断点
     * @param table 当前正在恢复的表名
     * @param offset 当前已恢复的偏移量
     */
    suspend fun setRestoreCheckpoint(table: String, offset: Int)

    /**
     * 获取恢复断点
     * @return Pair(TableName, Offset)，如果没有断点则返回 null 或默认值
     */
    suspend fun getRestoreCheckpoint(): Pair<String, Int>?

    /**
     * 清除恢复断点
     */
    suspend fun clearRestoreCheckpoint()

    // ========== App Settings Sync ==========

    /**
     * Get a snapshot of current app settings for sync
     */
    suspend fun getAppSettingsSnapshot(): com.jian.nemo.core.domain.model.AppSettings

    /**
     * Apply app settings from sync
     */
    suspend fun applyAppSettingsSnapshot(settings: com.jian.nemo.core.domain.model.AppSettings)

    /**
     * Get the time when settings were last modified (for sync check)
     */
    val lastSettingsModifiedTimeFlow: Flow<Long>
    suspend fun updateLastSettingsModifiedTime()

    // ========== 通知管理 ==========

    /** 已关闭的通知 ID 集合 Flow */
    val dismissedNotificationIdsFlow: Flow<Set<String>>

    /** 添加已关闭的通知 ID */
    suspend fun addDismissedNotificationId(id: String)

    // ========== 应用图标设置 ==========

    /** 
     * 应用图标名称 Flow 
     * 可选值: "Nemo" (默认), "Gold", "Daruma", "Zen"
     */
    val appIconFlow: Flow<String>

    /**
     * 设置应用图标
     */
    suspend fun setAppIcon(iconName: String)
}

/**
 * 学习会话数据封装
 */
data class SessionData(
    val ids: List<Int>,
    val currentIndex: Int,
    val level: String,
    val steps: Map<Int, Int>, // ID -> Step Index
    val waitingUntil: Long = 0L // 新增
)
