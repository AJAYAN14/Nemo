package com.jian.nemo.feature.test.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import com.jian.nemo.feature.test.presentation.ability.ListeningQuestion
import com.jian.nemo.feature.test.presentation.ability.ListeningSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListeningSessionStorageTest {

    private class FakeDataStore(
        initialPrefs: Preferences = preferencesOf()
    ) : DataStore<Preferences> {
        val state = MutableStateFlow(initialPrefs)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private fun sampleQuestion() = ListeningQuestion(
        word = "犬",
        hiragana = "いぬ",
        meaning = "狗",
        questionType = "IDENTIFY_WORD",
        options = listOf("犬", "猫", "鳥", "魚"),
        correctIndex = 0,
        explanation = "解析"
    )

    @Test
    fun `听力会话持久化与恢复正常`() = runTest {
        val fakeDataStore = FakeDataStore()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val storage = ListeningSessionStorage(
            dataStore = fakeDataStore,
            appScope = backgroundScope,
            ioDispatcher = dispatcher
        )

        val session = ListeningSession(
            level = "N5",
            questions = listOf(sampleQuestion()),
            currentIndex = 0,
            correctCount = 1,
            userAnswers = listOf(0)
        )

        storage.saveSession(session)
        storage.flushPendingSync()

        val restored = storage.restoreSession()
        assertNotNull(restored)
        assertEquals("N5", restored?.level)
        assertEquals("犬", restored?.questions?.first()?.word)
        assertEquals(1, restored?.correctCount)
    }

    @Test
    fun `先保存再清除后恢复为空`() = runTest {
        val fakeDataStore = FakeDataStore()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val storage = ListeningSessionStorage(
            dataStore = fakeDataStore,
            appScope = backgroundScope,
            ioDispatcher = dispatcher
        )

        val session = ListeningSession(
            level = "N4",
            questions = listOf(sampleQuestion()),
            currentIndex = 0,
            correctCount = 0
        )

        storage.saveSession(session)
        storage.clearSession()
        storage.flushPendingSync()

        val restored = storage.restoreSession()
        assertNull(restored)
    }
}
