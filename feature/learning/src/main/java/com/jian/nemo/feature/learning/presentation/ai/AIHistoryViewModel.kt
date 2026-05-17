package com.jian.nemo.feature.learning.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.AIExerciseHistory
import com.jian.nemo.core.domain.repository.AIWorkshopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.TtsEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


@HiltViewModel
class AIHistoryViewModel @Inject constructor(
    private val repository: AIWorkshopRepository,
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _playingAudioId = MutableStateFlow<String?>(null)
    val playingAudioId: StateFlow<String?> = _playingAudioId.asStateFlow()


    val historyState: StateFlow<List<AIExerciseHistory>> = repository.getExerciseHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        observeTtsEvents()
    }

    private fun observeTtsEvents() {
        viewModelScope.launch {
            audioRepository.ttsEvents.collect { event ->
                when (event) {
                    is TtsEvent.OnStart -> {
                        _playingAudioId.update { event.id }
                    }
                    is TtsEvent.OnDone, is TtsEvent.OnError -> {
                        _playingAudioId.update { 
                            if (it == (event as? TtsEvent.OnDone)?.id || 
                                it == (event as? TtsEvent.OnError)?.id) null else it
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun speakText(text: String, id: String) {
        audioRepository.playTts(text, id = id)
    }


    fun deleteHistory(historyId: Int) {
        // TODO: Repository 还没加删除单个的方法，以后再说
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
