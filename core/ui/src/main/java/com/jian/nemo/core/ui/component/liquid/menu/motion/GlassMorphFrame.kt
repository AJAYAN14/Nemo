package com.jian.nemo.core.ui.component.liquid.menu.motion

import android.graphics.RectF

/**
 * 记录单帧形变几何与渲染参数的状态容器
 */
internal class GlassMorphFrame {
    val bounds: RectF = RectF()
    var radiusX: Float = 0f
    var radiusY: Float = 0f
    var contentProgress: Float = 0f
    var rowsOpacity: Float = 0f
    val sourceLobe: RectF = RectF()
    var sourceRadius: Float = 0f
    var smoothing: Float = 0f
    var sourceOpacity: Float = 1f
    var sourceBlur: Float = 0f
    var sourceZoom: Float = 1f
    var contentBlur: Float = 0f
    var contentZoom: Float = 1f
    var portalBlur: Float = 0f
    var lensHeight: Float = 0f
    var lensAmount: Float = 0f
}
