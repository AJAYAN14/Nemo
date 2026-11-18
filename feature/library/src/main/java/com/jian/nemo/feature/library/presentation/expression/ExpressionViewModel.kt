package com.jian.nemo.feature.library.presentation.expression

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.Expression
import com.jian.nemo.core.domain.model.ExpressionCategory
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
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

private data class ExpressionRawState(
    val categories: List<ExpressionCategory> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class ExpressionUiState(
    val categories: List<ExpressionCategory> = emptyList(),
    val selectedCategory: ExpressionCategory? = null,
    val searchQuery: String = "",
    val selectedLevel: String = LEVEL_ALL,
    val filteredExpressions: List<Expression> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    companion object {
        const val LEVEL_ALL = "All"
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class ExpressionViewModel internal constructor(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val ioDispatcher: CoroutineDispatcher,
    private val filterDispatcher: CoroutineDispatcher
) : ViewModel() {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        savedStateHandle: SavedStateHandle
    ) : this(
        context = context,
        savedStateHandle = savedStateHandle,
        ioDispatcher = Dispatchers.IO,
        filterDispatcher = Dispatchers.Default
    )

    private val rawState = MutableStateFlow(ExpressionRawState(isLoading = true))

    private val _selectedCategoryKey = MutableStateFlow<String?>(
        savedStateHandle.get<String>(KEY_SELECTED_CATEGORY)
    )

    private val _searchQuery = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SEARCH_QUERY).orEmpty()
    )
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLevel = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SELECTED_LEVEL) ?: ExpressionUiState.LEVEL_ALL
    )

    private val debouncedSearchQuery = _searchQuery
        .debounce { query -> if (query.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()

    val uiState: StateFlow<ExpressionUiState> = combine(
        rawState,
        _selectedCategoryKey,
        debouncedSearchQuery,
        _selectedLevel
    ) { raw, selectedKey, query, selectedLevel ->
        buildExpressionUiState(
            categories = raw.categories,
            isLoading = raw.isLoading,
            error = raw.error,
            selectedCategoryKey = selectedKey,
            searchQuery = query,
            selectedLevel = selectedLevel
        )
    }
        .flowOn(filterDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExpressionUiState(
                searchQuery = _searchQuery.value,
                selectedLevel = _selectedLevel.value,
                isLoading = true
            )
        )

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val fileNames = listOf(
        "collocation.json",
        "sentence_pattern.json",
        "idiom.json",
        "four_character_idiom.json",
        "semi_fixed_template.json",
        "collocation_group.json"
    )

    init {
        loadExpressions()
    }

    private fun loadExpressions() {
        viewModelScope.launch(ioDispatcher) {
            rawState.value = ExpressionRawState(isLoading = true)
            try {
                val loadedCategories = mutableListOf<ExpressionCategory>()
                fileNames.forEach { fileName ->
                    try {
                        val inputStream = context.assets.open("expressions/$fileName")
                        val reader = BufferedReader(InputStreamReader(inputStream))
                        val jsonString = reader.use { it.readText() }
                        val category = json.decodeFromString<ExpressionCategory>(jsonString)
                        loadedCategories.add(category)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.e("ExpressionViewModel", "Failed to load asset: $fileName", e)
                    }
                }
                loadedCategories.sortBy { it.levelId }
                rawState.value = ExpressionRawState(
                    categories = loadedCategories,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                rawState.value = ExpressionRawState(
                    categories = emptyList(),
                    isLoading = false,
                    error = e.message ?: context.getString(R.string.library_expression_label_load_failed)
                )
            }
        }
    }

    fun selectCategory(category: ExpressionCategory?) {
        val key = category?.categoryKey
        if (_selectedCategoryKey.value != key) {
            _searchQuery.value = ""
            savedStateHandle[KEY_SEARCH_QUERY] = ""
            _selectedLevel.value = ExpressionUiState.LEVEL_ALL
            savedStateHandle[KEY_SELECTED_LEVEL] = ExpressionUiState.LEVEL_ALL
        }
        _selectedCategoryKey.value = key
        savedStateHandle[KEY_SELECTED_CATEGORY] = key
    }

    fun changeSearchQuery(query: String) {
        _searchQuery.value = query
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun changeSelectedLevel(level: String) {
        _selectedLevel.value = level
        savedStateHandle[KEY_SELECTED_LEVEL] = level
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 150L
        const val KEY_SELECTED_CATEGORY = "expression_selected_category"
        const val KEY_SEARCH_QUERY = "expression_search_query"
        const val KEY_SELECTED_LEVEL = "expression_selected_level"
    }
}

internal fun buildExpressionUiState(
    categories: List<ExpressionCategory>,
    isLoading: Boolean,
    error: String?,
    selectedCategoryKey: String?,
    searchQuery: String,
    selectedLevel: String
): ExpressionUiState {
    if (isLoading || error != null) {
        return ExpressionUiState(
            categories = categories,
            selectedCategory = null,
            searchQuery = searchQuery,
            selectedLevel = selectedLevel,
            filteredExpressions = emptyList(),
            isLoading = isLoading,
            error = error
        )
    }

    val selectedCategory = selectedCategoryKey?.let { key ->
        categories.firstOrNull { it.categoryKey == key }
    }
    val sourceItems = selectedCategory?.items ?: categories.flatMap { it.items }
    val isAllLevel = selectedLevel.equals(ExpressionUiState.LEVEL_ALL, ignoreCase = true)
    val hasQuery = searchQuery.isNotBlank()

    val filtered = if (isAllLevel && !hasQuery) {
        sourceItems
    } else {
        sourceItems.filter { item ->
            val matchesLevel = isAllLevel || item.level.equals(selectedLevel, ignoreCase = true)
            if (!matchesLevel) return@filter false
            !hasQuery ||
                item.japanese.contains(searchQuery, ignoreCase = true) ||
                item.furigana.contains(searchQuery, ignoreCase = true) ||
                item.chinese.contains(searchQuery, ignoreCase = true) ||
                item.clozeShow.contains(searchQuery, ignoreCase = true)
        }
    }

    return ExpressionUiState(
        categories = categories,
        selectedCategory = selectedCategory,
        searchQuery = searchQuery,
        selectedLevel = selectedLevel,
        filteredExpressions = filtered,
        isLoading = false,
        error = null
    )
}
