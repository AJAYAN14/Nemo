package com.jian.nemo.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jian.nemo.core.common.util.DateTimeUtils

/**
 * 单词学习状态实体
 * 专门存储用户相关的学习进度、SRS 参数及同步状态
 */
@Entity(
    tableName = "word_study_states",
    indices = [
        Index(value = ["next_review_date"]),
        Index(value = ["last_modified_time"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.NO_ACTION
        )
    ]
)
data class WordStudyStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "word_id")
    val wordId: Int,

    // ========== SRS 复习字段 (FSRS 6) ==========
    /** 重复次数 (0表示未学习) */
    @ColumnInfo(name = "repetition_count")
    val repetitionCount: Int = 0,

    /** 记忆稳定性 (FSRS) */
    @ColumnInfo(name = "stability")
    val stability: Float = 0f,

    /** 难度 (FSRS, 1-10) */
    @ColumnInfo(name = "difficulty")
    val difficulty: Float = 0f,

    /** 间隔天数 */
    @ColumnInfo(name = "interval")
    val interval: Int = 0,

    /** 下次复习日期 (Epoch Day) */
    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long = 0,

    /** 最后复习日期 (Epoch Day) */
    @ColumnInfo(name = "last_reviewed_date")
    val lastReviewedDate: Long? = null,

    /** 首次学习日期 (Epoch Day) */
    @ColumnInfo(name = "first_learned_date")
    val firstLearnedDate: Long? = null,

    /** 卡片类型 (0=New, 1=Learn, 2=Review, 3=Relearn) */
    @ColumnInfo(name = "type")
    val type: Int = 0,

    // ========== 用户交互字段 ==========
    /** 是否收藏 */
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    /** 是否被标记为跳过/暂停 */
    @ColumnInfo(name = "is_skipped")
    val isSkipped: Boolean = false,

    /** 今日暂缓到期日 (Epoch Day) */
    @ColumnInfo(name = "buried_until_day")
    val buriedUntilDay: Long = 0,

    // ========== 元数据 ==========
    /** 这里的 lastModifiedTime 只代表学习状态的变更时间 */
    @ColumnInfo(name = "last_modified_time")
    val lastModifiedTime: Long = DateTimeUtils.getCurrentCompensatedMillis(),

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "deleted_time")
    val deletedTime: Long = 0
)
