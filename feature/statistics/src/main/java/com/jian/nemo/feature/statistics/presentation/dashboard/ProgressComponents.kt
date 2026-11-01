package com.jian.nemo.feature.statistics.presentation.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jian.nemo.feature.statistics.R
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.SlidingDotIndicator
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoSecondary
import com.jian.nemo.core.designsystem.theme.NemoDanger
import com.jian.nemo.core.designsystem.theme.ChartColors
import com.jian.nemo.core.designsystem.theme.Rubik
import com.jian.nemo.core.designsystem.theme.StatisticsPalette
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import kotlinx.coroutines.delay

// 定义常量
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
    val plumpIcon: PlumpIcon,
    val accentColor: Color,
    val main: BentoMetric,
    val topRight: BentoMetric,
    val bottomRight: BentoMetric,
    val visualType: BentoVisualType
)

/**
 * 沉浸式头部 (V2: Clean & Minimal)
 * 去除背景干扰，强调信息本身
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
        // 主标题 (Bold & Clean)
        // 强制使用中文标题，虽然参数传进来了，但这里可以不做处理，依赖外部传入中文 "进度"
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
 * 学习摘要卡片 (V2: Premium Card - Clean Surface)
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
    cardColor: Color // Unused in V2, we use PremiumCard defaults
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

    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
    val unitItems = stringResource(R.string.stats_dashboard_unit_items)
    val unitDays = stringResource(R.string.stats_dashboard_unit_days)
    val unitPercent = stringResource(R.string.stats_dashboard_unit_percent)

    val pageModels = listOf(
        BentoPageModel(
            title = stringResource(R.string.stats_dashboard_bento_title_daily_overview),
            icon = Icons.Rounded.Today,
            plumpIcon = PlumpIcon.Calendar,
            accentColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else NemoPrimary,
            main = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_today_learned), todayLearned.toString(), unitItems),
            topRight = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_due_review), dueCount.toString(), unitItems),
            bottomRight = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_goal_rate), todayProgress.toString(), unitPercent),
            visualType = BentoVisualType.Progress
        ),
        BentoPageModel(
            title = stringResource(R.string.stats_dashboard_bento_title_learning_track),
            icon = Icons.Rounded.Timeline,
            plumpIcon = PlumpIcon.Activity,
            accentColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else NemoSecondary,
            main = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_streak), studyStreak.toString(), unitDays),
            topRight = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_mastered), masteredCount.toString(), unitItems),
            bottomRight = BentoMetric(stringResource(R.string.stats_dashboard_bento_label_unmastered), unmasteredCount.toString(), unitItems),
            visualType = BentoVisualType.Dots
        ),
        BentoPageModel(
            title = stringResource(R.string.stats_dashboard_bento_title_growth_overview),
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            plumpIcon = PlumpIcon.TrendingUp,
            accentColor = if (isPlumpDuo) IsoluminantPalette.LilacViolet else StatisticsPalette.GrowthAccentGold,
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
            plumpIcon = model.plumpIcon,
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
    plumpIcon: PlumpIcon,
    visualType: BentoVisualType,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val mainTileColor = if (isDark) accentColor.copy(alpha = 0.88f) else accentColor

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = mainTileColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO) {
                PlumpDuoIcon(
                    icon = plumpIcon,
                    color = Color.White.copy(alpha = 0.22f),
                    size = 72.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-6).dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(72.dp)
                        .offset(x = 8.dp, y = (-6).dp)
                )
            }

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
        shape = RoundedCornerShape(24.dp),
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
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.7f)
                        .background(Color.White, RoundedCornerShape(2.dp))
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
                                shape = RoundedCornerShape(2.dp)
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
                                shape = RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Premium Card (V2: Clean White/Dark + Soft Shadow)
 */
@Composable
fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Scale Animation if clickable
    val scale by if(onClick != null) {
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
    // Light mode: Pure white + strong soft shadow. Dark mode: Surface Container + light shadow.
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(24.dp),
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
 * 数据与资料 Box
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
                    color = NemoDanger,
                    title = stringResource(R.string.stats_dashboard_item_today_stats_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_today_stats_desc),
                    onClick = onStatisticsClick,
                    plumpIcon = PlumpIcon.ChartPie,
                    paletteColor = IsoluminantPalette.RoseRuby,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.Insights,
                    color = IosColors.Purple,
                    title = stringResource(R.string.stats_dashboard_item_history_stats_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_history_stats_desc),
                    onClick = onHistoricalStatisticsClick,
                    plumpIcon = PlumpIcon.History,
                    paletteColor = IsoluminantPalette.LilacViolet,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.ShowChart,
                    color = ChartColors.FreshGreen,
                    title = stringResource(R.string.stats_dashboard_item_curve_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_curve_desc),
                    onClick = onForgettingCurveClick,
                    plumpIcon = PlumpIcon.TrendingDown,
                    paletteColor = IsoluminantPalette.EmeraldGreen,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.BubbleChart,
                    color = NemoPrimary,
                    title = stringResource(R.string.stats_dashboard_item_calendar_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_calendar_desc),
                    onClick = onLearningCalendarClick,
                    plumpIcon = PlumpIcon.Calendar,
                    paletteColor = IsoluminantPalette.SkyIndigo,
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
                    color = NemoSecondary,
                    title = stringResource(R.string.stats_dashboard_item_word_list_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_word_list_desc),
                    onClick = onWordListClick,
                    plumpIcon = PlumpIcon.SpellCheck,
                    paletteColor = IsoluminantPalette.WarmAmber,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.Book,
                    color = NemoPrimary,
                    title = stringResource(R.string.stats_dashboard_item_grammar_list_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_grammar_list_desc),
                    onClick = onGrammarListClick,
                    plumpIcon = PlumpIcon.GrammarLayers,
                    paletteColor = IsoluminantPalette.SkyIndigo,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.Dataset,
                    color = NemoPrimary,
                    title = stringResource(R.string.stats_dashboard_item_category_vocab_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_category_vocab_desc),
                    onClick = onCategoryClassificationClick,
                    plumpIcon = PlumpIcon.LayoutGrid,
                    paletteColor = IsoluminantPalette.LakesideCyan,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.AutoAwesome,
                    color = NemoSecondary,
                    title = stringResource(R.string.stats_dashboard_item_authentic_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_authentic_desc),
                    onClick = onLibraryClick,
                    plumpIcon = PlumpIcon.Sparkles,
                    paletteColor = IsoluminantPalette.GlazeMint,
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
                    color = NemoSecondary,
                    title = stringResource(R.string.stats_dashboard_item_practice_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_practice_desc),
                    onClick = onCategoryPracticeClick,
                    plumpIcon = PlumpIcon.Gamepad,
                    paletteColor = IsoluminantPalette.SeaMarine,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.AutoFixHigh,
                    color = NemoDanger,
                    title = stringResource(R.string.stats_dashboard_item_leech_title),
                    subtitle = stringResource(R.string.stats_dashboard_item_leech_desc),
                    onClick = onLeechManagementClick,
                    plumpIcon = PlumpIcon.Flame,
                    paletteColor = IsoluminantPalette.CoralFlame,
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
 * ListItem with "Squircle" Icon (Apple Settings Style)
 * 支持 MD3 与 Plump Duo 双图标风格自适应切换
 */
@Composable
fun SquircleListItem(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true,
    plumpIcon: PlumpIcon? = null,
    paletteColor: Color? = null
) {
    val iconStyle = LocalSettingIconStyle.current
    val isPlumpDuo = iconStyle == SettingIconStyle.PLUMP_DUO && plumpIcon != null
    val effectiveColor = if (isPlumpDuo && paletteColor != null) paletteColor else color

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标容器：尺寸保持 42dp，保证右侧文字对齐基准线 100% 稳定，绝不抖动
        Box(
            modifier = Modifier
                .size(42.dp)
                .then(
                    if (!isPlumpDuo) {
                        Modifier.background(effectiveColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPlumpDuo && plumpIcon != null) {
                PlumpDuoIcon(
                    icon = plumpIcon,
                    color = effectiveColor,
                    size = 28.dp
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveColor,
                    modifier = Modifier.size(22.dp)
                )
            }
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
            modifier = Modifier.padding(start = 74.dp), // Align with text start
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}
