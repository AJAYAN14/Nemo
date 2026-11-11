package com.jian.nemo.feature.settings

import android.content.ComponentCallbacks2
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AboutLogoCacheTest {

    @Test
    fun `calculateTargetSizePx 按屏幕宽度与密度计算并钳制到安全范围`() {
        assertEquals(816, AboutLogoCache.calculateTargetSizePx(screenWidthDp = 360, density = 3.0f))
        assertEquals(
            AboutLogoCacheManager.MIN_TARGET_SIZE_PX,
            AboutLogoCache.calculateTargetSizePx(screenWidthDp = 240, density = 1.0f)
        )
        assertEquals(
            AboutLogoCacheManager.MAX_TARGET_SIZE_PX,
            AboutLogoCache.calculateTargetSizePx(screenWidthDp = 800, density = 3.0f)
        )
    }

    @Test
    fun `calculateTargetSizePx 在非法输入时回落到默认值`() {
        assertEquals(
            AboutLogoCacheManager.DEFAULT_TARGET_SIZE_PX,
            AboutLogoCache.calculateTargetSizePx(screenWidthDp = 0, density = 3.0f)
        )
        assertEquals(
            AboutLogoCacheManager.DEFAULT_TARGET_SIZE_PX,
            AboutLogoCache.calculateTargetSizePx(screenWidthDp = -120, density = 2.0f)
        )
        assertEquals(
            AboutLogoCacheManager.DEFAULT_TARGET_SIZE_PX,
            AboutLogoCache.calculateTargetSizePx(screenWidthDp = 360, density = 0f)
        )
        assertEquals(
            AboutLogoCacheManager.DEFAULT_TARGET_SIZE_PX,
            AboutLogoCache.calculateTargetSizePx(screenWidthDp = 360, density = Float.NaN)
        )
        assertEquals(
            AboutLogoCacheManager.DEFAULT_TARGET_SIZE_PX,
            AboutLogoCache.calculateTargetSizePx(screenWidthDp = 360, density = Float.POSITIVE_INFINITY)
        )
    }

    @Test
    fun `calculateInSampleSize 计算保持目标清晰度的最大2的幂下采样倍数`() {
        assertEquals(1, AboutLogoCache.calculateInSampleSize(1024, 1024, 1024))
        assertEquals(2, AboutLogoCache.calculateInSampleSize(1024, 1024, 512))
        assertEquals(4, AboutLogoCache.calculateInSampleSize(1024, 1024, 256))
        assertEquals(2, AboutLogoCache.calculateInSampleSize(1024, 1024, 300))
        assertEquals(1, AboutLogoCache.calculateInSampleSize(0, 1024, 512))
        assertEquals(1, AboutLogoCache.calculateInSampleSize(1024, 1024, 0))
    }

    @Test
    fun `preload 命中后复用同尺寸和更小尺寸请求，更大尺寸请求触发升级解码`() = runBlocking {
        val manager = AboutLogoCacheManager<String>(Dispatchers.Unconfined)
        val decodeCalls = mutableListOf<Int>()

        manager.preload(512) { target ->
            decodeCalls.add(target)
            "img-$target"
        }

        val cachedSame = manager.getOrLoad(512) { target ->
            decodeCalls.add(target)
            "unexpected-$target"
        }
        val cachedSmaller = manager.getOrLoad(400) { target ->
            decodeCalls.add(target)
            "unexpected-$target"
        }

        assertEquals("img-512", cachedSame)
        assertSame(cachedSame, cachedSmaller)
        assertEquals("img-512", manager.getCached(320))
        assertNull(manager.getCached(800))

        val upgraded = manager.getOrLoad(800) { target ->
            decodeCalls.add(target)
            "img-$target"
        }
        assertEquals("img-800", upgraded)
        assertEquals(listOf(512, 800), decodeCalls)
    }

    @Test
    fun `并发请求同一目标尺寸仅触发一次解码`() = runBlocking {
        val manager = AboutLogoCacheManager<String>(Dispatchers.IO)
        val decodeCount = AtomicInteger(0)
        val firstDecodeEntered = CountDownLatch(1)
        val releaseDecode = CountDownLatch(1)

        val jobs = (0 until 4).map { index ->
            async(Dispatchers.IO) {
                if (index > 0) {
                    firstDecodeEntered.await(2, TimeUnit.SECONDS)
                }
                manager.getOrLoad(640) { target ->
                    decodeCount.incrementAndGet()
                    firstDecodeEntered.countDown()
                    releaseDecode.await(2, TimeUnit.SECONDS)
                    "concurrent-$target"
                }
            }
        }

        assertTrue(firstDecodeEntered.await(2, TimeUnit.SECONDS))
        releaseDecode.countDown()
        val results = jobs.awaitAll()

        assertEquals(1, decodeCount.get())
        results.forEach { assertEquals("concurrent-640", it) }
    }

    @Test
    fun `解码返回空或异常时不污染缓存，后续可恢复加载`() = runBlocking {
        val manager = AboutLogoCacheManager<String>(Dispatchers.Unconfined)

        val failedNull = manager.getOrLoad(512) { null }
        assertNull(failedNull)
        assertNull(manager.getCached(512))

        val failedException = manager.getOrLoad(512) { throw IllegalStateException("decode failed") }
        assertNull(failedException)
        assertNull(manager.getCached(512))

        val recovered = manager.getOrLoad(512) { "recovered-$it" }
        assertEquals("recovered-512", recovered)
    }

    @Test
    fun `解码抛出协程取消异常时继续向上传播`() = runBlocking {
        val manager = AboutLogoCacheManager<String>(Dispatchers.Unconfined)
        try {
            manager.getOrLoad(512) { throw CancellationException("cancelled") }
            fail("Expected CancellationException to propagate")
        } catch (_: CancellationException) {
            assertNull(manager.getCached(512))
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `onTrimMemory 按级别裁剪缓存且 clear 立即释放`() = runBlocking {
        val manager = AboutLogoCacheManager<String>(Dispatchers.Unconfined)

        manager.preload(512) { "img" }
        manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE)
        assertEquals("img", manager.getCached(512))

        manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW)
        assertNull(manager.getCached(512))

        manager.preload(512) { "img2" }
        manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
        assertNull(manager.getCached(512))

        manager.preload(512) { "img3" }
        manager.onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        assertNull(manager.getCached(512))

        manager.preload(512) { "img4" }
        manager.clear()
        assertNull(manager.getCached(512))
    }

    @Test
    fun `预加载协程在解码途中被外部取消时已完成解码结果仍可被等待方复用`() = runBlocking {
        val manager = AboutLogoCacheManager<String>(Dispatchers.IO)
        val decodeCalls = AtomicInteger(0)
        val decodeStarted = CountDownLatch(1)
        val allowFinish = CountDownLatch(1)
        val waiterResult = async(Dispatchers.IO) {
            assertTrue(decodeStarted.await(2, TimeUnit.SECONDS))
            manager.getOrLoad(600) { target ->
                decodeCalls.incrementAndGet()
                "second-$target"
            }
        }

        val preloadJob = launch(Dispatchers.IO) {
            manager.preload(600) { target ->
                decodeCalls.incrementAndGet()
                decodeStarted.countDown()
                allowFinish.await(2, TimeUnit.SECONDS)
                "preloaded-$target"
            }
        }

        assertTrue(decodeStarted.await(2, TimeUnit.SECONDS))
        preloadJob.cancel()
        allowFinish.countDown()
        preloadJob.join()

        assertEquals("preloaded-600", waiterResult.await())
        assertEquals("preloaded-600", manager.getCached(600))
        assertEquals(1, decodeCalls.get())
    }
}
