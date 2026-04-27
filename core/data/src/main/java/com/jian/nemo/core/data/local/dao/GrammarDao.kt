package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.jian.nemo.core.data.local.entity.GrammarEntity
import com.jian.nemo.core.data.local.entity.GrammarStudyStateUpdate
import com.jian.nemo.core.data.local.entity.GrammarStudyStateEntity
import com.jian.nemo.core.data.local.entity.relations.GrammarWithUsages
import kotlinx.coroutines.flow.Flow

/**
 * 语法数据访问对象
 */
@Dao
interface GrammarDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(grammars: List<GrammarEntity>)

    /**
     * 插入单条语法
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grammar: GrammarEntity)

    @Update
    suspend fun update(grammar: GrammarEntity)

    /**
     * 批量更新进度
     */
    @Update(entity = GrammarStudyStateEntity::class)
    suspend fun updateStudyState(updates: List<GrammarStudyStateUpdate>): Int

    @Query("SELECT id FROM grammars WHERE id IN (:ids)")
    suspend fun getIdsIn(ids: List<Int>): List<Int>

    /**
     * 批量插入或更新 (用于同步)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(grammars: List<GrammarEntity>)

    /**
     * 逻辑删除语法
     */
    @Query("""
        UPDATE grammar_study_states SET
        is_deleted = 1,
        deleted_time = :deletedTime,
        last_modified_time = :deletedTime
        WHERE grammar_id IN (:ids)
    """)
    suspend fun softDeleteByIds(ids: List<Int>, deletedTime: Long)

    @Query("DELETE FROM grammars WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    /**
     * 获取所有语法 (包含已逻辑删除的)
     */
    /**
     * 获取所有语法 (包含已逻辑删除的)
     */
    @Query("SELECT * FROM grammars")
    suspend fun getAllGrammarsWithDeletedSync(): List<GrammarEntity>

    /**
     * 获取自指定时间以来修改过的语法 (用于增量同步)
     */
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.last_modified_time > :sinceTime
    """)
    suspend fun getModifiedSince(sinceTime: Long): List<GrammarEntity>

    @Query("""
        SELECT COUNT(*) FROM grammar_study_states
        WHERE last_modified_time > :timestamp
    """)
    suspend fun countModifiedSince(timestamp: Long): Int

    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE g.id = :id
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
    """)
    fun getById(id: Int): Flow<GrammarEntity?>

    /**
     * 获取语法（含用法和例句）
     */
    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE g.id = :id
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
    """)
    fun getGrammarWithUsages(id: Int): Flow<GrammarWithUsages?>

    /**
     * 获取所有语法（含用法和例句）
     */
    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
    """)
    fun getAllGrammarsWithUsages(): Flow<List<GrammarWithUsages>>

    /**
     * 根据等级获取语法（含用法和例句）
     * 忽略大小写匹配
     */
    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE UPPER(g.grammar_level) IN (:levels)
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
    """)
    fun getGrammarsByLevelsWithUsages(levels: List<String>): Flow<List<GrammarWithUsages>>

    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE UPPER(g.grammar_level) = UPPER(:level)
        AND (s.repetition_count IS NULL OR s.repetition_count = 0)
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
        ORDER BY g.id ASC
    """)
    fun getNewGrammarsByLevelWithUsages(level: String): Flow<List<GrammarWithUsages>>

    /**
     * 获取新语法（随机排序，含用法）
     */
    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE UPPER(g.grammar_level) = UPPER(:level)
        AND (s.repetition_count IS NULL OR s.repetition_count = 0)
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
        ORDER BY RANDOM()
    """)
    fun getNewGrammarsByLevelWithUsagesRandom(level: String): Flow<List<GrammarWithUsages>>

    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.next_review_date <= :currentDate
        AND s.repetition_count > 0
        AND s.is_skipped = 0
        AND s.is_deleted = 0
        AND g.is_delisted = 0
        ORDER BY s.next_review_date ASC
    """)
    fun getDueGrammarsWithUsages(currentDate: Long): Flow<List<GrammarWithUsages>>

    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.first_learned_date = :todayEpochDay
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND g.is_delisted = 0
        ORDER BY g.id DESC
    """)
    fun getTodayLearnedGrammarsWithUsages(todayEpochDay: Long): Flow<List<GrammarWithUsages>>

    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.last_reviewed_date = :todayEpochDay
        AND s.repetition_count > 0
        AND s.first_learned_date < :todayEpochDay
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND g.is_delisted = 0
        ORDER BY g.id DESC
    """)
    fun getTodayReviewedGrammarsWithUsages(todayEpochDay: Long): Flow<List<GrammarWithUsages>>

    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.is_favorite = 1 AND s.is_deleted = 0 AND g.is_delisted = 0 ORDER BY g.id DESC
    """)
    fun getFavoriteGrammarsWithUsages(): Flow<List<GrammarWithUsages>>

    /**
     * 获取跳过的语法（含用法和例句）
     */
    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.is_skipped = 1 AND s.is_deleted = 0 AND g.is_delisted = 0
        ORDER BY g.id DESC LIMIT :limit
    """)
    fun getSkippedGrammarsWithUsages(limit: Int): Flow<List<GrammarWithUsages>>

    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.repetition_count > 0
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND g.is_delisted = 0
        ORDER BY g.id DESC
    """)
    fun getAllLearnedGrammarsWithUsages(): Flow<List<GrammarWithUsages>>

    @Query("""
        SELECT COUNT(*) FROM grammar_study_states s
        JOIN grammars g ON s.grammar_id = g.id
        WHERE s.repetition_count > 0
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND g.is_delisted = 0
    """)
    fun getLearnedGrammarCount(): Flow<Int>

    /**
     * 获取指定等级的已学习语法（含用法和例句）
     */
    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.repetition_count > 0
        AND UPPER(g.grammar_level) = UPPER(:level)
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND g.is_delisted = 0
        ORDER BY g.id DESC
    """)
    fun getLearnedGrammarsByLevelWithUsages(level: String): Flow<List<GrammarWithUsages>>

    /**
     * 根据ID批量获取语法（含用法和例句）
     */
    @Transaction
    @Query("SELECT * FROM grammars WHERE id IN (:ids)")
    suspend fun getGrammarsByIdsWithUsages(ids: List<Int>): List<GrammarWithUsages>

    /**
     * 搜索语法（含用法和例句）
     */
    @Transaction
    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE g.grammar LIKE '%' || :query || '%'
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
        ORDER BY g.id ASC
    """)
    fun searchGrammarsWithUsages(query: String): Flow<List<GrammarWithUsages>>

    /**
     * 获取所有语法 (同步) - 用于导出
     */


    /**
     * 获取所有语法 (Cursor) - 用于流式导出
     * 优化：仅导出有进度或状态的语法
     */
    @Query("""
        SELECT
            g.id, g.grammar, g.grammar_level,
            s.repetition_count AS repetitionCount,
            s.stability AS stability,
            s.difficulty AS difficulty,
            s.interval AS interval,
            s.next_review_date AS nextReviewDate,
            s.is_favorite AS isFavorite,
            s.is_skipped AS isSkipped,
            s.is_deleted AS isDeleted,
            s.deleted_time AS deletedTime,
            s.last_modified_time AS lastModifiedTime,
            s.last_reviewed_date AS lastReviewedDate,
            s.first_learned_date AS firstLearnedDate
        FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.repetition_count > 0
        OR s.is_favorite = 1
        OR s.is_skipped = 1
        OR s.is_deleted = 1
        OR s.first_learned_date IS NOT NULL
        OR s.last_modified_time > 0
    """)
    fun getExportGrammarsCursor(): android.database.Cursor

    @Query("SELECT * FROM grammars")
    suspend fun getAllGrammarsSync(): List<GrammarEntity>

    /**
     * 获取所有语法
     */
    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
    """)
    fun getAllGrammars(): Flow<List<GrammarEntity>>

    /**
     * 获取所有已学习的语法 (不包含跳过的)
     */
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.repetition_count > 0
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND g.is_delisted = 0
        ORDER BY g.id DESC
    """)
    fun getAllLearnedGrammars(): Flow<List<GrammarEntity>>

    /**
     * 根据等级列表获取语法
     * 数据库中grammar_level存储为大写（N1, N2, N3, N4, N5）
     */
    /**
     * 根据等级列表获取语法
     * 数据库中grammar_level存储为大写（N1, N2, N3, N4, N5）
     * 忽略大小写
     */
    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE UPPER(g.grammar_level) IN (:levels)
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
    """)
    fun getAllGrammarsByLevels(levels: List<String>): Flow<List<GrammarEntity>>

    /**
     * 获取新语法（未学习且未跳过）
     */
    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE UPPER(g.grammar_level) = UPPER(:level)
        AND (s.repetition_count IS NULL OR s.repetition_count = 0)
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
        ORDER BY g.id ASC
    """)
    fun getNewGrammarsByLevel(level: String): Flow<List<GrammarEntity>>

    /**
     * 获取到期复习语法
     */
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.next_review_date <= :currentDate
        AND s.repetition_count > 0
        AND s.is_skipped = 0
        AND s.is_deleted = 0
        AND g.is_delisted = 0
        ORDER BY s.next_review_date ASC
    """)
    fun getDueGrammars(currentDate: Long): Flow<List<GrammarEntity>>

    @Query("""
        SELECT COUNT(*) FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.next_review_date <= :currentDate
        AND s.repetition_count > 0
        AND s.is_skipped = 0
        AND s.is_deleted = 0
        AND g.is_delisted = 0
    """)
    fun getDueGrammarsCount(currentDate: Long): Flow<Int>

    /**
     * 获取今日学习的语法
     */
    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.first_learned_date = :todayEpochDay
        AND g.is_delisted = 0
        ORDER BY g.id DESC
    """)
    fun getTodayLearnedGrammars(todayEpochDay: Long): Flow<List<GrammarEntity>>


    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.is_favorite = 1 AND s.is_deleted = 0 AND g.is_delisted = 0 ORDER BY g.id DESC
    """)
    fun getFavoriteGrammars(): Flow<List<GrammarEntity>>

    @Query("UPDATE grammar_study_states SET is_favorite = :isFavorite, last_modified_time = :lastModifiedTime WHERE grammar_id = :grammarId")
    suspend fun updateFavoriteStatus(grammarId: Int, isFavorite: Boolean, lastModifiedTime: Long)

    @Query("""
        SELECT g.* FROM grammars g
        JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE s.is_skipped = 1 AND s.is_deleted = 0 AND g.is_delisted = 0 ORDER BY g.id DESC LIMIT :limit
    """)
    fun getSkippedGrammars(limit: Int): Flow<List<GrammarEntity>>

    @Query("""
        SELECT COUNT(*) FROM grammar_study_states s
        JOIN grammars g ON s.grammar_id = g.id
        WHERE s.is_skipped = 1 AND s.is_deleted = 0 AND g.is_delisted = 0
    """)
    fun getSkippedGrammarsCount(): Flow<Int>

    @Query("SELECT * FROM grammars WHERE id IN (:ids)")
    suspend fun getGrammarsByIds(ids: List<Int>): List<GrammarEntity>

    /**
     * 搜索语法 (匹配标题)
     * 注意：explanation 字段已迁移到 grammar_usages 表
     */
    @Query("""
        SELECT g.* FROM grammars g
        LEFT JOIN grammar_study_states s ON g.id = s.grammar_id
        WHERE g.grammar LIKE '%' || :query || '%'
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND g.is_delisted = 0
        ORDER BY g.id ASC
    """)
    fun searchGrammars(query: String): Flow<List<GrammarEntity>>

    @Query("DELETE FROM grammar_study_states")
    suspend fun resetAllProgress()

    /**
     * 清空所有收藏
     */
    @Query("UPDATE grammar_study_states SET is_favorite = 0 WHERE is_favorite = 1")
    suspend fun clearAllFavorites()

    /**
     * 获取复习预测
     */
    @Query("""
        SELECT s.next_review_date AS date, COUNT(*) AS count
        FROM grammar_study_states s
        WHERE s.next_review_date BETWEEN :startDateEpochDay AND :endDateEpochDay
        AND s.is_skipped = 0
        GROUP BY s.next_review_date
    """)
    fun getReviewForecast(startDateEpochDay: Long, endDateEpochDay: Long): Flow<List<GrammarReviewForecastTuple>>

    // ========== 等级查询 ==========

    @Query("""
        SELECT DISTINCT w.grammar_level
        FROM grammar_study_states s
        JOIN grammars w ON s.grammar_id = w.id
        WHERE s.last_reviewed_date = :todayEpochDay
    """)
    fun getTodayReviewedGrammarLevels(todayEpochDay: Long): Flow<List<String>>

    @Query("""
        SELECT DISTINCT w.grammar_level
        FROM grammar_study_states s
        JOIN grammars w ON s.grammar_id = w.id
        WHERE s.first_learned_date = :todayEpochDay
    """)
    fun getTodayLearnedGrammarLevels(todayEpochDay: Long): Flow<List<String>>

    @Query("""
        SELECT DISTINCT w.grammar_level
        FROM grammar_study_states s
        JOIN grammars w ON s.grammar_id = w.id
        WHERE s.is_favorite = 1
    """)
    fun getFavoriteGrammarLevels(): Flow<List<String>>

    @Query("""
        SELECT DISTINCT w.grammar_level
        FROM grammar_study_states s
        JOIN grammars w ON s.grammar_id = w.id
        WHERE s.repetition_count > 0
    """)
    fun getLearnedGrammarLevels(): Flow<List<String>>

    @Query("SELECT DISTINCT T2.grammar_level FROM grammar_wrong_answers T1 JOIN grammars T2 ON T1.grammar_id = T2.id")
    fun getWrongAnswerGrammarLevels(): Flow<List<String>>

    /**
     * 获取语法总数
     */
    @Query("SELECT COUNT(*) FROM grammars WHERE is_delisted = 0")
    suspend fun getGrammarCount(): Int

    /**
     * 获取语法等级数（用于判断数据是否完整）
     */
    @Query("SELECT COUNT(DISTINCT grammar_level) FROM grammars WHERE is_delisted = 0")
    suspend fun getGrammarLevelCount(): Int

    // ========== 数据修复 ==========

    /**
     * 获取去重后的保留ID列表 (每个语法只保留ID最小的一个)
     */
    @Query("SELECT MIN(id) FROM grammars GROUP BY grammar, grammar_level")
    suspend fun getDuplicateKeepIds(): List<Int>

    /**
     * 将指定等级下，不在给定 ID 列表中的语法标记为已下架
     */
    @Query("UPDATE grammars SET is_delisted = 1 WHERE UPPER(grammar_level) = UPPER(:level) AND id NOT IN (:jsonIds)")
    suspend fun markMissingAsDelisted(level: String, jsonIds: List<Int>): Int
}

data class GrammarReviewForecastTuple(
    val date: Long,
    val count: Int
)
