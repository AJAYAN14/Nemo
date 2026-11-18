package com.jian.nemo.core.data.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import com.jian.nemo.core.common.network.Dispatcher
import com.jian.nemo.core.common.network.NemoDispatchers
import com.jian.nemo.core.domain.model.TtsVoice
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.TtsEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager private constructor(
    private val settingsRepository: SettingsRepository,
    ioDispatcher: CoroutineDispatcher,
    private val engineFactory: TtsEngineFactory,
    private val audioFocusController: TtsAudioFocusController,
    private val logger: TtsLogger,
    private val initTimeoutMs: Long
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
        @Dispatcher(NemoDispatchers.IO) ioDispatcher: CoroutineDispatcher
    ) : this(
        settingsRepository = settingsRepository,
        ioDispatcher = ioDispatcher,
        engineFactory = AndroidTtsEngineFactory(context),
        audioFocusController = AndroidTtsAudioFocusController(context),
        logger = AndroidTtsLogger,
        initTimeoutMs = DEFAULT_INIT_TIMEOUT_MS
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val ttsDispatcher: CoroutineDispatcher = ioDispatcher.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + ttsDispatcher)

    private var ttsEngine: TtsEngine? = null
    private var isInitialized = false
    private var initDeferred: CompletableDeferred<Boolean>? = null
    private var engineGeneration = 0L

    private var currentSpeechRate: Float = 1.0f
    private var currentPitch: Float = 1.0f
    private var savedJapaneseVoiceName: String? = null
    private var savedChineseVoiceName: String? = null

    private var cachedVoiceMap: Map<String, TtsVoiceDescriptor> = emptyMap()
    private var cachedJapaneseVoices: List<TtsVoice> = emptyList()
    private var cachedChineseVoices: List<TtsVoice> = emptyList()

    private var activeVoiceName: String? = null
    private var activeLanguage: Locale? = null
    private var appliedSpeechRate: Float? = null
    private var appliedPitch: Float? = null

    private var hasAudioFocus = false
    private var focusGeneration = 0L
    private val activeUtteranceIds = mutableSetOf<String>()

    private val _events = MutableSharedFlow<TtsEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events = _events.asSharedFlow()

    init {
        observeSettings()
    }

    private fun observeSettings() {
        scope.launch {
            settingsRepository.ttsSpeechRateFlow.collect { rate ->
                currentSpeechRate = rate
                if (isInitialized && appliedSpeechRate != rate) {
                    ttsEngine?.setSpeechRate(rate)
                    appliedSpeechRate = rate
                }
            }
        }

        scope.launch {
            settingsRepository.ttsPitchFlow.collect { pitch ->
                currentPitch = pitch
                if (isInitialized && appliedPitch != pitch) {
                    ttsEngine?.setPitch(pitch)
                    appliedPitch = pitch
                }
            }
        }

        scope.launch {
            settingsRepository.ttsVoiceNameFlow.collect { voiceName ->
                savedJapaneseVoiceName = voiceName
                if (isInitialized && !voiceName.isNullOrBlank() && isJapaneseOrUnset(activeLanguage)) {
                    setVoiceInternal(voiceName)
                }
            }
        }

        scope.launch {
            settingsRepository.ttsChineseVoiceNameFlow.collect { voiceName ->
                savedChineseVoiceName = voiceName
            }
        }
    }

    /**
     * 在后台单线程初始化 TTS 引擎，支持并发调用合并与调用方协程取消隔离。
     */
    suspend fun initialize(): Boolean {
        val deferred = withContext(ttsDispatcher) {
            getOrStartInitializationInternal()
        }
        return deferred.await()
    }

    private fun getOrStartInitializationInternal(): CompletableDeferred<Boolean> {
        if (isInitialized && ttsEngine != null) {
            return CompletableDeferred(true)
        }

        val ongoing = initDeferred
        if (ongoing != null && !ongoing.isCompleted) {
            return ongoing
        }

        val deferred = CompletableDeferred<Boolean>()
        initDeferred = deferred
        val token = ++engineGeneration

        if (!engineFactory.isGoogleTtsInstalled()) {
            initDeferred = null
            deferred.complete(false)
            _events.tryEmit(TtsEvent.GoogleTtsMissing)
            return deferred
        }

        try {
            val engine = engineFactory.create(GOOGLE_TTS_PACKAGE) { status ->
                // onInit 由系统在主线程持锁回调，必须立即切入后台单线程处理
                scope.launch {
                    handleOnInitInternal(token, status)
                }
            }
            ttsEngine = engine
            startInitTimeoutWatchdog(token, deferred)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to create TTS engine", e)
            cleanupEngineInternal(resetCache = true)
            initDeferred = null
            deferred.complete(false)
            _events.tryEmit(TtsEvent.OnError("init_failed", e.message))
        }

        return deferred
    }

    private fun startInitTimeoutWatchdog(token: Long, deferred: CompletableDeferred<Boolean>) {
        scope.launch {
            delay(initTimeoutMs)
            if (token == engineGeneration && !deferred.isCompleted) {
                logger.e(TAG, "TTS initialization timed out after ${initTimeoutMs}ms")
                cleanupEngineInternal(resetCache = true)
                initDeferred = null
                deferred.complete(false)
                _events.tryEmit(TtsEvent.OnError("init_timeout", "TTS initialization timed out"))
            }
        }
    }

    private suspend fun handleOnInitInternal(token: Long, status: Int) {
        if (token != engineGeneration) return
        val engine = ttsEngine ?: return
        val deferred = initDeferred

        if (status != TextToSpeech.SUCCESS) {
            logger.e(TAG, "TTS initialization failed with status: $status")
            val isGoogleInstalled = engineFactory.isGoogleTtsInstalled()
            cleanupEngineInternal(resetCache = true)
            initDeferred = null
            if (!isGoogleInstalled) {
                _events.tryEmit(TtsEvent.GoogleTtsMissing)
            } else {
                _events.tryEmit(TtsEvent.OnError("init_failed", "Initialization failed"))
            }
            deferred?.complete(false)
            return
        }

        setupUtteranceListener(engine, token)
        refreshVoicesCacheInternal(engine)

        val savedJpVoice = readPreferenceSafely { settingsRepository.ttsVoiceNameFlow.first() }
            ?: savedJapaneseVoiceName
        val savedCnVoice = readPreferenceSafely { settingsRepository.ttsChineseVoiceNameFlow.first() }
            ?: savedChineseVoiceName
        val savedRate = readPreferenceSafely { settingsRepository.ttsSpeechRateFlow.first() }
            ?: currentSpeechRate
        val savedPitch = readPreferenceSafely { settingsRepository.ttsPitchFlow.first() }
            ?: currentPitch

        if (token != engineGeneration || ttsEngine !== engine) {
            deferred?.complete(false)
            return
        }

        savedJapaneseVoiceName = savedJpVoice
        savedChineseVoiceName = savedCnVoice
        currentSpeechRate = savedRate
        currentPitch = savedPitch

        val configured = configureInitialJapaneseVoice(engine, savedJpVoice)
        if (!configured) {
            logger.e(TAG, "Japanese language is not supported or missing data")
            cleanupEngineInternal(resetCache = true)
            initDeferred = null
            _events.tryEmit(TtsEvent.OnError("lang_not_supported", "Japanese not supported"))
            deferred?.complete(false)
            return
        }

        engine.setSpeechRate(currentSpeechRate)
        appliedSpeechRate = currentSpeechRate
        engine.setPitch(currentPitch)
        appliedPitch = currentPitch

        isInitialized = true
        initDeferred = null
        deferred?.complete(true)
    }

    private suspend fun <T> readPreferenceSafely(block: suspend () -> T): T? {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e(TAG, "Error loading TTS preference", e)
            null
        }
    }

    private fun configureInitialJapaneseVoice(engine: TtsEngine, savedJpVoice: String?): Boolean {
        if (!savedJpVoice.isNullOrBlank() && cachedVoiceMap.containsKey(savedJpVoice)) {
            val result = engine.setVoice(savedJpVoice)
            if (result == TextToSpeech.SUCCESS) {
                activeVoiceName = savedJpVoice
                activeLanguage = cachedVoiceMap[savedJpVoice]?.locale ?: Locale.JAPAN
                return true
            }
        }

        val langResult = engine.setLanguage(Locale.JAPAN)
        if (langResult == TextToSpeech.LANG_MISSING_DATA ||
            langResult == TextToSpeech.LANG_NOT_SUPPORTED ||
            langResult == TextToSpeech.ERROR
        ) {
            return false
        }

        activeVoiceName = null
        activeLanguage = Locale.JAPAN
        return true
    }

    private fun setupUtteranceListener(engine: TtsEngine, token: Long) {
        engine.setProgressListener(object : TtsEngineListener {
            override fun onStart(utteranceId: String) {
                _events.tryEmit(TtsEvent.OnStart(utteranceId))
            }

            override fun onDone(utteranceId: String) {
                _events.tryEmit(TtsEvent.OnDone(utteranceId))
                scope.launch {
                    if (token != engineGeneration) return@launch
                    activeUtteranceIds.remove(utteranceId)
                    if (activeUtteranceIds.isEmpty()) {
                        abandonAudioFocusInternal()
                    }
                }
            }

            override fun onStop(utteranceId: String, interrupted: Boolean) {
                _events.tryEmit(TtsEvent.OnStop(utteranceId, interrupted))
                scope.launch {
                    if (token != engineGeneration) return@launch
                    activeUtteranceIds.remove(utteranceId)
                    if (activeUtteranceIds.isEmpty()) {
                        abandonAudioFocusInternal()
                    }
                }
            }

            override fun onError(utteranceId: String, errorCode: Int?) {
                val message = errorCode?.let { "Error code: $it" }
                _events.tryEmit(TtsEvent.OnError(utteranceId, message))
                scope.launch {
                    if (token != engineGeneration) return@launch
                    activeUtteranceIds.remove(utteranceId)
                    if (activeUtteranceIds.isEmpty()) {
                        abandonAudioFocusInternal()
                    }
                    if (errorCode == TextToSpeech.ERROR_SERVICE || errorCode == TextToSpeech.ERROR) {
                        handleTtsErrorInternal(failedGeneration = token)
                    }
                }
            }
        })
    }

    fun speak(
        text: String,
        language: Locale = Locale.JAPAN,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        id: String? = null,
        overrideVoiceName: String? = null,
        overrideSpeechRate: Float? = null
    ) {
        scope.launch {
            speakInternal(
                text = text,
                language = language,
                queueMode = queueMode,
                id = id,
                overrideVoiceName = overrideVoiceName,
                overrideSpeechRate = overrideSpeechRate
            )
        }
    }

    internal suspend fun speakSuspend(
        text: String,
        language: Locale = Locale.JAPAN,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        id: String? = null,
        overrideVoiceName: String? = null,
        overrideSpeechRate: Float? = null,
        shouldProceed: () -> Boolean = { true }
    ) {
        withContext(ttsDispatcher) {
            if (!shouldProceed()) return@withContext
            speakInternal(
                text = text,
                language = language,
                queueMode = queueMode,
                id = id,
                overrideVoiceName = overrideVoiceName,
                overrideSpeechRate = overrideSpeechRate,
                shouldProceed = shouldProceed
            )
        }
    }

    fun speakExample(japanese: String, chinese: String, id: String? = null) {
        scope.launch {
            speakExampleInternal(japanese = japanese, chinese = chinese, id = id)
        }
    }

    internal suspend fun speakExampleSuspend(
        japanese: String,
        chinese: String,
        id: String? = null,
        shouldProceed: () -> Boolean = { true }
    ) {
        withContext(ttsDispatcher) {
            if (!shouldProceed()) return@withContext
            speakExampleInternal(
                japanese = japanese,
                chinese = chinese,
                id = id,
                shouldProceed = shouldProceed
            )
        }
    }

    private fun speakExampleInternal(
        japanese: String,
        chinese: String,
        id: String?,
        shouldProceed: () -> Boolean = { true }
    ) {
        if (!shouldProceed()) return
        val utteranceId = id ?: "example_${System.currentTimeMillis()}"
        var queuedJapanese = false
        if (japanese.isNotBlank()) {
            queuedJapanese = speakInternal(
                text = japanese,
                language = Locale.JAPAN,
                queueMode = TextToSpeech.QUEUE_FLUSH,
                id = "$utteranceId-jp",
                overrideVoiceName = null,
                overrideSpeechRate = null,
                shouldProceed = shouldProceed
            )
            if (!queuedJapanese) return
        }
        if (chinese.isNotBlank() && shouldProceed()) {
            val mode = if (queuedJapanese) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
            speakInternal(
                text = chinese,
                language = Locale.CHINA,
                queueMode = mode,
                id = "$utteranceId-cn",
                overrideVoiceName = null,
                overrideSpeechRate = null,
                shouldProceed = shouldProceed
            )
        }
    }

    private fun speakInternal(
        text: String,
        language: Locale,
        queueMode: Int,
        id: String?,
        overrideVoiceName: String?,
        overrideSpeechRate: Float?,
        allowErrorRecovery: Boolean = true,
        shouldProceed: () -> Boolean = { true }
    ): Boolean {
        if (!shouldProceed()) return false

        val engine = ttsEngine
        if (!isInitialized || engine == null) {
            logger.w(TAG, "TTS not initialized yet")
            return false
        }

        val targetLanguage = if (language == Locale.JAPANESE) Locale.JAPAN else language
        val isChinese = isChineseLocale(targetLanguage)
        val desiredVoiceName = overrideVoiceName?.takeIf { it.isNotBlank() }
            ?: if (isChinese) savedChineseVoiceName else savedJapaneseVoiceName

        if (!applyVoiceOrLanguageForSpeak(
                engine = engine,
                targetLanguage = targetLanguage,
                desiredVoiceName = desiredVoiceName,
                text = text,
                language = language,
                queueMode = queueMode,
                id = id,
                overrideVoiceName = overrideVoiceName,
                overrideSpeechRate = overrideSpeechRate,
                allowErrorRecovery = allowErrorRecovery,
                shouldProceed = shouldProceed
            )
        ) {
            return false
        }

        val effectiveRate = overrideSpeechRate ?: currentSpeechRate
        if (appliedSpeechRate != effectiveRate) {
            engine.setSpeechRate(effectiveRate)
            appliedSpeechRate = effectiveRate
        }
        if (appliedPitch != currentPitch) {
            engine.setPitch(currentPitch)
            appliedPitch = currentPitch
        }

        val textToSpeak = cleanText(text)
        val utteranceId = id ?: System.currentTimeMillis().toString()
        if (textToSpeak.isBlank()) {
            _events.tryEmit(TtsEvent.OnDone(utteranceId))
            return false
        }

        // 已持有音频焦点时直接复用，避免 QUEUE_FLUSH 重复申请触发 AUDIOFOCUS_LOSS 误停新朗读
        val focusAcquired = if (hasAudioFocus) {
            true
        } else {
            requestAudioFocusInternal()
        }

        if (!focusAcquired) {
            logger.w(TAG, "Failed to get audio focus, skipping speak")
            _events.tryEmit(TtsEvent.OnError(utteranceId, "Focus denied"))
            return false
        }

        if (queueMode == TextToSpeech.QUEUE_FLUSH) {
            activeUtteranceIds.clear()
        }
        activeUtteranceIds.add(utteranceId)

        val currentGeneration = engineGeneration
        val result = engine.speak(textToSpeak, queueMode, utteranceId)
        if (result == TextToSpeech.ERROR) {
            activeUtteranceIds.remove(utteranceId)
            if (activeUtteranceIds.isEmpty()) {
                abandonAudioFocusInternal()
            }
            logger.e(TAG, "speak returned ERROR. TTS engine might be dead.")
            if (allowErrorRecovery) {
                handleTtsErrorInternal(
                    failedGeneration = currentGeneration,
                    retryRequest = PendingSpeakRequest(
                        text = text,
                        language = language,
                        queueMode = queueMode,
                        id = utteranceId,
                        overrideVoiceName = overrideVoiceName,
                        overrideSpeechRate = overrideSpeechRate,
                        shouldProceed = shouldProceed
                    )
                )
            } else {
                _events.tryEmit(TtsEvent.OnError(utteranceId, "TTS speak failed"))
            }
            return false
        }
        return true
    }

    private fun applyVoiceOrLanguageForSpeak(
        engine: TtsEngine,
        targetLanguage: Locale,
        desiredVoiceName: String?,
        text: String,
        language: Locale,
        queueMode: Int,
        id: String?,
        overrideVoiceName: String?,
        overrideSpeechRate: Float?,
        allowErrorRecovery: Boolean,
        shouldProceed: () -> Boolean
    ): Boolean {
        if (!desiredVoiceName.isNullOrBlank()) {
            ensureVoicesCachedInternal()
            if (cachedVoiceMap.containsKey(desiredVoiceName)) {
                if (activeVoiceName == desiredVoiceName) {
                    return true
                }
                val result = engine.setVoice(desiredVoiceName)
                when (result) {
                    TextToSpeech.SUCCESS -> {
                        activeVoiceName = desiredVoiceName
                        activeLanguage = cachedVoiceMap[desiredVoiceName]?.locale ?: targetLanguage
                        return true
                    }
                    TextToSpeech.ERROR -> {
                        logger.e(TAG, "setVoice returned ERROR. TTS engine might be dead.")
                        val currentGeneration = engineGeneration
                        if (allowErrorRecovery) {
                            handleTtsErrorInternal(
                                failedGeneration = currentGeneration,
                                retryRequest = PendingSpeakRequest(
                                    text = text,
                                    language = language,
                                    queueMode = queueMode,
                                    id = id,
                                    overrideVoiceName = overrideVoiceName,
                                    overrideSpeechRate = overrideSpeechRate,
                                    shouldProceed = shouldProceed
                                )
                            )
                        } else {
                            _events.tryEmit(TtsEvent.OnError(id ?: "set_voice_error", "TTS setVoice failed"))
                        }
                        return false
                    }
                }
            }
        }

        val needLanguageSwitch = activeVoiceName != null ||
            activeLanguage == null ||
            activeLanguage?.language != targetLanguage.language

        if (!needLanguageSwitch) {
            return true
        }

        val langResult = engine.setLanguage(targetLanguage)
        return when (langResult) {
            TextToSpeech.LANG_MISSING_DATA,
            TextToSpeech.LANG_NOT_SUPPORTED -> {
                logger.e(TAG, "Language $targetLanguage is not supported or missing data")
                _events.tryEmit(TtsEvent.OnError(id ?: "lang_not_supported", "Language not supported"))
                false
            }
            TextToSpeech.ERROR -> {
                logger.e(TAG, "setLanguage returned ERROR. TTS engine might be dead.")
                val currentGeneration = engineGeneration
                if (allowErrorRecovery) {
                    handleTtsErrorInternal(
                        failedGeneration = currentGeneration,
                        retryRequest = PendingSpeakRequest(
                            text = text,
                            language = language,
                            queueMode = queueMode,
                            id = id,
                            overrideVoiceName = overrideVoiceName,
                            overrideSpeechRate = overrideSpeechRate,
                            shouldProceed = shouldProceed
                        )
                    )
                } else {
                    _events.tryEmit(TtsEvent.OnError(id ?: "set_language_error", "TTS setLanguage failed"))
                }
                false
            }
            else -> {
                activeVoiceName = null
                activeLanguage = targetLanguage
                true
            }
        }
    }

    private fun handleTtsErrorInternal(
        failedGeneration: Long,
        retryRequest: PendingSpeakRequest? = null
    ) {
        if (failedGeneration != engineGeneration) return
        logger.e(TAG, "TTS error detected. Reinitializing engine...")
        // 同步清理死引擎并启动重建，防止后续排队任务误用失效实例或触发重复重建风暴
        val deferred = if (initDeferred?.isCompleted == false) {
            initDeferred!!
        } else {
            abandonAudioFocusInternal()
            cleanupEngineInternal(resetCache = true)
            getOrStartInitializationInternal()
        }

        scope.launch {
            val ready = try {
                deferred.await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(TAG, "Failed to reinitialize TTS", e)
                false
            }

            if (retryRequest != null && retryRequest.shouldProceed()) {
                if (ready) {
                    speakInternal(
                        text = retryRequest.text,
                        language = retryRequest.language,
                        queueMode = retryRequest.queueMode,
                        id = retryRequest.id,
                        overrideVoiceName = retryRequest.overrideVoiceName,
                        overrideSpeechRate = retryRequest.overrideSpeechRate,
                        allowErrorRecovery = false,
                        shouldProceed = retryRequest.shouldProceed
                    )
                } else {
                    _events.tryEmit(
                        TtsEvent.OnError(
                            retryRequest.id ?: "reinit_failed",
                            "TTS reinitialization failed"
                        )
                    )
                }
            }
        }
    }

    suspend fun getVoices(): List<TtsVoice> {
        if (!initialize()) return emptyList()
        return withContext(ttsDispatcher) {
            ensureVoicesCachedInternal()
            cachedJapaneseVoices
        }
    }

    suspend fun getChineseVoices(): List<TtsVoice> {
        if (!initialize()) return emptyList()
        return withContext(ttsDispatcher) {
            ensureVoicesCachedInternal()
            cachedChineseVoices
        }
    }

    fun setVoice(voiceName: String) {
        scope.launch {
            setVoiceInternal(voiceName)
        }
    }

    private fun setVoiceInternal(voiceName: String) {
        val engine = ttsEngine ?: return
        if (!isInitialized) return
        ensureVoicesCachedInternal()
        if (!cachedVoiceMap.containsKey(voiceName)) {
            logger.w(TAG, "Voice not found: $voiceName")
            return
        }
        if (activeVoiceName == voiceName) return
        val result = engine.setVoice(voiceName)
        if (result == TextToSpeech.SUCCESS) {
            activeVoiceName = voiceName
            activeLanguage = cachedVoiceMap[voiceName]?.locale
            logger.d(TAG, "Voice set to: $voiceName")
        } else if (result == TextToSpeech.ERROR) {
            logger.w(TAG, "setVoice returned ERROR, engine might be dead")
            handleTtsErrorInternal(failedGeneration = engineGeneration)
        }
    }

    fun stop() {
        scope.launch {
            stopInternal()
        }
    }

    private fun stopInternal() {
        activeUtteranceIds.clear()
        val engine = ttsEngine
        if (isInitialized && engine != null) {
            val result = engine.stop()
            if (result == TextToSpeech.ERROR) {
                logger.w(TAG, "stop returned ERROR, engine might be dead")
                handleTtsErrorInternal(failedGeneration = engineGeneration)
            }
        }
        abandonAudioFocusInternal()
    }

    fun setSpeechRate(rate: Float) {
        scope.launch {
            currentSpeechRate = rate
            if (isInitialized && appliedSpeechRate != rate) {
                ttsEngine?.setSpeechRate(rate)
                appliedSpeechRate = rate
            }
        }
    }

    fun shutdown() {
        scope.launch {
            shutdownInternal()
        }
    }

    private fun shutdownInternal() {
        val pendingInit = initDeferred
        initDeferred = null
        activeUtteranceIds.clear()
        val engine = ttsEngine
        if (isInitialized && engine != null) {
            try {
                engine.stop()
            } catch (e: Exception) {
                logger.e(TAG, "Error stopping TTS during shutdown", e)
            }
        }
        abandonAudioFocusInternal()
        cleanupEngineInternal(resetCache = true)
        pendingInit?.complete(false)
    }

    private fun cleanupEngineInternal(resetCache: Boolean) {
        engineGeneration++
        isInitialized = false
        activeUtteranceIds.clear()
        val oldEngine = ttsEngine
        ttsEngine = null
        if (oldEngine != null) {
            try {
                oldEngine.shutdown()
            } catch (e: Exception) {
                logger.e(TAG, "Error shutting down old TTS", e)
            }
        }
        activeVoiceName = null
        activeLanguage = null
        appliedSpeechRate = null
        appliedPitch = null
        if (resetCache) {
            cachedVoiceMap = emptyMap()
            cachedJapaneseVoices = emptyList()
            cachedChineseVoices = emptyList()
        }
    }

    private fun ensureVoicesCachedInternal() {
        val engine = ttsEngine ?: return
        if (cachedVoiceMap.isEmpty()) {
            refreshVoicesCacheInternal(engine)
        }
    }

    private fun refreshVoicesCacheInternal(engine: TtsEngine) {
        val descriptors = engine.getVoices()
        if (descriptors.isEmpty()) {
            cachedVoiceMap = emptyMap()
            cachedJapaneseVoices = emptyList()
            cachedChineseVoices = emptyList()
            return
        }

        val map = LinkedHashMap<String, TtsVoiceDescriptor>(descriptors.size)
        val japanese = mutableListOf<TtsVoice>()
        val chinese = mutableListOf<TtsVoice>()

        for (descriptor in descriptors) {
            map[descriptor.name] = descriptor
            if (descriptor.locale.language == Locale.JAPAN.language) {
                japanese.add(descriptor.toDomainVoice())
            } else if (isChineseLocale(descriptor.locale)) {
                chinese.add(descriptor.toDomainVoice())
            }
        }

        cachedVoiceMap = map
        cachedJapaneseVoices = japanese
        cachedChineseVoices = chinese
    }

    private fun TtsVoiceDescriptor.toDomainVoice(): TtsVoice {
        val gender = when {
            features != null && features.contains("latency_very_low") -> "fast"
            name.contains("female", ignoreCase = true) -> "female"
            name.contains("male", ignoreCase = true) -> "male"
            else -> "unknown"
        }

        val qualityLabel = when (quality) {
            Voice.QUALITY_VERY_HIGH -> "very_high"
            Voice.QUALITY_HIGH -> "high"
            else -> "normal"
        }

        return TtsVoice(
            name = name,
            locale = locale.toLanguageTag(),
            isNetworkConnectionRequired = isNetworkConnectionRequired,
            quality = qualityLabel,
            gender = gender
        )
    }

    private fun requestAudioFocusInternal(): Boolean {
        val token = ++focusGeneration
        val granted = audioFocusController.requestFocus {
            scope.launch {
                if (token == focusGeneration && hasAudioFocus) {
                    stopInternal()
                }
            }
        }
        hasAudioFocus = granted
        if (!granted) {
            focusGeneration++
        }
        return granted
    }

    private fun abandonAudioFocusInternal() {
        if (!hasAudioFocus) return
        hasAudioFocus = false
        focusGeneration++
        audioFocusController.abandonFocus()
    }

    private fun isChineseLocale(locale: Locale): Boolean {
        val lang = locale.language
        return lang == Locale.CHINESE.language || lang == Locale.CHINA.language || lang == "zh"
    }

    private fun isJapaneseOrUnset(locale: Locale?): Boolean {
        return locale == null || locale.language == Locale.JAPAN.language
    }

    private fun cleanText(text: String): String {
        val withoutBrackets = text.replace(Regex("\\(.*?\\)|（.*?）|\\[.*?\\]"), "")
        return withoutBrackets.replace("～", "").replace("~", "").trim()
    }

    internal fun closeForTest() {
        scope.cancel()
    }

    private data class PendingSpeakRequest(
        val text: String,
        val language: Locale,
        val queueMode: Int,
        val id: String?,
        val overrideVoiceName: String?,
        val overrideSpeechRate: Float?,
        val shouldProceed: () -> Boolean
    )

    internal companion object {
        private const val TAG = "TtsManager"
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        const val DEFAULT_INIT_TIMEOUT_MS = 10_000L

        internal fun createForTest(
            settingsRepository: SettingsRepository,
            ioDispatcher: CoroutineDispatcher,
            engineFactory: TtsEngineFactory,
            audioFocusController: TtsAudioFocusController,
            logger: TtsLogger = NoOpTtsLogger,
            initTimeoutMs: Long = DEFAULT_INIT_TIMEOUT_MS
        ): TtsManager = TtsManager(
            settingsRepository = settingsRepository,
            ioDispatcher = ioDispatcher,
            engineFactory = engineFactory,
            audioFocusController = audioFocusController,
            logger = logger,
            initTimeoutMs = initTimeoutMs
        )

        private object NoOpTtsLogger : TtsLogger {
            override fun d(tag: String, message: String) = Unit
            override fun w(tag: String, message: String) = Unit
            override fun e(tag: String, message: String, throwable: Throwable?) = Unit
        }
    }
}
