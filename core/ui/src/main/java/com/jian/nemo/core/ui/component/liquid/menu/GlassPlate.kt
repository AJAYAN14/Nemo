package com.jian.nemo.core.ui.component.liquid.menu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.ui.component.liquid.menu.motion.GlassSpringSpec

/**
 * 高光药丸滑动阻尼弹簧与淡入淡出弹簧规范
 */
internal val PlateSlide = GlassSpringSpec(dampingRatio = 0.85f, response = 0.13f)
internal val PlateFade = GlassSpringSpec(dampingRatio = 1.0f, response = 0.14f)

const val PlateInsetDpValue = 8f
const val ScrubSlackDpValue = 24f

private const val PlateDark = 0.155f
private const val PlateShine = 0.03f
private const val PlateLight = 0.09f

/**
 * 液态菜单条目磁吸高光药丸组件
 */
@Composable
fun GlassPlate(
    motionState: LiquidMenuMotionState,
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    plateInset: Dp = 8.dp
) {
    val alpha = motionState.plateAlphaVal
    if (alpha <= 0.001f) return

    val top = motionState.plateTopPx
    val height = motionState.plateHeightPx
    val lightX = motionState.plateLightXPx

    Canvas(modifier = modifier.fillMaxSize()) {
        val insetPx = plateInset.toPx()
        val left = insetPx
        val right = size.width - insetPx
        val bottom = top + height

        if (bottom <= top || right <= left) return@Canvas

        val pillRadius = height / 2f
        val pillWidth = right - left
        val centerX = lightX.coerceIn(left, right)
        val centerY = top + height / 2f
        val gradientRadius = pillWidth * 0.62f

        val (centerColor, edgeColor) = if (isDark) {
            val edgeAlpha = (PlateDark * alpha).coerceIn(0f, 1f)
            val centerAlpha = ((PlateDark + PlateShine) * alpha).coerceIn(0f, 1f)
            val tone = 235
            Color(tone, tone, 245, (centerAlpha * 255f).toInt()) to
                Color(tone, tone, 245, (edgeAlpha * 255f).toInt())
        } else {
            val edgeAlpha = (PlateLight * alpha).coerceIn(0f, 1f)
            val centerAlpha = ((PlateLight + 0.025f) * alpha).coerceIn(0f, 1f)
            val tone = 60
            Color(tone, tone, 67, (centerAlpha * 255f).toInt()) to
                Color(tone, tone, 67, (edgeAlpha * 255f).toInt())
        }

        val fillBrush = Brush.radialGradient(
            colors = listOf(centerColor, edgeColor),
            center = Offset(centerX, centerY),
            radius = gradientRadius.coerceAtLeast(1f)
        )

        drawRoundRect(
            brush = fillBrush,
            topLeft = Offset(left, top),
            size = Size(pillWidth, height),
            cornerRadius = CornerRadius(pillRadius, pillRadius)
        )

        val strokeWidth = 0.8.dp.toPx()
        val halfStroke = strokeWidth / 2f
        val rimBrush = Brush.verticalGradient(
            colors = if (isDark) {
                listOf(
                    Color.White.copy(alpha = (0.35f * alpha).coerceIn(0f, 1f)),
                    Color.White.copy(alpha = (0.15f * alpha).coerceIn(0f, 1f)),
                    Color.White.copy(alpha = (0.04f * alpha).coerceIn(0f, 1f))
                )
            } else {
                listOf(
                    Color.White.copy(alpha = (0.75f * alpha).coerceIn(0f, 1f)),
                    Color.White.copy(alpha = (0.30f * alpha).coerceIn(0f, 1f)),
                    Color.Black.copy(alpha = (0.05f * alpha).coerceIn(0f, 1f))
                )
            },
            startY = top,
            endY = bottom
        )

        drawRoundRect(
            brush = rimBrush,
            topLeft = Offset(left + halfStroke, top + halfStroke),
            size = Size((pillWidth - strokeWidth).coerceAtLeast(0f), (height - strokeWidth).coerceAtLeast(0f)),
            cornerRadius = CornerRadius((pillRadius - halfStroke).coerceAtLeast(0f), (pillRadius - halfStroke).coerceAtLeast(0f)),
            style = Stroke(width = strokeWidth)
        )
    }
}
