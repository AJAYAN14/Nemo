package com.jian.nemo.core.data.repository

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.jian.nemo.core.data.audio.FakeAudioFocusController
import com.jian.nemo.core.data.audio.FakeTtsEngineFactory
import com.jian.nemo.core.data.audio.SoundEffectManager
import com.jian.nemo.core.data.audio.TtsManager
import com.jian.nemo.core.data.audio.TtsVoiceDescriptor
import com.jian.nemo.core.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AudioRepositoryImplTest {

    private val voices = listOf(
        TtsVoiceDescriptor(
            name = "ja-voice-a",
            locale = Locale.JAPAN,
            isNetworkConnectionRequired = false,
            quality = Voice.QUALITY_HIGH,
            features = emptySet()
        ),
        TtsVoiceDescriptor(
            name = "ja-voice-b",
            locale = Locale.JAPAN,
            isNetworkConnectionRequired = false,
            quality = Voice.QUALITY_VERY_HIGH,
            features = emptySet()
        ),
        TtsVoiceDescriptor(
            name = "zh-voice-a",
            locale = Locale.CHINA,
            isNetworkConnectionRequired = false,
            quality = Voice.QUALITY_HIGH,
            features = emptySet()
        )
    )

    @Test
    fun `stop during pending initialization prevents stale playTts from speaking`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val externalScope = CoroutineScope(SupervisorJob() + dispatcher)
        val factory = FakeTtsEngineFactory(voices = voices, autoCompleteInit = false)
        val ttsManager = createManager(dispatcher, factory)
        val repository = AudioRepositoryImpl(
            ttsManager = ttsManager,
            soundEffectManager = mockk<SoundEffectManager>(relaxed = true),
            externalScope = externalScope
        )

        try {
            repository.playTts(text = "古い単語", id = "stale-1")
            runCurrent()
            assertEquals(1, factory.createCount)

            repository.stop()
            runCurrent()

            factory.completeInit(0, TextToSpeech.SUCCESS)
            advanceUntilIdle()

            assertTrue(factory.engines.single().speakCalls.isEmpty())
        } finally {
            externalScope.cancel()
            ttsManager.closeForTest()
        }
    }

    @Test
    fun `rapid playTts calls only speak latest request after init`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val externalScope = CoroutineScope(SupervisorJob() + dispatcher)
        val factory = FakeTtsEngineFactory(voices = voices, autoCompleteInit = false)
        val ttsManager = createManager(dispatcher, factory)
        val repository = AudioRepositoryImpl(
            ttsManager = ttsManager,
            soundEffectManager = mockk<SoundEffectManager>(relaxed = true),
            externalScope = externalScope
        )

        try {
            repository.playTts(text = "一", id = "1")
            repository.playTts(text = "二", id = "2")
            repository.playTts(text = "三", id = "3")
            runCurrent()

            factory.completeInit(0, TextToSpeech.SUCCESS)
            advanceUntilIdle()

            val speakCalls = factory.engines.single().speakCalls
            assertEquals(1, speakCalls.size)
            assertEquals("三", speakCalls.single().text)
        } finally {
            externalScope.cancel()
            ttsManager.closeForTest()
        }
    }

    @Test
    fun `previewVoice speaks with temporary voice and respects stop cancellation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val externalScope = CoroutineScope(SupervisorJob() + dispatcher)
        val factory = FakeTtsEngineFactory(voices = voices, autoCompleteInit = false)
        val ttsManager = createManager(
            dispatcher = dispatcher,
            factory = factory,
            savedJapaneseVoice = "ja-voice-a"
        )
        val repository = AudioRepositoryImpl(
            ttsManager = ttsManager,
            soundEffectManager = mockk<SoundEffectManager>(relaxed = true),
            externalScope = externalScope
        )

        try {
            val stalePreview = launch {
                repository.previewVoice("ja-voice-b", "キャンセル対象", Locale.JAPAN)
            }
            runCurrent()

            repository.stop()
            factory.completeInit(0, TextToSpeech.SUCCESS)
            advanceUntilIdle()
            stalePreview.join()
            assertTrue(factory.engines.single().speakCalls.isEmpty())

            repository.previewVoice("ja-voice-b", "こんにちは", Locale.JAPAN)
            advanceUntilIdle()

            val speakCalls = factory.engines.single().speakCalls
            assertEquals(1, speakCalls.size)
            assertEquals("ja-voice-b", speakCalls.single().voiceName)
            assertEquals("こんにちは", speakCalls.single().text)
        } finally {
            externalScope.cancel()
            ttsManager.closeForTest()
        }
    }

    @Test
    fun `playTts with temporary speechRate does not mutate global speechRate`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val externalScope = CoroutineScope(SupervisorJob() + dispatcher)
        val factory = FakeTtsEngineFactory(voices = voices, autoCompleteInit = true)
        val ttsManager = createManager(
            dispatcher = dispatcher,
            factory = factory,
            speechRate = 1.2f
        )
        val repository = AudioRepositoryImpl(
            ttsManager = ttsManager,
            soundEffectManager = mockk<SoundEffectManager>(relaxed = true),
            externalScope = externalScope
        )

        try {
            repository.playTts(text = "聴解テスト", speechRate = 0.75f, id = "l1")
            advanceUntilIdle()

            repository.playTts(text = "通常再生", id = "w1")
            advanceUntilIdle()

            val calls = factory.engines.single().speakCalls
            assertEquals(2, calls.size)
            assertEquals(0.75f, calls[0].speechRate)
            assertEquals(1.2f, calls[1].speechRate)
        } finally {
            externalScope.cancel()
            ttsManager.closeForTest()
        }
    }

    @Test
    fun `getAvailableVoices and getAvailableChineseVoices return filtered cached voices`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val externalScope = CoroutineScope(SupervisorJob() + dispatcher)
        val factory = FakeTtsEngineFactory(voices = voices, autoCompleteInit = true)
        val ttsManager = createManager(dispatcher, factory)
        val repository = AudioRepositoryImpl(
            ttsManager = ttsManager,
            soundEffectManager = mockk<SoundEffectManager>(relaxed = true),
            externalScope = externalScope
        )

        try {
            val jpVoices = repository.getAvailableVoices()
            val cnVoices = repository.getAvailableChineseVoices()

            assertEquals(listOf("ja-voice-a", "ja-voice-b"), jpVoices.map { it.name })
            assertEquals(listOf("zh-voice-a"), cnVoices.map { it.name })
            assertEquals(1, factory.engines.single().getVoicesCalls)
        } finally {
            externalScope.cancel()
            ttsManager.closeForTest()
        }
    }

    @Test
    fun `previewVoice forwards explicit id and generates unique default ids`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val externalScope = CoroutineScope(SupervisorJob() + dispatcher)
        val factory = FakeTtsEngineFactory(voices = voices, autoCompleteInit = true)
        val ttsManager = createManager(dispatcher, factory)
        val repository = AudioRepositoryImpl(
            ttsManager = ttsManager,
            soundEffectManager = mockk<SoundEffectManager>(relaxed = true),
            externalScope = externalScope
        )

        try {
            repository.previewVoice("ja-voice-a", "最初", Locale.JAPAN, id = "preview-explicit-1")
            repository.previewVoice("ja-voice-b", "二番目", Locale.JAPAN)
            repository.previewVoice("ja-voice-b", "三番目", Locale.JAPAN)
            advanceUntilIdle()

            val calls = factory.engines.single().speakCalls
            assertEquals(3, calls.size)
            assertEquals("preview-explicit-1", calls[0].utteranceId)
            assertTrue(calls[1].utteranceId.startsWith("preview-"))
            assertTrue(calls[2].utteranceId.startsWith("preview-"))
            assertTrue(calls[1].utteranceId != calls[2].utteranceId)
        } finally {
            externalScope.cancel()
            ttsManager.closeForTest()
        }
    }

    private fun createManager(
        dispatcher: TestDispatcher,
        factory: FakeTtsEngineFactory,
        speechRate: Float = 1.0f,
        savedJapaneseVoice: String? = null
    ): TtsManager {
        val settings = mockk<SettingsRepository>(relaxed = true)
        every { settings.ttsSpeechRateFlow } returns MutableStateFlow(speechRate)
        every { settings.ttsPitchFlow } returns MutableStateFlow(1.0f)
        every { settings.ttsVoiceNameFlow } returns MutableStateFlow(savedJapaneseVoice)
        every { settings.ttsChineseVoiceNameFlow } returns MutableStateFlow(null)

        return TtsManager.createForTest(
            settingsRepository = settings,
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = FakeAudioFocusController()
        )
    }
}
