package com.jian.nemo.core.data.local.entity

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
    val createdAt: Long = System.currentTimeMillis()
)
