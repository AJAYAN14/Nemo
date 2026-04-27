package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jian.nemo.core.data.local.entity.FavoriteQuestionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 收藏题目 DAO
 */
@Dao
interface FavoriteQuestionDao {

    @Query("SELECT * FROM favorite_questions ORDER BY timestamp DESC")
    fun getAllFavoriteQuestions(): Flow<List<FavoriteQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteQuestionEntity)

    @Query("DELETE FROM favorite_questions WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_questions WHERE grammar_id = :grammarId LIMIT 1)")
    suspend fun isFavoriteByGrammarId(grammarId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_questions WHERE json_id = :jsonId LIMIT 1)")
    suspend fun isFavoriteByJsonId(jsonId: String): Boolean

    @Query("DELETE FROM favorite_questions WHERE grammar_id = :grammarId")
    suspend fun deleteByGrammarId(grammarId: Int)

    @Query("DELETE FROM favorite_questions WHERE json_id = :jsonId")
    suspend fun deleteByJsonId(jsonId: String)

    @Query("DELETE FROM favorite_questions")
    suspend fun deleteAll()

    // Sync Support
    @Query("SELECT COUNT(*) FROM favorite_questions WHERE timestamp > :sinceTime")
    suspend fun countModifiedSince(sinceTime: Long): Int

    @Query("SELECT * FROM favorite_questions WHERE timestamp > :sinceTime")
    suspend fun getModifiedSince(sinceTime: Long): List<FavoriteQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<FavoriteQuestionEntity>)

    @Query("DELETE FROM favorite_questions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("SELECT * FROM favorite_questions")
    suspend fun getAllFavoriteQuestionsSync(): List<FavoriteQuestionEntity>

    @Query("SELECT id FROM favorite_questions WHERE id IN (:ids)")
    suspend fun getIdsIn(ids: List<Int>): List<Int>

    @Query("SELECT * FROM favorite_questions WHERE json_id = :jsonId LIMIT 1")
    suspend fun getByJsonId(jsonId: String): FavoriteQuestionEntity?

    @Query("SELECT * FROM favorite_questions WHERE question_text = :text LIMIT 1")
    suspend fun getByQuestionText(text: String): FavoriteQuestionEntity?

    /**
     * 获取所有收藏题目 (Cursor) - 用于流式导出
     */
    @Query("SELECT * FROM favorite_questions ORDER BY timestamp DESC")
    fun getExportFavoritesCursor(): android.database.Cursor

    @Query("UPDATE favorite_questions SET grammar_id = :newId WHERE grammar_id = :oldId")
    suspend fun migrateGrammarId(oldId: Int, newId: Int)
}
