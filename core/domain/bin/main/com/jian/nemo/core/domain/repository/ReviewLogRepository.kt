package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.ReviewLog

interface ReviewLogRepository {
    suspend fun insertLog(log: ReviewLog)
    suspend fun getRecentLogs(limit: Int = 1500): List<ReviewLog>
}
