package com.jian.nemo.core.ui.component.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.NemoPrimary
import kotlinx.coroutines.delay

/**
 * Nemo 自定义 Snackbar 类型
 */
enum class NemoSnackbarType {
    /** 默认信息提示 (蓝色) */
    INFO,
    /** 成功提示 (绿色) */
    SUCCESS,
    /** 警告提示 (橙色) */
    WARNING,
    /** 错误提示 (红色) */
    ERROR
}

/**
 * Nemo 自定义顶部 Snackbar
 *
 * 用于替代 MD3 官方 Snackbar，提供统一的项目 UI 风格
 *
 * @param visible 是否显示
 * @param message 消息内容
 * @param actionText 操作按钮文本 (可选)
 * @param icon 图标 (可选)
 * @param type Snackbar 类型，决定颜色
 * @param autoDismissMs 自动消失时间 (毫秒)，null 表示不自动消失，默认 5000ms
 * @param onDismiss 自动消失时的回调，用于更新外部状态
 * @param onClick 点击回调 (整个 Snackbar 可点击)
 * @param modifier Modifier
 */
@Composable
fun NemoSnackbar(
    visible: Boolean,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    icon: ImageVector? = null,
    type: NemoSnackbarType = NemoSnackbarType.INFO,
    cornerRadius: Dp = 16.dp,
    autoDismissMs: Long? = 5000L,
    onDismiss: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5

    // 根据类型和主题决定渐变色
    val gradientColors = getGradientColors(type, isDarkTheme)

    // 自动消失定时器
    if (visible && autoDismissMs != null && onDismiss != null) {
        LaunchedEffect(visible) {
            delay(autoDismissMs)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(200)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(8.dp, RoundedCornerShape(cornerRadius))
                .clip(RoundedCornerShape(cornerRadius))
                .background(Brush.horizontalGradient(gradientColors))
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            onClick = onClick,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (actionText != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 根据类型和主题获取渐变色
 */
private fun getGradientColors(type: NemoSnackbarType, isDarkTheme: Boolean): List<Color> {
    return when (type) {
        NemoSnackbarType.INFO -> {
            if (isDarkTheme) {
                listOf(Color(0xFF3D3A50), Color(0xFF2B2930))
            } else {
                listOf(NemoPrimary, Color(0xFF4A90D9))
            }
        }
        NemoSnackbarType.SUCCESS -> {
            if (isDarkTheme) {
                listOf(Color(0xFF2D4A3D), Color(0xFF1E3A2F))
            } else {
                listOf(Color(0xFF34C759), Color(0xFF28A745))
            }
        }
        NemoSnackbarType.WARNING -> {
            if (isDarkTheme) {
                listOf(Color(0xFF4A3D2D), Color(0xFF3A2F1E))
            } else {
                listOf(Color(0xFFFF9500), Color(0xFFE68A00))
            }
        }
        NemoSnackbarType.ERROR -> {
            if (isDarkTheme) {
                listOf(Color(0xFF4A2D2D), Color(0xFF3A1E1E))
            } else {
                listOf(Color(0xFFFF3B30), Color(0xFFE53935))
            }
        }
    }
}
