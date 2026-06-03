package com.jian.nemo.feature.library.presentation.expression

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.Expression
import com.jian.nemo.core.domain.model.ExpressionCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

data class ExpressionUiState(
    val categories: List<ExpressionCategory> = emptyList(),
    val selectedCategory: ExpressionCategory? = null,
    val searchQuery: String = "",
    val selectedLevel: String = "All",
    val filteredExpressions: List<Expression> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExpressionViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpressionUiState())
    val uiState: StateFlow<ExpressionUiState> = _uiState.asStateFlow()

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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true) }
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
                        Log.e("ExpressionViewModel", "加载文件失败: $fileName", e)
                    }
                }
                
                // 按 levelId 从小到大排序
                loadedCategories.sortBy { it.levelId }

                _uiState.update { 
                    it.copy(
                        categories = loadedCategories,
                        isLoading = false
                    ) 
                }
                // 刷新一次过滤列表
                applyFilters()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载数据失败"
                    ) 
                }
            }
        }
    }

    fun selectCategory(category: ExpressionCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    fun changeSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun changeSelectedLevel(level: String) {
        _uiState.update { it.copy(selectedLevel = level) }
        applyFilters()
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        val sourceItems = currentState.selectedCategory?.items 
            ?: currentState.categories.flatMap { it.items }
            
        val filtered = sourceItems.filter { item ->
            // 1. 过滤 JLPT 级别
            val matchesLevel = currentState.selectedLevel == "All" || item.level.uppercase() == currentState.selectedLevel.uppercase()
            
            // 2. 过滤检索词
            val matchesQuery = currentState.searchQuery.isBlank() ||
                    item.japanese.contains(currentState.searchQuery, ignoreCase = true) ||
                    item.furigana.contains(currentState.searchQuery, ignoreCase = true) ||
                    item.chinese.contains(currentState.searchQuery, ignoreCase = true) ||
                    item.clozeShow.contains(currentState.searchQuery, ignoreCase = true)
                    
            matchesLevel && matchesQuery
        }
        
        _uiState.update { it.copy(filteredExpressions = filtered) }
    }
}
