package com.jian.nemo.feature.learning.domain

import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.TtsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class LearningTtsManager @Inject constructor(
    private val audioRepository: AudioRepository
) {
    private val _playingAudioId = MutableStateFlow<String?>(null)
    val playingAudioId: StateFlow<String?> = _playingAudioId.asStateFlow()

    fun observeTtsEvents(scope: CoroutineScope) {
        scope.launch {
            audioRepository.ttsEvents.collect { event ->
                when (event) {
                    is TtsEvent.OnStart -> {
                        val normalizedId = event.id?.substringBefore("-jp")?.substringBefore("-cn")
                        _playingAudioId.update { normalizedId }
                    }
                    is TtsEvent.OnDone -> {
                        // 如果是日语部分结束，暂时不清除状态，等待中文部分开始，防止动画闪烁
                        if (event.id?.endsWith("-jp") == true) return@collect

                        val normalizedId = event.id?.substringBefore("-cn")
                        _playingAudioId.update { current ->
                            if (current == normalizedId) null else current
                        }
                    }
                    is TtsEvent.OnError -> {
                        _playingAudioId.update { null }
                    }
                    TtsEvent.GoogleTtsMissing -> {
                        _playingAudioId.update { null }
                    }
                }
            }
        }
    }

    /**
     * 朗读单词（日语 + 中文）
     */
    fun speakWord(text: String, chinese: String = "") {
        audioRepository.stop()
        if (chinese.isNotBlank()) {
            audioRepository.playExampleTts(text, chinese, "word")
        } else {
            audioRepository.playTts(text, "ja-JP", "word")
        }
    }

    /**
     * 朗读例句（日语 + 中文）
     */
    fun speakExample(japanese: String, chinese: String, id: String) {
        audioRepository.stop()
        audioRepository.playExampleTts(japanese, chinese, id)
    }

    fun stop() {
        audioRepository.stop()
    }
}
