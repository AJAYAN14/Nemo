package com.jian.nemo.core.data.manager

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端备份信息
 */
data class BackupInfo(
    val fileName: String,
    val sizeBytes: Long,
    val createdAt: Long // epoch millis
)

/**
 * 云端备份管理器
 *
 * 负责与 Supabase Storage 交互，实现备份的上传、列表、下载和自动清理。
 */
@Singleton
class CloudBackupManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val dataExportManager: DataExportManager,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "CloudBackupManager"
        private const val BUCKET_NAME = "backups"
        private const val UPLOAD_TIMEOUT_MS = 30_000L
        private const val DOWNLOAD_TIMEOUT_MS = 30_000L
        private const val MAX_DOWNLOAD_SIZE = 50 * 1024 * 1024L // 50MB
    }

    private val sharedPreferences by lazy {
        context.getSharedPreferences("cloud_backup_prefs", Context.MODE_PRIVATE)
    }

    /**
     * 获取当前登录用户的 ID
     * @throws IllegalStateException 如果用户未登录
     */
    private fun requireUserId(): String {
        return supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("请先登录账号")
    }

    /**
     * 上传备份到云端
     *
     * 1. 在本地生成压缩备份文件
     * 2. 上传到 Supabase Storage
     * 3. 自动清理多余的旧备份
     *
     * @return 上传的文件名
     */
    suspend fun uploadBackup(): String = withContext(Dispatchers.IO) {
        val userId = requireUserId()
        val epochSeconds = System.currentTimeMillis() / 1000
        val fileName = "backup_$epochSeconds.json.gz"
        val remotePath = "$userId/$fileName"

        val tempFile = File(context.cacheDir, "cloud_backup_temp_$epochSeconds.json.gz")
        try {
            Log.d(TAG, "开始生成备份文件...")
            // 使用真实用户 ID 生成备份（避免使用硬编码的 default_user）
            dataExportManager.exportDataToFile(userId, tempFile, isCompressed = true)

            Log.d(TAG, "上传备份到云端: $remotePath (${tempFile.length()} bytes)")
            withTimeout(UPLOAD_TIMEOUT_MS) {
                val bytes = tempFile.readBytes()
                supabase.storage.from(BUCKET_NAME).upload(remotePath, bytes) { upsert = true }
            }

            Log.d(TAG, "上传成功，后端已自动清理旧备份...")

            fileName
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    /**
     * 列出云端所有备份
     *
     * @return 按创建时间倒序排列的备份列表
     */
    suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val userId = requireUserId()

        try {
            val items = supabase.storage.from(BUCKET_NAME).list("$userId/")

            items.mapNotNull { item ->
                val name = item.name ?: return@mapNotNull null
                if (!name.startsWith("backup_") || !name.endsWith(".json.gz")) return@mapNotNull null

                // 从文件名中提取时间戳
                val timestampStr = name.removePrefix("backup_").removeSuffix(".json.gz")
                val createdAt = timestampStr.toLongOrNull()?.times(1000) ?: 0L

                BackupInfo(
                    fileName = name,
                    sizeBytes = item.metadata?.get("size")?.toString()?.toLongOrNull() ?: 0L,
                    createdAt = createdAt
                )
            }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e(TAG, "获取备份列表失败", e)
            throw e
        }
    }

    /**
     * 从云端下载备份并恢复
     *
     * @param fileName 云端备份文件名（不含用户文件夹前缀）
     * @param strategy 导入策略（合并 / 覆盖）
     * @return 导入结果消息
     */
    suspend fun downloadAndRestore(
        fileName: String,
        strategy: ImportStrategy
    ): String = withContext(Dispatchers.IO) {
        val userId = requireUserId()
        val remotePath = "$userId/$fileName"

        Log.d(TAG, "开始从云端下载: $remotePath")

        val bytes = withTimeout(DOWNLOAD_TIMEOUT_MS) {
            supabase.storage.from(BUCKET_NAME).downloadAuthenticated(remotePath)
        }

        // 前置大小检查
        if (bytes.size > MAX_DOWNLOAD_SIZE) {
            throw IllegalStateException("备份文件异常过大 (${bytes.size / 1024 / 1024}MB)，已拒绝导入")
        }

        Log.d(TAG, "下载完成: ${bytes.size} bytes，开始导入...")

        // 云端备份内容为 Base64(GZIP(JSON)) 文本，直接转为 String 传入导入引擎
        val content = bytes.toString(Charsets.UTF_8)
        val result = dataExportManager.importData(content, strategy)
        result.message
    }

    /**
     * 仅从云端下载备份内容，不执行导入
     * @return 下载的字符串内容
     */
    suspend fun downloadBackup(fileName: String): String = withContext(Dispatchers.IO) {
        val userId = requireUserId()
        val remotePath = "$userId/$fileName"

        Log.d(TAG, "开始从云端下载用于预览: $remotePath")

        val bytes = withTimeout(DOWNLOAD_TIMEOUT_MS) {
            supabase.storage.from(BUCKET_NAME).downloadAuthenticated(remotePath)
        }

        if (bytes.size > MAX_DOWNLOAD_SIZE) {
            throw IllegalStateException("备份文件异常过大 (${bytes.size / 1024 / 1024}MB)，已拒绝预览")
        }

        bytes.toString(Charsets.UTF_8)
    }

    /**
     * 预览云端备份恢复（dry-run）
     */
    suspend fun previewRestore(
        fileName: String,
        strategy: ImportStrategy
    ): com.jian.nemo.core.data.manager.ImportPreview {
        val content = downloadBackup(fileName)
        return dataExportManager.previewImport(content, strategy)
    }

    /**
     * 执行云端备份恢复
     */
    suspend fun executeRestore(content: String, strategy: ImportStrategy): String = withContext(Dispatchers.IO) {
        val result = dataExportManager.importData(content, strategy)
        result.message
    }

    /**
     * 尝试自动备份（防抖）
     * 距离上次自动备份不足 [intervalHours] 小时则跳过。
     */
    suspend fun tryAutoBackup(intervalHours: Int = 2) {
        val lastBackupTime = sharedPreferences.getLong("last_auto_backup_time", 0L)
        val now = System.currentTimeMillis()
        val intervalMillis = intervalHours * 60 * 60 * 1000L

        if (now - lastBackupTime >= intervalMillis) {
            try {
                // 检查是否登录
                if (supabase.auth.currentUserOrNull() == null) {
                    Log.d(TAG, "tryAutoBackup: 用户未登录，跳过自动备份")
                    return
                }

                Log.d(TAG, "tryAutoBackup: 触发自动备份")
                uploadBackup()
                sharedPreferences.edit().putLong("last_auto_backup_time", now).apply()
            } catch (e: Exception) {
                Log.e(TAG, "tryAutoBackup: 自动备份失败", e)
            }
        } else {
            val hoursLeft = (intervalMillis - (now - lastBackupTime)) / (60 * 60 * 1000f)
            Log.d(TAG, "tryAutoBackup: 距离上次备份不足 ${intervalHours} 小时，跳过 (还需 ${String.format("%.1f", hoursLeft)} 小时)")
        }
    }
}
