package com.jian.nemo.core.ui.component.dialog

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jian.nemo.core.designsystem.component.PremiumStepper

/**
 * 今日加餐居中配置对话框 (BonusStudyDialog) - 像素级对齐参考图设计
 * 特性：
 * 1. 右上角 ✕ 浅灰圆形关闭按钮
 * 2. 居中 54.dp 浅橙闪电 Header 块 + 居中双层标题
 * 3. 58.dp 巨幅 Stepper 加减器 (保持分位独立机械滚轮 + 震动 + 手写输入)
 * 4. 6 个全胶囊 Choice Chips 选项组 (2 行 3 列排列)
 * 5. 亮橙色全胶囊动作按钮 "开始挑战 →"
 */
@Composable
fun BonusStudyDialog(
    onDismissRequest: () -> Unit,
    onConfirmBonus: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialQuantity: Int = 10
) {
    var selectedQuantity by remember { mutableIntStateOf(initialQuantity) }
    // 6 个全胶囊快捷预设
    val presetOptions = listOf(5, 10, 15, 20, 30, 50)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun performHapticClick() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, 120))
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            ),
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 1. 右上角 ✕ 关闭按钮
                IconButton(
                    onClick = {
                        performHapticClick()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF2B2B2E) else Color(0xFFF1F5F9))
                        .align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = if (isDark) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 2. 居中核心布局
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // 居中 54.dp 浅橙闪电 Header 块
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDark) Color(0xFF332014) else Color(0xFFFFF1E6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFF5500),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 主大标题
                    Text(
                        text = "今日加餐",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 柔和副标题
                    Text(
                        text = "突破常规，设定追加练习目标",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 3. 中间 Stepper 巨幅加减器 (保持分位独立滚轮与手写输入)
                    PremiumStepper(
                        value = selectedQuantity,
                        onValueChange = { selectedQuantity = it },
                        min = 1,
                        max = 200,
                        step = 1
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 4. 6 个全胶囊 Choice Chips (2 行 3 列网格排列)
                    val rows = presetOptions.chunked(3)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rows.forEach { rowPresets ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPresets.forEach { preset ->
                                    val isSelected = selectedQuantity == preset
                                    val chipBgColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            if (isDark) Color.White else Color(0xFF18181B)
                                        } else {
                                            if (isDark) Color(0xFF262626) else Color.Transparent
                                        },
                                        label = "ChipBgColor"
                                    )
                                    val chipTextColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            if (isDark) Color.Black else Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        label = "ChipTextColor"
                                    )
                                    val chipBorderColor = if (isSelected) {
                                        Color.Transparent
                                    } else {
                                        if (isDark) Color(0xFF383838) else Color(0xFFE2E8F0)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .border(BorderStroke(1.dp, chipBorderColor), CircleShape)
                                            .clip(CircleShape)
                                            .background(chipBgColor)
                                            .clickable {
                                                performHapticClick()
                                                selectedQuantity = preset
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+ $preset",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 15.sp
                                            ),
                                            color = chipTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 5. 亮橙色全胶囊动作按钮 "开始挑战 →"
                    Button(
                        onClick = {
                            performHapticClick()
                            onConfirmBonus(selectedQuantity)
                            onDismissRequest()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5500),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "开始挑战",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
