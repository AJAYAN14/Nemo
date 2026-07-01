package com.jian.nemo.core.data.local.entity

import androidx.room.ColumnInfo

/**
 * 单词进度增量更新对象
 * 用于 Room @Update(entity = WordStudyStateEntity::class) 部分更新
 */
data class WordStudyStateUpdate(
    @ColumnInfo(name = "word_id")
    val wordId: Int,

    @ColumnInfo(name = "repetition_count")
    val repetitionCount: Int,

    @ColumnInfo(name = "stability")
    val stability: Float,

    @ColumnInfo(name = "difficulty")
    val difficulty: Float,

    @ColumnInfo(name = "interval")
    val interval: Int,

    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean,

    @ColumnInfo(name = "is_skipped")
    val isSkipped: Boolean,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean,

    @ColumnInfo(name = "deleted_time")
    val deletedTime: Long,

    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long,

    @ColumnInfo(name = "last_reviewed_date")
    val lastReviewedDate: Long?,

    @ColumnInfo(name = "first_learned_date")
    val firstLearnedDate: Long?
)

/**
 * 语法进度增量更新对象
 * 用于 Room @Update(entity = GrammarStudyStateEntity::class) 部分更新
 */
data class GrammarStudyStateUpdate(
    @ColumnInfo(name = "grammar_id")
    val grammarId: Int,

    @ColumnInfo(name = "repetition_count")
    val repetitionCount: Int,

    @ColumnInfo(name = "stability")
    val stability: Float,

    @ColumnInfo(name = "difficulty")
    val difficulty: Float,

    @ColumnInfo(name = "interval")
    val interval: Int,

    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean,

    @ColumnInfo(name = "is_skipped")
    val isSkipped: Boolean,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean,

    @ColumnInfo(name = "deleted_time")
    val deletedTime: Long,

    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long,

    @ColumnInfo(name = "last_reviewed_date")
    val lastReviewedDate: Long?,

    @ColumnInfo(name = "first_learned_date")
    val firstLearnedDate: Long?
)
