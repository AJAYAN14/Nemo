package com.jian.nemo.feature.statistics.presentation.curve.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.luminance
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
import com.jian.nemo.core.designsystem.theme.StatisticsPalette
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import kotlin.math.roundToInt
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.designsystem.theme.NemoTheme
import com.jian.nemo.feature.statistics.presentation.curve.CurvePoint
import com.jian.nemo.feature.statistics.presentation.curve.CurveStage
import com.jian.nemo.feature.statistics.presentation.curve.ForgettingCurveData
import kotlin.math.abs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 遗忘曲线对比折线图组件
 *
 * 核心优化：
 * - 引入 Animatable 实现平滑形变和线型生长效果
 * - 在用户曲线上方添加平滑的半透明渐变阴影填充
 * - 增加手势侦听，手指滑过时绘制 Crosshair（十字瞄准线）和 Tooltip（高亮信息面板）
 */
@Composable
fun ForgettingCurveChart(
    data: ForgettingCurveData,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    isPlumpDuo: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 颜色方案
    val gridColor = if (isDark) NemoNeutrals.Gray700.copy(alpha = 0.4f) else NemoNeutrals.Gray200
    val axisLabelColor = if (isDark) NemoNeutrals.Gray400 else NemoNeutrals.Gray500
    val standardLineColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else ChartColors.FreshGreen
    
    // 动态色彩联动：折线与面积阴影跟随当前选中的分层阶段主题色平滑补间过渡
    val activeThemeColor = if (isPlumpDuo) {
        when (data.activeStage) {
            CurveStage.COMPOSITE -> IsoluminantPalette.LilacViolet
            CurveStage.YOUNG_EARLY -> IsoluminantPalette.CoralFlame
            CurveStage.YOUNG_DEVELOPING -> IsoluminantPalette.SkyIndigo
            CurveStage.MATURE -> IsoluminantPalette.EmeraldGreen
            CurveStage.EXPERT -> IsoluminantPalette.WarmAmber
        }
    } else {
        data.activeStage.themeColor
    }
    val userLineColor by animateColorAsState(
        targetValue = activeThemeColor,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "userLineColorAnim"
    )

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

    val animatedThreshold by animateFloatAsState(
        targetValue = data.alertThreshold,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "alertThresholdAnim"
    )

    var touchX by remember { mutableStateOf<Float?>(null) }
    val chartBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(600.dp)
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val totalHeight = constraints.maxHeight.toFloat()

        val leftPadding = yLabelWidth + with(density) { 8.dp.toPx() }
        val rightPadding = with(density) { 16.dp.toPx() }
        val topPadding = with(density) { 24.dp.toPx() } // 给 Tooltip 留出空间
        val bottomPadding = with(density) { 40.dp.toPx() }

        val chartLeft = leftPadding
        val chartRight = totalWidth - rightPadding
        val chartTop = topPadding
        val chartBottom = totalHeight - bottomPadding
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        val maxX = maxDayIndex.coerceAtLeast(1)

        val activeTooltip = remember(touchX, data, totalWidth) {
            val currentTouchX = touchX ?: return@remember null
            if (data.userCurve.isEmpty() || data.standardCurve.isEmpty()) return@remember null

            var minDiff = Float.MAX_VALUE
            var bestDayIndex = 0
            for (day in 0..maxDayIndex) {
                val cx = chartLeft + (day.toFloat() / maxX) * chartWidth
                val diff = abs(cx - currentTouchX)
                if (diff < minDiff) {
                    minDiff = diff
                    bestDayIndex = day
                }
            }

            val userPt = data.userCurve.minByOrNull { abs(it.dayIndex - bestDayIndex) } ?: return@remember null
            val stdPt = data.standardCurve.minByOrNull { abs(it.dayIndex - bestDayIndex) } ?: return@remember null
            val targetX = chartLeft + (bestDayIndex.toFloat() / maxX) * chartWidth

            TooltipInfo(
                day = bestDayIndex,
                userRate = userPt.retentionRate,
                stdRate = stdPt.retentionRate,
                alertThreshold = data.alertThreshold,
                targetX = targetX,
                chartTop = chartTop,
                chartLeft = chartLeft,
                chartRight = chartRight
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(chartBackdrop)
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

            // ========== 绘制三元自适应记忆警戒线 ==========
            val yThresholdRatio = animatedThreshold.coerceIn(0f, 1f)
            val yThreshold = chartBottom - yThresholdRatio * chartHeight
            val alertLineColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else StatisticsPalette.AlertGold
            drawLine(
                color = alertLineColor.copy(alpha = 0.75f),
                start = Offset(chartLeft, yThreshold),
                end = Offset(chartRight, yThreshold),
                strokeWidth = with(density) { 1.dp.toPx() },
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )

            val thresholdPercent = (animatedThreshold * 100).roundToInt()
            val thresholdText = "${thresholdPercent}% 警戒线"
            val thresholdStyle = TextStyle(
                color = if (isPlumpDuo) {
                    if (isDark) alertLineColor else NemoNeutrals.Gray800
                } else {
                    StatisticsPalette.AlertGold.copy(alpha = 0.9f)
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            val thresholdLayout = textMeasurer.measure(thresholdText, thresholdStyle)
            drawText(
                textLayoutResult = thresholdLayout,
                topLeft = Offset(
                    x = chartRight - thresholdLayout.size.width - with(density) { 4.dp.toPx() },
                    y = yThreshold - thresholdLayout.size.height - with(density) { 2.dp.toPx() }
                )
            )

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

            // ========== 手势反馈 (Crosshair 虚线十字与焦点圆圈) ==========
            if (activeTooltip != null) {
                val targetX = activeTooltip.targetX
                val userY = chartBottom - activeTooltip.userRate.coerceIn(0f, 1f) * chartHeight * animationProgress.value
                val stdY = chartBottom - activeTooltip.stdRate.coerceIn(0f, 1f) * chartHeight * animationProgress.value

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
            }
        }

        // ========== 悬浮 Tooltip (实验室同款带底色液态按钮) ==========
        if (activeTooltip != null) {
            CurveLiquidTooltip(
                info = activeTooltip,
                backdrop = chartBackdrop,
                isDark = isDark,
                userColor = userLineColor,
                stageLabel = data.activeStage.label,
                isPlumpDuo = isPlumpDuo
            )
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

private data class TooltipInfo(
    val day: Int,
    val userRate: Float,
    val stdRate: Float,
    val alertThreshold: Float,
    val targetX: Float,
    val chartTop: Float,
    val chartLeft: Float,
    val chartRight: Float
)

@Composable
private fun CurveLiquidTooltip(
    info: TooltipInfo,
    backdrop: Backdrop?,
    isDark: Boolean,
    userColor: Color,
    stageLabel: String,
    isPlumpDuo: Boolean = false
) {
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val tooltipWidth = tooltipSize.width.toFloat()
    val tooltipHeight = tooltipSize.height.toFloat()

    var boxLeft = info.targetX - tooltipWidth / 2f
    if (boxLeft < info.chartLeft) boxLeft = info.chartLeft
    if (tooltipWidth > 0 && boxLeft + tooltipWidth > info.chartRight) {
        boxLeft = info.chartRight - tooltipWidth
    }

    val offsetPx = with(density) { 8.dp.toPx() }
    var boxTop = info.chartTop - tooltipHeight - offsetPx
    if (boxTop < 0f) {
        boxTop = info.chartTop + with(density) { 12.dp.toPx() }
    }

    val stdPointColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else ChartColors.FreshGreen
    val alertPointColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else StatisticsPalette.AlertGold

    Box(
        modifier = Modifier
            .onSizeChanged { tooltipSize = it }
            .graphicsLayer {
                translationX = boxLeft
                translationY = boxTop
            }
    ) {
        FrostedLiquidGlassButton(
            onClick = {},
            backdrop = backdrop,
            surfaceColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.6f)
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            isInteractive = false
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "第 ${info.day} 天",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDark) NemoNeutrals.Gray50 else NemoNeutrals.Gray900
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(userColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${stageLabel}留存: ${(info.userRate * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) NemoNeutrals.Gray300 else NemoNeutrals.Gray700
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(stdPointColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "标准参考: ${(info.stdRate * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) NemoNeutrals.Gray300 else NemoNeutrals.Gray700
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(alertPointColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "动态警戒: ${(info.alertThreshold * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) NemoNeutrals.Gray300 else NemoNeutrals.Gray700
                    )
                }
            }
        }
    }
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
