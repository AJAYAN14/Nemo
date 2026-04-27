package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.FavoriteQuestion
import kotlinx.coroutines.flow.Flow

/**
 * 收藏题目 Repository 接口
 */
interface FavoriteQuestionRepository {
    fun getAllFavoriteQuestions(): Flow<List<FavoriteQuestion>>

    suspend fun insertFavoriteQuestion(question: FavoriteQuestion)

    suspend fun deleteFavoriteQuestion(id: Int)

    suspend fun isFavorite(grammarId: Int?, jsonId: String?): Boolean

    suspend fun removeFavorite(grammarId: Int?, jsonId: String?)

    suspend fun clearAll()
}
