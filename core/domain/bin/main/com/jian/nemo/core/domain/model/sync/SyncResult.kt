package com.jian.nemo.core.domain.model.sync

import com.jian.nemo.core.domain.model.SyncReport

/**
 * 同步结果数据类
 */
data class SyncResult(
    val success: Boolean,
    val message: String,
    val cloudDataId: String? = null,
    val baseSnapshot: String? = null,
    val syncReport: SyncReport? = null,
    val syncVersion: Int? = null,
    val errorType: SyncErrorType? = null
)
