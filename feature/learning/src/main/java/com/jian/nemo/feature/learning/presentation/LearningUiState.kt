package com.jian.nemo.feature.learning.presentation

import com.jian.nemo.core.domain.model.LearningMode
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.model.Grammar

/**
 * 滑动方向枚举
 */
enum class SlideDirection {
    FORWARD,  // 向前（下一个）
    BACKWARD  // 向后（上一个）
}

/**
 * 学习界面 UI 状态
 * 遵循 MVI 模式：单一不可变数据源
 */
data class LearningUiState(
    // 学习模式
    val learningMode: LearningMode = LearningMode.Word,

    // 当前状态
    val status: LearningStatus = LearningStatus.Loading,

    // 当前单词
    val currentWord: Word? = null,
    val currentIndex: Int = 0,

    // 单词列表 (代表当前活动队列。注意：其 size 即为 UI 显示的“剩余数量”)
    val wordList: List<Word> = emptyList(),

    // 语法列表（Phase 10）
    val currentGrammar: Grammar? = null,
    // 语法列表 (代表当前活动队列。其 size 即为 UI 显示的“剩余数量”)
    val grammarList: List<Grammar> = emptyList(),
    val currentGrammarIndex: Int = 0,           // 当前语法索引
    val isGrammarDetailVisible: Boolean = false, // 语法详情是否可见

    // 卡片状态
    val isCardFlipped: Boolean = false,  // 是否翻转显示答案

    // 学习设置
    val selectedLevel: String = "GLOBAL",
    val dailyGoal: Int = 20,  // 每日目标默认值 20
    val completedToday: Int = 0,

    // SRS State
    val isAnswerShown: Boolean = false,
    val completedThisSession: Int = 0,
    val ratingIntervals: Map<Int, String> = emptyMap(), // key: quality 0-5, value: formatted interval text (e.g. "3d"), // 本次会话已完成数量
    val slideDirection: SlideDirection = SlideDirection.FORWARD, // 滑动方向，用于动画
    val isNavigating: Boolean = false, // 是否正在导航（防抖用）

    // 复习相关
    val reviewDueCount: Int = 0,  // 待复习单词数量

    // 打字练习
    val showTypingPractice: Boolean = false,

    // 错误信息
    val error: String? = null,

    // 会话初始大小（用于进度条显示，防止重入队导致总数一直增加）
    val sessionInitialSize: Int = 0,

    // [Fix]: Track processed items (Pass + Fail) for accurate Remaining Count
    val sessionProcessedCount: Int = 0,

    // 撤销功能
    val canUndo: Boolean = false,

    // Learn Ahead Limit 等待状态
    val waitingUntil: Long = 0L,  // 等待目标时间戳（毫秒）

    // 朗读状态
    val playingAudioId: String? = null,  // 当前正在朗读的 ID (null 表示未朗读)

    // 自定义开关
    val isAutoAudioEnabled: Boolean = false,  // 是否开启翻面自动朗读
    val isShowAnswerDelayEnabled: Boolean = false,
    val showAnswerDelayMs: Long = 5000L,
    val showAnswerAvailableAt: Long = 0,
    val isAutoRevealAnswerEnabled: Boolean = false,
    val autoRevealAnswerMs: Long = 5000L,
    val isGrammarAutoRevealAnswerEnabled: Boolean = false,
    val grammarAutoRevealAnswerMs: Long = 10000L,
    
    // 报告错误相关
    val showReportErrorDialog: Boolean = false,
    val successMessage: String? = null,

    // 队列状态计数（用于进度条旁的四色数字显示）
    val queueNewCount: Int = 0,
    val queueLearningCount: Int = 0,
    val queueReviewCount: Int = 0,
    val queueRelearnCount: Int = 0,

    /** 当前显示模式 (null=系统, true=深色, false=浅色) */
    val isDarkMode: Boolean? = null,
    
    /** 是否启用原生画板 */
    val isWhiteboardEnabled: Boolean = false,

    // 结算统计数据
    val sessionDurationSeconds: Long = 0L,
    val sessionMaxCombo: Int = 0,
    val sessionNewCount: Int = 0,
    val sessionReviewCount: Int = 0,
    val sessionRelearnCount: Int = 0,
    val tomorrowReviewForecastCount: Int = 0,
    val todayDueReviewCount: Int = 0,
    val maxDailyReviews: Int = 0,
    val todayReviewedCount: Int = 0,
    val dailyStreak: Int = 1,
    val settingIconStyle: com.jian.nemo.core.domain.repository.SettingIconStyle = com.jian.nemo.core.domain.repository.SettingIconStyle.SOFT_TRANSLUCENT
) {
    val hasPendingItems: Boolean
        get() = when (learningMode) {
            LearningMode.Word -> currentWord != null || wordList.isNotEmpty()
            LearningMode.Grammar -> currentGrammar != null || grammarList.isNotEmpty()
        }

    /** 今日新词/语法目标是否已达成 */
    val isDailyNewGoalMet: Boolean
        get() = if (dailyGoal > 0) completedToday >= dailyGoal else true

    /** 今日剩余可复习配额（受每日复习上限与实际到期待复习量约束） */
    val remainingDailyReviewCount: Int
        get() {
            if (todayDueReviewCount <= 0) return 0
            if (maxDailyReviews <= 0) return todayDueReviewCount
            val remainingQuota = (maxDailyReviews - todayReviewedCount).coerceAtLeast(0)
            return minOf(todayDueReviewCount, remainingQuota)
        }

    /** 今日是否还有未完成的常规复习任务 */
    val hasRemainingReviewTasks: Boolean
        get() = remainingDailyReviewCount > 0

    /** 今日全部常规任务（新学与复习）是否已彻底完成 */
    val isTodayFullyCompleted: Boolean
        get() = isDailyNewGoalMet && !hasRemainingReviewTasks && !hasPendingItems

    /** 新学目标已达成，但仍有今日复习任务待继续 */
    val isNewGoalMetWithReviewPending: Boolean
        get() = isDailyNewGoalMet && hasRemainingReviewTasks && !hasPendingItems

    val shouldShowDailyGoalMet: Boolean
        get() = completedToday >= dailyGoal && !hasPendingItems

    /** 本组微会话独立完成的单词/语法总数（新学 + 复习，攻克属于复习子集，杜绝重复计算） */
    val sessionTotalCount: Int
        get() = (sessionNewCount + maxOf(sessionReviewCount, sessionRelearnCount)).coerceAtLeast(1)

    /** 顺畅巩固复习数（一次性通过、未触发重学攻克） */
    val smoothReviewCount: Int
        get() = (sessionReviewCount - sessionRelearnCount).coerceAtLeast(0)
}

/**
 * 学习状态
 */
enum class LearningStatus {
    Idle,              // 空闲
    Loading,           // 加载中
    Learning,          // 学习中
    Processing,        // 处理中（掌握/跳过）
    Waiting,           // 等待中（Learn Ahead Limit 超限）
    SessionCompleted,  // 会话完成
    Error              // 错误
}

