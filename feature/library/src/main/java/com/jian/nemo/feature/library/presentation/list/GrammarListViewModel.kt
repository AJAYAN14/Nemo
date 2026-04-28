package com.jian.nemo.feature.library.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
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
import com.jian.nemo.core.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * 语法列表UI状态
 */
data class GrammarListUiState(
    val grammarsByLevel: Map<String, List<Grammar>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false
)

/**
 * 语法列表ViewModel
 * 负责获取所有语法并按等级分组，支持后台线程搜索过滤
 */
@HiltViewModel
class GrammarListViewModel @Inject constructor(
    private val grammarRepository: GrammarRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<GrammarListUiState> = combine(
        grammarRepository.getGrammarsByLevels(listOf("N1", "N2", "N3", "N4", "N5")),
        _searchQuery,
        _isRefreshing
    ) { allGrammars, query, isRefreshing ->
        // 过滤
        val filteredList = if (query.isBlank()) {
            allGrammars
        } else {
            allGrammars.filter { g ->
                // 1. 匹配语法标题
                if (GrammarSearchUtils.isMatch(g.grammar, query)) return@filter true
                
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

        // 分组 (N1, N2...)
        // 这里不需要 toSortedMap，因为Map本身顺序在UI层处理，
        // 或者可以在这里保证顺序。Map.groupBy 可能会乱序。
        // 为了安全起见，UI层会再排一次 Key。
        val grouped = filteredList.groupBy { it.grammarLevel }

        GrammarListUiState(
            grammarsByLevel = grouped,
            isLoading = false,
            searchQuery = query,
            isRefreshing = isRefreshing
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

    fun onRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 刷新词库，force = true 以便清理并全量同步
                syncRepository.performDictionarySync(force = true)
            } catch (e: Exception) {
                // 处理错误
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
