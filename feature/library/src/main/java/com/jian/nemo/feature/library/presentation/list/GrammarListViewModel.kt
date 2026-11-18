package com.jian.nemo.feature.library.presentation.list

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.util.GrammarSearchUtils
import com.jian.nemo.core.domain.repository.DictionarySyncManager
import com.jian.nemo.core.domain.repository.GrammarListSnapshot
import com.jian.nemo.core.domain.repository.GrammarPreloadItem
import com.jian.nemo.core.domain.repository.LibraryPreloadCache
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
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

data class GrammarListUiState(
    val grammarsByLevel: Map<String, List<GrammarPreloadItem>> = emptyMap(),
    val allGrammars: List<GrammarPreloadItem> = emptyList(),
    val countsByLevel: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val filterState: StudyFilter = StudyFilter.ALL,
    val selectedLevel: String = WordListUiState.LEVEL_ALL,
    val isRefreshing: Boolean = false,
    val syncMessage: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class GrammarListViewModel internal constructor(
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

    private val initialUiState: GrammarListUiState = run {
        val snapshot = libraryPreloadCache.grammarListSnapshot.value
        val query = _searchQuery.value
        val filter = _filterState.value
        val level = _selectedLevel.value
        if (snapshot.isReady && query.isBlank() && filter == StudyFilter.ALL) {
            GrammarListUiState(
                grammarsByLevel = snapshot.grammarsByLevel,
                allGrammars = snapshot.allGrammars,
                countsByLevel = snapshot.countsByLevel,
                isLoading = false,
                searchQuery = query,
                filterState = filter,
                selectedLevel = level
            )
        } else {
            GrammarListUiState(
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

    val uiState: StateFlow<GrammarListUiState> = combine(
        libraryPreloadCache.grammarListSnapshot,
        filterInputs,
        _isRefreshing,
        _syncMessage
    ) { snapshot, (query, filter, level), isRefreshing, syncMessage ->
        buildGrammarListUiState(
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
        const val KEY_SEARCH_QUERY = "grammar_list_search_query"
        const val KEY_STUDY_FILTER = "grammar_list_study_filter"
        const val KEY_SELECTED_LEVEL = "grammar_list_selected_level"
    }
}

internal fun buildGrammarListUiState(
    snapshot: GrammarListSnapshot,
    query: String,
    filter: StudyFilter,
    isRefreshing: Boolean,
    syncMessage: String?,
    selectedLevel: String = WordListUiState.LEVEL_ALL
): GrammarListUiState {
    if (!snapshot.isReady) {
        return GrammarListUiState(
            isLoading = true,
            searchQuery = query,
            filterState = filter,
            selectedLevel = selectedLevel,
            isRefreshing = isRefreshing,
            syncMessage = syncMessage
        )
    }

    val (grammarsByLevel, allGrammars, countsByLevel) = filterGroupedItems(
        itemsByLevel = snapshot.grammarsByLevel,
        allItems = snapshot.allGrammars,
        countsByLevel = snapshot.countsByLevel,
        filter = filter,
        isLearnedSelector = { it.grammar.isLearned },
        query = query,
        queryMatcher = { item, keyword ->
            val g = item.grammar
            GrammarSearchUtils.isMatch(g.grammar, keyword) ||
                g.usages.any { usage ->
                    GrammarSearchUtils.isMatch(usage.explanation, keyword) ||
                        GrammarSearchUtils.isMatch(usage.connection, keyword) ||
                        (usage.notes?.let { GrammarSearchUtils.isMatch(it, keyword) } ?: false) ||
                        usage.examples.any { ex ->
                            GrammarSearchUtils.isMatch(ex.sentence, keyword) ||
                                ex.translation.contains(keyword, ignoreCase = true)
                        }
                }
        }
    )

    return GrammarListUiState(
        grammarsByLevel = grammarsByLevel,
        allGrammars = allGrammars,
        countsByLevel = countsByLevel,
        isLoading = false,
        searchQuery = query,
        filterState = filter,
        selectedLevel = selectedLevel,
        isRefreshing = isRefreshing,
        syncMessage = syncMessage
    )
}
