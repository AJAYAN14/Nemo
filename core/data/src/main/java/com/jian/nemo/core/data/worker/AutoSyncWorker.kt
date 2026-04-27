package com.jian.nemo.core.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.model.sync.SyncErrorType
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.SyncRepository
import com.jian.nemo.core.domain.model.sync.SyncMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 自动同步 Worker
 * 在后台执行同步任务
 */
@HiltWorker
class AutoSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val syncMessageBus: com.jian.nemo.core.common.util.SyncMessageBus
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AutoSyncWorker"
        const val WORK_NAME = "auto_sync"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "开始执行自动同步")

        try {
            // 1. 检查是否启用自动同步
            val isEnabled = settingsRepository.isAutoSyncEnabledFlow.first()
            if (!isEnabled) {
                Log.d(TAG, "自动同步已禁用，取消任务")
                return@withContext Result.success()
            }

            // 2. 获取当前用户
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                Log.d(TAG, "未找到登录用户，取消同步")
                return@withContext Result.success()
            }

            Log.d(TAG, "用户已登录: ${currentUser.id}，开始同步...")

            // 3. 执行双向同步（performSync 包含了 Pull + Push）
            // 该方法在 TWO_WAY 模式下遵循：
            // 1. 拉取云端变更并合并 (SmartSyncMerger)
            // 2. 推送本地新增/修改的变更

            // 优化：增量检测
            val lastSyncTime = settingsRepository.lastSyncTimeFlow.first()
            if (lastSyncTime > 0) {
                // 检查是否有未同步的变更
                val hasChanges = syncRepository.hasUnsyncedChanges(lastSyncTime)
                if (!hasChanges) {
                    Log.d(TAG, "检测到本地无新增变更，但仍需检查云端更新")
                } else {
                    Log.d(TAG, "检测到本地有变更，准备执行同步...")
                }
            }

            Log.d(TAG, "执行增量同步（Pull + Push）...")
            val syncResult = syncRepository.checkAndRestoreCloudData(
                userId = currentUser.id,
                force = false,
                mode = SyncMode.TWO_WAY
            )

            if (syncResult.success) {
                 Log.d(TAG, "自动同步成功: ${syncResult.message}")

                 // P1 优化：记录冲突数量
                 val conflictCount = syncResult.syncReport?.conflicts?.size ?: 0
                 settingsRepository.setLastSyncConflictCount(conflictCount)
                 if (conflictCount > 0) {
                     Log.w(TAG, "同步合并中发现 $conflictCount 个冲突")
                 }

                 settingsRepository.setLastSyncTime(System.currentTimeMillis())
                 settingsRepository.setLastSyncSuccess(true)
                 settingsRepository.setLastSyncError("")
                 
                 // 优化：静默后台同步。不再发送全局成功通知，仅在失败时提醒。
                 // syncMessageBus.tryEmit(com.jian.nemo.core.common.util.SyncEvent.Success())

                 return@withContext Result.success()

            } else {
                 Log.w(TAG, "自动同步失败: ${syncResult.message}, 类型: ${syncResult.errorType}")

                 settingsRepository.setLastSyncSuccess(false)
                 settingsRepository.setLastSyncError(syncResult.message)

                 // 发送全局失败通知 (包含具体原因)
                 syncMessageBus.tryEmit(com.jian.nemo.core.common.util.SyncEvent.Error("同步失败: ${syncResult.message}"))

                 if (syncResult.errorType == SyncErrorType.NETWORK_ERROR) {
                     Log.e(TAG, "检测到网络异常，稍后重试")
                     return@withContext Result.retry()
                 }
                 return@withContext Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "自动同步异常", e)
            settingsRepository.setLastSyncSuccess(false)
            settingsRepository.setLastSyncError(e.message ?: "未知错误")

            // 发送全局异常通知
            syncMessageBus.tryEmit(com.jian.nemo.core.common.util.SyncEvent.Error("同步异常: ${e.message ?: "未知错误"}"))

            return@withContext Result.retry()
        }
    }
}
