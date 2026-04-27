package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.SyncProgress
import com.jian.nemo.core.domain.model.sync.SyncMode
import com.jian.nemo.core.domain.model.sync.SyncResult
import kotlinx.coroutines.flow.Flow

/**
 * 同步仓库接口 - 定义云端数据同步与恢复的相关业务逻辑
 */
interface SyncRepository {
    // 暂时假定不加 order 在单用户静态同步场景下问题不大。
    /**
     * 全局同步进度状态
     */
    val globalSyncProgress: kotlinx.coroutines.flow.StateFlow<SyncProgress>

    /**
     * 启动后台同步 (Fire-and-Forget)
     * 使用 Application Scope 执行，不会因 UI 销毁而中断
     */
    fun startBackgroundSync(
        userId: String,
        force: Boolean = false,
        mode: SyncMode = SyncMode.TWO_WAY
    )

    /**
     * 执行同步操作（核心 Flow 入口）
     * @param userId 用户 ID
     * @param force 是否强制全量（忽略时间戳）
     * @param mode 同步模式（双向、仅拉取等）
     */
    suspend fun performSync(
        userId: String,
        force: Boolean = false,
        mode: SyncMode = SyncMode.TWO_WAY
    ): Flow<SyncProgress>

    /**
     * 执行全量镜像恢复
     */
    suspend fun performRestore(userId: String): Flow<SyncProgress>

    /**
     * 检查并从云端恢复数据（同步版本，返回结果）
     */
    suspend fun checkAndRestoreCloudData(
        userId: String,
        force: Boolean = false,
        mode: SyncMode = SyncMode.TWO_WAY
    ): SyncResult

    /**
     * 彻底物理删除该用户在云端的所有数据记录
     */
    suspend fun deleteAllCloudData(userId: String): Boolean

    suspend fun hasUnsyncedChanges(sinceTimestamp: Long): Boolean

}
