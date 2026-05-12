package com.jian.nemo.core.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.jian.nemo.core.domain.repository.TtsEvent
import com.jian.nemo.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : TextToSpeech.OnInitListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 失去焦点（如来电、抢占），立即停止播放
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 暂时失去焦点但允许降低音量，在 TTS 场景通常也建议直接停止或配合降低音量
                // 对于短朗读，停止是更稳妥的做法
                stop()
            }
        }
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val initMutex = Mutex()
    private var isInitializing = false
    private var initDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentSpeechRate: Float = 1.0f
    private var currentPitch: Float = 1.0f

    // TTS 状态事件流
    private val _events = MutableSharedFlow<TtsEvent>(replay = 1, extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        observeSettings()
    }

    suspend fun initialize() {
        initMutex.withLock {
            if (isInitialized) return
            
            if (isInitializing) {
                // 如果正在初始化，等待结果即可
                try {
                    initDeferred.await()
                } catch (e: Exception) {
                    // 如果等待失败，允许重试
                }
                return
            }

            isInitializing = true
            initDeferred = kotlinx.coroutines.CompletableDeferred()

            // Enforce Google TTS engine
            if (!isGoogleTtsInstalled()) {
                 isInitializing = false
                 initDeferred.completeExceptionally(IllegalStateException("Google TTS not installed"))
                 _events.emit(TtsEvent.GoogleTtsMissing)
                 return
            }

            try {
                // Force Google TTS engine: "com.google.android.tts"
                tts = TextToSpeech(context, this, "com.google.android.tts")
                // 等待 onInit 回调
                initDeferred.await()
            } catch (e: Exception) {
                Log.e("TtsManager", "Failed to initialize TTS", e)
                isInitializing = false
                isInitialized = false
                initDeferred.completeExceptionally(e)
                _events.emit(TtsEvent.OnError("init_failed", e.message))
            }
        }
    }

    private fun isGoogleTtsInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.tts", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun observeSettings() {
        scope.launch {
            settingsRepository.ttsSpeechRateFlow.collect { rate ->
                currentSpeechRate = rate
                tts?.setSpeechRate(rate)
            }
        }

        scope.launch {
            settingsRepository.ttsPitchFlow.collect { pitch ->
                currentPitch = pitch
                tts?.setPitch(pitch)
            }
        }

        scope.launch {
            settingsRepository.ttsVoiceNameFlow.collect { voiceName ->
                if (voiceName != null) {
                    setVoice(voiceName)
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.JAPAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsManager", "Japanese language is not supported or missing data")
                isInitialized = false
                isInitializing = false
                initDeferred.completeExceptionally(IllegalStateException("Japanese not supported"))
                _events.tryEmit(TtsEvent.OnError("lang_not_supported", "Japanese not supported"))
            } else {
                isInitialized = true
                isInitializing = false

                // Initialize with saved voice if available
                scope.launch {
                    try {
                        val savedVoiceName = settingsRepository.ttsVoiceNameFlow.first()
                        if (!savedVoiceName.isNullOrBlank()) {
                            setVoice(savedVoiceName)
                        }
                    } catch (e: Exception) {
                        Log.e("TtsManager", "Error loading saved voice", e)
                    }

                    // Apply current settings
                    tts?.setSpeechRate(currentSpeechRate)
                    tts?.setPitch(currentPitch)
                    
                    // 全部准备就绪，释放等待
                    initDeferred.complete(Unit)
                }

                setupUtteranceListener()
            }
        } else {
            Log.e("TtsManager", "Initialization failed")
            isInitializing = false
            isInitialized = false
            initDeferred.completeExceptionally(IllegalStateException("TTS onInit failed"))
            if (!isGoogleTtsInstalled()) {
                _events.tryEmit(TtsEvent.GoogleTtsMissing)
            } else {
                 _events.tryEmit(TtsEvent.OnError("init_failed", "Initialization failed"))
            }
        }
    }

    // ... setupUtteranceListener implementation ...
    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                utteranceId?.let { _events.tryEmit(TtsEvent.OnStart(it)) }
            }

            override fun onDone(utteranceId: String?) {
                utteranceId?.let { _events.tryEmit(TtsEvent.OnDone(it)) }
                abandonAudioFocus()
            }

            @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
            override fun onError(utteranceId: String?) {
                utteranceId?.let { _events.tryEmit(TtsEvent.OnError(it)) }
                abandonAudioFocus()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                utteranceId?.let { _events.tryEmit(TtsEvent.OnError(it, "Error code: $errorCode")) }
                abandonAudioFocus()
            }
        })
    }

    fun speak(text: String, language: Locale = Locale.JAPAN, queueMode: Int = TextToSpeech.QUEUE_FLUSH, id: String? = null) {
        if (isInitialized) {
            // Logic: if we have a specific voice set, we should try to use it.
            // setLanguage() might reset the voice to default for that language.

            // Checks if we need to call setLanguage
            // If the current voice's locale matches the requested language, we might skip setLanguage
            // OR we ensure we re-set the voice after setLanguage if needed (though setLanguage generally resets it)

            // Safer strategy:
            // 1. Try to find if the current active voice matches the request.
            // 2. If not, call setLanguage.

            var moveProceed = false

            val currentVoice = tts?.voice
            val targetLanguage = if (language == Locale.JAPANESE) Locale.JAPAN else language

            if (currentVoice != null && currentVoice.locale.language == targetLanguage.language) {
                 // Current voice matches the requested language usage
                 // Skip setLanguage to avoid resetting custom voice
                 moveProceed = true
            } else {
                 val result = tts?.setLanguage(targetLanguage)
                 if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TtsManager", "Language $targetLanguage is not supported or missing data")
                    moveProceed = false
                 } else {
                    moveProceed = true
                    // Potentially we just reset the voice to default.
                    // If we have a stored preference, we might want to re-apply it here,
                    // but usually speak is called frequently, so maybe just rely on setLanguage being correct for now unless specific voice was forced.

                    // Re-apply voice if we have one in memory that matches this language?
                    // Ideally we should track `selectedVoiceName` in memory.
                    // For now, let's just proceed.
                 }
            }

             if (moveProceed) {
                // 清理文本 (仅对日语进行注音过滤，中文通常不需要或格式不同)
                // 这里简单起见对所有文本过滤，或者判断 Locale
                val textToSpeak = if (targetLanguage.language == Locale.JAPAN.language) {
                    cleanText(text)
                } else {
                    text
                }

                // 使用指定的队列模式
                val utteranceId = id ?: System.currentTimeMillis().toString()

                // Ensure settings are applied (sometimes engine resets on language change)
                tts?.setSpeechRate(currentSpeechRate)
                tts?.setPitch(currentPitch)

                // 请求音频焦点
                if (requestAudioFocus()) {
                    tts?.speak(textToSpeak, queueMode, null, utteranceId)
                } else {
                    Log.w("TtsManager", "Failed to get audio focus, skipping speak")
                    _events.tryEmit(TtsEvent.OnError(utteranceId, "Focus denied"))
                }
            }
        } else {
            Log.w("TtsManager", "TTS not initialized yet")
        }
    }

    /**
     * 朗读例句（日语 + 中文翻译）
     */
    fun speakExample(japanese: String, chinese: String, id: String? = null) {
        val utteranceId = id ?: "example_${System.currentTimeMillis()}"
        // 1. 朗读日语 (使用 FLUSH 模式中断之前的朗读)
        speak(japanese, Locale.JAPAN, TextToSpeech.QUEUE_FLUSH, "$utteranceId-jp")
        // 2. 朗读中文 (使用 QUEUE_ADD 模式追加到队列)
        speak(chinese, Locale.CHINA, TextToSpeech.QUEUE_ADD, "$utteranceId-cn")
    }

    /**
     * 获取可用语音列表 (当前过滤日语 "ja")
     */
    fun getVoices(): List<com.jian.nemo.core.domain.model.TtsVoice> {
        val voices = try {
            tts?.voices
        } catch (e: Exception) {
            return emptyList()
        }

        if (voices.isNullOrEmpty()) return emptyList()

        return voices.filter {
            // 过滤日语且未被标记为网络连接需要 (可选: 允许网络语音)
            it.locale.language == Locale.JAPAN.language
        }.map { voice ->
            // Determine gender if possible (Android Voice API doesn't strictly expose gender enum easily in older APIs via features)
            // But we can try to guess from features or name
            // Determine gender if possible
            val features = voice.features
            val gender = when {
                features != null && features.contains("latency_very_low") -> "fast"
                voice.name.contains("female", ignoreCase = true) -> "female"
                voice.name.contains("male", ignoreCase = true) -> "male"
                else -> "unknown"
            }

            val quality = when {
                 voice.quality == android.speech.tts.Voice.QUALITY_VERY_HIGH -> "very_high"
                 voice.quality == android.speech.tts.Voice.QUALITY_HIGH -> "high"
                 else -> "normal"
            }

            com.jian.nemo.core.domain.model.TtsVoice(
                name = voice.name,
                locale = voice.locale.toLanguageTag(),
                isNetworkConnectionRequired = voice.isNetworkConnectionRequired,
                quality = quality,
                gender = gender
            )
        }
    }

    /**
     * 设置指定名称的语音
     */
    fun setVoice(voiceName: String) {
        val targetVoice = tts?.voices?.find { it.name == voiceName }
        if (targetVoice != null) {
            tts?.voice = targetVoice
            Log.d("TtsManager", "Voice set to: $voiceName")
        } else {
            Log.w("TtsManager", "Voice not found: $voiceName")
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
            abandonAudioFocus()
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            audioManager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
    }

    /**
     * 清理文本，移除注音符号
     * 支持格式：
     * - 汉字(kana) -> 汉字
     * - 汉字（kana）-> 汉字
     * - 汉字[kana] -> 汉字
     */
    private fun cleanText(text: String): String {
        return text.replace(Regex("\\(.*?\\)|（.*?）|\\[.*?\\]"), "")
    }
}
