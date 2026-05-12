package com.jian.nemo.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_exercises")
data class AIExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val question: String,
    val type: String,
    val difficulty: String,
    val standardAnswer: String,
    val userAnswer: String,
    val score: Int,
    val feedback: String,
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * 语法专项模式下的语法点名称（自由模式为 null）
     */
    @ColumnInfo(name = "grammar_point")
    val grammarPoint: String? = null,

    /**
     * 语法专项模式下对应的用法分支 ID（自由模式为 null）
     */
    @ColumnInfo(name = "usage_id")
    val usageId: Int? = null
)
