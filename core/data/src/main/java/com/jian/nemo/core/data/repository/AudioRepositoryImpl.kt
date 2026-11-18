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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val ttsManager: TtsManager,
    private val soundEffectManager: SoundEffectManager,
    @ApplicationScope private val externalScope: CoroutineScope
) : AudioRepository {

    private val jobLock = Any()
    private val playbackGeneration = AtomicLong(0L)
    private var currentPlayJob: Job? = null

    override val ttsEvents: Flow<TtsEvent>
        get() = ttsManager.events

    override fun playTts(
        text: String,
        language: String,
        id: String?,
        speechRate: Float?
    ) {
        launchPlaybackTask { generation ->
            val initialized = ttsManager.initialize()
            if (!initialized || !isActive || playbackGeneration.get() != generation) {
                return@launchPlaybackTask
            }

            val locale = when (language) {
                "ja-JP" -> Locale.JAPAN
                "en-US" -> Locale.US
                "zh-CN" -> Locale.CHINA
                else -> Locale.JAPAN
            }
            ttsManager.speakSuspend(
                text = text,
                language = locale,
                id = id,
                overrideSpeechRate = speechRate,
                shouldProceed = { playbackGeneration.get() == generation }
            )
        }
    }

    override fun playExampleTts(japanese: String, chinese: String, id: String?) {
        launchPlaybackTask { generation ->
            val initialized = ttsManager.initialize()
            if (!initialized || !isActive || playbackGeneration.get() != generation) {
                return@launchPlaybackTask
            }
            ttsManager.speakExampleSuspend(
                japanese = japanese,
                chinese = chinese,
                id = id,
                shouldProceed = { playbackGeneration.get() == generation }
            )
        }
    }

    override fun preloadSoundEffects() {
        soundEffectManager.preload()
    }

    override fun playSoundEffect(type: AudioEffectType) {
        val effectType = when (type) {
            AudioEffectType.CORRECT -> SoundEffectType.CORRECT
            AudioEffectType.ERROR -> SoundEffectType.ERROR
            AudioEffectType.GOOD -> SoundEffectType.GOOD
            AudioEffectType.HARD -> SoundEffectType.HARD
            AudioEffectType.EASY -> SoundEffectType.EASY
            AudioEffectType.OTHER -> SoundEffectType.OTHER
        }
        soundEffectManager.play(effectType)
    }

    override fun stop() {
        invalidatePlaybackTasks()
        ttsManager.stop()
    }

    override fun release() {
        invalidatePlaybackTasks()
        ttsManager.shutdown()
        soundEffectManager.release()
    }

    override suspend fun getAvailableVoices(): List<TtsVoice> {
        return ttsManager.getVoices()
    }

    override suspend fun getAvailableChineseVoices(): List<TtsVoice> {
        return ttsManager.getChineseVoices()
    }

    override suspend fun previewVoice(
        voiceName: String,
        text: String,
        language: Locale,
        id: String?
    ) {
        val generation = invalidatePlaybackTasks()
        val initialized = ttsManager.initialize()
        if (!initialized || playbackGeneration.get() != generation) {
            return
        }
        ttsManager.speakSuspend(
            text = text,
            language = language,
            id = id ?: "preview-$generation",
            overrideVoiceName = voiceName,
            shouldProceed = { playbackGeneration.get() == generation }
        )
    }

    private fun launchPlaybackTask(block: suspend CoroutineScope.(generation: Long) -> Unit) {
        synchronized(jobLock) {
            val generation = playbackGeneration.incrementAndGet()
            currentPlayJob?.cancel()
            currentPlayJob = externalScope.launch {
                block(generation)
            }
        }
    }

    private fun invalidatePlaybackTasks(): Long {
        synchronized(jobLock) {
            val generation = playbackGeneration.incrementAndGet()
            currentPlayJob?.cancel()
            currentPlayJob = null
            return generation
        }
    }
}
