package com.jian.nemo.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jian.nemo.core.data.local.entity.WordEntity
import com.jian.nemo.core.data.local.entity.WordStudyStateUpdate
import com.jian.nemo.core.data.local.entity.WordStudyStateEntity
import kotlinx.coroutines.flow.Flow

/**
 * 单词数据访问对象
 */
@Dao
interface WordDao {

    // ========== 基础CRUD ==========

    /**
     * 批量插入单词
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<WordEntity>)

    /**
     * 插入单条单词
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: WordEntity)

    /**
     * 更新单词
     */
    @Update
    suspend fun update(word: WordEntity)

    /**
     * 批量更新单词 (用于同步更新释义等)
     */
    @Update
    suspend fun updateAll(words: List<WordEntity>)

    /**
     * 批量更新进度 (增量同步专用)
     */
    @Update(entity = WordStudyStateEntity::class)
    suspend fun updateStudyState(updates: List<WordStudyStateUpdate>): Int

    /**
     * 批量获取存在的ID (用于区分 Update 和 Insert)
     */
    @Query("SELECT id FROM words WHERE id IN (:ids)")
    suspend fun getIdsIn(ids: List<Int>): List<Int>

    /**
     * 按等级+日语匹配单词（用于云更新合并）
     */
    @Query("SELECT * FROM words WHERE level = :level AND japanese = :japanese LIMIT 1")
    suspend fun getWordByLevelAndJapanese(level: String, japanese: String): WordEntity?

    /**
     * 批量插入或更新 (用于同步 - 全量覆盖场景慎用)
     * 注意：对于增量同步，建议使用 updateProgress + insert 组合
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(words: List<WordEntity>)

    /**
     * 逻辑删除单词 (User State)
     */
    @Query("""
        UPDATE word_study_states SET
        is_deleted = 1,
        deleted_time = :deletedTime,
        last_modified_time = :deletedTime
        WHERE word_id IN (:ids)
    """)
    suspend fun softDeleteByIds(ids: List<Int>, deletedTime: Long)

    /**
     * 标记单词下架 (Dictionary Sync)
     * 用于处理 JSON 源文件中已被删除的单词
     */
    @Query("UPDATE words SET is_delisted = 1 WHERE id IN (:ids)")
    suspend fun markAsDelisted(ids: List<Int>)

    /**
     * 物理删除单词 (仅用于同步或特殊清理)
     */
    @Query("DELETE FROM words WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)

    /**
     * 清空所有单词 (用于数据重置)
     */
    @Query("DELETE FROM words")
    suspend fun deleteAll()

    /**
     * 获取所有单词 (包含已逻辑删除的) - 用于同步合并
     */
    /**
     * 获取所有单词 (包含已逻辑删除的) - 用于同步合并
     */
    @Query("SELECT * FROM words")
    suspend fun getAllWordsWithDeletedSync(): List<WordEntity>

    /**
     * 获取自指定时间以来修改过的单词 (用于增量同步)
     */
    @Query("""
        SELECT w.* FROM words w
        JOIN word_study_states s ON w.id = s.word_id
        WHERE s.last_modified_time > :sinceTime
    """)
    suspend fun getModifiedSince(sinceTime: Long): List<WordEntity>

    /**
     * 根据ID获取单词
     */
    @Query("""
        SELECT w.* FROM words w
        LEFT JOIN word_study_states s ON w.id = s.word_id
        WHERE w.id = :id
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND w.is_delisted = 0
    """)
    fun getById(id: Int): Flow<WordEntity?>

    // ========== 学习相关查询 ==========

    /**
     * 获取新单词（未学习且未跳过）
     * @param level JLPT等级
     * @return 新单词列表Flow
     */
    @Query("""
        SELECT w.* FROM words w
        LEFT JOIN word_study_states s ON w.id = s.word_id
        WHERE w.level = :level
        AND (s.repetition_count IS NULL OR s.repetition_count = 0)
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND w.is_delisted = 0
        ORDER BY w.id ASC
    """)
    fun getNewWordsByLevel(level: String): Flow<List<WordEntity>>

    /**
     * 获取新单词（随机排序）
     */
    @Query("""
        SELECT w.* FROM words w
        LEFT JOIN word_study_states s ON w.id = s.word_id
        WHERE w.level = :level
        AND (s.repetition_count IS NULL OR s.repetition_count = 0)
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND w.is_delisted = 0
        ORDER BY RANDOM()
    """)
    fun getNewWordsByLevelRandom(level: String): Flow<List<WordEntity>>

    /**
     * 获取到期复习单词
     * @param currentDate 当前日期 (Epoch Day)
     * @return 到期复习单词列表Flow
     */
    @Query("""
        SELECT w.* FROM words w
        JOIN word_study_states s ON w.id = s.word_id
        WHERE s.next_review_date <= :currentDate
        AND s.repetition_count > 0
        AND s.is_skipped = 0
        AND s.is_deleted = 0
        AND w.is_delisted = 0
        ORDER BY s.next_review_date ASC
    """)
    fun getDueWords(currentDate: Long): Flow<List<WordEntity>>

    /**
     * 获取到期复习单词数量
     */
    @Query("""
        SELECT COUNT(*) FROM word_study_states s
        JOIN words w ON s.word_id = w.id
        WHERE s.next_review_date <= :currentDate
        AND s.repetition_count > 0
        AND s.is_skipped = 0
        AND s.is_deleted = 0
        AND w.is_delisted = 0
    """)
    fun getDueWordsCount(currentDate: Long): Flow<Int>

    /**
     * 获取今日首次学习的单词
     * @param todayEpochDay 今天的Epoch Day
     * @return 今日学习的单词列表Flow
     */
    @Query("""
        SELECT w.* FROM words w
        JOIN word_study_states s ON w.id = s.word_id
        WHERE s.first_learned_date = :todayEpochDay
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND w.is_delisted = 0
        ORDER BY w.id DESC
    """)
    fun getTodayLearnedWords(todayEpochDay: Long): Flow<List<WordEntity>>

    /**
     * 获取今日复习过的单词
     * @param todayEpochDay 今天的Epoch Day
     */
    @Query("""
        SELECT w.* FROM words w
        JOIN word_study_states s ON w.id = s.word_id
        WHERE s.last_reviewed_date = :todayEpochDay
        AND s.repetition_count > 0
        AND s.first_learned_date < :todayEpochDay
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND w.is_delisted = 0
        ORDER BY w.id DESC
    """)
    fun getTodayReviewedWords(todayEpochDay: Long): Flow<List<WordEntity>>

    /**
     * 获取所有已学习的单词 (不包含跳过的)
     */
    @Query("""
        SELECT w.* FROM words w
        JOIN word_study_states s ON w.id = s.word_id
        WHERE s.repetition_count > 0
        AND s.is_deleted = 0
        AND w.is_delisted = 0
        ORDER BY w.id DESC
    """)
    fun getAllLearnedWords(): Flow<List<WordEntity>>

    /**
     * 获取所有已学习的单词总数 (不包含跳过的)
     */
    @Query("""
        SELECT COUNT(*) FROM word_study_states s
        JOIN words w ON s.word_id = w.id
        WHERE s.repetition_count > 0
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND w.is_delisted = 0
    """)
    fun getLearnedWordCount(): Flow<Int>

    /**
     * 获取指定等级的已学习单词 (不包含跳过的)
     */
    @Query("""
        SELECT w.* FROM words w
        JOIN word_study_states s ON w.id = s.word_id
        WHERE s.repetition_count > 0
        AND w.level = :level
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND w.is_delisted = 0
        ORDER BY w.id DESC
    """)
    fun getLearnedWordsByLevel(level: String): Flow<List<WordEntity>>


    // ========== 收藏与跳过 ==========

    /**
     * 获取收藏的单词
     */
    @Query("""
        SELECT w.* FROM words w
        JOIN word_study_states s ON w.id = s.word_id
        WHERE s.is_favorite = 1 AND s.is_deleted = 0 AND w.is_delisted = 0 ORDER BY w.id DESC
    """)
    fun getFavoriteWords(): Flow<List<WordEntity>>

    /**
     * 更新收藏状态
     */
    @Query("UPDATE word_study_states SET is_favorite = :isFavorite, last_modified_time = :lastModifiedTime WHERE word_id = :wordId")
    suspend fun updateFavoriteStatus(wordId: Int, isFavorite: Boolean, lastModifiedTime: Long)

    /**
     * 获取跳过的单词
     */
    @Query("""
        SELECT w.* FROM words w
        JOIN word_study_states s ON w.id = s.word_id
        WHERE s.is_skipped = 1 AND s.is_deleted = 0 AND w.is_delisted = 0 ORDER BY w.id DESC LIMIT :limit
    """)
    fun getSkippedWords(limit: Int): Flow<List<WordEntity>>

    /**
     * 获取跳过的单词数量
     */
    @Query("""
        SELECT COUNT(*) FROM word_study_states s
        JOIN words w ON s.word_id = w.id
        WHERE s.is_skipped = 1 AND s.is_deleted = 0 AND w.is_delisted = 0
    """)
    fun getSkippedWordsCount(): Flow<Int>

    /**
     * 获取指定等级的所有单词
     * @param level JLPT等级（如 "n5"、"n4" 等，小写）
     * @return 该等级所有单词列表Flow
     */
    @Query("""
        SELECT w.* FROM words w
        LEFT JOIN word_study_states s ON w.id = s.word_id
        WHERE w.level = :level
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND w.is_delisted = 0
        ORDER BY w.id ASC
    """)
    fun getAllWordsByLevel(level: String): Flow<List<WordEntity>>

    /**
     * 获取按复习日期排序的已学单词 (Adaptive Strategy Optimized Query)
     * 用于自适应测试：优先选择最该复习的单词 (nextReviewDate 越小越优先)
     */
    @Query("""
        SELECT w.* FROM words w
        JOIN word_study_states s ON w.id = s.word_id
        WHERE s.repetition_count > 0
        AND w.level IN (:levels)
        AND (s.is_skipped = 0 OR s.is_skipped IS NULL)
        AND s.is_deleted = 0
        AND w.is_delisted = 0
        ORDER BY s.next_review_date ASC
        LIMIT :limit
    """)
    suspend fun getWordsSortedByNextReviewDate(levels: List<String>, limit: Int): List<WordEntity>

    // ========== 词性分类查询（12种分类，首成分匹配）==========

    /**
     * 获取所有动词
     * 首成分为：他動 / 自動 / 自他動
     */
    @Query("""
        SELECT * FROM words
        WHERE (pos LIKE '他動%' OR pos LIKE '自動%' OR pos LIKE '自他動%')
        AND is_delisted = 0
        ORDER BY id ASC
    """)
    suspend fun getVerbs(): List<WordEntity>

    /**
     * 获取所有名词
     * 首成分为：名 / 代
     */
    @Query("""
        SELECT * FROM words
        WHERE (pos LIKE '名%' OR pos LIKE '代%')
        AND is_delisted = 0
        ORDER BY id ASC
    """)
    suspend fun getNouns(): List<WordEntity>

    /**
     * 获取所有形容词
     * 首成分为：イ形 / ナ形
     */
    @Query("""
        SELECT * FROM words
        WHERE (pos LIKE 'イ形%' OR pos LIKE 'ナ形%')
        AND is_delisted = 0
        ORDER BY id ASC
    """)
    suspend fun getAdjectives(): List<WordEntity>

    /**
     * 获取所有副词
     * 首成分为：副
     */
    @Query("SELECT * FROM words WHERE pos LIKE '副%' AND is_delisted = 0 ORDER BY id ASC")
    suspend fun getAdverbs(): List<WordEntity>

    /**
     * 获取所有助词
     * 首成分为：助
     */
    @Query("SELECT * FROM words WHERE pos LIKE '助%' AND is_delisted = 0 ORDER BY id ASC")
    suspend fun getParticles(): List<WordEntity>

    /**
     * 获取所有接续词
     * 首成分为：接 / 接続（排除接尾、接頭）
     */
    @Query("""
        SELECT * FROM words
        WHERE pos LIKE '接%' AND pos NOT LIKE '接尾%' AND pos NOT LIKE '接頭%'
        AND is_delisted = 0
        ORDER BY id ASC
    """)
    suspend fun getConjunctions(): List<WordEntity>

    /**
     * 获取所有连体词
     * 首成分为：連体
     */
    @Query("SELECT * FROM words WHERE pos = '連体' AND is_delisted = 0 ORDER BY id ASC")
    suspend fun getRentai(): List<WordEntity>

    /**
     * 获取所有接头词（前缀）
     * 首成分为：接頭
     */
    @Query("SELECT * FROM words WHERE pos LIKE '接頭%' AND is_delisted = 0 ORDER BY id ASC")
    suspend fun getPrefixes(): List<WordEntity>

    /**
     * 获取所有接尾词（后缀）
     * 首成分为：接尾
     */
    @Query("SELECT * FROM words WHERE pos LIKE '接尾%' AND is_delisted = 0 ORDER BY id ASC")
    suspend fun getSuffixes(): List<WordEntity>

    /**
     * 获取所有感叹词
     * 首成分为：嘆
     */
    @Query("SELECT * FROM words WHERE pos LIKE '嘆%' AND is_delisted = 0 ORDER BY id ASC")
    suspend fun getInterjections(): List<WordEntity>

    /**
     * 获取所有固定表达（惯用语/连语）
     * 首成分为：連語
     */
    @Query("SELECT * FROM words WHERE pos LIKE '連語%' AND is_delisted = 0 ORDER BY id ASC")
    suspend fun getFixedExpressions(): List<WordEntity>

    // ========== 批量操作 ==========

    /**
     * 根据ID列表获取单词
     */
    @Query("""
        SELECT COUNT(*) FROM word_study_states
        WHERE last_modified_time > :timestamp
    """)
    suspend fun countModifiedSince(timestamp: Long): Int

    @Query("SELECT * FROM words WHERE id IN (:ids)")
    suspend fun getWordsByIds(ids: List<Int>): List<WordEntity>

    /**
     * 搜索单词 (匹配日文、中文、假名)
     */
    @Query("""
        SELECT w.* FROM words w
        LEFT JOIN word_study_states s ON w.id = s.word_id
        WHERE (w.japanese LIKE '%' || :query || '%'
        OR w.chinese LIKE '%' || :query || '%'
        OR w.hiragana LIKE '%' || :query || '%')
        AND (s.is_deleted = 0 OR s.is_deleted IS NULL)
        AND w.is_delisted = 0
        ORDER BY w.id ASC
    """)
    fun searchWords(query: String): Flow<List<WordEntity>>

    /**
     * 重置所有学习进度
     */
    @Query("DELETE FROM word_study_states")
    suspend fun resetAllProgress()

    /**
     * 清空所有收藏
     */
    @Query("UPDATE word_study_states SET is_favorite = 0 WHERE is_favorite = 1")
    suspend fun clearAllFavorites()

    // ========== 测试用查询 ==========

    /**
     * 获取随机错误选项（用于选择题）
     * @param correctChinese 正确答案的中文
     * @param limit 需要的错误选项数量
     * @return 随机错误选项列表
     */
    @Query("SELECT * FROM words WHERE chinese != :correctChinese ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWrongOptions(correctChinese: String, limit: Int): List<WordEntity>

    /**
     * 获取所有单词
     * 用于内存筛选（例如外来语）
     */
    @Query("SELECT * FROM words")
    fun getAllWords(): List<WordEntity>

    /**
     * 获取所有单词 (同步) - 用于导出
     * 注意：getAllWords目前定义为fun getAllWords(): List<WordEntity> (suspend implied by Room if not Flow? No, must use suspend if not Flow)
     * To be safe and consistent with other DAOs, let's explicitly add getAllWordsSync
     */


    /**
     * 获取所有单词 (Cursor) - 用于流式导出
     * 优化：仅导出有进度或状态的单词
     */
    @Query("""
        SELECT
            w.id, w.japanese, w.hiragana, w.chinese, w.level,
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
        FROM words w
        JOIN word_study_states s ON w.id = s.word_id
        WHERE s.repetition_count > 0
        OR s.is_favorite = 1
        OR s.is_skipped = 1
        OR s.is_deleted = 1
        OR s.first_learned_date IS NOT NULL
        OR s.last_modified_time > 0
    """)
    fun getExportWordsCursor(): android.database.Cursor

    @Query("SELECT * FROM words")
    suspend fun getAllWordsSync(): List<WordEntity>

    /**
     * 获取复习预测
     * @param startDateEpochDay 开始日期
     * @param endDateEpochDay 结束日期
     */
    @Query("""
        SELECT s.next_review_date AS date, COUNT(*) AS count
        FROM word_study_states s
        WHERE s.next_review_date BETWEEN :startDateEpochDay AND :endDateEpochDay
        AND s.is_skipped = 0
        GROUP BY s.next_review_date
    """)
    fun getReviewForecast(startDateEpochDay: Long, endDateEpochDay: Long): Flow<List<ReviewForecastTuple>>

    // ========== 等级查询 ==========

    @Query("""
        SELECT DISTINCT w.level
        FROM word_study_states s
        JOIN words w ON s.word_id = w.id
        WHERE s.last_reviewed_date = :todayEpochDay
    """)
    fun getTodayReviewedLevels(todayEpochDay: Long): Flow<List<String>>

    @Query("""
        SELECT DISTINCT w.level
        FROM word_study_states s
        JOIN words w ON s.word_id = w.id
        WHERE s.first_learned_date = :todayEpochDay
    """)
    fun getTodayLearnedLevels(todayEpochDay: Long): Flow<List<String>>

    @Query("""
        SELECT DISTINCT w.level
        FROM word_study_states s
        JOIN words w ON s.word_id = w.id
        WHERE s.is_favorite = 1
    """)
    fun getFavoriteLevels(): Flow<List<String>>

    @Query("""
        SELECT DISTINCT w.level
        FROM word_study_states s
        JOIN words w ON s.word_id = w.id
        WHERE s.repetition_count > 0
    """)
    fun getLearnedLevels(): Flow<List<String>>

    @Query("SELECT DISTINCT T2.level FROM wrong_answers T1 JOIN words T2 ON T1.word_id = T2.id")
    fun getWrongAnswerLevels(): Flow<List<String>>

    // ========== 数据统计 ==========

    /**
     * 获取单词总数
     */
    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    // ========== 数据修复 ==========

    /**
     * 获取去重后的保留ID列表 (每个单词只保留ID最小的一个)
     * 用于清理本地重复数据
     */
    @Query("SELECT MIN(id) FROM words GROUP BY japanese, hiragana, chinese, level")
    suspend fun getDuplicateKeepIds(): List<Int>

    /**
     * 将指定等级下，不在给定日语原文列表中的单词标记为已下架
     * 用于云更新时的「静默下架」逻辑（处理 JSON 中直接删除行的情况）
     */
    @Query("UPDATE words SET is_delisted = 1 WHERE level = :level AND japanese NOT IN (:jsonJapaneseList)")
    suspend fun markMissingAsDelisted(level: String, jsonJapaneseList: List<String>): Int
}

data class ReviewForecastTuple(
    val date: Long,
    val count: Int
)
