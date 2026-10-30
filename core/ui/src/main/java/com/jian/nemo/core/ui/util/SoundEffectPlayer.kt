package com.jian.nemo.core.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/**
 * 资源来自 assets/audio/correct.mp3 与 assets/audio/error.mp3
 */
object SoundEffectPlayer {
    @Volatile
    private var soundPool: SoundPool? = null
    @Volatile
    private var correctSoundId: Int = 0
    @Volatile
    private var errorSoundId: Int = 0
    @Volatile
    private var goodSoundId: Int = 0
    @Volatile
    private var hardSoundId: Int = 0
    @Volatile
    private var easySoundId: Int = 0
    @Volatile
    private var otherSoundId: Int = 0
    @Volatile
    private var loadedIds: MutableSet<Int> = mutableSetOf()

    private const val CORRECT_ASSET = "audio/correct.mp3"
    private const val ERROR_ASSET = "audio/error.mp3"
    private const val GOOD_ASSET = "audio/sound_good.mp3"
    private const val HARD_ASSET = "audio/sound_hard.mp3"
    private const val EASY_ASSET = "audio/sound_easy.mp3"
    private const val OTHER_ASSET = "audio/sound_other.mp3"

    @Synchronized
    fun init(context: Context) {
        if (soundPool != null) return

        // 强制走媒体音量通道（STREAM_MUSIC）
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val sp = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attributes)
            .build()

        try {
            context.assets.openFd(CORRECT_ASSET).use { afd ->
                correctSoundId = sp.load(afd, 1)
            }
            context.assets.openFd(ERROR_ASSET).use { afd ->
                errorSoundId = sp.load(afd, 1)
            }
            context.assets.openFd(GOOD_ASSET).use { afd ->
                goodSoundId = sp.load(afd, 1)
            }
            context.assets.openFd(HARD_ASSET).use { afd ->
                hardSoundId = sp.load(afd, 1)
            }
            context.assets.openFd(EASY_ASSET).use { afd ->
                easySoundId = sp.load(afd, 1)
            }
            context.assets.openFd(OTHER_ASSET).use { afd ->
                otherSoundId = sp.load(afd, 1)
            }
        } catch (_: Throwable) {
            // 若加载失败，不崩溃；稍后将使用 MediaPlayer 兜底
        }

        sp.setOnLoadCompleteListener { _, sampleId, _ ->
            loadedIds.add(sampleId)
        }

        soundPool = sp
    }

    private fun bothLoaded(): Boolean {
        val c = correctSoundId
        val e = errorSoundId
        return c != 0 && e != 0 && loadedIds.contains(c) && loadedIds.contains(e)
    }

    fun playCorrect(context: Context) {
        ensureInit(context)
        val sp = soundPool
        val id = correctSoundId
        if (sp != null && id != 0 && loadedIds.contains(id)) {
            val stream = sp.play(id, 1f, 1f, 1, 0, 1f)
            if (stream == 0) fallbackPlay(context, CORRECT_ASSET)
        } else {
            fallbackPlay(context, CORRECT_ASSET)
        }
    }

    fun playError(context: Context) {
        ensureInit(context)
        val sp = soundPool
        val id = errorSoundId
        if (sp != null && id != 0 && loadedIds.contains(id)) {
            val stream = sp.play(id, 1f, 1f, 1, 0, 1f)
            if (stream == 0) fallbackPlay(context, ERROR_ASSET)
        } else {
            fallbackPlay(context, ERROR_ASSET)
        }
    }

    fun playGoodSound(context: Context) {
        ensureInit(context)
        val sp = soundPool
        val id = goodSoundId
        if (sp != null && id != 0 && loadedIds.contains(id)) {
            val stream = sp.play(id, 1f, 1f, 1, 0, 1f)
            if (stream == 0) fallbackPlay(context, GOOD_ASSET)
        } else {
            fallbackPlay(context, GOOD_ASSET)
        }
    }

    fun playHardSound(context: Context) {
        ensureInit(context)
        val sp = soundPool
        val id = hardSoundId
        if (sp != null && id != 0 && loadedIds.contains(id)) {
            val stream = sp.play(id, 1f, 1f, 1, 0, 1f)
            if (stream == 0) fallbackPlay(context, HARD_ASSET)
        } else {
            fallbackPlay(context, HARD_ASSET)
        }
    }

    fun playEasySound(context: Context) {
        ensureInit(context)
        val sp = soundPool
        val id = easySoundId
        if (sp != null && id != 0 && loadedIds.contains(id)) {
            val stream = sp.play(id, 1f, 1f, 1, 0, 1f)
            if (stream == 0) fallbackPlay(context, EASY_ASSET)
        } else {
            fallbackPlay(context, EASY_ASSET)
        }
    }

    fun playOtherSound(context: Context) {
        ensureInit(context)
        val sp = soundPool
        val id = otherSoundId
        if (sp != null && id != 0 && loadedIds.contains(id)) {
            val stream = sp.play(id, 1f, 1f, 1, 0, 1f)
            if (stream == 0) fallbackPlay(context, OTHER_ASSET)
        } else {
            fallbackPlay(context, OTHER_ASSET)
        }
    }

    private fun fallbackPlay(context: Context, assetPath: String) {
        var mp: MediaPlayer? = null
        try {
            // 媒体通道播放兜底（MediaPlayer）
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val afd = context.assets.openFd(assetPath)
            mp = MediaPlayer()
            mp.setAudioAttributes(attrs)
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { player ->
                try { player.release() } catch (_: Exception) {}
            }
            // 修复：添加错误监听器，确保播放失败时也释放资源
            mp.setOnErrorListener { player, _, _ ->
                try { player.release() } catch (_: Exception) {}
                true // 返回 true 表示已处理错误
            }
            mp.prepareAsync()
        } catch (_: Throwable) {
            // 异常时释放已创建的 MediaPlayer
            try { mp?.release() } catch (_: Exception) {}
        }
    }

    @Synchronized
    private fun ensureInit(context: Context) {
        if (soundPool == null) {
            init(context.applicationContext)
        }
    }

    @Synchronized
    fun release() {
        soundPool?.release()
        soundPool = null
        loadedIds.clear()
        correctSoundId = 0
        errorSoundId = 0
        goodSoundId = 0
        otherSoundId = 0
    }
}
