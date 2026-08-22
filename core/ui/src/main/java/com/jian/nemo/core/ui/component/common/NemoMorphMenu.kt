package com.jian.nemo.core.ui.component.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jian.nemo.core.ui.component.liquid.LiquidButton

/**
 * 贝塞尔弹性与减速曲线定义 (严格对齐 Transitions.dev 规格)
 */
private val MorphOpenEasing = CubicBezierEasing(0.34f, 1.25f, 0.64f, 1.0f)
private val MorphCloseEasing = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
private const val MORPH_OPEN_DURATION = 350
private const val MORPH_CLOSE_DURATION = 250
private const val FADE_DURATION = 200

/**
 * Morph 菜单作用域，用于控制菜单项点击后的关闭操作
 */
interface NemoMorphMenuScope {
    fun close()
}

/**
 * NemoMorphMenu - 形变展开菜单组件 (Plus/More to Menu Morph)
 *
 * 核心动效：
 * 1. 触发按钮采用项目统一的 LiquidButton（液态物理微弹性与弥散阴影，与 CommonHeader 左侧返回按钮完全对齐）。
 * 2. 容器尺寸（Width/Height/BorderRadius）在 44dp 圆形按钮与大菜单面板之间做弹性形变过渡。
 * 3. 图标层：展开时伴随微缩放、旋转 (45°) 与向左位移并淡出。
 * 4. 菜单内容层：展开时从右侧位移 + 缩放淡入；收起时快速淡出。
 * 5. Popup 悬浮锚定在右上角，点击外部或返回键平滑收起。
 *
 * @param modifier 修饰符
 * @param icon 触发按钮图标（默认三点图标 MoreVert，可自定义如 Add）
 * @param contentDescription 无障碍描述
 * @param buttonSize 触发按钮默认尺寸（默认 44.dp，与 CommonHeader 左侧返回按钮完全一致）
 * @param menuWidth 展开后菜单面板宽度（默认 192.dp）
 * @param content 菜单内容槽
 */
@Composable
fun NemoMorphMenu(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.MoreVert,
    contentDescription: String? = "更多选项",
    buttonSize: Dp = 44.dp,
    menuWidth: Dp = 192.dp,
    content: @Composable NemoMorphMenuScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    NemoMorphMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
        icon = icon,
        contentDescription = contentDescription,
        buttonSize = buttonSize,
        menuWidth = menuWidth,
        content = content
    )
}

/**
 * 受控状态的 NemoMorphMenu 重载
 */
@Composable
fun NemoMorphMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.MoreVert,
    contentDescription: String? = "更多选项",
    buttonSize: Dp = 44.dp,
    menuWidth: Dp = 192.dp,
    content: @Composable NemoMorphMenuScope.() -> Unit
) {
    val density = LocalDensity.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val navGroupBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.White
    val containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)

    // 用于管理动画状态（确保收起动画播放完毕后再完全隐藏浮层）
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = expanded

    // 测量菜单内容的真实完整高度（不受父容器动画高度截断）
    var menuMeasuredHeight by remember { mutableStateOf(168.dp) }

    val transition = rememberTransition(transitionState, label = "NemoMorphTransition")

    // 容器尺寸与圆角动画
    val animatedWidth by transition.animateDp(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = MORPH_OPEN_DURATION, easing = MorphOpenEasing)
            } else {
                tween(durationMillis = MORPH_CLOSE_DURATION, easing = MorphCloseEasing)
            }
        },
        label = "morphWidth"
    ) { isOpen ->
        if (isOpen) menuWidth else buttonSize
    }

    val animatedHeight by transition.animateDp(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = MORPH_OPEN_DURATION, easing = MorphOpenEasing)
            } else {
                tween(durationMillis = MORPH_CLOSE_DURATION, easing = MorphCloseEasing)
            }
        },
        label = "morphHeight"
    ) { isOpen ->
        if (isOpen) menuMeasuredHeight else buttonSize
    }

    val animatedCornerRadius by transition.animateDp(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = MORPH_OPEN_DURATION, easing = MorphOpenEasing)
            } else {
                tween(durationMillis = MORPH_CLOSE_DURATION, easing = MorphCloseEasing)
            }
        },
        label = "morphRadius"
    ) { isOpen ->
        if (isOpen) 18.dp else (buttonSize / 2)
    }

    val animatedShadowElevation by transition.animateDp(
        transitionSpec = { tween(durationMillis = 200) },
        label = "morphShadow"
    ) { isOpen ->
        if (isOpen) 10.dp else 0.dp
    }

    // 图标动画 (旋转、位移、淡出)
    val iconAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = FADE_DURATION, easing = MorphCloseEasing) },
        label = "iconAlpha"
    ) { isOpen ->
        if (isOpen) 0f else 1f
    }

    val iconTranslationX by transition.animateDp(
        transitionSpec = {
            if (targetState) tween(MORPH_OPEN_DURATION, easing = MorphOpenEasing)
            else tween(MORPH_CLOSE_DURATION, easing = MorphCloseEasing)
        },
        label = "iconTranslateX"
    ) { isOpen ->
        if (isOpen) (-28).dp else 0.dp
    }

    val iconRotation by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(MORPH_OPEN_DURATION, easing = MorphOpenEasing)
            else tween(MORPH_CLOSE_DURATION, easing = MorphCloseEasing)
        },
        label = "iconRotate"
    ) { isOpen ->
        if (isOpen) 45f else 0f
    }

    // 菜单内容动画 (缩放、位移、淡入)
    val contentAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = FADE_DURATION, easing = MorphOpenEasing) },
        label = "contentAlpha"
    ) { isOpen ->
        if (isOpen) 1f else 0f
    }

    val contentTranslationX by transition.animateDp(
        transitionSpec = {
            if (targetState) tween(MORPH_OPEN_DURATION, easing = MorphOpenEasing)
            else tween(MORPH_CLOSE_DURATION, easing = MorphCloseEasing)
        },
        label = "contentTranslateX"
    ) { isOpen ->
        if (isOpen) 0.dp else 20.dp
    }

    val contentScale by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(MORPH_OPEN_DURATION, easing = MorphOpenEasing)
            else tween(MORPH_CLOSE_DURATION, easing = MorphCloseEasing)
        },
        label = "contentScale"
    ) { isOpen ->
        if (isOpen) 1f else 0.96f
    }

    val scope = remember(onExpandedChange) {
        object : NemoMorphMenuScope {
            override fun close() {
                onExpandedChange(false)
            }
        }
    }

    // 常态下的液态占位触发按钮（与 CommonHeader 返回按钮完全统一）
    Box(
        modifier = modifier.size(buttonSize),
        contentAlignment = Alignment.Center
    ) {
        LiquidButton(
            onClick = { onExpandedChange(true) },
            backgroundColor = navGroupBg,
            shape = CircleShape,
            isInteractive = true,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 当需要渲染（展开中或正在进行收起动画）时挂载浮层 Popup
        if (transitionState.currentState || transitionState.targetState) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, 0),
                onDismissRequest = { onExpandedChange(false) },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                BackHandler(enabled = expanded) {
                    onExpandedChange(false)
                }

                // 形变主卡片（自右上角向左下平滑展开）
                Box(
                    modifier = Modifier
                        .width(animatedWidth)
                        .height(animatedHeight)
                        .shadow(
                            elevation = animatedShadowElevation,
                            shape = RoundedCornerShape(animatedCornerRadius)
                        )
                        .clip(RoundedCornerShape(animatedCornerRadius))
                        .background(containerColor)
                        .border(
                            BorderStroke(0.5.dp, borderColor),
                            RoundedCornerShape(animatedCornerRadius)
                        )
                ) {
                    // 1. 图标层（收起态居中，展开时滑出淡出）
                    if (iconAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .size(buttonSize)
                                .align(Alignment.TopEnd)
                                .graphicsLayer {
                                    alpha = iconAlpha
                                    translationX = iconTranslationX.toPx()
                                    rotationZ = iconRotation
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // 2. 菜单内容层（以固定 menuWidth 且不受高度约束进行真实高度测量与布局）
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .requiredWidth(menuWidth)
                            .wrapContentHeight(unbounded = true)
                            .onGloballyPositioned { coordinates ->
                                val h = with(density) { coordinates.size.height.toDp() }
                                if (h > 44.dp && h != menuMeasuredHeight) {
                                    menuMeasuredHeight = h
                                }
                            }
                            .graphicsLayer {
                                alpha = contentAlpha
                                translationX = contentTranslationX.toPx()
                                scaleX = contentScale
                                scaleY = contentScale
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            scope.content()
                        }
                    }
                }
            }
        }
    }
}

/**
 * NemoMorphMenuItem - 形变菜单标准项
 */
@Composable
fun NemoMorphMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isDestructive: Boolean = false
) {
    val mainColor = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val iconTint = if (isDestructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgAlpha = if (isPressed) 0.08f else 0f

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = bgAlpha),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint.copy(alpha = if (enabled) 1f else 0.38f)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = mainColor.copy(alpha = if (enabled) 1f else 0.38f),
                modifier = Modifier.weight(1f)
            )

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconTint.copy(alpha = if (enabled) 0.7f else 0.38f)
                )
            }
        }
    }
}
