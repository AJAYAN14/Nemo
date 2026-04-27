package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.StudyRecord
import kotlinx.coroutines.flow.Flow

/**
 * 学习记录 Repository 接口
 *
 * Domain层只定义接口，Data层实现
 */
interface StudyRecordRepository {

    // ========== 查询 ==========

    /**
     * 获取指定日期的学习记录
     */
    fun getRecordByDate(date: Long): Flow<StudyRecord?>

    /**
     * 获取今日学习记录
     */
    fun getTodayRecord(): Flow<StudyRecord?>

    /**
     * 获取所有学习记录（按日期降序）
     */
    fun getAllRecords(): Flow<List<StudyRecord>>

    /**
     * 获取日期范围内的学习记录
     */
    fun getRecordsBetween(startDate: Long, endDate: Long): Flow<List<StudyRecord>>

    /**
     * 获取总学习天数
     */
    fun getTotalStudyDays(): Flow<Int>

    /**
     * 获取每日学习活动计数 (用于热力图)
     * @return Map<DateEpoch, Count> Key=日期EpochDay, Value=当日总学习数(学+复习)
     */
    fun getDailyActivityCounts(startDate: Long, endDate: Long): Flow<Map<Long, Int>>

    // ========== 更新 ==========

    /**
     * 插入或更新学习记录
     */
    suspend fun upsertRecord(record: StudyRecord): Result<Unit>

    /**
     * 增加今日学习单词数
     */
    suspend fun incrementLearnedWords(count: Int = 1): Result<Unit>

    /**
     * 增加今日学习语法数
     */
    suspend fun incrementLearnedGrammars(count: Int = 1): Result<Unit>

    /**
     * 增加今日复习单词数
     */
    suspend fun incrementReviewedWords(count: Int = 1): Result<Unit>

    /**
     * 增加今日复习语法数
     */
    suspend fun incrementReviewedGrammars(count: Int = 1): Result<Unit>

    /**
     * 增加今日跳过单词数
     */
    suspend fun incrementSkippedWords(count: Int = 1): Result<Unit>

    /**
     * 增加今日跳过语法数
     */
    suspend fun incrementSkippedGrammars(count: Int = 1): Result<Unit>

    /**
     * 增加今日测试次数
     */
    suspend fun incrementTestCount(count: Int = 1): Result<Unit>

    // ========== 删除 ==========

    /**
     * 删除指定日期的记录
     */
    suspend fun deleteByDate(date: Long): Result<Unit>

    /**
     * 删除所有学习记录
     */
    suspend fun deleteAll(): Result<Unit>
}
