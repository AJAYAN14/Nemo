package com.jian.nemo.feature.statistics.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.usecase.statistics.HeatmapDay
import com.jian.nemo.core.ui.modifier.softCardShadow

import kotlinx.coroutines.delay

// Heatmap Colors (Fire Style)
private val Level0 = Color(0xFFEBEDF0)
private val Level1 = Color(0xFFFFD7D5)
private val Level2 = Color(0xFFFFA39E)
private val Level3 = Color(0xFFFF4D4F)
private val Level4 = Color(0xFFCF1322)

// Dark Mode Colors (Fire Style)
private val Level0Dark = Color(0xFF161B22)
private val Level1Dark = Color(0xFF3A1C1C)
private val Level2Dark = Color(0xFF682424)
private val Level3Dark = Color(0xFFB52A2A)
private val Level4Dark = Color(0xFFE63E3E)

private val WEEKDAYS = listOf("一", "", "三", "", "五", "", "日")
private val MONTHS = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

@Composable
fun LearningHeatmapCard(
    heatmapData: List<HeatmapDay>,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    cardColor: Color = MaterialTheme.colorScheme.surface
) {
    if (heatmapData.isEmpty()) return

    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }
    var selectionTimestamp by remember { mutableLongStateOf(0L) }
    val totalCount = remember(heatmapData) { heatmapData.sumOf { it.count } }
    val activeDays = remember(heatmapData) { heatmapData.count { it.count > 0 } }

    // 点击显示 3 秒后自动恢复，点击下一个立即打断并重新计时 3 秒
    LaunchedEffect(selectionTimestamp) {
        if (selectedDay != null) {
            delay(3000L)
            selectedDay = null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 24.dp, isDark = isDarkTheme),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 顶部信息条与图例 (固定高度，消除手势交互时的卡片高度抖动)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = selectedDay,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "HeatmapStatus"
                ) { day ->
                    if (day != null) {
                        Text(
                            text = "${formatDate(day.date)} · ${day.count} 次学习",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = "累计活跃 ${activeDays} 天 · 共 ${totalCount} 次",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                HeatmapLegend(isDarkTheme = isDarkTheme)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 日历热力图主体
            HeatmapContent(
                data = heatmapData,
                isDarkTheme = isDarkTheme,
                onDaySelected = { day ->
                    selectedDay = day
                    selectionTimestamp = System.currentTimeMillis()
                }
            )
        }
    }
}

@Composable
private fun HeatmapContent(
    data: List<HeatmapDay>,
    isDarkTheme: Boolean,
    onDaySelected: (HeatmapDay?) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // 方块与间距尺寸 (放大以提升点击精度与视觉饱满度)
    val blockSize = 16.dp
    val spacing = 4.dp
    val blockSizePx = with(density) { blockSize.toPx() }
    val spacingPx = with(density) { spacing.toPx() }

    // 周一对齐填充
    val paddedData = remember(data) {
        if (data.isEmpty()) return@remember emptyList<HeatmapDay?>()
        val calendar = java.util.Calendar.getInstance().apply { 
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            timeInMillis = data[0].date * 86400000L 
        }
        val firstDayOfWeek = (calendar.get(java.util.Calendar.DAY_OF_WEEK) - java.util.Calendar.MONDAY + 7) % 7
        List(firstDayOfWeek) { null } + data
    }

    val totalDays = paddedData.size
    val weeks = (totalDays + 6) / 7

    val weekdayLabelWidth = 22.dp
    val monthHeaderHeight = 20.dp
    val totalWidth = (blockSize + spacing) * weeks + weekdayLabelWidth
    val totalHeight = (blockSize + spacing) * 7 + monthHeaderHeight

    // 自动滚动到最近日期
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // 月份标签位置计算
    val monthLabels = remember(paddedData) {
        val labels = mutableListOf<Pair<String, Int>>()
        var currentMonth = -1
        paddedData.forEachIndexed { index, day ->
            if (day != null) {
                val calendar = java.util.Calendar.getInstance().apply { 
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                    timeInMillis = day.date * 86400000L 
                }
                val month = calendar.get(java.util.Calendar.MONTH)
                val weekIndex = index / 7
                if (month != currentMonth) {
                    if (labels.isEmpty() || weekIndex > labels.last().second + 2) {
                        labels.add(MONTHS[month] to weekIndex)
                        currentMonth = month
                    }
                }
            }
        }
        labels
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        val textPaint = remember(isDarkTheme) {
            android.graphics.Paint().apply {
                color = if (isDarkTheme) android.graphics.Color.parseColor("#8B949E") else android.graphics.Color.parseColor("#64748B")
                textSize = with(density) { 10.sp.toPx() }
                isAntiAlias = true
            }
        }
        val fontMetrics = remember(textPaint) { textPaint.fontMetrics }

        // 1. 左侧星期标签
        Canvas(
            modifier = Modifier
                .size(width = weekdayLabelWidth, height = totalHeight)
        ) {
            val headerHeightPx = monthHeaderHeight.toPx()
            WEEKDAYS.forEachIndexed { index, label ->
                if (label.isNotEmpty()) {
                    val centerY = headerHeightPx + index * (blockSizePx + spacingPx) + blockSizePx / 2f
                    val baselineY = centerY - (fontMetrics.descent + fontMetrics.ascent) / 2f
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        0f,
                        baselineY,
                        textPaint
                    )
                }
            }
        }

        // 2. 右侧热力图网格（可横向滚动）
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = totalWidth - weekdayLabelWidth, height = totalHeight)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                val col = (offset.x / (blockSizePx + spacingPx)).toInt()
                                val row = ((offset.y - with(density) { monthHeaderHeight.toPx() }) / (blockSizePx + spacingPx)).toInt()
                                val index = col * 7 + row
                                if (index in paddedData.indices && col >= 0 && row >= 0) {
                                    val target = paddedData[index]
                                    if (target != null) {
                                        onDaySelected(target)
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }
                        )
                    }
            ) {
                val headerHeightPx = monthHeaderHeight.toPx()
                val monthBaselineY = headerHeightPx / 2f - (fontMetrics.descent + fontMetrics.ascent) / 2f

                // 绘制月份标签
                monthLabels.forEach { (name, weekIndex) ->
                    drawContext.canvas.nativeCanvas.drawText(
                        name,
                        weekIndex * (blockSizePx + spacingPx),
                        monthBaselineY,
                        textPaint
                    )
                }

                // 绘制热力图单元格
                paddedData.forEachIndexed { index, day ->
                    if (day != null) {
                        val col = index / 7
                        val row = index % 7

                        val x = col * (blockSizePx + spacingPx)
                        val y = headerHeightPx + row * (blockSizePx + spacingPx)

                        val color = getHeatmapColor(day.level, isDarkTheme)

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(blockSizePx, blockSizePx),
                            cornerRadius = CornerRadius(with(density) { 3.dp.toPx() })
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapLegend(isDarkTheme: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "少",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 4.dp)
        )

        (0..4).forEach { level ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(10.dp)
                    .background(
                        color = getHeatmapColor(level, isDarkTheme),
                        shape = RoundedCornerShape(2.5.dp)
                    )
            )
        }

        Text(
            text = "多",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun getHeatmapColor(level: Int, isDark: Boolean): Color {
    return if (isDark) {
        when (level) {
            0 -> Level0Dark
            1 -> Level1Dark
            2 -> Level2Dark
            3 -> Level3Dark
            else -> Level4Dark
        }
    } else {
        when (level) {
            0 -> Level0
            1 -> Level1
            2 -> Level2
            3 -> Level3
            else -> Level4
        }
    }
}

private fun formatDate(epochDay: Long): String {
    return DateTimeUtils.formatEpochDayToDisplay(epochDay)
}
