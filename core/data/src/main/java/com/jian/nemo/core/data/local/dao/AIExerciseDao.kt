package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jian.nemo.core.data.local.entity.AIExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIExerciseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: AIExerciseEntity)

    @Query("SELECT * FROM ai_exercises ORDER BY createdAt DESC")
    fun getAllHistory(): Flow<List<AIExerciseEntity>>

    @Query("DELETE FROM ai_exercises WHERE createdAt < :timestamp")
    suspend fun deleteOldRecords(timestamp: Long)

    @Query("DELETE FROM ai_exercises")
    suspend fun clearAll()
}
