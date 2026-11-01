package com.jian.nemo.feature.collection.favorites

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.feature.collection.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 收藏语法列表ViewModel
 *

 */
@HiltViewModel
class FavoriteGrammarsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val grammarRepository: GrammarRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoriteGrammarsUiState())
    val uiState: StateFlow<FavoriteGrammarsUiState> = _uiState.asStateFlow()

    /**
     * 图标风格（MD3 / Plump Duo），供界面双风格自适应
     */
    val iconStyle: StateFlow<SettingIconStyle> = settingsRepository.settingIconStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingIconStyle.MD3
        )

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
                            error = context.getString(R.string.collection_favorite_grammars_format_load_failed, e.message ?: "")
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
                    it.copy(error = context.getString(R.string.collection_favorite_grammars_format_batch_unfavorite_failed, e.message ?: ""))
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
