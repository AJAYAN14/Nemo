package com.jian.nemo.feature.library.presentation.category

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.usecase.word.GetWordsByPartOfSpeechUseCase
import com.jian.nemo.feature.library.R
import com.jian.nemo.feature.library.presentation.list.StudyFilter
import com.jian.nemo.feature.library.presentation.list.WordListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class CategoryRawDataState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val words: List<Word> = emptyList(),
    val error: String? = null
)

data class CategoryWordsUiState(
    val isLoading: Boolean = false,
    val words: List<Word> = emptyList(),
    val filteredWords: List<Word> = emptyList(),
    val countsByLevel: Map<String, Int> = emptyMap(),
    val searchQuery: String = "",
    val filterState: StudyFilter = StudyFilter.ALL,
    val selectedLevel: String = WordListUiState.LEVEL_ALL,
    val error: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class CategoryWordsViewModel internal constructor(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val getWordsByPartOfSpeechUseCase: GetWordsByPartOfSpeechUseCase,
    private val settingsRepository: SettingsRepository,
    private val filterDispatcher: CoroutineDispatcher
) : ViewModel() {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        savedStateHandle: SavedStateHandle,
        getWordsByPartOfSpeechUseCase: GetWordsByPartOfSpeechUseCase,
        settingsRepository: SettingsRepository
    ) : this(
        context = context,
        savedStateHandle = savedStateHandle,
        getWordsByPartOfSpeechUseCase = getWordsByPartOfSpeechUseCase,
        settingsRepository = settingsRepository,
        filterDispatcher = Dispatchers.Default
    )

    val iconStyle: StateFlow<SettingIconStyle> = settingsRepository.settingIconStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingIconStyle.SOFT_TRANSLUCENT
        )

    private val rawDataState = MutableStateFlow(CategoryRawDataState(isLoading = true))

    private val _searchQuery = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SEARCH_QUERY).orEmpty()
    )
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterState = MutableStateFlow(
        savedStateHandle.get<String>(KEY_STUDY_FILTER)
            ?.let { runCatching { StudyFilter.valueOf(it) }.getOrNull() }
            ?: StudyFilter.ALL
    )

    private val _selectedLevel = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SELECTED_LEVEL) ?: WordListUiState.LEVEL_ALL
    )

    private val debouncedSearchQuery = _searchQuery
        .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()

    val uiState: StateFlow<CategoryWordsUiState> = combine(
        rawDataState,
        debouncedSearchQuery,
        _filterState,
        _selectedLevel
    ) { raw, query, filter, selectedLevel ->
        buildCategoryWordsUiState(
            isLoading = raw.isLoading,
            words = raw.words,
            error = raw.error,
            query = query,
            filter = filter,
            selectedLevel = selectedLevel
        )
    }
        .flowOn(filterDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CategoryWordsUiState(
                isLoading = true,
                searchQuery = _searchQuery.value,
                filterState = _filterState.value,
                selectedLevel = _selectedLevel.value
            )
        )

    private var currentCategory: String = ""
    private var loadJob: Job? = null

    fun loadWords(category: String) {
        if (currentCategory == category && rawDataState.value.hasLoaded && rawDataState.value.error == null) {
            return
        }

        currentCategory = category
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            rawDataState.value = CategoryRawDataState(isLoading = true, hasLoaded = false)
            try {
                val pos = mapCategoryToPos(category)
                val words = if (pos != null) {
                    getWordsByPartOfSpeechUseCase(pos)
                } else {
                    emptyList()
                }
                rawDataState.value = CategoryRawDataState(
                    isLoading = false,
                    hasLoaded = true,
                    words = words,
                    error = null
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                rawDataState.value = CategoryRawDataState(
                    isLoading = false,
                    hasLoaded = false,
                    words = emptyList(),
                    error = context.getString(R.string.library_category_words_format_load_failed, e.message ?: "")
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun onFilterStateChanged(filter: StudyFilter) {
        _filterState.value = filter
        savedStateHandle[KEY_STUDY_FILTER] = filter.name
    }

    fun onSelectedLevelChanged(level: String) {
        _selectedLevel.value = level
        savedStateHandle[KEY_SELECTED_LEVEL] = level
    }

    fun refresh() {
        if (currentCategory.isNotEmpty()) {
            val category = currentCategory
            currentCategory = ""
            loadWords(category)
        }
    }

    private fun mapCategoryToPos(category: String): PartOfSpeech? {
        return when (category) {
            "noun" -> PartOfSpeech.NOUN
            "adj" -> PartOfSpeech.ADJECTIVE
            "verb" -> PartOfSpeech.VERB
            "adv" -> PartOfSpeech.ADVERB
            "rentai" -> PartOfSpeech.RENTAI
            "conj" -> PartOfSpeech.CONJUNCTION
            "exclam" -> PartOfSpeech.INTERJECTION
            "particle" -> PartOfSpeech.PARTICLE
            "prefix" -> PartOfSpeech.PREFIX
            "suffix" -> PartOfSpeech.SUFFIX
            "expression" -> PartOfSpeech.FIXED_EXPRESSION
            "kata" -> PartOfSpeech.LOAN_WORD
            else -> null
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 150L
        const val KEY_SEARCH_QUERY = "category_words_search_query"
        const val KEY_STUDY_FILTER = "category_words_study_filter"
        const val KEY_SELECTED_LEVEL = "category_words_selected_level"
    }
}

internal fun buildCategoryWordsUiState(
    isLoading: Boolean,
    words: List<Word>,
    error: String?,
    query: String,
    filter: StudyFilter,
    selectedLevel: String
): CategoryWordsUiState {
    if (isLoading || error != null) {
        return CategoryWordsUiState(
            isLoading = isLoading,
            words = words,
            filteredWords = emptyList(),
            countsByLevel = emptyMap(),
            searchQuery = query,
            filterState = filter,
            selectedLevel = selectedLevel,
            error = error
        )
    }

    val counts = LinkedHashMap<String, Int>(5)
    val filteredWords = ArrayList<Word>(words.size)
    val hasQuery = query.isNotBlank()
    val isAllLevel = selectedLevel.equals(WordListUiState.LEVEL_ALL, ignoreCase = true)

    for (word in words) {
        val matchesFilter = when (filter) {
            StudyFilter.ALL -> true
            StudyFilter.LEARNED -> word.isLearned
            StudyFilter.UNLEARNED -> !word.isLearned
        }
        if (!matchesFilter) continue

        val matchesQuery = !hasQuery ||
            word.japanese.contains(query, ignoreCase = true) ||
            word.hiragana.contains(query, ignoreCase = true) ||
            word.chinese.contains(query, ignoreCase = true)
        if (!matchesQuery) continue

        val normalizedLevel = word.level.uppercase()
        if (normalizedLevel.isNotEmpty()) {
            counts[normalizedLevel] = (counts[normalizedLevel] ?: 0) + 1
        }

        if (isAllLevel || normalizedLevel.equals(selectedLevel, ignoreCase = true)) {
            filteredWords.add(word)
        }
    }

    return CategoryWordsUiState(
        isLoading = false,
        words = words,
        filteredWords = filteredWords,
        countsByLevel = counts,
        searchQuery = query,
        filterState = filter,
        selectedLevel = selectedLevel,
        error = null
    )
}
