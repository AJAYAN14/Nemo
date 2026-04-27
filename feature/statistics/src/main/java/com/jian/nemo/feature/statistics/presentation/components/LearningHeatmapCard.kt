package com.jian.nemo.feature.statistics.presentation.components

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.usecase.statistics.HeatmapDay

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

private val WEEKDAYS = listOf("", "二", "", "四", "", "六", "")
private val MONTHS = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

@Composable
fun LearningHeatmapCard(
    heatmapData: List<HeatmapDay>,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    cardColor: Color = MaterialTheme.colorScheme.surface
) {
    if (heatmapData.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp) // Reduced padding
        ) {
            // Header removed (Redundant with Screen Title)

            Spacer(modifier = Modifier.height(16.dp))

            // Heatmap Content
            HeatmapContent(
                data = heatmapData,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            HeatmapLegend(isDarkTheme = isDarkTheme)
        }
    }
}

@Composable
private fun HeatmapContent(
    data: List<HeatmapDay>,
    isDarkTheme: Boolean
) {
    // Haptics
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Config
    val blockSize = 14.dp
    val spacing = 4.dp
    val blockSizePx = with(density) { blockSize.toPx() }
    val spacingPx = with(density) { spacing.toPx() }

    // Monday start: padding to align first row to Monday
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

    val weekdayLabelWidth = 28.dp
    val monthHeaderHeight = 20.dp
    val totalWidth = (blockSize + spacing) * weeks + weekdayLabelWidth
    val totalHeight = (blockSize + spacing) * 7 + monthHeaderHeight

    // Auto scroll to end when layout is ready
    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // Selected Info
    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }
    
    // Month Labels Calculation
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
        // 1. Fixed Sidebar (Weekday Labels)
        val textPaint = android.graphics.Paint().apply {
            color = if (isDarkTheme) android.graphics.Color.parseColor("#8B949E") else android.graphics.Color.parseColor("#64748B")
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
        }

        Canvas(
            modifier = Modifier
                .size(width = weekdayLabelWidth, height = totalHeight)
        ) {
            val headerHeightPx = monthHeaderHeight.toPx()
            WEEKDAYS.forEachIndexed { index, label ->
                if (label.isNotEmpty()) {
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        0f,
                        headerHeightPx + index * (blockSizePx + spacingPx) + blockSizePx * 0.8f,
                        textPaint
                    )
                }
            }
        }

        // 2. Scrollable Content (Months + Heatmap Grid)
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
                            onPress = { offset ->
                                val col = (offset.x / (blockSizePx + spacingPx)).toInt()
                                val row = ((offset.y - with(density) { monthHeaderHeight.toPx() }) / (blockSizePx + spacingPx)).toInt()
                                val index = col * 7 + row
                                if (index in paddedData.indices && col >= 0 && row >= 0) {
                                    selectedDay = paddedData[index]
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                }
                                tryAwaitRelease()
                                selectedDay = null
                            }
                        )
                    }
            ) {
                val headerHeightPx = monthHeaderHeight.toPx()

                // Draw Month Labels
                monthLabels.forEach { (name, weekIndex) ->
                    drawContext.canvas.nativeCanvas.drawText(
                        name,
                        weekIndex * (blockSizePx + spacingPx),
                        headerHeightPx * 0.7f,
                        textPaint
                    )
                }

                // Draw Heatmap Cells
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
                            cornerRadius = CornerRadius(with(density) { 2.dp.toPx() })
                        )
                    }
                }
            }
        }
    }

    // Selection Popup (Smart Tooltip with Smooth Layout Animation)
    AnimatedVisibility(
        visible = selectedDay != null,
        enter = expandVertically() + fadeIn() + scaleIn(),
        exit = shrinkVertically() + fadeOut() + scaleOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
         Box(contentAlignment = Alignment.Center) {
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (selectedDay != null) "${formatDate(selectedDay!!.date)}: ${selectedDay!!.count} 次学习" else "",
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HeatmapLegend(isDarkTheme: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "少",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 4.dp)
        )

        (0..4).forEach { level ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(10.dp)
                    .background(
                        color = getHeatmapColor(level, isDarkTheme),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        Text(
            text = "多",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
