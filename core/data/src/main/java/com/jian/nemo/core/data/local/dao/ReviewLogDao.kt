package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jian.nemo.core.data.local.entity.ReviewLogEntity

@Dao
interface ReviewLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ReviewLogEntity): Long

    @Query("SELECT * FROM review_logs ORDER BY review_date DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<ReviewLogEntity>

    @Query("""
        SELECT CAST(SUM(CASE WHEN rating >= 2 THEN 1 ELSE 0 END) AS REAL) / COUNT(*)
        FROM (SELECT rating FROM review_logs ORDER BY review_date DESC LIMIT :limit)
    """)
    suspend fun getRecentSuccessRate(limit: Int = 200): Float?
}
