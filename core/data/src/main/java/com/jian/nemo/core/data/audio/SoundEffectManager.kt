package com.jian.nemo.core.data.audio

import android.content.Context
import com.jian.nemo.core.common.network.Dispatcher
import com.jian.nemo.core.common.network.NemoDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundEffectType {
    CORRECT,
    ERROR,
    GOOD,
    HARD,
    EASY,
    OTHER
}

@Singleton
class SoundEffectManager private constructor(
    ioDispatcher: CoroutineDispatcher,
    private val engineFactory: SoundPoolEngineFactory,
    private val logger: SoundEffectLogger,
    private val clock: () -> Long,
    private val pendingPlayTimeoutMs: Long
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @Dispatcher(NemoDispatchers.IO) ioDispatcher: CoroutineDispatcher
    ) : this(
        ioDispatcher = ioDispatcher,
        engineFactory = AndroidSoundPoolEngineFactory(context),
        logger = AndroidSoundEffectLogger,
        clock = { System.nanoTime() / 1_000_000L },
        pendingPlayTimeoutMs = DEFAULT_PENDING_PLAY_TIMEOUT_MS
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val soundDispatcher: CoroutineDispatcher = ioDispatcher.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + soundDispatcher)

    private var engine: SoundPoolEngine? = null
    private val soundMap = mutableMapOf<SoundEffectType, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private var pendingSoundId: Int? = null
    private var pendingSoundRequestedAtMs: Long = 0L

    private val soundFiles = linkedMapOf(
        SoundEffectType.CORRECT to "$AUDIO_ASSET_DIR/correct.mp3",
        SoundEffectType.ERROR to "$AUDIO_ASSET_DIR/error.mp3",
        SoundEffectType.GOOD to "$AUDIO_ASSET_DIR/sound_good.mp3",
        SoundEffectType.HARD to "$AUDIO_ASSET_DIR/sound_hard.mp3",
        SoundEffectType.EASY to "$AUDIO_ASSET_DIR/sound_easy.mp3",
        SoundEffectType.OTHER to "$AUDIO_ASSET_DIR/sound_other.mp3"
    )

    fun preload() {
        scope.launch {
            ensureInitializedInternal()
        }
    }

    fun play(type: SoundEffectType) {
        val requestedAtMs = clock()
        scope.launch {
            ensureInitializedInternal()
            val activeEngine = engine ?: return@launch
            val soundId = soundMap[type] ?: return@launch
            if (soundId == 0) return@launch

            val elapsedMs = clock() - requestedAtMs
            if (elapsedMs !in 0L..pendingPlayTimeoutMs) {
                return@launch
            }

            if (loadedSoundIds.contains(soundId)) {
                pendingSoundId = null
                pendingSoundRequestedAtMs = 0L
                try {
                    val streamId = activeEngine.play(soundId)
                    if (streamId == 0) {
                        logger.w(TAG, "SoundPool returned streamId=0 for sound effect: $type")
                    }
                } catch (e: Throwable) {
                    logger.w(TAG, "Failed to play sound effect: $type", e)
                }
            } else {
                pendingSoundId = soundId
                pendingSoundRequestedAtMs = requestedAtMs
            }
        }
    }

    fun release() {
        scope.launch {
            val activeEngine = engine
            engine = null
            soundMap.clear()
            loadedSoundIds.clear()
            pendingSoundId = null
            pendingSoundRequestedAtMs = 0L
            try {
                activeEngine?.release()
            } catch (e: Throwable) {
                logger.w(TAG, "Failed to release SoundPool", e)
            }
        }
    }

    private fun ensureInitializedInternal() {
        if (engine != null) return

        val createdEngine = try {
            engineFactory.create()
        } catch (e: Throwable) {
            logger.w(TAG, "Failed to create SoundPool", e)
            return
        }

        try {
            createdEngine.setOnLoadCompleteListener { sampleId, status ->
                scope.launch {
                    if (engine !== createdEngine) return@launch
                    if (status == 0) {
                        loadedSoundIds.add(sampleId)
                        if (pendingSoundId == sampleId) {
                            val elapsedMs = clock() - pendingSoundRequestedAtMs
                            pendingSoundId = null
                            pendingSoundRequestedAtMs = 0L
                            if (elapsedMs in 0L..pendingPlayTimeoutMs) {
                                try {
                                    val streamId = createdEngine.play(sampleId)
                                    if (streamId == 0) {
                                        logger.w(TAG, "SoundPool returned streamId=0 for pending sound effect: $sampleId")
                                    }
                                } catch (e: Throwable) {
                                    logger.w(TAG, "Failed to play pending sound effect: $sampleId", e)
                                }
                            }
                        }
                    } else {
                        logger.w(TAG, "Failed to decode sound effect: sampleId=$sampleId, status=$status")
                        loadedSoundIds.remove(sampleId)
                        soundMap.entries.removeAll { it.value == sampleId }
                        if (pendingSoundId == sampleId) {
                            pendingSoundId = null
                            pendingSoundRequestedAtMs = 0L
                        }
                        if (soundMap.isEmpty()) {
                            releaseEmptyEngine(
                                targetEngine = createdEngine,
                                reason = "All sound effects failed to decode; releasing empty SoundPool for retry"
                            )
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            logger.w(TAG, "Failed to attach OnLoadCompleteListener; releasing SoundPool", e)
            try {
                createdEngine.release()
            } catch (releaseError: Throwable) {
                logger.w(TAG, "Failed to release SoundPool after listener setup error", releaseError)
            }
            return
        }

        engine = createdEngine

        soundFiles.forEach { (type, assetPath) ->
            try {
                val soundId = createdEngine.loadAsset(assetPath)
                if (soundId != 0) {
                    soundMap[type] = soundId
                } else {
                    logger.w(TAG, "Failed to load sound effect (soundId=0): $assetPath")
                }
            } catch (e: Throwable) {
                logger.w(TAG, "Failed to load sound effect: $assetPath", e)
            }
        }

        if (soundMap.isEmpty()) {
            releaseEmptyEngine(
                targetEngine = createdEngine,
                reason = "No sound effects loaded; releasing empty SoundPool for retry"
            )
        }
    }

    private fun releaseEmptyEngine(targetEngine: SoundPoolEngine, reason: String) {
        logger.w(TAG, reason)
        if (engine === targetEngine) {
            engine = null
        }
        soundMap.clear()
        loadedSoundIds.clear()
        pendingSoundId = null
        pendingSoundRequestedAtMs = 0L
        try {
            targetEngine.release()
        } catch (e: Throwable) {
            logger.w(TAG, "Failed to release empty SoundPool", e)
        }
    }

    internal fun closeForTest() {
        scope.cancel()
    }

    internal companion object {
        private const val TAG = "SoundEffectManager"
        private const val AUDIO_ASSET_DIR = "audio"
        const val DEFAULT_PENDING_PLAY_TIMEOUT_MS = 400L

        internal fun createForTest(
            ioDispatcher: CoroutineDispatcher,
            engineFactory: SoundPoolEngineFactory,
            logger: SoundEffectLogger = NoOpSoundEffectLogger,
            clock: () -> Long = { System.nanoTime() / 1_000_000L },
            pendingPlayTimeoutMs: Long = DEFAULT_PENDING_PLAY_TIMEOUT_MS
        ): SoundEffectManager = SoundEffectManager(
            ioDispatcher = ioDispatcher,
            engineFactory = engineFactory,
            logger = logger,
            clock = clock,
            pendingPlayTimeoutMs = pendingPlayTimeoutMs
        )

        private object NoOpSoundEffectLogger : SoundEffectLogger {
            override fun w(tag: String, message: String, throwable: Throwable?) = Unit
        }
    }
}
