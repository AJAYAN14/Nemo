package com.jian.nemo.feature.statistics.presentation.curve.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.ChartColors
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.designsystem.theme.NemoTheme
import com.jian.nemo.feature.statistics.presentation.curve.CurvePoint
import com.jian.nemo.feature.statistics.presentation.curve.ForgettingCurveData

/**
 * 遗忘曲线对比折线图组件
 *
 * 使用 Compose Canvas 手绘实现，支持：
 * - Y 轴百分比标签 (0%~100%)
 * - X 轴自定义中文日期标签（今天/明天/后天/N天后）
 * - 浅灰色背景网格线
 * - 两条差异化样式折线（标准曲线 vs 用户曲线）
 *
 * @param data 遗忘曲线数据模型，包含标准曲线和用户曲线两组数据
 * @param modifier 外部修饰符
 */
@Composable
fun ForgettingCurveChart(
    data: ForgettingCurveData,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 颜色方案：使用项目已有的 ChartColors 和 NemoNeutrals
    val gridColor = if (isDark) NemoNeutrals.Gray700.copy(alpha = 0.4f) else NemoNeutrals.Gray200
    val axisLabelColor = if (isDark) NemoNeutrals.Gray400 else NemoNeutrals.Gray500
    val standardLineColor = ChartColors.FreshGreen
    val userLineColor = ChartColors.FreshOrange

    // 线条宽度 (dp 转 px)
    val standardLineWidth = with(density) { 2.dp.toPx() }
    val userLineWidth = with(density) { 3.dp.toPx() }
    val userDotRadius = with(density) { 4.dp.toPx() }
    val gridLineWidth = with(density) { 0.5.dp.toPx() }

    // 文字样式
    val axisTextStyle = TextStyle(
        color = axisLabelColor,
        fontSize = 10.sp,
        textAlign = TextAlign.Center
    )

    // 预计算 X 轴标签，确定最大数据点数
    val maxDayIndex = maxOf(
        data.standardCurve.maxOfOrNull { it.dayIndex } ?: 0,
        data.userCurve.maxOfOrNull { it.dayIndex } ?: 0
    )

    // 预先测量 Y 轴标签宽度，用于确定绘图区域左边距
    val yLabelWidth = remember(textMeasurer) {
        textMeasurer.measure("100%", axisTextStyle).size.width.toFloat()
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        // ========== 布局参数计算 ==========
        // 绘图区域边距（为坐标轴标签预留空间）
        val leftPadding = yLabelWidth + with(density) { 8.dp.toPx() }
        val rightPadding = with(density) { 16.dp.toPx() }
        val topPadding = with(density) { 8.dp.toPx() }
        val bottomPadding = with(density) { 40.dp.toPx() } // X 轴标签空间

        // 实际绘图区域
        val chartLeft = leftPadding
        val chartRight = size.width - rightPadding
        val chartTop = topPadding
        val chartBottom = size.height - bottomPadding
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        // ========== 绘制背景网格线 ==========
        val ySteps = 10 // 0%, 10%, 20% ... 100%

        for (i in 0..ySteps) {
            val yRatio = i.toFloat() / ySteps
            val y = chartBottom - yRatio * chartHeight

            // 横向网格线
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = gridLineWidth
            )

            // Y 轴百分比标签（格式化为带百分号的字符串，例如 "20%", "100%"）
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

        // 纵向网格线 + X 轴标签
        // 根据数据范围动态计算标签间隔：
        // ≤7天: 每天  ≤30天: 每5天  ≤90天: 每15天  >90天: 每60天
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

            // 纵向网格线
            drawLine(
                color = gridColor,
                start = Offset(x, chartTop),
                end = Offset(x, chartBottom),
                strokeWidth = gridLineWidth
            )

            // X 轴标签
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

        // ========== 绘制标准遗忘曲线（绿色线）==========
        // 纯实线，线条较细，节点处不绘制圆点标记
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
            dotRadius = 0f
        )

        // ========== 绘制用户实际曲线（橙色线）==========
        // 线条较粗，天数 ≤7 时绘制圆点标记，>7 时不绘制以避免拥挤
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
            dotRadius = userDotRadius
        )
    }
}

/**
 * 在 Canvas 中绘制一条折线
 *
 * @param points 数据点列表
 * @param color 线条颜色
 * @param strokeWidth 线条宽度
 * @param chartLeft 绘图区域左边界
 * @param chartRight 绘图区域右边界
 * @param chartTop 绘图区域上边界
 * @param chartBottom 绘图区域下边界
 * @param maxDayIndex X 轴最大天数索引
 * @param drawDots 是否在每个数据节点处绘制实心圆点
 * @param dotRadius 圆点半径
 */
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
    dotRadius: Float
) {
    if (points.size < 2) return

    val chartWidth = chartRight - chartLeft
    val chartHeight = chartBottom - chartTop
    val maxX = maxDayIndex.coerceAtLeast(1)

    // 将数据点转换为 Canvas 坐标
    val canvasPoints = points.map { point ->
        val xRatio = point.dayIndex.toFloat() / maxX
        val yRatio = point.retentionRate.coerceIn(0f, 1f)
        Offset(
            x = chartLeft + xRatio * chartWidth,
            y = chartBottom - yRatio * chartHeight
        )
    }

    // 绘制折线路径
    val path = Path().apply {
        canvasPoints.forEachIndexed { index, offset ->
            if (index == 0) moveTo(offset.x, offset.y)
            else lineTo(offset.x, offset.y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // 绘制数据节点圆点（仅用户曲线需要）
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

/**
 * X 轴标签自定义转换规则
 *
 * 根据总天数范围动态调整标签格式：
 * - 短期 (≤7天): 使用中文描述（今天/明天/后天/N天后）
 * - 中长期 (>7天): 使用简洁的数字格式（0, 5, 10...）+ "天" 后缀
 */
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
    // Mock 数据：模拟截图中的下降曲线趋势
    val mockData = ForgettingCurveData(
        // 标准 FSRS 遗忘曲线（绿色线）— 下降较缓
        standardCurve = listOf(
            CurvePoint(0, 1.00f),
            CurvePoint(1, 0.34f),
            CurvePoint(2, 0.29f),
            CurvePoint(3, 0.28f),
            CurvePoint(4, 0.27f),
            CurvePoint(5, 0.27f),
            CurvePoint(6, 0.26f),
            CurvePoint(7, 0.25f),
            CurvePoint(8, 0.25f),
            CurvePoint(9, 0.25f),
            CurvePoint(10, 0.25f),
            CurvePoint(11, 0.24f)
        ),
        // 用户实际记忆曲线（橙色线）— 下降更陡峭
        userCurve = listOf(
            CurvePoint(0, 1.00f),
            CurvePoint(1, 0.50f),
            CurvePoint(2, 0.30f),
            CurvePoint(3, 0.25f),
            CurvePoint(4, 0.15f),
            CurvePoint(5, 0.08f),
            CurvePoint(6, 0.03f),
            CurvePoint(7, 0.01f),
            CurvePoint(8, 0.01f),
            CurvePoint(9, 0.005f),
            CurvePoint(10, 0.003f),
            CurvePoint(11, 0.002f)
        )
    )

    NemoTheme {
        ForgettingCurveChart(
            data = mockData,
            modifier = Modifier.padding(16.dp)
        )
    }
}
