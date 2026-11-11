package com.jian.nemo.core.data.repository

import com.jian.nemo.core.common.di.ApplicationScope
import com.jian.nemo.core.data.audio.SoundEffectManager
import com.jian.nemo.core.data.audio.SoundEffectType
import com.jian.nemo.core.data.audio.TtsManager
import com.jian.nemo.core.domain.model.TtsVoice
import com.jian.nemo.core.domain.repository.AudioEffectType
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.TtsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val ttsManager: TtsManager,
    private val soundEffectManager: SoundEffectManager,
    @ApplicationScope private val externalScope: CoroutineScope
) : AudioRepository {

    private var currentPlayJob: Job? = null

    override val ttsEvents: Flow<TtsEvent>
        get() = ttsManager.events

    override fun playTts(text: String, language: String, id: String?) {
        currentPlayJob?.cancel()
        currentPlayJob = externalScope.launch(Dispatchers.Main.immediate) {
            ttsManager.initialize()

            val locale = when (language) {
                "ja-JP" -> Locale.JAPAN
                "en-US" -> Locale.US
                "zh-CN" -> Locale.CHINA
                else -> Locale.JAPAN
            }
            ttsManager.speak(text, locale, id = id)
        }
    }

    override fun playExampleTts(japanese: String, chinese: String, id: String?) {
        currentPlayJob?.cancel()
        currentPlayJob = externalScope.launch(Dispatchers.Main.immediate) {
            ttsManager.initialize()
            ttsManager.speakExample(japanese, chinese, id)
        }
    }

    override fun playSoundEffect(type: AudioEffectType) {
        val effectType = when (type) {
            AudioEffectType.CORRECT -> SoundEffectType.CORRECT
            AudioEffectType.WRONG -> SoundEffectType.WRONG
            AudioEffectType.COMPLETE -> SoundEffectType.COMPLETE
        }
        soundEffectManager.play(effectType)
    }

    override fun stop() {
        currentPlayJob?.cancel()
        currentPlayJob = null
        ttsManager.stop()
    }

    override fun release() {
        currentPlayJob?.cancel()
        currentPlayJob = null
        ttsManager.shutdown()
    }

    override fun getAvailableVoices(): List<TtsVoice> {
        return ttsManager.getVoices()
    }

    override fun getAvailableChineseVoices(): List<TtsVoice> {
        return ttsManager.getChineseVoices()
    }

    override suspend fun previewVoice(voiceName: String, text: String, language: Locale) {
        ttsManager.initialize()
        // Temporarily set voice and speak (does not persist to settings)
        ttsManager.setVoice(voiceName)
        ttsManager.speak(text, language, id = "preview-${System.currentTimeMillis()}")
    }
}
