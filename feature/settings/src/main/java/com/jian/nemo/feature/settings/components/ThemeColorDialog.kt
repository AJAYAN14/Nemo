package com.jian.nemo.feature.settings.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.SettingsPalette
import com.jian.nemo.core.designsystem.theme.UserPalette
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
 * 主题色选择弹窗 (重构为项目通用的 NemoDialog，保留全部环形物理吸附动画与微触感)
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
        ThemeColor("默认", UserPalette.PrimaryGlassTintLight),
        ThemeColor("蔷薇红", IosColors.Pink),
        ThemeColor("活力橙", IosColors.Orange),
        ThemeColor("明快黄", IosColors.Yellow),
        ThemeColor("薄荷绿", SettingsPalette.ThemeAppleGreen),
        ThemeColor("天空蓝", IosColors.Teal),
        ThemeColor("经典蓝", IosColors.Blue),
        ThemeColor("薰衣草紫", IosColors.Indigo),
        ThemeColor("罗兰紫", IosColors.Purple),
        ThemeColor("荧光青", SettingsPalette.ThemeElectricCyan),
        ThemeColor("亮青绿", SettingsPalette.ThemeBrightTeal),
        ThemeColor("炽热红", SettingsPalette.ThemeBrightOrangeRed)
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

    NemoDialog(
        onDismissRequest = onDismiss,
        title = "环形配色选择器",
        confirmText = "确定",
        dismissText = "取消",
        confirmButtonColor = currentlySelectedColor,
        onConfirm = {
            performHapticConfirm()
            onColorSelect(currentlySelectedColor)
            onDismiss()
        },
        onDismiss = onDismiss,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Color Ring (核心物理动画完全不动)
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .padding(vertical = 4.dp),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
        }
    )
}
