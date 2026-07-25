package com.jian.nemo.core.ui.component.speaker

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.ui.component.liquid.LiquidButton

/**
 * 朗读按钮组件，支持播放时的声纹动画与液态交互效果
 *
 * @param isPlaying 是否正在播放（显示声纹动画）
 * @param onClick 点击回调
 * @param modifier Modifier
 * @param tint 图标/动画颜色
 * @param size 按钮尺寸
 * @param backgroundColor 背景颜色（可选）
 */
@Composable
fun SpeakerButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 44.dp,
    backgroundColor: Color = Color.Transparent
) {
    LiquidButton(
        onClick = onClick,
        backgroundColor = backgroundColor,
        shape = CircleShape,
        elevation = 0.dp,
        modifier = modifier.requiredSize(size)
    ) {
        if (isPlaying) {
            // 播放状态：显示声纹动画
            SoundWaveAnimation(
                tint = tint,
                size = size * 0.55f
            )
        } else {
            // 默认状态：静态喇叭图标
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "朗读",
                tint = tint,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

/**
 * 声纹波形动画 - 3条竖条交替跳动
 */
@Composable
private fun SoundWaveAnimation(
    tint: Color,
    size: Dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "soundWave")

    // 三条竖条的动画，使用不同的相位偏移
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Canvas(modifier = Modifier.size(size)) {
        val canvasWidth = this.size.width
        val canvasHeight = this.size.height

        val barWidth = canvasWidth / 5f
        val gap = barWidth * 0.5f
        val totalWidth = 3 * barWidth + 2 * gap
        val startX = (canvasWidth - totalWidth) / 2f

        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        // 绘制三条竖条
        val heights = listOf(bar1Height, bar2Height, bar3Height)

        heights.forEachIndexed { index, heightFraction ->
            val barHeight = canvasHeight * heightFraction
            val x = startX + index * (barWidth + gap)
            val y = (canvasHeight - barHeight) / 2f

            drawRoundRect(
                color = tint,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
