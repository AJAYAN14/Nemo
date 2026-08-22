package com.jian.nemo.feature.settings.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import kotlin.math.cos
import kotlin.math.sin

/**
 * 主题颜色定义
 */
private data class ThemeColor(
    val name: String,
    val color: Color
)

/**
 * 主题色选择弹窗 (环形颜色选择器 CircularColorPicker)
 */
@Composable
fun ThemeColorDialog(
    currentColor: Long? = null,
    onDismiss: () -> Unit,
    onColorSelect: (Color) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // 硬件 Vibrator 初始化
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // 1. 细腻微触感 (Tick) - 用于色块飞行起飞瞬间
    fun performHapticTick() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(10, 60))
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } catch (_: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // 2. 确认保存触感 (Click) - 用于保存完成
    fun performHapticConfirm() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, 150))
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val themeColors = listOf(
        ThemeColor("默认", Color(0xFF0E68FF)), // 品牌原生蓝置顶
        ThemeColor("蔷薇红", Color(0xFFFF2D55)),
        ThemeColor("活力橙", Color(0xFFFF9500)),
        ThemeColor("明快黄", Color(0xFFFFCC00)),
        ThemeColor("薄荷绿", Color(0xFF4CD964)),
        ThemeColor("天空蓝", Color(0xFF5AC8FA)),
        ThemeColor("经典蓝", Color(0xFF007AFF)),
        ThemeColor("薰衣草紫", Color(0xFF5856D6)),
        ThemeColor("罗兰紫", Color(0xFFAF52DE)),
        ThemeColor("荧光青", Color(0xFF00E5FF)),
        ThemeColor("亮青绿", Color(0xFF1DE9B6)),
        ThemeColor("炽热红", Color(0xFFFF3D00))
    )

    // 默认颜色与外部选中颜色初始化
    val defaultColor = themeColors.first().color
    val matchedColor = remember(currentColor) {
        themeColors.find { 
            currentColor?.let { c -> Color(c.toULong()).value == it.color.value } ?: false 
        }?.color ?: defaultColor
    }

    // 本地状态：当前选中颜色
    var currentlySelectedColor by remember { mutableStateOf(matchedColor) }

    // 智能获取当前应用所呈现的深色状态 (避免 custom app-level dark mode 不与 system dark mode 同步的问题)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
            onDispose { }
        }

        // 通透质感遮罩 (20% alpha)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .width(320.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                color = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Area (居中标题，右侧 × 关闭按钮)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧占位以实现标题绝对水平居中
                        Spacer(modifier = Modifier.width(48.dp))
                        Text(
                            text = "环形配色选择器",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                letterSpacing = (-0.3).sp
                            ),
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF1C1C1E),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "关闭",
                                tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF8E8E93)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Circular Color Ring
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        themeColors.forEachIndexed { index, themeColor ->
                            val isSelected = currentlySelectedColor.value == themeColor.color.value

                            // 每个点的动画插值 (0f 表示轨道，1f 表示中心点)
                            val t by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0f,
                                animationSpec = spring(
                                    dampingRatio = 0.65f, // 精美弹簧物理特性
                                    stiffness = 110f
                                ),
                                label = "colorAnim_$index"
                            )

                            // 极坐标轨道定位计算
                            val angle = (index * 2 * Math.PI / themeColors.size) - Math.PI / 2
                            val outerRadius = 86.dp
                            val itemSize = 26.dp
                            val centerSize = 74.dp

                            val slotXDp = outerRadius * cos(angle).toFloat()
                            val slotYDp = outerRadius * sin(angle).toFloat()

                            // 根据插值 t 进行平滑位置与大小过渡
                            val currentXDp = slotXDp * (1f - t)
                            val currentYDp = slotYDp * (1f - t)

                            val currentSize = itemSize + (centerSize - itemSize) * t

                            val zIndexValue = if (isSelected) 2f else if (t > 0.01f) 1f else 0f

                            Box(
                                modifier = Modifier
                                    .offset(x = currentXDp, y = currentYDp)
                                    .size(currentSize)
                                    .zIndex(zIndexValue)
                                    .clip(CircleShape)
                                    .background(themeColor.color)
                                    .clickable(
                                        enabled = !isSelected,
                                        onClick = {
                                            performHapticTick()
                                            currentlySelectedColor = themeColor.color
                                        },
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    )
                            )
                        }
                    }

                    // 中文配色文本标签提示
                    val activeColorName = themeColors.find { it.color.value == currentlySelectedColor.value }?.name ?: ""
                    Text(
                        text = "当前选择：$activeColorName",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF8E8E93),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // 确认保存按钮 (发光投影胶囊样式)
                    Button(
                        onClick = {
                            performHapticConfirm()
                            onColorSelect(currentlySelectedColor)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentlySelectedColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .softCardShadow(borderRadius = 25.dp, isDark = currentlySelectedColor.luminance() < 0.5f),
                        shape = RoundedCornerShape(25.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "确认保存",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
