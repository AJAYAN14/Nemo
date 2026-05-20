package com.jian.nemo.feature.collection.mistakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.GrammarWrongAnswer
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 错误语法列表UI状态
 */
data class WrongGrammarsUiState(
    val isLoading: Boolean = true,
    val wrongAnswers: List<GrammarWrongAnswer> = emptyList(),
    val error: String? = null
)

/**
 * 错误语法列表ViewModel
 */
@HiltViewModel
class WrongGrammarsViewModel @Inject constructor(
    private val grammarWrongAnswerRepository: GrammarWrongAnswerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrongGrammarsUiState())
    val uiState: StateFlow<WrongGrammarsUiState> = _uiState.asStateFlow()

    init {
        loadWrongGrammars()
    }

    /**
     * 加载所有错题语法
     */
    private fun loadWrongGrammars() {
        viewModelScope.launch {
            grammarWrongAnswerRepository.getAllWrongAnswers()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    println("❌ 加载错题语法失败: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "加载失败: ${e.message}"
                        )
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            wrongAnswers = list,
                            error = null
                        )
                    }
                }
        }
    }

    /**
     * 刷新列表
     */
    fun refresh() {
        loadWrongGrammars()
    }

    /**
     * 批量删除语法错题
     */
    fun deleteWrongGrammars(grammarIds: Collection<Int>) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                grammarIds.forEach { grammarId ->
                    grammarWrongAnswerRepository.deleteByGrammarId(grammarId)
                }
            } catch (e: Exception) {
                println("❌ 批量删除语法错题失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "删除失败: ${e.message}"
                    )
                }
            }
        }
    }
}

