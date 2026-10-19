package com.jian.nemo.feature.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 积压排产与微会话控制卡片 (Backlog & Micro-Session Controller Card)
 * 严格遵循方案 V2.4 三层控制架构之第二层（高级记忆算法调校）
 */
@Composable
fun BacklogControllerCard(
    sessionBatchSize: Int,
    onBatchSizeChange: (Int) -> Unit,
    maxDailyReviews: Int,
    onMaxReviewsChange: (Int) -> Unit,
    strategy: String,
    onStrategyChange: (String) -> Unit,
    accentColor: Color = Color(0xFF34C759),
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 头部标题与说明 (纯文字左对齐，与全页面各卡片排版规范统一)
            Column {
                Text(
                    text = "积压排产与微会话",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "物理防滚雪球收敛，3~5 分钟微会话保底打卡",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. 单组微会话容量选择
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "单组微会话容量",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$sessionBatchSize 词 / 组",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
                Text(
                    text = "每组只排少量词汇，极速完成即点亮今日连续打卡",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val batchOptions = listOf(10, 15, 20, 25, 30)
                    batchOptions.forEach { size ->
                        val selected = sessionBatchSize == size
                        val bg by animateColorAsState(
                            if (selected) accentColor.copy(alpha = if (isDark) 0.22f else 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            label = "batchBg"
                        )
                        val textColor by animateColorAsState(
                            if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                            label = "batchText"
                        )

                        Surface(
                            onClick = { onBatchSizeChange(size) },
                            shape = RoundedCornerShape(12.dp),
                            color = bg,
                            border = BorderStroke(
                                1.dp,
                                if (selected) accentColor else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$size",
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // 2. 每日复习上限
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "每日复习上限",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (maxDailyReviews == 0) "不设限制" else "$maxDailyReviews 词",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
                Text(
                    text = "设定单日复习最大安全负荷，超出部分由系统智能平滑后推",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val reviewLimitOptions = listOf(0 to "不限", 40 to "40 词", 60 to "60 词", 80 to "80 词")
                    reviewLimitOptions.forEach { (limit, label) ->
                        val selected = maxDailyReviews == limit
                        val bg by animateColorAsState(
                            if (selected) accentColor.copy(alpha = if (isDark) 0.22f else 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            label = "limitBg"
                        )
                        val textColor by animateColorAsState(
                            if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
                            label = "limitText"
                        )

                        Surface(
                            onClick = { onMaxReviewsChange(limit) },
                            shape = RoundedCornerShape(12.dp),
                            color = bg,
                            border = BorderStroke(
                                1.dp,
                                if (selected) accentColor else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // 3. 积压期新词策略
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "积压期新词策略",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "当待复习队列积压时，每日首组会话的新词投放策略",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 选项 A: 微量尝鲜
                    val isTasteSelected = strategy != "review_only"
                    StrategyOptionCard(
                        title = "微量尝鲜",
                        desc = "穿插 2~3 词新知，保持新鲜感",
                        selected = isTasteSelected,
                        accentColor = accentColor,
                        onClick = { onStrategyChange("taste") },
                        modifier = Modifier.weight(1f)
                    )

                    // 选项 B: 纯复习优先
                    val isReviewOnlySelected = strategy == "review_only"
                    StrategyOptionCard(
                        title = "纯复习优先",
                        desc = "暂停新词，全力冲刺清偿旧账",
                        selected = isReviewOnlySelected,
                        accentColor = accentColor,
                        onClick = { onStrategyChange("review_only") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StrategyOptionCard(
    title: String,
    desc: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bg by animateColorAsState(
        if (selected) accentColor.copy(alpha = if (isDark) 0.16f else 0.10f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
        label = "optBg"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(
            1.dp,
            if (selected) accentColor.copy(alpha = 0.8f) else Color.Transparent
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) accentColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        }
    }
}
