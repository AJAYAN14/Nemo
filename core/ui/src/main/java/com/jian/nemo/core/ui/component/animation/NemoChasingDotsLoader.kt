package com.jian.nemo.core.ui.component.animation

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.FlatUiColors

/**
 * 旋转双星 (Chasing Dots) 加载动画组件
 *
 * @param modifier 修饰符
 * @param size 容器大小，默认 45.dp
 * @param duration 旋转一周的时间（毫秒），默认 2000ms
 */
@Composable
fun NemoChasingDotsLoader(
    modifier: Modifier = Modifier,
    size: Dp = 45.dp,
    duration: Int = 2000
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ChasingDots")

    // 1. 整体旋转动画 (2s linear)
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Box(
        modifier = modifier
            .size(size)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        // 圆点 1
        ChasingDot(
            infiniteTransition = infiniteTransition,
            delayMillis = 0,
            duration = duration,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 圆点 2
        ChasingDot(
            infiniteTransition = infiniteTransition,
            delayMillis = duration / 2, // 延迟半个周期 (1s)
            duration = duration,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ChasingDot(
    infiniteTransition: InfiniteTransition,
    delayMillis: Int,
    duration: Int,
    modifier: Modifier = Modifier
) {
    // 2. 缩放动画 (0 -> 1 -> 0, 2s ease-in-out)
    // 使用 keyframes 模拟 CSS 的 0%, 100% { scale(0) } 50% { scale(1) }
    val scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = duration
                0f at 0 with FastOutSlowInEasing
                1f at duration / 2 with FastOutSlowInEasing
                0f at duration
            },
            initialStartOffset = StartOffset(delayMillis)
        ),
        label = "DotScale"
    )

    // 3. 颜色流转动画 (6s linear 循环)
    val colors = listOf(
        FlatUiColors.Yellow,
        FlatUiColors.Orange,
        FlatUiColors.Red,
        FlatUiColors.Purple,
        FlatUiColors.Blue,
        FlatUiColors.Green
    )
    
    val colorShiftDuration = duration * 3 // 6s

    val color by infiniteTransition.animateColor(
        initialValue = colors[0],
        targetValue = colors[0],
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = colorShiftDuration
                colors.forEachIndexed { index, c ->
                    c at (colorShiftDuration / colors.size * index) with LinearEasing
                }
                colors[0] at colorShiftDuration
            },
            initialStartOffset = StartOffset(delayMillis)
        ),
        label = "ColorShift"
    )

    Box(
        modifier = modifier
            .fillMaxSize(0.6f) // 圆点大小为容器的 60%
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(color)
    )
}
