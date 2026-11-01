package com.jian.nemo.feature.library.presentation.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.usecase.word.GetWordsByPartOfSpeechUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Context
import com.jian.nemo.feature.library.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 词性单词列表ViewModel
 */
@HiltViewModel
class PosWordsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getWordsByPartOfSpeechUseCase: GetWordsByPartOfSpeechUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val partOfSpeech: PartOfSpeech = try {
        val posString = savedStateHandle.get<String>("pos") ?: "NOUN"
        PartOfSpeech.valueOf(posString)
    } catch (e: Exception) {
        PartOfSpeech.NOUN
    }

    private val _uiState = MutableStateFlow(PosWordsUiState(partOfSpeech = partOfSpeech))
    val uiState: StateFlow<PosWordsUiState> = _uiState.asStateFlow()

    init {
        loadWords()
    }

    private fun loadWords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val words = getWordsByPartOfSpeechUseCase(partOfSpeech)

                _uiState.update {
                    it.copy(
                        words = words,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = context.getString(R.string.library_pos_words_format_load_failed, e.message ?: "")
                    )
                }
            }
        }
    }
}

/**
 * 词性单词列表UI状态
 */
data class PosWordsUiState(
    val partOfSpeech: PartOfSpeech,
    val words: List<Word> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
