package com.jian.nemo.core.ui.component.dialog

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jian.nemo.core.designsystem.component.PremiumStepper
import com.jian.nemo.core.designsystem.theme.NemoPrimary

/**
 * 今日加餐居中配置对话框 (BonusStudyDialog)
 * 严格遵循 Nemo UI/UX 设计规范 (V1.0)：
 * 1. 屏幕居中 Modal Dialog
 * 2. 扁平化 Surface (shadowElevation = 0.dp)
 * 3. 容器级 24.dp 圆角，组件级 12.dp Squircle 图标背景块
 * 4. Nemo 品牌原生蓝/橙配色，摒弃 MD3 默认系统灰紫调
 * 5. Choice Chips 与重写版 PremiumStepper 实时翻页动画同步
 */
@Composable
fun BonusStudyDialog(
    onDismissRequest: () -> Unit,
    onConfirmBonus: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialQuantity: Int = 10
) {
    var selectedQuantity by remember { mutableIntStateOf(initialQuantity) }
    val presetOptions = listOf(10, 20, 30)

    val context = androidx.compose.ui.platform.LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val vibrator = remember(context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            context.getSystemService(android.os.Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
    }

    fun performHapticClick() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(15, 120))
            } else {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            }
        } else {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ),
            shadowElevation = 0.dp // Nemo Flat 设计规范：无物理阴影
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (12.dp Squircle 图标背景块 + 大标题)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = NemoPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = NemoPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "今日加餐配置",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "保持常规每日目标不变，仅针对今天追加练习组",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 快捷预设 Choice Chips 选项组 (12.dp 圆角)
                Text(
                    text = "快捷预设",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presetOptions.forEach { preset ->
                        val isSelected = selectedQuantity == preset
                        val chipBgColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                NemoPrimary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            label = "ChipBgColor"
                        )
                        val chipTextColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            label = "ChipTextColor"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(chipBgColor)
                                .clickable {
                                    performHapticClick()
                                    selectedQuantity = preset
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$preset",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = chipTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 自定义微调控件 (PremiumStepper 动画控件)
                Text(
                    text = "自定义微调",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 12.dp)
                )

                PremiumStepper(
                    value = selectedQuantity,
                    onValueChange = { selectedQuantity = it },
                    min = 1,
                    max = 200,
                    step = 1
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 提交开始加餐主按钮 (容器级 24.dp 圆角)
                Button(
                    onClick = {
                        performHapticClick()
                        onConfirmBonus(selectedQuantity)
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NemoPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "开始今日加餐 (+$selectedQuantity)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
