package com.jian.nemo.core.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale

internal data class TtsVoiceDescriptor(
    val name: String,
    val locale: Locale,
    val isNetworkConnectionRequired: Boolean,
    val quality: Int,
    val features: Set<String>?
)

internal interface TtsEngineListener {
    fun onStart(utteranceId: String)
    fun onDone(utteranceId: String)
    fun onStop(utteranceId: String, interrupted: Boolean)
    fun onError(utteranceId: String, errorCode: Int?)
}

internal interface TtsEngine {
    fun setLanguage(locale: Locale): Int
    fun getVoices(): List<TtsVoiceDescriptor>
    fun setVoice(voiceName: String): Int
    fun setSpeechRate(rate: Float): Int
    fun setPitch(pitch: Float): Int
    fun speak(text: String, queueMode: Int, utteranceId: String): Int
    fun stop(): Int
    fun shutdown()
    fun setProgressListener(listener: TtsEngineListener)

    companion object {
        const val VOICE_NOT_FOUND = -100
    }
}

internal interface TtsEngineFactory {
    fun isGoogleTtsInstalled(): Boolean
    fun create(enginePackage: String, onInit: (status: Int) -> Unit): TtsEngine
}

internal interface TtsAudioFocusController {
    fun requestFocus(onFocusLoss: () -> Unit): Boolean
    fun abandonFocus()
}

internal interface TtsLogger {
    fun d(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

internal object AndroidTtsLogger : TtsLogger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}

internal class AndroidTtsEngineFactory(
    private val context: Context
) : TtsEngineFactory {

    override fun isGoogleTtsInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.tts", 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun create(enginePackage: String, onInit: (status: Int) -> Unit): TtsEngine {
        val tts = TextToSpeech(context, { status -> onInit(status) }, enginePackage)
        return AndroidTtsEngine(tts)
    }
}

private class AndroidTtsEngine(
    private val tts: TextToSpeech
) : TtsEngine {

    private var cachedNativeVoices: Map<String, Voice> = emptyMap()

    override fun setLanguage(locale: Locale): Int = tts.setLanguage(locale)

    override fun getVoices(): List<TtsVoiceDescriptor> {
        val voices = try {
            tts.voices
        } catch (_: Exception) {
            return emptyList()
        } ?: return emptyList()

        val nativeMap = LinkedHashMap<String, Voice>(voices.size)
        val descriptors = ArrayList<TtsVoiceDescriptor>(voices.size)
        for (voice in voices) {
            nativeMap[voice.name] = voice
            descriptors.add(
                TtsVoiceDescriptor(
                    name = voice.name,
                    locale = voice.locale,
                    isNetworkConnectionRequired = voice.isNetworkConnectionRequired,
                    quality = voice.quality,
                    features = voice.features
                )
            )
        }
        cachedNativeVoices = nativeMap
        return descriptors
    }

    override fun setVoice(voiceName: String): Int {
        val targetVoice = cachedNativeVoices[voiceName] ?: return TtsEngine.VOICE_NOT_FOUND
        return tts.setVoice(targetVoice)
    }

    override fun setSpeechRate(rate: Float): Int = tts.setSpeechRate(rate)

    override fun setPitch(pitch: Float): Int = tts.setPitch(pitch)

    override fun speak(text: String, queueMode: Int, utteranceId: String): Int {
        return tts.speak(text, queueMode, null, utteranceId)
    }

    override fun stop(): Int = tts.stop()

    override fun shutdown() {
        cachedNativeVoices = emptyMap()
        tts.shutdown()
    }

    override fun setProgressListener(listener: TtsEngineListener) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                utteranceId?.let(listener::onStart)
            }

            override fun onDone(utteranceId: String?) {
                utteranceId?.let(listener::onDone)
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                utteranceId?.let { listener.onStop(it, interrupted) }
            }

            @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId, -1)"))
            override fun onError(utteranceId: String?) {
                utteranceId?.let { listener.onError(it, null) }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                utteranceId?.let { listener.onError(it, errorCode) }
            }
        })
    }
}

internal class AndroidTtsAudioFocusController(
    private val context: Context
) : TtsAudioFocusController {

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var focusRequest: AudioFocusRequest? = null
    private var legacyListener: AudioManager.OnAudioFocusChangeListener? = null
    @Volatile
    private var activeFocusToken: Long = 0L

    override fun requestFocus(onFocusLoss: () -> Unit): Boolean {
        // 避免重复申请覆盖引用导致旧 AudioFocusRequest 泄漏并反向触发 AUDIOFOCUS_LOSS
        abandonFocus()

        val token = activeFocusToken + 1L
        activeFocusToken = token

        val changeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            if (token != activeFocusToken) return@OnAudioFocusChangeListener
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onFocusLoss()
            }
        }

        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(changeListener)
                .build()

            val result = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (result) {
                focusRequest = request
            }
            result
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                changeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (result) {
                legacyListener = changeListener
            }
            result
        }

        if (!granted) {
            activeFocusToken = token + 1L
        }
        return granted
    }

    override fun abandonFocus() {
        activeFocusToken += 1L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            legacyListener?.let {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(it)
            }
            legacyListener = null
        }
    }
}
