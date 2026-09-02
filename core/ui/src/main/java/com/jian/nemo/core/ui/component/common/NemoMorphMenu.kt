package com.jian.nemo.core.ui.component.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────────────────────────────────────
// 动画时序与缓动曲线常量
// ──────────────────────────────────────────────────────────────────────────────

/** 超调弹性曲线 — 容器形变（展开/收起）、图标层动画、菜单项入场共用 */
private val MorphEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

/** 标准减速曲线 — 菜单项退场 */
private val ItemExitEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

/** 容器（宽/高/圆角）形变时长 */
private const val MORPH_DURATION = 300

/** 图标（旋转/位移/淡出）渐变时长 */
private const val FADE_DURATION = 160

/** 单个菜单项入场动画时长 */
private const val ITEM_ENTER_DURATION = 320

/** 单个菜单项退场动画时长 */
private const val ITEM_EXIT_DURATION = 180

/** 入场错峰：相邻菜单项之间的延迟间隔 */
private const val ITEM_STAGGER_ENTER_DELAY = 35

/** 退场错峰：相邻菜单项之间的延迟间隔（逆序，最后一项先退） */
private const val ITEM_STAGGER_EXIT_DELAY = 20

/** 入场错峰：首个菜单项在容器开始展开后的初始等待 */
private const val ITEM_INITIAL_DELAY = 30

/**
 * Morph 菜单作用域，用于控制菜单项点击后的关闭操作，
 * 并为每个菜单项自动分配错峰动画索引。
 */
interface NemoMorphMenuScope {
    /** 关闭菜单 */
    fun close()
    /** 菜单当前是否处于展开状态 */
    val isExpanded: Boolean
    /** 根过渡动画驱动器 */
    val transition: androidx.compose.animation.core.Transition<Boolean>
    /** 获取下一个菜单项索引（Composition 期间自动递增） */
    fun nextItemIndex(): Int
    /** 菜单项总数 */
    val itemCount: Int
}

/**
 * NemoMorphMenu - 右上角下拉弹性展开菜单组件
 *
 * 核心动效规格：
 * 1. 触发按钮采用项目统一的 LiquidButton（液态物理微弹性与弥散阴影）。
 * 2. 精准局部锚定：以右上角为变换原点 (TransformOrigin(1f, 0f))，
 *    面板通过 scale(0.6 -> 1.0) + translateY(-8dp -> 0dp) + alpha(0 -> 1)
 *    搭配超调弹性曲线 (0.34, 1.56, 0.64, 1.0) 展开，具备极富弹性的丝滑回弹质感。
 * 3. 菜单项错峰：展开时逐项自右向左 (translateX 24dp -> 0dp, scale 0.9 -> 1.0) 错峰滑入，
 *    收起时逆序快速淡出，全程由 GPU 图层加速，零重排重测。
 *
 * @param modifier 修饰符
 * @param icon 触发按钮图标（默认三点图标 MoreVert）
 * @param contentDescription 无障碍描述
 * @param buttonSize 触发按钮尺寸（默认 44.dp）
 * @param menuWidth 展开后菜单面板宽度（默认 192.dp）
 * @param content 菜单内容槽（在 [NemoMorphMenuScope] 内组装菜单项）
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
    val haptic = LocalHapticFeedback.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val navGroupBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.White
    val containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val borderColor = if (isDarkTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)

    // 用于管理动画状态（确保收起动画播放完毕后再完全隐藏浮层）
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = expanded

    val transition = rememberTransition(transitionState, label = "NemoMenuTransition")

    // ── 面板整体硬件加速动画（右上角为原点进行 scale + translateY + alpha）──
    val panelScale by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = MORPH_DURATION, easing = MorphEasing)
        },
        label = "panelScale"
    ) { isOpen ->
        if (isOpen) 1f else 0.6f
    }

    val panelTranslationY by transition.animateDp(
        transitionSpec = {
            tween(durationMillis = MORPH_DURATION, easing = MorphEasing)
        },
        label = "panelTranslationY"
    ) { isOpen ->
        if (isOpen) 0.dp else (-8).dp
    }

    val panelAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            } else {
                tween(durationMillis = 140, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            }
        },
        label = "panelAlpha"
    ) { isOpen ->
        if (isOpen) 1f else 0f
    }

    // ── 菜单项错峰索引管理 ──
    val staggerInfo = remember {
        object {
            var counter = 0
            var total = 0
            var expanded = false
        }
    }
    staggerInfo.expanded = expanded

    val scope = remember(transition, onExpandedChange) {
        object : NemoMorphMenuScope {
            override fun close() { onExpandedChange(false) }
            override val isExpanded: Boolean get() = staggerInfo.expanded
            override val transition: androidx.compose.animation.core.Transition<Boolean> get() = transition
            override fun nextItemIndex(): Int = staggerInfo.counter++
            override val itemCount: Int get() = staggerInfo.total
        }
    }

    // 常态下的液态占位触发按钮（与 CommonHeader 返回按钮完全统一）
    Box(
        modifier = modifier.size(buttonSize),
        contentAlignment = Alignment.Center
    ) {
        LiquidButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onExpandedChange(true)
            },
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

        // 当需要渲染（展开中或正在进行收起动画）时挂载局部精准锚定的 Popup
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

                // 面板卡片容器（右上角原位弹性下拉展开）
                Box(
                    modifier = Modifier
                        .width(menuWidth)
                        .graphicsLayer {
                            scaleX = panelScale
                            scaleY = panelScale
                            translationY = panelTranslationY.toPx()
                            alpha = panelAlpha
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)
                        }
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(containerColor)
                        .border(
                            BorderStroke(0.5.dp, borderColor),
                            RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // 重置计数器 → content 中各菜单项按顺序递增 → 捕获总数
                        staggerInfo.counter = 0
                        scope.content()
                        staggerInfo.total = staggerInfo.counter
                    }
                }
            }
        }
    }
}

/**
 * NemoMorphMenuItem - 形变菜单标准项（内置错峰入场/退场动画）
 *
 * 动效规格：
 * - **入场**：延迟 80ms + index * 60ms，从右侧偏移 24dp + 缩放 0.9 伴随超调弹性曲线滑入
 * - **退场**：逆序延迟 (total - 1 - index) * 40ms，减速曲线快速消失
 */
@Composable
fun NemoMorphMenuScope.NemoMorphMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isDestructive: Boolean = false
) {
    val itemIndex = nextItemIndex()
    val totalCount = itemCount

    // ── 从 scope.transition 派生高帧率、无协程开销的错峰动画 ──
    val itemAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = ITEM_ENTER_DURATION,
                    delayMillis = (ITEM_INITIAL_DELAY + itemIndex * ITEM_STAGGER_ENTER_DELAY).coerceAtLeast(0),
                    easing = MorphEasing
                )
            } else {
                tween(
                    durationMillis = ITEM_EXIT_DURATION,
                    delayMillis = ((totalCount - 1 - itemIndex).coerceAtLeast(0) * ITEM_STAGGER_EXIT_DELAY),
                    easing = ItemExitEasing
                )
            }
        },
        label = "itemAlpha_$itemIndex"
    ) { isOpen ->
        if (isOpen) 1f else 0f
    }

    val itemTranslationX by transition.animateDp(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = ITEM_ENTER_DURATION,
                    delayMillis = (ITEM_INITIAL_DELAY + itemIndex * ITEM_STAGGER_ENTER_DELAY).coerceAtLeast(0),
                    easing = MorphEasing
                )
            } else {
                tween(
                    durationMillis = ITEM_EXIT_DURATION,
                    delayMillis = ((totalCount - 1 - itemIndex).coerceAtLeast(0) * ITEM_STAGGER_EXIT_DELAY),
                    easing = ItemExitEasing
                )
            }
        },
        label = "itemTranslationX_$itemIndex"
    ) { isOpen ->
        if (isOpen) 0.dp else 24.dp
    }

    val itemScale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = ITEM_ENTER_DURATION,
                    delayMillis = (ITEM_INITIAL_DELAY + itemIndex * ITEM_STAGGER_ENTER_DELAY).coerceAtLeast(0),
                    easing = MorphEasing
                )
            } else {
                tween(
                    durationMillis = ITEM_EXIT_DURATION,
                    delayMillis = ((totalCount - 1 - itemIndex).coerceAtLeast(0) * ITEM_STAGGER_EXIT_DELAY),
                    easing = ItemExitEasing
                )
            }
        },
        label = "itemScale_$itemIndex"
    ) { isOpen ->
        if (isOpen) 1f else 0.9f
    }

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
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = bgAlpha),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .graphicsLayer {
                alpha = itemAlpha
                translationX = itemTranslationX.toPx()
                scaleX = itemScale
                scaleY = itemScale
            }
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
