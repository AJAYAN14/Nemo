package com.jian.nemo.feature.statistics

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader

import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.feature.statistics.presentation.components.HeatmapShareBottomSheet
import com.jian.nemo.feature.statistics.presentation.components.LearningHeatmapCard
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.text.TextStyle

/**
 * 学习热力图与数据高光界面
 */
@Composable
fun ActivityHeatmapScreen(
    onBack: () -> Unit,
    viewModel: ActivityHeatmapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    var showShareSheet by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            item {
                Column {
                    HeatmapSectionTitle(stringResource(R.string.stats_heatmap_title_annual_review))
                    if (uiState.isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            NemoChasingDotsLoader()
                        }
                    } else {
                        LearningHeatmapCard(
                            heatmapData = uiState.heatmapData,
                            isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f,
                            cardColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.surfaceContainer else Color.White
                        )
                    }
                }
            }

            item {
                if (!uiState.isLoading) {
                    Column {
                        HeatmapSectionTitle(stringResource(R.string.stats_heatmap_title_memory_panorama))
                        MemoryPanoramaCard(
                            data = uiState.panoramaData,
                            isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        )
                    }
                }
            }

            if (!uiState.isLoading) {
                item {
                    Column {
                        HeatmapSectionTitle(stringResource(R.string.stats_heatmap_title_data_highlights))
                        RichStatsGrid(
                            streak = uiState.streak,
                            longestStreak = uiState.longestStreak,
                            totalActiveDays = uiState.totalActiveDays,
                            bestDayCount = uiState.bestDayCount,
                            bestDayDate = uiState.bestDayDate,
                            dailyAverage = uiState.dailyAverage,
                            todayCount = uiState.todayCount
                        )
                    }
                }

                // 3. Motivational Footer
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.stats_heatmap_banner_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.60f),
                            backgroundColor.copy(alpha = 0.35f),
                            backgroundColor.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = stringResource(R.string.stats_heatmap_title_screen),
                onBack = onBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                actions = {
                    if (!uiState.isLoading) {
                        FrostedLiquidGlassButton(
                            onClick = { showShareSheet = true },
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share_ios),
                                contentDescription = stringResource(R.string.stats_heatmap_cd_share),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
        }
    }
    }

    // 分享底板
    if (showShareSheet) {
        HeatmapShareBottomSheet(
            uiState = uiState,
            onDismiss = { showShareSheet = false }
        )
    }
}

// Re-using the RichStatsGrid logic here (copied from previous iteration)
@Composable
private fun RichStatsGrid(
    streak: Int,
    longestStreak: Int,
    totalActiveDays: Int,
    bestDayCount: Int,
    bestDayDate: Long,
    dailyAverage: Int,
    todayCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Streak
            RichStatItem(
                icon = Icons.Rounded.LocalFireDepartment,
                label = stringResource(R.string.stats_heatmap_label_current_streak),
                value = streak.toString(),
                unit = stringResource(R.string.stats_heatmap_unit_days),
                subLabel = stringResource(R.string.stats_heatmap_format_longest_streak, longestStreak),
                color = IosColors.Orange,
                modifier = Modifier.weight(1f)
            )

            // Total Days
            RichStatItem(
                icon = Icons.Rounded.Bolt,
                label = stringResource(R.string.stats_heatmap_label_active_days),
                value = totalActiveDays.toString(),
                unit = stringResource(R.string.stats_heatmap_unit_days),
                subLabel = stringResource(R.string.stats_heatmap_label_unremitting),
                color = NemoPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Best Day
            val dateStr = if (bestDayDate > 0) {
                 val formatter = SimpleDateFormat("MM/dd", Locale.CHINA)
                 formatter.format(Date(bestDayDate * 86_400_000L))
            } else "--/--"

            RichStatItem(
                icon = Icons.Rounded.EmojiEvents,
                label = stringResource(R.string.stats_heatmap_label_best_day),
                value = bestDayCount.toString(),
                unit = stringResource(R.string.stats_heatmap_unit_items),
                subLabel = dateStr,
                color = IosColors.Green,
                modifier = Modifier.weight(1f)
            )

             // Daily Average
             RichStatItem(
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                label = stringResource(R.string.stats_heatmap_label_daily_avg),
                value = dailyAverage.toString(),
                unit = stringResource(R.string.stats_heatmap_unit_items),
                subLabel = if (todayCount >= dailyAverage && dailyAverage > 0) {
                    stringResource(R.string.stats_heatmap_label_above_avg)
                } else {
                    stringResource(R.string.stats_heatmap_label_steady_progress)
                },
                color = IosColors.Purple,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RichStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    subLabel: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    Surface(
        shape = RoundedRectangle(22.dp),
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 22.dp, isDark = isDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val iconStyle = LocalSettingIconStyle.current
            val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
            val containerBg = if (isSoftTranslucent) color.copy(alpha = if (isDark) 0.20f else 0.15f) else color
            val iconTint = if (isSoftTranslucent) color else Color.White

            // 顶部：Squircle 语义图标 + 胶囊状态标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = containerBg,
                            shape = RoundedRectangle(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp) // 严格保持 2:3 黄金比例 (36 * 2/3 = 24.dp)
                    )
                }

                if (subLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedRectangle(6.dp),
                        color = color.copy(alpha = if (isDark) 0.15f else 0.08f)
                    ) {
                        Text(
                            text = subLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // 中部与底部：粗体数据与淡雅指标说明
            Column {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val intValue = value.toIntOrNull()
                    if (intValue != null) {
                        EntranceCountUpText(
                            targetValue = intValue,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = Rubik,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = Rubik,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HeatmapSectionTitle(
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

@Composable
private fun MemoryPanoramaCard(
    data: MemoryPanoramaData,
    isDarkTheme: Boolean
) {
    val containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else Color.White


    Surface(
        shape = RoundedRectangle(26.dp),
        color = containerColor,

        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 26.dp, isDark = isDarkTheme)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header: Icon and Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val iconStyle = LocalSettingIconStyle.current
                    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
                    val baseColor = IosColors.Green
                    val containerBg = if (isSoftTranslucent) {
                        if (isDarkTheme) baseColor.copy(alpha = 0.20f) else StatisticsPalette.EmeraldBgLight
                    } else {
                        baseColor
                    }
                    val iconTint = if (isSoftTranslucent) {
                        if (isDarkTheme) StatisticsPalette.EmeraldTextDark else StatisticsPalette.EmeraldTextLight
                    } else {
                        Color.White
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = containerBg,
                                shape = RoundedRectangle(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp) // 严格 2:3 黄金比例 (36 * 2/3 = 24.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.stats_heatmap_title_panorama),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedRectangle(8.dp)
                ) {
                    EntranceCountUpText(
                        targetValue = data.totalCount,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        suffix = stringResource(R.string.stats_heatmap_suffix_items),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stacked Progress Bar with Smooth Gradient Transition
            val validBuckets = remember(data.buckets) { data.buckets.filter { it.ratio > 0f } }
            val barBrush = remember(validBuckets) {
                if (validBuckets.isEmpty()) {
                    null
                } else if (validBuckets.size == 1) {
                    val color = runCatching { Color(android.graphics.Color.parseColor(validBuckets[0].color)) }
                        .getOrDefault(NemoSecondary)
                    Brush.horizontalGradient(listOf(color, color))
                } else {
                    val totalRatio = validBuckets.sumOf { it.ratio.toDouble() }.toFloat().coerceAtLeast(0.001f)
                    val rawStops = mutableListOf<Pair<Float, Color>>()
                    var currentProgress = 0f

                    for (i in validBuckets.indices) {
                        val bucket = validBuckets[i]
                        val bucketColor = runCatching { Color(android.graphics.Color.parseColor(bucket.color)) }
                            .getOrDefault(NemoPrimary)
                        val span = (bucket.ratio / totalRatio).coerceIn(0f, 1f)
                        val startPos = currentProgress
                        val endPos = (currentProgress + span).coerceAtMost(1f)
                        val blendMargin = minOf(0.04f, span * 0.25f)

                        if (i == 0) {
                            rawStops.add(0f to bucketColor)
                        } else {
                            rawStops.add((startPos - blendMargin).coerceAtLeast(0f) to rawStops.last().second)
                            rawStops.add((startPos + blendMargin).coerceAtMost(1f) to bucketColor)
                        }

                        if (i == validBuckets.lastIndex) {
                            rawStops.add(1f to bucketColor)
                        } else {
                            rawStops.add((endPos - blendMargin).coerceAtLeast(0f) to bucketColor)
                        }

                        currentProgress = endPos
                    }

                    val sanitizedStops = mutableListOf<Pair<Float, Color>>()
                    var lastPos = -1f
                    rawStops.sortedBy { it.first }.forEach { (pos, color) ->
                        val safePos = pos.coerceIn(0f, 1f)
                        if (safePos > lastPos) {
                            sanitizedStops.add(safePos to color)
                            lastPos = safePos
                        }
                    }

                    if (sanitizedStops.size >= 2) {
                        Brush.horizontalGradient(colorStops = sanitizedStops.toTypedArray())
                    } else {
                        val fallbackColor = runCatching { Color(android.graphics.Color.parseColor(validBuckets[0].color)) }
                            .getOrDefault(NemoSecondary)
                        Brush.horizontalGradient(listOf(fallbackColor, fallbackColor))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedRectangle(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                if (barBrush != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(barBrush)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.buckets.forEach { bucket ->
                    LegendItem(bucket)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Divider
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Grid Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.buckets.forEach { bucket ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        EntranceCountUpText(
                            targetValue = bucket.count,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = Rubik,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(bucket.ratio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = Rubik,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(bucket: PanoramaBucket) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = Color(android.graphics.Color.parseColor(bucket.color)),
                    shape = RoundedRectangle(2.dp)
                )
        )
        Text(
            text = bucket.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 带有优雅入场数流攀升效果的数字组件 (Entrance Spring CountUp)
 * 仅在页面进入或目标值首次加载时播放一次平滑入场动效，快起缓停，带轻微落盘弹簧反馈
 */
@Composable
private fun EntranceCountUpText(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    durationMillis: Int = 850,
    prefix: String = "",
    suffix: String = "",
    useGrouping: Boolean = true,
    marqueeMinLength: Int = 5
) {
    val animatedProgress = remember { Animatable(0f) }
    val arrivalScale = remember { Animatable(1f) }

    LaunchedEffect(targetValue) {
        if (targetValue > 0) {
            animatedProgress.snapTo(0f)
            arrivalScale.snapTo(1f)
            animatedProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            )
            arrivalScale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(90, easing = LinearOutSlowInEasing)
            )
            arrivalScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
            )
        } else {
            animatedProgress.snapTo(1f)
            arrivalScale.snapTo(1f)
        }
    }

    val currentValue = (targetValue * animatedProgress.value).toInt()
    val formattedNumber = if (useGrouping && targetValue >= 1000) {
        String.format(Locale.US, "%,d", currentValue)
    } else {
        currentValue.toString()
    }

    val fullText = "$prefix$formattedNumber$suffix"
    val shouldMarquee = fullText.length >= marqueeMinLength

    Text(
        text = fullText,
        style = style.copy(
            fontFeatureSettings = "tnum"
        ),
        color = color,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .scale(arrivalScale.value)
            .then(
                if (shouldMarquee) {
                    Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1200,
                        repeatDelayMillis = 1200
                    )
                } else {
                    Modifier
                }
            )
    )
}

