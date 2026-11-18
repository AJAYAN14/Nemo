package com.jian.nemo.core.data.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class SoundEffectManagerTest {

    @Test
    fun `preload and play run off caller thread without blocking caller`() = runBlocking {
        val executor = Executors.newFixedThreadPool(4) { runnable ->
            Thread(runnable, "sfx-test-pool-${System.nanoTime()}")
        }
        val poolDispatcher = executor.asCoroutineDispatcher()
        val callerThreadId = Thread.currentThread().id
        val createStarted = CountDownLatch(1)
        val allowCreateFinish = CountDownLatch(1)
        val playRecorded = CountDownLatch(1)

        val factory = FakeSoundPoolEngineFactory(
            autoCompleteLoad = true,
            onCreateHook = {
                createStarted.countDown()
                allowCreateFinish.await(2, TimeUnit.SECONDS)
            },
            onPlayHook = {
                playRecorded.countDown()
            }
        )
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = poolDispatcher,
            engineFactory = factory
        )

        try {
            manager.preload()
            assertTrue(createStarted.await(2, TimeUnit.SECONDS))

            val startNs = System.nanoTime()
            manager.play(SoundEffectType.GOOD)
            val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
            assertTrue("play() 应立即非阻塞返回，实际耗时=${elapsedMs}ms", elapsedMs < 150L)

            allowCreateFinish.countDown()
            assertTrue(playRecorded.await(2, TimeUnit.SECONDS))

            assertTrue(factory.recordedThreadIds.isNotEmpty())
            assertTrue(factory.recordedThreadIds.none { it == callerThreadId })
            val engine = factory.engines.single()
            assertEquals(1, engine.playedSoundIds.size)
            assertEquals(engine.assetToSoundId["audio/sound_good.mp3"], engine.playedSoundIds.single())
        } finally {
            allowCreateFinish.countDown()
            manager.closeForTest()
            executor.shutdownNow()
        }
    }

    @Test
    fun `repeated preload is idempotent and loads all six audio assets once`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeSoundPoolEngineFactory(autoCompleteLoad = true)
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory
        )

        try {
            manager.preload()
            manager.preload()
            manager.preload()
            advanceUntilIdle()

            assertEquals(1, factory.createCount)
            val engine = factory.engines.single()
            assertEquals(
                listOf(
                    "audio/correct.mp3",
                    "audio/error.mp3",
                    "audio/sound_good.mp3",
                    "audio/sound_hard.mp3",
                    "audio/sound_easy.mp3",
                    "audio/sound_other.mp3"
                ),
                engine.loadedAssetPaths
            )
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `create failure allows automatic retry on subsequent call`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeSoundPoolEngineFactory(
            autoCompleteLoad = true,
            failFirstCreateAttempts = 1
        )
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory
        )

        try {
            manager.preload()
            advanceUntilIdle()
            assertEquals(1, factory.createCount)
            assertTrue(factory.engines.isEmpty())

            manager.play(SoundEffectType.CORRECT)
            advanceUntilIdle()

            assertEquals(2, factory.createCount)
            val engine = factory.engines.single()
            assertEquals(
                listOf(engine.assetToSoundId["audio/correct.mp3"]),
                engine.playedSoundIds
            )
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `single asset load failure does not affect remaining assets`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeSoundPoolEngineFactory(
            autoCompleteLoad = true,
            failingAssets = setOf("audio/error.mp3")
        )
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory
        )

        try {
            manager.preload()
            advanceUntilIdle()

            val engine = factory.engines.single()
            assertEquals(5, engine.assetToSoundId.size)
            assertTrue("audio/error.mp3" !in engine.assetToSoundId)

            manager.play(SoundEffectType.ERROR)
            manager.play(SoundEffectType.EASY)
            advanceUntilIdle()

            assertEquals(
                listOf(engine.assetToSoundId["audio/sound_easy.mp3"]),
                engine.playedSoundIds
            )
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `pending play triggers within timeout window and drops when expired or failed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var nowMs = 1_000L
        val factory = FakeSoundPoolEngineFactory(autoCompleteLoad = false)
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory,
            clock = { nowMs },
            pendingPlayTimeoutMs = 400L
        )

        try {
            manager.play(SoundEffectType.GOOD)
            advanceUntilIdle()

            val engine = factory.engines.single()
            val goodId = engine.assetToSoundId.getValue("audio/sound_good.mp3")
            val easyId = engine.assetToSoundId.getValue("audio/sound_easy.mp3")
            val hardId = engine.assetToSoundId.getValue("audio/sound_hard.mp3")
            assertTrue(engine.playedSoundIds.isEmpty())

            nowMs += 250L
            engine.completeLoad(goodId, status = 0)
            advanceUntilIdle()
            assertEquals(listOf(goodId), engine.playedSoundIds)

            manager.play(SoundEffectType.EASY)
            advanceUntilIdle()
            nowMs += 401L
            engine.completeLoad(easyId, status = 0)
            advanceUntilIdle()
            assertEquals(listOf(goodId), engine.playedSoundIds)

            manager.play(SoundEffectType.EASY)
            advanceUntilIdle()
            assertEquals(listOf(goodId, easyId), engine.playedSoundIds)

            manager.play(SoundEffectType.HARD)
            advanceUntilIdle()
            nowMs += 100L
            engine.completeLoad(hardId, status = -1)
            advanceUntilIdle()
            assertEquals(listOf(goodId, easyId), engine.playedSoundIds)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `rapid pending plays keep only latest sound request`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var nowMs = 5_000L
        val factory = FakeSoundPoolEngineFactory(autoCompleteLoad = false)
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory,
            clock = { nowMs },
            pendingPlayTimeoutMs = 400L
        )

        try {
            manager.play(SoundEffectType.HARD)
            nowMs += 50L
            manager.play(SoundEffectType.OTHER)
            advanceUntilIdle()

            val engine = factory.engines.single()
            val hardId = engine.assetToSoundId.getValue("audio/sound_hard.mp3")
            val otherId = engine.assetToSoundId.getValue("audio/sound_other.mp3")

            nowMs += 50L
            engine.completeLoad(hardId, status = 0)
            engine.completeLoad(otherId, status = 0)
            advanceUntilIdle()

            assertEquals(listOf(otherId), engine.playedSoundIds)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `release ignores stale load completion callbacks and allows reinitialization`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var nowMs = 10_000L
        val factory = FakeSoundPoolEngineFactory(autoCompleteLoad = false)
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory,
            clock = { nowMs },
            pendingPlayTimeoutMs = 400L
        )

        try {
            manager.play(SoundEffectType.CORRECT)
            advanceUntilIdle()

            val firstEngine = factory.engines[0]
            val firstCorrectId = firstEngine.assetToSoundId.getValue("audio/correct.mp3")

            manager.release()
            advanceUntilIdle()
            assertEquals(1, firstEngine.releaseCount)

            firstEngine.completeLoad(firstCorrectId, status = 0)
            advanceUntilIdle()
            assertTrue(firstEngine.playedSoundIds.isEmpty())

            manager.play(SoundEffectType.ERROR)
            advanceUntilIdle()
            assertEquals(2, factory.engines.size)

            val secondEngine = factory.engines[1]
            val secondErrorId = secondEngine.assetToSoundId.getValue("audio/error.mp3")

            firstEngine.completeLoad(secondErrorId, status = 0)
            advanceUntilIdle()
            assertTrue(secondEngine.playedSoundIds.isEmpty())

            nowMs += 100L
            secondEngine.completeLoad(secondErrorId, status = 0)
            advanceUntilIdle()
            assertEquals(listOf(secondErrorId), secondEngine.playedSoundIds)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `play drops stale request when queue wait exceeds timeout even if asset is already loaded`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var nowMs = 20_000L
        val factory = FakeSoundPoolEngineFactory(autoCompleteLoad = true)
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory,
            clock = { nowMs },
            pendingPlayTimeoutMs = 400L
        )

        try {
            manager.preload()
            advanceUntilIdle()

            val engine = factory.engines.single()
            val correctId = engine.assetToSoundId.getValue("audio/correct.mp3")

            manager.play(SoundEffectType.CORRECT)
            nowMs += 401L
            advanceUntilIdle()
            assertTrue("排队等待超过 400ms 的陈旧音效不应播放", engine.playedSoundIds.isEmpty())

            manager.play(SoundEffectType.CORRECT)
            nowMs += 100L
            advanceUntilIdle()
            assertEquals(listOf(correctId), engine.playedSoundIds)
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `all asset load failures release empty engine and allow retry on next call`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val allAssets = setOf(
            "audio/correct.mp3",
            "audio/error.mp3",
            "audio/sound_good.mp3",
            "audio/sound_hard.mp3",
            "audio/sound_easy.mp3",
            "audio/sound_other.mp3"
        )
        val factory = FakeSoundPoolEngineFactory(
            autoCompleteLoad = true,
            failingAssetsProvider = { attempt -> if (attempt == 1) allAssets else emptySet() }
        )
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory
        )

        try {
            manager.preload()
            advanceUntilIdle()

            assertEquals(1, factory.createCount)
            val failedEngine = factory.engines.single()
            assertEquals(1, failedEngine.releaseCount)

            manager.play(SoundEffectType.GOOD)
            advanceUntilIdle()

            assertEquals(2, factory.createCount)
            val recoveredEngine = factory.engines[1]
            assertEquals(
                listOf(recoveredEngine.assetToSoundId["audio/sound_good.mp3"]),
                recoveredEngine.playedSoundIds
            )
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `failed async decode clears pending state and removes broken sound mapping`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var nowMs = 30_000L
        val warnings = mutableListOf<String>()
        val logger = object : SoundEffectLogger {
            override fun w(tag: String, message: String, throwable: Throwable?) {
                warnings.add(message)
            }
        }
        val factory = FakeSoundPoolEngineFactory(autoCompleteLoad = false)
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory,
            logger = logger,
            clock = { nowMs },
            pendingPlayTimeoutMs = 400L
        )

        try {
            manager.play(SoundEffectType.HARD)
            advanceUntilIdle()

            val engine = factory.engines.single()
            val hardId = engine.assetToSoundId.getValue("audio/sound_hard.mp3")
            val goodId = engine.assetToSoundId.getValue("audio/sound_good.mp3")

            engine.completeLoad(hardId, status = -1)
            advanceUntilIdle()
            assertTrue(warnings.any { it.contains("sampleId=$hardId") && it.contains("status=-1") })

            manager.play(SoundEffectType.GOOD)
            manager.play(SoundEffectType.HARD)
            advanceUntilIdle()

            nowMs += 100L
            engine.completeLoad(goodId, status = 0)
            advanceUntilIdle()
            assertEquals(
                "已解码失败的音效再次调用时不应覆盖其他合法音效的待播状态",
                listOf(goodId),
                engine.playedSoundIds
            )
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `all async decode failures release empty engine and allow retry on next call`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var nowMs = 40_000L
        val warnings = mutableListOf<String>()
        val logger = object : SoundEffectLogger {
            override fun w(tag: String, message: String, throwable: Throwable?) {
                warnings.add(message)
            }
        }
        val factory = FakeSoundPoolEngineFactory(
            autoCompleteLoadProvider = { attempt -> attempt >= 2 }
        )
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory,
            logger = logger,
            clock = { nowMs },
            pendingPlayTimeoutMs = 400L
        )

        try {
            manager.preload()
            advanceUntilIdle()

            assertEquals(1, factory.createCount)
            val firstEngine = factory.engines.single()
            assertEquals(6, firstEngine.assetToSoundId.size)
            assertEquals(0, firstEngine.releaseCount)

            val allIds = firstEngine.assetToSoundId.values.toList()
            allIds.dropLast(1).forEach { id ->
                firstEngine.completeLoad(id, status = -1)
            }
            advanceUntilIdle()
            assertEquals("尚有未完成解码的音效时不应提前释放引擎", 0, firstEngine.releaseCount)

            firstEngine.completeLoad(allIds.last(), status = -1)
            advanceUntilIdle()
            assertEquals("全部音效异步解码失败后应释放空引擎", 1, firstEngine.releaseCount)
            assertTrue(warnings.any { it.contains("All sound effects failed to decode") })

            manager.play(SoundEffectType.CORRECT)
            advanceUntilIdle()

            assertEquals("释放空引擎后下一次调用应自动重建并恢复播放", 2, factory.createCount)
            val recoveredEngine = factory.engines[1]
            assertEquals(
                listOf(recoveredEngine.assetToSoundId["audio/correct.mp3"]),
                recoveredEngine.playedSoundIds
            )
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `mixed sync load and async decode failures release empty engine when no assets remain`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val fiveFailingAssets = setOf(
            "audio/correct.mp3",
            "audio/error.mp3",
            "audio/sound_hard.mp3",
            "audio/sound_easy.mp3",
            "audio/sound_other.mp3"
        )
        val factory = FakeSoundPoolEngineFactory(
            autoCompleteLoadProvider = { attempt -> attempt >= 2 },
            failingAssetsProvider = { attempt -> if (attempt == 1) fiveFailingAssets else emptySet() }
        )
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory
        )

        try {
            manager.preload()
            advanceUntilIdle()

            val firstEngine = factory.engines.single()
            assertEquals(1, firstEngine.assetToSoundId.size)
            val onlyId = firstEngine.assetToSoundId.getValue("audio/sound_good.mp3")
            assertEquals(0, firstEngine.releaseCount)

            firstEngine.completeLoad(onlyId, status = -1)
            advanceUntilIdle()
            assertEquals(1, firstEngine.releaseCount)

            manager.play(SoundEffectType.GOOD)
            advanceUntilIdle()

            assertEquals(2, factory.createCount)
            val recoveredEngine = factory.engines[1]
            assertEquals(
                listOf(recoveredEngine.assetToSoundId["audio/sound_good.mp3"]),
                recoveredEngine.playedSoundIds
            )
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `listener registration failure releases created engine and allows retry`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val factory = FakeSoundPoolEngineFactory(
            autoCompleteLoad = true,
            failListenerSetupAttempts = setOf(1)
        )
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory
        )

        try {
            manager.preload()
            advanceUntilIdle()

            assertEquals(1, factory.createCount)
            val failedEngine = factory.engines.single()
            assertEquals(1, failedEngine.releaseCount)

            manager.play(SoundEffectType.EASY)
            advanceUntilIdle()

            assertEquals(2, factory.createCount)
            val recoveredEngine = factory.engines[1]
            assertEquals(
                listOf(recoveredEngine.assetToSoundId["audio/sound_easy.mp3"]),
                recoveredEngine.playedSoundIds
            )
        } finally {
            manager.closeForTest()
        }
    }

    @Test
    fun `zero soundId and zero streamId log warnings and release empty engine when all return zero`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val warnings = mutableListOf<String>()
        val logger = object : SoundEffectLogger {
            override fun w(tag: String, message: String, throwable: Throwable?) {
                warnings.add(message)
            }
        }
        val factory = FakeSoundPoolEngineFactory(
            autoCompleteLoad = true,
            returnZeroSoundIdAttempts = setOf(1),
            returnZeroStreamId = true
        )
        val manager = SoundEffectManager.createForTest(
            ioDispatcher = dispatcher,
            engineFactory = factory,
            logger = logger
        )

        try {
            manager.preload()
            advanceUntilIdle()

            assertEquals(1, factory.createCount)
            assertEquals(1, factory.engines.single().releaseCount)
            assertTrue(warnings.any { it.contains("soundId=0") })

            manager.play(SoundEffectType.OTHER)
            advanceUntilIdle()

            assertEquals(2, factory.createCount)
            val recoveredEngine = factory.engines[1]
            assertEquals(
                listOf(recoveredEngine.assetToSoundId["audio/sound_other.mp3"]),
                recoveredEngine.playedSoundIds
            )
            assertTrue(warnings.any { it.contains("streamId=0") })
        } finally {
            manager.closeForTest()
        }
    }
}

internal class FakeSoundPoolEngineFactory(
    private val autoCompleteLoad: Boolean = true,
    private val autoCompleteLoadProvider: ((attempt: Int) -> Boolean)? = null,
    private val failFirstCreateAttempts: Int = 0,
    private val failListenerSetupAttempts: Set<Int> = emptySet(),
    private val returnZeroSoundIdAttempts: Set<Int> = emptySet(),
    private val returnZeroStreamId: Boolean = false,
    private val failingAssets: Set<String> = emptySet(),
    private val failingAssetsProvider: ((attempt: Int) -> Set<String>)? = null,
    private val onCreateHook: (() -> Unit)? = null,
    private val onPlayHook: ((Int) -> Unit)? = null
) : SoundPoolEngineFactory {

    val engines = CopyOnWriteArrayList<FakeSoundPoolEngine>()
    val recordedThreadIds = CopyOnWriteArrayList<Long>()
    private val createAttempts = AtomicInteger(0)
    val createCount: Int get() = createAttempts.get()

    override fun create(): SoundPoolEngine {
        recordedThreadIds.add(Thread.currentThread().id)
        val attempt = createAttempts.incrementAndGet()
        onCreateHook?.invoke()
        if (attempt <= failFirstCreateAttempts) {
            throw IllegalStateException("Simulated SoundPool creation failure #$attempt")
        }
        val engine = FakeSoundPoolEngine(
            autoCompleteLoad = autoCompleteLoadProvider?.invoke(attempt) ?: autoCompleteLoad,
            failListenerSetup = attempt in failListenerSetupAttempts,
            returnZeroSoundId = attempt in returnZeroSoundIdAttempts,
            returnZeroStreamId = returnZeroStreamId,
            failingAssets = failingAssetsProvider?.invoke(attempt) ?: failingAssets,
            recordedThreadIds = recordedThreadIds,
            onPlayHook = onPlayHook
        )
        engines.add(engine)
        return engine
    }
}

internal class FakeSoundPoolEngine(
    private val autoCompleteLoad: Boolean,
    private val failListenerSetup: Boolean,
    private val returnZeroSoundId: Boolean,
    private val returnZeroStreamId: Boolean,
    private val failingAssets: Set<String>,
    private val recordedThreadIds: CopyOnWriteArrayList<Long>,
    private val onPlayHook: ((Int) -> Unit)?
) : SoundPoolEngine {

    private val nextSoundId = AtomicInteger(0)
    private var loadCompleteListener: ((sampleId: Int, status: Int) -> Unit)? = null

    val loadedAssetPaths = CopyOnWriteArrayList<String>()
    val assetToSoundId = LinkedHashMap<String, Int>()
    val playedSoundIds = CopyOnWriteArrayList<Int>()
    var releaseCount: Int = 0
        private set

    override fun setOnLoadCompleteListener(listener: (sampleId: Int, status: Int) -> Unit) {
        if (failListenerSetup) {
            throw IllegalStateException("Simulated listener registration failure")
        }
        loadCompleteListener = listener
    }

    override fun loadAsset(assetPath: String): Int {
        recordedThreadIds.add(Thread.currentThread().id)
        if (assetPath in failingAssets) {
            throw IOException("Simulated missing asset: $assetPath")
        }
        if (returnZeroSoundId) {
            return 0
        }
        val soundId = nextSoundId.incrementAndGet()
        loadedAssetPaths.add(assetPath)
        assetToSoundId[assetPath] = soundId
        if (autoCompleteLoad) {
            loadCompleteListener?.invoke(soundId, 0)
        }
        return soundId
    }

    override fun play(soundId: Int, volume: Float): Int {
        recordedThreadIds.add(Thread.currentThread().id)
        playedSoundIds.add(soundId)
        onPlayHook?.invoke(soundId)
        return if (returnZeroStreamId) 0 else playedSoundIds.size
    }

    override fun release() {
        recordedThreadIds.add(Thread.currentThread().id)
        releaseCount += 1
    }

    fun completeLoad(sampleId: Int, status: Int) {
        loadCompleteListener?.invoke(sampleId, status)
    }
}
