package com.jian.nemo.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 应用版本更新配置模型
 */
@Serializable
data class AppUpdateConfig(
    @SerialName("version_code") val versionCode: Int,            // version_code
    @SerialName("version_name") val versionName: String,        // version_name
    @SerialName("update_log") val updateLog: String,          // update_log
    @SerialName("download_url") val downloadUrl: String,        // download_url
    @SerialName("is_force") val isForce: Boolean = false,    // is_force
    @SerialName("can_close") val canClose: Boolean = true     // can_close
)
