package com.jian.nemo.feature.library.presentation.partofspeech

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.domain.repository.WordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import com.jian.nemo.feature.library.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 词性分类ViewModel
 *
 * 管理12种词性的统计数据
 */
@HiltViewModel
class PartOfSpeechViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wordRepository: WordRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartOfSpeechUiState())
    val uiState: StateFlow<PartOfSpeechUiState> = _uiState.asStateFlow()

    init {
        loadPartOfSpeechStats()
    }

    private fun loadPartOfSpeechStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // 获取每种词性的单词数量
                val stats = PartOfSpeech.entries.associateWith { pos ->
                    wordRepository.getWordsByPartOfSpeech(pos).size
                }

                _uiState.update {
                    it.copy(
                        partOfSpeechStats = stats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.library_part_of_speech_format_load_failed, e.message ?: "")
                    )
                }
            }
        }
    }
}

/**
 * 词性分类UI状态
 */
data class PartOfSpeechUiState(
    val partOfSpeechStats: Map<PartOfSpeech, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
