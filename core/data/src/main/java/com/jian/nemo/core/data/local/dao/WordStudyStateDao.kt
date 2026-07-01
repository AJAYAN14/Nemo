package com.jian.nemo.core.data.local.dao

import androidx.room.*
import com.jian.nemo.core.data.local.entity.WordStudyStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordStudyStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: WordStudyStateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(states: List<WordStudyStateEntity>)

    @Update
    suspend fun update(state: WordStudyStateEntity)

    @Query("SELECT * FROM word_study_states WHERE word_id = :wordId")
    suspend fun getByWordId(wordId: Int): WordStudyStateEntity?

    @Query("SELECT * FROM word_study_states WHERE word_id = :wordId")
    fun getByWordIdFlow(wordId: Int): Flow<WordStudyStateEntity?>

    @Query("SELECT * FROM word_study_states")
    suspend fun getAllSync(): List<WordStudyStateEntity>

    /**
     * 获取指定时间戳之后修改过的状态（用于同步）
     */
    @Query("SELECT * FROM word_study_states WHERE last_modified_time > :timestamp")
    suspend fun getModifiedSince(timestamp: Long): List<WordStudyStateEntity>

    /**
     * 获取收藏的单词状态
     */
    @Query("SELECT * FROM word_study_states WHERE is_favorite = 1 AND is_deleted = 0")
    fun getFavorites(): Flow<List<WordStudyStateEntity>>

    @Query("SELECT * FROM word_study_states WHERE word_id IN (:wordIds)")
    suspend fun getStatesByIds(wordIds: List<Int>): List<WordStudyStateEntity>

    @Query("UPDATE word_study_states SET is_favorite = :isFavorite, last_modified_time = :time WHERE word_id = :wordId")
    suspend fun updateFavoriteStatus(wordId: Int, isFavorite: Boolean, time: Long): Int

    @Query("UPDATE word_study_states SET is_skipped = :isSkipped, last_modified_time = :time WHERE word_id = :wordId")
    suspend fun updateSkipStatus(wordId: Int, isSkipped: Boolean, time: Long)

    @Query("UPDATE word_study_states SET repetition_count = 0, stability = 0, difficulty = 0, interval = 0, next_review_date = 0, last_reviewed_date = NULL, first_learned_date = NULL, last_modified_time = :time")
    suspend fun resetAllProgress(time: Long)

    @Query("UPDATE word_study_states SET is_favorite = 0, last_modified_time = :time WHERE is_favorite = 1")
    suspend fun clearAllFavorites(time: Long)

    @Query("UPDATE word_study_states SET is_deleted = 1, deleted_time = :time, last_modified_time = :time WHERE word_id IN (:wordIds)")
    suspend fun markDeletedByIds(wordIds: List<Int>, time: Long)

    @Query("UPDATE word_study_states SET is_deleted = 1, deleted_time = :time, last_modified_time = :time WHERE word_id = :wordId")
    suspend fun markDeletedByWordId(wordId: Int, time: Long)

    @Query("DELETE FROM word_study_states")
    suspend fun deleteAll()

}
