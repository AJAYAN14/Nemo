package com.jian.nemo.feature.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.text.format.Formatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.ui.util.AvatarCache
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * 存储与内存快照数据模型
 */
data class StorageMemorySnapshot(
    val appSizeBytes: Long = 0L,
    val databaseBytes: Long = 0L,
    val preferencesBytes: Long = 0L,
    val filesBytes: Long = 0L,
    val otherUserDataBytes: Long = 0L,
    val internalCacheBytes: Long = 0L,
    val externalCacheBytes: Long = 0L,
    val codeCacheBytes: Long = 0L,
    val totalPssBytes: Long = 0L,
    val dalvikPssBytes: Long = 0L,
    val nativePssBytes: Long = 0L,
    val otherPssBytes: Long = 0L,
    val jvmHeapUsedBytes: Long = 0L,
    val jvmHeapMaxBytes: Long = 0L,
    val deviceAvailableRamBytes: Long = 0L,
    val deviceTotalRamBytes: Long = 0L
) {
    val userDataBytes: Long
        get() = databaseBytes + preferencesBytes + filesBytes + otherUserDataBytes

    val totalCacheBytes: Long
        get() = internalCacheBytes + externalCacheBytes + codeCacheBytes

    val totalStorageBytes: Long
        get() = appSizeBytes + userDataBytes + totalCacheBytes

    val jvmHeapUsageFraction: Float
        get() = if (jvmHeapMaxBytes > 0L) {
            (jvmHeapUsedBytes.toFloat() / jvmHeapMaxBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}

/**
 * 存储与内存管理界面 UI 状态
 */
data class StorageManagementUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isClearingCache: Boolean = false,
    val snapshot: StorageMemorySnapshot = StorageMemorySnapshot(),
    val settingIconStyle: SettingIconStyle = SettingIconStyle.SOFT_TRANSLUCENT,
    val feedbackMessage: String? = null,
    val isFeedbackError: Boolean = false
)

/**
 * 存储与内存管理 ViewModel
 *
 * 负责统计应用本体体积、用户数据、缓存目录占用及当前进程实时内存（PSS / JVM Heap），
 * 并提供与系统「清除缓存」等价的安全清理能力（绝不触碰数据库、偏好设置与用户文件）。
 */
@HiltViewModel
class StorageManagementViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageManagementUiState())
    val uiState: StateFlow<StorageManagementUiState> = _uiState.asStateFlow()

    init {
        observeIconStyle()
        loadStats(showRefreshFeedback = false)
    }

    private fun observeIconStyle() {
        viewModelScope.launch {
            settingsRepository.settingIconStyleFlow.collect { style ->
                _uiState.update { it.copy(settingIconStyle = style) }
            }
        }
    }

    /**
     * 重新扫描应用存储空间与运行内存
     */
    fun refresh(showFeedback: Boolean = true) {
        if (_uiState.value.isRefreshing || _uiState.value.isClearingCache) return
        loadStats(showRefreshFeedback = showFeedback)
    }

    /**
     * 清除应用缓存目录与可重建的内存图片缓存，完整保留所有用户数据
     */
    fun clearCache() {
        if (_uiState.value.isClearingCache) return
        val beforeCacheBytes = _uiState.value.snapshot.totalCacheBytes

        viewModelScope.launch {
            _uiState.update { it.copy(isClearingCache = true) }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    clearCacheDirectoryContents(appContext.cacheDir)
                    appContext.externalCacheDirs?.filterNotNull()?.forEach { dir ->
                        clearCacheDirectoryContents(dir)
                    }
                    clearCacheDirectoryContents(appContext.codeCacheDir)
                    AvatarCache.clear()
                    AboutLogoCache.clear()
                    collectSnapshot()
                }
            }

            result.onSuccess { updatedSnapshot ->
                val freedBytes = (beforeCacheBytes - updatedSnapshot.totalCacheBytes).coerceAtLeast(0L)
                val message = if (freedBytes > 0L) {
                    appContext.getString(
                        R.string.settings_storage_toast_cleared_size,
                        formatSize(freedBytes)
                    )
                } else {
                    appContext.getString(R.string.settings_storage_toast_cleared_done)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isClearingCache = false,
                        snapshot = updatedSnapshot,
                        feedbackMessage = message,
                        isFeedbackError = false
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isClearingCache = false,
                        feedbackMessage = appContext.getString(R.string.settings_storage_toast_clear_failed),
                        isFeedbackError = true
                    )
                }
            }
        }
    }

    fun consumeFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, isFeedbackError = false) }
    }

    fun formatSize(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0L)
        return Formatter.formatFileSize(appContext, safeBytes)
    }

    private fun loadStats(showRefreshFeedback: Boolean) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isRefreshing = true,
                    isLoading = it.isLoading && it.snapshot.totalStorageBytes == 0L
                )
            }
            val snapshot = withContext(Dispatchers.IO) { collectSnapshot() }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    snapshot = snapshot,
                    feedbackMessage = if (showRefreshFeedback) {
                        appContext.getString(R.string.settings_storage_toast_refreshed)
                    } else {
                        it.feedbackMessage
                    },
                    isFeedbackError = if (showRefreshFeedback) false else it.isFeedbackError
                )
            }
        }
    }

    private fun collectSnapshot(): StorageMemorySnapshot {
        val appSize = calculateAppSizeBytes()
        val cacheSizes = calculateCacheSizeBreakdown()
        val userDataSizes = calculateUserDataBreakdown(cacheSizes.visitedCanonicalPaths)
        val memoryStats = collectMemoryStats()

        return StorageMemorySnapshot(
            appSizeBytes = appSize,
            databaseBytes = userDataSizes.databaseBytes,
            preferencesBytes = userDataSizes.preferencesBytes,
            filesBytes = userDataSizes.filesBytes,
            otherUserDataBytes = userDataSizes.otherBytes,
            internalCacheBytes = cacheSizes.internalCacheBytes,
            externalCacheBytes = cacheSizes.externalCacheBytes,
            codeCacheBytes = cacheSizes.codeCacheBytes,
            totalPssBytes = memoryStats.totalPssBytes,
            dalvikPssBytes = memoryStats.dalvikPssBytes,
            nativePssBytes = memoryStats.nativePssBytes,
            otherPssBytes = memoryStats.otherPssBytes,
            jvmHeapUsedBytes = memoryStats.jvmHeapUsedBytes,
            jvmHeapMaxBytes = memoryStats.jvmHeapMaxBytes,
            deviceAvailableRamBytes = memoryStats.deviceAvailableRamBytes,
            deviceTotalRamBytes = memoryStats.deviceTotalRamBytes
        )
    }

    private fun calculateAppSizeBytes(): Long {
        val appInfo = appContext.applicationInfo
        val countedPaths = mutableSetOf<String>()
        var total = 0L

        fun addPathSize(path: String?) {
            if (path.isNullOrBlank()) return
            val file = File(path)
            if (!file.exists()) return
            total += calculateDirectoryOrFileSize(file, visitedPaths = countedPaths)
        }

        addPathSize(appInfo.sourceDir)
        appInfo.splitSourceDirs?.forEach { addPathSize(it) }
        addPathSize(appInfo.nativeLibraryDir)

        val apkParent = appInfo.sourceDir?.let { File(it).parentFile }
        if (apkParent != null) {
            addPathSize(File(apkParent, "oat").absolutePath)
        }

        return total
    }

    private data class CacheSizeBreakdown(
        val internalCacheBytes: Long,
        val externalCacheBytes: Long,
        val codeCacheBytes: Long,
        val visitedCanonicalPaths: Set<String>
    )

    private fun calculateCacheSizeBreakdown(): CacheSizeBreakdown {
        val visited = mutableSetOf<String>()

        val internalCache = appContext.cacheDir?.let { dir ->
            calculateDirectoryOrFileSize(dir, visited)
        } ?: 0L

        var externalCache = 0L
        appContext.externalCacheDirs?.filterNotNull()?.forEach { dir ->
            externalCache += calculateDirectoryOrFileSize(dir, visited)
        }

        val codeCache = appContext.codeCacheDir?.let { dir ->
            calculateDirectoryOrFileSize(dir, visited)
        } ?: 0L

        return CacheSizeBreakdown(
            internalCacheBytes = internalCache,
            externalCacheBytes = externalCache,
            codeCacheBytes = codeCache,
            visitedCanonicalPaths = visited
        )
    }

    private data class UserDataBreakdown(
        val databaseBytes: Long,
        val preferencesBytes: Long,
        val filesBytes: Long,
        val otherBytes: Long
    )

    private fun calculateUserDataBreakdown(excludedCachePaths: Set<String>): UserDataBreakdown {
        val dataDir = File(appContext.applicationInfo.dataDir)
        val visited = excludedCachePaths.toMutableSet()

        // 排除 lib 软链接，避免与原生库重复统计
        File(dataDir, "lib").takeIf { it.exists() }?.let { libDir ->
            visited.add(safeCanonicalPath(libDir))
            visited.add(libDir.absolutePath)
        }

        val databasesDir = File(dataDir, "databases")
        val databaseBytes = calculateDirectoryOrFileSize(databasesDir, visited)

        val sharedPrefsDir = File(dataDir, "shared_prefs")
        val datastoreDir = File(appContext.filesDir, "datastore")
        val preferencesBytes =
            calculateDirectoryOrFileSize(sharedPrefsDir, visited) +
                calculateDirectoryOrFileSize(datastoreDir, visited)

        var filesBytes = calculateDirectoryOrFileSize(appContext.filesDir, visited)
        filesBytes += calculateDirectoryOrFileSize(appContext.noBackupFilesDir, visited)
        appContext.getExternalFilesDirs(null)?.filterNotNull()?.forEach { dir ->
            filesBytes += calculateDirectoryOrFileSize(dir, visited)
        }

        var otherBytes = 0L
        dataDir.listFiles()?.forEach { child ->
            // 跳过指向外部目录的符号链接（如 lib）以及已统计过的目录
            if (isSymlink(child)) return@forEach
            val canonical = safeCanonicalPath(child)
            if (canonical !in visited) {
                otherBytes += calculateDirectoryOrFileSize(child, visited)
            }
        }

        return UserDataBreakdown(
            databaseBytes = databaseBytes,
            preferencesBytes = preferencesBytes,
            filesBytes = filesBytes,
            otherBytes = otherBytes
        )
    }

    private data class MemoryStats(
        val totalPssBytes: Long,
        val dalvikPssBytes: Long,
        val nativePssBytes: Long,
        val otherPssBytes: Long,
        val jvmHeapUsedBytes: Long,
        val jvmHeapMaxBytes: Long,
        val deviceAvailableRamBytes: Long,
        val deviceTotalRamBytes: Long
    )

    private fun collectMemoryStats(): MemoryStats {
        val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val processMemoryInfo = activityManager
            ?.getProcessMemoryInfo(intArrayOf(Process.myPid()))
            ?.firstOrNull()

        val totalPssKb = (processMemoryInfo?.totalPss ?: 0).coerceAtLeast(0).toLong()
        val dalvikPssKb = (processMemoryInfo?.dalvikPss ?: 0).coerceAtLeast(0).toLong()
        val nativePssKb = (processMemoryInfo?.nativePss ?: 0).coerceAtLeast(0).toLong()
        val otherPssKb = (totalPssKb - dalvikPssKb - nativePssKb).coerceAtLeast(0L)

        val runtime = Runtime.getRuntime()
        val jvmUsed = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L)
        val jvmMax = runtime.maxMemory().coerceAtLeast(jvmUsed)

        val sysMemInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(sysMemInfo)

        return MemoryStats(
            totalPssBytes = totalPssKb * 1024L,
            dalvikPssBytes = dalvikPssKb * 1024L,
            nativePssBytes = nativePssKb * 1024L,
            otherPssBytes = otherPssKb * 1024L,
            jvmHeapUsedBytes = jvmUsed,
            jvmHeapMaxBytes = jvmMax,
            deviceAvailableRamBytes = sysMemInfo.availMem.coerceAtLeast(0L),
            deviceTotalRamBytes = sysMemInfo.totalMem.coerceAtLeast(0L)
        )
    }

    private fun calculateDirectoryOrFileSize(
        target: File?,
        visitedPaths: MutableSet<String>
    ): Long {
        if (target == null || !target.exists()) return 0L
        val canonical = safeCanonicalPath(target)
        if (target.isFile) {
            return if (visitedPaths.add(canonical)) target.length().coerceAtLeast(0L) else 0L
        }
        if (!target.isDirectory) return 0L
        if (!visitedPaths.add(canonical)) return 0L

        var size = 0L
        val children = target.listFiles() ?: return 0L
        for (child in children) {
            if (isSymlink(child)) continue
            if (child.isDirectory) {
                size += calculateDirectoryOrFileSize(child, visitedPaths)
            } else if (child.isFile) {
                val childCanonical = safeCanonicalPath(child)
                if (visitedPaths.add(childCanonical)) {
                    size += child.length().coerceAtLeast(0L)
                }
            }
        }
        return size
    }

    private fun clearCacheDirectoryContents(directory: File?) {
        if (directory == null || !directory.exists() || !directory.isDirectory) return
        val children = directory.listFiles() ?: return
        for (child in children) {
            deleteRecursivelySafely(child)
        }
    }

    private fun deleteRecursivelySafely(file: File) {
        if (isSymlink(file)) return
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursivelySafely(child)
            }
        }
        runCatching { file.delete() }
    }

    private fun safeCanonicalPath(file: File): String {
        return runCatching { file.canonicalPath }.getOrElse { file.absolutePath }
    }

    private fun isSymlink(file: File): Boolean {
        return runCatching {
            val parent = file.parentFile ?: return false
            val canonicalInParent = File(parent.canonicalFile, file.name)
            canonicalInParent.canonicalFile != canonicalInParent.absoluteFile
        }.getOrDefault(false)
    }
}
