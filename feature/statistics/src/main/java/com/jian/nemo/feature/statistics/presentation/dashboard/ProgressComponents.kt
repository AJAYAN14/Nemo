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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.SlidingDotIndicator
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.NemoSecondary
import com.jian.nemo.core.designsystem.theme.NemoDanger
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
        todayLearned >= dailyGoal -> 100
        else -> ((todayLearned.toFloat() / dailyGoal) * 100).toInt()
    }

    val pageModels = listOf(
        BentoPageModel(
            title = "日间概览",
            icon = Icons.Rounded.Today,
            accentColor = NemoPrimary,
            main = BentoMetric("今日已学", todayLearned.toString(), "项"),
            topRight = BentoMetric("待复习", dueCount.toString(), "项"),
            bottomRight = BentoMetric("目标完成度", todayProgress.toString(), "%"),
            visualType = BentoVisualType.Progress
        ),
        BentoPageModel(
            title = "学习轨迹",
            icon = Icons.Rounded.Timeline,
            accentColor = NemoSecondary,
            main = BentoMetric("连续学习", studyStreak.toString(), "天"),
            topRight = BentoMetric("累计掌握", masteredCount.toString(), "项"),
            bottomRight = BentoMetric("待学习", unmasteredCount.toString(), "项"),
            visualType = BentoVisualType.Dots
        ),
        BentoPageModel(
            title = "成长总览",
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            accentColor = Color(0xFFF4B73F),
            main = BentoMetric("总进度", (progress * 100).toInt().toString(), "%"),
            topRight = BentoMetric("累计学习", totalStudyDays.toString(), "天"),
            bottomRight = BentoMetric("本周学习", weekStudyDays.toString(), "天"),
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
        shape = RoundedCornerShape(24.dp),
        color = mainTileColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.12f),
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
                        fontWeight = FontWeight.Black,
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
                    fontWeight = FontWeight.Black,
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
    val shadowElevation = if (isDark) 4.dp else 12.dp
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.05f) // Very soft ambient shadow

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        content = { Column(content = content) }
    )
}

/**
 * 复习与训练 Box
 */
@Composable
fun ReviewSection(
    isDarkTheme: Boolean,
    cardColor: Color,
    onDueReviewClick: () -> Unit,
    onCategoryPracticeClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("复习与训练")

        PremiumCard {
            Column(modifier = Modifier.padding(8.dp)) {
                SquircleListItem(
                    icon = Icons.Rounded.Schedule,
                    color = NemoPrimary,
                    title = "今日到期复习",
                    subtitle = "核心复习任务",
                    onClick = onDueReviewClick,
                    showDivider = true
                )
                SquircleListItem(
                    icon = Icons.Rounded.SportsEsports,
                    color = NemoSecondary,
                    title = "专项训练",
                    subtitle = "针对性强化练习",
                    onClick = onCategoryPracticeClick,
                    showDivider = false
                )
            }
        }
    }
}

/**
 * 数据与资料 Box
 */
@Composable
fun DataAndResourcesSection(
    isDarkTheme: Boolean,
    cardColor: Color,
    onStatisticsClick: () -> Unit,
    onLearningCalendarClick: () -> Unit,
    onHistoricalStatisticsClick: () -> Unit,
    onWordListClick: () -> Unit,
    onGrammarListClick: () -> Unit,
    onCategoryClassificationClick: () -> Unit,
    onLeechManagementClick: () -> Unit
) {
     Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle("数据与资料")

        PremiumCard {
             Column(modifier = Modifier.padding(8.dp)) {
                SquircleListItem(
                    icon = Icons.Rounded.BubbleChart,
                    color = NemoPrimary,
                    title = "学习日历",
                    subtitle = "查看学习计划与记录",
                    onClick = onLearningCalendarClick
                )
                 SquircleListItem(
                    icon = Icons.Rounded.QueryStats,
                    color = NemoDanger,
                    title = "今日统计",
                    subtitle = "查看今日详细的学习统计",
                    onClick = onStatisticsClick
                )
                SquircleListItem(
                    icon = Icons.Rounded.Insights,
                    color = Color(0xFFAF52DE), // Purple
                    title = "历史统计",
                    subtitle = "查看所有详细的学习统计",
                    onClick = onHistoricalStatisticsClick
                )
                 SquircleListItem(
                    icon = Icons.Rounded.SortByAlpha,
                    color = NemoSecondary,
                    title = "单词列表",
                    subtitle = "词汇库管理",
                    onClick = onWordListClick
                )
                 SquircleListItem(
                    icon = Icons.Rounded.Dataset,
                    color = NemoPrimary,
                    title = "专项词汇",
                    subtitle = "按分类查看词汇",
                    onClick = onCategoryClassificationClick
                )
                SquircleListItem(
                    icon = Icons.Rounded.Book,
                    color = NemoPrimary,
                    title = "语法列表",
                    subtitle = "语法知识库",
                    onClick = onGrammarListClick
                )
                SquircleListItem(
                    icon = Icons.Rounded.AutoFixHigh,
                    color = NemoDanger,
                    title = "复学清单",
                    subtitle = "难点项召回与复习",
                    onClick = onLeechManagementClick,
                    showDivider = false
                )
             }
        }
     }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
    )
}

/**
 * ListItem with "Squircle" Icon (Apple Settings Style)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)), // Squircle approx
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
