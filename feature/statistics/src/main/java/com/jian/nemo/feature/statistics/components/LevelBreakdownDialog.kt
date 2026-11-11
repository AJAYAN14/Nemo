package com.jian.nemo.feature.statistics.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.jian.nemo.feature.statistics.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.designsystem.theme.Rubik
import com.jian.nemo.feature.statistics.LevelPredictionInfo
import com.jian.nemo.feature.statistics.OverallPredictionSummary
import kotlinx.coroutines.delay

/**
 * 等级分布弹窗
 * 
 * 展示单词/语法的等级占比，支持点击等级卡片展开预测达成日期与3秒自动收起
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LevelBreakdownDialog(
    title: String,
    data: Map<String, Int>,
    totalData: Map<String, Int>,
    themeColor: Color,
    predictions: Map<String, LevelPredictionInfo> = emptyMap(),
    overallPrediction: OverallPredictionSummary? = null,
    itemUnit: String = stringResource(R.string.stats_breakdown_unit_word_default),
    onDismiss: () -> Unit
) {
    val totalCount = remember(data) { data.values.sum() }
    val grandTotalCount = remember(totalData) { totalData.values.sum() }
    val grandPercentage = remember(totalCount, grandTotalCount) {
        if (grandTotalCount > 0) {
            (totalCount.toFloat() / grandTotalCount * 100).toInt()
        } else {
            0
        }
    }

    var expandedLevel by remember { mutableStateOf<String?>(null) }
    var isPinned by remember { mutableStateOf(false) }

    // 仅在非固定（单击触发）时，3 秒后自动收起
    LaunchedEffect(expandedLevel, isPinned) {
        if (expandedLevel != null && !isPinned) {
            delay(3000L)
            expandedLevel = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current

        // 开启 Android 12+ 官方窗口级高斯毛玻璃 (Blur Behind)
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = 48
                    dimAmount = 0.20f
                }
            }
            onDispose {
                if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        window.attributes = window.attributes.apply {
                            blurBehindRadius = 0
                            dimAmount = 0f
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 350.dp),
            shape = RoundedRectangle(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Title + Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = (-0.5).sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedRectangle(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 顶层极简扁平进度卡片
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), RoundedRectangle(14.dp))
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedRectangle(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.stats_breakdown_label_mastery_ratio, totalCount, grandTotalCount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$grandPercentage%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val animateGrandProgress by animateFloatAsState(
                        targetValue = if (grandTotalCount > 0) totalCount.toFloat() / grandTotalCount else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessVeryLow, dampingRatio = Spring.DampingRatioLowBouncy),
                        label = "grandProgress"
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedRectangle(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animateGrandProgress)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(themeColor.copy(alpha = 0.7f), themeColor)
                                    ),
                                    shape = RoundedRectangle(3.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Data Rows (N5 to N1)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val levelList = listOf("N5", "N4", "N3", "N2", "N1")
                    val levelColors = mapOf(
                        "N1" to SystemColors.Red,
                        "N2" to SystemColors.Orange,
                        "N3" to SystemColors.Yellow,
                        "N4" to SystemColors.Green,
                        "N5" to SystemColors.Blue
                    )

                    levelList.forEach { level ->
                        val count = data[level] ?: 0
                        val total = totalData[level] ?: 0
                        val color = levelColors[level] ?: themeColor
                        val prediction = predictions[level]
                        val isExpanded = expandedLevel == level

                        val progress = if (total > 0) count.toFloat() / total else 0f
                        val animateProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = spring(stiffness = Spring.StiffnessVeryLow, dampingRatio = Spring.DampingRatioLowBouncy),
                            label = "levelProgress_$level"
                        )

                        Surface(
                            shape = RoundedRectangle(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f),
                            border = BorderStroke(
                                if (isExpanded) (if (isPinned) 1.5.dp else 1.dp) else 0.5.dp,
                                if (isExpanded) color.copy(alpha = if (isPinned) 0.85f else 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedRectangle(14.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (isExpanded) {
                                            expandedLevel = null
                                            isPinned = false
                                        } else {
                                            expandedLevel = level
                                            isPinned = false
                                        }
                                    },
                                    onLongClick = {
                                        if (isExpanded && isPinned) {
                                            expandedLevel = null
                                            isPinned = false
                                        } else {
                                            expandedLevel = level
                                            isPinned = true
                                        }
                                    }
                                )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    // 1. 流光渐变进度底色
                                    if (animateProgress > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .fillMaxHeight()
                                                .fillMaxWidth(animateProgress)
                                                .background(
                                                    brush = Brush.horizontalGradient(
                                                        colors = listOf(
                                                            color.copy(alpha = 0.04f),
                                                            color.copy(alpha = 0.15f)
                                                        )
                                                    )
                                                )
                                        )
                                    }

                                    // 2. 上层漂浮展示的文字数据 Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 级别小徽章 Badge
                                        Box(
                                            modifier = Modifier
                                                .size(width = 38.dp, height = 22.dp)
                                                .background(color.copy(alpha = 0.12f), RoundedRectangle(6.dp))
                                                .border(0.5.dp, color.copy(alpha = 0.3f), RoundedRectangle(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = level,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = color
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // 对比分数说明
                                        Text(
                                            text = stringResource(R.string.stats_breakdown_label_learned_ratio, count, total),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            modifier = Modifier.weight(1f)
                                        )

                                        // 百分比大字靠右
                                        val percent = (progress * 100).toInt()
                                        Text(
                                            text = "$percent%",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (percent > 0) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                }

                                // 3. 点击/长按展开预测详情面板 (行内平滑展开/淡入动画)
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = fadeIn() + expandVertically(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy)
                                    ),
                                    exit = fadeOut() + shrinkVertically(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(color.copy(alpha = 0.08f))
                                            .padding(horizontal = 14.dp, vertical = 9.dp)
                                    ) {
                                        if (prediction != null) {
                                            if (prediction.isCompleted) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.stats_breakdown_format_achieve_record, prediction.completionDateText ?: stringResource(R.string.stats_breakdown_achieved_default)),
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = color
                                                    )
                                                    if (isPinned) {
                                                        Text(
                                                            text = stringResource(R.string.stats_breakdown_badge_locking),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            fontWeight = FontWeight.Medium,
                                                            color = color.copy(alpha = 0.9f)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.stats_breakdown_format_predict_finish, prediction.predictedDateText ?: "—"),
                                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.stats_breakdown_format_remaining_days, prediction.remainingDays, prediction.remainingCount, itemUnit),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                            fontWeight = FontWeight.Medium,
                                                            color = color
                                                        )
                                                        if (isPinned) {
                                                            Text(
                                                                text = stringResource(R.string.stats_breakdown_badge_locked),
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                fontWeight = FontWeight.Medium,
                                                                color = color.copy(alpha = 0.8f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 全等级通关预测概览
                if (overallPrediction != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedRectangle(12.dp),
                        color = themeColor.copy(alpha = 0.08f),
                        border = BorderStroke(0.5.dp, themeColor.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (overallPrediction.isAllCompleted) {
                                        stringResource(R.string.stats_breakdown_all_completed)
                                    } else {
                                        stringResource(
                                            R.string.stats_breakdown_prediction_format,
                                            overallPrediction.estimatedCompletionDateText ?: "—"
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!overallPrediction.isAllCompleted) {
                                    Text(
                                        text = stringResource(R.string.stats_breakdown_format_daily_goal_deduction, overallPrediction.dailyGoal, itemUnit),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (!overallPrediction.isAllCompleted) {
                                Text(
                                    text = stringResource(R.string.stats_breakdown_format_approx_days, overallPrediction.totalRemainingDays),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = Rubik,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = themeColor
                                )
                            }
                        }
                    }
                }

                // Footer (Total Mastered)
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.stats_breakdown_label_total_mastered),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                }
            }
        }
    }
}
