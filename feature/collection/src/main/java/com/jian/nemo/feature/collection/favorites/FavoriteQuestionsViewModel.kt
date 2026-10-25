package com.jian.nemo.feature.collection.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.FavoriteQuestion
import com.jian.nemo.core.domain.repository.FavoriteQuestionRepository
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 收藏题目 ViewModel
 */
@HiltViewModel
class FavoriteQuestionsViewModel @Inject constructor(
    private val favoriteQuestionRepository: FavoriteQuestionRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = true,
        val favoriteQuestions: List<FavoriteQuestion> = emptyList()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

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
        loadFavoriteQuestions()
    }

    private fun loadFavoriteQuestions() {
        viewModelScope.launch {
            favoriteQuestionRepository.getAllFavoriteQuestions().collect { questions ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        favoriteQuestions = questions
                    )
                }
            }
        }
    }

    /**
     * 取消收藏
     */
    fun unfavorite(questionId: Int) {
        viewModelScope.launch {
            favoriteQuestionRepository.deleteFavoriteQuestion(questionId)
        }
    }

    /**
     * 批量取消题目收藏
     */
    fun deleteQuestionFavorites(questionIds: Collection<Int>) {
        viewModelScope.launch {
            try {
                questionIds.forEach { questionId ->
                    favoriteQuestionRepository.deleteFavoriteQuestion(questionId)
                }
            } catch (e: Exception) {
                println("❌ 批量取消题目收藏失败: ${e.message}")
            }
        }
    }
}
