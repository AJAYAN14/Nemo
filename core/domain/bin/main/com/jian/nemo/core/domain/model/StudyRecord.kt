package com.jian.nemo.core.domain.model

/**
 * 学习记录领域模型
 *
 * 参考: _reference/old-nemo/.../StudyRecord.kt
 *
 * 用于记录用户每日的学习情况
 */
data class StudyRecord(
    /**
     * 学习日期 (Epoch Day)
     * 作为主键，每天只有一条记录
     */
    val date: Long,

    /**
     * 今日学习的单词数
     */
    val learnedWords: Int = 0,

    /**
     * 今日学习的语法数
     */
    val learnedGrammars: Int = 0,

    /**
     * 今日复习的单词数
     */
    val reviewedWords: Int = 0,

    /**
     * 今日复习的语法数
     */
    val reviewedGrammars: Int = 0,

    /**
     * 今日跳过的单词数
     */
    val skippedWords: Int = 0,

    /**
     * 今日跳过的语法数
     */
    val skippedGrammars: Int = 0,

    /**
     * 今日测试次数
     */
    val testCount: Int = 0,

    /**
     * 记录创建时间戳 (毫秒)
     */
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 今日总学习数 (单词+语法)
     */
    val totalLearned: Int
        get() = learnedWords + learnedGrammars

    /**
     * 今日总复习数 (单词+语法)
     */
    val totalReviewed: Int
        get() = reviewedWords + reviewedGrammars

    /**
     * 今日总活动数
     */
    val totalActivity: Int
        get() = totalLearned + totalReviewed + testCount
}
