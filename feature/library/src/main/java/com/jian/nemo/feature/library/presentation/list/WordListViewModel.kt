package com.jian.nemo.feature.library.presentation.list

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.DictionarySyncManager
import com.jian.nemo.core.domain.repository.LibraryPreloadCache
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordListSnapshot
import com.jian.nemo.feature.library.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
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

enum class StudyFilter {
    ALL,
    LEARNED,
    UNLEARNED
}

data class WordListUiState(
    val isLoading: Boolean = true,
    val wordsByLevel: Map<String, List<Word>> = emptyMap(),
    val allWords: List<Word> = emptyList(),
    val countsByLevel: Map<String, Int> = emptyMap(),
    val error: String? = null,
    val searchQuery: String = "",
    val filterState: StudyFilter = StudyFilter.ALL,
    val selectedLevel: String = LEVEL_ALL,
    val isRefreshing: Boolean = false,
    val syncMessage: String? = null
) {
    companion object {
        const val LEVEL_ALL = "ALL"
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class WordListViewModel internal constructor(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val libraryPreloadCache: LibraryPreloadCache,
    private val syncManager: DictionarySyncManager,
    private val settingsRepository: SettingsRepository,
    private val filterDispatcher: CoroutineDispatcher
) : ViewModel() {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        savedStateHandle: SavedStateHandle,
        libraryPreloadCache: LibraryPreloadCache,
        syncManager: DictionarySyncManager,
        settingsRepository: SettingsRepository
    ) : this(
        context = context,
        savedStateHandle = savedStateHandle,
        libraryPreloadCache = libraryPreloadCache,
        syncManager = syncManager,
        settingsRepository = settingsRepository,
        filterDispatcher = Dispatchers.Default
    )

    init {
        libraryPreloadCache.ensureStartedNow()
    }

    val iconStyle: StateFlow<SettingIconStyle> = settingsRepository.settingIconStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = libraryPreloadCache.settingIconStyleState.value
        )

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

    private val _isRefreshing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)

    private val debouncedSearchQuery = _searchQuery
        .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()

    private val initialUiState: WordListUiState = run {
        val snapshot = libraryPreloadCache.wordListSnapshot.value
        val query = _searchQuery.value
        val filter = _filterState.value
        val level = _selectedLevel.value
        if (snapshot.isReady && query.isBlank() && filter == StudyFilter.ALL) {
            WordListUiState(
                isLoading = false,
                wordsByLevel = snapshot.wordsByLevel,
                allWords = snapshot.allWords,
                countsByLevel = snapshot.countsByLevel,
                searchQuery = query,
                filterState = filter,
                selectedLevel = level
            )
        } else {
            WordListUiState(
                isLoading = true,
                searchQuery = query,
                filterState = filter,
                selectedLevel = level
            )
        }
    }

    private val filterInputs = combine(
        debouncedSearchQuery,
        _filterState,
        _selectedLevel
    ) { query, filter, level ->
        Triple(query, filter, level)
    }

    val uiState: StateFlow<WordListUiState> = combine(
        libraryPreloadCache.wordListSnapshot,
        filterInputs,
        _isRefreshing,
        _syncMessage
    ) { snapshot, (query, filter, level), isRefreshing, syncMessage ->
        buildWordListUiState(
            snapshot = snapshot,
            query = query,
            filter = filter,
            selectedLevel = level,
            isRefreshing = isRefreshing,
            syncMessage = syncMessage
        )
    }
        .flowOn(filterDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = initialUiState
        )

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

    fun onRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = syncManager.performDictionarySync(forceIncremental = true)

                if (result.updatedWords > 0 || result.updatedGrammars > 0 || result.updatedWordRelations > 0) {
                    val message = buildString {
                        append(context.getString(R.string.lib_sync_updated_prefix))
                        if (result.updatedWords > 0) append(context.getString(R.string.lib_sync_updated_words, result.updatedWords))
                        if (result.updatedGrammars > 0) append(context.getString(R.string.lib_sync_updated_grammars, result.updatedGrammars))
                        if (result.updatedWordRelations > 0) append(context.getString(R.string.lib_sync_updated_relations, result.updatedWordRelations))
                    }
                    _syncMessage.value = message
                } else {
                    _syncMessage.value = context.getString(
                        R.string.lib_sync_already_latest,
                        result.localVersion,
                        result.remoteVersion
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _syncMessage.value = context.getString(R.string.lib_sync_failed, e.message ?: "")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 150L
        const val KEY_SEARCH_QUERY = "word_list_search_query"
        const val KEY_STUDY_FILTER = "word_list_study_filter"
        const val KEY_SELECTED_LEVEL = "word_list_selected_level"
    }
}

internal fun buildWordListUiState(
    snapshot: WordListSnapshot,
    query: String,
    filter: StudyFilter,
    isRefreshing: Boolean,
    syncMessage: String?,
    selectedLevel: String = WordListUiState.LEVEL_ALL
): WordListUiState {
    if (!snapshot.isReady) {
        return WordListUiState(
            isLoading = true,
            searchQuery = query,
            filterState = filter,
            selectedLevel = selectedLevel,
            isRefreshing = isRefreshing,
            syncMessage = syncMessage
        )
    }

    val (wordsByLevel, allWords, countsByLevel) = filterGroupedItems(
        itemsByLevel = snapshot.wordsByLevel,
        allItems = snapshot.allWords,
        countsByLevel = snapshot.countsByLevel,
        filter = filter,
        isLearnedSelector = { it.isLearned },
        query = query,
        queryMatcher = { word, keyword ->
            word.japanese.contains(keyword, ignoreCase = true) ||
                word.hiragana.contains(keyword, ignoreCase = true) ||
                word.chinese.contains(keyword, ignoreCase = true)
        }
    )

    return WordListUiState(
        isLoading = false,
        wordsByLevel = wordsByLevel,
        allWords = allWords,
        countsByLevel = countsByLevel,
        searchQuery = query,
        filterState = filter,
        selectedLevel = selectedLevel,
        isRefreshing = isRefreshing,
        syncMessage = syncMessage,
        error = null
    )
}

internal inline fun <T> filterGroupedItems(
    itemsByLevel: Map<String, List<T>>,
    allItems: List<T>,
    countsByLevel: Map<String, Int>,
    filter: StudyFilter,
    isLearnedSelector: (T) -> Boolean,
    query: String,
    queryMatcher: (T, String) -> Boolean
): Triple<Map<String, List<T>>, List<T>, Map<String, Int>> {
    if (query.isBlank() && filter == StudyFilter.ALL) {
        return Triple(itemsByLevel, allItems, countsByLevel)
    }

    val filteredByLevel = LinkedHashMap<String, List<T>>(itemsByLevel.size)
    val filteredCounts = LinkedHashMap<String, Int>(itemsByLevel.size)
    val filteredAll = ArrayList<T>(allItems.size)

    itemsByLevel.forEach { (level, items) ->
        val filtered = items.filter { item ->
            val matchesFilter = when (filter) {
                StudyFilter.ALL -> true
                StudyFilter.LEARNED -> isLearnedSelector(item)
                StudyFilter.UNLEARNED -> !isLearnedSelector(item)
            }
            if (!matchesFilter) return@filter false
            query.isBlank() || queryMatcher(item, query)
        }
        if (filtered.isNotEmpty()) {
            filteredByLevel[level] = filtered
            filteredCounts[level] = filtered.size
            filteredAll.addAll(filtered)
        }
    }

    return Triple(filteredByLevel, filteredAll, filteredCounts)
}
