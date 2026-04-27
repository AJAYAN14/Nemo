package com.jian.nemo.feature.learning.presentation

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
    val status: LearningStatus = LearningStatus.Idle,

    // 当前单词
    val currentWord: Word? = null,
    val currentIndex: Int = 0,

    // 单词列表
    val wordList: List<Word> = emptyList(),

    // 语法列表（Phase 10）
    val currentGrammar: Grammar? = null,
    val grammarList: List<Grammar> = emptyList(),
    val currentGrammarIndex: Int = 0,           // 当前语法索引
    val isGrammarDetailVisible: Boolean = false, // 语法详情是否可见

    // 卡片状态
    val isCardFlipped: Boolean = false,  // 是否翻转显示答案

    // 学习设置
    val selectedLevel: String = "N5",
    val dailyGoal: Int = 20,  // 默认值改为20，与旧项目保持一致
    val completedToday: Int = 0,

    // SRS State
    val isAnswerShown: Boolean = false,
    val sessionQueue: List<Word> = emptyList(), // 暂未完全实现，预留
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

    // [Fix]: Session Stats for Learning Mode
    val sessionCorrectCount: Int = 0, // 本次会话正确数

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
    
    // 报告错误相关
    val showReportErrorDialog: Boolean = false,
    val successMessage: String? = null,

    /** 当前显示模式 (null=系统, true=深色, false=浅色) */
    val isDarkMode: Boolean? = null
) {
    val hasPendingItems: Boolean
        get() = when (learningMode) {
            LearningMode.Word -> currentWord != null || wordList.isNotEmpty()
            LearningMode.Grammar -> currentGrammar != null || grammarList.isNotEmpty()
        }

    val shouldShowDailyGoalMet: Boolean
        get() = completedToday >= dailyGoal && !hasPendingItems
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
    NoMoreWords,       // 无更多单词
    Error              // 错误
}

