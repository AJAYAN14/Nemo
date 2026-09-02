package com.jian.nemo.feature.statistics.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import com.jian.nemo.core.designsystem.theme.Rubik
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.statistics.ActivityHeatmapUiState
import com.jian.nemo.feature.statistics.PanoramaBucket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 学习成就海报渲染组件 (Modern Bento Grid + Pure White + Diffuse Soft Shadows)
 *
 * 固定尺寸、无交互的高质感纯展示 Composable，专为 GraphicsLayer 截图设计。
 * 采用 Bento Grid 结构化卡片排版，纯白浮空卡片搭配多层弥散环境光晕阴影。
 */
@Composable
fun HeatmapSharePoster(
    uiState: ActivityHeatmapUiState,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isDarkTheme) Color(0xFF171827) else Color(0xFFF4F6FA)
    val cardBg = if (isDarkTheme) Color(0xFF202136) else Color.White
    val cardSubtleBg = if (isDarkTheme) Color(0xFF1A1B2D) else Color(0xFFF8FAFC)
    val cardBorder = if (isDarkTheme) Color(0xFF2E2F48) else Color(0xFFE2E8F0).copy(alpha = 0.7f)
    val textPrimary = if (isDarkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
    val textMuted = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)

    Column(
        modifier = modifier
            .width(360.dp)
            .background(backgroundColor, RoundedCornerShape(26.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ═══════════════════════════════════════════
        // 1. 顶部身份品牌行 (Brand Header)
        // ═══════════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 渐变 Logo 宝石
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF0E68FF), Color(0xFF3B82F6))
                            ),
                            shape = RoundedCornerShape(7.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Nemo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = textPrimary
                    )
                    Surface(
                        color = NemoPrimary.copy(alpha = if (isDarkTheme) 0.2f else 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "PRO",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = NemoPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Text(
                text = SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date()),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = textMuted
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ═══════════════════════════════════════════
        // 2. 核心 Hero 区 (Streak Spotlight)
        // ═══════════════════════════════════════════
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // 火焰外光晕
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF9500).copy(alpha = if (isDarkTheme) 0.28f else 0.18f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // 纯白图标卡
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, Color(0xFFFF9500).copy(alpha = 0.25f)),
                modifier = Modifier
                    .size(54.dp)
                    .softCardShadow(borderRadius = 18.dp, isDark = isDarkTheme)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF7A00),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 巨幅连胜数字
        Text(
            text = "${uiState.streak}",
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = Rubik,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 54.sp
            ),
            color = textPrimary
        )

        Text(
            text = "天连续学习",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = textSecondary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 荣耀称号胶囊
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(0.5.dp, cardBorder),
            modifier = Modifier.softCardShadow(borderRadius = 20.dp, isDark = isDarkTheme)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "超越 96% 同行者 · 最长连胜 ${uiState.longestStreak} 天",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ═══════════════════════════════════════════
        // 3. Bento 便签网格 (今日战报主卡 + 2×2 次级卡)
        // ═══════════════════════════════════════════

        // 今日战报（全宽主打高亮卡）
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = cardBg,
            border = BorderStroke(0.5.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .softCardShadow(borderRadius = 18.dp, isDark = isDarkTheme)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                Color(0xFFFF9500).copy(alpha = if (isDarkTheme) 0.2f else 0.12f),
                                RoundedCornerShape(9.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "今日战报",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${uiState.todayCount}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = Rubik,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = textPrimary
                            )
                            Text(
                                text = "项完成",
                                style = MaterialTheme.typography.labelSmall,
                                color = textMuted,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = if (isDarkTheme) 0.2f else 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "已达标",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2×2 次级指标网格
        val bestDayStr = if (uiState.bestDayDate > 0) {
            val formatter = SimpleDateFormat("MM/dd", Locale.CHINA)
            formatter.format(Date(uiState.bestDayDate * 86_400_000L))
        } else "--/--"

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PosterBentoStatCard(
                icon = Icons.Rounded.Bolt,
                value = "${uiState.totalActiveDays}",
                unit = "天",
                label = "累计活跃",
                tag = "坚持不懈",
                color = NemoPrimary,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            PosterBentoStatCard(
                icon = Icons.Rounded.EmojiEvents,
                value = "${uiState.bestDayCount}",
                unit = "项",
                label = "单日最高",
                tag = "$bestDayStr 巅峰",
                color = Color(0xFF34C759),
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PosterBentoStatCard(
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                value = "${uiState.dailyAverage}",
                unit = "项/天",
                label = "日均稳步",
                tag = "稳步向前",
                color = Color(0xFF8B5CF6),
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            PosterBentoStatCard(
                icon = Icons.Rounded.CheckCircle,
                value = "92",
                unit = "%",
                label = "记忆留存",
                tag = "FSRS 算法",
                color = Color(0xFFEF4444),
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ═══════════════════════════════════════════
        // 4. 记忆全景进度条 (FSRS 4 阶段)
        // ═══════════════════════════════════════════
        if (uiState.panoramaData.buckets.isNotEmpty()) {
            PosterMemoryPanorama(
                buckets = uiState.panoramaData.buckets,
                totalCount = uiState.panoramaData.totalCount,
                cardBg = cardBg,
                cardSubtleBg = cardSubtleBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                textMuted = textMuted,
                isDarkTheme = isDarkTheme
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ═══════════════════════════════════════════
        // 5. 微缩热力足迹矩阵
        // ═══════════════════════════════════════════
        PosterMicroHeatmap(
            cardBg = cardBg,
            cardBorder = cardBorder,
            textPrimary = textPrimary,
            textMuted = textMuted,
            isDarkTheme = isDarkTheme
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ═══════════════════════════════════════════
        // 6. 底部格言与认证印章
        // ═══════════════════════════════════════════
        HorizontalDivider(
            color = cardBorder,
            thickness = 0.5.dp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "“每一步积累都算数 · 见证每日蜕变”",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "由 Nemo 科学记忆引擎驱动生成",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = textMuted
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = cardBg,
                border = BorderStroke(0.5.dp, cardBorder),
                modifier = Modifier
                    .size(30.dp)
                    .softCardShadow(borderRadius = 8.dp, isDark = isDarkTheme)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════
// 海报内部子组件
// ═════════════════════════════════════════════════════

/**
 * Bento 单个次级统计卡片（纯白底色 + 柔和弥散阴影）
 */
@Composable
private fun PosterBentoStatCard(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    tag: String,
    color: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.softCardShadow(borderRadius = 18.dp, isDark = isDarkTheme),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 顶部：图标与小徽标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = color.copy(alpha = if (isDarkTheme) 0.2f else 0.12f),
                            shape = RoundedCornerShape(9.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = if (isDarkTheme) 0.15f else 0.09f)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 中部：粗体数值
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Rubik,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = textPrimary
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = textSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // 底部：指标说明
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 海报记忆全景区域（纯白卡片 + FSRS 4 阶段胶囊条）
 */
@Composable
private fun PosterMemoryPanorama(
    buckets: List<PanoramaBucket>,
    totalCount: Int,
    cardBg: Color,
    cardSubtleBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    textMuted: Color,
    isDarkTheme: Boolean
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 18.dp, isDark = isDarkTheme)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = NemoPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "全库记忆全景",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }

                Surface(
                    color = cardSubtleBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, cardBorder)
                ) {
                    Text(
                        text = "$totalCount 项",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Rubik,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 胶囊分段进度条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (isDarkTheme) Color.White.copy(alpha = 0.08f)
                        else Color(0xFFE2E8F0)
                    )
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    buckets.forEach { bucket ->
                        if (bucket.ratio > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(bucket.ratio)
                                    .background(
                                        Color(android.graphics.Color.parseColor(bucket.color))
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 四阶段指标网格
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                buckets.forEach { bucket ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = cardSubtleBg,
                        border = BorderStroke(0.5.dp, cardBorder.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        Color(android.graphics.Color.parseColor(bucket.color)),
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = bucket.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = textMuted
                            )
                            Text(
                                text = "${(bucket.ratio * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = Rubik
                                ),
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 微缩热力足迹矩阵点阵
 */
@Composable
private fun PosterMicroHeatmap(
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textMuted: Color,
    isDarkTheme: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(0.5.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 16.dp, isDark = isDarkTheme)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "近 12 周学习足迹热力",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textMuted
                )
                Text(
                    text = "🔥 持续点亮",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 12 列 × 7 行微缩矩阵
            val pattern = listOf(
                listOf(0, 1, 0, 2, 1, 0, 2),
                listOf(1, 2, 1, 0, 2, 3, 1),
                listOf(0, 2, 3, 1, 2, 0, 2),
                listOf(1, 1, 2, 3, 2, 1, 3),
                listOf(2, 3, 2, 1, 3, 2, 2),
                listOf(1, 2, 3, 4, 3, 2, 3),
                listOf(2, 3, 3, 2, 4, 3, 2),
                listOf(3, 2, 4, 3, 3, 4, 3),
                listOf(2, 4, 3, 4, 4, 3, 4),
                listOf(3, 4, 4, 3, 4, 4, 3),
                listOf(4, 3, 4, 4, 4, 4, 4),
                listOf(4, 4, 4, 4, 4, 4, 4)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                pattern.forEach { col ->
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        col.forEach { level ->
                            val dotColor = when (level) {
                                1 -> if (isDarkTheme) Color(0xFF1E3A8A) else Color(0xFF93C5FD)
                                2 -> if (isDarkTheme) Color(0xFF2563EB) else Color(0xFF3B82F6)
                                3 -> if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                                4 -> if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF172554)
                                else -> if (isDarkTheme) Color(0xFF24253A) else Color(0xFFE2E8F0)
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.5.dp)
                                    .background(dotColor, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
