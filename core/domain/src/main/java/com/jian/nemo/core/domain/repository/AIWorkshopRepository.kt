package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.AIExerciseHistory
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
        feedback: String
    )
    
    suspend fun deleteOldHistory(days: Int = 30)
    
    suspend fun clearHistory()
}
