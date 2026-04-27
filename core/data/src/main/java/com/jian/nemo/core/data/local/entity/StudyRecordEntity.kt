package com.jian.nemo.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 学习记录实体
 * 用于记录用户每日的学习情况
 */
@Entity(tableName = "study_records")
data class StudyRecordEntity(
    /**
     * 学习日期 (Epoch Day)
     * 作为主键，每天只有一条记录
     */
    @PrimaryKey
    val date: Long,

    /**
     * 今日学习的单词数
     */
    @ColumnInfo(name = "learned_words")
    val learnedWords: Int = 0,

    /**
     * 今日学习的语法数
     */
    @ColumnInfo(name = "learned_grammars")
    val learnedGrammars: Int = 0,

    /**
     * 今日复习的单词数
     */
    @ColumnInfo(name = "reviewed_words")
    val reviewedWords: Int = 0,

    /**
     * 今日复习的语法数
     */
    @ColumnInfo(name = "reviewed_grammars")
    val reviewedGrammars: Int = 0,

    /**
     * 今日跳过的单词数
     */
    @ColumnInfo(name = "skipped_words")
    val skippedWords: Int = 0,

    /**
     * 今日跳过的语法数
     */
    @ColumnInfo(name = "skipped_grammars")
    val skippedGrammars: Int = 0,

    /**
     * 今日测试次数
     */
    @ColumnInfo(name = "test_count")
    val testCount: Int = 0,

    /**
     * 是否已删除
     */
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    /**
     * 删除时间戳
     */
    @ColumnInfo(name = "deleted_time")
    val deletedTime: Long = 0,

    /**
     * 记录创建/修改时间戳 (毫秒)
     */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
