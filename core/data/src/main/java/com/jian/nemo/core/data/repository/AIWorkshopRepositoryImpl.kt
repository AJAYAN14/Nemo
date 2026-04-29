package com.jian.nemo.core.data.repository

import com.jian.nemo.core.data.local.dao.AIExerciseDao
import com.jian.nemo.core.data.local.entity.AIExerciseEntity
import com.jian.nemo.core.domain.model.AIExerciseHistory
import com.jian.nemo.core.domain.repository.AIWorkshopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIWorkshopRepositoryImpl @Inject constructor(
    private val aiExerciseDao: AIExerciseDao
) : AIWorkshopRepository {

    override fun getExerciseHistory(): Flow<List<AIExerciseHistory>> {
        return aiExerciseDao.getAllHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveExercise(
        question: String,
        type: String,
        difficulty: String,
        standardAnswer: String,
        userAnswer: String,
        score: Int,
        feedback: String
    ) {
        val entity = AIExerciseEntity(
            question = question,
            type = type,
            difficulty = difficulty,
            standardAnswer = standardAnswer,
            userAnswer = userAnswer,
            score = score,
            feedback = feedback
        )
        aiExerciseDao.insert(entity)
    }

    override suspend fun deleteOldHistory(days: Int) {
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        aiExerciseDao.deleteOldRecords(cutoff)
    }

    override suspend fun clearHistory() {
        aiExerciseDao.clearAll()
    }

    private fun AIExerciseEntity.toDomain() = AIExerciseHistory(
        id = id,
        question = question,
        type = type,
        difficulty = difficulty,
        standardAnswer = standardAnswer,
        userAnswer = userAnswer,
        score = score,
        feedback = feedback,
        createdAt = createdAt
    )
}
