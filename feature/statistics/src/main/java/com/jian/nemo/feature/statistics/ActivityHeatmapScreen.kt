package com.jian.nemo.feature.statistics

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader

import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.common.NemoScaffold
import com.jian.nemo.feature.statistics.presentation.components.LearningHeatmapCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip

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

    NemoScaffold(
        title = "学习热力图",
        onBack = onBack,
        backgroundColor = backgroundColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
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
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = Rubik,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isDarkTheme) NemoPrimary.copy(alpha = 0.2f) else Color(0xFFF0F0FF),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents, // Replace with appropriate star-like icon if available
                            contentDescription = null,
                            tint = NemoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
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
                    Text(
                        text = "${data.totalCount} 项",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Stacked Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    data.buckets.forEach { bucket ->
                        if (bucket.ratio > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(bucket.ratio)
                                    .background(Color(android.graphics.Color.parseColor(bucket.color)))
                            )
                        }
                    }
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
                        Text(
                            text = bucket.count.toString(),
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
