package com.jian.nemo.feature.library.presentation.list

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.DictionarySyncManager
import com.jian.nemo.core.domain.repository.GrammarListSnapshot
import com.jian.nemo.core.domain.repository.LibraryPreloadCache
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordListSnapshot
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WordListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `search query debounces non-blank input, clears immediately, and persists in SavedStateHandle`() = runTest(testDispatcher) {
        val n5Word = Word(id = 1, rawId = "1", japanese = "桜", hiragana = "さくら", chinese = "樱花", level = "N5", repetitionCount = 1)
        val n1Word = Word(id = 2, rawId = "2", japanese = "抽象", hiragana = "ちゅうしょう", chinese = "抽象", level = "N1", repetitionCount = 0)
        val snapshot = WordListSnapshot(
            wordsByLevel = linkedMapOf("N5" to listOf(n5Word), "N1" to listOf(n1Word)),
            countsByLevel = linkedMapOf("N5" to 1, "N1" to 1),
            allWords = listOf(n5Word, n1Word),
            isReady = true
        )

        val cache = FakeLibraryPreloadCache(snapshot)
        val syncManager = mockk<DictionarySyncManager>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>()
        every { settingsRepository.settingIconStyleFlow } returns MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)
        val context = mockk<Context>(relaxed = true)
        val savedStateHandle = SavedStateHandle()

        val viewModel = WordListViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            libraryPreloadCache = cache,
            syncManager = syncManager,
            settingsRepository = settingsRepository,
            filterDispatcher = testDispatcher
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(2, viewModel.uiState.value.allWords.size)

        viewModel.onSearchQueryChanged("さくら")
        assertEquals("さくら", viewModel.searchQuery.value)

        advanceTimeBy(100)
        runCurrent()
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(2, viewModel.uiState.value.allWords.size)

        advanceTimeBy(60)
        advanceUntilIdle()
        assertEquals("さくら", viewModel.uiState.value.searchQuery)
        assertEquals(listOf(n5Word), viewModel.uiState.value.allWords)
        assertEquals(mapOf("N5" to 1), viewModel.uiState.value.countsByLevel)

        viewModel.onSearchQueryChanged("")
        runCurrent()
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertEquals(2, viewModel.uiState.value.allWords.size)

        viewModel.onSearchQueryChanged("さくら")
        viewModel.onSelectedLevelChanged("N5")
        viewModel.onFilterStateChanged(StudyFilter.LEARNED)
        advanceTimeBy(200)
        advanceUntilIdle()

        val restored = WordListViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            libraryPreloadCache = cache,
            syncManager = syncManager,
            settingsRepository = settingsRepository,
            filterDispatcher = testDispatcher
        )
        assertEquals("さくら", restored.searchQuery.value)
        assertTrue(restored.uiState.value.isLoading)
        assertEquals("N5", restored.uiState.value.selectedLevel)
        assertEquals(StudyFilter.LEARNED, restored.uiState.value.filterState)

        val restoredJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            restored.uiState.collect {}
        }
        advanceTimeBy(200)
        advanceUntilIdle()
        assertFalse(restored.uiState.value.isLoading)
        assertEquals(listOf(n5Word), restored.uiState.value.allWords)

        restoredJob.cancel()
        collectJob.cancel()
    }

    private class FakeLibraryPreloadCache(
        initialWords: WordListSnapshot
    ) : LibraryPreloadCache {
        override val wordListSnapshot = MutableStateFlow(initialWords)
        override val grammarListSnapshot = MutableStateFlow(GrammarListSnapshot())
        override val settingIconStyleState = MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)
        override fun warmUp() = Unit
        override fun ensureStartedNow() = Unit
    }
}
