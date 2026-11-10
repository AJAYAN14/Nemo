package com.jian.nemo.feature.statistics.calendar

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.ActivityRingPalette
import com.jian.nemo.core.designsystem.theme.Rubik
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.statistics.R
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.util.Calendar

/**
 * 学习日历界面
 *
 * 遵循 Nemo Design 黄金实现范式：
 * 1. 顶层 CommonHeader 结合四阶垂直渐变遮罩 (0.95f -> 0.75f -> 0.35f -> 0f) 与物理折射 Backdrop；
 * 2. 分组标题与卡片对齐规范：水平外边距 16.dp，标题与卡片亲密间距 8.dp；
 * 3. 2:3 黄金比例与双轨系统：微卡片使用 24dp 容器配 16dp Google Fonts 原生实心图标；
 * 4. 严格遵循 docs/design/color-palette 调色盘与层级背景规范。
 */
@Composable
fun LearningCalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: LearningCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedDate = uiState.selectedDate
    var isMonthMode by remember { mutableStateOf(false) }

    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. 底层滚动内容（被 backdrop 采样源录制）
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusBarTop + 60.dp + 16.dp,
                bottom = navBarBottom + 24.dp
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
                        CalendarSectionTitle(
                            if (monthMode) stringResource(R.string.stats_calendar_section_month_rings)
                            else stringResource(R.string.stats_calendar_section_week_rings)
                        )
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
                CalendarSectionTitle(stringResource(R.string.stats_calendar_section_details))
                DayDetailPanel(
                    uiState = uiState
                )
            }
        }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和四阶垂直渐变保护遮罩，读取 backdrop）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.95f),
                            backgroundColor.copy(alpha = 0.75f),
                            backgroundColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = stringResource(R.string.stats_calendar_title_screen),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
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
                                text = if (monthMode) stringResource(R.string.stats_calendar_mode_month) else stringResource(R.string.stats_calendar_mode_week),
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
        }
    }
}

/**
 * 分组标题标准组件
 *
 * 遵循 Nemo Design 规范：
 * 水平边距 0.dp（外层 LazyColumn 已有 16.dp padding，首字在 16.dp 处与卡片完全左对齐）；
 * 纵向边距 8.dp，与下方卡片紧密连接。
 */
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
 * Premium Card (统一卡片容器)
 */
@Composable
fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val scale by if (onClick != null) {
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

    Surface(
        onClick = {
            if (onClick != null) {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
        },
        enabled = onClick != null,
        shape = RoundedRectangle(26.dp),
        color = containerColor,
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

/**
 * 详情微指标数据模型
 */
data class MetricDetailItem(
    val color: Color,
    val label: String,
    val count: Int,
    val unit: String,
    val icon: ImageVector? = null
)

/**
 * 详情面板卡片 (2x2 极简微卡面板，风格与活动三环统一)
 */
@Composable
fun DayDetailPanel(
    uiState: LearningCalendarUiState
) {
    val context = LocalContext.current
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

    // 根据日期形态生成 2x2 对称微卡列表，严格指定 Google Fonts 原生 Material 实心图标
    val metricItems = remember(
        selectedEpoch, isToday, isFuture,
        reviewWordsValue, reviewGrammarValue, newWordsValue, newGrammarValue,
        todayActualReviewWordsValue, todayActualReviewGrammarValue,
        context
    ) {
        val list = mutableListOf<MetricDetailItem>()
        when {
            isToday -> {
                list.add(MetricDetailItem(ActivityRingColors.MovePink, context.getString(R.string.stats_calendar_detail_due_words), reviewWordsValue, context.getString(R.string.stats_calendar_unit_word), Icons.Rounded.TrackChanges))
                list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, context.getString(R.string.stats_calendar_detail_due_grammars), reviewGrammarValue, context.getString(R.string.stats_calendar_unit_grammar), Icons.AutoMirrored.Rounded.MenuBook))
                list.add(MetricDetailItem(ActivityRingColors.StandCyan, context.getString(R.string.stats_calendar_detail_reviewed_words_today), todayActualReviewWordsValue, context.getString(R.string.stats_calendar_unit_word), Icons.Rounded.CheckCircle))
                list.add(MetricDetailItem(ActivityRingColors.StandCyan, context.getString(R.string.stats_calendar_detail_reviewed_grammars_today), todayActualReviewGrammarValue, context.getString(R.string.stats_calendar_unit_grammar), Icons.Rounded.CheckCircle))
                if (newWordsValue > 0 || newGrammarValue > 0) {
                    list.add(MetricDetailItem(ActivityRingColors.MovePink, context.getString(R.string.stats_calendar_detail_learned_words_today), newWordsValue, context.getString(R.string.stats_calendar_unit_word), Icons.Rounded.TrackChanges))
                    list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, context.getString(R.string.stats_calendar_detail_learned_grammars_today), newGrammarValue, context.getString(R.string.stats_calendar_unit_grammar), Icons.AutoMirrored.Rounded.MenuBook))
                }
            }
            isFuture -> {
                list.add(MetricDetailItem(ActivityRingColors.MovePink, context.getString(R.string.stats_calendar_detail_forecast_words), reviewWordsValue, context.getString(R.string.stats_calendar_unit_word), Icons.Rounded.TrackChanges))
                list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, context.getString(R.string.stats_calendar_detail_forecast_grammars), reviewGrammarValue, context.getString(R.string.stats_calendar_unit_grammar), Icons.AutoMirrored.Rounded.MenuBook))
            }
            else -> {
                list.add(MetricDetailItem(ActivityRingColors.MovePink, context.getString(R.string.stats_calendar_detail_learned_words), newWordsValue, context.getString(R.string.stats_calendar_unit_word), Icons.Rounded.TrackChanges))
                list.add(MetricDetailItem(ActivityRingColors.ExerciseGreen, context.getString(R.string.stats_calendar_detail_learned_grammars), newGrammarValue, context.getString(R.string.stats_calendar_unit_grammar), Icons.AutoMirrored.Rounded.MenuBook))
                list.add(MetricDetailItem(ActivityRingColors.StandCyan, context.getString(R.string.stats_calendar_detail_reviewed_words), reviewWordsValue, context.getString(R.string.stats_calendar_unit_word), Icons.Rounded.CheckCircle))
                list.add(MetricDetailItem(ActivityRingColors.StandCyan, context.getString(R.string.stats_calendar_detail_reviewed_grammars), reviewGrammarValue, context.getString(R.string.stats_calendar_unit_grammar), Icons.Rounded.CheckCircle))
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
                            text = stringResource(R.string.stats_calendar_empty_detail_desc),
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
 *
 * 严格遵循 2:3 黄金比例与单容器双轨复用架构：
 * 容器尺寸 24dp (极小级 Micro)，内部图标 16dp (严格 2:3)，四周留白 4dp；
 * 底框使用 G2 连续平滑曲率 RoundedRectangle(6.dp)。
 */
@Composable
fun DetailMiniMetricCard(
    item: MetricDetailItem,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) {
        ActivityRingPalette.MetricCardBgDark
    } else {
        ActivityRingPalette.MetricCardBgLight
    }

    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val containerBgColor = if (isSoftTranslucent) item.color.copy(alpha = if (isDark) 0.2f else 0.12f) else item.color
    val iconTint = if (isSoftTranslucent) item.color else Color.White

    Surface(
        color = cardBg,
        shape = RoundedRectangle(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // 顶部：2:3 黄金比例双轨图标 + 标签名
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.icon != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(containerBgColor, RoundedRectangle(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
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
