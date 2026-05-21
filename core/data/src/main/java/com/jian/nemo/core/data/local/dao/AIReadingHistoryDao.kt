package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jian.nemo.core.data.local.entity.AIReadingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIReadingHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: AIReadingHistoryEntity): Long

    @Query("SELECT * FROM ai_reading_history ORDER BY created_at DESC")
    fun getAllHistory(): Flow<List<AIReadingHistoryEntity>>

    @Query("UPDATE ai_reading_history SET selected_answers_json = :selectedAnswersJson, is_submitted = :isSubmitted WHERE id = :id")
    suspend fun updateAnswers(id: Int, selectedAnswersJson: String, isSubmitted: Boolean)

    @Query("DELETE FROM ai_reading_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)

    @Query("DELETE FROM ai_reading_history")
    suspend fun clearAll()
}
