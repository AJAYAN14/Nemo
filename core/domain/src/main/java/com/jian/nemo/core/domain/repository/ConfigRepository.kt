package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.AppUpdateConfig

/**
 * 配置仓库接口
 */
interface ConfigRepository {

    /**
     * 获取最新的应用更新配置
     *
     * @param forceRefresh 是否跳过进程内存缓存直接请求云端
     */
    suspend fun getUpdateConfig(forceRefresh: Boolean = false): AppUpdateConfig?

    /**
     * 获取当前有效的更新配置缓存（优先读取进程内存缓存，其次读取版本匹配的本地持久化缓存）
     */
    suspend fun getCachedUpdateConfig(): AppUpdateConfig?

    /**
     * 获取当前安装的应用版本号
     */
    fun getCurrentVersionCode(): Int
}
