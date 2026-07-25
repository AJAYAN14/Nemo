package com.jian.nemo.feature.library.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.DictionarySyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
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

/**
 * 语法列表UI状态
 */
data class GrammarListUiState(
    val grammarsByLevel: Map<String, List<Grammar>> = emptyMap(),
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
    private val grammarRepository: GrammarRepository,
    private val syncManager: DictionarySyncManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _filterState = MutableStateFlow(StudyFilter.ALL)
    private val _syncMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GrammarListUiState> = combine(
        grammarRepository.getGrammarsByLevels(listOf("N1", "N2", "N3", "N4", "N5")),
        _searchQuery,
        _filterState,
        _isRefreshing,
        _syncMessage
    ) { allGrammars, query, filter, isRefreshing, syncMessage ->
        // 过滤
        val filteredList = allGrammars.filter { g ->
            // 1. 过滤已学/未学/全部
            val matchesFilter = when (filter) {
                StudyFilter.ALL -> true
                StudyFilter.LEARNED -> g.isLearned
                StudyFilter.UNLEARNED -> !g.isLearned
            }
            if (!matchesFilter) return@filter false

            // 2. 过滤搜索词
            if (query.isBlank()) {
                true
            } else {
                // 1. 匹配语法标题
                if (GrammarSearchUtils.isMatch(g.grammar, query)) {
                    true
                } else {
                    // 2. 匹配用法详情 (解释、接续、笔记)
                    g.usages.any { usage ->
                        GrammarSearchUtils.isMatch(usage.explanation, query) ||
                        GrammarSearchUtils.isMatch(usage.connection, query) ||
                        (usage.notes?.let { GrammarSearchUtils.isMatch(it, query) } ?: false) ||
                        // 3. 匹配例句 (文本、翻译)
                        usage.examples.any { ex ->
                            GrammarSearchUtils.isMatch(ex.sentence, query) ||
                            ex.translation.contains(query, ignoreCase = true)
                        }
                    }
                }
            }
        }

        // 分组 (N1, N2...)
        // 这里不需要 toSortedMap，因为Map本身顺序在UI层处理，
        // 或者可以在这里保证顺序。Map.groupBy 可能会乱序。
        // 为了安全起见，UI层会再排一次 Key。
        val grouped = filteredList.groupBy { it.grammarLevel }

        GrammarListUiState(
            grammarsByLevel = grouped,
            isLoading = false,
            searchQuery = query,
            filterState = filter,
            isRefreshing = isRefreshing,
            syncMessage = syncMessage
        )
    }
    .flowOn(Dispatchers.Default) // 后台计算
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GrammarListUiState(isLoading = true)
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
                
                if (result.updatedWords > 0 || result.updatedGrammars > 0) {
                    val message = buildString {
                        append("词库已更新：")
                        if (result.updatedWords > 0) append("${result.updatedWords}条单词 ")
                        if (result.updatedGrammars > 0) append("${result.updatedGrammars}条语法")
                    }
                    _syncMessage.value = message
                } else {
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
