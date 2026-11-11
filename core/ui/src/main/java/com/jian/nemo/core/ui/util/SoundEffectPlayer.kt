package com.jian.nemo.core.ui.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.util.concurrent.ConcurrentHashMap

/**
 * 界面交互轻量短音效播放器（基于 SoundPool 内存直出，零延迟）
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

    private val loadedIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private const val CORRECT_ASSET = "audio/correct.mp3"
    private const val ERROR_ASSET = "audio/error.mp3"
    private const val GOOD_ASSET = "audio/sound_good.mp3"
    private const val HARD_ASSET = "audio/sound_hard.mp3"
    private const val EASY_ASSET = "audio/sound_easy.mp3"
    private const val OTHER_ASSET = "audio/sound_other.mp3"

    @Synchronized
    fun init(context: Context) {
        if (soundPool != null) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val sp = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attributes)
            .build()

        sp.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedIds.add(sampleId)
            }
        }

        try {
            val am = context.assets
            am.openFd(CORRECT_ASSET).use { afd ->
                correctSoundId = sp.load(afd, 1)
            }
            am.openFd(ERROR_ASSET).use { afd ->
                errorSoundId = sp.load(afd, 1)
            }
            am.openFd(GOOD_ASSET).use { afd ->
                goodSoundId = sp.load(afd, 1)
            }
            am.openFd(HARD_ASSET).use { afd ->
                hardSoundId = sp.load(afd, 1)
            }
            am.openFd(EASY_ASSET).use { afd ->
                easySoundId = sp.load(afd, 1)
            }
            am.openFd(OTHER_ASSET).use { afd ->
                otherSoundId = sp.load(afd, 1)
            }
        } catch (_: Throwable) {
            // 资源解压异常时静默忽略，保证不崩溃
        }

        soundPool = sp
    }

    private fun playSound(context: Context, soundId: Int) {
        ensureInit(context)
        val sp = soundPool ?: return
        if (soundId != 0 && loadedIds.contains(soundId)) {
            sp.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun playCorrect(context: Context) = playSound(context, correctSoundId)

    fun playError(context: Context) = playSound(context, errorSoundId)

    fun playGoodSound(context: Context) = playSound(context, goodSoundId)

    fun playHardSound(context: Context) = playSound(context, hardSoundId)

    fun playEasySound(context: Context) = playSound(context, easySoundId)

    fun playOtherSound(context: Context) = playSound(context, otherSoundId)

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
        hardSoundId = 0
        easySoundId = 0
        otherSoundId = 0
    }
}
