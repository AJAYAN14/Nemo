package com.jian.nemo.core.ui.component.progress

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI

/**
 * 自定义高保真环形进度条组件
 * 具备“呼吸缺口”与“数值生长动效”，符合 Nemo "Flat UI" 设计语言。
 *
 * @param progress 进度值 (0.0 - 1.0)
 * @param modifier 修饰符
 * @param isLoading 是否处于加载状态（用于触发归零重填转场）
 * @param color 进度条颜色
 * @param trackColor 底轨颜色
 * @param strokeWidth 线宽
 */
@Composable
fun NemoCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    color: Color = Color.Black,
    trackColor: Color = Color.LightGray.copy(alpha = 0.3f),
    strokeWidth: Dp = 12.dp
) {
    // 进度值从 0 平滑生长至目标值 (Cubic 曲线)
    val animatedProgress by animateFloatAsState(
        targetValue = if (isLoading) 0f else progress,
        animationSpec = tween(
            durationMillis = 800,
            easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1.0f)
        ),
        label = "progress"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val strokeWidthPx = strokeWidth.toPx()
        val radius = (size.minDimension - strokeWidthPx) / 2
        
        // 显式构造 Rect
        val rect = Rect(
            left = center.x - radius,
            top = center.y - radius,
            right = center.x + radius,
            bottom = center.y + radius
        )

        val gapAngleDegrees = if (radius > 0) (1.5f * strokeWidthPx / radius) * (180f / PI.toFloat()) else 0f
        val progressSweep = animatedProgress * 360f

        // 1. 绘制进度条 (Progress)
        if (progressSweep > 0.1f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = progressSweep,
                useCenter = false,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
            )
        }

        // 2. 绘制底轨 (Track) - 包含动态避让缺口逻辑
        val trackSweepAngle = if (progressSweep > 0.1f) {
            (360f - progressSweep - 2 * gapAngleDegrees).coerceAtLeast(0f)
        } else {
            360f
        }

        if (trackSweepAngle > 1f) {
            val trackStartAngle = if (progressSweep > 0.1f) {
                -90f + progressSweep + gapAngleDegrees
            } else {
                -90f
            }

            drawArc(
                color = trackColor,
                startAngle = trackStartAngle,
                sweepAngle = trackSweepAngle,
                useCenter = false,
                topLeft = Offset(rect.left, rect.top),
                size = Size(rect.width, rect.height),
                style = Stroke(
                    width = strokeWidthPx, 
                    cap = if (progressSweep > 0.1f) StrokeCap.Round else StrokeCap.Butt
                )
            )
        }
    }
}
