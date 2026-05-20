package com.jian.nemo.feature.collection.mistakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.core.domain.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 错误单词列表UI状态
 */
data class WrongWordsUiState(
    val isLoading: Boolean = true,
    val words: List<Word> = emptyList(),
    val error: String? = null
)

/**
 * 错误单词列表ViewModel
 *
 * 参考：_reference/old-nemo/app/src/main/java/com/jian/nemo/ui/viewmodel/WrongWordsViewModel.kt
 */
@HiltViewModel
class WrongWordsViewModel @Inject constructor(
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val wordRepository: WordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WrongWordsUiState())
    val uiState: StateFlow<WrongWordsUiState> = _uiState.asStateFlow()

    init {
        loadWrongWords()
    }

    /**
     * 加载所有错题单词
     */
    private fun loadWrongWords() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 1. 获取所有错题单词ID
                val wrongWordIds = wrongAnswerRepository.getAllWrongWordIds()

                // 2. 通过ID获取完整的Word对象
                val words = if (wrongWordIds.isNotEmpty()) {
                    wordRepository.getWordsByIds(wrongWordIds)
                } else {
                    emptyList()
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        words = words,
                        error = null
                    )
                }
            } catch (e: Exception) {
                println("❌ 加载错题单词失败: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * 刷新列表
     */
    fun refresh() {
        loadWrongWords()
    }

    /**
     * 批量删除错题单词
     */
    fun deleteWrongWords(wordIds: Collection<Int>) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                wordIds.forEach { wordId ->
                    wrongAnswerRepository.deleteByWordId(wordId)
                }
                loadWrongWords()
            } catch (e: Exception) {
                println("❌ 批量删除错词失败: ${e.message}")
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

