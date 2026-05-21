package com.jian.nemo.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_reading_history")
data class AIReadingHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val level: String,
    @ColumnInfo(name = "content_raw")
    val contentRaw: String,
    val translation: String,
    @ColumnInfo(name = "vocabulary_json")
    val vocabularyJson: String,
    @ColumnInfo(name = "questions_json")
    val questionsJson: String,
    @ColumnInfo(name = "selected_answers_json")
    val selectedAnswersJson: String,
    @ColumnInfo(name = "is_submitted")
    val isSubmitted: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
