package com.jian.nemo.core.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.jian.nemo.core.common.network.Dispatcher
import com.jian.nemo.core.common.network.NemoDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundEffectType {
    CORRECT,
    WRONG,
    COMPLETE
}

@Singleton
class SoundEffectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(NemoDispatchers.IO) ioDispatcher: CoroutineDispatcher
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val soundDispatcher: CoroutineDispatcher = ioDispatcher.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + soundDispatcher)

    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<SoundEffectType, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private var pendingSoundId: Int? = null
    private var isInitializedAttempted = false

    private val soundFiles = mapOf(
        SoundEffectType.CORRECT to "correct.mp3",
        SoundEffectType.WRONG to "wrong.mp3",
        SoundEffectType.COMPLETE to "complete.mp3"
    )

    fun play(type: SoundEffectType) {
        scope.launch {
            ensureInitializedInternal()
            val pool = soundPool ?: return@launch
            val soundId = soundMap[type] ?: return@launch
            if (soundId == 0) return@launch

            if (loadedSoundIds.contains(soundId)) {
                pool.play(soundId, 1f, 1f, 1, 0, 1f)
            } else {
                pendingSoundId = soundId
            }
        }
    }

    fun release() {
        scope.launch {
            soundPool?.release()
            soundPool = null
            soundMap.clear()
            loadedSoundIds.clear()
            pendingSoundId = null
            isInitializedAttempted = false
        }
    }

    private fun ensureInitializedInternal() {
        if (isInitializedAttempted) return
        isInitializedAttempted = true

        val availableAssets = try {
            context.assets.list(AUDIO_ASSET_DIR)?.toSet().orEmpty()
        } catch (e: IOException) {
            Log.w(TAG, "Failed to list audio assets", e)
            emptySet()
        }

        val existingEntries = soundFiles.filterValues { it in availableAssets }
        if (existingEntries.isEmpty()) {
            return
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                scope.launch {
                    if (soundPool !== pool) return@launch
                    loadedSoundIds.add(sampleId)
                    if (pendingSoundId == sampleId) {
                        pendingSoundId = null
                        pool.play(sampleId, 1f, 1f, 1, 0, 1f)
                    }
                }
            }
        }

        soundPool = pool

        existingEntries.forEach { (type, filename) ->
            try {
                context.assets.openFd("$AUDIO_ASSET_DIR/$filename").use { afd ->
                    val soundId = pool.load(afd, 1)
                    if (soundId != 0) {
                        soundMap[type] = soundId
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "Failed to load sound effect: $filename", e)
            }
        }
    }

    private companion object {
        private const val TAG = "SoundEffectManager"
        private const val AUDIO_ASSET_DIR = "audio"
    }
}
