package com.jian.nemo.core.ui.component.dialog

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jian.nemo.core.designsystem.component.PremiumStepper

/**
 * 今日加餐居中配置对话框 (BonusStudyDialog)
 *
 * 包含：
 * 1. 双模式选择（消化积压 vs 学习新词）；
 * 2. 巨幅步进器（支持分位滚轮与手写输入）；
 * 3. 6 个常见常用词数预设（10/20/30/40/50/100 词）；
 * 4. 规范语言学习“词”文案体系。
 */
@Composable
fun BonusStudyDialog(
    onDismissRequest: () -> Unit,
    onConfirmBonus: (Int, BonusMode) -> Unit,
    modifier: Modifier = Modifier,
    initialQuantity: Int = 10,
    initialMode: BonusMode = BonusMode.ReviewBacklog
) {
    var selectedValue by remember(initialQuantity) { mutableIntStateOf(initialQuantity) }
    var selectedMode by remember(initialMode) { mutableStateOf(initialMode) }

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

    // 6 个常见标准预设阶梯（整十规范进阶）
    val presets = listOf(10, 20, 30, 40, 50, 100)
    val accentOrange = Color(0xFFFF5500)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current

        // 窗口级高斯毛玻璃 (Blur Behind)
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
                // 1. 右上角关闭按钮
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

                // 2. 核心内容
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // 顶部橙色闪电 Header Icon
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
                            tint = accentOrange,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "今日加餐",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "突破常规，设定追加练习目标",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. 模式选择器（高质感 Spring 弹簧滑动分段控制器）
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF202024) else Color(0xFFF1F5F9))
                            .padding(4.dp)
                    ) {
                        val segmentWidth = (maxWidth - 8.dp) / 2
                        val selectedIndex = if (selectedMode == BonusMode.ReviewBacklog) 0 else 1

                        // 滑块水平偏移量 (Spring 物理阻尼平滑滑移)
                        val indicatorOffset by animateDpAsState(
                            targetValue = segmentWidth * selectedIndex,
                            animationSpec = spring(
                                dampingRatio = 0.75f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "bonusModeIndicator"
                        )

                        // 浮动悬浮药丸滑块（带柔光微阴影）
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(segmentWidth)
                                .fillMaxHeight()
                                .shadow(
                                    elevation = if (isDark) 4.dp else 2.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    clip = false,
                                    spotColor = Color.Black.copy(alpha = if (isDark) 0.35f else 0.08f),
                                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.20f else 0.04f)
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF2D2520) else Color.White)
                        )

                        // 选项文本交互层
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 选项一：消化积压
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (selectedMode != BonusMode.ReviewBacklog) {
                                            performHapticClick()
                                            selectedMode = BonusMode.ReviewBacklog
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val textColor by animateColorAsState(
                                    targetValue = if (selectedIndex == 0) accentOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(durationMillis = 200),
                                    label = "backlogTextColor"
                                )
                                Text(
                                    text = "消化积压",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }

                            // 选项二：学习新词
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (selectedMode != BonusMode.NewItems) {
                                            performHapticClick()
                                            selectedMode = BonusMode.NewItems
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val textColor by animateColorAsState(
                                    targetValue = if (selectedIndex == 1) accentOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                    animationSpec = tween(durationMillis = 200),
                                    label = "newItemsTextColor"
                                )
                                Text(
                                    text = "学习新词",
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 4. PremiumStepper 词数步进器
                    PremiumStepper(
                        value = selectedValue,
                        onValueChange = { selectedValue = it },
                        min = 1,
                        max = 200,
                        step = 1
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. 6个预设快捷胶囊（两行排列，每行 3 个，规范显示“词”）
                    val rows = presets.chunked(3)
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
                                    val isSelected = selectedValue == preset
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
                                            .height(44.dp)
                                            .border(BorderStroke(1.dp, chipBorderColor), CircleShape)
                                            .clip(CircleShape)
                                            .background(chipBgColor)
                                            .clickable {
                                                performHapticClick()
                                                selectedValue = preset
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${preset}项",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = chipTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // 6. 底部“开始挑战 →”主动作按钮
                    Button(
                        onClick = {
                            performHapticClick()
                            onConfirmBonus(selectedValue, selectedMode)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentOrange,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "开始挑战 →",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 预设奖励学习弹窗单回调重载
 */
@Composable
fun BonusStudyDialog(
    onDismissRequest: () -> Unit,
    onConfirmBonus: (Int) -> Unit,
    modifier: Modifier = Modifier,
    initialQuantity: Int = 10
) {
    BonusStudyDialog(
        onDismissRequest = onDismissRequest,
        onConfirmBonus = { count, _ -> onConfirmBonus(count) },
        modifier = modifier,
        initialQuantity = initialQuantity
    )
}
