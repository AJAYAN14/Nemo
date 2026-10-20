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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.dialog.CommonConfigDialog

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
    var showBatchDialog by remember { mutableStateOf(false) }
    var showReviewLimitDialog by remember { mutableStateOf(false) }

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

                    // 自定义单组微会话容量
                    val isCustomBatch = sessionBatchSize !in batchOptions
                    val customBg by animateColorAsState(
                        if (isCustomBatch) accentColor.copy(alpha = if (isDark) 0.22f else 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        label = "customBatchBg"
                    )
                    val customTextColor by animateColorAsState(
                        if (isCustomBatch) accentColor else MaterialTheme.colorScheme.onSurface,
                        label = "customBatchText"
                    )

                    Surface(
                        onClick = { showBatchDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = customBg,
                        border = BorderStroke(
                            1.dp,
                            if (isCustomBatch) accentColor else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isCustomBatch) "$sessionBatchSize" else "自定义",
                                fontSize = 12.sp,
                                fontWeight = if (isCustomBatch) FontWeight.Bold else FontWeight.Medium,
                                color = customTextColor
                            )
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

                    // 自定义每日复习上限
                    val isCustomReviewLimit = maxDailyReviews !in listOf(0, 40, 60, 80)
                    val customReviewBg by animateColorAsState(
                        if (isCustomReviewLimit) accentColor.copy(alpha = if (isDark) 0.22f else 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        label = "customLimitBg"
                    )
                    val customReviewTextColor by animateColorAsState(
                        if (isCustomReviewLimit) accentColor else MaterialTheme.colorScheme.onSurface,
                        label = "customLimitText"
                    )

                    Surface(
                        onClick = { showReviewLimitDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = customReviewBg,
                        border = BorderStroke(
                            1.dp,
                            if (isCustomReviewLimit) accentColor else Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isCustomReviewLimit) "$maxDailyReviews 词" else "自定义",
                                fontSize = 12.sp,
                                fontWeight = if (isCustomReviewLimit) FontWeight.Bold else FontWeight.Medium,
                                color = customReviewTextColor
                            )
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

    // 自定义单组微会话容量弹窗 (加餐同款通用配置弹窗 - 统一绿色调)
    if (showBatchDialog) {
        CommonConfigDialog(
            headerIcon = Icons.Rounded.Layers,
            headerIconTint = accentColor,
            title = "单组微会话容量",
            subtitle = "自定义每组排产词数，极速完成即点亮连续打卡",
            initialValue = sessionBatchSize,
            min = 5,
            max = 100,
            step = 1,
            presets = listOf(10, 15, 20, 25, 30, 50),
            presetLabelFormatter = { "$it 词" },
            confirmText = "确认保存",
            confirmButtonColor = accentColor,
            onDismissRequest = { showBatchDialog = false },
            onConfirm = { newVal ->
                onBatchSizeChange(newVal)
                showBatchDialog = false
            }
        )
    }

    // 自定义每日复习上限弹窗 (加餐同款通用配置弹窗 - 统一绿色调)
    if (showReviewLimitDialog) {
        CommonConfigDialog(
            headerIcon = Icons.Rounded.Shield,
            headerIconTint = accentColor,
            title = "每日复习上限",
            subtitle = "设定单日最大安全负荷（输入 0 表示不设限制）",
            initialValue = maxDailyReviews,
            min = 0,
            max = 500,
            step = 5,
            presets = listOf(0, 30, 50, 80, 100, 150),
            presetLabelFormatter = { if (it == 0) "不限" else "$it 词" },
            confirmText = "确认保存",
            confirmButtonColor = accentColor,
            onDismissRequest = { showReviewLimitDialog = false },
            onConfirm = { newVal ->
                onMaxReviewsChange(newVal)
                showReviewLimitDialog = false
            }
        )
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
