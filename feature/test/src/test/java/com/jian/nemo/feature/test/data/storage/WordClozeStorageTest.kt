package com.jian.nemo.feature.test.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import com.jian.nemo.feature.test.presentation.ability.ClozeQuestion
import com.jian.nemo.feature.test.presentation.ability.ClozeSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WordClozeStorageTest {

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

    private fun sampleQuestion() = ClozeQuestion(
        wordId = 1,
        japanese = "猫",
        hiragana = "ねこ",
        chinese = "猫",
        maskIndices = listOf(0),
        explanation = "解释"
    )

    @Test
    fun `填空会话保存且保留 questionCorrectStates`() = runTest {
        val fakeDataStore = FakeDataStore()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val storage = WordClozeStorage(
            dataStore = fakeDataStore,
            appScope = backgroundScope,
            ioDispatcher = dispatcher
        )

        val session = ClozeSession(
            level = "N5",
            questions = listOf(sampleQuestion()),
            currentIndex = 0,
            correctCount = 1,
            userInputs = listOf(listOf("ね")),
            questionCorrectStates = listOf(true)
        )

        storage.saveSession(session)
        storage.flushPendingSync()

        val restored = storage.restoreSession()
        assertNotNull(restored)
        assertEquals("N5", restored?.level)
        assertEquals(listOf(true), restored?.questionCorrectStates)
        assertEquals("ね", restored?.userInputs?.first()?.first())
    }

    @Test
    fun `按键输入防抖合并与 flushPending 立即冲刷`() = runTest {
        val fakeDataStore = FakeDataStore()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val storage = WordClozeStorage(
            dataStore = fakeDataStore,
            appScope = backgroundScope,
            ioDispatcher = dispatcher
        )

        val session1 = ClozeSession("N5", listOf(sampleQuestion()), 0, 0, listOf(listOf("a")))
        val session2 = ClozeSession("N5", listOf(sampleQuestion()), 0, 0, listOf(listOf("ab")))

        // 连续按键防抖
        storage.saveSession(session1, debounceMs = 350L)
        advanceTimeBy(100)
        storage.saveSession(session2, debounceMs = 350L)

        // 200ms 尚未写入底层 DataStore
        advanceTimeBy(200)
        assertEquals(0, fakeDataStore.editCount)

        // 冲刷管道
        storage.flushPendingSync()
        assertEquals(1, fakeDataStore.editCount)

        val restored = storage.restoreSession()
        assertEquals("ab", restored?.userInputs?.first()?.first())
    }
}
