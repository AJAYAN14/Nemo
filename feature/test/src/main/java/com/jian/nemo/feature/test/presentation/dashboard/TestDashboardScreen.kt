package com.jian.nemo.feature.test.presentation.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.BentoColors
import com.jian.nemo.core.designsystem.theme.NemoDanger
import com.jian.nemo.core.designsystem.theme.NemoIndigo
import com.jian.nemo.core.designsystem.theme.NemoOrange

import com.jian.nemo.core.designsystem.theme.NemoPurple
import com.jian.nemo.core.designsystem.theme.NemoSecondary
import com.jian.nemo.core.designsystem.theme.NemoTeal
import com.jian.nemo.core.ui.component.SlidingDotIndicator
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val CAROUSEL_AUTO_SCROLL_MS = 5000L

/**
 * 测试选择主界面
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TestDashboardScreen(
    viewModel: TestDashboardViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToTestSettings: (String?) -> Unit = {},
    onNavigateToMistakes: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToGrammarTest: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f

    val backgroundColor = if (isDark) colorScheme.background else BentoColors.BgBase

    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarHeight + 16.dp,
                    bottom = navigationBarHeight + 100.dp
                )
        ) {
            ImmersiveTestHeader(title = "测试")

            Spacer(modifier = Modifier.height(20.dp))

            StatsPager(uiState = uiState)

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("复习与回顾")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardTile(
                    modifier = Modifier.weight(1f),
                    title = "我的错题",
                    subtitle = "${uiState.wrongWordsCount} 个",
                    icon = Icons.Rounded.Cancel,
                    color = NemoDanger,
                    onClick = onNavigateToMistakes
                )
                DashboardTile(
                    modifier = Modifier.weight(1f),
                    title = "我的收藏",
                    subtitle = "${uiState.favoriteWordsCount} 个",
                    icon = Icons.Rounded.Star,
                    color = NemoOrange,
                    onClick = onNavigateToFavorites
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("基础练习")
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardTile(
                        modifier = Modifier.weight(1f),
                        title = "选择题",
                        subtitle = "快速认知",
                        icon = Icons.AutoMirrored.Rounded.Assignment,
                        color = NemoSecondary,
                        onClick = { onNavigateToTestSettings("multiple_choice") }
                    )
                    DashboardTile(
                        modifier = Modifier.weight(1f),
                        title = "手打题",
                        subtitle = "拼写强化",
                        icon = Icons.Rounded.TextFields,
                        color = NemoIndigo,
                        onClick = { onNavigateToTestSettings("typing") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardTile(
                        modifier = Modifier.weight(1f),
                        title = "卡片题",
                        subtitle = "翻牌记忆",
                        icon = Icons.Rounded.ViewModule,
                        color = NemoTeal,
                        onClick = { onNavigateToTestSettings("card_matching") }
                    )
                    DashboardTile(
                        modifier = Modifier.weight(1f),
                        title = "排序题",
                        subtitle = "逻辑构建",
                        icon = Icons.Rounded.Extension,
                        color = NemoPurple,
                        onClick = { onNavigateToTestSettings("sorting") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle("挑战自我")
            DashboardBanner(
                title = "综合测试",
                subtitle = "随机组合所有题型进行全面检测",
                icon = Icons.Rounded.AllInclusive,
                gradientColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                onClick = { onNavigateToTestSettings("comprehensive") }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StatsPager(
    uiState: TestDashboardUiState
) {
    data class CompactStatsPage(
        val title: String,
        val icon: ImageVector,
        val accent: Color,
        val primaryValue: String,
        val primaryLabel: String,
        val secondaryValue: String,
        val secondaryUnit: String,
        val secondaryLabel: String,
        val ringValue: Int,
        val ringLabel: String,
        val useSoftRingColor: Boolean = false
    )

    fun ringColorByAccuracy(accuracyPercent: Int): Color {
        return when {
            accuracyPercent < 60 -> NemoDanger
            accuracyPercent < 85 -> NemoOrange
            else -> NemoSecondary
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
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

    val pages = listOf(
        CompactStatsPage(
            title = "今日测试",
            icon = Icons.Rounded.Bolt,
            accent = MaterialTheme.colorScheme.primary,
            primaryValue = uiState.todayTestCount.toString(),
            primaryLabel = "已测题目",
            secondaryValue = uiState.consecutiveTestDays.toString(),
            secondaryUnit = "天",
            secondaryLabel = "连续学习",
            ringValue = (uiState.todayAccuracy * 100).roundToInt(),
            ringLabel = "今日正确率"
        ),
        CompactStatsPage(
            title = "总体统计",
            icon = Icons.Rounded.EmojiEvents,
            accent = NemoDanger,
            primaryValue = uiState.totalTestCount.toString(),
            primaryLabel = "累计测试",
            secondaryValue = uiState.maxTestStreak.toString(),
            secondaryUnit = "天",
            secondaryLabel = "最高连签",
            ringValue = (uiState.overallAccuracy * 100).roundToInt(),
            ringLabel = "累计正确率",
            useSoftRingColor = true
        )
    )

    val activePage = pages[pagerState.currentPage % pages.size]
    val visualPageIndex = pagerState.currentPage % pages.size

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 10.dp,
            contentPadding = PaddingValues(horizontal = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val pageData = pages[page % pages.size]
            val baseRingColor = ringColorByAccuracy(pageData.ringValue)
            val ringColor = if (pageData.useSoftRingColor) baseRingColor.copy(alpha = 0.9f) else baseRingColor

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(pageData.accent, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = pageData.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = pageData.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CompactStatItem(
                                value = pageData.primaryValue,
                                label = pageData.primaryLabel,
                                valueColor = MaterialTheme.colorScheme.onSurface,
                                isDark = isDark
                            )
                            CompactStatItem(
                                value = pageData.secondaryValue,
                                label = pageData.secondaryLabel,
                                unit = pageData.secondaryUnit,
                                valueColor = pageData.accent,
                                isDark = isDark
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.size(84.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else Color(0xFFEFF3F8),
                                    strokeWidth = 10.dp,
                                    trackColor = Color.Transparent
                                )
                                CircularProgressIndicator(
                                    progress = { (pageData.ringValue / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxSize(),
                                    color = ringColor,
                                    strokeWidth = 10.dp,
                                    trackColor = Color.Transparent
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = pageData.ringValue.toString(),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = ringColor,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pageData.ringLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                    }

                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            SlidingDotIndicator(
                pagerState = pagerState,
                pageCount = 2,
                activeColor = activePage.accent,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${visualPageIndex + 1}/${pages.size}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun CompactStatItem(
    value: String,
    label: String,
    valueColor: Color,
    isDark: Boolean,
    unit: String = ""
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color(0xFFF8FAFC),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFeatureSettings = "tnum"
                    ),
                    color = valueColor
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun DashboardTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else BentoColors.Surface
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else BentoColors.TextMain
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else BentoColors.TextSub

    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = textMain,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = textSub,
                    lineHeight = 14.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun DashboardBanner(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else BentoColors.Surface
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else BentoColors.TextMain
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else BentoColors.TextSub

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = gradientColors.first().copy(alpha = 0.08f),
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = textMain
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = textSub
                )
            }
        }
    }
}
