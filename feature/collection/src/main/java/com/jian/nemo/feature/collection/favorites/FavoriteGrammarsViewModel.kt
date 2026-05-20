package com.jian.nemo.feature.collection.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 收藏语法列表ViewModel
 *
 * 参考：_reference/old-nemo/app/src/main/java/com/jian/nemo/ui/viewmodel/FavoriteGrammarsViewModel.kt
 */
@HiltViewModel
class FavoriteGrammarsViewModel @Inject constructor(
    private val grammarRepository: GrammarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoriteGrammarsUiState())
    val uiState: StateFlow<FavoriteGrammarsUiState> = _uiState.asStateFlow()

    init {
        loadFavoriteGrammars()
    }

    /**
     * 加载收藏语法列表
     */
    private fun loadFavoriteGrammars() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            grammarRepository.getFavoriteGrammars()
                .catch { e ->
                    println("❌ 加载收藏语法失败: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "加载失败: ${e.message}"
                        )
                    }
                }
                .collect { grammars ->
                    _uiState.update {
                        it.copy(
                            favoriteGrammars = grammars,
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * 取消收藏
     */
    fun unfavorite(grammarId: Int) {
        viewModelScope.launch {
            grammarRepository.updateFavoriteStatus(grammarId, false)
        }
    }

    /**
     * 批量取消语法收藏
     */
    fun deleteGrammarFavorites(grammarIds: Collection<Int>) {
        viewModelScope.launch {
            try {
                grammarIds.forEach { grammarId ->
                    grammarRepository.updateFavoriteStatus(grammarId, false)
                }
            } catch (e: Exception) {
                println("❌ 批量取消语法收藏失败: ${e.message}")
                _uiState.update {
                    it.copy(error = "批量取消收藏失败: ${e.message}")
                }
            }
        }
    }
}

/**
 * 收藏语法列表UI状态
 */
data class FavoriteGrammarsUiState(
    val favoriteGrammars: List<Grammar> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
