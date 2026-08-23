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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jian.nemo.core.designsystem.theme.IosColors

/**
 * 等级分布弹窗 (极致精美紧凑版 / UI/UX Pro Max)
 * 
 * 以极富设计感、高利用率的空间结构展示单词或语法的等级占比及精确百分比
 */
@Composable
fun LevelBreakdownDialog(
    title: String,
    data: Map<String, Int>,
    totalData: Map<String, Int>,
    themeColor: Color,
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
                .widthIn(max = 340.dp),
            shape = RoundedCornerShape(24.dp),
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
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
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
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "整体掌握比: $totalCount / $grandTotalCount",
                            style = MaterialTheme.typography.bodyMedium, // 升级为标准易读大小
                            fontWeight = FontWeight.SemiBold, // 柔和中粗
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$grandPercentage%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, // 降级为Bold，杜绝过度抢眼
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
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animateGrandProgress)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(themeColor.copy(alpha = 0.7f), themeColor)
                                    ),
                                    shape = RoundedCornerShape(3.dp)
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
                        "N1" to IosColors.Red,
                        "N2" to IosColors.Orange,
                        "N3" to IosColors.Yellow,
                        "N4" to IosColors.Green,
                        "N5" to IosColors.Blue
                    )

                    levelList.forEach { level ->
                        val count = data[level] ?: 0
                        val total = totalData[level] ?: 0
                        val color = levelColors[level] ?: themeColor

                        val progress = if (total > 0) count.toFloat() / total else 0f
                        val animateProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = spring(stiffness = Spring.StiffnessVeryLow, dampingRatio = Spring.DampingRatioLowBouncy),
                            label = "levelProgress_$level"
                        )

                        Surface(
                            onClick = { /* Future: Navigate to filtered list */ },
                            shape = RoundedCornerShape(14.dp), // 稍微圆润一点，极具卡片感
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)) {
                                // 1. 流光渐变进度底色 (从左到右填充， matchParentSize 自适应)
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
                                    // 级别小徽章 Badge (现在徽章也可以是略带半透明圆角的精致样式)
                                    Box(
                                        modifier = Modifier
                                            .size(width = 38.dp, height = 22.dp)
                                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
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

                                    // 对比分数说明 (占据 weight(1f) 自适应拉伸)
                                    Text(
                                        text = "已学 $count / $total",
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
                        }
                    }
                }

                // Footer (Total Mastered)
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已掌握总计",
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
