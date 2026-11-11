package com.jian.nemo.feature.library.presentation.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.feature.library.R
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.DictionarySyncManager
import com.jian.nemo.core.domain.repository.LibraryPreloadCache
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordListSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StudyFilter {
    ALL,      // 全部
    LEARNED,  // 已学
    UNLEARNED // 未学
}

data class WordListUiState(
    val isLoading: Boolean = true,
    val wordsByLevel: Map<String, List<Word>> = emptyMap(),
    val allWords: List<Word> = emptyList(),
    val countsByLevel: Map<String, Int> = emptyMap(),
    val error: String? = null,
    val searchQuery: String = "",
    val filterState: StudyFilter = StudyFilter.ALL,
    val isRefreshing: Boolean = false,
    val syncMessage: String? = null
)

@HiltViewModel
class WordListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val libraryPreloadCache: LibraryPreloadCache,
    private val syncManager: DictionarySyncManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    init {
        libraryPreloadCache.ensureStartedNow()
    }

    val iconStyle: StateFlow<SettingIconStyle> = settingsRepository.settingIconStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = libraryPreloadCache.settingIconStyleState.value
        )

    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _filterState = MutableStateFlow(StudyFilter.ALL)
    private val _syncMessage = MutableStateFlow<String?>(null)

    private val initialUiState = buildWordListUiState(
        snapshot = libraryPreloadCache.wordListSnapshot.value,
        query = "",
        filter = StudyFilter.ALL,
        isRefreshing = false,
        syncMessage = null
    )

    val uiState: StateFlow<WordListUiState> = combine(
        libraryPreloadCache.wordListSnapshot,
        _searchQuery,
        _filterState,
        _isRefreshing,
        _syncMessage
    ) { snapshot, query, filter, isRefreshing, syncMessage ->
        buildWordListUiState(
            snapshot = snapshot,
            query = query,
            filter = filter,
            isRefreshing = isRefreshing,
            syncMessage = syncMessage
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialUiState
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterStateChanged(filter: StudyFilter) {
        _filterState.value = filter
    }

    fun onRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 触发强制增量同步 (forceIncremental = true)，绕过版本号检查
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
                    _syncMessage.value = context.getString(R.string.lib_sync_already_latest, result.localVersion, result.remoteVersion)
                }
            } catch (e: Exception) {
                _syncMessage.value = context.getString(R.string.lib_sync_failed, e.message ?: "")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}

internal fun buildWordListUiState(
    snapshot: WordListSnapshot,
    query: String,
    filter: StudyFilter,
    isRefreshing: Boolean,
    syncMessage: String?
): WordListUiState {
    if (!snapshot.isReady) {
        return WordListUiState(
            isLoading = true,
            searchQuery = query,
            filterState = filter,
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
