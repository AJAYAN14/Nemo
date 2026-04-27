package com.jian.nemo.core.data.local.dao

import androidx.room.*
import com.jian.nemo.core.data.local.entity.GrammarStudyStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GrammarStudyStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: GrammarStudyStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(states: List<GrammarStudyStateEntity>)

    @Update
    suspend fun update(state: GrammarStudyStateEntity)

    @Query("SELECT * FROM grammar_study_states WHERE grammar_id = :grammarId")
    suspend fun getByGrammarId(grammarId: Int): GrammarStudyStateEntity?

    @Query("SELECT * FROM grammar_study_states WHERE grammar_id = :grammarId")
    fun getByGrammarIdFlow(grammarId: Int): Flow<GrammarStudyStateEntity?>

    @Query("SELECT * FROM grammar_study_states")
    suspend fun getAllSync(): List<GrammarStudyStateEntity>

    @Query("SELECT * FROM grammar_study_states WHERE last_modified_time > :timestamp")
    suspend fun getModifiedSince(timestamp: Long): List<GrammarStudyStateEntity>

    @Query("SELECT * FROM grammar_study_states WHERE is_favorite = 1 AND is_deleted = 0")
    fun getFavorites(): Flow<List<GrammarStudyStateEntity>>

    @Query("SELECT * FROM grammar_study_states WHERE grammar_id IN (:grammarIds)")
    suspend fun getStatesByIds(grammarIds: List<Int>): List<GrammarStudyStateEntity>

    @Query("UPDATE grammar_study_states SET is_favorite = :isFavorite, last_modified_time = :time WHERE grammar_id = :grammarId")
    suspend fun updateFavoriteStatus(grammarId: Int, isFavorite: Boolean, time: Long)

    @Query("UPDATE grammar_study_states SET is_skipped = :isSkipped, last_modified_time = :time WHERE grammar_id = :grammarId")
    suspend fun updateSkipStatus(grammarId: Int, isSkipped: Boolean, time: Long)

    @Query("UPDATE grammar_study_states SET repetition_count = 0, stability = 0, difficulty = 0, interval = 0, next_review_date = 0, last_reviewed_date = NULL, first_learned_date = NULL, last_modified_time = :time")
    suspend fun resetAllProgress(time: Long)

    @Query("UPDATE grammar_study_states SET is_favorite = 0, last_modified_time = :time WHERE is_favorite = 1")
    suspend fun clearAllFavorites(time: Long)

    @Query("UPDATE grammar_study_states SET is_deleted = 1, deleted_time = :time, last_modified_time = :time WHERE grammar_id IN (:grammarIds)")
    suspend fun markDeletedByIds(grammarIds: List<Int>, time: Long)

    @Query("UPDATE grammar_study_states SET is_deleted = 1, deleted_time = :time, last_modified_time = :time WHERE grammar_id = :grammarId")
    suspend fun markDeletedByGrammarId(grammarId: Int, time: Long)

    @Query("DELETE FROM grammar_study_states WHERE grammar_id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    @Query("SELECT * FROM grammar_study_states WHERE grammar_id = :id")
    suspend fun getByIdSync(id: Int): GrammarStudyStateEntity?

    @Query("DELETE FROM grammar_study_states")
    suspend fun deleteAll()
}
