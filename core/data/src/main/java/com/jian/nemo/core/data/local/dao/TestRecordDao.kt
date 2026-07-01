package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jian.nemo.core.data.local.entity.TestRecordEntity
import kotlinx.coroutines.flow.Flow

/**
 * 测试记录数据访问对象
 */
@Dao
interface TestRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TestRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<TestRecordEntity>)

    /**
     * 获取所有测试记录（按时间降序）
     */
    @Query("SELECT * FROM test_records WHERE is_deleted = 0 ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<TestRecordEntity>>

    /**
     * 获取所有测试记录 (同步) - 用于导出
     */
    @Query("SELECT * FROM test_records WHERE is_deleted = 0 ORDER BY timestamp DESC")
    suspend fun getAllTestRecordsSync(): List<TestRecordEntity>

    /**
     * 获取所有测试记录 (Cursor) - 用于流式导出
     */
    @Query("SELECT * FROM test_records ORDER BY timestamp DESC")
    fun getExportTestRecordsCursor(): android.database.Cursor

    /**
     * 获取指定日期的测试记录
     */
    @Query("SELECT * FROM test_records WHERE date = :date AND is_deleted = 0 ORDER BY timestamp DESC")
    fun getRecordsByDate(date: Long): Flow<List<TestRecordEntity>>

    /**
     * 获取指定测试模式的记录
     */
    @Query("SELECT * FROM test_records WHERE test_mode = :testMode AND is_deleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecordsByMode(testMode: String, limit: Int): Flow<List<TestRecordEntity>>

    @Query("UPDATE test_records SET is_deleted = 1, deleted_time = :time, timestamp = :time WHERE uuid = :uuid")
    suspend fun markDeletedByUuid(uuid: String, time: Long)

    /**
     * 逻辑删除所有测试记录
     */
    @Query("UPDATE test_records SET is_deleted = 1, deleted_time = :time, timestamp = :time")
    suspend fun deleteAll(time: Long = System.currentTimeMillis())

    /**
     * 获取测试总数
     */
    @Query("SELECT COUNT(*) FROM test_records WHERE is_deleted = 0")
    fun getTotalTestCount(): Flow<Int>

    /**
     * 获取总正确题数
     */
    @Query("SELECT SUM(correct_answers) FROM test_records WHERE is_deleted = 0")
    fun getTotalCorrectAnswers(): Flow<Int?>

    /**
     * 获取总题目数
     */
    @Query("SELECT SUM(total_questions) FROM test_records WHERE is_deleted = 0")
    fun getTotalQuestions(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM test_records WHERE timestamp > :timestamp")
    suspend fun countModifiedSince(timestamp: Long): Int

    /**
     * 获取自指定时间以来修改过的所有记录 (包含已删除)
     */
    @Query("SELECT * FROM test_records WHERE timestamp > :sinceTime")
    suspend fun getModifiedSince(sinceTime: Long): List<TestRecordEntity>

    /**
     * 根据 UUID 列表批量获取记录
     */
    @Query("SELECT * FROM test_records WHERE uuid IN (:uuids)")
    suspend fun getByUuids(uuids: List<String>): List<TestRecordEntity>

}
