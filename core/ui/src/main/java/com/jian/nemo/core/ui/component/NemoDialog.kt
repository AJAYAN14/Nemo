package com.jian.nemo.core.ui.component

import android.os.Build
import android.view.WindowManager
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader

/**
 * Nemo 通用高质感弹窗组件
 *
 * 采用 48dp 连续曲率平滑圆角 (Squircle) 极简纯白卡片设计，
 * 配色 100% 自动跟随 App 当前的主题风格 ([MaterialTheme.colorScheme])。
 *
 * @param onDismissRequest 点击背景遮罩或取消按钮时触发
 * @param title 弹窗标题
 * @param text 弹窗正文文本（当 [content] 为空时生效）
 * @param confirmText 确认按钮文本，为 null 时不展示确认按钮
 * @param dismissText 取消按钮文本，为 null 时不展示取消按钮
 * @param onConfirm 点击确认按钮回调
 * @param isDangerous 是否为危险警示操作（若为 true，确认按钮显示为警告红 `#FF3B30`）
 * @param confirmButtonColor 自定义确认按钮背景颜色（为空时根据 [isDangerous] 自动决定）
 * @param confirmEnabled 确认按钮是否允许点击
 * @param isLoading 是否在确认按钮中展示加载转圈动画
 * @param onDismiss 点击取消/放弃按钮时的专门回调（未指定时默认触发 [onDismissRequest]）
 * @param content 自定义弹窗内部 Compose 布局
 */
@Composable
fun NemoDialog(
    onDismissRequest: () -> Unit,
    title: String = "提示",
    text: String? = null,
    confirmText: String? = "确定",
    dismissText: String? = "取消",
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    isDangerous: Boolean = false,
    confirmButtonColor: Color? = null,
    confirmEnabled: Boolean = true,
    isLoading: Boolean = false,
    content: (@Composable () -> Unit)? = null
) {
    // 自动判定 App 当前主题是否为深色模式（跟随 App 主题 colorScheme，而非盲目跟随系统开关）
    val isAppDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dialogShape = RoundedRectangle(48.dp)

    // 危险红颜色定义
    val dangerRedColor = if (isAppDark) Color(0xFFFF453A) else Color(0xFFFF3B30)

    // 配色方案：自动对齐 App 主题
    val containerColor = if (isAppDark) Color(0xFF1E1E1E) else Color.White
    val titleColor = if (isDangerous) dangerRedColor else MaterialTheme.colorScheme.onSurface
    val textBodyColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 确认按钮背景色
    val resolvedConfirmBgColor = when {
        confirmButtonColor != null -> confirmButtonColor
        isDangerous -> dangerRedColor
        else -> MaterialTheme.colorScheme.primary
    }

    val buttonBgColor = if (isAppDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
    val borderColor = if (isAppDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    Dialog(
        onDismissRequest = {
            if (!isLoading) {
                onDismissRequest()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading
        )
    ) {
        val view = LocalView.current
        val haptic = LocalHapticFeedback.current

        // 开启 Android 12+ 官方窗口级高斯毛玻璃 (Blur Behind)
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    blurBehindRadius = 48 // 48px 原生 GPU 硬件级细腻毛玻璃
                    dimAmount = 0.20f    // 20% 通透柔光暗化
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

        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!isAppDark) {
                            Modifier.shadow(12.dp, dialogShape)
                        } else {
                            Modifier
                        }
                    )
                    .clip(dialogShape)
                    .background(containerColor, dialogShape)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = dialogShape
                    )
            ) {
                // 1. 标题
                BasicText(
                    text = title,
                    modifier = Modifier.padding(start = 28.dp, top = 28.dp, end = 28.dp, bottom = 12.dp),
                    style = TextStyle(
                        color = titleColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                // 2. 正文内容
                if (content != null) {
                    Box(modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)) {
                        content()
                    }
                } else if (text != null) {
                    BasicText(
                        text = text,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                        style = TextStyle(
                            color = textBodyColor,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. 底部操作按钮区域
                if (dismissText != null || confirmText != null) {
                    Row(
                        modifier = Modifier
                            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dismissText != null) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(Capsule())
                                    .background(buttonBgColor, Capsule())
                                    .clickable(enabled = !isLoading) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (onDismiss != null) {
                                            onDismiss()
                                        } else {
                                            onDismissRequest()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(
                                    text = dismissText,
                                    style = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isLoading) 0.38f else 0.85f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        if (confirmText != null) {
                            val isClickable = confirmEnabled && !isLoading
                            val actualBgColor = if (confirmEnabled) resolvedConfirmBgColor else resolvedConfirmBgColor.copy(alpha = 0.4f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(Capsule())
                                    .background(actualBgColor, Capsule())
                                    .clickable(enabled = isClickable) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onConfirm?.invoke()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    NemoChasingDotsLoader(size = 20.dp, color = Color.White)
                                } else {
                                    BasicText(
                                        text = confirmText,
                                        style = TextStyle(
                                            color = Color.White.copy(alpha = if (confirmEnabled) 1.0f else 0.6f),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
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
