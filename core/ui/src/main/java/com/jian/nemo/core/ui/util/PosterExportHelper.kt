package com.jian.nemo.core.ui.util

import android.app.Activity
import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 成就海报导出与分享工具（主线程安全）
 *
 * 线程边界：
 * 1. `GraphicsLayer.toImageBitmap()` 在主线程执行；
 * 2. PNG 无损压缩、缓存落盘、MediaStore / 磁盘 I/O 与 Bitmap 回收全部切至 IO 调度器执行；
 * 3. 系统分享选择器调起与结果返回切回主线程执行。
 */
object PosterExportHelper {

    private const val DEFAULT_SHARE_DIR = "share_posters"
    private const val DEFAULT_SHARE_FILE_NAME = "nemo_poster.png"
    private const val DEFAULT_ALBUM_NAME = "Nemo"
    private const val DEFAULT_FILE_PREFIX = "Nemo"
    private const val MIME_TYPE_PNG = "image/png"
    private const val PNG_QUALITY = 100

    /**
     * 将 [GraphicsLayer] 捕获为高清 PNG 并保存至系统相册。
     *
     * 取消协程时会清理未提交的临时资源并重新抛出 [CancellationException]。
     */
    suspend fun saveGraphicsLayerToGallery(
        context: Context,
        graphicsLayer: GraphicsLayer,
        albumName: String = DEFAULT_ALBUM_NAME,
        fileNamePrefix: String = DEFAULT_FILE_PREFIX,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ): Result<Unit> {
        val appContext = context.applicationContext
        val executor = PosterExportExecutor(
            ioDispatcher = ioDispatcher,
            mainDispatcher = mainDispatcher
        )
        return executor.saveToGallery(
            captureBitmap = {
                captureGraphicsLayerBitmap(graphicsLayer)
            },
            releaseBitmap = { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            },
            writeToGallery = { bitmap ->
                val displayName = "${fileNamePrefix}_${System.currentTimeMillis()}.png"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val gateway = AndroidMediaStoreGateway(appContext.contentResolver)
                    PosterMediaStoreCommitter.commitPng(
                        gateway = gateway,
                        displayName = displayName,
                        relativePath = "${Environment.DIRECTORY_PICTURES}/$albumName",
                        encodePng = { out -> encodeBitmapAsPng(bitmap, out) }
                    )
                } else {
                    saveViaLegacyStorage(
                        context = appContext,
                        albumName = albumName,
                        displayName = displayName,
                        encodePng = { out -> encodeBitmapAsPng(bitmap, out) }
                    )
                }
            }
        )
    }

    /**
     * 将 [GraphicsLayer] 捕获为高清 PNG 写入缓存，并通过系统分享选择器分享。
     *
     * 取消协程时会清理临时缓存并重新抛出 [CancellationException]。
     */
    suspend fun shareGraphicsLayer(
        context: Context,
        graphicsLayer: GraphicsLayer,
        chooserTitle: String,
        cacheFileName: String = DEFAULT_SHARE_FILE_NAME,
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
        mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
    ) {
        val appContext = context.applicationContext
        val executor = PosterExportExecutor(
            ioDispatcher = ioDispatcher,
            mainDispatcher = mainDispatcher
        )
        executor.sharePoster(
            captureBitmap = {
                captureGraphicsLayerBitmap(graphicsLayer)
            },
            releaseBitmap = { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            },
            writeToCache = { bitmap ->
                val shareDir = File(appContext.cacheDir, DEFAULT_SHARE_DIR)
                val file = PosterFileOps.writeAtomicPngFile(
                    targetDir = shareDir,
                    fileName = cacheFileName,
                    encodePng = { out -> encodeBitmapAsPng(bitmap, out) }
                )
                FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    file
                )
            },
            launchShare = { uri ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = MIME_TYPE_PNG
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData(chooserTitle, arrayOf(MIME_TYPE_PNG), ClipData.Item(uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val hostActivity = context.findActivity()
                val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
                    if (hostActivity == null) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                (hostActivity ?: context).startActivity(chooserIntent)
            }
        )
    }

    private suspend fun captureGraphicsLayerBitmap(graphicsLayer: GraphicsLayer): Bitmap {
        if (graphicsLayer.isReleased) {
            throw IllegalStateException("GraphicsLayer has already been released")
        }
        val size = graphicsLayer.size
        if (size.width <= 0 || size.height <= 0) {
            throw IllegalStateException("GraphicsLayer size is empty: ${size.width}x${size.height}")
        }
        return graphicsLayer.toImageBitmap().asAndroidBitmap()
    }

    private fun encodeBitmapAsPng(bitmap: Bitmap, out: OutputStream): Boolean {
        if (bitmap.isRecycled) {
            throw IllegalStateException("Cannot export recycled bitmap")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
            val softwareCopy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                ?: throw IOException("Failed to copy hardware bitmap to ARGB_8888")
            return try {
                softwareCopy.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
            } finally {
                if (!softwareCopy.isRecycled) {
                    softwareCopy.recycle()
                }
            }
        }
        return bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
    }

    @Suppress("DEPRECATION")
    private suspend fun saveViaLegacyStorage(
        context: Context,
        albumName: String,
        displayName: String,
        encodePng: (OutputStream) -> Boolean
    ) {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val albumDir = File(picturesDir, albumName)
        val uniqueFileName = PosterFileOps.resolveUniqueFileName(albumDir, displayName)
        val savedFile = PosterFileOps.writeAtomicPngFile(
            targetDir = albumDir,
            fileName = uniqueFileName,
            encodePng = encodePng
        )
        MediaScannerConnection.scanFile(
            context,
            arrayOf(savedFile.absolutePath),
            arrayOf(MIME_TYPE_PNG),
            null
        )
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

/**
 * 线程调度编排器：强制隔离主线程截图/启动分享与后台 IO 编码落盘
 */
internal class PosterExportExecutor(
    private val ioDispatcher: CoroutineDispatcher,
    private val mainDispatcher: CoroutineDispatcher
) {
    suspend fun <T> saveToGallery(
        captureBitmap: suspend () -> T,
        releaseBitmap: (T) -> Unit,
        writeToGallery: suspend (T) -> Unit
    ): Result<Unit> {
        var capturedBitmap: T? = null
        val released = AtomicBoolean(false)
        fun releaseOnce() {
            val target = capturedBitmap ?: return
            if (released.compareAndSet(false, true)) {
                releaseBitmap(target)
            }
        }
        return try {
            val bitmap = withContext(mainDispatcher) {
                captureBitmap().also { capturedBitmap = it }
            }
            withContext(ioDispatcher) {
                try {
                    currentCoroutineContext().ensureActive()
                    writeToGallery(bitmap)
                } finally {
                    releaseOnce()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            releaseOnce()
        }
    }

    suspend fun <T, U> sharePoster(
        captureBitmap: suspend () -> T,
        releaseBitmap: (T) -> Unit,
        writeToCache: suspend (T) -> U,
        launchShare: (U) -> Unit
    ) {
        var capturedBitmap: T? = null
        val released = AtomicBoolean(false)
        fun releaseOnce() {
            val target = capturedBitmap ?: return
            if (released.compareAndSet(false, true)) {
                releaseBitmap(target)
            }
        }
        val shareTarget = try {
            val bitmap = withContext(mainDispatcher) {
                captureBitmap().also { capturedBitmap = it }
            }
            withContext(ioDispatcher) {
                try {
                    currentCoroutineContext().ensureActive()
                    writeToCache(bitmap)
                } finally {
                    releaseOnce()
                }
            }
        } finally {
            releaseOnce()
        }
        withContext(mainDispatcher) {
            currentCoroutineContext().ensureActive()
            launchShare(shareTarget)
        }
    }
}

/**
 * 原子文件写入工具：先写入临时文件并校验编码结果，成功后原子替换目标文件，失败或取消时清理临时文件
 */
internal object PosterFileOps {

    private const val TEMP_PREFIX = "poster_"
    private const val TEMP_SUFFIX = ".tmp"
    private const val STALE_TEMP_AGE_MS = 60 * 60 * 1000L

    fun validateFileName(fileName: String) {
        require(fileName.isNotBlank()) { "Poster file name must not be blank" }
        require(
            !fileName.contains('/') &&
                !fileName.contains('\\') &&
                fileName != "." &&
                fileName != ".." &&
                !fileName.contains("..")
        ) {
            "Invalid poster file name: $fileName"
        }
    }

    fun resolveUniqueFileName(targetDir: File, desiredFileName: String): String {
        validateFileName(desiredFileName)
        if (!File(targetDir, desiredFileName).exists()) {
            return desiredFileName
        }
        val dotIndex = desiredFileName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) desiredFileName.substring(0, dotIndex) else desiredFileName
        val extension = if (dotIndex > 0) desiredFileName.substring(dotIndex) else ""
        var index = 1
        while (true) {
            val candidate = "${baseName}_$index$extension"
            if (!File(targetDir, candidate).exists()) {
                return candidate
            }
            index++
        }
    }

    fun cleanupStaleTempFiles(
        targetDir: File,
        nowMs: Long = System.currentTimeMillis(),
        maxAgeMs: Long = STALE_TEMP_AGE_MS
    ) {
        val children = targetDir.listFiles() ?: return
        for (child in children) {
            if (child.isFile &&
                child.name.startsWith(TEMP_PREFIX) &&
                child.name.endsWith(TEMP_SUFFIX) &&
                nowMs - child.lastModified() >= maxAgeMs
            ) {
                child.delete()
            }
        }
    }

    suspend fun writeAtomicPngFile(
        targetDir: File,
        fileName: String,
        encodePng: (OutputStream) -> Boolean
    ): File {
        validateFileName(fileName)
        if (!targetDir.exists() && !targetDir.mkdirs() && !targetDir.exists()) {
            throw IOException("Failed to create target directory: ${targetDir.absolutePath}")
        }
        cleanupStaleTempFiles(targetDir)
        val targetFile = File(targetDir, fileName)
        val tempFile = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX, targetDir)
        try {
            currentCoroutineContext().ensureActive()
            BufferedOutputStream(FileOutputStream(tempFile)).use { out ->
                val encoded = encodePng(out)
                if (!encoded) {
                    throw IOException("Bitmap PNG compression returned false")
                }
                out.flush()
            }
            currentCoroutineContext().ensureActive()
            if (!tempFile.renameTo(targetFile)) {
                if (targetFile.exists() && !targetFile.delete()) {
                    throw IOException("Failed to delete existing file: ${targetFile.absolutePath}")
                }
                if (!tempFile.renameTo(targetFile)) {
                    throw IOException("Failed to move temp file to: ${targetFile.absolutePath}")
                }
            }
            return targetFile
        } catch (t: Throwable) {
            tempFile.delete()
            throw t
        }
    }
}

internal interface MediaStoreGateway<TUri> {
    fun insertPending(displayName: String, relativePath: String): TUri?
    fun openOutputStream(uri: TUri): OutputStream?
    fun markPublished(uri: TUri): Boolean
    fun delete(uri: TUri)
}

/**
 * API 29+ MediaStore 事务提交器：使用 IS_PENDING 防止相册扫到半成品，失败或取消时自动回滚删除
 */
internal object PosterMediaStoreCommitter {

    suspend fun <TUri> commitPng(
        gateway: MediaStoreGateway<TUri>,
        displayName: String,
        relativePath: String,
        encodePng: (OutputStream) -> Boolean
    ) {
        currentCoroutineContext().ensureActive()
        val uri = gateway.insertPending(displayName, relativePath)
            ?: throw IOException("MediaStore insert returned null URI")
        try {
            currentCoroutineContext().ensureActive()
            val stream = gateway.openOutputStream(uri)
                ?: throw IOException("MediaStore openOutputStream returned null")
            BufferedOutputStream(stream).use { out ->
                val encoded = encodePng(out)
                if (!encoded) {
                    throw IOException("Bitmap PNG compression returned false")
                }
                out.flush()
            }
            currentCoroutineContext().ensureActive()
            if (!gateway.markPublished(uri)) {
                throw IOException("MediaStore failed to publish pending record")
            }
        } catch (t: Throwable) {
            runCatching { gateway.delete(uri) }
            throw t
        }
    }
}

private class AndroidMediaStoreGateway(
    private val resolver: ContentResolver
) : MediaStoreGateway<Uri> {

    override fun insertPending(displayName: String, relativePath: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    override fun openOutputStream(uri: Uri): OutputStream? {
        return resolver.openOutputStream(uri)
    }

    override fun markPublished(uri: Uri): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publishValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            return resolver.update(uri, publishValues, null, null) > 0
        }
        return true
    }

    override fun delete(uri: Uri) {
        resolver.delete(uri, null, null)
    }
}
