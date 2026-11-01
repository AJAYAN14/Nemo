package com.jian.nemo.feature.library.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.content.Context
import com.jian.nemo.feature.library.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.ContentReportRepository
import com.jian.nemo.core.domain.repository.TtsEvent
import com.jian.nemo.core.domain.usecase.audio.PlayTtsUseCase
import com.jian.nemo.core.ui.navigation.DetailContextHolder

/**
 * 语法详情 ViewModel
 *
 * 从导航参数中获取 grammarId，加载并展示语法详细信息
 */
@HiltViewModel
class GrammarDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val grammarRepository: GrammarRepository,
    private val playTtsUseCase: PlayTtsUseCase,
    private val audioRepository: AudioRepository,
    private val contentReportRepository: ContentReportRepository
) : ViewModel() {

    // Audio Status
    private val _playingAudioId = MutableStateFlow<String?>(null)
    val playingAudioId = _playingAudioId.asStateFlow()

    private val startGrammarId: Int = checkNotNull(savedStateHandle["grammarId"])

    // Context IDs for Swipe Navigation
    private val _contextIds = MutableStateFlow<List<Int>>(emptyList())
    val contextIds = _contextIds.asStateFlow()

    private val _currentGrammar = MutableStateFlow<Grammar?>(null)
    val currentGrammar = _currentGrammar.asStateFlow()

    // Reporting States
    private val _showReportDialog = MutableStateFlow(false)
    val showReportDialog = _showReportDialog.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        loadInitialGrammarAndContext()
        observeTtsEvents()
    }

    private fun loadInitialGrammarAndContext() {
        viewModelScope.launch {
            grammarRepository.getGrammarById(startGrammarId).collect { grammar ->
                if (grammar != null) {
                    _currentGrammar.value = grammar

                    if (_contextIds.value.isEmpty()) {
                        val customContext = DetailContextHolder.consumeGrammarContext(startGrammarId)
                        if (customContext != null) {
                            _contextIds.value = customContext
                        } else if (!grammar.grammarLevel.isNullOrBlank()) {
                            // Assuming getGrammarsByLevels can handle a single level list
                            loadContextGrammars(grammar.grammarLevel!!)
                        }
                    }
                }
            }
        }
    }

    private fun loadContextGrammars(level: String) {
        viewModelScope.launch {
            // Note: GrammarRepository uses getGrammarsByLevels(List<String>)
            // We'll pass the single level of the current grammar
            grammarRepository.getGrammarsByLevels(listOf(level)).collect { grammars ->
                _contextIds.value = grammars.map { it.id }
            }
        }
    }

    fun getGrammarFlow(id: Int): kotlinx.coroutines.flow.Flow<Grammar?> {
        return grammarRepository.getGrammarById(id)
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
        val uniqueId = id ?: "grammar_${text.hashCode()}"
        playTtsUseCase(text, "ja-JP", uniqueId)
    }

    fun openReportDialog() {
        _showReportDialog.value = true
    }

    fun cancelReportDialog() {
        _showReportDialog.value = false
    }

    fun reportContentError(grammarId: Int, errorType: String, description: String?) {
        _showReportDialog.value = false
        viewModelScope.launch {
            val result = contentReportRepository.reportContentError(grammarId, "grammar", errorType, description)
            if (result is Result.Success) {
                _successMessage.value = context.getString(R.string.library_grammar_detail_toast_feedback_success)
            } else if (result is Result.Error) {
                val message = result.exception.message ?: ""
                val friendlyError = when {
                    message.contains("unique_user_item_report") -> context.getString(R.string.library_grammar_detail_toast_report_duplicate)
                    message.contains("connection closed", ignoreCase = true) ||
                    message.contains("Unable to resolve host", ignoreCase = true) ||
                    message.contains("timeout", ignoreCase = true) ||
                    message.contains("HTTP request to", ignoreCase = true) -> context.getString(R.string.library_grammar_detail_toast_network_error)
                    else -> context.getString(R.string.library_grammar_detail_format_feedback_failed, message)
                }
                _errorMessage.value = friendlyError
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
