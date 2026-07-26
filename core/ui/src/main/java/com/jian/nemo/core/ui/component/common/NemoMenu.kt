package com.jian.nemo.core.ui.component.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.jian.nemo.core.ui.modifier.softCardShadow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize

/**
 * Nemo 风格的顶级下拉菜单 (Premium Design)
 *
 * 遵循项目 UI/UX 规范：
 * - 圆角: 16dp
 * - 阴影: 8dp 高质感悬浮阴影
 * - 边框: 0.5dp 柔和微光感边框
 * - 配色: 适配深浅模式 Semantic Colors
 *
 * @param expanded 是否展开
 * @param onDismissRequest 关闭回调
 * @param modifier 修饰符
 * @param offset 偏移量
 * @param content 菜单内容 (使用 NemoMenuItem)
 */
@Composable
fun NemoDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 8.dp), // 增加垂直偏移，避免紧贴按钮
    content: @Composable ColumnScope.() -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface
    // 极致柔和微光边框：浅色 8% 软黑折射，深色 15% 水晶高光
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
    val shape = RoundedCornerShape(16.dp)

    MaterialTheme(
        shapes = MaterialTheme.shapes.copy(extraSmall = shape)
    ) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            modifier = modifier
                .background(containerColor, shape)
                .border(
                    BorderStroke(0.5.dp, borderColor),
                    shape
                )
                .widthIn(min = 200.dp),
            offset = offset,
            containerColor = containerColor,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp, // 恢复 8dp 系统悬浮立面阴影
            content = content
        )
    }
}

/**
 * Nemo 风格的菜单项 (Premium Design)
 *
 * - 字体: Title Medium (16sp) 提升可读性
 * - 图标: 使用主题色 (NemoPrimary) 增强视觉引导
 * - 间距: 宽敞的 Horizontal Padding
 *
 * @param text 文本
 * @param onClick 点击回调
 * @param leadingIcon 图标 (可选)
 * @param trailingIcon 尾部图标 (可选)
 * @param enabled 是否启用
 * @param isDestructive 是否是破坏性操作 (红色)
 */
@Composable
fun NemoMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isDestructive: Boolean = false
) {
    // 使用主题色 (Primary) 或 错误色 (Error)
    val mainColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val iconTint = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary // 普通图标使用主色调，更精致
    }

    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium, // 使用稍大的字体 (16sp)
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp, // 微调
                color = mainColor
            )
        },
        onClick = onClick,
        modifier = modifier,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint.copy(alpha = if (enabled) 1f else 0.38f)
                )
            }
        } else null,
        trailingIcon = if (trailingIcon != null) {
            {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconTint.copy(alpha = if (enabled) 0.7f else 0.38f)
                )
            }
        } else null,
        enabled = enabled,
        colors = MenuDefaults.itemColors(
            textColor = mainColor,
            leadingIconColor = iconTint,
            trailingIconColor = iconTint,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp) // 增加间距
    )
}
