package com.jian.nemo.core.data.cache

import com.jian.nemo.core.data.local.dao.GrammarDao
import com.jian.nemo.core.data.local.dao.WordDao
import com.jian.nemo.core.data.local.dao.WordStudyStateDao
import com.jian.nemo.core.data.local.entity.GrammarEntity
import com.jian.nemo.core.data.local.entity.GrammarUsageEntity
import com.jian.nemo.core.data.local.entity.WordEntity
import com.jian.nemo.core.data.local.entity.WordStudyStateEntity
import com.jian.nemo.core.data.local.entity.relations.GrammarWithUsages
import com.jian.nemo.core.data.local.entity.relations.UsageWithExamples
import com.jian.nemo.core.domain.repository.DictionarySyncManager
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLibraryPreloadCacheTest {

    @Test
    fun `warmUp waits for dictionary ready then builds normalized word and grammar snapshots`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val appScope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            val wordDao = mockk<WordDao>()
            val wordStudyStateDao = mockk<WordStudyStateDao>()
            val grammarDao = mockk<GrammarDao>()
            val settingsRepository = mockk<SettingsRepository>()
            val syncManager = mockk<DictionarySyncManager>()

            val readyFlow = MutableStateFlow(false)
            val wordsFlow = MutableSharedFlow<List<WordEntity>>(replay = 1)
            val grammarsFlow = MutableSharedFlow<List<GrammarWithUsages>>(replay = 1)
            val iconStyleFlow = MutableStateFlow(SettingIconStyle.VIBRANT_SOLID)

            coEvery { syncManager.isDictionaryReady() } returns false
            every { syncManager.isDictionaryReadyFlow } returns readyFlow
            every { wordDao.observeAllActiveWords() } returns wordsFlow
            coEvery { wordStudyStateDao.getAllSync() } returns listOf(
                WordStudyStateEntity(wordId = 2, repetitionCount = 3)
            )
            every { grammarDao.observeAllActiveGrammarsWithUsages() } returns grammarsFlow
            every { settingsRepository.settingIconStyleFlow } returns iconStyleFlow

            wordsFlow.tryEmit(
                listOf(
                    WordEntity(id = 1, rawId = "w1", japanese = "猫", hiragana = "ねこ", chinese = "猫", level = "n5"),
                    WordEntity(id = 2, rawId = "w2", japanese = "桜", hiragana = "さくら", chinese = "樱花", level = "N1"),
                    WordEntity(id = 3, rawId = "w3", japanese = "未知", hiragana = "みち", chinese = "过滤测试", level = "EXTRA")
                )
            )
            grammarsFlow.tryEmit(
                listOf(
                    GrammarWithUsages(
                        grammar = GrammarEntity(id = 11, grammar = "〜に違[ちが]いない", grammarLevel = "n3"),
                        usages = listOf(
                            UsageWithExamples(
                                usage = GrammarUsageEntity(
                                    id = 101,
                                    grammarId = 11,
                                    subtype = null,
                                    connection = "N",
                                    explanation = "表示必[かなら]ず如此",
                                    notes = null,
                                    usageOrder = 0
                                ),
                                examples = emptyList()
                            )
                        ),
                        state = null
                    )
                )
            )

            val cache = DefaultLibraryPreloadCache(
                wordDao = wordDao,
                wordStudyStateDao = wordStudyStateDao,
                grammarDao = grammarDao,
                settingsRepository = settingsRepository,
                dictionarySyncManager = syncManager,
                applicationScope = appScope,
                ioDispatcher = dispatcher,
                defaultDispatcher = dispatcher,
                debounceMillis = 200L
            )

            cache.warmUp()
            advanceUntilIdle()
            assertFalse(cache.wordListSnapshot.value.isReady)
            assertFalse(cache.grammarListSnapshot.value.isReady)

            readyFlow.value = true
            advanceUntilIdle()

            val wordSnapshot = cache.wordListSnapshot.value
            assertTrue(wordSnapshot.isReady)
            assertEquals(listOf("N1", "N5"), wordSnapshot.wordsByLevel.keys.toList())
            assertEquals(mapOf("N1" to 1, "N5" to 1), wordSnapshot.countsByLevel)
            assertEquals(listOf(2, 1), wordSnapshot.allWords.map { it.id })
            assertTrue(wordSnapshot.allWords.first { it.id == 2 }.isLearned)
            assertFalse(wordSnapshot.allWords.first { it.id == 1 }.isLearned)

            val grammarSnapshot = cache.grammarListSnapshot.value
            assertTrue(grammarSnapshot.isReady)
            assertEquals(listOf("N3"), grammarSnapshot.grammarsByLevel.keys.toList())
            val item = grammarSnapshot.allGrammars.single()
            assertEquals("〜に違いない", item.cleanedTitle)
            assertEquals("表示必ず如此", item.cleanedFirstExplanation)
            assertEquals(SettingIconStyle.VIBRANT_SOLID, cache.settingIconStyleState.value)
        } finally {
            appScope.cancel()
        }
    }

    @Test
    fun `ensureStartedNow is idempotent and subsequent bursts are debounced`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val appScope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            val wordDao = mockk<WordDao>()
            val wordStudyStateDao = mockk<WordStudyStateDao>()
            val grammarDao = mockk<GrammarDao>()
            val settingsRepository = mockk<SettingsRepository>()
            val syncManager = mockk<DictionarySyncManager>()

            val wordsFlow = MutableSharedFlow<List<WordEntity>>(replay = 1)
            val grammarsFlow = MutableSharedFlow<List<GrammarWithUsages>>(replay = 1)

            coEvery { syncManager.isDictionaryReady() } returns true
            every { syncManager.isDictionaryReadyFlow } returns MutableStateFlow(true)
            every { wordDao.observeAllActiveWords() } returns wordsFlow
            coEvery { wordStudyStateDao.getAllSync() } returns emptyList() andThen listOf(
                WordStudyStateEntity(wordId = 1, repetitionCount = 1)
            )
            every { grammarDao.observeAllActiveGrammarsWithUsages() } returns grammarsFlow
            every { settingsRepository.settingIconStyleFlow } returns MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)

            wordsFlow.tryEmit(listOf(WordEntity(id = 1, rawId = "w1", japanese = "雨", hiragana = "あめ", chinese = "雨", level = "N5")))
            grammarsFlow.tryEmit(emptyList())

            val cache = DefaultLibraryPreloadCache(
                wordDao = wordDao,
                wordStudyStateDao = wordStudyStateDao,
                grammarDao = grammarDao,
                settingsRepository = settingsRepository,
                dictionarySyncManager = syncManager,
                applicationScope = appScope,
                ioDispatcher = dispatcher,
                defaultDispatcher = dispatcher,
                debounceMillis = 200L
            )

            cache.ensureStartedNow()
            cache.ensureStartedNow()
            cache.warmUp()
            advanceUntilIdle()

            verify(exactly = 1) { wordDao.observeAllActiveWords() }
            verify(exactly = 1) { grammarDao.observeAllActiveGrammarsWithUsages() }
            assertFalse(cache.wordListSnapshot.value.allWords.single().isLearned)

            // 连续高频发射两次，防抖窗口内只采纳最终结果
            wordsFlow.emit(listOf(WordEntity(id = 1, rawId = "w1", japanese = "雨", hiragana = "あめ", chinese = "雨", level = "N5")))
            advanceTimeBy(100L)
            assertFalse(cache.wordListSnapshot.value.allWords.single().isLearned)

            wordsFlow.emit(listOf(WordEntity(id = 1, rawId = "w1", japanese = "雨", hiragana = "あめ", chinese = "雨", level = "N5")))
            advanceTimeBy(199L)
            assertFalse(cache.wordListSnapshot.value.allWords.single().isLearned)

            advanceTimeBy(2L)
            advanceUntilIdle()
            assertTrue(cache.wordListSnapshot.value.allWords.single().isLearned)
        } finally {
            appScope.cancel()
        }
    }

    @Test
    fun `dao failure emits empty ready snapshot instead of stalling loading forever`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val appScope = CoroutineScope(SupervisorJob() + dispatcher)
        try {
            val wordDao = mockk<WordDao>()
            val wordStudyStateDao = mockk<WordStudyStateDao>()
            val grammarDao = mockk<GrammarDao>()
            val settingsRepository = mockk<SettingsRepository>()
            val syncManager = mockk<DictionarySyncManager>()

            coEvery { syncManager.isDictionaryReady() } returns true
            every { syncManager.isDictionaryReadyFlow } returns MutableStateFlow(true)
            every { wordDao.observeAllActiveWords() } returns flow { throw IllegalStateException("db broken") }
            every { grammarDao.observeAllActiveGrammarsWithUsages() } returns flow { throw IllegalStateException("db broken") }
            every { settingsRepository.settingIconStyleFlow } returns MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)

            val cache = DefaultLibraryPreloadCache(
                wordDao = wordDao,
                wordStudyStateDao = wordStudyStateDao,
                grammarDao = grammarDao,
                settingsRepository = settingsRepository,
                dictionarySyncManager = syncManager,
                applicationScope = appScope,
                ioDispatcher = dispatcher,
                defaultDispatcher = dispatcher,
                debounceMillis = 200L
            )

            cache.ensureStartedNow()
            advanceUntilIdle()

            assertTrue(cache.wordListSnapshot.value.isReady)
            assertTrue(cache.wordListSnapshot.value.allWords.isEmpty())
            assertTrue(cache.grammarListSnapshot.value.isReady)
            assertTrue(cache.grammarListSnapshot.value.allGrammars.isEmpty())
        } finally {
            appScope.cancel()
        }
    }
}
