package com.jian.nemo.core.data.repository

import com.jian.nemo.core.data.local.dao.AIExerciseDao
import com.jian.nemo.core.data.local.dao.AIReadingHistoryDao
import com.jian.nemo.core.data.local.entity.AIExerciseEntity
import com.jian.nemo.core.data.local.entity.AIReadingHistoryEntity
import com.jian.nemo.core.domain.model.AIExerciseHistory
import com.jian.nemo.core.domain.model.AIReadingArticle
import com.jian.nemo.core.domain.model.AIReadingHistory
import com.jian.nemo.core.domain.model.ReadingVocabulary
import com.jian.nemo.core.domain.model.ReadingQuestion
import com.jian.nemo.core.domain.repository.AIWorkshopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIWorkshopRepositoryImpl @Inject constructor(
    private val aiExerciseDao: AIExerciseDao,
    private val aiReadingHistoryDao: AIReadingHistoryDao
) : AIWorkshopRepository {

    private val json = Json { ignoreUnknownKeys = true }

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
        feedback: String,
        grammarPoint: String?,
        usageId: Int?
    ) {
        val entity = AIExerciseEntity(
            question = question,
            type = type,
            difficulty = difficulty,
            standardAnswer = standardAnswer,
            userAnswer = userAnswer,
            score = score,
            feedback = feedback,
            grammarPoint = grammarPoint,
            usageId = usageId
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

    // ========== AI 智能阅读历史实现 ==========

    override fun getReadingHistory(): Flow<List<AIReadingHistory>> {
        return aiReadingHistoryDao.getAllHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveReadingHistory(
        article: AIReadingArticle,
        selectedAnswers: List<Int?>,
        isSubmitted: Boolean
    ): Long {
        val entity = AIReadingHistoryEntity(
            title = article.title,
            level = article.level,
            contentRaw = article.contentRaw,
            translation = article.translation,
            vocabularyJson = json.encodeToString(article.vocabulary),
            questionsJson = json.encodeToString(article.questions),
            selectedAnswersJson = json.encodeToString(selectedAnswers),
            isSubmitted = isSubmitted
        )
        return aiReadingHistoryDao.insert(entity)
    }

    override suspend fun updateReadingAnswers(
        id: Int,
        selectedAnswers: List<Int?>,
        isSubmitted: Boolean
    ) {
        val answersJson = json.encodeToString(selectedAnswers)
        aiReadingHistoryDao.updateAnswers(id, answersJson, isSubmitted)
    }

    override suspend fun deleteReadingHistoryById(id: Int) {
        aiReadingHistoryDao.deleteHistoryById(id)
    }

    override suspend fun clearReadingHistory() {
        aiReadingHistoryDao.clearAll()
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
        createdAt = createdAt,
        grammarPoint = grammarPoint
    )

    private fun AIReadingHistoryEntity.toDomain() = AIReadingHistory(
        id = id,
        title = title,
        level = level,
        contentRaw = contentRaw,
        translation = translation,
        vocabulary = try { json.decodeFromString<List<ReadingVocabulary>>(vocabularyJson) } catch (e: Exception) { emptyList() },
        questions = try { json.decodeFromString<List<ReadingQuestion>>(questionsJson) } catch (e: Exception) { emptyList() },
        selectedAnswers = try { json.decodeFromString<List<Int?>>(selectedAnswersJson) } catch (e: Exception) { listOf(null, null, null) },
        isSubmitted = isSubmitted,
        createdAt = createdAt
    )
}
