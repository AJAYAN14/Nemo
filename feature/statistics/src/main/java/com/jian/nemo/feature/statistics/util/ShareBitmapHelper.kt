package com.jian.nemo.feature.statistics.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.media.MediaScannerConnection
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 海报分享与保存工具
 *
 * 提供两个核心功能：
 * 1. [shareImage] — 将 GraphicsLayer 渲染为 PNG 并调起系统分享选择器
 * 2. [saveToPictures] — 将 GraphicsLayer 渲染为 PNG 并保存至系统相册
 */
object ShareBitmapHelper {

    private const val SHARE_DIR = "share_posters"
    private const val SHARE_FILE_NAME = "nemo_achievement.png"
    private const val ALBUM_DIR = "Nemo"

    /**
     * 将 GraphicsLayer 渲染为图片并调起系统分享选择器
     */
    suspend fun shareImage(context: Context, graphicsLayer: GraphicsLayer) {
        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
        val file = writeBitmapToCache(context, bitmap)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享学习成就"))
    }

    /**
     * 将 GraphicsLayer 渲染为图片并保存至系统相册
     *
     * @return true 保存成功，false 保存失败
     */
    suspend fun saveToPictures(context: Context, graphicsLayer: GraphicsLayer): Boolean {
        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, bitmap)
            } else {
                saveViaLegacy(context, bitmap)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 写入缓存目录（用于分享 Intent）
     */
    private fun writeBitmapToCache(context: Context, bitmap: Bitmap): File {
        val dir = File(context.cacheDir, SHARE_DIR)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, SHARE_FILE_NAME)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    /**
     * API 29+ (Android 10+)：通过 MediaStore 写入系统相册
     */
    private fun saveViaMediaStore(context: Context, bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "Nemo_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/$ALBUM_DIR"
            )
        }
        val resolver = context.contentResolver
        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } ?: throw IllegalStateException("MediaStore insert returned null URI")
    }

    /**
     * API 24-28：通过传统文件路径写入 + MediaScanner 刷新
     */
    @Suppress("DEPRECATION")
    private fun saveViaLegacy(context: Context, bitmap: Bitmap) {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val albumDir = File(picturesDir, ALBUM_DIR)
        if (!albumDir.exists()) albumDir.mkdirs()
        val file = File(albumDir, "Nemo_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("image/png"),
            null
        )
    }
}
