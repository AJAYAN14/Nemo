package com.jian.nemo.feature.learning.presentation.kana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.TtsEvent
import com.jian.nemo.core.domain.usecase.audio.PlayTtsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KanaChartViewModel @Inject constructor(
    private val playTtsUseCase: PlayTtsUseCase,
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _playingAudioId = MutableStateFlow<String?>(null)
    val playingAudioId = _playingAudioId.asStateFlow()

    init {
        observeTtsEvents()
    }

    private fun observeTtsEvents() {
        viewModelScope.launch {
            audioRepository.ttsEvents.collect { event ->
                when (event) {
                    is TtsEvent.OnStart -> _playingAudioId.value = event.id
                    is TtsEvent.OnDone, is TtsEvent.OnStop -> {
                        if (_playingAudioId.value == (event as? TtsEvent.OnDone)?.id || 
                            _playingAudioId.value == (event as? TtsEvent.OnStop)?.id) {
                            _playingAudioId.value = null
                        }
                    }
                    is TtsEvent.OnError -> {
                        if (_playingAudioId.value == event.id) {
                            _playingAudioId.value = null
                        }
                    }
                    TtsEvent.GoogleTtsMissing -> _playingAudioId.value = null
                }
            }
        }
    }

    fun speakKana(text: String, id: String) {
        playTtsUseCase(text = text, language = "ja-JP", id = id)
    }
}
