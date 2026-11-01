package com.jian.nemo.feature.test.presentation.dashboard

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.test.R
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
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.Rubik
import com.jian.nemo.core.ui.animation.containerTransform
import com.jian.nemo.core.ui.component.progress.NemoCircularProgress
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
    onNavigateToGrammarTest: () -> Unit = {},
    onNavigateToAbilityWorkshop: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val isAbilityWorkshopEnabled = true

    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
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
            ImmersiveTestHeader(title = stringResource(R.string.test_dashboard_title_screen))

            Spacer(modifier = Modifier.height(20.dp))

            StatsPager(uiState = uiState)

            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.test_dashboard_section_review))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardTile(
                    modifier = Modifier
                        .weight(1f)
                        .containerTransform(
                            key = "container_mistakes",
                            shape = RoundedRectangle(24.dp)
                        ),
                    title = stringResource(R.string.test_dashboard_item_mistakes),
                    subtitle = stringResource(R.string.test_dashboard_format_count_items, uiState.wrongWordsCount),
                    icon = Icons.Rounded.Cancel,
                    color = IosColors.Red,
                    onClick = onNavigateToMistakes
                )
                DashboardTile(
                    modifier = Modifier
                        .weight(1f)
                        .containerTransform(
                            key = "container_favorites",
                            shape = RoundedRectangle(24.dp)
                        ),
                    title = stringResource(R.string.test_dashboard_item_favorites),
                    subtitle = stringResource(R.string.test_dashboard_format_count_items, uiState.favoriteWordsCount),
                    icon = Icons.Rounded.Star,
                    color = IosColors.Orange,
                    onClick = onNavigateToFavorites
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.test_dashboard_section_basic))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardTile(
                        modifier = Modifier
                            .weight(1f)
                            .containerTransform(
                                key = "container_test_multiple_choice",
                                shape = RoundedRectangle(24.dp)
                            ),
                        title = stringResource(R.string.test_dashboard_type_choice_title),
                        subtitle = stringResource(R.string.test_dashboard_type_choice_subtitle),
                        icon = Icons.AutoMirrored.Rounded.Assignment,
                        color = IosColors.Green,
                        onClick = { onNavigateToTestSettings("multiple_choice") }
                    )
                    DashboardTile(
                        modifier = Modifier
                            .weight(1f)
                            .containerTransform(
                                key = "container_test_typing",
                                shape = RoundedRectangle(24.dp)
                            ),
                        title = stringResource(R.string.test_dashboard_type_typing_title),
                        subtitle = stringResource(R.string.test_dashboard_type_typing_subtitle),
                        icon = Icons.Rounded.TextFields,
                        color = IosColors.Indigo,
                        onClick = { onNavigateToTestSettings("typing") }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DashboardTile(
                        modifier = Modifier
                            .weight(1f)
                            .containerTransform(
                                key = "container_test_card_matching",
                                shape = RoundedRectangle(24.dp)
                            ),
                        title = stringResource(R.string.test_dashboard_type_card_title),
                        subtitle = stringResource(R.string.test_dashboard_type_card_subtitle),
                        icon = Icons.Rounded.ViewModule,
                        color = IosColors.Teal,
                        onClick = { onNavigateToTestSettings("card_matching") }
                    )
                    DashboardTile(
                        modifier = Modifier
                            .weight(1f)
                            .containerTransform(
                                key = "container_test_sorting",
                                shape = RoundedRectangle(24.dp)
                            ),
                        title = stringResource(R.string.test_dashboard_type_sorting_title),
                        subtitle = stringResource(R.string.test_dashboard_type_sorting_subtitle),
                        icon = Icons.Rounded.Extension,
                        color = IosColors.Purple,
                        onClick = { onNavigateToTestSettings("sorting") }
                    )
                }
            }

            if (isAbilityWorkshopEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle(stringResource(R.string.test_dashboard_section_other))
                DashboardBanner(
                    modifier = Modifier.containerTransform(
                        key = "container_ability_workshop",
                        shape = RoundedRectangle(24.dp)
                    ),
                    title = stringResource(R.string.test_dashboard_workshop_title),
                    subtitle = stringResource(R.string.test_dashboard_workshop_subtitle),
                    icon = Icons.Rounded.AllInclusive,
                    gradientColors = listOf(IosColors.Blue, IosColors.Blue.copy(alpha = 0.8f)),
                    onClick = onNavigateToAbilityWorkshop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
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

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    fun ringColorByAccuracy(accuracyPercent: Int): Color {
        return when {
            accuracyPercent < 60 -> IosColors.Red
            accuracyPercent < 85 -> IosColors.Orange
            else -> IosColors.Green
        }
    }
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
            title = stringResource(R.string.test_dashboard_stat_today_title),
            icon = Icons.Rounded.Bolt,
            accent = IosColors.Blue,
            primaryValue = uiState.todayTestCount.toString(),
            primaryLabel = stringResource(R.string.test_dashboard_stat_tested_questions),
            secondaryValue = uiState.consecutiveTestDays.toString(),
            secondaryUnit = stringResource(R.string.test_dashboard_stat_unit_day),
            secondaryLabel = stringResource(R.string.test_dashboard_stat_streak_learn),
            ringValue = (uiState.todayAccuracy * 100).roundToInt(),
            ringLabel = stringResource(R.string.test_dashboard_stat_accuracy_today)
        ),
        CompactStatsPage(
            title = stringResource(R.string.test_dashboard_stat_total_title),
            icon = Icons.Rounded.EmojiEvents,
            accent = IosColors.Orange,
            primaryValue = uiState.totalTestCount.toString(),
            primaryLabel = stringResource(R.string.test_dashboard_stat_total_tests),
            secondaryValue = uiState.maxTestStreak.toString(),
            secondaryUnit = stringResource(R.string.test_dashboard_stat_unit_day),
            secondaryLabel = stringResource(R.string.test_dashboard_stat_max_streak),
            ringValue = (uiState.overallAccuracy * 100).roundToInt(),
            ringLabel = stringResource(R.string.test_dashboard_stat_accuracy_total),
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
                shape = RoundedRectangle(24.dp),
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
                            val iconStyle = LocalSettingIconStyle.current
                            val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
                            val headerContainerBg = if (isSoftTranslucent) pageData.accent.copy(alpha = 0.15f) else pageData.accent
                            val headerIconTint = if (isSoftTranslucent) pageData.accent else Color.White

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(headerContainerBg, RoundedRectangle(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = pageData.icon,
                                    contentDescription = null,
                                    tint = headerIconTint,
                                    modifier = Modifier.size(24.dp)
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
                                NemoCircularProgress(
                                    progress = pageData.ringValue / 100f,
                                    modifier = Modifier.fillMaxSize(),
                                    color = ringColor,
                                    trackColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceVariant,
                                    strokeWidth = 10.dp
                                )
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = pageData.ringValue.toString(),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = Rubik,
                                            fontWeight = FontWeight.Bold,
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
        shape = RoundedRectangle(24.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
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
                        fontFamily = Rubik,
                        fontWeight = FontWeight.Bold,
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
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
    val containerBgColor = if (isSoftTranslucent) color.copy(alpha = 0.15f) else color
    val iconTint = if (isSoftTranslucent) color else Color.White

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surfaceColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else BentoColors.Surface
    val textMain = if (isDark) MaterialTheme.colorScheme.onSurface else BentoColors.TextMain
    val textSub = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else BentoColors.TextSub

    Card(
        modifier = modifier
            .clip(RoundedRectangle(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedRectangle(24.dp),
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
    modifier: Modifier = Modifier,
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
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedRectangle(24.dp),
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
