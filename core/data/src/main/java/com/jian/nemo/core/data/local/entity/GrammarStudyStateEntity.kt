package com.jian.nemo.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jian.nemo.core.common.util.DateTimeUtils

/**
 * 语法学习状态实体
 */
@Entity(
    tableName = "grammar_study_states",
    indices = [
        Index(value = ["next_review_date"]),
        Index(value = ["last_modified_time"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = GrammarEntity::class,
            parentColumns = ["id"],
            childColumns = ["grammar_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GrammarStudyStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "grammar_id")
    val grammarId: Int,

    // ========== SRS 复习字段 (FSRS 6) ==========
    @ColumnInfo(name = "repetition_count")
    val repetitionCount: Int = 0,

    @ColumnInfo(name = "stability")
    val stability: Float = 0f,

    @ColumnInfo(name = "difficulty")
    val difficulty: Float = 0f,

    @ColumnInfo(name = "interval")
    val interval: Int = 0,

    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long = 0,

    @ColumnInfo(name = "last_reviewed_date")
    val lastReviewedDate: Long? = null,

    @ColumnInfo(name = "first_learned_date")
    val firstLearnedDate: Long? = null,

    /** 卡片类型 (0=New, 1=Learn, 2=Review, 3=Relearn) */
    @ColumnInfo(name = "type")
    val type: Int = 0,

    // ========== 用户交互字段 ==========
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_skipped")
    val isSkipped: Boolean = false,

    @ColumnInfo(name = "buried_until_day")
    val buriedUntilDay: Long = 0,

    // ========== 元数据 ==========
    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long = DateTimeUtils.getCurrentCompensatedMillis(),

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "deleted_time")
    val deletedTime: Long = 0
)
