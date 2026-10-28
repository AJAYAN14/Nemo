package com.jian.nemo.core.ui.component.common

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * 苹果风格（iOS Context Menu）轻量化下拉菜单
 *
 * 基于 AndroidX Compose 官方原生 [DropdownMenu] 构建，继承官方完善的位置测算、按键返回和遮罩收起体系，
 * 同时注入了苹果设计语言的核心视觉特性：
 * 1. 毛玻璃拟态背景：对接项目现有的 [HazeState] 实时模糊底层；
 * 2. 漫反射下沉软阴影：关闭 Material 生硬黑边，绘制大半径弥散软阴影；
 * 3. 边缘微光高亮：极细 0.5dp 微光描边，塑造通透玻璃轮廓；
 * 4. 连续圆角几何：标准 16dp 苹果圆角曲线。
 *
 * @param expanded 菜单是否展开
 * @param onDismissRequest 关闭菜单回调
 * @param modifier 外部修饰符
 * @param hazeState 毛玻璃模糊状态源，传入后自动开启背景毛玻璃
 * @param offset 弹出相对锚点的坐标偏移
 * @param shape 菜单外形，默认为 16dp 圆角
 * @param minWidth 菜单最小宽度，默认 210dp
 * @param maxWidth 菜单最大宽度，默认 260dp
 * @param properties 弹窗属性
 * @param content 菜单子项内容
 */
@Composable
fun AppleDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    offset: DpOffset = DpOffset(0.dp, 6.dp),
    shape: Shape = RoundedCornerShape(16.dp),
    minWidth: Dp = 210.dp,
    maxWidth: Dp = 260.dp,
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // 苹果风格半透明背景色（若未启用 Haze，以此作为高质感磨砂回落底色）
    val containerBgColor = if (isDark) {
        Color(0xCC202024)
    } else {
        Color(0xEBFAFAFC)
    }

    // 边缘微光描边（iOS 玻璃高光反光质感）
    val highlightBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        shape = shape,
        containerColor = containerBgColor,
        shadowElevation = 0.dp, // 禁用 Material 默认硬质投影，改用原生漫反射阴影
        border = BorderStroke(0.5.dp, highlightBorderColor),
        properties = properties,
        modifier = modifier
            .widthIn(min = minWidth, max = maxWidth)
            .appleDiffuseShadow(shape = shape, isDark = isDark)
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState, shape = shape)
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

/**
 * 苹果风格分组分割条 (Apple Menu Section Divider)
 *
 * 替代传统 1px 细线，采用苹果设计语言中具有厚度与弱半透明底色的分组槽条，
 * 在菜单功能组（如卡片处置、快捷设置、显示模式）之间形成清晰的层次区隔。
 */
@Composable
fun AppleMenuSectionDivider(
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val isDark = isSystemInDarkTheme()
    val dividerColor = if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(dividerColor)
    )
}

/**
 * 苹果风格菜单项 (Apple Dropdown Menu Item)
 *
 * 单项行高 44dp，两端留白 14dp，支持前置图标、彩色 PlumpDuoIcon、后置辅助指示与危险动作红色警示。
 */
@Composable
fun AppleDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingPainter: Painter? = null,
    plumpIcon: PlumpIcon? = null,
    iconColor: Color? = null,
    trailingText: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isDestructive -> IsoluminantPalette.CoralFlame
        else -> MaterialTheme.colorScheme.onSurface
    }

    val finalIconColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isDestructive -> IsoluminantPalette.CoralFlame
        iconColor != null -> iconColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = {
            if (enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        enabled = enabled,
        color = Color.Transparent,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (plumpIcon != null && isPlumpDuo) {
                    PlumpDuoIcon(
                        icon = plumpIcon,
                        color = finalIconColor,
                        size = 20.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else if (leadingPainter != null) {
                    Icon(
                        painter = leadingPainter,
                        contentDescription = null,
                        tint = finalIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = finalIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = text,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1
                )
            }

            if (trailingText != null) {
                Text(
                    text = trailingText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            } else if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

/**
 * 苹果风格开关菜单项 (Apple Dropdown Toggle Item)
 *
 * 结合 LiquidToggle 交互开关，点击整行皆可触发切换并伴随触觉微反馈。
 */
@Composable
fun AppleDropdownMenuToggleItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            if (enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
        },
        enabled = enabled,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            )

            LiquidToggle(
                checked = checked,
                onCheckedChange = {
                    if (enabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCheckedChange(it)
                    }
                },
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 绘制苹果风格的下沉漫反射阴影修饰符
 *
 * 使用原生 Canvas 和 [BlurMaskFilter] 绘制低饱和度、大模糊半径的真实弥散阴影，
 * 避免 Android 默认 elevation 的硬切角和生硬投影。
 */
private fun Modifier.appleDiffuseShadow(
    shape: Shape,
    isDark: Boolean,
    shadowRadius: Dp = 20.dp,
    shadowOffsetY: Dp = 8.dp
): Modifier = this.drawBehind {
    val shadowRadiusPx = shadowRadius.toPx()
    val shadowOffsetYPx = shadowOffsetY.toPx()

    val shadowColor = if (isDark) {
        Color.Black.copy(alpha = 0.36f)
    } else {
        Color.Black.copy(alpha = 0.10f)
    }

    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = shadowColor.toArgb()
        maskFilter = BlurMaskFilter(shadowRadiusPx, BlurMaskFilter.Blur.NORMAL)
    }

    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(0f, shadowOffsetYPx)
        val outline = shape.createOutline(size, layoutDirection, this)
        when (outline) {
            is androidx.compose.ui.graphics.Outline.Rounded -> {
                val roundRect = outline.roundRect
                canvas.nativeCanvas.drawRoundRect(
                    roundRect.left,
                    roundRect.top,
                    roundRect.right,
                    roundRect.bottom,
                    roundRect.bottomLeftCornerRadius.x,
                    roundRect.bottomLeftCornerRadius.y,
                    paint
                )
            }
            is androidx.compose.ui.graphics.Outline.Rectangle -> {
                val rect = outline.rect
                canvas.nativeCanvas.drawRect(
                    rect.left,
                    rect.top,
                    rect.right,
                    rect.bottom,
                    paint
                )
            }
            is androidx.compose.ui.graphics.Outline.Generic -> {
                canvas.nativeCanvas.drawPath(
                    outline.path.asAndroidPath(),
                    paint
                )
            }
        }
        canvas.restore()
    }
}
