package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jian.nemo.core.data.local.entity.GrammarWrongAnswerEntity
import kotlinx.coroutines.flow.Flow

/**
 * 语法错题数据访问对象
 */
@Dao
interface GrammarWrongAnswerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(wrongAnswer: GrammarWrongAnswerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wrongAnswers: List<GrammarWrongAnswerEntity>)

    @Query("SELECT * FROM grammar_wrong_answers WHERE is_deleted = 0 ORDER BY timestamp DESC")
    fun getAllWrongAnswers(): Flow<List<GrammarWrongAnswerEntity>>

    /**
     * 获取所有语法错题记录 (同步) - 用于导出
     */
    @Query("SELECT * FROM grammar_wrong_answers WHERE is_deleted = 0 ORDER BY timestamp DESC")
    suspend fun getAllWrongAnswersSync(): List<GrammarWrongAnswerEntity>

    /**
     * 获取所有语法错题记录 (Cursor) - 用于流式导出
     */
    @Query("SELECT * FROM grammar_wrong_answers ORDER BY timestamp DESC")
    fun getExportWrongAnswersCursor(): android.database.Cursor

    @Query("SELECT * FROM grammar_wrong_answers WHERE grammar_id = :grammarId AND is_deleted = 0 ORDER BY timestamp DESC")
    fun getWrongAnswersByGrammarId(grammarId: Int): Flow<List<GrammarWrongAnswerEntity>>

    /**
     * 获取指定语法的错题记录 (同步) - 用于做题时判断
     */
    @Query("SELECT * FROM grammar_wrong_answers WHERE grammar_id = :grammarId AND is_deleted = 0 LIMIT 1")
    suspend fun getWrongAnswerByGrammarIdSync(grammarId: Int): GrammarWrongAnswerEntity?

    @Query("SELECT DISTINCT T2.grammar_level FROM grammar_wrong_answers T1 JOIN grammars T2 ON T1.grammar_id = T2.id WHERE T1.is_deleted = 0")
    fun getWrongAnswerGrammarLevels(): Flow<List<String>>

    @Query("SELECT DISTINCT grammar_id FROM grammar_wrong_answers WHERE is_deleted = 0")
    suspend fun getAllWrongGrammarIds(): List<Int>

    @Query("UPDATE grammar_wrong_answers SET is_deleted = 1, deleted_time = :time, timestamp = :time WHERE grammar_id = :grammarId")
    suspend fun markDeletedByGrammarId(grammarId: Int, time: Long)

    /**
     * 逻辑删除所有语法错题记录
     */
    @Query("UPDATE grammar_wrong_answers SET is_deleted = 1, deleted_time = :time, timestamp = :time")
    suspend fun deleteAll(time: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM grammar_wrong_answers WHERE timestamp > :timestamp")
    suspend fun countModifiedSince(timestamp: Long): Int

    /**
     * 获取自指定时间以来修改过的所有记录 (包含已删除)
     */
    @Query("SELECT * FROM grammar_wrong_answers WHERE timestamp > :sinceTime")
    suspend fun getModifiedSince(sinceTime: Long): List<GrammarWrongAnswerEntity>

    /**
     * 根据 UUID 列表批量获取记录
     */
    @Query("SELECT * FROM grammar_wrong_answers WHERE uuid IN (:uuids)")
    suspend fun getByUuids(uuids: List<String>): List<GrammarWrongAnswerEntity>

    @Query("UPDATE grammar_wrong_answers SET grammar_id = :newId WHERE grammar_id = :oldId")
    suspend fun migrateGrammarId(oldId: Int, newId: Int)
}
