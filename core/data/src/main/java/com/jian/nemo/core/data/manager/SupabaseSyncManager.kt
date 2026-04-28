package com.jian.nemo.core.data.manager

import android.util.Log
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.data.local.dao.*
import com.jian.nemo.core.data.local.entity.*
import com.jian.nemo.core.data.local.NemoDatabase
import com.jian.nemo.core.domain.model.SyncProgress
import com.jian.nemo.core.domain.model.SyncReport
import com.jian.nemo.core.domain.model.SyncStats
import com.jian.nemo.core.domain.model.WordDto
import com.jian.nemo.core.domain.model.DictionarySyncResult
import com.jian.nemo.core.domain.model.GrammarDto
import com.jian.nemo.core.domain.repository.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import com.jian.nemo.core.data.util.DataSeedService
import com.jian.nemo.core.domain.repository.ContentRepository
import com.jian.nemo.core.domain.repository.ContentUpdateApplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.jian.nemo.core.domain.model.sync.SyncMode
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder

@Serializable
data class SyncMetaDto(
    @SerialName("min_compatible_version") val minVersion: Int
)

@Singleton
class SupabaseSyncManager @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val wordDao: WordDao,
    private val grammarDao: GrammarDao,
    private val wordStudyStateDao: WordStudyStateDao,
    private val grammarStudyStateDao: GrammarStudyStateDao,
    private val studyRecordDao: StudyRecordDao,
    private val testRecordDao: TestRecordDao,
    private val wrongAnswerDao: WrongAnswerDao,
    private val grammarWrongAnswerDao: GrammarWrongAnswerDao,
    private val favoriteQuestionDao: FavoriteQuestionDao,
    private val settingsRepository: SettingsRepository,
    private val database: NemoDatabase,
    private val syncMetadata: com.jian.nemo.core.data.model.sync.SyncMetadata,
    private val dataSeedService: DataSeedService,
    private val contentRepository: ContentRepository,
    private val contentUpdateApplier: ContentUpdateApplier
) {
    private val syncMutex = kotlinx.coroutines.sync.Mutex()
    suspend fun performDictionarySync(
        force: Boolean = false,
        forceIncremental: Boolean = false
    ): DictionarySyncResult {
        return performDictionarySyncInternal(force, forceIncremental)
    }

    private suspend fun performDictionarySyncInternal(
        force: Boolean = false,
        forceIncremental: Boolean = false
    ): DictionarySyncResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "开始检查字典同步...")
        try {
            val remoteVersion = contentRepository.getRemoteContentVersion()
            val lastVersion = settingsRepository.getLastContentVersion()
            val lastSyncTimestamp = if (force) 0L else settingsRepository.getLastDictionarySyncTimestamp()

            // 如果版本不一致，或者本地数据库为空，则触发同步
            val wordCount = wordDao.getWordCount()
            val grammarCount = grammarDao.getGrammarCount()
            val isDatabaseEmpty = wordCount == 0 || grammarCount == 0

            Log.i(TAG, "词库同步状态检查: RemoteV=$remoteVersion, LocalV=$lastVersion, LastSyncTime=$lastSyncTimestamp, WordCount=$wordCount, GrammarCount=$grammarCount, isEmpty=$isDatabaseEmpty, force=$force, forceIncremental=$forceIncremental")

            if (force || forceIncremental || (remoteVersion != null && (remoteVersion > lastVersion || isDatabaseEmpty || lastSyncTimestamp == 0L))) {
                val isFullSync = force || isDatabaseEmpty || lastSyncTimestamp == 0L
                Log.i(TAG, ">>> 开始同步词库 (${if (isFullSync) "全量模式" else "增量模式"}): force=$force, forceIncremental=$forceIncremental, V$lastVersion -> V$remoteVersion")

                if (force) {
                    Log.w(TAG, "强制重置模式：正在清空本地词库数据...")
                    wordDao.deleteAll()
                    grammarDao.deleteAll()
                }

                // 执行同步拉取
                val (allWords: List<WordDto>, allGrammars: List<GrammarDto>) = coroutineScope {
                    if (isFullSync) {
                        val w = async { contentRepository.fetchAllRemoteWords() }
                        val g = async { contentRepository.fetchAllRemoteGrammars() }
                        w.await() to g.await()
                    } else {
                        // 增量模式：使用时间戳拉取
                        val timestampStr = DateTimeUtils.formatIso8601(java.util.Date(lastSyncTimestamp))
                        val w = async { contentRepository.fetchWordsModifiedSince(timestampStr) }
                        val g = async { contentRepository.fetchGrammarsModifiedSince(timestampStr) }
                        w.await() to g.await()
                    }
                }

                Log.i(TAG, "下载完成: ${allWords.size} 单词, ${allGrammars.size} 语法")

                // 应用变更
                if (allWords.isNotEmpty()) {
                    contentUpdateApplier.applyAllWords(allWords)
                }
                if (allGrammars.isNotEmpty()) {
                    contentUpdateApplier.applyAllGrammars(allGrammars)
                }

                // 计算并更新新的同步锚点时间戳 (取结果中最大的 updated_at)
                val maxWordTimestamp = allWords.mapNotNull { DateTimeUtils.parseIso8601(it.updatedAt)?.time }.maxOrNull() ?: 0L
                val maxGrammarTimestamp = allGrammars.mapNotNull { DateTimeUtils.parseIso8601(it.updatedAt)?.time }.maxOrNull() ?: 0L
                
                // 如果是全量同步且数据中没有时间戳（如从 Storage 下载的初始 JSON），
                // 则使用当前时间作为锚点，确保下次能够正常进行增量同步。
                var newSyncTimestamp = maxOf(lastSyncTimestamp, maxOf(maxWordTimestamp, maxGrammarTimestamp))
                if (isFullSync && newSyncTimestamp == 0L) {
                    newSyncTimestamp = System.currentTimeMillis()
                }

                if (newSyncTimestamp > lastSyncTimestamp) {
                    settingsRepository.setLastDictionarySyncTimestamp(newSyncTimestamp)
                    Log.d(TAG, "更新词库同步时间戳锚点: $newSyncTimestamp")
                }

                // 更新本地版本号
                remoteVersion?.let {
                    settingsRepository.setLastContentVersion(it)
                }
                Log.i(TAG, "词库同步任务结束: 已成功更新至 V$remoteVersion")
                
                return@withContext DictionarySyncResult(
                    updatedWords = allWords.size,
                    updatedGrammars = allGrammars.size,
                    isFullSync = isFullSync,
                    localVersion = lastVersion,
                    remoteVersion = remoteVersion ?: lastVersion
                )
            } else {
                Log.i(TAG, "词库检查结果: 无需更新 (本地 V$lastVersion, 远程 V$remoteVersion)")
                return@withContext DictionarySyncResult(
                    localVersion = lastVersion,
                    remoteVersion = remoteVersion ?: lastVersion
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "字典同步失败: ${e.message}", e)
            return@withContext DictionarySyncResult()
        }
    }

    companion object {
        private const val TAG = "SupabaseSyncManager"
        private const val TABLE_WORD_STATES = "user_word_states"
        private const val TABLE_GRAMMAR_STATES = "user_grammar_states"
        private const val TABLE_STUDY_RECORDS = "user_study_records"
        private const val TABLE_TEST_RECORDS = "user_test_records"
        private const val TABLE_WRONG_ANSWERS = "user_wrong_answers"
        private const val TABLE_GRAMMAR_WRONG_ANSWERS = "user_grammar_wrong_answers"
        private const val TABLE_FAVORITE_QUESTIONS = "favorite_questions"
        private const val TABLE_USER_SETTINGS = "user_settings"
        private const val TABLE_SYNC_META = "sync_meta"
        private const val BATCH_SIZE = 200
        private const val SYNC_SCHEMA_VERSION = 1
    }


    /**
     * 执行同步操作
     * @param userId 用户 ID
     * @param force 是否强制全量同步（忽略上次同步时间戳，检查云端所有变更）
     * @param mode 同步模式
     */
    suspend fun performSync(
        userId: String,
        force: Boolean = false,
        mode: SyncMode = SyncMode.TWO_WAY
    ): Flow<SyncProgress> = flow {
        if (!syncMutex.tryLock()) {
            Log.d(TAG, "同步已在运行中，跳过本次触发")
            emit(SyncProgress.AlreadyRunning)
            return@flow
        }

        try {
            Log.d(TAG, "开始执行同步: User $userId, mode=$mode, force=$force")
            
            // 0. 核心依赖检查：确保本地词库已初始化（优先尝试网络同步，失败则使用本地兜底）
            emit(SyncProgress.Running("正在检查词库更新...", 0, 0))
            performDictionarySync()
            
            emit(SyncProgress.Running("正在准备本地库...", 0, 0))
            dataSeedService.ensureDataSeeded()

            // 0.1 时间校验 (RPC)
            try {
                val serverTime = supabaseClient.postgrest.rpc("get_server_time").decodeAs<Long>()
                syncMetadata.updateServerTimeOffset(serverTime)
            } catch (e: Exception) {
                Log.w(TAG, "服务器时间校准失败，将使用本地时间基准", e)
            }

            emit(SyncProgress.Running("准备开始同步...", 0, 0))

            // 1. 获取上次同步时间
            var lastSyncTime = if (force) 0L else settingsRepository.lastSyncTimeFlow.first()
            var isAutoRestoring = false
            
            // [Optimization] Check if local database is empty even if lastSyncTime > 0
            // This happens when Android Auto Backup restores preferences but not the Room database.
            if (!force && lastSyncTime > 0) {
                val localCount = wordStudyStateDao.getAllSync().size
                if (localCount == 0) {
                    Log.i(TAG, "检测到本地核心数据为空但同步时间戳 > 0，疑为重装恢复，强制开启全量拉取模式")
                    lastSyncTime = 0L
                    isAutoRestoring = true
                }
            }

            if (isAutoRestoring) {
                emit(SyncProgress.Running("正在恢复云端数据...", 0, 0))
                kotlinx.coroutines.delay(200) // 给 UI 一些反应时间
            }

            val startTime = DateTimeUtils.getCurrentCompensatedMillis()
            val queryTime = if (lastSyncTime > 0) lastSyncTime - 60_000 else 0L

            // 2. 协议版本检查
            try {
                val remoteVersion = getRemoteMinVersion()
                if (remoteVersion > SYNC_SCHEMA_VERSION) {
                    emit(SyncProgress.Failed("APP 版本过低 ($SYNC_SCHEMA_VERSION < $remoteVersion)，无法兼容云端数据，请先升级应用"))
                    return@flow
                }
            } catch (e: Exception) {
                Log.e(TAG, "版本检查时出错", e)
            }

            // 3. PULL Phase: 收集所有云端变更到内存
            // ------------------------------------------------------
            emit(SyncProgress.Running("正在拉取云端数据...", 0, 0))

            val wordPull = pullWords(userId, queryTime, false)
            emit(SyncProgress.Running("同步单词...", wordPull.pulledCount, 0))

            val grammarPull = pullGrammars(userId, queryTime, false)
            emit(SyncProgress.Running("同步语法...", grammarPull.pulledCount, 0))

            val studyPull = pullStudyRecords(userId, queryTime, false)
            emit(SyncProgress.Running("同步学习记录...", studyPull.pulledCount, 0))

            val testPull = pullTestRecords(userId, queryTime, false)
            emit(SyncProgress.Running("同步测试记录...", testPull.pulledCount, 0))

            val wordWrongPull = pullWrongAnswers(userId, queryTime, false)
            emit(SyncProgress.Running("同步单词错题...", wordWrongPull.pulledCount, 0))

            val grammarWrongPull = pullGrammarWrongAnswers(userId, queryTime, false)
            emit(SyncProgress.Running("同步语法错题...", grammarWrongPull.pulledCount, 0))

            val favoritePull = pullFavoriteQuestions(userId, queryTime, false)
            emit(SyncProgress.Running("同步收藏题目...", favoritePull.pulledCount, 0))

            val settingsPullCount = pullSettings(userId)
            emit(SyncProgress.Running("同步应用配置...", settingsPullCount, 0))

            // 4. TRANSACTION Phase: 统一写入本地数据库
            // ------------------------------------------------------
            emit(SyncProgress.Running("正在写入本地数据库...", 0, 0))

            database.withTransaction {
                if (wordPull.toUpsert.isNotEmpty()) wordStudyStateDao.insertAll(wordPull.toUpsert)
                if (grammarPull.toUpsert.isNotEmpty()) grammarStudyStateDao.insertAll(grammarPull.toUpsert)
                if (studyPull.toUpsert.isNotEmpty()) studyRecordDao.insertAll(studyPull.toUpsert)
                if (testPull.toUpsert.isNotEmpty()) testRecordDao.insertAll(testPull.toUpsert)
                if (wordWrongPull.toUpsert.isNotEmpty()) wrongAnswerDao.insertAll(wordWrongPull.toUpsert)
                if (grammarWrongPull.toUpsert.isNotEmpty()) grammarWrongAnswerDao.insertAll(grammarWrongPull.toUpsert)
                if (favoritePull.toUpsert.isNotEmpty()) favoriteQuestionDao.upsertAll(favoritePull.toUpsert)
            }
            // Settings applied inside pullSettings

            // 5. PUSH Phase: 推送本地变更到云端
            // ------------------------------------------------------
            var pushedCount = 0
            if (mode != SyncMode.PULL_ONLY) {
                emit(SyncProgress.Running("正在上传本地变更...", 0, 0))

                pushedCount += pushWords(userId, queryTime, wordPull.acceptedIds)
                pushedCount += pushGrammars(userId, queryTime, grammarPull.acceptedIds)
                pushedCount += pushStudyRecords(userId, queryTime, studyPull.acceptedIds)
                pushedCount += pushTestRecords(userId, queryTime, testPull.acceptedIds)
                pushedCount += pushWrongAnswers(userId, queryTime, wordWrongPull.acceptedIds)
                pushedCount += pushGrammarWrongAnswers(userId, queryTime, grammarWrongPull.acceptedIds)
                pushedCount += pushFavoriteQuestions(userId, queryTime, favoritePull.acceptedIds)
                pushedCount += pushSettings(userId)
            }

            // 6. Finalize
            // ------------------------------------------------------
            settingsRepository.setLastSyncTime(startTime)
            settingsRepository.setLastSyncSuccess(true)
            settingsRepository.setLastSyncError("")

            val report = SyncReport(
                timestamp = startTime,
                syncVersion = 1,
                stats = SyncStats(
                    wordCount = wordPull.pulledCount,
                    grammarCount = grammarPull.pulledCount,
                    updatedItems = pushedCount,
                    addedItems = wordPull.pulledCount + grammarPull.pulledCount + studyPull.pulledCount + testPull.pulledCount + wordWrongPull.pulledCount + grammarWrongPull.pulledCount + favoritePull.pulledCount,
                    wrongAnswerCount = wordWrongPull.pulledCount + grammarWrongPull.pulledCount,
                    testRecordCount = testPull.pulledCount,
                    favoriteQuestionCount = favoritePull.pulledCount
                )
            )
            emit(SyncProgress.Completed(report))

        } catch (e: Exception) {
            Log.e(TAG, "同步过程发生严重错误", e)
            settingsRepository.setLastSyncSuccess(false)
            settingsRepository.setLastSyncError(e.message ?: "Unknown error")
            emit(SyncProgress.Failed("Sync failed: ${e.message}"))
        } finally {
            syncMutex.unlock()
        }
    }



    /**
     * 执行全量镜像恢复 (分批 + 断点续传)
     */
    suspend fun performRestore(userId: String): Flow<SyncProgress> = flow {
        if (!syncMutex.tryLock()) {
            Log.d(TAG, "恢复已在运行中，跳过本次触发")
            emit(SyncProgress.AlreadyRunning)
            return@flow
        }

        try {
            Log.d(TAG, "开始执行镜像恢复: User $userId")
            
            // 确保本地库就绪
            emit(SyncProgress.Running("正在准备本地库...", 0, 0))
            dataSeedService.ensureDataSeeded()
            
            settingsRepository.setIsRestoring(true)

            // 0. 检查断点
            val checkpoint = settingsRepository.getRestoreCheckpoint()
            val isResuming = checkpoint != null
            val startTableRaw = checkpoint?.first ?: ""
            val startOffset = checkpoint?.second ?: 0

            // 表名列表（有序）
            val tables = listOf(
                TABLE_WORD_STATES,
                TABLE_GRAMMAR_STATES,
                TABLE_STUDY_RECORDS,
                TABLE_TEST_RECORDS,
                TABLE_WRONG_ANSWERS,
                TABLE_GRAMMAR_WRONG_ANSWERS,
                TABLE_FAVORITE_QUESTIONS,
                TABLE_USER_SETTINGS
            )

            // 如果不是断点续传，先清空本地数据
            if (!isResuming) {
                emit(SyncProgress.Running("正在清理本地数据...", 0, 0))
                clearLocalUserData()
            } else {
                emit(SyncProgress.Running("检测到断点，从 $startTableRaw 偏移量 $startOffset 继续...", 0, 0))
            }

            // 确定开始的表索引
            val startTableIndex = if (isResuming && startTableRaw.isNotEmpty()) {
                tables.indexOf(startTableRaw).takeIf { it >= 0 } ?: 0
            } else {
                0
            }

            // 1. 逐表处理
            for (i in startTableIndex until tables.size) {
                val tableName = tables[i]
                // 如果是当前断点表，使用断点 offset；否则从 0 开始
                var currentOffset = if (i == startTableIndex) startOffset else 0
                val pageSize = 1000

                emit(SyncProgress.Running("正在恢复 $tableName...", currentOffset, 0))

                while (true) {
                    val batchCount = processBatch(tableName, userId, currentOffset, pageSize)

                    if (batchCount == 0) break

                    currentOffset += batchCount
                    // 记录断点
                    settingsRepository.setRestoreCheckpoint(tableName, currentOffset)
                    emit(SyncProgress.Running("正在恢复 $tableName...", currentOffset, 0))

                    if (batchCount < pageSize) break
                }
            }

            // 2. 完成
            settingsRepository.setLastSyncTime(DateTimeUtils.getCurrentCompensatedMillis())
            settingsRepository.setLastSyncSuccess(true)
            settingsRepository.setIsRestoring(false)
            settingsRepository.clearRestoreCheckpoint()

            emit(SyncProgress.Completed(SyncReport(timestamp = System.currentTimeMillis())))

        } catch (e: Exception) {
            Log.e(TAG, "恢复过程发生严重错误", e)
            settingsRepository.setIsRestoring(false)
            emit(SyncProgress.Failed("Restore failed: ${e.message}"))
        } finally {
            syncMutex.unlock()
        }
    }

    /**
     * 处理单个批次：拉取 -> 写入 -> 返回数量
     * 使用 when 来分发类型，避免泛型擦除问题
     */
    private suspend fun processBatch(
        tableName: String,
        userId: String,
        offset: Int,
        limit: Int
    ): Int {
        return when (tableName) {
            TABLE_WORD_STATES -> {
                val dtos = pullBatch<SyncWordStateDto>(tableName, userId, offset, limit)
                if (dtos.isNotEmpty()) database.withTransaction {
                    wordStudyStateDao.insertAll(dtos.map { it.toEntity() })
                }
                dtos.size
            }
            TABLE_GRAMMAR_STATES -> {
                val dtos = pullBatch<SyncGrammarStateDto>(tableName, userId, offset, limit)
                if (dtos.isNotEmpty()) database.withTransaction {
                    grammarStudyStateDao.insertAll(dtos.map { it.toEntity() })
                }
                dtos.size
            }
            TABLE_STUDY_RECORDS -> {
                val dtos = pullBatch<SyncStudyRecordDto>(tableName, userId, offset, limit)
                if (dtos.isNotEmpty()) database.withTransaction {
                    studyRecordDao.insertAll(dtos.map { it.toEntity() })
                }
                dtos.size
            }
            TABLE_FAVORITE_QUESTIONS -> {
                val dtos = pullBatch<SyncFavoriteQuestionDto>(tableName, userId, offset, limit)
                if (dtos.isNotEmpty()) database.withTransaction {
                    favoriteQuestionDao.upsertAll(dtos.map { it.toEntity() })
                }
                dtos.size
            }
            TABLE_USER_SETTINGS -> {
                // Settings imply only 1 batch (at offset 0)
                if (offset == 0) {
                    pullSettings(userId)
                } else {
                    0
                }
            }
            TABLE_TEST_RECORDS -> {
                val dtos = pullBatch<SyncTestRecordDto>(tableName, userId, offset, limit)
                if (dtos.isNotEmpty()) database.withTransaction {
                    testRecordDao.insertAll(dtos.map { it.toEntity() })
                }
                dtos.size
            }
            TABLE_WRONG_ANSWERS -> {
                val dtos = pullBatch<SyncWrongAnswerDto>(tableName, userId, offset, limit)
                if (dtos.isNotEmpty()) database.withTransaction {
                    wrongAnswerDao.insertAll(dtos.map { it.toEntity() })
                }
                dtos.size
            }
            TABLE_GRAMMAR_WRONG_ANSWERS -> {
                val dtos = pullBatch<SyncGrammarWrongAnswerDto>(tableName, userId, offset, limit)
                if (dtos.isNotEmpty()) database.withTransaction {
                    grammarWrongAnswerDao.insertAll(dtos.map { it.toEntity() })
                }
                dtos.size
            }
            else -> 0
        }
    }

    /** 泛型分页拉取辅助方法 */
    private suspend inline fun <reified T : Any> pullBatch(
        tableName: String,
        userId: String,
        offset: Int,
        limit: Int
    ): List<T> {
        return supabaseClient.postgrest[tableName]
            .select(columns = Columns.ALL) {
                filter { eq("user_id", userId) }
                range(offset.toLong(), (offset + limit - 1).toLong())
            }.decodeList<T>()
    }

    /** 泛型全量分页拉取辅助方法 */
    private suspend inline fun <reified T : Any> pullAllPaged(
        tableName: String,
        userId: String,
        crossinline filterBlock: PostgrestFilterBuilder.() -> Unit = {}
    ): List<T> {
        val all = mutableListOf<T>()
        var offset = 0L
        val pageSize = 1000L
        while (true) {
            val batch = supabaseClient.postgrest[tableName]
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                        filterBlock()
                    }
                    range(offset, offset + pageSize - 1)
                }.decodeList<T>()
            if (batch.isEmpty()) break
            all.addAll(batch)
            if (batch.size < pageSize) break
            offset += pageSize
        }
        return all
    }


    /** 辅助方法：全量拉取指定表的数据 */
    private suspend inline fun <reified T : Any> pullAllFromCloud(tableName: String, userId: String): List<T> {
        return supabaseClient.postgrest[tableName]
            .select(columns = Columns.ALL) {
                filter { eq("user_id", userId) }
            }.decodeList<T>()
    }

    /** 事务内清空本地所有用户业务数据 */
    private suspend fun clearLocalUserDataInTransaction() {
        wordStudyStateDao.deleteAll()
        grammarStudyStateDao.deleteAll()
        studyRecordDao.deleteAll()
        testRecordDao.deleteAll()
        wrongAnswerDao.deleteAll()
        grammarWrongAnswerDao.deleteAll()
        favoriteQuestionDao.deleteAll()
    }


    /** 从云端获取最低兼容版本号 */
    private suspend fun getRemoteMinVersion(): Int = withContext(Dispatchers.IO) {
        try {
            val meta = supabaseClient.postgrest[TABLE_SYNC_META]
                .select()
                .decodeSingleOrNull<SyncMetaDto>()
            meta?.minVersion ?: SYNC_SCHEMA_VERSION
        } catch (e: Exception) {
            Log.w(TAG, "获取远程版本号失败，使用本地默认值", e)
            SYNC_SCHEMA_VERSION
        }
    }

    private suspend fun clearLocalUserData(): Unit = withContext(Dispatchers.IO) {
        Log.d(TAG, "正在清空本地用户数据表...")
        database.withTransaction {
            clearLocalUserDataInTransaction()
        }
    }


    // ===================================
    // Words Sync Logic
    // ===================================

    private suspend fun pullWords(
        userId: String,
        sinceTime: Long,
        isFullReset: Boolean
    ): PullResult<WordStudyStateEntity, Int> {
        val remoteChanges = pullAllPaged<SyncWordStateDto>(TABLE_WORD_STATES, userId) {
            if (!isFullReset) gt("last_modified_time", sinceTime)
        }

        Log.d(TAG, "Pull WordStates: Found ${remoteChanges.size} changes from cloud")

        val toUpsert = mutableListOf<WordStudyStateEntity>()
        val acceptedIds = mutableSetOf<Int>()

        if (remoteChanges.isNotEmpty()) {
            val remoteIds = remoteChanges.map { it.wordId }

            if (isFullReset) {
                // 全量覆盖
                toUpsert.addAll(remoteChanges.filter { !it.isDeleted }.map { it.toEntity() })
                acceptedIds.addAll(remoteIds)
            } else {
                // 增量合并
                val localStatesMap = wordStudyStateDao.getStatesByIds(remoteIds).associateBy { it.wordId }

                remoteChanges.forEach { remoteDto ->
                    val localState = localStatesMap[remoteDto.wordId]
                    val remoteProgress = remoteDto.toWordProgress()

                    if (localState != null) {
                        when (val result = SmartSyncMerger.mergeWordProgress(localState, remoteProgress)) {
                            is SmartSyncMerger.MergeResult.RemoteUpdated -> {
                                toUpsert.add(result.data)
                                acceptedIds.add(remoteDto.wordId)
                            }
                            is SmartSyncMerger.MergeResult.LocalKept -> {
                                // Local is newer, keep it. Do NOT add to acceptedIds (so we can push local)
                            }
                        }
                    } else if (!remoteDto.isDeleted) {
                        toUpsert.add(remoteDto.toEntity())
                        acceptedIds.add(remoteDto.wordId)
                    }
                }
            }
        }
        return PullResult(toUpsert, acceptedIds, remoteChanges.size)
    }

    private suspend fun pushWords(
        userId: String,
        sinceTime: Long,
        acceptedIds: Set<Int>
    ): Int {
        val localChanges = wordStudyStateDao.getModifiedSince(sinceTime)
            .filter { !acceptedIds.contains(it.wordId) } // 仅过滤掉明确被云端更新覆盖的记录

        Log.d(TAG, "Push WordStates: Found ${localChanges.size} changes to push")

        if (localChanges.isNotEmpty()) {
             localChanges.chunked(BATCH_SIZE).forEach { chunk ->
                val dtos = chunk.map { it.toSyncDto(userId) }
                supabaseClient.postgrest[TABLE_WORD_STATES].upsert(dtos) {
                    onConflict = "user_id, word_id"
                    ignoreDuplicates = false
                }
            }
        }
        return localChanges.size
    }

    // ===================================
    // Grammars Sync Logic
    // ===================================

    private suspend fun pullGrammars(
        userId: String,
        sinceTime: Long,
        isFullReset: Boolean
    ): PullResult<GrammarStudyStateEntity, Int> {
        val remoteChanges = pullAllPaged<SyncGrammarStateDto>(TABLE_GRAMMAR_STATES, userId) {
            if (!isFullReset) gt("last_modified_time", sinceTime)
        }

        val toUpsert = mutableListOf<GrammarStudyStateEntity>()
        val acceptedIds = mutableSetOf<Int>()

        if (remoteChanges.isNotEmpty()) {
            val remoteIds = remoteChanges.map { it.grammarId }

            if (isFullReset) {
                toUpsert.addAll(remoteChanges.filter { !it.isDeleted }.map { it.toEntity() })
                acceptedIds.addAll(remoteIds)
            } else {
                val localStatesMap = grammarStudyStateDao.getStatesByIds(remoteIds).associateBy { it.grammarId }

                remoteChanges.forEach { remoteDto ->
                    val localState = localStatesMap[remoteDto.grammarId]
                    val remoteProgress = remoteDto.toGrammarProgress()

                    if (localState != null) {
                        when (val result = SmartSyncMerger.mergeGrammarProgress(localState, remoteProgress)) {
                            is SmartSyncMerger.MergeResult.RemoteUpdated -> {
                                toUpsert.add(result.data)
                                acceptedIds.add(remoteDto.grammarId)
                            }
                            is SmartSyncMerger.MergeResult.LocalKept -> { }
                        }
                    } else if (!remoteDto.isDeleted) {
                        toUpsert.add(remoteDto.toEntity())
                        acceptedIds.add(remoteDto.grammarId)
                    }
                }
            }
        }
        return PullResult(toUpsert, acceptedIds, remoteChanges.size)
    }

    private suspend fun pushGrammars(
        userId: String,
        sinceTime: Long,
        acceptedIds: Set<Int>
    ): Int {
        val localChanges = grammarStudyStateDao.getModifiedSince(sinceTime)
            .filter { !acceptedIds.contains(it.grammarId) }

        if (localChanges.isNotEmpty()) {
            localChanges.chunked(BATCH_SIZE).forEach { chunk ->
                val dtos = chunk.map { it.toSyncDto(userId) }
                supabaseClient.postgrest[TABLE_GRAMMAR_STATES].upsert(dtos) {
                    onConflict = "user_id, grammar_id"
                }
            }
        }
        return localChanges.size
    }

    // ===================================
    // StudyRecords Sync Logic
    // ===================================

    private suspend fun pullStudyRecords(
        userId: String,
        sinceTime: Long,
        isFullReset: Boolean
    ): PullResult<StudyRecordEntity, Long> {
        val remoteChanges = pullAllPaged<SyncStudyRecordDto>(TABLE_STUDY_RECORDS, userId) {
            if (!isFullReset) gt("timestamp", sinceTime)
        }

        val toUpsert = mutableListOf<StudyRecordEntity>()
        val acceptedIds = mutableSetOf<Long>()

        if (remoteChanges.isNotEmpty()) {
            if (isFullReset) {
                toUpsert.addAll(remoteChanges.filter { !it.isDeleted }.map { it.toEntity() })
                acceptedIds.addAll(remoteChanges.map { it.date })
            } else {
                remoteChanges.forEach { remoteDto ->
                    val localState = studyRecordDao.getByDate(remoteDto.date).first()

                    if (localState != null) {
                        when (val result = SmartSyncMerger.mergeStudyRecord(localState, remoteDto)) {
                            is SmartSyncMerger.MergeResult.RemoteUpdated -> {
                                toUpsert.add(result.data)
                                acceptedIds.add(remoteDto.date)
                            }
                            is SmartSyncMerger.MergeResult.LocalKept -> { }
                        }
                    } else if (!remoteDto.isDeleted) {
                        toUpsert.add(remoteDto.toEntity())
                        acceptedIds.add(remoteDto.date)
                    }
                }
            }
        }
        return PullResult(toUpsert, acceptedIds, remoteChanges.size)
    }

    private suspend fun pushStudyRecords(
        userId: String,
        sinceTime: Long,
        acceptedIds: Set<Long>
    ): Int {
        val localChanges = studyRecordDao.getModifiedSince(sinceTime)
            .filter { !acceptedIds.contains(it.date) }

        if (localChanges.isNotEmpty()) {
            localChanges.chunked(BATCH_SIZE).forEach { chunk ->
                val dtos = chunk.map { it.toSyncDto(userId) }
                supabaseClient.postgrest[TABLE_STUDY_RECORDS].upsert(dtos) {
                    onConflict = "user_id, date"
                }
            }
        }
        return localChanges.size
    }

    // ===================================
    // TestRecords Sync Logic
    // ===================================

    private suspend fun pullTestRecords(
        userId: String,
        sinceTime: Long,
        isFullReset: Boolean
    ): PullResult<TestRecordEntity, String> {
        val remoteChanges = pullAllPaged<SyncTestRecordDto>(TABLE_TEST_RECORDS, userId) {
            if (!isFullReset) gt("timestamp", sinceTime)
        }

        val toUpsert = mutableListOf<TestRecordEntity>()
        val acceptedIds = mutableSetOf<String>() // UUID

        if (remoteChanges.isNotEmpty()) {
            val remoteUuids = remoteChanges.map { it.uuid }

            if (isFullReset) {
                toUpsert.addAll(remoteChanges.filter { !it.isDeleted }.map { it.toEntity() })
                acceptedIds.addAll(remoteUuids)
            } else {
                val localStatesMap = testRecordDao.getByUuids(remoteUuids).associateBy { it.uuid }

                remoteChanges.forEach { remoteDto ->
                    val localState = localStatesMap[remoteDto.uuid]

                    if (localState != null) {
                        when (val result = SmartSyncMerger.mergeTestRecord(localState, remoteDto)) {
                            is SmartSyncMerger.MergeResult.RemoteUpdated -> {
                                toUpsert.add(result.data)
                                acceptedIds.add(remoteDto.uuid)
                            }
                            is SmartSyncMerger.MergeResult.LocalKept -> { }
                        }
                    } else if (!remoteDto.isDeleted) {
                        toUpsert.add(remoteDto.toEntity())
                        acceptedIds.add(remoteDto.uuid)
                    }
                }
            }
        }
        return PullResult(toUpsert, acceptedIds, remoteChanges.size)
    }

    private suspend fun pushTestRecords(
        userId: String,
        sinceTime: Long,
        acceptedIds: Set<String>
    ): Int {
        val localChanges = testRecordDao.getModifiedSince(sinceTime)
            .filter { !acceptedIds.contains(it.uuid) }

        if (localChanges.isNotEmpty()) {
            localChanges.chunked(BATCH_SIZE).forEach { chunk ->
                val dtos = chunk.map { it.toSyncDto(userId) }
                supabaseClient.postgrest[TABLE_TEST_RECORDS].upsert(dtos) {
                    onConflict = "user_id, uuid"
                }
            }
        }
        return localChanges.size
    }

    // ===================================
    // WrongAnswers Sync Logic
    // ===================================

    private suspend fun pullWrongAnswers(
        userId: String,
        sinceTime: Long,
        isFullReset: Boolean
    ): PullResult<WrongAnswerEntity, Int> {
        val remoteChanges = pullAllPaged<SyncWrongAnswerDto>(TABLE_WRONG_ANSWERS, userId) {
            if (!isFullReset) gt("timestamp", sinceTime)
        }

        val toUpsert = mutableListOf<WrongAnswerEntity>()
        val acceptedIds = mutableSetOf<Int>() // WordId

        if (remoteChanges.isNotEmpty()) {
            val remoteWordIds = remoteChanges.map { it.wordId }

            if (isFullReset) {
                toUpsert.addAll(remoteChanges.filter { !it.isDeleted }.map { it.toEntity() })
                acceptedIds.addAll(remoteWordIds)
            } else {
                val remoteUuids = remoteChanges.map { it.uuid }
                val localStatesMap = wrongAnswerDao.getByUuids(remoteUuids).associateBy { it.uuid }

                remoteChanges.forEach { remoteDto ->
                    val localState = localStatesMap[remoteDto.uuid]

                    if (localState != null) {
                        when (val result = SmartSyncMerger.mergeWrongAnswer(localState, remoteDto)) {
                            is SmartSyncMerger.MergeResult.RemoteUpdated -> {
                                toUpsert.add(result.data)
                                acceptedIds.add(remoteDto.wordId)
                            }
                            is SmartSyncMerger.MergeResult.LocalKept -> { }
                        }
                    } else if (!remoteDto.isDeleted) {
                        toUpsert.add(remoteDto.toEntity())
                        acceptedIds.add(remoteDto.wordId)
                    }
                }
            }
        }
        return PullResult(toUpsert, acceptedIds, remoteChanges.size)
    }

    private suspend fun pushWrongAnswers(
        userId: String,
        sinceTime: Long,
        acceptedIds: Set<Int>
    ): Int {
        val localChanges = wrongAnswerDao.getModifiedSince(sinceTime)
            .filter { !acceptedIds.contains(it.wordId) }
            .sortedByDescending { it.timestamp }
            .distinctBy { it.wordId }

        if (localChanges.isNotEmpty()) {
            localChanges.chunked(BATCH_SIZE).forEach { chunk ->
                val dtos = chunk.map { it.toSyncDto(userId) }
                supabaseClient.postgrest[TABLE_WRONG_ANSWERS].upsert(dtos) {
                    onConflict = "user_id, word_id"
                }
            }
        }
        return localChanges.size
    }

    // ===================================
    // GrammarWrongAnswers Sync Logic
    // ===================================

    private suspend fun pullGrammarWrongAnswers(
        userId: String,
        sinceTime: Long,
        isFullReset: Boolean
    ): PullResult<GrammarWrongAnswerEntity, Int> {
        val remoteChanges = pullAllPaged<SyncGrammarWrongAnswerDto>(TABLE_GRAMMAR_WRONG_ANSWERS, userId) {
            if (!isFullReset) gt("timestamp", sinceTime)
        }

        val toUpsert = mutableListOf<GrammarWrongAnswerEntity>()
        val acceptedIds = mutableSetOf<Int>() // GrammarId

        if (remoteChanges.isNotEmpty()) {
            val remoteIds = remoteChanges.map { it.grammarId }

            if (isFullReset) {
                toUpsert.addAll(remoteChanges.filter { !it.isDeleted }.map { it.toEntity() })
                acceptedIds.addAll(remoteIds)
            } else {
                val remoteUuids = remoteChanges.map { it.uuid }
                val localStatesMap = grammarWrongAnswerDao.getByUuids(remoteUuids).associateBy { it.uuid }

                remoteChanges.forEach { remoteDto ->
                    val localState = localStatesMap[remoteDto.uuid]

                    if (localState != null) {
                        when (val result = SmartSyncMerger.mergeGrammarWrongAnswer(localState, remoteDto)) {
                            is SmartSyncMerger.MergeResult.RemoteUpdated -> {
                                toUpsert.add(result.data)
                                acceptedIds.add(remoteDto.grammarId)
                            }
                            is SmartSyncMerger.MergeResult.LocalKept -> { }
                        }
                    } else if (!remoteDto.isDeleted) {
                        toUpsert.add(remoteDto.toEntity())
                        acceptedIds.add(remoteDto.grammarId)
                    }
                }
            }
        }
        return PullResult(toUpsert, acceptedIds, remoteChanges.size)
    }

    private suspend fun pushGrammarWrongAnswers(
        userId: String,
        sinceTime: Long,
        acceptedIds: Set<Int>
    ): Int {
        val localChanges = grammarWrongAnswerDao.getModifiedSince(sinceTime)
            .filter { !acceptedIds.contains(it.grammarId) }
            .sortedByDescending { it.timestamp }
            .distinctBy { it.grammarId }

        if (localChanges.isNotEmpty()) {
            localChanges.chunked(BATCH_SIZE).forEach { chunk ->
                val dtos = chunk.map { it.toSyncDto(userId) }
                supabaseClient.postgrest[TABLE_GRAMMAR_WRONG_ANSWERS].upsert(dtos) {
                    onConflict = "user_id, grammar_id"
                }
            }
        }
        return localChanges.size
    }

    private suspend fun pullFavoriteQuestions(
        userId: String,
        sinceTime: Long,
        forceAll: Boolean
    ): PullResult<com.jian.nemo.core.data.local.entity.FavoriteQuestionEntity, String> {
        val queryTime = if (forceAll) 0L else sinceTime
        val remoteDtos = try {
            pullAllPaged<com.jian.nemo.core.data.manager.SyncFavoriteQuestionDto>(TABLE_FAVORITE_QUESTIONS, userId) {
                gt("timestamp", queryTime)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pull favorite questions failed: ${e.message}")
            emptyList()
        }

        val entities = remoteDtos.map { it.toEntity() }
        val acceptedIds = remoteDtos.map { it.timestamp.toString() }.toSet()
        return PullResult(entities, acceptedIds, entities.size)
    }

    private suspend fun pushFavoriteQuestions(
        userId: String,
        sinceTime: Long,
        acceptedTimestamps: Set<String>
    ): Int {
        val localChanges = favoriteQuestionDao.getModifiedSince(sinceTime)
            .filter { !acceptedTimestamps.contains(it.timestamp.toString()) }
            .sortedByDescending { it.timestamp }
            .distinctBy { it.timestamp }

        if (localChanges.isNotEmpty()) {
            localChanges.chunked(BATCH_SIZE).forEach { chunk ->
                val dtos = chunk.map { it.toSyncDto(userId) }
                try {
                    supabaseClient.postgrest[TABLE_FAVORITE_QUESTIONS].upsert(dtos) {
                        onConflict = "user_id, timestamp"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Push favorite questions failed: ${e.message}")
                }
            }
        }
        return localChanges.size
    }

    private suspend fun pullSettings(userId: String): Int {
        try {
            val remoteDto = supabaseClient.postgrest[TABLE_USER_SETTINGS].select(columns = Columns.ALL) {
                filter {
                    eq("user_id", userId)
                }
                limit(1)
            }.decodeSingleOrNull<com.jian.nemo.core.data.manager.SyncAppSettingsDto>()

            if (remoteDto != null) {
                // [MOD] 增加时间戳校验：只有当云端设置更新时才应用
                val localModifiedTime = settingsRepository.lastSettingsModifiedTimeFlow.first()
                if (remoteDto.updatedAt > localModifiedTime) {
                    settingsRepository.applyAppSettingsSnapshot(remoteDto.settings)
                    return 1
                } else {
                    Log.d(TAG, "云端设置较旧或与本地一致 (Remote: ${remoteDto.updatedAt}, Local: $localModifiedTime)，跳过应用")
                }
            }
        } catch (e: Exception) {
             Log.e(TAG, "Pull settings failed: ${e.message}")
        }
        return 0
    }

    private suspend fun pushSettings(userId: String): Int {
        try {
            val snapshot = settingsRepository.getAppSettingsSnapshot()
            val dto = com.jian.nemo.core.data.manager.SyncAppSettingsDto(
                userId = userId,
                settings = snapshot,
                updatedAt = System.currentTimeMillis()
            )
            supabaseClient.postgrest[TABLE_USER_SETTINGS].upsert(dto) {
                onConflict = "user_id"
            }
            return 1
        } catch (e: Exception) {
            Log.e(TAG, "Push settings failed: ${e.message}")
        }
        return 0
    }

    /**
     * 彻底物理删除该用户在云端的所有数据记录
     */
    suspend fun deleteAllCloudData(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "正在物理擦除云端数据: User $userId")

            val tables = listOf(
                TABLE_WORD_STATES,
                TABLE_GRAMMAR_STATES,
                TABLE_STUDY_RECORDS,
                TABLE_TEST_RECORDS,
                TABLE_WRONG_ANSWERS,
                TABLE_GRAMMAR_WRONG_ANSWERS,
                TABLE_FAVORITE_QUESTIONS, // [NEW]
                TABLE_USER_SETTINGS // [NEW]
            )

            var allSuccess = true

            tables.forEach { tableName ->
                try {
                    supabaseClient.postgrest[tableName].delete {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    Log.d(TAG, "已成功清理云端表: $tableName")
                } catch (e: Exception) {
                    Log.e(TAG, "清理云端表 $tableName 失败: ${e.message}")
                    allSuccess = false
                }
            }

            allSuccess
        } catch (e: Exception) {
            Log.e(TAG, "远程数据擦除过程发生异常", e)
            false
        }
    }
}

/**
 * 通用拉取结果
 */
private data class PullResult<T, ID>(
    val toUpsert: List<T>,
    val acceptedIds: Set<ID>, // 用于 Push 时的过滤，存储已从云端接受的 ID
    val pulledCount: Int
)
