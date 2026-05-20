package com.jian.nemo.feature.collection.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.GrammarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 收藏列表ViewModel
 *
 * 参考：
 * - _reference/old-nemo/app/src/main/java/com/jian/nemo/ui/viewmodel/FavoriteWordsViewModel.kt
 * - 错误处理规范.md
 * - rules.md (MVVM + MVI模式)
 *
 * 设计模式：
 * - 使用StateFlow暴露UI状态
 * - 使用Hilt依赖注入
 * - 遵循单向数据流
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val favoriteQuestionRepository: com.jian.nemo.core.domain.repository.FavoriteQuestionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    /**
     * 加载收藏单词和题目列表
     */
    private fun loadFavorites() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 同时观察单词和题目收藏 (以前是语法收藏)
            combine(
                wordRepository.getFavoriteWords(),
                favoriteQuestionRepository.getAllFavoriteQuestions()
            ) { words, questions ->
                Pair(words, questions)
            }.catch { e ->
                println("❌ 加载收藏列表失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败: ${e.message}"
                    )
                }
            }.collect { (words, questions) ->
                _uiState.update {
                    it.copy(
                        favoriteWords = words,
                        favoriteWordsCount = words.size,
                        favoriteGrammarsCount = questions.size, // UI字段复用，实为题目数量
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    /**
     * 取消单词收藏
     */
    fun unfavorite(wordId: Int) {
        viewModelScope.launch {
            when (val result = wordRepository.updateFavoriteStatus(wordId, false)) {
                is Result.Success -> {
                    println("✅ 取消单词收藏成功: wordId=$wordId")
                }
                is Result.Error -> {
                    println("❌ 取消单词收藏失败: ${result.exception.message}")
                    _uiState.update {
                        it.copy(error = "取消收藏失败: ${result.exception.message}")
                    }
                }
                is Result.Loading -> { /* Do nothing */ }
            }
        }
    }

    /**
     * 批量取消单词收藏
     */
    fun deleteWordFavorites(wordIds: Collection<Int>) {
        viewModelScope.launch {
            try {
                wordIds.forEach { wordId ->
                    wordRepository.updateFavoriteStatus(wordId, false)
                }
            } catch (e: Exception) {
                println("❌ 批量取消单词收藏失败: ${e.message}")
                _uiState.update {
                    it.copy(error = "批量取消收藏失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 清空所有收藏（单词和题目）
     */
    fun clearAll() {
        viewModelScope.launch {
            // 清空单词收藏
            wordRepository.clearAllFavorites()
            // 清空题目收藏
            favoriteQuestionRepository.clearAll()
        }
    }

    /**
     * 仅清空单词收藏
     */
    fun clearAllWordFavorites() {
        viewModelScope.launch {
            wordRepository.clearAllFavorites()
        }
    }

    /**
     * 仅清空题目收藏
     */
    fun clearAllGrammarFavorites() {
        viewModelScope.launch {
            favoriteQuestionRepository.clearAll()
        }
    }

    /**
     * 清除错误信息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
