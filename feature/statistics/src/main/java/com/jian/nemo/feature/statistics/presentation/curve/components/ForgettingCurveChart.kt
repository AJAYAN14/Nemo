package com.jian.nemo.feature.statistics.presentation.curve.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.ChartColors
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.designsystem.theme.NemoTheme
import com.jian.nemo.feature.statistics.presentation.curve.CurvePoint
import com.jian.nemo.feature.statistics.presentation.curve.ForgettingCurveData
import kotlin.math.abs

/**
 * 遗忘曲线对比折线图组件 (Pro Max 高级增强版)
 *
 * 核心优化：
 * - 引入 Animatable 实现平滑形变和线型生长效果
 * - 在用户曲线上方添加平滑的半透明渐变阴影填充
 * - 增加手势侦听，手指滑过时绘制 Crosshair（十字瞄准线）和 Tooltip（高亮信息面板）
 */
@Composable
fun ForgettingCurveChart(
    data: ForgettingCurveData,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 颜色方案
    val gridColor = if (isDark) NemoNeutrals.Gray700.copy(alpha = 0.4f) else NemoNeutrals.Gray200
    val axisLabelColor = if (isDark) NemoNeutrals.Gray400 else NemoNeutrals.Gray500
    val standardLineColor = ChartColors.FreshGreen
    val userLineColor = ChartColors.FreshOrange

    // 线条宽度
    val standardLineWidth = with(density) { 2.dp.toPx() }
    val userLineWidth = with(density) { 3.dp.toPx() }
    val userDotRadius = with(density) { 4.dp.toPx() }
    val gridLineWidth = with(density) { 0.5.dp.toPx() }

    val axisTextStyle = TextStyle(
        color = axisLabelColor,
        fontSize = 10.sp,
        textAlign = TextAlign.Center
    )

    val maxDayIndex = maxOf(
        data.standardCurve.maxOfOrNull { it.dayIndex } ?: 0,
        data.userCurve.maxOfOrNull { it.dayIndex } ?: 0
    )

    val yLabelWidth = remember(textMeasurer) {
        textMeasurer.measure("100%", axisTextStyle).size.width.toFloat()
    }

    // ========== 动画与手势状态 ==========
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    var touchX by remember { mutableStateOf<Float?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(600.dp)
            .pointerInput(data) {
                detectDragGestures(
                    onDragStart = { offset -> touchX = offset.x },
                    onDrag = { change, _ -> touchX = change.position.x },
                    onDragEnd = { touchX = null },
                    onDragCancel = { touchX = null }
                )
            }
            .pointerInput(data, "tap") {
                detectTapGestures(
                    onPress = { offset ->
                        touchX = offset.x
                        tryAwaitRelease()
                        touchX = null
                    }
                )
            }
    ) {
        val leftPadding = yLabelWidth + with(density) { 8.dp.toPx() }
        val rightPadding = with(density) { 16.dp.toPx() }
        val topPadding = with(density) { 24.dp.toPx() } // 给 Tooltip 留出空间
        val bottomPadding = with(density) { 40.dp.toPx() }

        val chartLeft = leftPadding
        val chartRight = size.width - rightPadding
        val chartTop = topPadding
        val chartBottom = size.height - bottomPadding
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        // ========== 绘制背景网格线 ==========
        val ySteps = 10
        for (i in 0..ySteps) {
            val yRatio = i.toFloat() / ySteps
            val y = chartBottom - yRatio * chartHeight
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = gridLineWidth
            )
            val labelText = "${(i * 10)}%"
            val labelResult = textMeasurer.measure(labelText, axisTextStyle)
            drawText(
                textLayoutResult = labelResult,
                topLeft = Offset(
                    x = chartLeft - labelResult.size.width - with(density) { 4.dp.toPx() },
                    y = y - labelResult.size.height / 2f
                )
            )
        }

        val xLabelStep = when {
            maxDayIndex <= 7 -> 1
            maxDayIndex <= 30 -> 5
            maxDayIndex <= 90 -> 15
            else -> 60
        }
        val maxX = maxDayIndex.coerceAtLeast(1)

        for (dayValue in 0..maxDayIndex step xLabelStep) {
            val xRatio = dayValue.toFloat() / maxX
            val x = chartLeft + xRatio * chartWidth
            drawLine(
                color = gridColor,
                start = Offset(x, chartTop),
                end = Offset(x, chartBottom),
                strokeWidth = gridLineWidth
            )
            val dayLabel = formatDayLabel(dayValue, maxDayIndex)
            val xLabelResult = textMeasurer.measure(dayLabel, axisTextStyle)
            drawText(
                textLayoutResult = xLabelResult,
                topLeft = Offset(
                    x = x - xLabelResult.size.width / 2f,
                    y = chartBottom + with(density) { 4.dp.toPx() }
                )
            )
        }

        // ========== 绘制标准遗忘曲线 ==========
        drawCurveLine(
            points = data.standardCurve,
            color = standardLineColor,
            strokeWidth = standardLineWidth,
            chartLeft = chartLeft,
            chartRight = chartRight,
            chartTop = chartTop,
            chartBottom = chartBottom,
            maxDayIndex = maxDayIndex,
            drawDots = false,
            dotRadius = 0f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f),
            fillGradient = false,
            progress = animationProgress.value
        )

        // ========== 绘制用户实际曲线 ==========
        val showUserDots = maxDayIndex <= 7
        drawCurveLine(
            points = data.userCurve,
            color = userLineColor,
            strokeWidth = userLineWidth,
            chartLeft = chartLeft,
            chartRight = chartRight,
            chartTop = chartTop,
            chartBottom = chartBottom,
            maxDayIndex = maxDayIndex,
            drawDots = showUserDots,
            dotRadius = userDotRadius,
            fillGradient = true,
            progress = animationProgress.value
        )

        // ========== 手势反馈 (Crosshair & Tooltip) ==========
        if (touchX != null && data.userCurve.isNotEmpty() && data.standardCurve.isNotEmpty()) {
            var minDiff = Float.MAX_VALUE
            var bestDayIndex = 0
            for (day in 0..maxDayIndex) {
                val cx = chartLeft + (day.toFloat() / maxX) * chartWidth
                val diff = abs(cx - touchX!!)
                if (diff < minDiff) {
                    minDiff = diff
                    bestDayIndex = day
                }
            }

            // 查找最近的数据点
            val userPt = data.userCurve.minByOrNull { abs(it.dayIndex - bestDayIndex) }
            val stdPt = data.standardCurve.minByOrNull { abs(it.dayIndex - bestDayIndex) }

            if (userPt != null && stdPt != null) {
                val targetX = chartLeft + (bestDayIndex.toFloat() / maxX) * chartWidth
                val userY = chartBottom - userPt.retentionRate.coerceIn(0f, 1f) * chartHeight * animationProgress.value
                val stdY = chartBottom - stdPt.retentionRate.coerceIn(0f, 1f) * chartHeight * animationProgress.value

                // 虚线十字
                drawLine(
                    color = axisLabelColor.copy(alpha = 0.5f),
                    start = Offset(targetX, chartTop),
                    end = Offset(targetX, chartBottom),
                    strokeWidth = with(density) { 1.dp.toPx() },
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // 焦点圆圈
                drawCircle(color = userLineColor, radius = userDotRadius * 1.5f, center = Offset(targetX, userY))
                drawCircle(color = Color.White, radius = userDotRadius * 0.8f, center = Offset(targetX, userY))

                drawCircle(color = standardLineColor, radius = userDotRadius * 1.5f, center = Offset(targetX, stdY))
                drawCircle(color = Color.White, radius = userDotRadius * 0.8f, center = Offset(targetX, stdY))

                // Tooltip 卡片
                drawTooltip(
                    day = bestDayIndex,
                    userRate = userPt.retentionRate,
                    stdRate = stdPt.retentionRate,
                    anchorX = targetX,
                    chartTop = chartTop,
                    chartLeft = chartLeft,
                    chartRight = chartRight,
                    textMeasurer = textMeasurer,
                    density = density,
                    isDark = isDark
                )
            }
        }
    }
}

private fun DrawScope.drawCurveLine(
    points: List<CurvePoint>,
    color: Color,
    strokeWidth: Float,
    chartLeft: Float,
    chartRight: Float,
    chartTop: Float,
    chartBottom: Float,
    maxDayIndex: Int,
    drawDots: Boolean,
    dotRadius: Float,
    pathEffect: PathEffect? = null,
    fillGradient: Boolean = false,
    progress: Float = 1f
) {
    if (points.size < 2) return

    val chartWidth = chartRight - chartLeft
    val chartHeight = chartBottom - chartTop
    val maxX = maxDayIndex.coerceAtLeast(1)

    val canvasPoints = points.map { point ->
        val xRatio = point.dayIndex.toFloat() / maxX
        val yRatio = point.retentionRate.coerceIn(0f, 1f) * progress
        Offset(
            x = chartLeft + xRatio * chartWidth,
            y = chartBottom - yRatio * chartHeight
        )
    }

    val path = Path().apply {
        canvasPoints.forEachIndexed { index, offset ->
            if (index == 0) moveTo(offset.x, offset.y)
            else lineTo(offset.x, offset.y)
        }
    }

    if (fillGradient) {
        val fillPath = Path().apply {
            addPath(path)
            lineTo(canvasPoints.last().x, chartBottom)
            lineTo(canvasPoints.first().x, chartBottom)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                startY = chartTop,
                endY = chartBottom
            )
        )
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = pathEffect
        )
    )

    if (drawDots) {
        canvasPoints.forEach { offset ->
            drawCircle(
                color = color,
                radius = dotRadius,
                center = offset
            )
        }
    }
}

private fun DrawScope.drawTooltip(
    day: Int,
    userRate: Float,
    stdRate: Float,
    anchorX: Float,
    chartTop: Float,
    chartLeft: Float,
    chartRight: Float,
    textMeasurer: TextMeasurer,
    density: Density,
    isDark: Boolean
) {
    val bgColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val descColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val shadowColor = Color.Black.copy(alpha = 0.15f)

    val titleStyle = TextStyle(color = titleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    val descStyle = TextStyle(color = descColor, fontSize = 10.sp)

    val titleResult = textMeasurer.measure("第 ${day} 天", titleStyle)
    val userResult = textMeasurer.measure("你的留存: ${(userRate * 100).toInt()}%", descStyle)
    val stdResult = textMeasurer.measure("标准参考: ${(stdRate * 100).toInt()}%", descStyle)

    val padding = with(density) { 8.dp.toPx() }
    val textWidth = maxOf(titleResult.size.width, userResult.size.width, stdResult.size.width).toFloat()
    val boxWidth = textWidth + padding * 2
    val boxHeight = titleResult.size.height + userResult.size.height + stdResult.size.height + padding * 2.5f

    // 防止 Tooltip 越界
    var boxLeft = anchorX - boxWidth / 2f
    if (boxLeft < chartLeft) boxLeft = chartLeft
    if (boxLeft + boxWidth > chartRight) boxLeft = chartRight - boxWidth

    // 默认放在上方，如果超出顶边界，就放下面一点
    var boxTop = chartTop - boxHeight - with(density) { 4.dp.toPx() }
    if (boxTop < 0f) {
        boxTop = chartTop + with(density) { 10.dp.toPx() }
    }

    // 阴影
    drawRoundRect(
        color = shadowColor,
        topLeft = Offset(boxLeft + 4f, boxTop + 4f),
        size = Size(boxWidth, boxHeight),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // 背景
    drawRoundRect(
        color = bgColor,
        topLeft = Offset(boxLeft, boxTop),
        size = Size(boxWidth, boxHeight),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // 文字
    var currentY = boxTop + padding
    drawText(textLayoutResult = titleResult, topLeft = Offset(boxLeft + padding, currentY))
    currentY += titleResult.size.height + padding * 0.5f

    drawCircle(ChartColors.FreshOrange, radius = 6f, center = Offset(boxLeft + padding + 6f, currentY + userResult.size.height / 2f))
    drawText(textLayoutResult = userResult, topLeft = Offset(boxLeft + padding + 18f, currentY))
    currentY += userResult.size.height

    drawCircle(ChartColors.FreshGreen, radius = 6f, center = Offset(boxLeft + padding + 6f, currentY + stdResult.size.height / 2f))
    drawText(textLayoutResult = stdResult, topLeft = Offset(boxLeft + padding + 18f, currentY))
}

fun formatDayLabel(dayIndex: Int, maxDays: Int): String {
    return if (maxDays <= 7) {
        when (dayIndex) {
            0 -> "今\n天"
            1 -> "明\n天"
            2 -> "后\n天"
            else -> "${dayIndex}\n天\n后"
        }
    } else {
        "${dayIndex}天"
    }
}

// ========== Preview ==========
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ForgettingCurveChartPreview() {
    val mockData = ForgettingCurveData(
        standardCurve = listOf(
            CurvePoint(0, 1.00f), CurvePoint(1, 0.34f), CurvePoint(2, 0.29f),
            CurvePoint(3, 0.28f), CurvePoint(4, 0.27f), CurvePoint(5, 0.27f),
            CurvePoint(6, 0.26f), CurvePoint(7, 0.25f), CurvePoint(8, 0.25f),
            CurvePoint(9, 0.25f), CurvePoint(10, 0.25f), CurvePoint(11, 0.24f)
        ),
        userCurve = listOf(
            CurvePoint(0, 1.00f), CurvePoint(1, 0.50f), CurvePoint(2, 0.30f),
            CurvePoint(3, 0.25f), CurvePoint(4, 0.15f), CurvePoint(5, 0.08f),
            CurvePoint(6, 0.03f), CurvePoint(7, 0.01f), CurvePoint(8, 0.01f),
            CurvePoint(9, 0.005f), CurvePoint(10, 0.003f), CurvePoint(11, 0.002f)
        )
    )
    NemoTheme {
        ForgettingCurveChart(
            data = mockData,
            modifier = Modifier.padding(16.dp)
        )
    }
}
