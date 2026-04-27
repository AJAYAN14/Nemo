package com.jian.nemo.core.data.service

import android.util.Log
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.common.util.SyncEvent
import com.jian.nemo.core.common.util.SyncMessageBus
import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.SyncRepository
import com.jian.nemo.core.domain.service.SyncManager
import com.jian.nemo.core.domain.service.SyncService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * 同步服务实现
 * 负责核心业务决策：是否开启同步、同步频率检查等。
 */
@Singleton
class SyncServiceImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val syncManager: SyncManager,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val syncMessageBus: SyncMessageBus 
) : SyncService {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var foregroundTimerJob: Job? = null

    companion object {
        private const val TAG = "SyncServiceImpl"
        private const val MIN_SYNC_INTERVAL = 60 * 1000L // 1分钟
        private val FOREGROUND_SYNC_INTERVAL = 5.minutes
    }

    override fun onLearningCompleted() {
        scope.launch {
            val isEnabled = settingsRepository.isSyncOnLearningCompleteFlow.first()
            if (isEnabled) {
                scheduleSyncIfNeeded(checkInterval = false)
            }
        }
    }

    override fun onTestCompleted() {
        scope.launch {
            val isEnabled = settingsRepository.isSyncOnTestCompleteFlow.first()
            if (isEnabled) {
                scheduleSyncIfNeeded(checkInterval = false)
            }
        }
    }

    override fun onAppForeground() {
        Log.d(TAG, "应用进入前台，启动 5 分钟定时器并检查后台同步")
        // 确保周期性任务已运行
        syncManager.startPeriodicSync()

        // 立即触发一次校验
        scheduleSyncIfNeeded(checkInterval = true)

        // 开启 5 分钟定时器
        startForegroundTimer()
    }

    override fun onAppBackground() {
        Log.d(TAG, "应用进入后台，停止定时器")
        stopForegroundTimer()
    }

    /**
     * 启动前台高频同步定时器
     */
    private fun startForegroundTimer() {
        foregroundTimerJob?.cancel()
        foregroundTimerJob = scope.launch {
            // 初始等待 5 分钟后开始循环（onAppForeground 已触发立即同步）
            while (isActive) {
                delay(FOREGROUND_SYNC_INTERVAL)
                Log.d(TAG, "前台 5 分钟定时器触发，准备执行同步...")
                
                // 在前台时，直接执行同步而不是通过 WorkManager 调度，以规避系统的低电量节流策略
                performDirectSyncIfNeeded()
            }
        }
    }

    private fun stopForegroundTimer() {
        foregroundTimerJob?.cancel()
        foregroundTimerJob = null
    }

    private fun scheduleSyncIfNeeded(checkInterval: Boolean) {
        scope.launch {
            // 1. 检查总开关
            val isEnabled = settingsRepository.isAutoSyncEnabledFlow.first()
            if (!isEnabled) return@launch

            // 2. 检查时间间隔（仅在 checkInterval 为 true 时检查）
            if (checkInterval) {
                val lastSyncTime = settingsRepository.lastSyncTimeFlow.first()
                val now = DateTimeUtils.getCurrentCompensatedMillis()
                if (now - lastSyncTime < MIN_SYNC_INTERVAL) {
                    Log.d(TAG, "同步频率过高，跳过本次检查")
                    return@launch
                }
            }

            // 3. 执行物理同步
            Log.d(TAG, "触发后台 Worker 同步任务...")
            syncManager.startSync()
        }
    }
    /**
     * 前台直接触发同步 (高优先级)
     */
    private suspend fun performDirectSyncIfNeeded() {
        // 1. 检查开关
        val isEnabled = settingsRepository.isAutoSyncEnabledFlow.first()
        if (!isEnabled) return

        // 2. 检查登录状态
        val user = authRepository.getCurrentUser()
        if (user == null) {
            Log.d(TAG, "未检测到登录用户，跳过前台定时器同步")
            return
        }

        // 3. 执行同步过程 (由 Repository 管理具体 Pull/Push 逻辑)
        Log.i(TAG, "执行前台直接同步任务 (Bypass WorkManager)...")
        try {
            syncRepository.performSync(
                userId = user.id,
                force = false
            ).collect { /* 进度由全局 UI 监听 */ }
        } catch (e: Exception) {
            Log.e(TAG, "前台定时同步运行异常", e)
            // 发送全局失败通知，以便用户知晓自动同步中断
            syncMessageBus.tryEmit(SyncEvent.Error("前台同步异常: ${e.message ?: "网络异常"}"))
        }
    }
}
