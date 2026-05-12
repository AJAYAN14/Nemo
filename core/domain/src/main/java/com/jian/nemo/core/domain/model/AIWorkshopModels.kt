package com.jian.nemo.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AIExercise(
    val id: Int = 0,
    val question: String,
    val type: String, // CN_TO_JP, JP_TO_CN
    val difficulty: String,
    val answer: String,
    val hints: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class AIGradeResult(
    val score: Int,
    val feedback: String,
    val is_correct: Boolean,
    val standard_answer: String? = null
)

data class AIExerciseHistory(
    val id: Int,
    val question: String,
    val type: String,
    val difficulty: String,
    val standardAnswer: String,
    val userAnswer: String,
    val score: Int,
    val feedback: String,
    val createdAt: Long,
    val grammarPoint: String? = null
)
