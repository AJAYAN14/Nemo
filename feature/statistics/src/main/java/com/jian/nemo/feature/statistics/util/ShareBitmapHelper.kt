package com.jian.nemo.feature.statistics.util

import android.content.Context
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.jian.nemo.core.ui.util.PosterExportHelper
import com.jian.nemo.feature.statistics.R

/**
 * 海报分享与保存工具（主线程安全）
 *
 * 提供两个核心功能：
 * 1. [shareImage] — 将 GraphicsLayer 渲染为 PNG 并调起系统分享选择器
 * 2. [saveToPictures] — 将 GraphicsLayer 渲染为 PNG 并保存至系统相册
 */
object ShareBitmapHelper {

    private const val SHARE_FILE_NAME = "nemo_achievement.png"
    private const val ALBUM_DIR = "Nemo"

    /**
     * 将 GraphicsLayer 渲染为图片并调起系统分享选择器（主线程捕获，IO 线程压缩写盘，主线程分享）
     */
    suspend fun shareImage(context: Context, graphicsLayer: GraphicsLayer) {
        PosterExportHelper.shareGraphicsLayer(
            context = context,
            graphicsLayer = graphicsLayer,
            chooserTitle = context.getString(R.string.stats_share_dialog_chooser_title),
            cacheFileName = SHARE_FILE_NAME
        )
    }

    /**
     * 将 GraphicsLayer 渲染为图片并保存至系统相册（主线程捕获，IO 线程压缩与 MediaStore 写入）
     *
     * @return true 保存成功，false 保存失败
     */
    suspend fun saveToPictures(context: Context, graphicsLayer: GraphicsLayer): Boolean {
        return PosterExportHelper.saveGraphicsLayerToGallery(
            context = context,
            graphicsLayer = graphicsLayer,
            albumName = ALBUM_DIR
        ).isSuccess
    }
}
