package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.AIExerciseHistory
import com.jian.nemo.core.domain.model.AIReadingArticle
import com.jian.nemo.core.domain.model.AIReadingHistory
import kotlinx.coroutines.flow.Flow

interface AIWorkshopRepository {
    fun getExerciseHistory(): Flow<List<AIExerciseHistory>>
    
    suspend fun saveExercise(
        question: String,
        type: String,
        difficulty: String,
        standardAnswer: String,
        userAnswer: String,
        score: Int,
        feedback: String,
        grammarPoint: String? = null,
        usageId: Int? = null
    )
    
    suspend fun deleteOldHistory(days: Int = 30)
    
    suspend fun clearHistory()

    // ========== AI 智能阅读历史 ==========
    
    fun getReadingHistory(): Flow<List<AIReadingHistory>>

    suspend fun saveReadingHistory(
        article: AIReadingArticle,
        selectedAnswers: List<Int?>,
        isSubmitted: Boolean
    ): Long

    suspend fun updateReadingAnswers(
        id: Int,
        selectedAnswers: List<Int?>,
        isSubmitted: Boolean
    )

    suspend fun deleteReadingHistoryById(id: Int)

    suspend fun clearReadingHistory()
}
