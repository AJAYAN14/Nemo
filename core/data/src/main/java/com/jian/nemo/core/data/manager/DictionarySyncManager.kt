package com.jian.nemo.core.data.manager

import android.util.Log
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.data.local.dao.*
import com.jian.nemo.core.data.local.entity.*
import com.jian.nemo.core.data.local.NemoDatabase
import com.jian.nemo.core.domain.model.WordDto
import com.jian.nemo.core.domain.model.DictionarySyncResult
import com.jian.nemo.core.domain.model.GrammarDto
import com.jian.nemo.core.domain.repository.DictionarySyncManager
import com.jian.nemo.core.domain.repository.SettingsRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
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

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.withLock

import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

@Serializable
data class SyncMetaDto(
    @SerialName("min_compatible_version") val minVersion: Int
)

@Singleton
class DictionarySyncManagerImpl @Inject constructor(
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

    private val dataSeedService: DataSeedService,
    private val contentRepository: ContentRepository,
    private val contentUpdateApplier: ContentUpdateApplier
) : DictionarySyncManager {
    private val syncMutex = kotlinx.coroutines.sync.Mutex()

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncingFlow: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _readyCheckTrigger = MutableStateFlow(0L)

    override suspend fun isDictionaryReady(): Boolean = withContext(Dispatchers.IO) {
        if (_isSyncing.value) return@withContext false
        val wordCount = wordDao.getWordCount()
        val grammarCount = grammarDao.getGrammarCount()
        wordCount >= 1000 && grammarCount >= 200
    }

    override val isDictionaryReadyFlow: Flow<Boolean> = combine(
        _isSyncing,
        _readyCheckTrigger,
        settingsRepository.lastContentVersionFlow
    ) { syncing, _, _ ->
        if (syncing) {
            false
        } else {
            val wordCount = wordDao.getWordCount()
            val grammarCount = grammarDao.getGrammarCount()
            wordCount >= 1000 && grammarCount >= 200
        }
    }

    override suspend fun performDictionarySync(
        force: Boolean,
        forceIncremental: Boolean
    ): DictionarySyncResult {
        return performDictionarySyncInternal(force, forceIncremental)
    }

    private suspend fun performDictionarySyncInternal(
        force: Boolean = false,
        forceIncremental: Boolean = false
    ): DictionarySyncResult = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            _isSyncing.value = true
            try {
                Log.d(TAG, "开始检查字典同步...")
                val remoteVersion = contentRepository.getRemoteContentVersion()
                val lastVersion = settingsRepository.getLastContentVersion()
            
            // [FIX] 閽堝 v23 鐨勫悓褰㈠紓涔夎瘝 Bug 淇锛屽己鍒剁幇鏈夌敤鎴疯繘琛屼竴娆″叏閲忎簯绔媺鍙?
            val hasAppliedV23Fix = settingsRepository.getHasAppliedV23Fix()
            var forceFullSync = force
            if (!hasAppliedV23Fix) {
                Log.w(TAG, "鈿狅笍 妫€娴嬪埌灏氭湭搴旂敤 v23 鍚屽舰璇嶄簯绔慨澶嶏紝寮哄埗鎵ц涓€娆′簯绔叏閲忔媺鍙栵紒")
                forceFullSync = true
            }

            // 鑾峰彇鏃х殑缁熶竴鏃堕棿鎴筹紙鐢ㄤ簬骞虫粦杩佺Щ锛?
            val oldGlobalSyncTimestamp = settingsRepository.getLastDictionarySyncTimestamp()
            
            // 获取独立的单词、语法、相关词时间戳
            var lastWordSyncTimestamp = if (forceFullSync) 0L else settingsRepository.getLastWordSyncTimestamp()
            var lastGrammarSyncTimestamp = if (forceFullSync) 0L else settingsRepository.getLastGrammarSyncTimestamp()
            var lastWordRelationSyncTimestamp = if (forceFullSync) 0L else settingsRepository.getLastWordRelationSyncTimestamp()
            
            // 平滑迁移：如果独立时间戳为 0 但全局时间戳 > 0，说明是升级后的首次同步，借用全局时间戳
            if (!forceFullSync && lastWordSyncTimestamp == 0L && oldGlobalSyncTimestamp > 0L) {
                lastWordSyncTimestamp = oldGlobalSyncTimestamp
                Log.i(TAG, "单词同步分家：检测到旧全局锚点，借用 $oldGlobalSyncTimestamp 进行平滑迁移")
            }
            if (!forceFullSync && lastGrammarSyncTimestamp == 0L && oldGlobalSyncTimestamp > 0L) {
                lastGrammarSyncTimestamp = oldGlobalSyncTimestamp
                Log.i(TAG, "语法同步分家：检测到旧全局锚点，借用 $oldGlobalSyncTimestamp 进行平滑迁移")
            }
            if (!forceFullSync && lastWordRelationSyncTimestamp == 0L && oldGlobalSyncTimestamp > 0L) {
                lastWordRelationSyncTimestamp = oldGlobalSyncTimestamp
                Log.i(TAG, "相关词同步分家：检测到旧全局锚点，借用 $oldGlobalSyncTimestamp 进行平滑迁移")
            }

            // 如果版本不一致，或者本地数据库为空，则触发同步
            val wordCount = wordDao.getWordCount()
            val grammarCount = grammarDao.getGrammarCount()
            val isDatabaseEmpty = wordCount == 0 || grammarCount == 0
            val isAnySyncRequired = lastWordSyncTimestamp == 0L || lastGrammarSyncTimestamp == 0L || lastWordRelationSyncTimestamp == 0L

            Log.i(TAG, "词库同步状态检查: RemoteV=$remoteVersion, LocalV=$lastVersion, WordTS=$lastWordSyncTimestamp, GrammarTS=$lastGrammarSyncTimestamp, WordRelTS=$lastWordRelationSyncTimestamp, WordCount=$wordCount, GrammarCount=$grammarCount, isEmpty=$isDatabaseEmpty, force=$forceFullSync, forceIncremental=$forceIncremental")

            if (forceFullSync || forceIncremental || (remoteVersion != null && (remoteVersion > lastVersion || isDatabaseEmpty || isAnySyncRequired))) {
                val isFullSync = forceFullSync || isDatabaseEmpty || (lastWordSyncTimestamp == 0L && lastGrammarSyncTimestamp == 0L && lastWordRelationSyncTimestamp == 0L)
                Log.i(TAG, ">>> 寮€濮嬪悓姝ヨ瘝搴?(${if (isFullSync) "鍏ㄩ噺妯″紡" else "澧為噺妯″紡"}): force=$forceFullSync, forceIncremental=$forceIncremental, V$lastVersion -> V$remoteVersion")

                if (forceFullSync) {
                    Log.w(TAG, "寮哄埗閲嶇疆妯″紡锛氬皢鎵ц鍏ㄩ噺瑕嗙洊骞堕€昏緫涓嬫灦杩囨椂鏁版嵁")
                }

                // 执行同步拉取
                val (allWords: List<WordDto>, allGrammars: List<GrammarDto>, allWordRelations: List<com.jian.nemo.core.domain.model.WordRelationDto>) = coroutineScope {
                    if (isFullSync) {
                        val w = async { contentRepository.fetchAllRemoteWords() }
                        val g = async { contentRepository.fetchAllRemoteGrammars() }
                        val r = async { contentRepository.fetchAllRemoteWordRelations() }
                        Triple(w.await(), g.await(), r.await())
                    } else {
                        // 增量模式：使用各自的时间戳拉取
                        val wordTimestampStr = DateTimeUtils.formatIso8601(java.util.Date(lastWordSyncTimestamp))
                        val grammarTimestampStr = DateTimeUtils.formatIso8601(java.util.Date(lastGrammarSyncTimestamp))
                        val wordRelationTimestampStr = DateTimeUtils.formatIso8601(java.util.Date(lastWordRelationSyncTimestamp))
                        
                        Log.d(TAG, "增量拉取时间戳: Word=$wordTimestampStr, Grammar=$grammarTimestampStr, WordRelation=$wordRelationTimestampStr")
                        
                        val w = async { contentRepository.fetchWordsModifiedSince(wordTimestampStr) }
                        val g = async { contentRepository.fetchGrammarsModifiedSince(grammarTimestampStr) }
                        val r = async { contentRepository.fetchWordRelationsModifiedSince(wordRelationTimestampStr) }
                        Triple(w.await(), g.await(), r.await())
                    }
                }

                Log.i(TAG, "下载完成: ${allWords.size} 单词, ${allGrammars.size} 语法, ${allWordRelations.size} 相关词")

                // 应用变更
                if (allWords.isNotEmpty()) {
                    contentUpdateApplier.applyAllWords(allWords, isFullSync)
                }
                if (allGrammars.isNotEmpty()) {
                    contentUpdateApplier.applyAllGrammars(allGrammars, isFullSync)
                }
                if (allWordRelations.isNotEmpty()) {
                    contentUpdateApplier.applyAllWordRelations(allWordRelations, isFullSync)
                }

                // 计算并更新新的同步锚点时间戳 (分别取结果中最大的 updated_at)
                // [FIX] Supabase 的 updated_at 是微秒级，转为毫秒时会丢失精度，导致后续 gt 查询无限匹配到同一条记录。加 1 毫秒跳出循环。
                val maxWordTimestamp = allWords.mapNotNull { DateTimeUtils.parseIso8601(it.updatedAt)?.time }.maxOrNull()?.plus(1L) ?: 0L
                val maxGrammarTimestamp = allGrammars.mapNotNull { DateTimeUtils.parseIso8601(it.updatedAt)?.time }.maxOrNull()?.plus(1L) ?: 0L
                val maxWordRelationTimestamp = allWordRelations.mapNotNull { DateTimeUtils.parseIso8601(it.createdAt ?: "")?.time }.maxOrNull()?.plus(1L) ?: 0L
                
                // 单词锚点处理
                var newWordSyncTimestamp = maxOf(lastWordSyncTimestamp, maxWordTimestamp)
                if (isFullSync && newWordSyncTimestamp == 0L) newWordSyncTimestamp = System.currentTimeMillis()
                
                if (newWordSyncTimestamp > lastWordSyncTimestamp) {
                    settingsRepository.setLastWordSyncTimestamp(newWordSyncTimestamp)
                    Log.d(TAG, "更新单词同步时间戳锚点: $newWordSyncTimestamp")
                }

                // 语法锚点处理
                var newGrammarSyncTimestamp = maxOf(lastGrammarSyncTimestamp, maxGrammarTimestamp)
                if (isFullSync && newGrammarSyncTimestamp == 0L) newGrammarSyncTimestamp = System.currentTimeMillis()
                
                if (newGrammarSyncTimestamp > lastGrammarSyncTimestamp) {
                    settingsRepository.setLastGrammarSyncTimestamp(newGrammarSyncTimestamp)
                    Log.d(TAG, "更新语法同步时间戳锚点: $newGrammarSyncTimestamp")
                }
                
                // 相关词锚点处理
                var newWordRelationSyncTimestamp = maxOf(lastWordRelationSyncTimestamp, maxWordRelationTimestamp)
                if (isFullSync && newWordRelationSyncTimestamp == 0L) newWordRelationSyncTimestamp = System.currentTimeMillis()
                
                if (newWordRelationSyncTimestamp > lastWordRelationSyncTimestamp) {
                    settingsRepository.setLastWordRelationSyncTimestamp(newWordRelationSyncTimestamp)
                    Log.d(TAG, "更新相关词同步时间戳锚点: $newWordRelationSyncTimestamp")
                }

                // 同时更新旧的全局时间戳以保证完全兼容
                val finalGlobalTS = maxOf(newWordSyncTimestamp, newGrammarSyncTimestamp, newWordRelationSyncTimestamp)
                settingsRepository.setLastDictionarySyncTimestamp(finalGlobalTS)

                // 鏇存柊鏈湴鐗堟湰鍙?
                // 更新本地版本号
                remoteVersion?.let {
                    settingsRepository.setLastContentVersion(it)
                }

                if (!hasAppliedV23Fix) {
                    settingsRepository.setHasAppliedV23Fix(true)
                    Log.i(TAG, "v23 fix applied")
                }

                Log.i(TAG, "Dictionary sync task finished")
                
                return@withContext DictionarySyncResult(
                    updatedWords = allWords.size,
                    updatedGrammars = allGrammars.size,
                    updatedWordRelations = allWordRelations.size,
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
            DictionarySyncResult()
        } finally {
            _isSyncing.value = false
            _readyCheckTrigger.value = System.currentTimeMillis()
        }
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


}
