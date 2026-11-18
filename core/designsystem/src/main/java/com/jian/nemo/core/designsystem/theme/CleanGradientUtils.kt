package com.jian.nemo.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * 洁净色环渐变构建工具
 *
 * 针对多段堆叠进度条或撞色渐变，在相邻色环夹角较大时自动沿色环最短弧线插入高饱和过渡色标，
 * 避免 sRGB 直线插值穿过低饱和度泥灰区，并保持明度平滑过渡。
 */
object CleanGradientUtils {

    private const val MAX_DIRECT_HUE_STEP_DEGREES = 45f
    private const val MIN_CHROMA_FOR_HUE_BRIDGE = 0.20f

    /**
     * 为单一色块生成色环相邻（ΔH 仅约 14°）的克制同色系横向微渐变，不偏色、不发灰。
     */
    fun buildAnalogousHorizontalBrush(
        baseColor: Color,
        deltaDegrees: Float = 14f
    ): Brush {
        val adjacent = shiftHueSlightly(baseColor, deltaDegrees = deltaDegrees)
        return Brush.horizontalGradient(listOf(baseColor, adjacent))
    }

    /**
     * 为多段比例条构建无灰区过渡的横向渐变 [Brush]。
     *
     * @param segments 有序分段列表，每项为 (权重/占比, 分段主色)，会自动过滤非正权重项
     * @param fallbackColor 当无有效分段时的兜底颜色
     * @param maxBlendMargin 单个交界半过渡带宽上限（占总宽度比例）
     */
    fun buildCleanSegmentedHorizontalBrush(
        segments: List<Pair<Float, Color>>,
        fallbackColor: Color,
        maxBlendMargin: Float = 0.045f
    ): Brush {
        val valid = segments.filter { it.first > 0f }
        if (valid.isEmpty()) {
            return Brush.horizontalGradient(listOf(fallbackColor, fallbackColor))
        }
        if (valid.size == 1) {
            val single = valid[0].second
            val adjacent = shiftHueSlightly(single, deltaDegrees = 14f)
            return Brush.horizontalGradient(listOf(single, adjacent))
        }

        val stops = buildCleanSegmentedGradientStops(
            segments = valid,
            maxBlendMargin = maxBlendMargin
        )
        return if (stops.size >= 2) {
            Brush.horizontalGradient(colorStops = stops)
        } else {
            val single = valid[0].second
            Brush.horizontalGradient(listOf(single, single))
        }
    }

    /**
     * 为多段比例条计算色标数组，确保任意相邻色标不发生跨色环穿灰。
     */
    fun buildCleanSegmentedGradientStops(
        segments: List<Pair<Float, Color>>,
        maxBlendMargin: Float = 0.045f
    ): Array<Pair<Float, Color>> {
        val valid = segments.filter { it.first > 0f }
        if (valid.isEmpty()) return emptyArray()
        if (valid.size == 1) {
            val c = valid[0].second
            return arrayOf(0f to c, 1f to shiftHueSlightly(c, deltaDegrees = 14f))
        }

        val total = valid.sumOf { it.first.toDouble() }.toFloat().coerceAtLeast(0.0001f)
        val spans = valid.map { (it.first / total).coerceIn(0f, 1f) }

        val rawStops = mutableListOf<Pair<Float, Color>>()
        var cursor = 0f

        rawStops.add(0f to valid[0].second)

        for (i in 0 until valid.size - 1) {
            val spanA = spans[i]
            val spanB = spans[i + 1]
            val boundary = (cursor + spanA).coerceIn(0f, 1f)

            val halfBand = min(
                maxBlendMargin,
                min(spanA, spanB) * 0.35f
            ).coerceAtLeast(0.004f)

            val transitionStart = (boundary - halfBand).coerceAtLeast(cursor)
            val transitionEnd = (boundary + halfBand).coerceAtMost((boundary + spanB).coerceAtMost(1f))

            val colorA = valid[i].second
            val colorB = valid[i + 1].second

            rawStops.add(transitionStart to colorA)

            val bridgeColors = computeHueBridgeColors(colorA, colorB)
            if (bridgeColors.isNotEmpty() && transitionEnd > transitionStart) {
                val steps = bridgeColors.size + 1
                bridgeColors.forEachIndexed { idx, bridgeColor ->
                    val fraction = (idx + 1).toFloat() / steps.toFloat()
                    val pos = transitionStart + (transitionEnd - transitionStart) * fraction
                    rawStops.add(pos to bridgeColor)
                }
            }

            rawStops.add(transitionEnd to colorB)
            cursor = boundary
        }

        rawStops.add(1f to valid.last().second)

        // 严格单调递增去重，保证 Shader 稳定
        val sanitized = mutableListOf<Pair<Float, Color>>()
        var lastPos = -1f
        for ((pos, color) in rawStops.sortedBy { it.first }) {
            val clamped = pos.coerceIn(0f, 1f)
            if (clamped > lastPos + 0.0001f) {
                sanitized.add(clamped to color)
                lastPos = clamped
            } else if (sanitized.isNotEmpty() && clamped == 1f) {
                sanitized[sanitized.lastIndex] = 1f to color
            }
        }
        return sanitized.toTypedArray()
    }

    /**
     * 计算两个颜色之间沿色环最短弧线的高饱和过渡色列表（不含两端点）。
     */
    internal fun computeHueBridgeColors(start: Color, end: Color): List<Color> {
        val hsvA = rgbToHsv(start)
        val hsvB = rgbToHsv(end)

        if (hsvA[1] < MIN_CHROMA_FOR_HUE_BRIDGE || hsvB[1] < MIN_CHROMA_FOR_HUE_BRIDGE) {
            return emptyList()
        }

        val deltaHue = shortestSignedHueDelta(hsvA[0], hsvB[0])
        val absDelta = abs(deltaHue)
        if (absDelta <= MAX_DIRECT_HUE_STEP_DEGREES) {
            return emptyList()
        }

        val segmentsCount = ceil(absDelta / MAX_DIRECT_HUE_STEP_DEGREES).toInt().coerceIn(2, 4)
        val bridges = ArrayList<Color>(segmentsCount - 1)
        val minSatFloor = min(hsvA[1], hsvB[1]).coerceAtLeast(0.65f)

        for (step in 1 until segmentsCount) {
            val t = step.toFloat() / segmentsCount.toFloat()
            val h = ((hsvA[0] + deltaHue * t) % 360f + 360f) % 360f
            val s = (hsvA[1] + (hsvB[1] - hsvA[1]) * t).coerceAtLeast(minSatFloor)
            val v = (hsvA[2] + (hsvB[2] - hsvA[2]) * t).coerceIn(0.45f, 1f)
            val alpha = start.alpha + (end.alpha - start.alpha) * t
            bridges.add(hsvToColor(h, s, v, alpha))
        }
        return bridges
    }

    internal fun shiftHueSlightly(color: Color, deltaDegrees: Float): Color {
        val hsv = rgbToHsv(color)
        if (hsv[1] < MIN_CHROMA_FOR_HUE_BRIDGE) return color
        val nextHue = ((hsv[0] + deltaDegrees) % 360f + 360f) % 360f
        val nextVal = (hsv[2] * 1.04f).coerceAtMost(1f)
        return hsvToColor(nextHue, hsv[1], nextVal, color.alpha)
    }

    internal fun shortestSignedHueDelta(h1: Float, h2: Float): Float {
        var diff = (h2 - h1) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return diff
    }

    internal fun rgbToHsv(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        val maxVal = max(r, max(g, b))
        val minVal = min(r, min(g, b))
        val delta = maxVal - minVal

        val h = when {
            delta < 1e-5f -> 0f
            maxVal == r -> 60f * (((g - b) / delta) % 6f)
            maxVal == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }

        val s = if (maxVal < 1e-5f) 0f else delta / maxVal
        return floatArrayOf(h, s, maxVal)
    }

    internal fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float = 1f): Color {
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
            alpha = alpha.coerceIn(0f, 1f)
        )
    }
}
