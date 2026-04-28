package com.jian.nemo.core.data.util

import android.util.Log
import com.jian.nemo.core.data.local.NemoDatabase
import com.jian.nemo.core.data.manager.SupabaseSyncManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库初始化器
 *
 * 用于在应用启动时触发数据库创建，并执行启动时的词库同步检查
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val database: NemoDatabase,
    private val syncManager: SupabaseSyncManager
) {
    suspend fun initialize() {
        try {
            Log.d(TAG, "Initializing database and checking dictionary sync...")
            
            // 1. 触发数据库创建
            database.wordDao().getDueWordsCount(System.currentTimeMillis() / 86400000).first()
            
            // 2. 启动时词库同步检查（检测版本并增量下载）
            syncManager.performDictionarySync()
            
            Log.d(TAG, "Database initialization and sync check completed")
        } catch (e: Exception) {
            Log.e(TAG, "Initialization/Sync failed during startup", e)
        }
    }

    companion object {
        private const val TAG = "DatabaseInitializer"
    }
}
