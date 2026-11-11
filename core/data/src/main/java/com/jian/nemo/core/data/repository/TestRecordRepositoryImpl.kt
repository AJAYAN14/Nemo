package com.jian.nemo.core.data.repository

import com.jian.nemo.core.data.local.dao.TestRecordDao
import com.jian.nemo.core.data.local.entity.TestRecordEntity
import com.jian.nemo.core.domain.model.TestRecord
import com.jian.nemo.core.domain.repository.TestRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestRecordRepositoryImpl @Inject constructor(
    private val testRecordDao: TestRecordDao
) : TestRecordRepository {

    override fun getRecordsByMode(testMode: String, limit: Int): Flow<List<TestRecord>> {
        return testRecordDao.getRecordsByMode(testMode, limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertRecord(record: TestRecord) {
        testRecordDao.insert(record.toEntity())
    }

    override suspend fun insertRecords(records: List<TestRecord>) {
        testRecordDao.insertAll(records.map { it.toEntity() })
    }

    override fun getAllRecords(): Flow<List<TestRecord>> {
        return testRecordDao.getAllRecords().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun TestRecordEntity.toDomain(): TestRecord {
        return TestRecord(
            id = id,
            date = date,
            totalQuestions = totalQuestions,
            correctAnswers = correctAnswers,
            testMode = testMode,
            timestamp = timestamp
        )
    }

    private fun TestRecord.toEntity(): TestRecordEntity {
        return TestRecordEntity(
            id = id,
            date = date,
            totalQuestions = totalQuestions,
            correctAnswers = correctAnswers,
            testMode = testMode,
            timestamp = timestamp
        )
    }
}
