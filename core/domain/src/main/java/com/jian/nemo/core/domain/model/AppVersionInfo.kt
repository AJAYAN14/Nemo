package com.jian.nemo.core.domain.model

/**
 * 应用版本信息
 *
 * @property versionCode 编译版本号 (versionCode)
 * @property versionName 显示版本名 (versionName)
 */
data class AppVersionInfo(
    val versionCode: Int,
    val versionName: String
)
