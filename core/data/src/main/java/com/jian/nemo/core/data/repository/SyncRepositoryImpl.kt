package com.jian.nemo.core.data.repository

import android.util.Log
import com.jian.nemo.core.data.local.NemoDatabase
import com.jian.nemo.core.domain.repository.SyncRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.data.manager.SupabaseSyncManager
import com.jian.nemo.core.domain.model.SyncProgress
import com.jian.nemo.core.domain.model.sync.SyncMode
import com.jian.nemo.core.domain.model.sync.SyncResult
import com.jian.nemo.core.domain.model.sync.SyncErrorType
import com.jian.nemo.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val syncManager: SupabaseSyncManager,
    private val database: NemoDatabase,
    private val settingsRepository: SettingsRepository,
    @ApplicationScope private val externalScope: CoroutineScope
) : SyncRepository {

    private val _globalSyncProgress = MutableStateFlow<SyncProgress>(SyncProgress.Idle)
    override val globalSyncProgress = _globalSyncProgress.asStateFlow()

    override fun startBackgroundSync(userId: String, force: Boolean, mode: SyncMode) {
        externalScope.launch {
            performSync(userId, force, mode).collect { progress ->
                _globalSyncProgress.value = progress
            }
        }
    }

    override suspend fun performSync(
        userId: String,
        force: Boolean,
        mode: SyncMode
    ): Flow<SyncProgress> = syncManager.performSync(userId, force, mode)

    override suspend fun performRestore(userId: String): Flow<SyncProgress> =
        syncManager.performRestore(userId)

    override suspend fun checkAndRestoreCloudData(
        userId: String,
        force: Boolean,
        mode: SyncMode
    ): SyncResult {
        Log.d("SyncRepository", "请求检查并恢复云端数据: User $userId, force=$force, mode=$mode")
        return try {
            val progress = performSync(userId, force, mode).lastOrNull() ?: SyncProgress.AlreadyRunning
            
            when (progress) {
                is SyncProgress.Completed -> SyncResult(
                    success = true,
                    message = "同步成功",
                    syncReport = progress.report
                )
                is SyncProgress.AlreadyRunning -> SyncResult(
                    success = true,
                    message = "同步已在运行中"
                )
                is SyncProgress.Failed -> SyncResult(
                    success = false,
                    message = progress.error,
                    errorType = SyncErrorType.UNKNOWN
                )
                else -> SyncResult(
                    success = false,
                    message = "同步状态异常: $progress",
                    errorType = SyncErrorType.UNKNOWN
                )
            }
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "解析同步结果失败: ${e.message}",
                errorType = SyncErrorType.UNKNOWN
            )
        }
    }

    override suspend fun deleteAllCloudData(userId: String): Boolean {
        Log.d("SyncRepository", "请求清空云端所有同步数据: User $userId")
        return syncManager.deleteAllCloudData(userId)
    }

    override suspend fun hasUnsyncedChanges(sinceTimestamp: Long): Boolean {
        return withContext(Dispatchers.IO) {
            val wordsChanged = database.wordDao().countModifiedSince(sinceTimestamp)
            if (wordsChanged > 0) return@withContext true

            val grammarsChanged = database.grammarDao().countModifiedSince(sinceTimestamp)
            if (grammarsChanged > 0) return@withContext true

            val wrongAnswersChanged = database.wrongAnswerDao().countModifiedSince(sinceTimestamp)
            if (wrongAnswersChanged > 0) return@withContext true

            val grammarWrongAnswersChanged = database.grammarWrongAnswerDao().countModifiedSince(sinceTimestamp)
            if (grammarWrongAnswersChanged > 0) return@withContext true

            val testRecordsChanged = database.testRecordDao().countModifiedSince(sinceTimestamp)
            if (testRecordsChanged > 0) return@withContext true

            val studyRecordsChanged = database.studyRecordDao().countModifiedSince(sinceTimestamp)
            if (studyRecordsChanged > 0) return@withContext true

            val favoritesChanged = database.favoriteQuestionDao().countModifiedSince(sinceTimestamp)
            if (favoritesChanged > 0) return@withContext true

            val settingsModifiedTime = settingsRepository.lastSettingsModifiedTimeFlow.first()
            if (settingsModifiedTime > sinceTimestamp) return@withContext true

            false
        }
    }
}
