package com.jian.nemo.core.ui.modifier

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.UiPalette

/**
 * 弥散阴影修饰符，提供高质感多重采样环境光与漫反射柔和投影。
 *
 * @param color 阴影颜色与透明度（默认: 5% 纯黑 0x0D000000）
 * @param borderRadius 卡片圆角半径（默认: 22.dp）
 * @param blurRadius 模糊半径（默认: 16.dp）
 * @param offsetY Y轴偏移量（默认: 2.dp）
 * @param offsetX X轴偏移量（默认: 0.dp）
 * @param spread 扩展半径（默认: 0.dp）
 */
fun Modifier.softShadow(
    color: Color = UiPalette.ShadowLight,
    borderRadius: Dp = 22.dp,
    blurRadius: Dp = 16.dp,
    offsetY: Dp = 2.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.dp
): Modifier = this then SoftShadowElement(
    color = color,
    borderRadius = borderRadius,
    blurRadius = blurRadius,
    offsetY = offsetY,
    offsetX = offsetX,
    spread = spread
)

private data class SoftShadowElement(
    val color: Color,
    val borderRadius: Dp,
    val blurRadius: Dp,
    val offsetY: Dp,
    val offsetX: Dp,
    val spread: Dp
) : ModifierNodeElement<SoftShadowNode>() {
    override fun create(): SoftShadowNode = SoftShadowNode(
        color = color,
        borderRadius = borderRadius,
        blurRadius = blurRadius,
        offsetY = offsetY,
        offsetX = offsetX,
        spread = spread
    )

    override fun update(node: SoftShadowNode) {
        val changed = node.color != color ||
            node.borderRadius != borderRadius ||
            node.blurRadius != blurRadius ||
            node.offsetY != offsetY ||
            node.offsetX != offsetX ||
            node.spread != spread
        if (changed) {
            node.color = color
            node.borderRadius = borderRadius
            node.blurRadius = blurRadius
            node.offsetY = offsetY
            node.offsetX = offsetX
            node.spread = spread
            node.invalidateCache()
            node.invalidateDraw()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "softShadow"
        properties["color"] = color
        properties["borderRadius"] = borderRadius
        properties["blurRadius"] = blurRadius
        properties["offsetY"] = offsetY
        properties["offsetX"] = offsetX
        properties["spread"] = spread
    }
}

private class SoftShadowNode(
    var color: Color,
    var borderRadius: Dp,
    var blurRadius: Dp,
    var offsetY: Dp,
    var offsetX: Dp,
    var spread: Dp
) : DrawModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private val paint = Paint()
    private var cachedSize = Size.Unspecified
    private var cachedDensity = Float.NaN
    private var paramsDirty = true

    private var left = 0f
    private var top = 0f
    private var right = 0f
    private var bottom = 0f
    private var cornerRadiusPx = 0f

    fun invalidateCache() {
        paramsDirty = true
    }

    override fun ContentDrawScope.draw() {
        if (paramsDirty || size != cachedSize || density != cachedDensity) {
            val frameworkPaint = paint.asFrameworkPaint()
            val blurPx = blurRadius.toPx()
            frameworkPaint.maskFilter = if (blurPx > 0f) {
                BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
            } else {
                null
            }
            frameworkPaint.color = color.toArgb()

            val offsetXPx = offsetX.toPx()
            val offsetYPx = offsetY.toPx()
            val spreadPx = spread.toPx()
            left = offsetXPx - spreadPx
            top = offsetYPx - spreadPx
            right = size.width + offsetXPx + spreadPx
            bottom = size.height + offsetYPx + spreadPx
            cornerRadiusPx = borderRadius.toPx()

            cachedSize = size
            cachedDensity = density
            paramsDirty = false
        }

        drawIntoCanvas { canvas ->
            canvas.drawRoundRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                radiusX = cornerRadiusPx,
                radiusY = cornerRadiusPx,
                paint = paint
            )
        }
        drawContent()
    }
}

/**
 * 专为卡片打造的通用软阴影修饰符（基于 16dp 模糊半径与 5% Alpha 极简弥散效果）
 */
fun Modifier.softCardShadow(
    borderRadius: Dp = 22.dp,
    isDark: Boolean = false
): Modifier {
    val shadowColor = if (isDark) UiPalette.ShadowDark else UiPalette.ShadowLight
    return this.softShadow(
        color = shadowColor,
        borderRadius = borderRadius,
        blurRadius = 16.dp,
        offsetY = 2.dp
    )
}
