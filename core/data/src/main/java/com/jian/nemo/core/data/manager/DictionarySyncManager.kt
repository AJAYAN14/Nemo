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
        Log.d(TAG, "寮€濮嬫鏌ュ瓧鍏稿悓姝?..")
        try {
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
            
            // 鑾峰彇鐙珛鐨勫崟璇嶅拰璇硶鏃堕棿鎴?
            var lastWordSyncTimestamp = if (forceFullSync) 0L else settingsRepository.getLastWordSyncTimestamp()
            var lastGrammarSyncTimestamp = if (forceFullSync) 0L else settingsRepository.getLastGrammarSyncTimestamp()
            
            // 骞虫粦杩佺Щ锛氬鏋滅嫭绔嬫椂闂存埑涓?0 浣嗗叏灞€鏃堕棿鎴?> 0锛岃鏄庢槸鍗囩骇鍚庣殑棣栨鍚屾锛屽€熺敤鍏ㄥ眬鏃堕棿鎴?
            if (!forceFullSync && lastWordSyncTimestamp == 0L && oldGlobalSyncTimestamp > 0L) {
                lastWordSyncTimestamp = oldGlobalSyncTimestamp
                Log.i(TAG, "鍗曡瘝鍚屾鍒嗗锛氭娴嬪埌鏃у叏灞€閿氱偣锛屽€熺敤 $oldGlobalSyncTimestamp 杩涜骞虫粦杩佺Щ")
            }
            if (!forceFullSync && lastGrammarSyncTimestamp == 0L && oldGlobalSyncTimestamp > 0L) {
                lastGrammarSyncTimestamp = oldGlobalSyncTimestamp
                Log.i(TAG, "璇硶鍚屾鍒嗗锛氭娴嬪埌鏃у叏灞€閿氱偣锛屽€熺敤 $oldGlobalSyncTimestamp 杩涜骞虫粦杩佺Щ")
            }

            // 濡傛灉鐗堟湰涓嶄竴鑷达紝鎴栬€呮湰鍦版暟鎹簱涓虹┖锛屽垯瑙﹀彂鍚屾
            val wordCount = wordDao.getWordCount()
            val grammarCount = grammarDao.getGrammarCount()
            val isDatabaseEmpty = wordCount == 0 || grammarCount == 0
            val isAnySyncRequired = lastWordSyncTimestamp == 0L || lastGrammarSyncTimestamp == 0L

            Log.i(TAG, "璇嶅簱鍚屾鐘舵€佹鏌? RemoteV=$remoteVersion, LocalV=$lastVersion, WordTS=$lastWordSyncTimestamp, GrammarTS=$lastGrammarSyncTimestamp, WordCount=$wordCount, GrammarCount=$grammarCount, isEmpty=$isDatabaseEmpty, force=$forceFullSync, forceIncremental=$forceIncremental")

            if (forceFullSync || forceIncremental || (remoteVersion != null && (remoteVersion > lastVersion || isDatabaseEmpty || isAnySyncRequired))) {
                val isFullSync = forceFullSync || isDatabaseEmpty || (lastWordSyncTimestamp == 0L && lastGrammarSyncTimestamp == 0L)
                Log.i(TAG, ">>> 寮€濮嬪悓姝ヨ瘝搴?(${if (isFullSync) "鍏ㄩ噺妯″紡" else "澧為噺妯″紡"}): force=$forceFullSync, forceIncremental=$forceIncremental, V$lastVersion -> V$remoteVersion")

                if (forceFullSync) {
                    Log.w(TAG, "寮哄埗閲嶇疆妯″紡锛氬皢鎵ц鍏ㄩ噺瑕嗙洊骞堕€昏緫涓嬫灦杩囨椂鏁版嵁")
                }

                // 鎵ц鍚屾鎷夊彇
                val (allWords: List<WordDto>, allGrammars: List<GrammarDto>) = coroutineScope {
                    if (isFullSync) {
                        val w = async { contentRepository.fetchAllRemoteWords() }
                        val g = async { contentRepository.fetchAllRemoteGrammars() }
                        w.await() to g.await()
                    } else {
                        // 澧為噺妯″紡锛氫娇鐢ㄥ悇鑷殑鏃堕棿鎴虫媺鍙?
                        val wordTimestampStr = DateTimeUtils.formatIso8601(java.util.Date(lastWordSyncTimestamp))
                        val grammarTimestampStr = DateTimeUtils.formatIso8601(java.util.Date(lastGrammarSyncTimestamp))
                        
                        Log.d(TAG, "澧為噺鎷夊彇鏃堕棿鎴? Word=$wordTimestampStr, Grammar=$grammarTimestampStr")
                        
                        val w = async { contentRepository.fetchWordsModifiedSince(wordTimestampStr) }
                        val g = async { contentRepository.fetchGrammarsModifiedSince(grammarTimestampStr) }
                        w.await() to g.await()
                    }
                }

                Log.i(TAG, "涓嬭浇瀹屾垚: ${allWords.size} 鍗曡瘝, ${allGrammars.size} 璇硶")

                // 搴旂敤鍙樻洿
                if (allWords.isNotEmpty()) {
                    contentUpdateApplier.applyAllWords(allWords, isFullSync)
                }
                if (allGrammars.isNotEmpty()) {
                    contentUpdateApplier.applyAllGrammars(allGrammars, isFullSync)
                }

                // 璁＄畻骞舵洿鏂版柊鐨勫悓姝ラ敋鐐规椂闂存埑 (鍒嗗埆鍙栫粨鏋滀腑鏈€澶х殑 updated_at)
                // [FIX] Supabase 鐨?updated_at 鏄井绉掔骇锛岃浆涓烘绉掓椂浼氫涪澶辩簿搴︼紝瀵艰嚧鍚庣画 gt 鏌ヨ鏃犻檺鍖归厤鍒板悓涓€鏉¤褰曘€傚姞 1 姣璺冲嚭寰幆銆?
                val maxWordTimestamp = allWords.mapNotNull { DateTimeUtils.parseIso8601(it.updatedAt)?.time }.maxOrNull()?.plus(1L) ?: 0L
                val maxGrammarTimestamp = allGrammars.mapNotNull { DateTimeUtils.parseIso8601(it.updatedAt)?.time }.maxOrNull()?.plus(1L) ?: 0L
                
                // 鍗曡瘝閿氱偣澶勭悊
                var newWordSyncTimestamp = maxOf(lastWordSyncTimestamp, maxWordTimestamp)
                if (isFullSync && newWordSyncTimestamp == 0L) newWordSyncTimestamp = System.currentTimeMillis()
                
                if (newWordSyncTimestamp > lastWordSyncTimestamp) {
                    settingsRepository.setLastWordSyncTimestamp(newWordSyncTimestamp)
                    Log.d(TAG, "鏇存柊鍗曡瘝鍚屾鏃堕棿鎴抽敋鐐? $newWordSyncTimestamp")
                }

                // 璇硶閿氱偣澶勭悊
                var newGrammarSyncTimestamp = maxOf(lastGrammarSyncTimestamp, maxGrammarTimestamp)
                if (isFullSync && newGrammarSyncTimestamp == 0L) newGrammarSyncTimestamp = System.currentTimeMillis()
                
                if (newGrammarSyncTimestamp > lastGrammarSyncTimestamp) {
                    settingsRepository.setLastGrammarSyncTimestamp(newGrammarSyncTimestamp)
                    Log.d(TAG, "鏇存柊璇硶鍚屾鏃堕棿鎴抽敋鐐? $newGrammarSyncTimestamp")
                }

                // 鍚屾椂鏇存柊鏃х殑鍏ㄥ眬鏃堕棿鎴充互淇濊瘉瀹屽叏鍏煎
                val finalGlobalTS = maxOf(newWordSyncTimestamp, newGrammarSyncTimestamp)
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
                    isFullSync = isFullSync,
                    localVersion = lastVersion,
                    remoteVersion = remoteVersion ?: lastVersion
                )
            } else {
                Log.i(TAG, "璇嶅簱妫€鏌ョ粨鏋? 鏃犻渶鏇存柊 (鏈湴 V$lastVersion, 杩滅▼ V$remoteVersion)")
                return@withContext DictionarySyncResult(
                    localVersion = lastVersion,
                    remoteVersion = remoteVersion ?: lastVersion
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "瀛楀吀鍚屾澶辫触: ${e.message}", e)
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


}
