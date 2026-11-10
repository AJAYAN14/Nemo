package com.jian.nemo.feature.statistics.presentation.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.Rubik
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.SlidingDotIndicator
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.statistics.R
import kotlinx.coroutines.delay

private const val ANIMATION_DURATION = 200
private const val PRESS_SCALE = 0.97f
private const val CAROUSEL_AUTO_SCROLL_MS = 5000L
private val BENTO_GRID_HEIGHT = 168.dp

private data class BentoMetric(
    val label: String,
    val value: String,
    val unit: String
)

private enum class BentoVisualType {
    Progress,
    Dots,
    Bars
}

private data class BentoPageModel(
    val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val main: BentoMetric,
    val topRight: BentoMetric,
    val bottomRight: BentoMetric,
    val visualType: BentoVisualType
)

/**
 * 沉浸式头部 (Clean & Minimal)
 */
@Composable
fun ImmersiveDashboardHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp, top = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * 学习摘要卡片 (轮播概览)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LearningSummaryCard(
    progress: Float,
    masteredCount: Int,
    totalWords: Int,
    todayLearned: Int,
    dailyGoal: Int,
    unmasteredCount: Int,
    studyStreak: Int,
    dueCount: Int,
    totalStudyDays: Int,
    weekStudyDays: Int,
    cardColor: Color
) {
    val pageCount = 10000
    val pagerState = rememberPagerState(
        initialPage = pageCount / 2,
        pageCount = { pageCount }
    )
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(pagerState, isDragged) {
        while (true) {
            delay(CAROUSEL_AUTO_SCROLL_MS)
            if (!isDragged) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    val todayProgress = when {
        dailyGoal <= 0 -> 0
        else -> ((todayLearned.toFloat() / dailyGoal) * 100).toInt()
    }

    val unitItems = stringResource(R.string.stats_dashboard_unit_items)
    val unitDays = stringResource(R.string.stats_dashboard_unit_days)
    val unitPercent = stringResource(R.string.stats_dashboard_unit_percent)

    val pageModels = listOf(
        BentoPageModel(
            title = stringResource(R.string.stats_dashboard_bento_title_daily_overview),
            icon = Icons.Rounded.Today,
            accentColor = IosColors.Blue,
            main = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_today_learned), todayLearned.toString(), unitItems),
            topRight = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_due_review), dueCount.toString(), unitItems),
            bottomRight = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_goal_rate), todayProgress.toString(), unitPercent),
            visualType = BentoVisualType.Progress
        ),
        BentoPageModel(
            title = stringResource(R.string.stats_dashboard_bento_title_learning_track),
            icon = Icons.Rounded.Timeline,
            accentColor = IosColors.Green,
            main = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_streak), studyStreak.toString(), unitDays),
            topRight = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_mastered), masteredCount.toString(), unitItems),
            bottomRight = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_unmastered), unmasteredCount.toString(), unitItems),
            visualType = BentoVisualType.Dots
        ),
        BentoPageModel(
            title = stringResource(R.string.stats_dashboard_bento_title_growth_overview),
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            accentColor = IosColors.Orange,
            main = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_total_progress), (progress * 100).toInt().toString(), unitPercent),
            topRight = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_total_days), totalStudyDays.toString(), unitDays),
            bottomRight = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_week_days), weekStudyDays.toString(), unitDays),
            visualType = BentoVisualType.Bars
        )
    )

    val actualPage = pagerState.currentPage % pageModels.size
    val currentPage = pageModels[actualPage]

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val model = pageModels[page % pageModels.size]
            BentoStatsGrid(
                model = model,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SlidingDotIndicator(
            pagerState = pagerState,
            pageCount = 3,
            activeColor = currentPage.accentColor,
            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp)
        )
    }
}

@Composable
private fun BentoStatsGrid(
    model: BentoPageModel,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(BENTO_GRID_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BentoMainTile(
            metric = model.main,
            accentColor = model.accentColor,
            icon = model.icon,
            visualType = model.visualType,
            modifier = Modifier
                .weight(1.45f)
                .fillMaxHeight()
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoSubTile(
                metric = model.topRight,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
            BentoSubTile(
                metric = model.bottomRight,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BentoMainTile(
    metric: BentoMetric,
    accentColor: Color,
    icon: ImageVector,
    visualType: BentoVisualType,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mainTileColor = if (isDark) accentColor.copy(alpha = 0.88f) else accentColor

    Surface(
        modifier = modifier,
        shape = RoundedRectangle(24.dp),
        color = mainTileColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(72.dp)
                    .offset(x = 8.dp, y = (-6).dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                BentoVisualHint(visualType)
                Text(
                    text = metric.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White.copy(alpha = 0.78f)
                )
                StatValueRow(
                    value = metric.value,
                    unit = metric.unit,
                    valueStyle = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = Rubik,
                        fontWeight = FontWeight.Bold,
                        fontSize = 44.sp,
                        color = Color.White
                    ),
                    unitStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.56f)
                    )
                )
            }
        }
    }
}

@Composable
private fun BentoSubTile(
    metric: BentoMetric,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val subTileColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White

    Surface(
        modifier = modifier,
        shape = RoundedRectangle(24.dp),
        color = subTileColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = metric.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
            )
            StatValueRow(
                value = metric.value,
                unit = metric.unit,
                valueStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = Rubik,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                unitStyle = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            )
        }
    }
}

@Composable
private fun StatValueRow(
    value: String,
    unit: String,
    valueStyle: TextStyle,
    unitStyle: TextStyle
) {
    Row(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = valueStyle.copy(fontFeatureSettings = "tnum"),
            modifier = Modifier.alignByBaseline()
        )
        Text(
            text = unit,
            style = unitStyle,
            modifier = Modifier.alignByBaseline()
        )
    }
}

@Composable
private fun BentoVisualHint(type: BentoVisualType) {
    when (type) {
        BentoVisualType.Progress -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedRectangle(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.7f)
                        .background(Color.White, RoundedRectangle(2.dp))
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        BentoVisualType.Dots -> {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(7) { index ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (index < 5) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.24f),
                                shape = RoundedRectangle(2.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        BentoVisualType.Bars -> {
            Row(
                modifier = Modifier.height(16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                listOf(8.dp, 12.dp, 16.dp, 10.dp).forEachIndexed { index, barHeight ->
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(barHeight)
                            .background(
                                color = when (index) {
                                    2 -> Color.White
                                    1 -> Color.White.copy(alpha = 0.45f)
                                    3 -> Color.White.copy(alpha = 0.62f)
                                    else -> Color.White.copy(alpha = 0.24f)
                                },
                                shape = RoundedRectangle(3.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Premium Card
 */
@Composable
fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    val scale by if (onClick != null) {
        val isPressed by interactionSource.collectIsPressedAsState()
        animateFloatAsState(
            targetValue = if (isPressed) PRESS_SCALE else 1f,
            label = "cardScale",
            animationSpec = tween(ANIMATION_DURATION)
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedRectangle(24.dp),
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 24.dp, isDark = isDark)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        content = { Column(content = content) }
    )
}

/**
 * 数据与资料卡片组
 */
@Composable
fun DataAndResourcesSection(
    isDarkTheme: Boolean,
    cardColor: Color,
    onCategoryPracticeClick: () -> Unit = {},
    onStatisticsClick: () -> Unit,
    onLearningCalendarClick: () -> Unit,
    onHistoricalStatisticsClick: () -> Unit,
    onWordListClick: () -> Unit,
    onGrammarListClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onCategoryClassificationClick: () -> Unit,
    onLeechManagementClick: () -> Unit,
    onForgettingCurveClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. 统计与分析
        SectionTitle(stringResource(R.string.stats_dashboard_section_stats_analysis))
        PremiumCard {
            Column(modifier = Modifier.padding(8.dp)) {
                SquircleListItem(
                    icon = Icons.Rounded.QueryStats,
                    color = IosColors.Red,
                    title = stringResource(R.string.stats_dashboard_item_today_stats_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_today_stats_desc),
                    onClick = onStatisticsClick,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.Insights,
                    color = IosColors.Purple,
                    title = stringResource(R.string.stats_dashboard_item_history_stats_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_history_stats_desc),
                    onClick = onHistoricalStatisticsClick,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.AutoMirrored.Rounded.ShowChart,
                    color = IosColors.Green,
                    title = stringResource(R.string.stats_dashboard_item_curve_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_curve_desc),
                    onClick = onForgettingCurveClick,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.CalendarMonth,
                    color = IosColors.Blue,
                    title = stringResource(R.string.stats_dashboard_item_calendar_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_calendar_desc),
                    onClick = onLearningCalendarClick,
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. 资料与词库
        SectionTitle(stringResource(R.string.stats_dashboard_section_materials_lexicon))
        PremiumCard {
            Column(modifier = Modifier.padding(8.dp)) {
                SquircleListItem(
                    icon = Icons.Rounded.SortByAlpha,
                    color = IosColors.Orange,
                    title = stringResource(R.string.stats_dashboard_item_word_list_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_word_list_desc),
                    onClick = onWordListClick,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    color = IosColors.Indigo,
                    title = stringResource(R.string.stats_dashboard_item_grammar_list_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_grammar_list_desc),
                    onClick = onGrammarListClick,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.Category,
                    color = IosColors.Teal,
                    title = stringResource(R.string.stats_dashboard_item_category_vocab_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_category_vocab_desc),
                    onClick = onCategoryClassificationClick,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.AutoAwesome,
                    color = IosColors.Mint,
                    title = stringResource(R.string.stats_dashboard_item_authentic_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_authentic_desc),
                    onClick = onLibraryClick,
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. 强化与突破
        SectionTitle(stringResource(R.string.stats_dashboard_section_reinforce_breakthrough))
        PremiumCard {
            Column(modifier = Modifier.padding(8.dp)) {
                SquircleListItem(
                    icon = Icons.Rounded.SportsEsports,
                    color = IosColors.Cyan,
                    title = stringResource(R.string.stats_dashboard_item_practice_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_practice_desc),
                    onClick = onCategoryPracticeClick,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.Whatshot,
                    color = IosColors.Red,
                    title = stringResource(R.string.stats_dashboard_item_leech_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_leech_desc),
                    onClick = onLeechManagementClick,
                    showDivider = false
                )
            }
        }
    }
}

@Composable
fun SectionTitle(
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
 * 列表项组件 (Squircle Icon)
 * 严格遵循 2:3 黄金比例、G2 连续平滑曲率与双轨风格单容器架构
 */
@Composable
fun SquircleListItem(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val containerBgColor = if (isSoftTranslucent) color.copy(alpha = 0.15f) else color
    val iconTint = if (isSoftTranslucent) color else Color.White

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESS_SCALE else 1f,
        label = "listItemScale",
        animationSpec = tween(ANIMATION_DURATION)
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(containerBgColor, RoundedRectangle(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 74.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
        }
    }
}
