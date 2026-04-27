package com.jian.nemo.feature.library.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.usecase.audio.PlayTtsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import javax.inject.Inject
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.ContentReportRepository
import com.jian.nemo.core.domain.repository.TtsEvent

/**
 * 单词详情 ViewModel
 *
 * 从导航参数中获取 wordId，加载并展示单词详细信息
 */
@HiltViewModel
class WordDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wordRepository: WordRepository,
    private val playTtsUseCase: PlayTtsUseCase,
    private val audioRepository: AudioRepository,
    private val contentReportRepository: ContentReportRepository
) : ViewModel() {

    // Audio Status
    private val _playingAudioId = MutableStateFlow<String?>(null)
    val playingAudioId = _playingAudioId.asStateFlow()

    private val startWordId: Int = checkNotNull(savedStateHandle["wordId"])

    // Context IDs for Swipe Navigation
    private val _contextIds = MutableStateFlow<List<Int>>(emptyList())
    val contextIds = _contextIds.asStateFlow()

    // Current word flow (for initial loading to determine context)
    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord = _currentWord.asStateFlow()

    // Reporting States
    private val _showReportDialog = MutableStateFlow(false)
    val showReportDialog = _showReportDialog.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        loadInitialWordAndContext()
        observeTtsEvents()
    }

    private fun loadInitialWordAndContext() {
        viewModelScope.launch {
            // 1. Fetch the target word first to know its level
            wordRepository.getWordById(startWordId).collect { word ->
                if (word != null) {
                    _currentWord.value = word

                    // 2. Once we have the word/level, fetch the context list (siblings)
                    // Only fetch if we haven't already (to avoid infinite loops if words update)
                    if (_contextIds.value.isEmpty()) {
                        loadContextWords(word.level)
                    }
                }
            }
        }
    }

    private fun loadContextWords(level: String) {
        viewModelScope.launch {
            // Fetch all words in this level to build the swipe list
            // Note: This assumes the default list order is acceptable.
            // In a real app we might want to respect the sorted order from the list screen.
            wordRepository.getAllWordsByLevel(level).collect { words ->
                _contextIds.value = words.map { it.id }
            }
        }
    }

    /**
     * Get a flow for a specific word by ID (used by Pager pages)
     */
    fun getWordFlow(id: Int): kotlinx.coroutines.flow.Flow<Word?> {
        return wordRepository.getWordById(id)
    }

    private fun observeTtsEvents() {
        viewModelScope.launch {
            audioRepository.ttsEvents.collect { event ->
                when (event) {
                    is TtsEvent.OnStart -> _playingAudioId.value = event.id
                    is TtsEvent.OnDone -> {
                        if (_playingAudioId.value == event.id) {
                            _playingAudioId.value = null
                        }
                    }
                    is TtsEvent.OnError -> {
                        if (_playingAudioId.value == event.id) {
                            _playingAudioId.value = null
                        }
                    }
                    TtsEvent.GoogleTtsMissing -> {
                        _playingAudioId.value = null
                    }
                }
            }
        }
    }

    fun playAudio(text: String, id: String? = null) {
        val uniqueId = id ?: "word_${text.hashCode()}"
        playTtsUseCase(text, "ja-JP", uniqueId)
    }

    fun openReportDialog() {
        _showReportDialog.value = true
    }

    fun cancelReportDialog() {
        _showReportDialog.value = false
    }

    fun reportContentError(wordId: Int) {
        _showReportDialog.value = false
        viewModelScope.launch {
            val result = contentReportRepository.reportContentError(wordId, "word")
            if (result is Result.Success) {
                _successMessage.value = "反馈成功，感谢您的反馈！"
            } else if (result is Result.Error) {
                _errorMessage.value = "反馈失败: ${result.exception.message}"
            }
        }
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
