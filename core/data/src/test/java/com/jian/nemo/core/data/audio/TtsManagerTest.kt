package com.jian.nemo.core.data.audio

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.TtsEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class TtsManagerTest {

    private val defaultVoices = listOf(
        TtsVoiceDescriptor(
            name = "ja-jp-x-jab-local",
            locale = Locale.JAPAN,
            isNetworkConnectionRequired = false,
            quality = Voice.QUALITY_HIGH,
            features = setOf("latency_very_low")
        ),
        TtsVoiceDescriptor(
            name = "ja-jp-x-jac-local",
            locale = Locale.JAPAN,
            isNetworkConnectionRequired = false,
            quality = Voice.QUALITY_VERY_HIGH,
            features = emptySet()
        ),
        TtsVoiceDescriptor(
            name = "cmn-cn-x-ccc-local",
            locale = Locale.CHINA,
            isNetworkConnectionRequired = false,
            quality = Voice.QUALITY_HIGH,
            features = emptySet()
        )
    )

    @Test
    fun `initialize and speak run off caller thread on single serialized worker`() = runBlocking {
        val executor = Executors.newFixedThreadPool(4) { runnable ->
            Thread(runnable, "tts-test-pool-${System.nanoTime()}")
        }
        val poolDispatcher = executor.asCoroutineDispatcher()
        val callerThreadId = Thread.currentThread().id

        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val focusController = FakeAudioFocusController()
        val settings = createMockSettings(savedJapaneseVoice = "ja-jp-x-jab-local")
        val manager = TtsManager.createForTest(
            settingsRepository = settings,
            ioDispatcher = poolDispatcher,
            engineFactory = factory,
            audioFocusController = focusController
        )

        try {
            assertTrue(manager.initialize())
            assertEquals(2, manager.getVoices().size)
            assertEquals(1, manager.getChineseVoices().size)
            manager.speakSuspend("食べる", Locale.JAPAN, id = "w1")
            manager.stop()
            manager.shutdown()
            delay(100)

            assertTrue(factory.recordedThreadIds.isNotEmpty())
            assertTrue(factory.recordedThreadIds.none { it == callerThreadId })
            val engine = factory.engines.single()
            assertTrue(engine.recordedThreadIds.none { it == callerThreadId })
            assertTrue(focusController.recordedThreadIds.none { it == callerThreadId })
            assertEquals(0, engine.concurrentOverlapCount.get())
        } finally {
            manager.closeForTest()
            poolDispatcher.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun `initialize deduplicates concurrent callers and isolates caller cancellation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = false)
        val focusController = FakeAudioFocusController()
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = focusController
        )

        try {
            val caller1 = async { manager.initialize() }
            val caller2 = async { manager.initialize() }
            val caller3 = async { manager.initialize() }

            runCurrent()
            assertEquals(1, factory.createCount)

            caller1.cancel(CancellationException("caller cancelled"))
            runCurrent()

            factory.completeInit(0, TextToSpeech.SUCCESS)
            advanceUntilIdle()

            assertTrue(caller1.isCancelled)
            assertTrue(caller2.await())
            assertTrue(caller3.await())
            assertEquals(1, factory.createCount)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `initialize times out cleans up dead engine and allows clean retry`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = false)
        val focusController = FakeAudioFocusController()
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = focusController,
            initTimeoutMs = 500L
        )

        val events = mutableListOf<TtsEvent>()
        val collector = launch { manager.events.collect { events.add(it) } }

        try {
            val firstInit = async { manager.initialize() }
            advanceTimeBy(600L)
            advanceUntilIdle()

            assertFalse(firstInit.await())
            assertTrue(factory.engines[0].shutdownCalled)
            assertTrue(events.any { it is TtsEvent.OnError && it.id == "init_timeout" })

            // 过期引擎的迟到回调必须被忽略
            factory.completeInit(0, TextToSpeech.SUCCESS)
            advanceUntilIdle()

            val retryInit = async { manager.initialize() }
            runCurrent()
            assertEquals(2, factory.createCount)
            factory.completeInit(1, TextToSpeech.SUCCESS)
            advanceUntilIdle()

            assertTrue(retryInit.await())
        } finally {
            collector.cancel()
            manager.closeForTest()
        }
    }

    @Test
    fun `getVoices and getChineseVoices return cached lists without redundant IPC`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = FakeAudioFocusController()
        )

        try {
            val jp1 = manager.getVoices()
            val cn1 = manager.getChineseVoices()
            val jp2 = manager.getVoices()
            val cn2 = manager.getChineseVoices()

            assertEquals(2, jp1.size)
            assertEquals(1, cn1.size)
            assertEquals(jp1, jp2)
            assertEquals(cn1, cn2)
            assertEquals(1, factory.engines.single().getVoicesCalls)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `consecutive same language speaks minimize binder IPC calls`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(savedJapaneseVoice = "ja-jp-x-jab-local"),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = FakeAudioFocusController()
        )

        try {
            assertTrue(manager.initialize())
            val engine = factory.engines.single()
            val voicesCallsAfterInit = engine.getVoicesCalls
            val setVoiceCallsAfterInit = engine.setVoiceCalls.size
            val setLangCallsAfterInit = engine.setLanguageCalls.size
            val rateCallsAfterInit = engine.setSpeechRateCalls.size
            val pitchCallsAfterInit = engine.setPitchCalls.size

            manager.speakSuspend("猫", Locale.JAPAN, id = "1")
            manager.speakSuspend("犬", Locale.JAPAN, id = "2")
            manager.speakSuspend("鳥", Locale.JAPAN, id = "3")

            assertEquals(3, engine.speakCalls.size)
            assertEquals(voicesCallsAfterInit, engine.getVoicesCalls)
            assertEquals(setVoiceCallsAfterInit, engine.setVoiceCalls.size)
            assertEquals(setLangCallsAfterInit, engine.setLanguageCalls.size)
            assertEquals(rateCallsAfterInit, engine.setSpeechRateCalls.size)
            assertEquals(pitchCallsAfterInit, engine.setPitchCalls.size)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `overrideVoiceName previews target voice without overwriting saved voice`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(savedJapaneseVoice = "ja-jp-x-jab-local"),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = FakeAudioFocusController()
        )

        try {
            assertTrue(manager.initialize())
            val engine = factory.engines.single()

            manager.speakSuspend(
                text = "こんにちは",
                language = Locale.JAPAN,
                id = "preview-1",
                overrideVoiceName = "ja-jp-x-jac-local"
            )
            assertEquals("ja-jp-x-jac-local", engine.currentVoiceName)

            manager.speakSuspend(
                text = "ありがとう",
                language = Locale.JAPAN,
                id = "normal-1"
            )
            assertEquals("ja-jp-x-jab-local", engine.currentVoiceName)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `overrideSpeechRate applies only to single utterance and preserves global rate`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(speechRate = 1.25f),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = FakeAudioFocusController()
        )

        try {
            assertTrue(manager.initialize())
            val engine = factory.engines.single()
            assertEquals(1.25f, engine.currentSpeechRate)

            manager.speakSuspend(
                text = "ゆっくり",
                language = Locale.JAPAN,
                id = "listen-1",
                overrideSpeechRate = 0.7f
            )
            assertEquals(0.7f, engine.currentSpeechRate)

            manager.speakSuspend(
                text = "ふつう",
                language = Locale.JAPAN,
                id = "word-1"
            )
            assertEquals(1.25f, engine.currentSpeechRate)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `speakExample retains audio focus after jp segment until cn segment completes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val focusController = FakeAudioFocusController()
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = focusController
        )

        try {
            assertTrue(manager.initialize())
            val engine = factory.engines.single()

            manager.speakExampleSuspend("これは本です", "这是一本书", id = "ex1")
            assertEquals(2, engine.speakCalls.size)
            assertEquals("ex1-jp", engine.speakCalls[0].utteranceId)
            assertEquals(TextToSpeech.QUEUE_FLUSH, engine.speakCalls[0].queueMode)
            assertEquals("ex1-cn", engine.speakCalls[1].utteranceId)
            assertEquals(TextToSpeech.QUEUE_ADD, engine.speakCalls[1].queueMode)
            assertEquals(1, focusController.requestCount)
            assertEquals(0, focusController.abandonCount)

            engine.listener?.onDone("ex1-jp")
            advanceUntilIdle()
            assertEquals(0, focusController.abandonCount)

            engine.listener?.onDone("ex1-cn")
            advanceUntilIdle()
            assertEquals(1, focusController.abandonCount)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `stale onStop from interrupted utterance does not abandon audio focus of new utterance`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val focusController = FakeAudioFocusController()
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = focusController
        )

        try {
            assertTrue(manager.initialize())
            val engine = factory.engines.single()

            manager.speakSuspend("最初", Locale.JAPAN, id = "u1")
            manager.speakSuspend("次", Locale.JAPAN, id = "u2")

            engine.listener?.onStop("u1", true)
            advanceUntilIdle()
            assertEquals(0, focusController.abandonCount)

            engine.listener?.onDone("u2")
            advanceUntilIdle()
            assertEquals(1, focusController.abandonCount)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `events shared flow does not replay old events to late subscribers`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(
            voices = defaultVoices,
            autoCompleteInit = true,
            googleInstalled = false
        )
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = FakeAudioFocusController()
        )

        try {
            assertFalse(manager.initialize())

            val lateEvents = mutableListOf<TtsEvent>()
            val collector = launch { manager.events.collect { lateEvents.add(it) } }
            runCurrent()

            assertTrue(lateEvents.isEmpty())
            collector.cancel()
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `shutdown cleans up engine and allows subsequent reinitialization`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val focusController = FakeAudioFocusController()
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = focusController
        )

        try {
            assertTrue(manager.initialize())
            manager.speakSuspend("テスト", Locale.JAPAN, id = "s1")

            manager.shutdown()
            advanceUntilIdle()

            assertTrue(factory.engines[0].shutdownCalled)
            assertEquals(1, focusController.abandonCount)

            assertTrue(manager.initialize())
            assertEquals(2, factory.createCount)
            assertNotEquals(factory.engines[0], factory.engines[1])
            manager.speakSuspend("再開", Locale.JAPAN, id = "s2")
            assertEquals(1, factory.engines[1].speakCalls.size)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `consecutive QUEUE_FLUSH speaks reuse audio focus and ignore stale focus loss callbacks`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val focusController = FakeAudioFocusController()
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = focusController
        )

        try {
            assertTrue(manager.initialize())
            val engine = factory.engines.single()

            manager.speakSuspend("最初", Locale.JAPAN, id = "q1")
            val firstFocusLoss = focusController.lastOnFocusLoss
            manager.speakSuspend("次", Locale.JAPAN, id = "q2")

            // 连续 QUEUE_FLUSH 复用现有音频焦点，不重复请求导致自抢焦点
            assertEquals(1, focusController.requestCount)
            assertEquals(0, focusController.abandonCount)
            assertEquals(0, engine.stopCalls)

            // q2 正常完成后释放焦点
            engine.listener?.onDone("q2")
            advanceUntilIdle()
            assertEquals(1, focusController.abandonCount)

            // 再次开始新朗读 q3，旧焦点回调若迟到不得误杀 q3
            manager.speakSuspend("三番目", Locale.JAPAN, id = "q3")
            assertEquals(2, focusController.requestCount)
            firstFocusLoss?.invoke()
            advanceUntilIdle()
            assertEquals(0, engine.stopCalls)
            assertEquals(1, focusController.abandonCount)

            // 当前有效焦点丧失回调应正常停止 q3 并释放焦点
            focusController.lastOnFocusLoss?.invoke()
            advanceUntilIdle()
            assertEquals(1, engine.stopCalls)
            assertEquals(2, focusController.abandonCount)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `speakExample with blank chinese releases audio focus when jp segment completes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val focusController = FakeAudioFocusController()
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = focusController
        )

        try {
            assertTrue(manager.initialize())
            val engine = factory.engines.single()

            manager.speakExampleSuspend("これは本です", "", id = "jp-only")
            assertEquals(1, engine.speakCalls.size)
            assertEquals("jp-only-jp", engine.speakCalls.single().utteranceId)
            assertEquals(1, focusController.requestCount)
            assertEquals(0, focusController.abandonCount)

            engine.listener?.onDone("jp-only-jp")
            advanceUntilIdle()
            assertEquals(1, focusController.abandonCount)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `speak ERROR reinitializes engine once and emits OnError if retry also fails`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val focusController = FakeAudioFocusController()
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = focusController
        )

        val events = mutableListOf<TtsEvent>()
        val collector = launch { manager.events.collect { events.add(it) } }

        try {
            assertTrue(manager.initialize())
            factory.engines[0].speakResult = TextToSpeech.ERROR

            manager.speakSuspend("回復テスト", Locale.JAPAN, id = "err-recover")
            advanceUntilIdle()

            assertEquals(2, factory.createCount)
            assertTrue(factory.engines[0].shutdownCalled)
            assertEquals(1, factory.engines[1].speakCalls.size)
            assertEquals("回復テスト", factory.engines[1].speakCalls.single().text)

            // 第二次恢复时新引擎依然失败：应仅重试一次并发出 OnError，避免死循环与 UI 卡死
            factory.nextSpeakResult = TextToSpeech.ERROR
            factory.engines[1].speakResult = TextToSpeech.ERROR
            manager.speakSuspend("二重失敗", Locale.JAPAN, id = "err-twice")
            advanceUntilIdle()

            assertEquals(3, factory.createCount)
            assertTrue(events.any { it is TtsEvent.OnError && it.id == "err-twice" })
        } finally {
            collector.cancel()
            manager.closeForTest()
        }
    }

    @Test
    fun `blank text after cleaning skips engine speak and emits OnDone`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeTtsEngineFactory(voices = defaultVoices, autoCompleteInit = true)
        val focusController = FakeAudioFocusController()
        val manager = TtsManager.createForTest(
            settingsRepository = createMockSettings(),
            ioDispatcher = dispatcher,
            engineFactory = factory,
            audioFocusController = focusController
        )

        val events = mutableListOf<TtsEvent>()
        val collector = launch { manager.events.collect { events.add(it) } }

        try {
            assertTrue(manager.initialize())
            manager.speakSuspend("（注釈のみ）", Locale.JAPAN, id = "blank-1")
            advanceUntilIdle()

            assertTrue(factory.engines.single().speakCalls.isEmpty())
            assertEquals(0, focusController.requestCount)
            assertTrue(events.any { it is TtsEvent.OnDone && it.id == "blank-1" })
        } finally {
            collector.cancel()
            manager.closeForTest()
        }
    }

    private fun createMockSettings(
        speechRate: Float = 1.0f,
        pitch: Float = 1.0f,
        savedJapaneseVoice: String? = null,
        savedChineseVoice: String? = null
    ): SettingsRepository {
        val settings = mockk<SettingsRepository>(relaxed = true)
        every { settings.ttsSpeechRateFlow } returns MutableStateFlow(speechRate)
        every { settings.ttsPitchFlow } returns MutableStateFlow(pitch)
        every { settings.ttsVoiceNameFlow } returns MutableStateFlow(savedJapaneseVoice)
        every { settings.ttsChineseVoiceNameFlow } returns MutableStateFlow(savedChineseVoice)
        return settings
    }
}

internal class FakeTtsEngineFactory(
    private val voices: List<TtsVoiceDescriptor>,
    private val autoCompleteInit: Boolean,
    var googleInstalled: Boolean = true
) : TtsEngineFactory {

    val engines = mutableListOf<FakeTtsEngine>()
    val recordedThreadIds = CopyOnWriteArrayList<Long>()
    private val initCallbacks = mutableListOf<(Int) -> Unit>()
    var nextSpeakResult: Int = TextToSpeech.SUCCESS

    var createCount: Int = 0
        private set

    override fun isGoogleTtsInstalled(): Boolean {
        recordedThreadIds.add(Thread.currentThread().id)
        return googleInstalled
    }

    override fun create(enginePackage: String, onInit: (status: Int) -> Unit): TtsEngine {
        recordedThreadIds.add(Thread.currentThread().id)
        createCount++
        val engine = FakeTtsEngine(voices).apply {
            speakResult = nextSpeakResult
        }
        engines.add(engine)
        initCallbacks.add(onInit)
        if (autoCompleteInit) {
            onInit(TextToSpeech.SUCCESS)
        }
        return engine
    }

    fun completeInit(index: Int, status: Int) {
        initCallbacks[index].invoke(status)
    }
}

internal class FakeTtsEngine(
    private val voices: List<TtsVoiceDescriptor>
) : TtsEngine {

    data class SpeakCall(
        val text: String,
        val queueMode: Int,
        val utteranceId: String,
        val voiceName: String?,
        val language: Locale?,
        val speechRate: Float
    )

    val recordedThreadIds = CopyOnWriteArrayList<Long>()
    val concurrentOverlapCount = AtomicInteger(0)
    private val activeCalls = AtomicInteger(0)

    val setLanguageCalls = mutableListOf<Locale>()
    val setVoiceCalls = mutableListOf<String>()
    val setSpeechRateCalls = mutableListOf<Float>()
    val setPitchCalls = mutableListOf<Float>()
    val speakCalls = mutableListOf<SpeakCall>()
    var getVoicesCalls: Int = 0
        private set
    var stopCalls: Int = 0
        private set
    var shutdownCalled: Boolean = false
        private set

    var currentVoiceName: String? = null
        private set
    var currentLanguage: Locale? = null
        private set
    var currentSpeechRate: Float = 1.0f
        private set
    var currentPitch: Float = 1.0f
        private set
    var listener: TtsEngineListener? = null
        private set
    var speakResult: Int = TextToSpeech.SUCCESS

    private inline fun <T> trackExecution(block: () -> T): T {
        recordedThreadIds.add(Thread.currentThread().id)
        if (activeCalls.incrementAndGet() > 1) {
            concurrentOverlapCount.incrementAndGet()
        }
        return try {
            block()
        } finally {
            activeCalls.decrementAndGet()
        }
    }

    override fun setLanguage(locale: Locale): Int = trackExecution {
        setLanguageCalls.add(locale)
        currentLanguage = locale
        currentVoiceName = null
        TextToSpeech.LANG_AVAILABLE
    }

    override fun getVoices(): List<TtsVoiceDescriptor> = trackExecution {
        getVoicesCalls++
        voices
    }

    override fun setVoice(voiceName: String): Int = trackExecution {
        setVoiceCalls.add(voiceName)
        val match = voices.find { it.name == voiceName } ?: return@trackExecution TtsEngine.VOICE_NOT_FOUND
        currentVoiceName = voiceName
        currentLanguage = match.locale
        TextToSpeech.SUCCESS
    }

    override fun setSpeechRate(rate: Float): Int = trackExecution {
        setSpeechRateCalls.add(rate)
        currentSpeechRate = rate
        TextToSpeech.SUCCESS
    }

    override fun setPitch(pitch: Float): Int = trackExecution {
        setPitchCalls.add(pitch)
        currentPitch = pitch
        TextToSpeech.SUCCESS
    }

    override fun speak(text: String, queueMode: Int, utteranceId: String): Int = trackExecution {
        speakCalls.add(
            SpeakCall(
                text = text,
                queueMode = queueMode,
                utteranceId = utteranceId,
                voiceName = currentVoiceName,
                language = currentLanguage,
                speechRate = currentSpeechRate
            )
        )
        speakResult
    }

    override fun stop(): Int = trackExecution {
        stopCalls++
        TextToSpeech.SUCCESS
    }

    override fun shutdown() = trackExecution {
        shutdownCalled = true
    }

    override fun setProgressListener(listener: TtsEngineListener) = trackExecution {
        this.listener = listener
    }
}

internal class FakeAudioFocusController(
    var grantFocus: Boolean = true
) : TtsAudioFocusController {

    val recordedThreadIds = CopyOnWriteArrayList<Long>()
    var requestCount: Int = 0
        private set
    var abandonCount: Int = 0
        private set
    var lastOnFocusLoss: (() -> Unit)? = null
        private set

    override fun requestFocus(onFocusLoss: () -> Unit): Boolean {
        recordedThreadIds.add(Thread.currentThread().id)
        requestCount++
        lastOnFocusLoss = onFocusLoss
        return grantFocus
    }

    override fun abandonFocus() {
        recordedThreadIds.add(Thread.currentThread().id)
        abandonCount++
    }
}
