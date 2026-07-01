package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jian.nemo.core.data.local.entity.StudyRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 学习记录数据访问对象
 */
@Dao
interface StudyRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: StudyRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<StudyRecordEntity>)

    @Update
    suspend fun update(record: StudyRecordEntity)

    /**
     * 获取指定日期的学习记录
     */
    @Query("SELECT COUNT(*) FROM study_records WHERE timestamp > :timestamp")
    suspend fun countModifiedSince(timestamp: Long): Int

    @Query("SELECT * FROM study_records WHERE date = :date AND is_deleted = 0")
    fun getByDate(date: Long): Flow<StudyRecordEntity?>

    /**
     * 获取所有学习记录（按日期降序）
     */
    @Query("SELECT * FROM study_records WHERE is_deleted = 0 ORDER BY date DESC")
    fun getAllRecords(): Flow<List<StudyRecordEntity>>

    /**
     * 获取所有学习记录 (同步) - 用于导出
     */
    @Query("SELECT * FROM study_records WHERE is_deleted = 0 ORDER BY date DESC")
    suspend fun getAllStudyRecordsSync(): List<StudyRecordEntity>

    /**
     * 获取所有学习记录 (Cursor) - 用于流式导出
     */
    @Query("SELECT * FROM study_records ORDER BY date DESC")
    fun getExportStudyRecordsCursor(): android.database.Cursor

    /**
     * 获取日期范围内的学习记录
     */
    @Query("SELECT * FROM study_records WHERE date BETWEEN :startDate AND :endDate AND is_deleted = 0 ORDER BY date DESC")
    fun getRecordsBetween(startDate: Long, endDate: Long): Flow<List<StudyRecordEntity>>

    /**
     * 获取总学习天数
     */
    @Query("SELECT COUNT(*) FROM study_records WHERE is_deleted = 0")
    fun getTotalStudyDays(): Flow<Int>

    /**
     * 获取每日活动计数 (learned + reviewed)
     */
    @Query("""
        SELECT date,
        (learned_words + learned_grammars + reviewed_words + reviewed_grammars) as totalCount
        FROM study_records
        WHERE date BETWEEN :startDate AND :endDate AND is_deleted = 0
    """)
    fun getDailyActivityCounts(startDate: Long, endDate: Long): Flow<List<DailyActivityCount>>

    /**
     * 删除指定日期的记录
     */
    @Query("UPDATE study_records SET is_deleted = 1, deleted_time = :time, timestamp = :time WHERE date = :date")
    suspend fun markDeletedByDate(date: Long, time: Long)

    /**
     * 逻辑删除所有学习记录
     */
    @Query("UPDATE study_records SET is_deleted = 1, deleted_time = :time, timestamp = :time")
    suspend fun deleteAll(time: Long = System.currentTimeMillis())

    /**
     * 获取自指定时间以来修改过的所有记录 (包含已删除)
     */
    @Query("SELECT * FROM study_records WHERE timestamp > :sinceTime")
    suspend fun getModifiedSince(sinceTime: Long): List<StudyRecordEntity>

}

data class DailyActivityCount(
    val date: Long,
    val totalCount: Int
)
