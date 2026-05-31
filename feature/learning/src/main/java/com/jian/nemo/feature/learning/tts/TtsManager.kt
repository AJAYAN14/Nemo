package com.jian.nemo.feature.learning.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * TTS 管理器
 * 基于 Android TextToSpeech 官方 API 实现
 *
 * 官方文档: https://developer.android.com/reference/android/speech/tts/TextToSpeech
 */
class TtsManager(context: Context) {

    companion object {
        private const val TAG = "TtsManager"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val context = context.applicationContext

    // 播放状态回调 (返回 utteranceId)
    var onSpeakStart: ((String) -> Unit)? = null
    var onSpeakDone: ((String) -> Unit)? = null

    init {
        // 初始化 TTS 引擎
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                Log.d(TAG, "TTS 初始化成功")
            } else {
                isInitialized = false
                Log.e(TAG, "TTS 初始化失败，错误码: $status")
            }
        }

        // 设置播放状态监听器
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "开始朗读: $utteranceId")
                utteranceId?.let { onSpeakStart?.invoke(it) }
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "朗读完成: $utteranceId")
                utteranceId?.let { onSpeakDone?.invoke(it) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "朗读出错: $utteranceId")
                utteranceId?.let { onSpeakDone?.invoke(it) }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "朗读出错: $utteranceId, errorCode: $errorCode")
                utteranceId?.let { onSpeakDone?.invoke(it) }
                if (errorCode == TextToSpeech.ERROR_SERVICE || errorCode == TextToSpeech.ERROR) {
                    handleTtsError()
                }
            }
        })
    }

    /**
     * 重新初始化 TTS 引擎，用于引擎断开或崩溃后的恢复
     */
    private fun handleTtsError(retryRunnable: (() -> Unit)? = null) {
        Log.e(TAG, "检测到 TTS 错误，正在尝试重新初始化...")
        isInitialized = false
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "释放旧 TTS 引擎失败", e)
        }
        
        // 重新初始化
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                Log.d(TAG, "TTS 重新初始化成功")
                // 重置监听器
                setupListener()
                // 重试刚才失败的操作
                retryRunnable?.invoke()
            } else {
                isInitialized = false
                Log.e(TAG, "TTS 重新初始化失败，错误码: $status")
            }
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "开始朗读: $utteranceId")
                utteranceId?.let { onSpeakStart?.invoke(it) }
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "朗读完成: $utteranceId")
                utteranceId?.let { onSpeakDone?.invoke(it) }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "朗读出错: $utteranceId")
                utteranceId?.let { onSpeakDone?.invoke(it) }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "朗读出错: $utteranceId, errorCode: $errorCode")
                utteranceId?.let { onSpeakDone?.invoke(it) }
                if (errorCode == TextToSpeech.ERROR_SERVICE || errorCode == TextToSpeech.ERROR) {
                    handleTtsError()
                }
            }
        })
    }

    /**
     * 检查 TTS 是否已初始化
     */
    fun isReady(): Boolean = isInitialized && tts != null

    /**
     * 朗读日语文本
     * @param text 要朗读的文本
     * @param id 唯一标识符，用于回调
     * @return 是否成功开始朗读
     */
    fun speakJapanese(text: String, id: String = "default"): Boolean {
        if (!isReady()) {
            Log.w(TAG, "TTS 未初始化")
            return false
        }

        if (text.isBlank()) {
            Log.w(TAG, "文本为空")
            return false
        }

        // 设置日语
        val result = tts?.setLanguage(Locale.JAPANESE)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "日语不支持")
            return false
        } else if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "设置语言返回 ERROR，引擎可能已失效")
            handleTtsError { speakJapanese(text, id) }
            return false
        }

        // 播放（QUEUE_FLUSH = 立即播放，打断之前的）
        // 清理文本，去除注音
        val cleanedText = cleanText(text)
        Log.d(TAG, "朗读日语: $cleanedText (ID: $id)")

        val speakResult = tts?.speak(
            cleanedText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            id
        )

        if (speakResult == TextToSpeech.ERROR) {
            Log.e(TAG, "speak 返回 ERROR，引擎可能已失效")
            handleTtsError { speakJapanese(text, id) }
        }

        return speakResult == TextToSpeech.SUCCESS
    }

    /**
     * 朗读中文文本
     * @param text 要朗读的文本
     * @param queueMode 队列模式
     * @param id 唯一标识符
     * @return 是否成功开始朗读
     */
    fun speakChinese(text: String, queueMode: Int = TextToSpeech.QUEUE_ADD, id: String = "default"): Boolean {
        if (!isReady()) {
            Log.w(TAG, "TTS 未初始化")
            return false
        }

        if (text.isBlank()) {
            Log.w(TAG, "文本为空")
            return false
        }

        // 设置中文
        val result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "中文不支持")
            return false
        } else if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "设置语言返回 ERROR，引擎可能已失效")
            handleTtsError { speakChinese(text, queueMode, id) }
            return false
        }

        // 播放（QUEUE_ADD = 追加到队列）
        val speakResult = tts?.speak(
            text,
            queueMode,
            null,
            id
        )

        if (speakResult == TextToSpeech.ERROR) {
            Log.e(TAG, "speak 返回 ERROR，引擎可能已失效")
            handleTtsError { speakChinese(text, queueMode, id) }
        }

        return speakResult == TextToSpeech.SUCCESS
    }

    /**
     * 朗读例句（日语 + 中文翻译）
     * @param japanese 日语例句
     * @param chinese 中文翻译
     * @param id 唯一标识符
     * @return 是否成功开始朗读
     */
    fun speakExample(japanese: String, chinese: String, id: String = "example"): Boolean {
        if (!isReady()) {
            Log.w(TAG, "TTS 未初始化")
            return false
        }

        var success = false

        // 先朗读日语 (使用相同的 ID)
        if (japanese.isNotBlank()) {
            success = speakJapanese(japanese, id)
        }

        // 再朗读中文（追加到队列，使用相同的 ID）
        if (chinese.isNotBlank()) {
            success = speakChinese(chinese, TextToSpeech.QUEUE_ADD, id) || success
        }

        return success
    }
    fun stop() {
        val result = tts?.stop()
        if (result == TextToSpeech.ERROR) {
            Log.w(TAG, "stop 返回 ERROR，引擎可能已失效")
            handleTtsError()
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    /**
     * 清理文本，移除注音符号
     * 支持格式：
     * - 汉字(kana) -> 汉字
     * - 汉字（kana）-> 汉字
     * - 汉字[kana] -> 汉字
     */
    private fun cleanText(text: String): String {
        // 移除 (...) （...） 和 [...] 内容
        // 使用 | 分隔不同括号类型，避免混合匹配
        return text.replace(Regex("\\(.*?\\)|（.*?）|\\[.*?\\]"), "")
    }
}
