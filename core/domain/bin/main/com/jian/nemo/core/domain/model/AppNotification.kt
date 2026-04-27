package com.jian.nemo.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 远程通知数据模型
 *
 * 对应 Supabase notifications 表
 */
@Serializable
data class AppNotification(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
    @SerialName("active") val active: Boolean = true
)
