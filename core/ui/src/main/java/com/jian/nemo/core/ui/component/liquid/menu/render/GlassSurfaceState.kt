package com.jian.nemo.core.ui.component.liquid.menu.render

import android.graphics.Color
import android.graphics.RectF

/**
 * 玻璃材质渲染配置项
 */
internal data class GlassMaterialConfig(
    val blur: Float = 16f,
    val morphBlur: Float = 1f,
    val tintColor: Color? = null,
    val tintOpacity: Float? = null,
    val saturation: Float = 1f,
    val refraction: Float = 1f,
    val rim: Float = 1f,
    val shine: Float = 1f,
    val highlightOpacity: Float = 1f,
    val jelly: Float = 1f,
    val speed: Float = 1f
)

/**
 * 触发按钮材质与形变选项
 */
internal data class GlassTriggerConfig(
    val blur: Float = 1f,
    val tintColor: Color? = null,
    val tintOpacity: Float? = null,
    val saturation: Float = 1f,
    val refraction: Float = 1f,
    val rim: Float = 1f,
    val shine: Float = 1f,
    val jelly: Float = 1f
)

/**
 * 传递给底层 GPU Shader 的即时表面渲染状态
 */
internal class GlassSurfaceState {
    val incoming: RectF = RectF()
    val captureBounds: RectF = RectF()
    var radiusX: Float = 0f
    var radiusY: Float = 0f
    val source: RectF = RectF()
    var hasSource: Boolean = false
    var sourceRadius: Float = 0f
    var smoothing: Float = 0f
    var lensHeight: Float = 0f
    var lensAmount: Float = 0f
    var materialProgress: Float = 0f
    var surfaceOpacity: Float = 1f
    var dark: Boolean = true
    var pressX: Float = 0f
    var pressY: Float = 0f
    var pressRadius: Float = 1f
    var poolGain: Float = 0f
    var pressLift: Float = 0f
    var rimLift: Float = 0f
    var config: GlassMaterialConfig = GlassMaterialConfig()
    var trigger: GlassTriggerConfig = GlassTriggerConfig()
}
