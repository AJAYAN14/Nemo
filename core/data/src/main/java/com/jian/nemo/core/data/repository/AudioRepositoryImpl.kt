package com.jian.nemo.core.data.repository

import com.jian.nemo.core.data.audio.SoundEffectManager
import com.jian.nemo.core.data.audio.SoundEffectType
import com.jian.nemo.core.data.audio.TtsManager
import com.jian.nemo.core.domain.repository.AudioEffectType
import com.jian.nemo.core.domain.repository.AudioRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val ttsManager: TtsManager,
    private val soundEffectManager: SoundEffectManager
) : AudioRepository {

    override val ttsEvents: kotlinx.coroutines.flow.Flow<com.jian.nemo.core.domain.repository.TtsEvent>
        get() = ttsManager.events

    override fun playTts(text: String, language: String, id: String?) {
        // Ensure initialized
        kotlinx.coroutines.MainScope().launch {
            ttsManager.initialize()

            val locale = when (language) {
                "ja-JP" -> java.util.Locale.JAPAN
                "en-US" -> java.util.Locale.US
                "zh-CN" -> java.util.Locale.CHINA
                else -> java.util.Locale.JAPAN
            }
            ttsManager.speak(text, locale, id = id)
        }
    }

    override fun playExampleTts(japanese: String, chinese: String, id: String?) {
        kotlinx.coroutines.MainScope().launch {
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
        ttsManager.stop()
    }

    override fun release() {
        ttsManager.shutdown()
    }

    override fun getAvailableVoices(): List<com.jian.nemo.core.domain.model.TtsVoice> {
        return ttsManager.getVoices()
    }

    override fun getAvailableChineseVoices(): List<com.jian.nemo.core.domain.model.TtsVoice> {
        return ttsManager.getChineseVoices()
    }

    override suspend fun previewVoice(voiceName: String, text: String, language: java.util.Locale) {
        ttsManager.initialize()
        // Temporarily set voice and speak (does not persist to settings)
        ttsManager.setVoice(voiceName)
        ttsManager.speak(text, language, id = "preview-${System.currentTimeMillis()}")
    }
}
