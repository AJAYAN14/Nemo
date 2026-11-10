package com.jian.nemo.feature.learning.presentation.components.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.IosColors

/**
 * 400米跑道风格胶囊进度指示器，进度沿赛道轮廓线顺时针转圈推进
 */
@Composable
fun TrackProgressIndicator(
    progress: Float,
    queueNewCount: Int,
    queueLearningCount: Int,
    queueReviewCount: Int,
    queueRelearnCount: Int,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "track_progress_anim"
    )

    val primaryThemeColor = MaterialTheme.colorScheme.primary
    val trackLineColor = primaryThemeColor.copy(alpha = if (isDarkTheme) 0.22f else 0.15f)
    val progressLineColor = primaryThemeColor

    val newColor = if (isDarkTheme) IosColors.BlueDark else IosColors.Blue
    val learningColor = if (isDarkTheme) IosColors.CyanDark else IosColors.Cyan
    val reviewColor = if (isDarkTheme) IosColors.GreenDark else IosColors.Green
    val relearnColor = if (isDarkTheme) IosColors.OrangeDark else IosColors.Orange

    val pathMeasure = remember { PathMeasure() }
    val fullPath = remember { Path() }
    val progressPath = remember { Path() }

    Box(
        modifier = modifier
            .width(130.dp)
            .height(34.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Canvas 沿 400 米跑道轮廓线顺时针绘制赛道底轨与进度线
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 2.dp.toPx()
            val inset = strokeWidthPx / 2f
            val w = size.width - strokeWidthPx
            val h = size.height - strokeWidthPx
            val r = h / 2f

            fullPath.reset()
            // 顺时针构建 400 米封闭胶囊赛道：从顶部直道中点出发 -> 右直道 -> 右半圆弯道 -> 底部直道 -> 左半圆弯道 -> 回到起点
            val midX = inset + w / 2f
            val rightStraightX = inset + w - r
            val leftStraightX = inset + r
            val topY = inset
            val bottomY = inset + h

            fullPath.moveTo(midX, topY)
            // 顶部前半段向右
            fullPath.lineTo(rightStraightX, topY)
            // 右半圆弯道（顺时针从 -90° 旋转 180° 到 90°）
            fullPath.arcTo(
                rect = Rect(rightStraightX - r, topY, rightStraightX + r, bottomY),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            // 底部直道向左
            fullPath.lineTo(leftStraightX, bottomY)
            // 左半圆弯道（顺时针从 90° 旋转 180° 到 270°）
            fullPath.arcTo(
                rect = Rect(leftStraightX - r, topY, leftStraightX + r, bottomY),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            // 顶部后半段回到起点闭合
            fullPath.lineTo(midX, topY)
            fullPath.close()

            // 1. 绘制完整跑道底轨线（纤细优雅）
            drawPath(
                path = fullPath,
                color = trackLineColor,
                style = Stroke(width = strokeWidthPx)
            )

            // 2. 绘制沿赛道转圈推进的活力进度线（带圆头）
            if (animatedProgress > 0.001f) {
                pathMeasure.setPath(fullPath, false)
                val totalLength = pathMeasure.length
                progressPath.reset()
                pathMeasure.getSegment(
                    startDistance = 0f,
                    stopDistance = totalLength * animatedProgress,
                    destination = progressPath,
                    startWithMoveTo = true
                )
                drawPath(
                    path = progressPath,
                    color = progressLineColor,
                    style = Stroke(
                        width = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // 内场无大背景色，干干净净居中排布四态数字
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 新学
            Text(
                text = "$queueNewCount",
                color = newColor.copy(alpha = if (queueNewCount > 0) 1f else 0.25f),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            // 在学
            Text(
                text = "$queueLearningCount",
                color = learningColor.copy(alpha = if (queueLearningCount > 0) 1f else 0.25f),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            // 复习
            Text(
                text = "$queueReviewCount",
                color = reviewColor.copy(alpha = if (queueReviewCount > 0) 1f else 0.25f),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            // 重学
            Text(
                text = "$queueRelearnCount",
                color = relearnColor.copy(alpha = if (queueRelearnCount > 0) 1f else 0.25f),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
