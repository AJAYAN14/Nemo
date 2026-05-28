package com.jian.nemo.core.domain.model

import kotlinx.serialization.Serializable

/**
 * 同步报错日志项
 */
@Serializable
data class SyncErrorLog(
    val timestamp: Long,
    val message: String
)
