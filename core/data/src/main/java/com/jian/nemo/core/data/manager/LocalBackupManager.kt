package com.jian.nemo.core.data.manager

import android.content.Context
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataExportManager: DataExportManager
) {
    companion object {
        private const val TAG = "LocalBackupManager"
        private const val MAX_FILES_PER_DAY = 50
        private const val MAX_DAYS_RETENTION = 7
    }

    suspend fun performBackup() = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val baseBackupDir = File(downloadsDir, "NemoBackups")
            if (!baseBackupDir.exists()) {
                baseBackupDir.mkdirs()
            }

            val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            val now = Date()
            
            val todayFolderStr = dateFormat.format(now)
            val todayDir = File(baseBackupDir, todayFolderStr)
            if (!todayDir.exists()) {
                todayDir.mkdirs()
            }

            // 1. 生成备份文件
            val fileName = "nemo_sync_auto_${todayFolderStr}_${timeFormat.format(now)}.json"
            val backupFile = File(todayDir, fileName)
            Log.d(TAG, "开始自动备份到: ${backupFile.absolutePath}")
            
            // 直接复用底层流式导出逻辑，防止 OOM，确保格式一致
            dataExportManager.exportDataToFile("default_user", backupFile)

            // 2. 清理当日文件夹（最多保留 50 个文件）
            cleanUpTodayFiles(todayDir)

            // 3. 清理历史文件夹（最多保留 7 天）
            cleanUpOldFolders(baseBackupDir)
            
            Log.d(TAG, "自动备份及清理完成")
        } catch (e: Exception) {
            Log.e(TAG, "自动备份失败", e)
        }
    }

    private fun cleanUpTodayFiles(todayDir: File) {
        val files = todayDir.listFiles()?.filter { it.isFile && it.name.startsWith("nemo_sync_auto_") } ?: return
        if (files.size > MAX_FILES_PER_DAY) {
            val sortedFiles = files.sortedByDescending { it.lastModified() }
            val filesToDelete = sortedFiles.drop(MAX_FILES_PER_DAY)
            filesToDelete.forEach {
                if (it.delete()) {
                    Log.d(TAG, "删除了过多的当日备份文件: ${it.name}")
                }
            }
        }
    }

    private fun cleanUpOldFolders(baseBackupDir: File) {
        val folders = baseBackupDir.listFiles()?.filter { it.isDirectory } ?: return
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        
        // 只筛选符合 YYYYMMDD 命名格式的文件夹
        val validFolders = folders.filter { folder ->
            try {
                dateFormat.parse(folder.name) != null
                folder.name.length == 8
            } catch (e: Exception) {
                false
            }
        }

        val sortedFolders = validFolders.sortedByDescending { it.name }
        if (sortedFolders.size > MAX_DAYS_RETENTION) {
            val foldersToDelete = sortedFolders.drop(MAX_DAYS_RETENTION)
            foldersToDelete.forEach { folder ->
                if (folder.deleteRecursively()) {
                    Log.d(TAG, "删除了过期的历史备份文件夹: ${folder.name}")
                }
            }
        }
    }
}
