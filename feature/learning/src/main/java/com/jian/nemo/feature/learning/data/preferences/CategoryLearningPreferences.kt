package com.jian.nemo.feature.learning.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.jian.nemo.core.common.di.ApplicationScope
import com.jian.nemo.core.common.network.Dispatcher
import com.jian.nemo.core.common.network.NemoDispatchers
import com.jian.nemo.feature.learning.di.CategoryPreferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface CategoryLearningPreferences {
    suspend fun getLastIndex(categoryId: String): Int
    fun saveLastIndex(categoryId: String, index: Int)
    fun flushPending()
}

@Singleton
class CategoryLearningPreferencesImpl @Inject constructor(
    @CategoryPreferencesDataStore private val dataStore: DataStore<Preferences>,
    @ApplicationScope private val appScope: CoroutineScope,
    @Dispatcher(NemoDispatchers.IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CategoryLearningPreferences {

    private val indexCache = ConcurrentHashMap<String, Int>()
    private val pendingWrites = ConcurrentHashMap<String, Int>()
    private var debounceJob: Job? = null
    private val debounceLock = Any()

    override suspend fun getLastIndex(categoryId: String): Int {
        indexCache[categoryId]?.let { return it }
        val key = intPreferencesKey(getKey(categoryId))
        val value = withContext(ioDispatcher) {
            val prefs = dataStore.data.first()
            prefs[key] ?: 0
        }
        indexCache[categoryId] = value
        return value
    }

    override fun saveLastIndex(categoryId: String, index: Int) {
        indexCache[categoryId] = index
        pendingWrites[categoryId] = index

        synchronized(debounceLock) {
            debounceJob?.cancel()
            debounceJob = appScope.launch(ioDispatcher) {
                delay(300)
                flushInternal()
            }
        }
    }

    override fun flushPending() {
        synchronized(debounceLock) {
            debounceJob?.cancel()
            debounceJob = null
        }
        appScope.launch(NonCancellable + ioDispatcher) {
            flushInternal()
        }
    }

    private suspend fun flushInternal() {
        if (pendingWrites.isEmpty()) return
        val snapshot = HashMap(pendingWrites)
        dataStore.edit { prefs ->
            snapshot.forEach { (catId, idx) ->
                prefs[intPreferencesKey(getKey(catId))] = idx
            }
        }
        snapshot.forEach { (catId, idx) ->
            pendingWrites.remove(catId, idx)
        }
    }

    private fun getKey(categoryId: String): String = "last_index_$categoryId"
}
