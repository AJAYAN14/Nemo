package com.jian.nemo.core.domain.model

/**
 * 学习统计数据模型
 *
 * 用于统计界面展示
 */
data class LearningStats(
    /**
     * 连续学习天数
     */
    val dailyStreak: Int,

    /**
     * 累计学习天数
     */
    val totalStudyDays: Int,

    /**
     * 今日学习单词数
     */
    val todayLearnedWords: Int,

    /**
     * 今日学习语法数
     */
    val todayLearnedGrammars: Int,

    /**
     * 今日复习单词数
     */
    val todayReviewedWords: Int,

    /**
     * 今日复习语法数
     */
    val todayReviewedGrammars: Int,

    /**
     * 已掌握单词数
     */
    val masteredWords: Int,

    /**
     * 已掌握语法数
     */
    val masteredGrammars: Int,

    /**
     * 待复习单词数
     */
    val dueWords: Int,

    /**
     * 待复习语法数
     */
    val dueGrammars: Int,

    /**
     * 每日单词学习目标
     */
    val wordDailyGoal: Int = 50,

    /**
     * 每日语法学习目标
     */
    val grammarDailyGoal: Int = 10,

    /**
     * 总单词数
     */
    val totalWords: Int = 0,

    /**
     * 总语法数
     */
    val totalGrammars: Int = 0,

    /**
     * 本周学习天数
     */
    val weekStudyDays: Int = 0
) {
    /**
     * 今日总学习数
     */
    val todayTotalLearned: Int
        get() = todayLearnedWords + todayLearnedGrammars

    /**
     * 今日总复习数
     */
    val todayTotalReviewed: Int
        get() = todayReviewedWords + todayReviewedGrammars

    /**
     * 总已掌握数
     */
    val totalMastered: Int
        get() = masteredWords + masteredGrammars

    /**
     * 总待复习数
     */
    val totalDue: Int
        get() = dueWords + dueGrammars
}
