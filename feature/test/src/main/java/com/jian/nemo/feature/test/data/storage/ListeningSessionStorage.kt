package com.jian.nemo.feature.test.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jian.nemo.core.common.di.ApplicationScope
import com.jian.nemo.core.common.network.Dispatcher
import com.jian.nemo.core.common.network.NemoDispatchers
import com.jian.nemo.feature.test.di.ListeningSessionDataStore
import com.jian.nemo.feature.test.presentation.ability.ListeningSession
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
class ListeningSessionStorage @Inject constructor(
    @ListeningSessionDataStore private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val appScope: CoroutineScope,
    @Dispatcher(NemoDispatchers.IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val json = Json { ignoreUnknownKeys = true }

    private sealed interface WriteOp {
        data class SaveSession(val session: ListeningSession) : WriteOp
        data object ClearSession : WriteOp
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

    suspend fun restoreSession(): ListeningSession? = withContext(ioDispatcher) {
        try {
            val prefs = dataStore.data.first()
            val raw = prefs[KEY_CURRENT_SESSION] ?: return@withContext null
            json.decodeFromString<ListeningSession>(raw)
        } catch (e: Exception) {
            clearSession()
            null
        }
    }

    fun saveSession(session: ListeningSession) {
        writeChannel.trySend(WriteOp.SaveSession(session))
    }

    fun clearSession() {
        writeChannel.trySend(WriteOp.ClearSession)
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
    }
}
