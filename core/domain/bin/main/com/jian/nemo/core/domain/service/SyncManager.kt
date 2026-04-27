package com.jian.nemo.core.domain.service

/**
 * 同步执行器接口 (物理执行)
 */
interface SyncManager {
    /**
     * 执行物理同步任务（由 Data 层 WorkManager 实现）
     */
    fun startSync()

    /**
     * 开启周期性后台同步 (12小时/次)
     */
    fun startPeriodicSync()
}
