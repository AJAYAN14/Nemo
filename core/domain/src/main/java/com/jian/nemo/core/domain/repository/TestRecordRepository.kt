package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.TestRecord
import kotlinx.coroutines.flow.Flow

/**
 * 测试记录仓储接口
 */
interface TestRecordRepository {
    fun getRecordsByMode(testMode: String, limit: Int = 50): Flow<List<TestRecord>>
    suspend fun insertRecord(record: TestRecord)
    suspend fun insertRecords(records: List<TestRecord>)
    fun getAllRecords(): Flow<List<TestRecord>>
}
