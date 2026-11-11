package com.jian.nemo.feature.settings

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.jian.nemo.core.designsystem.R as DesignR
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

/**
 * 关于界面品牌 Logo 后台预解码与可控尺寸内存缓存管理核心。
 */
internal class AboutLogoCacheManager<T : Any>(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private data class CachedEntry<T : Any>(
        val targetPx: Int,
        val image: T
    )

    private val mutex = Mutex()

    @Volatile
    private var cachedEntry: CachedEntry<T>? = null

    fun getCached(targetPx: Int): T? {
        val safeTarget = sanitizeTargetPx(targetPx)
        val entry = cachedEntry ?: return null
        return if (entry.targetPx >= safeTarget) entry.image else null
    }

    suspend fun preload(targetPx: Int, decode: (Int) -> T?) {
        if (getCached(targetPx) != null) return
        getOrLoad(targetPx, decode)
    }

    suspend fun getOrLoad(targetPx: Int, decode: (Int) -> T?): T? {
        val safeTarget = sanitizeTargetPx(targetPx)
        getCached(safeTarget)?.let { return it }

        return mutex.withLock {
            getCached(safeTarget)?.let { return@withLock it }
            currentCoroutineContext().ensureActive()
            val decoded = withContext(ioDispatcher + NonCancellable) {
                try {
                    decode(safeTarget)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    null
                }
            }
            if (decoded != null) {
                val existing = cachedEntry
                if (existing == null || existing.targetPx < safeTarget) {
                    cachedEntry = CachedEntry(targetPx = safeTarget, image = decoded)
                }
            }
            // 解码成果已安全写入缓存后再响应外部取消，避免等待中的界面重复解码
            currentCoroutineContext().ensureActive()
            decoded
        }
    }

    fun clear() {
        cachedEntry = null
    }

    @Suppress("DEPRECATION")
    fun onTrimMemory(level: Int) {
        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
        ) {
            clear()
        }
    }

    internal fun sanitizeTargetPx(targetPx: Int): Int {
        return if (targetPx <= 0) {
            DEFAULT_TARGET_SIZE_PX
        } else {
            targetPx.coerceIn(MIN_TARGET_SIZE_PX, MAX_TARGET_SIZE_PX)
        }
    }

    companion object {
        const val MIN_TARGET_SIZE_PX = 256
        const val DEFAULT_TARGET_SIZE_PX = 512
        const val MAX_TARGET_SIZE_PX = 1024
        const val CARD_HORIZONTAL_MARGIN_DP = 32
        const val LOGO_HORIZONTAL_PADDING_DP = 56
    }
}

/**
 * 关于界面品牌 Logo 的应用级预解码缓存入口。
 */
object AboutLogoCache {
    private val manager = AboutLogoCacheManager<ImageBitmap>()
    private val callbacksRegistered = AtomicBoolean(false)

    /**
     * 按屏幕宽度与密度计算卡片内 Logo 实际最大渲染边长（像素），并钳制在可控区间内。
     */
    fun calculateTargetSizePx(screenWidthDp: Int, density: Float): Int {
        if (screenWidthDp <= 0 || !density.isFinite() || density <= 0f) {
            return AboutLogoCacheManager.DEFAULT_TARGET_SIZE_PX
        }
        val contentWidthDp = (
            screenWidthDp -
                AboutLogoCacheManager.CARD_HORIZONTAL_MARGIN_DP -
                AboutLogoCacheManager.LOGO_HORIZONTAL_PADDING_DP
            ).coerceAtLeast(1)
        return (contentWidthDp * density)
            .roundToInt()
            .coerceIn(
                AboutLogoCacheManager.MIN_TARGET_SIZE_PX,
                AboutLogoCacheManager.MAX_TARGET_SIZE_PX
            )
    }

    /**
     * 计算在不低于目标清晰度前提下的最大 2 的幂下采样倍数。
     */
    internal fun calculateInSampleSize(rawWidth: Int, rawHeight: Int, reqMaxPx: Int): Int {
        if (rawWidth <= 0 || rawHeight <= 0 || reqMaxPx <= 0) return 1
        var inSampleSize = 1
        val maxRaw = maxOf(rawWidth, rawHeight)
        while (maxRaw / (inSampleSize * 2) >= reqMaxPx) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    fun getCached(targetPx: Int): ImageBitmap? = manager.getCached(targetPx)

    suspend fun preload(context: Context, targetPx: Int) {
        val appContext = context.applicationContext
        ensureTrimCallbacksRegistered(appContext)
        manager.preload(targetPx) { safeTargetPx ->
            decodeAboutLogo(appContext, safeTargetPx)
        }
    }

    suspend fun getOrLoad(context: Context, targetPx: Int): ImageBitmap? {
        val appContext = context.applicationContext
        ensureTrimCallbacksRegistered(appContext)
        return manager.getOrLoad(targetPx) { safeTargetPx ->
            decodeAboutLogo(appContext, safeTargetPx)
        }
    }

    fun clear() {
        manager.clear()
    }

    private fun ensureTrimCallbacksRegistered(appContext: Context) {
        if (!callbacksRegistered.compareAndSet(false, true)) return
        appContext.registerComponentCallbacks(
            object : ComponentCallbacks2 {
                override fun onTrimMemory(level: Int) {
                    manager.onTrimMemory(level)
                }

                override fun onConfigurationChanged(newConfig: Configuration) = Unit

                @Deprecated("Deprecated in Java")
                override fun onLowMemory() {
                    manager.clear()
                }
            }
        )
    }

    private fun decodeAboutLogo(appContext: Context, targetMaxPx: Int): ImageBitmap? {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(appContext.resources, DesignR.drawable.img_about_butterfly_logo)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // 使用软件位图避免硬件纹理跨渲染层限制，并在系统内存紧张时允许安全回收
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val rawWidth = info.size.width
                val rawHeight = info.size.height
                val maxRaw = maxOf(rawWidth, rawHeight)
                if (maxRaw > targetMaxPx && targetMaxPx > 0) {
                    val scale = targetMaxPx.toFloat() / maxRaw.toFloat()
                    val dstWidth = (rawWidth * scale).roundToInt().coerceAtLeast(1)
                    val dstHeight = (rawHeight * scale).roundToInt().coerceAtLeast(1)
                    decoder.setTargetSize(dstWidth, dstHeight)
                }
            }
        } else {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(
                appContext.resources,
                DesignR.drawable.img_about_butterfly_logo,
                boundsOptions
            )
            val sampleSize = calculateInSampleSize(
                rawWidth = boundsOptions.outWidth,
                rawHeight = boundsOptions.outHeight,
                reqMaxPx = targetMaxPx
            )
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inScaled = false
            }
            val sampled = BitmapFactory.decodeResource(
                appContext.resources,
                DesignR.drawable.img_about_butterfly_logo,
                decodeOptions
            ) ?: return null
            val maxSampled = maxOf(sampled.width, sampled.height)
            if (maxSampled > targetMaxPx && targetMaxPx > 0) {
                val scale = targetMaxPx.toFloat() / maxSampled.toFloat()
                val dstWidth = (sampled.width * scale).roundToInt().coerceAtLeast(1)
                val dstHeight = (sampled.height * scale).roundToInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(sampled, dstWidth, dstHeight, true)
                if (scaled !== sampled) {
                    sampled.recycle()
                }
                scaled
            } else {
                sampled
            }
        }
        bitmap.prepareToDraw()
        return bitmap.asImageBitmap()
    }
}
