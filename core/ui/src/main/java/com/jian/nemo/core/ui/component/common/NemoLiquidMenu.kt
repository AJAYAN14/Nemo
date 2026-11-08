package com.jian.nemo.core.ui.component.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 液态悬浮菜单触发按钮（圆润三横平滑形变变为 ✕）
 *
 * @param isExpanded 菜单当前是否处于展开状态
 * @param onClick 点击事件
 * @param modifier 修饰符
 * @param backdrop 背景采样层（同级非空时启用透镜折射）
 * @param buttonSize 按钮尺寸（默认 48.dp）
 * @param enableHaptic 是否触发触感振动
 */
@Composable
fun NemoLiquidMenuButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    buttonSize: Dp = 48.dp,
    enableHaptic: Boolean = true,
    chromaticAberration: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    FrostedLiquidGlassButton(
        onClick = {
            if (enableHaptic) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onClick()
        },
        shape = CircleShape,
        backdrop = backdrop,
        hasLens = backdrop != null,
        chromaticAberration = chromaticAberration,
        contentPadding = PaddingValues(0.dp),
        isInteractive = true,
        modifier = modifier.size(buttonSize)
    ) {
        AnimatedMenuCloseIcon(
            isExpanded = isExpanded,
            color = MaterialTheme.colorScheme.onSurface,
            size = 24.dp
        )
    }
}

/**
 * 页面同级液态悬浮菜单浮层（彻底杜绝跨 Window 隔离，实现真实物理凸透镜折射）
 *
 * @param isVisible 浮层是否可见
 * @param onDismiss 关闭回调
 * @param modifier 修饰符
 * @param backdrop 背景采样源
 * @param menuWidth 菜单面板宽度（默认 240.dp）
 * @param blurRadius 模糊半径（默认 12.dp）
 * @param refractionHeightRatio 动态折射高度比例（默认 0.10f）
 * @param refractionAmountRatio 动态折射强度比例（默认 0.20f）
 * @param content 菜单列表内容
 */
@Composable
fun NemoLiquidMenuOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    menuWidth: Dp = 260.dp,
    blurRadius: Dp = 12.dp,
    refractionHeightRatio: Float = 0.10f,
    refractionAmountRatio: Float = 0.20f,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(enabled = isVisible, onBack = onDismiss)

    val scaleX = remember { Animatable(0.45f) }
    val scaleY = remember { Animatable(0.35f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            coroutineScope {
                launch { scaleX.animateTo(1f, spring(0.82f, 420f)) }
                launch { scaleY.animateTo(1f, spring(0.78f, 380f)) }
                launch { contentAlpha.animateTo(1f, spring(0.9f, 500f)) }
            }
        } else {
            scaleX.snapTo(0.45f)
            scaleY.snapTo(0.35f)
            contentAlpha.snapTo(0f)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

            val menuShape = RoundedRectangle(22.dp)
            val effectiveBackdrop = backdrop ?: remember { emptyBackdrop() }

            val borderBrush = Brush.verticalGradient(
                colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.05f)
                    )
                }
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(
                        top = statusBarTop + 60.dp,
                        end = 16.dp,
                        bottom = 32.dp
                    )
                    .graphicsLayer {
                        this.scaleX = scaleX.value
                        this.scaleY = scaleY.value
                        this.alpha = contentAlpha.value
                        this.transformOrigin = TransformOrigin(0.88f, 0.05f)
                    }
                    .widthIn(min = 230.dp, max = menuWidth)
                    .drawBackdrop(
                        backdrop = effectiveBackdrop,
                        shape = { menuShape },
                        effects = {
                            val minDimension = size.minDimension
                            colorControls(
                                brightness = if (isDark) 0.05f else 0.10f,
                                saturation = 1.35f
                            )
                            blur(blurRadius.toPx())
                            lens(
                                refractionHeight = refractionHeightRatio * minDimension,
                                refractionAmount = refractionAmountRatio * minDimension,
                                depthEffect = true,
                                chromaticAberration = false
                            )
                        },
                        highlight = { Highlight.Plain },
                        onDrawSurface = {
                            drawRect(
                                color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.06f)
                            )
                        }
                    )
                    .border(0.6.dp, borderBrush, menuShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* 阻断背景点击穿透 */ }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * 液态悬浮菜单微光微透分割线
 */
@Composable
fun NemoLiquidMenuDivider(
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    HorizontalDivider(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 2.dp),
        color = dividerColor,
        thickness = 0.5.dp
    )
}

/**
 * 液态悬浮菜单标准条目
 *
 * @param text 标题文本
 * @param onClick 点击事件
 * @param modifier 修饰符
 * @param leadingIcon 前置图标
 * @param iconColor 图标强调色
 * @param trailingText 尾部指示文本
 * @param enabled 是否可用
 * @param isDestructive 是否为破坏性操作
 */
@Composable
fun NemoLiquidMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconColor: Color? = null,
    trailingText: String? = null,
    enabled: Boolean = true,
    isDestructive: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val destructiveColor = if (isDark) IosColors.RedDark else IosColors.Red

    val effectiveIconColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isDestructive -> destructiveColor
        else -> iconColor ?: MaterialTheme.colorScheme.primary
    }

    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isDestructive -> destructiveColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgAlpha = if (isPressed && enabled) 0.08f else 0f
    val itemShape = RoundedRectangle(12.dp)

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = bgAlpha),
        shape = itemShape,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = effectiveIconColor
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )

            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 液态悬浮菜单带开关条目
 *
 * @param text 标题文本
 * @param checked 开关状态
 * @param onCheckedChange 开关状态变更回调
 * @param modifier 修饰符
 * @param enabled 是否可用
 */
@Composable
fun NemoLiquidMenuToggleItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCheckedChange(!checked)
        },
        enabled = enabled,
        color = Color.Transparent,
        shape = RoundedRectangle(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            LiquidToggle(
                checked = checked,
                onCheckedChange = onCheckedChange,
                accentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 液态悬浮菜单聚合组件（自动管理展开状态）
 *
 * @param modifier 按钮修饰符
 * @param backdrop 背景采样层
 * @param buttonSize 触发按钮尺寸
 * @param isMenuExpanded 外部展开状态（为 null 时由组件内部管理）
 * @param onToggleMenu 展开状态变更回调
 * @param content 菜单面板内容
 */
@Composable
fun NemoLiquidMenu(
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    buttonSize: Dp = 48.dp,
    isMenuExpanded: Boolean = false,
    onToggleMenu: ((Boolean) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val expanded = if (onToggleMenu != null) isMenuExpanded else internalExpanded
    val setExpanded: (Boolean) -> Unit = { expandedValue ->
        internalExpanded = expandedValue
        onToggleMenu?.invoke(expandedValue)
    }

    NemoLiquidMenuButton(
        isExpanded = expanded,
        onClick = { setExpanded(!expanded) },
        modifier = modifier,
        backdrop = backdrop,
        buttonSize = buttonSize
    )

    NemoLiquidMenuOverlay(
        isVisible = expanded,
        onDismiss = { setExpanded(false) },
        backdrop = backdrop,
        content = content
    )
}
