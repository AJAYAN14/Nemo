package com.jian.nemo.feature.learning.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryLearningPreferencesTest {

    private class FakeDataStore(
        initialPrefs: Preferences = preferencesOf()
    ) : DataStore<Preferences> {
        val state = MutableStateFlow(initialPrefs)
        var editCount = 0

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            editCount++
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    @Test
    fun `首次加载读取 DataStore，二次读取直接命中内存缓存`() = runTest {
        val fakeDataStore = FakeDataStore(
            preferencesOf(intPreferencesKey("last_index_n5") to 15)
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val prefs = CategoryLearningPreferencesImpl(
            dataStore = fakeDataStore,
            appScope = this,
            ioDispatcher = dispatcher
        )

        val first = prefs.getLastIndex("n5")
        assertEquals(15, first)

        // 外部篡改 DataStore 底层值
        fakeDataStore.state.value = preferencesOf(intPreferencesKey("last_index_n5") to 99)

        // 再次获取应直接命中内存缓存 15，避免重复读盘
        val second = prefs.getLastIndex("n5")
        assertEquals(15, second)
    }

    @Test
    fun `连续翻卡 300ms 防抖合并多次写入，并在 flushPending 时立即落盘`() = runTest {
        val fakeDataStore = FakeDataStore()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val prefs = CategoryLearningPreferencesImpl(
            dataStore = fakeDataStore,
            appScope = this,
            ioDispatcher = dispatcher
        )

        // 快速连续翻卡
        prefs.saveLastIndex("n5", 1)
        prefs.saveLastIndex("n5", 2)
        prefs.saveLastIndex("n5", 3)

        // 内存值立即可见
        assertEquals(3, prefs.getLastIndex("n5"))

        // 100ms 尚未落盘
        advanceTimeBy(100)
        assertEquals(0, fakeDataStore.editCount)

        // 触发 flushPending，立即落盘
        prefs.flushPending()
        advanceTimeBy(10)

        assertEquals(1, fakeDataStore.editCount)
        assertEquals(3, fakeDataStore.state.value[intPreferencesKey("last_index_n5")])
    }
}
