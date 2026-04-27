package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.AppUpdateConfig

/**
 * 配置仓库接口
 */
interface ConfigRepository {

    /**
     * 获取最新的应用更新配置
     */
    suspend fun getUpdateConfig(): AppUpdateConfig?

    /**
     * 获取当前安装的应用版本号
     */
    fun getCurrentVersionCode(): Int
}
