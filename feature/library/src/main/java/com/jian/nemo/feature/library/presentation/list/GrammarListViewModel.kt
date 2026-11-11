package com.jian.nemo.feature.library.presentation.list

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.feature.library.R
import com.jian.nemo.core.domain.repository.DictionarySyncManager
import com.jian.nemo.core.domain.repository.GrammarListSnapshot
import com.jian.nemo.core.domain.repository.GrammarPreloadItem
import com.jian.nemo.core.domain.repository.LibraryPreloadCache
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.jian.nemo.core.common.util.GrammarSearchUtils
import javax.inject.Inject
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository

/**
 * 语法列表UI状态
 */
data class GrammarListUiState(
    val grammarsByLevel: Map<String, List<GrammarPreloadItem>> = emptyMap(),
    val allGrammars: List<GrammarPreloadItem> = emptyList(),
    val countsByLevel: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val filterState: StudyFilter = StudyFilter.ALL,
    val isRefreshing: Boolean = false,
    val syncMessage: String? = null
)

/**
 * 语法列表ViewModel
 * 负责获取所有语法并按等级分组，支持后台线程搜索过滤
 */
@HiltViewModel
class GrammarListViewModel @Inject constructor(
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

    private val initialUiState = buildGrammarListUiState(
        snapshot = libraryPreloadCache.grammarListSnapshot.value,
        query = "",
        filter = StudyFilter.ALL,
        isRefreshing = false,
        syncMessage = null
    )

    val uiState: StateFlow<GrammarListUiState> = combine(
        libraryPreloadCache.grammarListSnapshot,
        _searchQuery,
        _filterState,
        _isRefreshing,
        _syncMessage
    ) { snapshot, query, filter, isRefreshing, syncMessage ->
        buildGrammarListUiState(
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

internal fun buildGrammarListUiState(
    snapshot: GrammarListSnapshot,
    query: String,
    filter: StudyFilter,
    isRefreshing: Boolean,
    syncMessage: String?
): GrammarListUiState {
    if (!snapshot.isReady) {
        return GrammarListUiState(
            isLoading = true,
            searchQuery = query,
            filterState = filter,
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
        isRefreshing = isRefreshing,
        syncMessage = syncMessage
    )
}
