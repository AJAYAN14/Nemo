package com.jian.nemo.core.ui.modifier

import android.graphics.BlurMaskFilter
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 极简高质感弥散阴影修饰符，效果完全等同于 Flutter/Figma 中的 BoxShadow。
 *
 * @param color 阴影颜色与透明度（默认: 5% 纯黑 0x0D000000）
 * @param borderRadius 卡片圆角半径（默认: 22.dp）
 * @param blurRadius 模糊半径（默认: 16.dp）
 * @param offsetY Y轴偏移量（默认: 2.dp）
 * @param offsetX X轴偏移量（默认: 0.dp）
 * @param spread 扩展半径（默认: 0.dp）
 */
fun Modifier.softShadow(
    color: Color = Color(0x0D000000),
    borderRadius: Dp = 22.dp,
    blurRadius: Dp = 16.dp,
    offsetY: Dp = 2.dp,
    offsetX: Dp = 0.dp,
    spread: Dp = 0.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()

        val blurPx = blurRadius.toPx()
        if (blurPx > 0f) {
            frameworkPaint.maskFilter = BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
        }
        frameworkPaint.color = color.toArgb()

        val left = offsetX.toPx() - spread.toPx()
        val top = offsetY.toPx() - spread.toPx()
        val right = size.width + offsetX.toPx() + spread.toPx()
        val bottom = size.height + offsetY.toPx() + spread.toPx()
        val cornerRadiusPx = borderRadius.toPx()

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
}

/**
 * 专为卡片打造的通用软阴影修饰符（基于 16dp 模糊半径与 5% Alpha 极简弥散效果）
 */
fun Modifier.softCardShadow(
    borderRadius: Dp = 22.dp,
    isDark: Boolean = false
): Modifier {
    val shadowColor = if (isDark) Color(0x26000000) else Color(0x0D000000)
    return this.softShadow(
        color = shadowColor,
        borderRadius = borderRadius,
        blurRadius = 16.dp,
        offsetY = 2.dp
    )
}
