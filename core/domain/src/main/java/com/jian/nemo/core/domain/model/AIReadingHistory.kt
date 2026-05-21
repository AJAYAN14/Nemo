package com.jian.nemo.core.domain.model

data class AIReadingHistory(
    val id: Int,
    val title: String,
    val level: String,
    val contentRaw: String,
    val translation: String,
    val vocabulary: List<ReadingVocabulary>,
    val questions: List<ReadingQuestion>,
    val selectedAnswers: List<Int?>,
    val isSubmitted: Boolean,
    val createdAt: Long
)
