package com.jian.nemo.feature.statistics

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
 * 学习热力图与数据高光专属界面 (Activity Heatmap Pro Max)
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
                    HeatmapSectionTitle("年度回顾")
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
                        HeatmapSectionTitle("记忆全景")
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
                        HeatmapSectionTitle("数据高光")
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
                            text = "每一天都在进步，保持连胜！",
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
                title = "学习热力图",
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
                                contentDescription = "分享",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
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
                plumpIcon = PlumpIcon.Flame,
                isoluminantColor = IsoluminantPalette.WarmAmber,
                label = "当前坚持",
                value = streak.toString(),
                unit = "天",
                subLabel = "最长 $longestStreak 天",
                color = Color(0xFFFF9500), // Vibrant Orange
                modifier = Modifier.weight(1f)
            )

            // Total Days
            RichStatItem(
                icon = Icons.Rounded.Bolt,
                plumpIcon = PlumpIcon.Zap,
                isoluminantColor = IsoluminantPalette.SkyIndigo,
                label = "累计活跃",
                value = totalActiveDays.toString(),
                unit = "天",
                subLabel = "坚持不懈",
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
                plumpIcon = PlumpIcon.Trophy,
                isoluminantColor = IsoluminantPalette.EmeraldGreen,
                label = "单日最佳",
                value = bestDayCount.toString(),
                unit = "项",
                subLabel = dateStr,
                color = Color(0xFF34C759), // Success Green
                modifier = Modifier.weight(1f)
            )

             // Daily Average
             RichStatItem(
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                plumpIcon = PlumpIcon.TrendingUp,
                isoluminantColor = IsoluminantPalette.LilacViolet,
                label = "日均学习",
                value = dailyAverage.toString(),
                unit = "项",
                subLabel = if (todayCount >= dailyAverage && dailyAverage > 0) "超过平均" else "稳步前进",
                color = Color(0xFF8B5CF6), // Premium Purple
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun RichStatItem(
    icon: ImageVector,
    plumpIcon: PlumpIcon? = null,
    isoluminantColor: Color? = null,
    label: String,
    value: String,
    unit: String,
    subLabel: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 22.dp, isDark = isDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部：Squircle 语义图标 + 胶囊状态标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO && plumpIcon != null && isoluminantColor != null) {
                    PlumpDuoIcon(
                        icon = plumpIcon,
                        color = isoluminantColor,
                        size = 28.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = color.copy(alpha = if (isDark) 0.2f else 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (subLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
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
private fun HeatmapSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun MemoryPanoramaCard(
    data: MemoryPanoramaData,
    isDarkTheme: Boolean
) {
    val containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDarkTheme) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor),
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
                    if (LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO) {
                        PlumpDuoIcon(
                            icon = PlumpIcon.BrainCircuit,
                            color = IsoluminantPalette.EmeraldGreen,
                            size = 28.dp
                        )
                    } else {
                        val emeraldTint = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF059669)
                        val emeraldBackground = if (isDarkTheme) Color(0xFF10B981).copy(alpha = 0.20f) else Color(0xFFECFDF5)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = emeraldBackground,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = emeraldTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "全库记忆全景",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    EntranceCountUpText(
                        targetValue = data.totalCount,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        suffix = " 项",
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
                        .getOrDefault(Color(0xFF10B981))
                    Brush.horizontalGradient(listOf(color, color))
                } else {
                    val totalRatio = validBuckets.sumOf { it.ratio.toDouble() }.toFloat().coerceAtLeast(0.001f)
                    val rawStops = mutableListOf<Pair<Float, Color>>()
                    var currentProgress = 0f

                    for (i in validBuckets.indices) {
                        val bucket = validBuckets[i]
                        val bucketColor = runCatching { Color(android.graphics.Color.parseColor(bucket.color)) }
                            .getOrDefault(Color(0xFF3B82F6))
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
                            .getOrDefault(Color(0xFF10B981))
                        Brush.horizontalGradient(listOf(fallbackColor, fallbackColor))
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
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
                    shape = RoundedCornerShape(2.dp)
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

