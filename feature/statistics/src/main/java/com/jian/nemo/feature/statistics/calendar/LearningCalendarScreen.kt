package com.jian.nemo.feature.statistics.calendar

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
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.LearningStats
import com.jian.nemo.core.domain.model.ReviewForecast
import com.jian.nemo.core.ui.component.common.CommonHeader
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

    val backgroundColor = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            CommonHeader(
                title = "学习日历",
                onBack = onNavigateBack
            )
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 今日概览 (Today Summary)
            item {
                CalendarSectionTitle("今日概览")
                TodaySummaryCard(
                    stats = todayStats
                )
            }


            // 3. 周视图 (Week View)
            item {
                CalendarSectionTitle("本周进度")
                WeekViewCard(
                    selectedDate = selectedDate,
                    todayEpochDay = uiState.todayEpochDay,
                    onDateSelected = viewModel::onDateSelected
                )
            }

            // 4. 详情面板 (Day Detail)
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
fun CalendarSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
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
    val shadowElevation = if (isDark) 2.dp else 10.dp
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.04f)

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(26.dp),
                spotColor = shadowColor,
                ambientColor = shadowColor
            ),
        interactionSource = interactionSource,
        content = { Column(modifier = Modifier.padding(20.dp), content = content) }
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
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
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

// 周视图卡片
@Composable
fun WeekViewCard(
    selectedDate: Date,
    todayEpochDay: Long,
    onDateSelected: (Date) -> Unit
) {
    // 将 todayEpochDay 转换回 Date 对象作为本周显示基准的起点 (也就是逻辑上的今天)
    val today = Date(todayEpochDay * 86400000L)

    // 生成选中日期的具体日期文本
    val selectedCal = Calendar.getInstance()
    selectedCal.time = selectedDate
    selectedCal.set(Calendar.HOUR_OF_DAY, 0)
    selectedCal.set(Calendar.MINUTE, 0)
    selectedCal.set(Calendar.SECOND, 0)
    selectedCal.set(Calendar.MILLISECOND, 0)

    // 周几标签映射
    val weekDayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    PremiumCard {
        // 周日期网格
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

            // 显示今天及未来6天，共7天
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

                val dayOfWeek = dateCal.get(Calendar.DAY_OF_WEEK)
                val dayIndex = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                val dayLabel = weekDayLabels[dayIndex]

                WeekDayItem(
                    dayLabel = dayLabel,
                    dayNumber = dateCal.get(Calendar.DAY_OF_MONTH).toString(),
                    isToday = isToday,
                    isSelected = isSelected,
                    onClick = { onDateSelected(date) }
                )

                currentCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Date Indicator Text underneath
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

// 周日期项组件 (Vertical Pill Style)
@Composable
fun WeekDayItem(
    dayLabel: String,
    dayNumber: String,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
            .clickable(onClick = onClick)
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
    }
}

// 详情面板卡片
@Composable
fun DayDetailPanel(
    uiState: LearningCalendarUiState
) {
    val selectedDate = uiState.selectedDate
    val todayEpoch = uiState.todayEpochDay

    // Manually calculate local epoch day for API 24 compatibility
    // Using simple offset calculation which matches DateTimeUtils logic implicitly (via desugaring) but is safer explicitly here
    val selectedEpoch = (selectedDate.time + TimeZone.getDefault().getOffset(selectedDate.time)) / 86400000L

    // 计算显示数据
    val isToday = selectedEpoch == todayEpoch
    val isFuture = selectedEpoch > todayEpoch

    var reviewWordsValue = 0
    var reviewGrammarValue = 0
    var newWordsValue = 0
    var newGrammarValue = 0

    when {
        isToday -> {
            val stats = uiState.todayStats
            if (stats != null) {
                reviewWordsValue = stats.dueWords
                reviewGrammarValue = stats.dueGrammars
                newWordsValue = stats.todayLearnedWords
                newGrammarValue = stats.todayLearnedGrammars
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

    val reviewLabelSuffix = if (isFuture) "预计复习" else if (isToday) "待复习" else "已复习"
    val newLabelSuffix = if (isToday || isFuture) "新学" else "已学"

    // Check if empty
    val hasData = reviewWordsValue > 0 || reviewGrammarValue > 0 || newWordsValue > 0 || newGrammarValue > 0

    PremiumCard {
        AnimatedContent(
            targetState = selectedEpoch to hasData,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220, delayMillis = 90)) + 
                 slideInVertically(initialOffsetY = { it / 4 })).togetherWith(
                    fadeOut(animationSpec = tween(90))
                )
            },
            label = "DayDetailAnimation"
        ) { (epoch, data) ->
            if (data) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (reviewWordsValue > 0) {
                        DetailSquircleItem(
                            icon = if (isFuture) Icons.Rounded.Update else Icons.Rounded.PlayArrow,
                            color = if (isFuture) NemoIndigo else NemoOrange,
                            label = "${reviewLabelSuffix}单词",
                            value = "$reviewWordsValue 个",
                            showDivider = (reviewGrammarValue > 0 || newWordsValue > 0 || newGrammarValue > 0)
                        )
                    }
                    if (reviewGrammarValue > 0) {
                        DetailSquircleItem(
                            icon = if (isFuture) Icons.Rounded.Update else Icons.Rounded.PlayArrow,
                            color = if (isFuture) NemoIndigo.copy(alpha = 0.8f) else NemoOrange.copy(alpha = 0.8f),
                            label = "${reviewLabelSuffix}语法",
                            value = "$reviewGrammarValue 条",
                            showDivider = (newWordsValue > 0 || newGrammarValue > 0)
                        )
                    }
                    if (newWordsValue > 0) {
                        DetailSquircleItem(
                            icon = Icons.Rounded.Book,
                            color = NemoPrimary,
                            label = "${newLabelSuffix}单词",
                            value = "$newWordsValue 个",
                            showDivider = (newGrammarValue > 0)
                        )
                    }
                    if (newGrammarValue > 0) {
                        DetailSquircleItem(
                            icon = Icons.Rounded.Create,
                            color = NemoSecondary,
                            label = "${newLabelSuffix}语法",
                            value = "$newGrammarValue 条",
                            showDivider = false
                        )
                    }
                }
            } else {
                 Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "该日无学习记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// 详情项组件 (Squircle Style)
@Composable
fun DetailSquircleItem(
    icon: ImageVector,
    color: Color,
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 58.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
            thickness = 0.5.dp
        )
    }
}
