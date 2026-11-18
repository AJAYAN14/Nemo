package com.jian.nemo.feature.library.presentation.list

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarUsage
import com.jian.nemo.core.domain.repository.DictionarySyncManager
import com.jian.nemo.core.domain.repository.GrammarListSnapshot
import com.jian.nemo.core.domain.repository.GrammarPreloadItem
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
class GrammarListViewModelTest {

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
    fun `grammar search debounces, matches rubi kana, and restores SavedStateHandle`() = runTest(testDispatcher) {
        val g1 = GrammarPreloadItem(
            grammar = Grammar(
                id = 1,
                rawId = "g1",
                grammar = "〜に違[ちが]いない",
                grammarLevel = "N3",
                usages = listOf(
                    GrammarUsage(
                        id = 1,
                        subtype = null,
                        connection = "普通形",
                        explanation = "一定是",
                        notes = null,
                        examples = emptyList()
                    )
                ),
                repetitionCount = 1
            ),
            cleanedTitle = "〜に違いない",
            cleanedFirstExplanation = "一定是"
        )
        val g2 = GrammarPreloadItem(
            grammar = Grammar(
                id = 2,
                rawId = "g2",
                grammar = "〜わけではない",
                grammarLevel = "N2",
                usages = listOf(
                    GrammarUsage(
                        id = 2,
                        subtype = null,
                        connection = "普通形",
                        explanation = "并非",
                        notes = null,
                        examples = emptyList()
                    )
                ),
                repetitionCount = 0
            ),
            cleanedTitle = "〜わけではない",
            cleanedFirstExplanation = "并非"
        )
        val snapshot = GrammarListSnapshot(
            grammarsByLevel = linkedMapOf("N3" to listOf(g1), "N2" to listOf(g2)),
            countsByLevel = linkedMapOf("N3" to 1, "N2" to 1),
            allGrammars = listOf(g1, g2),
            isReady = true
        )

        val cache = FakeLibraryPreloadCache(snapshot)
        val syncManager = mockk<DictionarySyncManager>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>()
        every { settingsRepository.settingIconStyleFlow } returns MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)
        val context = mockk<Context>(relaxed = true)
        val savedStateHandle = SavedStateHandle()

        val viewModel = GrammarListViewModel(
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

        viewModel.onSearchQueryChanged("ちがいない")
        assertEquals("ちがいない", viewModel.searchQuery.value)
        advanceTimeBy(100)
        runCurrent()
        assertEquals("", viewModel.uiState.value.searchQuery)

        advanceTimeBy(60)
        advanceUntilIdle()
        assertEquals("ちがいない", viewModel.uiState.value.searchQuery)
        assertEquals(listOf(g1), viewModel.uiState.value.allGrammars)
        assertEquals(mapOf("N3" to 1), viewModel.uiState.value.countsByLevel)

        viewModel.onSelectedLevelChanged("N3")
        viewModel.onFilterStateChanged(StudyFilter.LEARNED)
        advanceUntilIdle()

        val restored = GrammarListViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            libraryPreloadCache = cache,
            syncManager = syncManager,
            settingsRepository = settingsRepository,
            filterDispatcher = testDispatcher
        )
        assertEquals("ちがいない", restored.searchQuery.value)
        assertTrue(restored.uiState.value.isLoading)
        assertEquals("N3", restored.uiState.value.selectedLevel)
        assertEquals(StudyFilter.LEARNED, restored.uiState.value.filterState)

        val restoredJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            restored.uiState.collect {}
        }
        advanceTimeBy(200)
        advanceUntilIdle()
        assertFalse(restored.uiState.value.isLoading)
        assertEquals(listOf(g1), restored.uiState.value.allGrammars)

        restoredJob.cancel()
        collectJob.cancel()
    }

    private class FakeLibraryPreloadCache(
        initialGrammars: GrammarListSnapshot
    ) : LibraryPreloadCache {
        override val wordListSnapshot = MutableStateFlow(WordListSnapshot())
        override val grammarListSnapshot = MutableStateFlow(initialGrammars)
        override val settingIconStyleState = MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)
        override fun warmUp() = Unit
        override fun ensureStartedNow() = Unit
    }
}
