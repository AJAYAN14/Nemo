package com.jian.nemo.feature.library.presentation.category

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.usecase.word.GetWordsByPartOfSpeechUseCase
import com.jian.nemo.feature.library.presentation.components.computeAllLevelsCount
import com.jian.nemo.feature.library.presentation.list.StudyFilter
import com.jian.nemo.feature.library.presentation.list.WordListUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
class CategoryWordsViewModelTest {

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
    fun `buildCategoryWordsUiState counts only JLPT levels and never duplicates ALL`() {
        val words = listOf(
            sampleWord(1, "猫", "ねこ", "猫", "N5", repetitionCount = 1),
            sampleWord(2, "犬", "いぬ", "狗", "N5", repetitionCount = 0),
            sampleWord(3, "抽象", "ちゅうしょう", "抽象", "N1", repetitionCount = 2)
        )

        val state = buildCategoryWordsUiState(
            isLoading = false,
            words = words,
            error = null,
            query = "",
            filter = StudyFilter.ALL,
            selectedLevel = WordListUiState.LEVEL_ALL
        )

        assertFalse(state.countsByLevel.containsKey("ALL"))
        assertEquals(mapOf("N5" to 2, "N1" to 1), state.countsByLevel)
        assertEquals(3, computeAllLevelsCount(state.countsByLevel))
        assertEquals(3, state.filteredWords.size)
    }

    @Test
    fun `buildCategoryWordsUiState filters by study filter, level, and query`() {
        val words = listOf(
            sampleWord(1, "猫", "ねこ", "猫", "N5", repetitionCount = 1),
            sampleWord(2, "犬", "いぬ", "狗", "N5", repetitionCount = 0),
            sampleWord(3, "抽象", "ちゅうしょう", "抽象", "N1", repetitionCount = 2)
        )

        val learnedN1 = buildCategoryWordsUiState(
            isLoading = false,
            words = words,
            error = null,
            query = "",
            filter = StudyFilter.LEARNED,
            selectedLevel = "N1"
        )
        assertEquals(listOf(words[2]), learnedN1.filteredWords)
        assertEquals(mapOf("N5" to 1, "N1" to 1), learnedN1.countsByLevel)

        val queryMatched = buildCategoryWordsUiState(
            isLoading = false,
            words = words,
            error = null,
            query = "ねこ",
            filter = StudyFilter.ALL,
            selectedLevel = WordListUiState.LEVEL_ALL
        )
        assertEquals(listOf(words[0]), queryMatched.filteredWords)
        assertEquals(mapOf("N5" to 1), queryMatched.countsByLevel)
    }

    @Test
    fun `viewModel persists filter state in SavedStateHandle and applies debounced search`() = runTest(testDispatcher) {
        val context = mockk<Context>(relaxed = true)
        val useCase = mockk<GetWordsByPartOfSpeechUseCase>()
        val settingsRepository = mockk<SettingsRepository>()
        every { settingsRepository.settingIconStyleFlow } returns MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)
        val words = listOf(
            sampleWord(1, "猫", "ねこ", "猫", "N5", repetitionCount = 1),
            sampleWord(2, "犬", "いぬ", "狗", "N4", repetitionCount = 0)
        )
        coEvery { useCase(PartOfSpeech.NOUN) } returns words

        val savedStateHandle = SavedStateHandle()
        val viewModel = CategoryWordsViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            getWordsByPartOfSpeechUseCase = useCase,
            settingsRepository = settingsRepository,
            filterDispatcher = testDispatcher
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.loadWords("noun")
        advanceUntilIdle()

        viewModel.onFilterStateChanged(StudyFilter.LEARNED)
        viewModel.onSelectedLevelChanged("N5")
        viewModel.onSearchQueryChanged("ねこ")
        advanceTimeBy(100)
        runCurrent()
        assertEquals("", viewModel.uiState.value.searchQuery)

        advanceTimeBy(60)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(words[0]), state.filteredWords)
        assertEquals("N5", state.selectedLevel)
        assertEquals(StudyFilter.LEARNED, state.filterState)
        assertEquals(1, computeAllLevelsCount(state.countsByLevel))

        val restoredViewModel = CategoryWordsViewModel(
            context = context,
            savedStateHandle = savedStateHandle,
            getWordsByPartOfSpeechUseCase = useCase,
            settingsRepository = settingsRepository,
            filterDispatcher = testDispatcher
        )
        assertEquals("ねこ", restoredViewModel.searchQuery.value)
        assertEquals("N5", restoredViewModel.uiState.value.selectedLevel)
        assertEquals(StudyFilter.LEARNED, restoredViewModel.uiState.value.filterState)

        collectJob.cancel()
    }

    @Test
    fun `loadWords skips reloading empty category and cancels stale concurrent request`() = runTest(testDispatcher) {
        val context = mockk<Context>(relaxed = true)
        val useCase = mockk<GetWordsByPartOfSpeechUseCase>()
        val settingsRepository = mockk<SettingsRepository>()
        every { settingsRepository.settingIconStyleFlow } returns MutableStateFlow(SettingIconStyle.SOFT_TRANSLUCENT)

        val slowNoun = CompletableDeferred<List<Word>>()
        val verbWord = sampleWord(9, "食べる", "たべる", "吃", "N5", repetitionCount = 0)
        coEvery { useCase(PartOfSpeech.NOUN) } coAnswers { slowNoun.await() }
        coEvery { useCase(PartOfSpeech.VERB) } returns listOf(verbWord)
        coEvery { useCase(PartOfSpeech.INTERJECTION) } returns emptyList()

        val viewModel = CategoryWordsViewModel(
            context = context,
            savedStateHandle = SavedStateHandle(),
            getWordsByPartOfSpeechUseCase = useCase,
            settingsRepository = settingsRepository,
            filterDispatcher = testDispatcher
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.loadWords("noun")
        runCurrent()
        viewModel.loadWords("verb")
        advanceUntilIdle()
        slowNoun.complete(listOf(sampleWord(1, "猫", "ねこ", "猫", "N5", repetitionCount = 0)))
        advanceUntilIdle()

        assertEquals(listOf(verbWord), viewModel.uiState.value.filteredWords)

        viewModel.loadWords("exclam")
        advanceUntilIdle()
        viewModel.loadWords("exclam")
        advanceUntilIdle()

        coVerify(exactly = 1) { useCase(PartOfSpeech.INTERJECTION) }
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.filteredWords.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun `buildCategoryWordsUiState returns empty list on error or loading`() {
        val loadingState = buildCategoryWordsUiState(
            isLoading = true,
            words = emptyList(),
            error = null,
            query = "",
            filter = StudyFilter.ALL,
            selectedLevel = WordListUiState.LEVEL_ALL
        )
        assertTrue(loadingState.isLoading)
        assertTrue(loadingState.filteredWords.isEmpty())

        val errorState = buildCategoryWordsUiState(
            isLoading = false,
            words = emptyList(),
            error = "network",
            query = "",
            filter = StudyFilter.ALL,
            selectedLevel = WordListUiState.LEVEL_ALL
        )
        assertEquals("network", errorState.error)
        assertTrue(errorState.countsByLevel.isEmpty())
    }

    private fun sampleWord(
        id: Int,
        japanese: String,
        hiragana: String,
        chinese: String,
        level: String,
        repetitionCount: Int
    ): Word = Word(
        id = id,
        rawId = "raw_$id",
        japanese = japanese,
        hiragana = hiragana,
        chinese = chinese,
        level = level,
        repetitionCount = repetitionCount
    )
}
