package com.jian.nemo.feature.test.data.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import com.jian.nemo.feature.test.presentation.ability.VerbConjugationQuestion
import com.jian.nemo.feature.test.presentation.ability.VerbConjugationSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VerbConjugationStorageTest {

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

    private fun sampleQuestion() = VerbConjugationQuestion(
        word = "食べる",
        furigana = "たべる",
        meaning = "吃",
        qText = "[たべる]",
        translation = "吃",
        options = listOf("たべます", "たべない", "たべた", "たべろ"),
        correctIndex = 0,
        explanation = "一段动词连用形"
    )

    @Test
    fun `存储会话后可正确恢复`() = runTest {
        val fakeDataStore = FakeDataStore()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val storage = VerbConjugationStorage(
            dataStore = fakeDataStore,
            appScope = backgroundScope,
            ioDispatcher = dispatcher
        )

        val session = VerbConjugationSession(
            level = "N5",
            questions = listOf(sampleQuestion()),
            currentIndex = 0,
            correctCount = 0,
            userAnswers = listOf(0)
        )

        storage.saveSession(session)
        storage.flushPendingSync()

        val restored = storage.restoreSession()
        assertNotNull(restored)
        assertEquals("N5", restored?.level)
        assertEquals(1, restored?.questions?.size)
        assertEquals("食べる", restored?.questions?.first()?.word)
    }

    @Test
    fun `严格 FIFO 写入通道保证 save 紧接 clear 不发生旧会话复活`() = runTest {
        val fakeDataStore = FakeDataStore()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val storage = VerbConjugationStorage(
            dataStore = fakeDataStore,
            appScope = backgroundScope,
            ioDispatcher = dispatcher
        )

        val session = VerbConjugationSession(
            level = "N5",
            questions = listOf(sampleQuestion()),
            currentIndex = 0,
            correctCount = 0
        )

        // 连续快速调用 save 与 clear
        storage.saveSession(session)
        storage.clearSession()
        storage.flushPendingSync()

        val restored = storage.restoreSession()
        assertNull(restored)
    }

    @Test
    fun `缓存池保存与 O1 整型题量计数统计正确`() = runTest {
        val fakeDataStore = FakeDataStore()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val storage = VerbConjugationStorage(
            dataStore = fakeDataStore,
            appScope = backgroundScope,
            ioDispatcher = dispatcher
        )

        val poolN5 = listOf(listOf(sampleQuestion()), listOf(sampleQuestion())) // 2 套 (10题)
        storage.saveCachePool("N5", poolN5)
        storage.flushPendingSync()

        val counts = storage.getGameCacheCounts()
        // 10 题 / 5.0 -> 2
        assertEquals(2, counts["verb_conjugation"])
    }
}
