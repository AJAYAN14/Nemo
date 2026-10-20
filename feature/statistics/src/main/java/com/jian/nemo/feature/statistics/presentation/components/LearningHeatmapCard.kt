package com.jian.nemo.feature.statistics.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.usecase.statistics.HeatmapDay
import com.jian.nemo.core.ui.modifier.softCardShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

// 热力图主题配色（火焰渐变）
private val Level0 = Color(0xFFF1F5F9)
private val Level1 = Color(0xFFFFD7D5)
private val Level2 = Color(0xFFFFA39E)
private val Level3 = Color(0xFFFF4D4F)
private val Level4 = Color(0xFFCF1322)

// 暗色模式配色（炽热熔岩微光）
private val Level0Dark = Color(0xFF1E2430)
private val Level1Dark = Color(0xFF3F2020)
private val Level2Dark = Color(0xFF742929)
private val Level3Dark = Color(0xFFBD3131)
private val Level4Dark = Color(0xFFF04A4A)

// 粒子高光核心色
private val DotGlowLight = Color(0xFFFFFFFF)
private val DotGlowDark = Color(0xFFFFECEB)

private val WEEKDAYS = listOf("一", "", "三", "", "五", "", "日")
private val MONTHS = listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")

/**
 * 学习热力图卡片（融合 Dither 点阵粒子微光呼吸 + 果冻弹性滑动探针与解压触感）
 */
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

    // 点击/滑动显示 3.5 秒后自动淡回总体统计
    LaunchedEffect(selectionTimestamp) {
        if (selectedDay != null) {
            delay(3500L)
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
                    .height(28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = selectedDay,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180)) + scaleIn(initialScale = 0.94f))
                            .togetherWith(fadeOut(animationSpec = tween(150)))
                    },
                    label = "HeatmapStatus"
                ) { day ->
                    if (day != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        color = getHeatmapColor(day.level, isDarkTheme),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${formatDate(day.date)} · ${day.count} 次复习",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = "累计活跃 ${activeDays} 天 · 共 ${totalCount} 次",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }

                HeatmapLegend(isDarkTheme = isDarkTheme)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 日历热力图主体（集成果冻弹性探针与点阵呼吸）
            HeatmapContent(
                data = heatmapData,
                isDarkTheme = isDarkTheme,
                selectedDay = selectedDay,
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
    selectedDay: HeatmapDay?,
    onDaySelected: (HeatmapDay?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 方块与间距尺寸
    val blockSize = 16.dp
    val spacing = 4.dp
    val blockSizePx = with(density) { blockSize.toPx() }
    val spacingPx = with(density) { spacing.toPx() }
    val stepPx = blockSizePx + spacingPx

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

    // --- 对角线波前扩散入场动效驱动 (从左下角到右上角，微晶弹泡弹性生长) ---
    val entranceProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    // --- 方案一：Amicro 风格点阵正弦波呼吸驱动 ---
    val infiniteTransition = rememberInfiniteTransition(label = "HeatmapPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    // --- 方案二：Jelly 果冻弹性指示器位置与形变 ---
    val indicatorX = remember { Animatable(-100f) }
    val indicatorY = remember { Animatable(-100f) }
    val indicatorScale = remember { Animatable(1f) }
    val indicatorAlpha = remember { Animatable(0f) }
    var lastInteractedIndex by remember { mutableIntStateOf(-1) }

    // 当选中的日期变化时，弹性弹簧驱动指示器移动到目标方格
    LaunchedEffect(selectedDay) {
        if (selectedDay != null) {
            val targetIdx = paddedData.indexOfFirst { it?.date == selectedDay.date }
            if (targetIdx != -1) {
                val col = targetIdx / 7
                val row = targetIdx % 7
                val targetX = col * stepPx
                val targetY = with(density) { monthHeaderHeight.toPx() } + row * stepPx

                if (indicatorAlpha.value < 0.1f) {
                    indicatorX.snapTo(targetX)
                    indicatorY.snapTo(targetY)
                    indicatorAlpha.animateTo(1f, tween(180))
                } else {
                    // 具备果冻质感的阻尼回弹
                    coroutineScope.launch {
                        indicatorX.animateTo(
                            targetValue = targetX,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                    coroutineScope.launch {
                        indicatorY.animateTo(
                            targetValue = targetY,
                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
            }
        } else {
            indicatorAlpha.animateTo(0f, tween(250))
        }
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
            modifier = Modifier.size(width = weekdayLabelWidth, height = totalHeight)
        ) {
            val headerHeightPx = monthHeaderHeight.toPx()
            WEEKDAYS.forEachIndexed { index, label ->
                if (label.isNotEmpty()) {
                    val centerY = headerHeightPx + index * stepPx + blockSizePx / 2f
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

        // 2. 右侧热力图网格（支持横向滚动、连续滑动探针与果冻阻尼触感）
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
        ) {
            val headerHeightPx = with(density) { monthHeaderHeight.toPx() }
            val primaryColor = MaterialTheme.colorScheme.primary

            fun handleTouch(offset: Offset, isDrag: Boolean) {
                val col = (offset.x / stepPx).toInt()
                val row = ((offset.y - headerHeightPx) / stepPx).toInt()
                val index = col * 7 + row
                if (index in paddedData.indices && col >= 0 && row in 0..6) {
                    val target = paddedData[index]
                    if (target != null && index != lastInteractedIndex) {
                        lastInteractedIndex = index
                        onDaySelected(target)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        if (isDrag) {
                            coroutineScope.launch {
                                indicatorScale.animateTo(0.92f, spring(stiffness = Spring.StiffnessHigh))
                            }
                        }
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .size(width = totalWidth - weekdayLabelWidth, height = totalHeight)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                handleTouch(offset, isDrag = false)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                handleTouch(offset, isDrag = true)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                handleTouch(change.position, isDrag = true)
                            },
                            onDragEnd = {
                                lastInteractedIndex = -1
                                coroutineScope.launch {
                                    indicatorScale.animateTo(1.0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
                                }
                            },
                            onDragCancel = {
                                lastInteractedIndex = -1
                                coroutineScope.launch {
                                    indicatorScale.animateTo(1.0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
                                }
                            }
                        )
                    }
            ) {
                val monthBaselineY = headerHeightPx / 2f - (fontMetrics.descent + fontMetrics.ascent) / 2f

                // 绘制月份标签
                monthLabels.forEach { (name, weekIndex) ->
                    drawContext.canvas.nativeCanvas.drawText(
                        name,
                        weekIndex * stepPx,
                        monthBaselineY,
                        textPaint
                    )
                }

                // 绘制热力图单元格（集成对角线波前扩散入场 + Dither 点阵粒子与动态波浪微光）
                val isEntering = entranceProgress.value < 1f
                val currentProg = entranceProgress.value
                val maxCol = (weeks - 1).coerceAtLeast(1)

                paddedData.forEachIndexed { index, day ->
                    if (day != null) {
                        val col = index / 7
                        val row = index % 7

                        // 计算对角线入场比例 (从左下角 col=0, row=6 沿对角线扩散至右上角 col=weeks-1, row=0)
                        var cellScale = 1.0f
                        var cellEntranceAlpha = 1.0f
                        if (isEntering) {
                            val diagDist = (col.toFloat() / maxCol) * 0.82f + ((6 - row).toFloat() / 6f) * 0.18f
                            val startTime = diagDist * 0.62f
                            val window = 0.38f
                            val localT = ((currentProg - startTime) / window).coerceIn(0f, 1f)

                            if (localT <= 0f) {
                                return@forEachIndexed // 尚未被波前扫到，暂不绘制
                            }

                            // 弹性微晶弹泡 (0 -> 1.15 -> 1.0)
                            cellScale = if (localT < 0.5f) {
                                (localT / 0.5f) * 1.15f
                            } else {
                                val settle = (localT - 0.5f) / 0.5f
                                1.15f - settle * 0.15f
                            }
                            cellEntranceAlpha = (localT * 2.5f).coerceIn(0f, 1f)
                        }

                        val baseColor = getHeatmapColor(day.level, isDarkTheme)
                        val centerX = col * stepPx + blockSizePx / 2f
                        val centerY = headerHeightPx + row * stepPx + blockSizePx / 2f
                        val drawBlockSize = blockSizePx * cellScale
                        val drawTopLeft = Offset(centerX - drawBlockSize / 2f, centerY - drawBlockSize / 2f)
                        val drawCornerRadius = CornerRadius(with(density) { (3.5 * cellScale).dp.toPx() })

                        if (day.level == 0) {
                            // 未打卡单元格：平滑微圆角纯净基底
                            drawRoundRect(
                                color = baseColor.copy(alpha = cellEntranceAlpha),
                                topLeft = drawTopLeft,
                                size = Size(drawBlockSize, drawBlockSize),
                                cornerRadius = drawCornerRadius
                            )
                        } else {
                            // 活跃单元格：微波呼吸与入场透明度合成
                            val wave = sin(col * 0.35f + row * 0.2f + waveOffset)
                            val dynamicAlpha = ((pulseAlpha + wave * 0.08f).coerceIn(0.7f, 1.0f)) * cellEntranceAlpha
                            val renderColor = baseColor.copy(alpha = dynamicAlpha)

                            // 1. 底层饱满色块
                            drawRoundRect(
                                color = renderColor,
                                topLeft = drawTopLeft,
                                size = Size(drawBlockSize, drawBlockSize),
                                cornerRadius = drawCornerRadius
                            )

                            // 2. Dither 微型粒子点阵渲染（等比缩放）
                            val glowColor = if (isDarkTheme) DotGlowDark else DotGlowLight

                            when (day.level) {
                                1 -> {
                                    // Level 1: 中心单点微光核
                                    drawCircle(
                                        color = glowColor.copy(alpha = 0.45f * dynamicAlpha),
                                        radius = with(density) { (1.2 * cellScale).dp.toPx() },
                                        center = Offset(centerX, centerY)
                                    )
                                }
                                2 -> {
                                    // Level 2: 2x2 微型对角点阵
                                    val offset2 = with(density) { (2.4 * cellScale).dp.toPx() }
                                    val dotRadius = with(density) { (1.1 * cellScale).dp.toPx() }
                                    val alpha2 = 0.55f * dynamicAlpha
                                    drawCircle(glowColor.copy(alpha = alpha2), dotRadius, Offset(centerX - offset2, centerY - offset2))
                                    drawCircle(glowColor.copy(alpha = alpha2), dotRadius, Offset(centerX + offset2, centerY + offset2))
                                }
                                3 -> {
                                    // Level 3: 四角对称发光核 + 中心主光点
                                    val offset3 = with(density) { (3.0 * cellScale).dp.toPx() }
                                    val dotRadius = with(density) { (1.1 * cellScale).dp.toPx() }
                                    val centerRadius = with(density) { (1.6 * cellScale).dp.toPx() }
                                    val alpha3 = 0.7f * dynamicAlpha
                                    drawCircle(glowColor.copy(alpha = alpha3), dotRadius, Offset(centerX - offset3, centerY - offset3))
                                    drawCircle(glowColor.copy(alpha = alpha3), dotRadius, Offset(centerX + offset3, centerY - offset3))
                                    drawCircle(glowColor.copy(alpha = alpha3), dotRadius, Offset(centerX - offset3, centerY + offset3))
                                    drawCircle(glowColor.copy(alpha = alpha3), dotRadius, Offset(centerX + offset3, centerY + offset3))
                                    drawCircle(glowColor.copy(alpha = 0.85f * dynamicAlpha), centerRadius, Offset(centerX, centerY))
                                }
                                4 -> {
                                    // Level 4: 炽热高能态（3x3 紧凑矩阵 + 中心光晕扩散）
                                    val offset4 = with(density) { (3.2 * cellScale).dp.toPx() }
                                    val dotRadius = with(density) { (1.2 * cellScale).dp.toPx() }
                                    val centerRadius = with(density) { (2.0 * cellScale).dp.toPx() }
                                    val alpha4 = 0.85f * dynamicAlpha

                                    // 中心光晕外圈
                                    drawCircle(
                                        color = glowColor.copy(alpha = 0.25f * dynamicAlpha),
                                        radius = with(density) { (4.5 * cellScale).dp.toPx() },
                                        center = Offset(centerX, centerY)
                                    )
                                    // 点阵分布
                                    listOf(-offset4, 0f, offset4).forEach { dx ->
                                        listOf(-offset4, 0f, offset4).forEach { dy ->
                                            val r = if (dx == 0f && dy == 0f) centerRadius else dotRadius
                                            drawCircle(glowColor.copy(alpha = alpha4), r, Offset(centerX + dx, centerY + dy))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 绘制果冻弹性指示器（Jelly Indicator）
                if (indicatorAlpha.value > 0.01f) {
                    val ix = indicatorX.value
                    val iy = indicatorY.value
                    val scale = indicatorScale.value
                    val alpha = indicatorAlpha.value

                    val scaledBlockSize = blockSizePx * scale
                    val pad = (blockSizePx - scaledBlockSize) / 2f
                    val expand = with(density) { 2.5.dp.toPx() }

                    // 外圈果冻光环
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.25f * alpha),
                        topLeft = Offset(ix + pad - expand, iy + pad - expand),
                        size = Size(scaledBlockSize + expand * 2, scaledBlockSize + expand * 2),
                        cornerRadius = CornerRadius(with(density) { 5.dp.toPx() })
                    )

                    // 内圈精准高亮边框
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.9f * alpha),
                        topLeft = Offset(ix + pad, iy + pad),
                        size = Size(scaledBlockSize, scaledBlockSize),
                        cornerRadius = CornerRadius(with(density) { 3.5.dp.toPx() }),
                        style = Stroke(width = with(density) { 1.8.dp.toPx() })
                    )
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
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
