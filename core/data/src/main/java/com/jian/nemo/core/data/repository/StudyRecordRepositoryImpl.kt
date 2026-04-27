package com.jian.nemo.core.data.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.data.local.dao.StudyRecordDao
import com.jian.nemo.core.data.mapper.StudyRecordMapper.toDomainModel
import com.jian.nemo.core.data.mapper.StudyRecordMapper.toDomainModels
import com.jian.nemo.core.data.mapper.StudyRecordMapper.toEntity
import com.jian.nemo.core.domain.model.StudyRecord
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * StudyRecord Repository 实现
 *
 * 职责:
 * 1. 调用StudyRecordDao获取数据
 * 2. Entity → Domain Model转换
 * 3.实现增量更新方法（incrementLearnedWords等）
 * 4. 异常处理和日志
 */
@Singleton
class StudyRecordRepositoryImpl @Inject constructor(
    private val studyRecordDao: StudyRecordDao,
    private val settingsRepository: SettingsRepository
) : StudyRecordRepository {

    // ========== 查询实现 ==========

    override fun getRecordByDate(date: Long): Flow<StudyRecord?> {
        return studyRecordDao.getByDate(date)
            .map { it?.toDomainModel() }
            .catch { e ->
                println("❌ 获取学习记录失败: date=$date, error=${e.message}")
                emit(null)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTodayRecord(): Flow<StudyRecord?> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val today = DateTimeUtils.getLearningDay(resetHour)
            getRecordByDate(today)
        }
    }

    override fun getAllRecords(): Flow<List<StudyRecord>> {
        return studyRecordDao.getAllRecords()
            .map { it.toDomainModels() }
            .catch { e ->
                println("❌ 获取所有学习记录失败: error=${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getRecordsBetween(startDate: Long, endDate: Long): Flow<List<StudyRecord>> {
        return studyRecordDao.getRecordsBetween(startDate, endDate)
            .map { it.toDomainModels() }
            .catch { e ->
                println("❌ 获取学习记录范围失败: error=${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getTotalStudyDays(): Flow<Int> {
        return studyRecordDao.getTotalStudyDays()
            .catch { e ->
                println("❌ 获取总学习天数失败: error=${e.message}")
                emit(0)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getDailyActivityCounts(startDate: Long, endDate: Long): Flow<Map<Long, Int>> {
        return studyRecordDao.getDailyActivityCounts(startDate, endDate)
            .map { list ->
                list.associate { it.date to it.totalCount }
            }
            .catch { e ->
                println("❌ 获取热力图数据失败: error=${e.message}")
                emit(emptyMap())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    // ========== 更新实现 ==========

    override suspend fun upsertRecord(record: StudyRecord): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val entity = record.toEntity()
            studyRecordDao.insert(entity)
            println("✅ 学习记录已保存: date=${record.date}")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 保存学习记录失败: error=${e.message}")
            Result.Error(e)
        }
    }

    /**
     * 获取或创建今日记录
     */
    private suspend fun getOrCreateTodayRecord(): StudyRecord {
        val resetHour = settingsRepository.learningDayResetHourFlow.first()
        val today = DateTimeUtils.getLearningDay(resetHour)

        runCatching { settingsRepository.updateDailyStreak() }
            .onFailure { e -> println("⚠️ 更新连续学习天数失败: ${e.message}") }

        return studyRecordDao.getByDate(today).first()?.toDomainModel()
            ?: StudyRecord(date = today)
    }

    override suspend fun incrementLearnedWords(count: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val record = getOrCreateTodayRecord()
            val updated = record.copy(
                learnedWords = record.learnedWords + count,
                timestamp = DateTimeUtils.getCurrentCompensatedMillis()
            )
            upsertRecord(updated)
        } catch (e: Exception) {
            println("❌ 增加学习单词数失败: error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun incrementLearnedGrammars(count: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val record = getOrCreateTodayRecord()
            val updated = record.copy(
                learnedGrammars = record.learnedGrammars + count,
                timestamp = DateTimeUtils.getCurrentCompensatedMillis()
            )
            upsertRecord(updated)
        } catch (e: Exception) {
            println("❌ 增加学习语法数失败: error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun incrementReviewedWords(count: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val record = getOrCreateTodayRecord()
            val updated = record.copy(
                reviewedWords = record.reviewedWords + count,
                timestamp = DateTimeUtils.getCurrentCompensatedMillis()
            )
            upsertRecord(updated)
        } catch (e: Exception) {
            println("❌ 增加复习单词数失败: error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun incrementReviewedGrammars(count: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val record = getOrCreateTodayRecord()
            val updated = record.copy(
                reviewedGrammars = record.reviewedGrammars + count,
                timestamp = DateTimeUtils.getCurrentCompensatedMillis()
            )
            upsertRecord(updated)
        } catch (e: Exception) {
            println("❌ 增加复习语法数失败: error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun incrementSkippedWords(count: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val record = getOrCreateTodayRecord()
            val updated = record.copy(
                skippedWords = record.skippedWords + count,
                timestamp = DateTimeUtils.getCurrentCompensatedMillis()
            )
            upsertRecord(updated)
        } catch (e: Exception) {
            println("❌ 增加跳过单词数失败: error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun incrementSkippedGrammars(count: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val record = getOrCreateTodayRecord()
            val updated = record.copy(
                skippedGrammars = record.skippedGrammars + count,
                timestamp = DateTimeUtils.getCurrentCompensatedMillis()
            )
            upsertRecord(updated)
        } catch (e: Exception) {
            println("❌ 增加跳过语法数失败: error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun incrementTestCount(count: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val record = getOrCreateTodayRecord()
            val updated = record.copy(
                testCount = record.testCount + count,
                timestamp = DateTimeUtils.getCurrentCompensatedMillis()
            )
            upsertRecord(updated)
        } catch (e: Exception) {
            println("❌ 增加测试次数失败: error=${e.message}")
            Result.Error(e)
        }
    }

    // ========== 删除 ==========

    override suspend fun deleteByDate(date: Long): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            studyRecordDao.markDeletedByDate(date, DateTimeUtils.getCurrentCompensatedMillis())
            println("✅ 学习记录已标记为删除: date=$date")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 删除学习记录失败: error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun deleteAll(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            studyRecordDao.deleteAll()
            println("✅ 所有学习记录已删除")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 删除所有学习记录失败: error=${e.message}")
            Result.Error(e)
        }
    }
}
