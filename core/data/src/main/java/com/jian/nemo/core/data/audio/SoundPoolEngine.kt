package com.jian.nemo.core.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

internal interface SoundPoolEngine {
    fun setOnLoadCompleteListener(listener: (sampleId: Int, status: Int) -> Unit)
    fun loadAsset(assetPath: String): Int
    fun play(soundId: Int, volume: Float = 1f): Int
    fun release()
}

internal interface SoundPoolEngineFactory {
    fun create(): SoundPoolEngine
}

internal interface SoundEffectLogger {
    fun w(tag: String, message: String, throwable: Throwable? = null)
}

internal object AndroidSoundEffectLogger : SoundEffectLogger {
    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
}

internal class AndroidSoundPoolEngineFactory(
    context: Context
) : SoundPoolEngineFactory {

    private val appContext: Context = context.applicationContext

    override fun create(): SoundPoolEngine {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(audioAttributes)
            .build()

        return AndroidSoundPoolEngine(appContext, soundPool)
    }

    private companion object {
        private const val MAX_STREAMS = 6
    }
}

private class AndroidSoundPoolEngine(
    private val context: Context,
    private val soundPool: SoundPool
) : SoundPoolEngine {

    override fun setOnLoadCompleteListener(listener: (sampleId: Int, status: Int) -> Unit) {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            listener(sampleId, status)
        }
    }

    override fun loadAsset(assetPath: String): Int {
        return context.assets.openFd(assetPath).use { afd ->
            soundPool.load(afd, 1)
        }
    }

    override fun play(soundId: Int, volume: Float): Int {
        return soundPool.play(soundId, volume, volume, 1, 0, 1f)
    }

    override fun release() {
        soundPool.setOnLoadCompleteListener(null)
        soundPool.release()
    }
}
