package com.jian.nemo.feature.statistics.calendar

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.LearningStats
import com.jian.nemo.core.domain.model.ReviewForecast
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.modifier.softCardShadow
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * 学习日历界面 (UI/UX Pro Max)
 * 风格统一：Solid Typography, Premium Card, Squircle Icons
 * 结构：Scaffold + CommonHeader 用于保持应用一致性
 */
@Composable
fun LearningCalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: LearningCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate = uiState.selectedDate
    val todayStats = uiState.todayStats ?: LearningStats(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0)
    var isMonthMode by remember { mutableStateOf(false) }

    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val backdrop = rememberLayerBackdrop()
    val view = LocalView.current

    Scaffold(
        topBar = {
            CommonHeader(
                title = "学习日历",
                onBack = onNavigateBack,
                backdrop = backdrop,
                actions = {
                    FrostedLiquidGlassButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            isMonthMode = !isMonthMode
                        },
                        backdrop = backdrop,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        AnimatedContent(
                            targetState = isMonthMode,
                            transitionSpec = {
                                if (targetState) {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> -height } + fadeOut())
                                } else {
                                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> height } + fadeOut())
                                }.using(SizeTransform(clip = false))
                            },
                            label = "calendarViewToggleText"
                        ) { monthMode ->
                            Text(
                                text = if (monthMode) "月视图" else "周视图",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 核心活动三环卡片 (周模式 ↔ 月模式联动)
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = isMonthMode,
                        transitionSpec = {
                            fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                        },
                        label = "calendarSectionTitle"
                    ) { monthMode ->
                        CalendarSectionTitle(if (monthMode) "月度活动日历" else "周进度与目标")
                    }
                }
                ActivityRingsCard(
                    isMonthMode = isMonthMode,
                    selectedDate = selectedDate,
                    todayEpochDay = uiState.todayEpochDay,
                    todayStats = uiState.todayStats,
                    recordsMap = uiState.recordsMap,
                    forecastMap = uiState.weekForecast,
                    onDateSelected = viewModel::onDateSelected,
                    onMonthChanged = viewModel::loadMonthRecords
                )
            }

            // 2. 详情面板 (Day Detail)
            item {
                CalendarSectionTitle("详细记录")
                DayDetailPanel(
                    uiState = uiState
                )
            }
        }
    }
}

@Composable
fun CalendarSectionTitle(
    text: String,
    modifier: Modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Premium Card (Shared Style)
 */
@Composable
fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val scale by if(onClick != null) {
        val isPressed by interactionSource.collectIsPressedAsState()
         animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            label = "cardScale",
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    Surface(
        onClick = {
            if (onClick != null) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
        },
        enabled = onClick != null,
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 26.dp, isDark = isDark)
            .then(
                if (onClick != null) {
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                } else Modifier
            ),
        interactionSource = interactionSource,
        content = {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                content = content
            )
        }
    )
}

// 今日概览卡片
@Composable
fun TodaySummaryCard(
    stats: LearningStats
) {
    PremiumCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 新学单词
            StatItem(
                value = stats.todayLearnedWords.toString(),
                label = "新学单词",
                color = NemoPrimary,
                modifier = Modifier.weight(1f)
            )

            // 新学语法
            StatItem(
                value = stats.todayLearnedGrammars.toString(),
                label = "新学语法",
                color = NemoSecondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 待复习
            StatItem(
                value = (stats.dueWords + stats.dueGrammars).toString(),
                label = "待复习",
                color = NemoOrange, // Orange equivalent
                modifier = Modifier.weight(1f)
            )

            // 已完成
            val completed = stats.todayLearnedWords + stats.todayLearnedGrammars +
                           stats.todayReviewedWords + stats.todayReviewedGrammars
            StatItem(
                value = completed.toString(),
                label = "已完成",
                color = NemoIndigo, // Indigo equivalent
                modifier = Modifier.weight(1f)
            )
        }
    }
}


// 统计项组件 (Squircle Style)
@Composable
fun StatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.06f))
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = Rubik,
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}


// 可交互日历卡片 (支持周/月模式动画切换)
@Composable
fun InteractiveCalendarCard(
    isMonthMode: Boolean,
    selectedDate: Date,
    todayEpochDay: Long,
    weekForecast: Map<Long, ReviewForecast>,
    heatmapData: List<com.jian.nemo.core.domain.usecase.statistics.HeatmapDay>,
    onDateSelected: (Date) -> Unit
) {
    PremiumCard {
        AnimatedContent(
            targetState = isMonthMode,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + slideInVertically { it / 8 }).togetherWith(
                    fadeOut(animationSpec = tween(120))
                )
            },
            label = "CalendarViewSwitch"
        ) { inMonthMode ->
            if (inMonthMode) {
                MonthViewContent(
                    selectedDate = selectedDate,
                    todayEpochDay = todayEpochDay,
                    heatmapData = heatmapData,
                    weekForecast = weekForecast,
                    onDateSelected = onDateSelected
                )
            } else {
                WeekViewContent(
                    selectedDate = selectedDate,
                    todayEpochDay = todayEpochDay,
                    weekForecast = weekForecast,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

// 周视图卡片内容
@Composable
fun WeekViewContent(
    selectedDate: Date,
    todayEpochDay: Long,
    weekForecast: Map<Long, ReviewForecast>,
    onDateSelected: (Date) -> Unit
) {
    val today = Date(todayEpochDay * 86400000L)

    val selectedCal = Calendar.getInstance()
    selectedCal.time = selectedDate
    selectedCal.set(Calendar.HOUR_OF_DAY, 0)
    selectedCal.set(Calendar.MINUTE, 0)
    selectedCal.set(Calendar.SECOND, 0)
    selectedCal.set(Calendar.MILLISECOND, 0)

    val weekDayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val currentCal = Calendar.getInstance()
            currentCal.time = today
            currentCal.set(Calendar.HOUR_OF_DAY, 0)
            currentCal.set(Calendar.MINUTE, 0)
            currentCal.set(Calendar.SECOND, 0)
            currentCal.set(Calendar.MILLISECOND, 0)

            for (i in 0 until 7) {
                val dateCal = Calendar.getInstance()
                dateCal.time = currentCal.time
                val date = dateCal.time
                val isToday = i == 0

                val dateToCheck = Calendar.getInstance()
                dateToCheck.time = date
                dateToCheck.set(Calendar.HOUR_OF_DAY, 0)
                dateToCheck.set(Calendar.MINUTE, 0)
                dateToCheck.set(Calendar.SECOND, 0)
                dateToCheck.set(Calendar.MILLISECOND, 0)

                val isSelected = dateToCheck.timeInMillis == selectedCal.timeInMillis
                val dateEpochDay = todayEpochDay + i
                val forecast = weekForecast[dateEpochDay]
                val count = (forecast?.wordCount ?: 0) + (forecast?.grammarCount ?: 0)

                val warningColor = when {
                    count >= 50 -> MaterialTheme.colorScheme.error
                    count > 0 -> NemoPrimary
                    else -> null
                }

                val dayOfWeek = dateCal.get(Calendar.DAY_OF_WEEK)
                val dayIndex = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                val dayLabel = weekDayLabels[dayIndex]

                WeekDayItem(
                    dayLabel = dayLabel,
                    dayNumber = dateCal.get(Calendar.DAY_OF_MONTH).toString(),
                    isToday = isToday,
                    isSelected = isSelected,
                    warningColor = warningColor,
                    onClick = { onDateSelected(date) }
                )

                currentCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val dateText = "${selectedCal.get(Calendar.MONTH) + 1}月${selectedCal.get(Calendar.DAY_OF_MONTH)}日"
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = CircleShape
            ) {
                Text(
                    text = if(selectedCal.timeInMillis == todayEpochDay * 86400000L) "今天 · $dateText" else dateText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// 月视图卡片内容
@Composable
fun MonthViewContent(
    selectedDate: Date,
    todayEpochDay: Long,
    heatmapData: List<com.jian.nemo.core.domain.usecase.statistics.HeatmapDay>,
    weekForecast: Map<Long, ReviewForecast>,
    onDateSelected: (Date) -> Unit
) {
    val view = LocalView.current
    var displayedDate by remember { mutableStateOf(selectedDate) }

    val displayedCal = remember(displayedDate) {
        Calendar.getInstance().apply {
            time = displayedDate
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val selectedCal = remember(selectedDate) {
        Calendar.getInstance().apply {
            time = selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val todayCal = remember(todayEpochDay) {
        Calendar.getInstance().apply {
            timeInMillis = todayEpochDay * 86400000L
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val heatmapMap = remember(heatmapData) {
        heatmapData.associateBy { it.date }
    }

    val weekDayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Column {
        // 月份切换控制条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                val c = Calendar.getInstance().apply {
                    time = displayedCal.time
                    add(Calendar.MONTH, -1)
                }
                displayedDate = c.time
            }) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowLeft,
                    contentDescription = "上一月",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${displayedCal.get(Calendar.YEAR)}年 ${displayedCal.get(Calendar.MONTH) + 1}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                val c = Calendar.getInstance().apply {
                    time = displayedCal.time
                    add(Calendar.MONTH, 1)
                }
                displayedDate = c.time
            }) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = "下一月",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 月历网格背景卡片
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // 星期表头
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDayLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 月历天数 7x6 矩阵
                val firstDayOfWeek = displayedCal.get(Calendar.DAY_OF_WEEK)
                val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY
                val daysInMonth = displayedCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                var dayCounter = 1
                for (row in 0 until 6) {
                    if (dayCounter > daysInMonth && row > 0) break

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            if (cellIndex < offset || dayCounter > daysInMonth) {
                                Box(modifier = Modifier.weight(1f))
                            } else {
                                val currentDayNumber = dayCounter
                                val cellCal = Calendar.getInstance().apply {
                                    time = displayedCal.time
                                    set(Calendar.DAY_OF_MONTH, currentDayNumber)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }

                                val cellDate = cellCal.time
                                val isSelected = cellCal.timeInMillis == selectedCal.timeInMillis
                                val isToday = cellCal.timeInMillis == todayCal.timeInMillis
                                val epochDay = (cellCal.timeInMillis + TimeZone.getDefault().getOffset(cellCal.timeInMillis)) / 86400000L
                                val dayRecord = heatmapMap[epochDay]
                                val forecast = weekForecast[epochDay]
                                val hasStudyRecord = (dayRecord != null && dayRecord.count > 0) ||
                                    (epochDay > todayEpochDay && forecast != null && (forecast.wordCount + forecast.grammarCount) > 0)

                                MonthDayCell(
                                    dayNumber = currentDayNumber.toString(),
                                    isSelected = isSelected,
                                    isToday = isToday,
                                    hasRecord = hasStudyRecord,
                                    onClick = { onDateSelected(cellDate) },
                                    modifier = Modifier.weight(1f)
                                )

                                dayCounter++
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Date Tag
        val selMonth = selectedCal.get(Calendar.MONTH) + 1
        val selDay = selectedCal.get(Calendar.DAY_OF_MONTH)
        val dateText = "${selMonth}月${selDay}日"
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = CircleShape
            ) {
                Text(
                    text = if(selectedCal.timeInMillis == todayCal.timeInMillis) "今天 · $dateText" else dateText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// 月历天数网格单元 (圆形选中效果 + 调大字号)
@Composable
fun MonthDayCell(
    dayNumber: String,
    isSelected: Boolean,
    isToday: Boolean,
    hasRecord: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> Color.White
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bgColor)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = dayNumber,
            fontSize = 16.sp,
            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Bold,
            color = contentColor
        )

        if (hasRecord && !isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(NemoTeal)
            )
        }
    }
}

// 周日期项组件 (Vertical Pill Style)
@Composable
fun WeekDayItem(
    dayLabel: String,
    dayNumber: String,
    isToday: Boolean,
    isSelected: Boolean,
    warningColor: Color? = null,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> Color.White
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium
    val elevation = if (isSelected) 4.dp else 0.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(42.dp) // 更符合胶囊型的宽度比例
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .padding(vertical = 14.dp, horizontal = 4.dp)
    ) {
        Text(
            text = dayLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor.copy(alpha = if(isSelected) 0.8f else 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dayNumber,
            fontSize = 15.sp,
            fontWeight = fontWeight,
            color = contentColor
        )
        
        // 复习预警红点
        if (warningColor != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White else warningColor)
            )
        } else {
            // 占位，防止抖动
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

// 详情微指标数据模型
data class MetricDetailItem(
    val color: Color,
    val label: String,
    val count: Int,
    val unit: String
)

// 详情面板卡片 (2x2 极简微卡面板，风格与 #099 活动三环统一)
@Composable
fun DayDetailPanel(
    uiState: LearningCalendarUiState
) {
    val selectedDate = uiState.selectedDate
    val todayEpoch = uiState.todayEpochDay

    val cal = Calendar.getInstance().apply { time = selectedDate }
    val selectedEpoch = java.time.LocalDate.of(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    ).toEpochDay()

    val isToday = selectedEpoch == todayEpoch
    val isFuture = selectedEpoch > todayEpoch

    var reviewWordsValue = 0
    var reviewGrammarValue = 0
    var newWordsValue = 0
    var newGrammarValue = 0
    var todayActualReviewWordsValue = 0
    var todayActualReviewGrammarValue = 0

    when {
        isToday -> {
            val stats = uiState.todayStats
            if (stats != null) {
                reviewWordsValue = stats.dueWords
                reviewGrammarValue = stats.dueGrammars
                newWordsValue = stats.todayLearnedWords
                newGrammarValue = stats.todayLearnedGrammars
                todayActualReviewWordsValue = stats.todayReviewedWords
                todayActualReviewGrammarValue = stats.todayReviewedGrammars
            }
        }
        isFuture -> {
            val forecast = uiState.weekForecast[selectedEpoch]
            if (forecast != null) {
                reviewWordsValue = forecast.wordCount
                reviewGrammarValue = forecast.grammarCount
            }
        }
        else -> {
            val record = uiState.selectedDateRecord
            if (record != null) {
                reviewWordsValue = record.reviewedWords
                reviewGrammarValue = record.reviewedGrammars
                newWordsValue = record.learnedWords
                newGrammarValue = record.learnedGrammars
            }
        }
    }

    // 根据日期形态生成 2x2 对称微卡列表
    val metricItems = remember(
        selectedEpoch, isToday, isFuture,
        reviewWordsValue, reviewGrammarValue, newWordsValue, newGrammarValue,
        todayActualReviewWordsValue, todayActualReviewGrammarValue
    ) {
        val list = mutableListOf<MetricDetailItem>()
        when {
            isToday -> {
                list.add(MetricDetailItem(ActivityRingColors.MovePink, "待复习单词", reviewWordsValue, "个"))
                list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, "待复习语法", reviewGrammarValue, "条"))
                list.add(MetricDetailItem(ActivityRingColors.StandCyan, "今日已复习单词", todayActualReviewWordsValue, "个"))
                list.add(MetricDetailItem(ActivityRingColors.StandCyan, "今日已复习语法", todayActualReviewGrammarValue, "条"))
                if (newWordsValue > 0 || newGrammarValue > 0) {
                    list.add(MetricDetailItem(ActivityRingColors.MovePink, "今日新学单词", newWordsValue, "个"))
                    list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, "今日新学语法", newGrammarValue, "条"))
                }
            }
            isFuture -> {
                list.add(MetricDetailItem(ActivityRingColors.MovePink, "预计复习单词", reviewWordsValue, "个"))
                list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, "预计复习语法", reviewGrammarValue, "条"))
            }
            else -> {
                list.add(MetricDetailItem(ActivityRingColors.MovePink, "已学单词", newWordsValue, "个"))
                list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, "已学语法", newGrammarValue, "条"))
                list.add(MetricDetailItem(ActivityRingColors.StandCyan, "已复习单词", reviewWordsValue, "个"))
                list.add(MetricDetailItem(ActivityRingColors.StandCyan, "已复习语法", reviewGrammarValue, "条"))
            }
        }
        list
    }

    val hasData = metricItems.any { it.count > 0 }

    PremiumCard {
        AnimatedContent(
            targetState = selectedEpoch to hasData,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 60)) +
                 slideInVertically(initialOffsetY = { it / 6 })).togetherWith(
                    fadeOut(animationSpec = tween(90))
                )
            },
            label = "DayDetailAnimation"
        ) { (_, dataExists) ->
            if (dataExists) {
                // 2x2 极简双列网格布局
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val rows = metricItems.chunked(2)
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                DetailMiniMetricCard(
                                    item = item,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // 如果单行只有一个元素，用透明占位保持对称
                            if (rowItems.size == 1) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "该日暂无详细记录",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2x2 极简微指标卡片 (Modern Metric Chip)
 */
@Composable
fun DetailMiniMetricCard(
    item: MetricDetailItem,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    } else {
        Color(0xFFF6F4EF) // 与 #099 --ux-surface-2 统一
    }

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // 顶部：彩色小圆点 + 标签名
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(item.color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 底部：大字号数字 (Rubik Black) + 紧凑单位
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = item.count.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Rubik,
                        fontWeight = FontWeight.Black
                    ),
                    color = if (item.count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.unit,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
    }
}
