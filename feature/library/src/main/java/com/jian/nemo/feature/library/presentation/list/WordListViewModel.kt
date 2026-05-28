package com.jian.nemo.feature.library.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val error: String? = null,
    val searchQuery: String = "",
    val filterState: StudyFilter = StudyFilter.ALL,
    val isRefreshing: Boolean = false,
    val syncMessage: String? = null
)

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _filterState = MutableStateFlow(StudyFilter.ALL)

    // 所有单词聚合 Flow
    private val allWordsFlow = combine(
        wordRepository.getAllWordsByLevel("N1"),
        wordRepository.getAllWordsByLevel("N2"),
        wordRepository.getAllWordsByLevel("N3"),
        wordRepository.getAllWordsByLevel("N4"),
        wordRepository.getAllWordsByLevel("N5")
    ) { n1, n2, n3, n4, n5 ->
        n1 + n2 + n3 + n4 + n5
    }

    val uiState: StateFlow<WordListUiState> = combine(
        allWordsFlow,
        _searchQuery,
        _filterState,
        _isRefreshing,
        _syncMessage
    ) { allWords, query, filter, isRefreshing, syncMessage ->
        // Perform filtering on IO thread via flowOn below
        val filteredList = allWords.filter { w ->
            // 1. 过滤已学/未学/全部
            val matchesFilter = when (filter) {
                StudyFilter.ALL -> true
                StudyFilter.LEARNED -> w.isLearned
                StudyFilter.UNLEARNED -> !w.isLearned
            }
            if (!matchesFilter) return@filter false

            // 2. 过滤搜索词
            if (query.isBlank()) {
                true
            } else {
                w.japanese.contains(query, ignoreCase = true) ||
                w.hiragana.contains(query, ignoreCase = true) ||
                w.chinese.contains(query, ignoreCase = true)
            }
        }

        val grouped = filteredList.groupBy { it.level }
        // Optional: Sort keys if needed, but Map keeps insertion order if LinkedHashMap (default groupBy)
        // Usually we want N1..N5 or N5..N1. The repository calls order suggests N1..N5 but map keys might be anything.
        // Let's rely on sorted keys in UI or ensure sorted map here.
        // For now, simple grouping.

        WordListUiState(
            isLoading = false,
            wordsByLevel = grouped,
            searchQuery = query,
            filterState = filter,
            isRefreshing = isRefreshing,
            syncMessage = syncMessage,
            error = null
        )
    }
    .flowOn(Dispatchers.Default) // Move computation to Default dispatcher
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WordListUiState(isLoading = true)
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
                val result = syncRepository.performDictionarySync(forceIncremental = true)
                
                if (result.updatedWords > 0 || result.updatedGrammars > 0) {
                    val message = buildString {
                        append("词库已更新：")
                        if (result.updatedWords > 0) append("${result.updatedWords}条单词 ")
                        if (result.updatedGrammars > 0) append("${result.updatedGrammars}条语法")
                    }
                    _syncMessage.value = message
                } else {
                    // 如果没有更新，显示更详细的信息（用于排查）
                    _syncMessage.value = "词库已是最新 (本地:V${result.localVersion}, 远程:V${result.remoteVersion})"
                }
            } catch (e: Exception) {
                _syncMessage.value = "同步失败: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
