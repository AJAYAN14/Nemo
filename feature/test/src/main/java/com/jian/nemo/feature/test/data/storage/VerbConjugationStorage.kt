package com.jian.nemo.feature.test.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jian.nemo.core.common.di.ApplicationScope
import com.jian.nemo.core.common.network.Dispatcher
import com.jian.nemo.core.common.network.NemoDispatchers
import com.jian.nemo.feature.test.di.VerbSessionDataStore
import com.jian.nemo.feature.test.presentation.ability.VerbConjugationQuestion
import com.jian.nemo.feature.test.presentation.ability.VerbConjugationSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VerbConjugationStorage @Inject constructor(
    @VerbSessionDataStore private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val appScope: CoroutineScope,
    @Dispatcher(NemoDispatchers.IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json { ignoreUnknownKeys = true }

    private sealed interface WriteOp {
        data class SaveSession(val session: VerbConjugationSession) : WriteOp
        data object ClearSession : WriteOp
        data class SaveCachePool(val level: String, val pool: List<List<VerbConjugationQuestion>>) : WriteOp
        data class SetPregen(val enabled: Boolean) : WriteOp
        data class SetPlatform(val platform: String) : WriteOp
        data class ClearExpiredCaches(val thirtyDaysMs: Long) : WriteOp
        data class Flush(val ack: CompletableDeferred<Unit>) : WriteOp
    }

    private val writeChannel = Channel<WriteOp>(Channel.UNLIMITED)

    init {
        appScope.launch(ioDispatcher) {
            for (op in writeChannel) {
                try {
                    when (op) {
                        is WriteOp.SaveSession -> {
                            val str = json.encodeToString(op.session)
                            dataStore.edit { it[KEY_CURRENT_SESSION] = str }
                        }
                        is WriteOp.ClearSession -> {
                            dataStore.edit { it.remove(KEY_CURRENT_SESSION) }
                        }
                        is WriteOp.SaveCachePool -> {
                            val lvl = op.level.uppercase()
                            val poolStr = json.encodeToString(op.pool)
                            dataStore.edit { prefs ->
                                prefs[stringPreferencesKey("cache_pool_$lvl")] = poolStr
                                prefs[intPreferencesKey("cache_pool_size_$lvl")] = op.pool.size
                                if (op.pool.isEmpty()) {
                                    prefs.remove(longPreferencesKey("cache_pool_timestamp_$lvl"))
                                } else {
                                    prefs[longPreferencesKey("cache_pool_timestamp_$lvl")] = System.currentTimeMillis()
                                }
                            }
                        }
                        is WriteOp.SetPregen -> {
                            dataStore.edit { it[KEY_PREF_VERB_PREGEN_ENABLED] = op.enabled }
                        }
                        is WriteOp.SetPlatform -> {
                            dataStore.edit { it[KEY_PREF_LAST_USED_PLATFORM] = op.platform }
                        }
                        is WriteOp.ClearExpiredCaches -> {
                            val now = System.currentTimeMillis()
                            val levels = listOf("N5", "N4", "N3", "N2", "N1")
                            dataStore.edit { prefs ->
                                for (lvl in levels) {
                                    val ts = prefs[longPreferencesKey("cache_pool_timestamp_$lvl")] ?: 0L
                                    if (ts > 0L && (now - ts) > op.thirtyDaysMs) {
                                        prefs.remove(stringPreferencesKey("cache_pool_$lvl"))
                                        prefs.remove(intPreferencesKey("cache_pool_size_$lvl"))
                                        prefs.remove(longPreferencesKey("cache_pool_timestamp_$lvl"))
                                    }
                                }
                            }
                        }
                        is WriteOp.Flush -> {
                            op.ack.complete(Unit)
                        }
                    }
                } catch (_: Exception) {
                    if (op is WriteOp.Flush) op.ack.complete(Unit)
                }
            }
        }
    }

    suspend fun restoreSession(): VerbConjugationSession? = withContext(ioDispatcher) {
        try {
            val prefs = dataStore.data.first()
            val raw = prefs[KEY_CURRENT_SESSION] ?: return@withContext null
            json.decodeFromString<VerbConjugationSession>(raw)
        } catch (e: Exception) {
            clearSession()
            null
        }
    }

    fun saveSession(session: VerbConjugationSession) {
        writeChannel.trySend(WriteOp.SaveSession(session))
    }

    fun clearSession() {
        writeChannel.trySend(WriteOp.ClearSession)
    }

    suspend fun getCachePool(level: String): List<List<VerbConjugationQuestion>> = withContext(ioDispatcher) {
        val lvl = level.uppercase()
        val prefs = dataStore.data.first()
        val raw = prefs[stringPreferencesKey("cache_pool_$lvl")] ?: return@withContext emptyList()
        try {
            json.decodeFromString<List<List<VerbConjugationQuestion>>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveCachePool(level: String, pool: List<List<VerbConjugationQuestion>>) {
        writeChannel.trySend(WriteOp.SaveCachePool(level, pool))
    }

    suspend fun getGameCacheCounts(): Map<String, Int> = withContext(ioDispatcher) {
        val prefs = dataStore.data.first()
        val levels = listOf("N5", "N4", "N3", "N2", "N1")
        val totalQuestions = levels.sumOf { level ->
            val lvl = level.uppercase()
            val size = prefs[intPreferencesKey("cache_pool_size_$lvl")] ?: run {
                val raw = prefs[stringPreferencesKey("cache_pool_$lvl")]
                if (raw != null) {
                    try {
                        json.decodeFromString<List<List<VerbConjugationQuestion>>>(raw).size
                    } catch (_: Exception) { 0 }
                } else 0
            }
            size * 5
        }
        val verbCount = Math.round(totalQuestions / 5.0).toInt().coerceAtMost(20)
        mapOf(
            "verb_conjugation" to verbCount,
            "synonym_connection" to 0,
            "antonym_matching" to 0,
            "collocation" to 0,
            "grammar_correction" to 0
        )
    }

    suspend fun isPregenEnabled(): Boolean = withContext(ioDispatcher) {
        val prefs = dataStore.data.first()
        prefs[KEY_PREF_VERB_PREGEN_ENABLED] ?: false
    }

    fun setPregenEnabled(enabled: Boolean) {
        writeChannel.trySend(WriteOp.SetPregen(enabled))
    }

    suspend fun getLastUsedPlatform(): String = withContext(ioDispatcher) {
        val prefs = dataStore.data.first()
        prefs[KEY_PREF_LAST_USED_PLATFORM] ?: ""
    }

    fun setLastUsedPlatform(platform: String) {
        writeChannel.trySend(WriteOp.SetPlatform(platform))
    }

    fun clearExpiredCaches() {
        writeChannel.trySend(WriteOp.ClearExpiredCaches(30L * 24 * 60 * 60 * 1000))
    }

    fun flushPending() {
        val ack = CompletableDeferred<Unit>()
        writeChannel.trySend(WriteOp.Flush(ack))
    }

    suspend fun flushPendingSync() = withContext(ioDispatcher) {
        val ack = CompletableDeferred<Unit>()
        writeChannel.send(WriteOp.Flush(ack))
        ack.await()
    }

    companion object {
        val KEY_CURRENT_SESSION = stringPreferencesKey("current_session")
        val KEY_PREF_VERB_PREGEN_ENABLED = booleanPreferencesKey("pref_verb_pregen_enabled")
        val KEY_PREF_LAST_USED_PLATFORM = stringPreferencesKey("pref_last_used_platform")
    }
}
