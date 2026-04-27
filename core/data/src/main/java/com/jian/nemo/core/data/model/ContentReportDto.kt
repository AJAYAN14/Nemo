package com.jian.nemo.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 内容报告数据传输对象 (与 Supabase 表 structure 对应)
 */
@Serializable
data class ContentReportDto(
    @SerialName("item_id")
    val itemId: Int,
    @SerialName("item_type")
    val itemType: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("status")
    val status: String = "pending"
)
