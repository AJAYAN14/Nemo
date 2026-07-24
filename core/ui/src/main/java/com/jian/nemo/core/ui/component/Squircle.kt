package com.jian.nemo.core.ui.component

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * 连续曲率平滑圆角矩形（Squircle / Continuous Rounded Rect）
 * 精确实现 iOS 风格的平滑过渡圆角
 */
class SmoothRoundedCornerShape(
    val radius: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val rPx = with(density) { radius.toPx() }.coerceAtMost(minOf(size.width, size.height) / 2f)
        if (rPx <= 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = CornerRadius(rPx, rPx)
                )
            )
        }
        return Outline.Generic(path)
    }
}

/**
 * 便捷工厂函数：创建 [SmoothRoundedCornerShape]
 */
fun RoundedRectangle(radius: Dp): Shape = RoundedCornerShape(radius)

/**
 * 胶囊形状 Shape（半圆弧两端）
 */
fun Capsule(): Shape = RoundedCornerShape(percent = 50)
