package com.jian.nemo.core.domain.repository

interface AudioRepository {
    /**
     * TTS 状态事件流
     */
    val ttsEvents: kotlinx.coroutines.flow.Flow<TtsEvent>

    /**
     * 播放 TTS 音频
     * @param text 要朗读的文本
     * @param language 语言代码 (默认为日语)
     * @param id 唯一标识符 (可选)
     */
    fun playTts(text: String, language: String = "ja-JP", id: String? = null)

    /**
     * 播放音效
     */
    fun playSoundEffect(type: AudioEffectType)

    /**
     * 停止播放
     */
    fun stop()

    /**
     * 释放资源
     */
    fun release()

    /**
     * 获取可用语音列表 (当前语言)
     */
    fun getAvailableVoices(): List<com.jian.nemo.core.domain.model.TtsVoice>

    /**
     * 预览指定语音（不保存设置，仅临时切换并播放）
     */
    suspend fun previewVoice(voiceName: String, text: String)
}

sealed interface TtsEvent {
    data class OnStart(val id: String) : TtsEvent
    data class OnDone(val id: String) : TtsEvent
    data class OnError(val id: String, val error: String? = null) : TtsEvent
    object GoogleTtsMissing : TtsEvent
}

enum class AudioEffectType {
    CORRECT,
    WRONG,
    COMPLETE
}
