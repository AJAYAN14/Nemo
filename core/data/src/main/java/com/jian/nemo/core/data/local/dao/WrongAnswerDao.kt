package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jian.nemo.core.data.local.entity.WrongAnswerEntity
import kotlinx.coroutines.flow.Flow

/**
 * 单词错题数据访问对象
 */
@Dao
interface WrongAnswerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wrongAnswer: WrongAnswerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wrongAnswers: List<WrongAnswerEntity>)

    /**
     * 获取所有错题记录
     */
    @Query("SELECT * FROM wrong_answers WHERE is_deleted = 0 ORDER BY timestamp DESC")
    fun getAllWrongAnswers(): Flow<List<WrongAnswerEntity>>

    /**
     * 获取所有错题记录 (同步) - 用于导出
     */
    @Query("SELECT * FROM wrong_answers WHERE is_deleted = 0 ORDER BY timestamp DESC")
    suspend fun getAllWrongAnswersSync(): List<WrongAnswerEntity>

    /**
     * 获取所有错题记录 (Cursor) - 用于流式导出
     */
    @Query("SELECT * FROM wrong_answers ORDER BY timestamp DESC")
    fun getExportWrongAnswersCursor(): android.database.Cursor

    /**
     * 获取指定单词的错题记录
     */
    @Query("SELECT * FROM wrong_answers WHERE word_id = :wordId AND is_deleted = 0 ORDER BY timestamp DESC")
    fun getWrongAnswersByWordId(wordId: Int): Flow<List<WrongAnswerEntity>>

    /**
     * 获取指定单词的错题记录 (同步) - 用于做题时判断
     */
    @Query("SELECT * FROM wrong_answers WHERE word_id = :wordId AND is_deleted = 0 LIMIT 1")
    suspend fun getWrongAnswerByWordIdSync(wordId: Int): WrongAnswerEntity?

    /**
     * 获取所有错题单词的ID列表
     */
    @Query("SELECT DISTINCT word_id FROM wrong_answers WHERE is_deleted = 0")
    suspend fun getAllWrongWordIds(): List<Int>

    /**
     * 删除指定单词的错题记录
     */
    @Query("UPDATE wrong_answers SET is_deleted = 1, deleted_time = :time, timestamp = :time WHERE word_id = :wordId")
    suspend fun markDeletedByWordId(wordId: Int, time: Long)

    /**
     * 逻辑删除所有错题记录
     */
    @Query("UPDATE wrong_answers SET is_deleted = 1, deleted_time = :time, timestamp = :time")
    suspend fun deleteAll(time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM wrong_answers WHERE timestamp > :timestamp")
    suspend fun countModifiedSince(timestamp: Long): Int

    /**
     * 获取自指定时间以来修改过的所有记录 (包含已删除)
     */
    @Query("SELECT * FROM wrong_answers WHERE timestamp > :sinceTime")
    suspend fun getModifiedSince(sinceTime: Long): List<WrongAnswerEntity>

    /**
     * 根据 UUID 列表批量获取记录
     */
    @Query("SELECT * FROM wrong_answers WHERE uuid IN (:uuids)")
    suspend fun getByUuids(uuids: List<String>): List<WrongAnswerEntity>

}
