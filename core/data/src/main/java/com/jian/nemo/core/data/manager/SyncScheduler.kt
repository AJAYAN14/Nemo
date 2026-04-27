package com.jian.nemo.core.data.manager

import android.content.Context
import android.util.Log
import androidx.work.*
import com.jian.nemo.core.data.worker.AutoSyncWorker
import com.jian.nemo.core.domain.service.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步调度器
 * 负责与 WorkManager 交互，执行后台同步任务。
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncManager {

    companion object {
        private const val TAG = "SyncScheduler"
    }

    override fun startSync() {
        Log.d(TAG, "手动触发单次同步任务 (Unique: manual_sync)...")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<AutoSyncWorker>()
            .setConstraints(constraints)
            .addTag(AutoSyncWorker.WORK_NAME)
            .build()

        // 使用固定名称和 REPLACE 策略，确保队列中只有一个待执行任务，防止堆积
        WorkManager.getInstance(context).enqueueUniqueWork(
            "nemo_manual_sync_task",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    override fun startPeriodicSync() {
        // Android 系统要求周期性任务最小间隔为 15 分钟
        Log.d(TAG, "启动周期性后台同步调度 (15分钟/次)...")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<AutoSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(AutoSyncWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AutoSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }
}
