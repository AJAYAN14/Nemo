package com.jian.nemo.feature.learning.presentation.components.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import com.jian.nemo.feature.learning.R
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
 * 具有连续平滑弹性形变动效的菜单/关闭图标（圆润三横 <-> ✕）
 *
 * @param isExpanded 菜单当前是否处于展开状态（true: ✕，false: 三横）
 * @param modifier 修饰符
 * @param color 图标线条颜色
 * @param size 图标尺寸
 */
@Composable
fun AnimatedMenuCloseIcon(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 22.dp
) {
    val progress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 380f
        ),
        label = "menuCloseMorph"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = 2.4.dp.toPx()
        val cap = StrokeCap.Round

        // 横线宽度比例（留出边缘保护，呈现饱满圆润视觉）
        val lineWidth = w * 0.70f
        val left = (w - lineWidth) / 2f
        val right = left + lineWidth
        val centerY = h / 2f

        // 三横分布间距
        val spacing = h * 0.22f
        val topY = centerY - spacing
        val bottomY = centerY + spacing

        // 1. 中间横线：随 progress 收缩并快速淡出
        val centerAlpha = (1f - progress * 2.5f).coerceIn(0f, 1f)
        val centerWidth = lineWidth * (1f - progress).coerceIn(0f, 1f)
        if (centerAlpha > 0f && centerWidth > 0f) {
            val cLeft = (w - centerWidth) / 2f
            val cRight = cLeft + centerWidth
            drawLine(
                color = color.copy(alpha = centerAlpha),
                start = Offset(cLeft, centerY),
                end = Offset(cRight, centerY),
                strokeWidth = strokeWidth,
                cap = cap
            )
        }

        // 2. 顶线：由水平状态 (left, topY) -> (right, topY) 移向中心并旋转 45°
        val currentTopY = (1f - progress) * topY + progress * centerY
        rotate(degrees = progress * 45f, pivot = Offset(w / 2f, currentTopY)) {
            drawLine(
                color = color,
                start = Offset(left, currentTopY),
                end = Offset(right, currentTopY),
                strokeWidth = strokeWidth,
                cap = cap
            )
        }

        // 3. 底线：由水平状态 (left, bottomY) -> (right, bottomY) 移向中心并旋转 -45°
        val currentBottomY = (1f - progress) * bottomY + progress * centerY
        rotate(degrees = progress * -45f, pivot = Offset(w / 2f, currentBottomY)) {
            drawLine(
                color = color,
                start = Offset(left, currentBottomY),
                end = Offset(right, currentBottomY),
                strokeWidth = strokeWidth,
                cap = cap
            )
        }
    }
}

/**
 * 学习界面顶部操作菜单触发按钮 (支持圆润三横与 ✕ 的平滑形变动效)
 */
@Composable
fun LearnHeaderMoreMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    backdrop: Backdrop? = null,
    buttonSize: Dp = 48.dp
) {
    val haptic = LocalHapticFeedback.current

    FrostedLiquidGlassButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = CircleShape,
        backdrop = backdrop,
        hasLens = backdrop != null,
        chromaticAberration = false,
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
 * 学习界面液态玻璃悬浮操作菜单浮层
 *
 * 核心渲染技术规格：
 * 1. 采用全屏同级兄弟采样架构，窗口背景保持透明手势拦截，不施加全局背景模糊与暗色遮罩；
 * 2. 菜单面板四周使用 22.dp G2 连续平滑曲率圆角与 0.6dp 垂直微光渐变描边；
 * 3. 采样层使用 12.dp 黄金模糊并结合响应式尺寸等比透镜折射与光学提亮；
 * 4. 动画驱动使用非对称流体弹性形变 Spring 曲线。
 */
@Composable
fun LearnHeaderMoreMenuOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    remainingCount: Int,
    onSuspend: () -> Unit,
    onBury: () -> Unit,
    onReportError: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    blurRadius: Dp = 12.dp,
    refractionHeightRatio: Float = 0.10f,
    refractionAmountRatio: Float = 0.20f,
    onShowRatingGuide: (() -> Unit)? = null,
    isAutoAudioEnabled: Boolean = false,
    onToggleAutoAudio: ((Boolean) -> Unit)? = null,
    isShowAnswerDelayEnabled: Boolean = false,
    onToggleShowAnswerDelay: ((Boolean) -> Unit)? = null,
    showAnswerDelayDurationLabel: String = "1.0s",
    onCycleShowAnswerDelayDuration: (() -> Unit)? = null,
    onOpenShowAnswerDelayDialog: (() -> Unit)? = null,
    isAutoRevealAnswerEnabled: Boolean = false,
    onToggleAutoRevealAnswer: ((Boolean) -> Unit)? = null,
    autoRevealAnswerDurationLabel: String = "5.0s",
    onCycleAutoRevealAnswerDuration: (() -> Unit)? = null,
    onOpenAutoRevealAnswerDialog: (() -> Unit)? = null,
    isWhiteboardEnabled: Boolean = false,
    onToggleWhiteboard: ((Boolean) -> Unit)? = null,
    isDarkMode: Boolean? = null,
    onCycleDarkMode: () -> Unit = {}
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

            val dividerColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

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
                    .widthIn(min = 230.dp, max = 260.dp)
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
                    if (onShowRatingGuide != null) {
                        LearnHeaderMenuItem(
                            text = stringResource(R.string.learn_menu_item_rating_guide),
                            leadingIcon = Icons.Rounded.CheckCircle,
                            iconColor = IosColors.Green,
                            onClick = {
                                onDismiss()
                                onShowRatingGuide()
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            color = dividerColor,
                            thickness = 0.5.dp
                        )
                    }

                    if (remainingCount > 0) {
                        LearnHeaderMenuItem(
                            text = stringResource(R.string.learn_menu_item_suspend),
                            leadingIcon = Icons.Rounded.Pause,
                            iconColor = IosColors.Orange,
                            onClick = {
                                onDismiss()
                                onSuspend()
                            }
                        )
                        LearnHeaderMenuItem(
                            text = stringResource(R.string.learn_menu_item_bury),
                            leadingIcon = Icons.Rounded.AccessTime,
                            iconColor = IosColors.Indigo,
                            onClick = {
                                onDismiss()
                                onBury()
                            }
                        )
                        LearnHeaderMenuItem(
                            text = stringResource(R.string.learn_menu_item_report_error),
                            leadingIcon = Icons.Rounded.Report,
                            iconColor = IosColors.Red,
                            isDestructive = true,
                            onClick = {
                                onDismiss()
                                onReportError()
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            color = dividerColor,
                            thickness = 0.5.dp
                        )
                    }

                    if (onToggleAutoAudio != null) {
                        MorphMenuToggleItem(
                            text = stringResource(R.string.learn_menu_toggle_auto_audio),
                            checked = isAutoAudioEnabled,
                            onCheckedChange = { onToggleAutoAudio(it) }
                        )
                    }

                    if (onToggleShowAnswerDelay != null) {
                        MorphMenuToggleItem(
                            text = stringResource(R.string.learn_menu_toggle_delay),
                            checked = isShowAnswerDelayEnabled,
                            onCheckedChange = { onToggleShowAnswerDelay(it) }
                        )

                        if (onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null) {
                            LearnHeaderMenuItem(
                                text = stringResource(R.string.learn_menu_item_delay_duration, showAnswerDelayDurationLabel),
                                leadingIcon = Icons.Rounded.Timer,
                                iconColor = IosColors.Purple,
                                onClick = {
                                    onDismiss()
                                    if (onOpenShowAnswerDelayDialog != null) {
                                        onOpenShowAnswerDelayDialog()
                                    } else {
                                        onCycleShowAnswerDelayDuration?.invoke()
                                    }
                                }
                            )
                        }
                    }

                    if (onToggleAutoRevealAnswer != null) {
                        MorphMenuToggleItem(
                            text = stringResource(R.string.learn_menu_toggle_auto_reveal),
                            checked = isAutoRevealAnswerEnabled,
                            onCheckedChange = { onToggleAutoRevealAnswer(it) }
                        )

                        if (onCycleAutoRevealAnswerDuration != null || onOpenAutoRevealAnswerDialog != null) {
                            LearnHeaderMenuItem(
                                text = stringResource(R.string.learn_menu_item_auto_reveal_duration, autoRevealAnswerDurationLabel),
                                leadingIcon = Icons.Rounded.Timer,
                                iconColor = IosColors.Purple,
                                onClick = {
                                    onDismiss()
                                    if (onOpenAutoRevealAnswerDialog != null) {
                                        onOpenAutoRevealAnswerDialog()
                                    } else {
                                        onCycleAutoRevealAnswerDuration?.invoke()
                                    }
                                }
                            )
                        }
                    }

                    if (onToggleWhiteboard != null) {
                        val whiteboardLabel = if (isWhiteboardEnabled) {
                            stringResource(R.string.learn_menu_status_enabled)
                        } else {
                            stringResource(R.string.learn_menu_status_disabled)
                        }
                        LearnHeaderMenuItem(
                            text = stringResource(R.string.learn_menu_item_whiteboard, whiteboardLabel),
                            leadingIcon = Icons.Rounded.Edit,
                            iconColor = IosColors.Blue,
                            onClick = {
                                onToggleWhiteboard(!isWhiteboardEnabled)
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        color = dividerColor,
                        thickness = 0.5.dp
                    )

                    val themeLabel = when (isDarkMode) {
                        null -> stringResource(R.string.learn_menu_theme_system)
                        true -> stringResource(R.string.learn_menu_theme_dark)
                        false -> stringResource(R.string.learn_menu_theme_light)
                    }
                    val themeIcon = when (isDarkMode) {
                        null -> Icons.Rounded.SettingsBrightness
                        true -> Icons.Rounded.DarkMode
                        false -> Icons.Rounded.LightMode
                    }
                    val themeColor = when (isDarkMode) {
                        null -> IosColors.Blue
                        true -> IosColors.Indigo
                        false -> IosColors.Orange
                    }

                    LearnHeaderMenuItem(
                        text = stringResource(R.string.learn_menu_item_display_mode, themeLabel),
                        leadingIcon = themeIcon,
                        iconColor = themeColor,
                        onClick = {
                            onCycleDarkMode()
                        }
                    )
                }
            }
        }
    }
}

/**
 * 学习界面顶部操作菜单标准项
 */
@Composable
private fun LearnHeaderMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    iconColor: Color? = null,
    isDestructive: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val destructiveColor = if (isDark) IosColors.RedDark else IosColors.Red
    val effectiveIconColor = if (isDestructive) destructiveColor else (iconColor ?: MaterialTheme.colorScheme.primary)
    val mainColor = if (isDestructive) destructiveColor else MaterialTheme.colorScheme.onSurface
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bgAlpha = if (isPressed) 0.08f else 0f
    val itemShape = RoundedRectangle(12.dp)

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
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
                color = mainColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 带有液态切换开关的菜单选项行
 */
@Composable
private fun MorphMenuToggleItem(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCheckedChange(!checked)
        },
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
                color = MaterialTheme.colorScheme.onSurface
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
 * 兼容原有调用的聚合组件
 */
@Composable
fun LearnHeaderMoreMenu(
    remainingCount: Int,
    onSuspend: () -> Unit,
    onBury: () -> Unit,
    onReportError: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    onShowRatingGuide: (() -> Unit)? = null,
    isAutoAudioEnabled: Boolean = false,
    onToggleAutoAudio: ((Boolean) -> Unit)? = null,
    isShowAnswerDelayEnabled: Boolean = false,
    onToggleShowAnswerDelay: ((Boolean) -> Unit)? = null,
    showAnswerDelayDurationLabel: String = "1.0s",
    onCycleShowAnswerDelayDuration: (() -> Unit)? = null,
    onOpenShowAnswerDelayDialog: (() -> Unit)? = null,
    isAutoRevealAnswerEnabled: Boolean = false,
    onToggleAutoRevealAnswer: ((Boolean) -> Unit)? = null,
    autoRevealAnswerDurationLabel: String = "5.0s",
    onCycleAutoRevealAnswerDuration: (() -> Unit)? = null,
    onOpenAutoRevealAnswerDialog: (() -> Unit)? = null,
    isWhiteboardEnabled: Boolean = false,
    onToggleWhiteboard: ((Boolean) -> Unit)? = null,
    isDarkMode: Boolean? = null,
    onCycleDarkMode: () -> Unit = {},
    isMenuExpanded: Boolean = false,
    onToggleMenu: ((Boolean) -> Unit)? = null
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val expanded = if (onToggleMenu != null) isMenuExpanded else internalExpanded
    val setExpanded: (Boolean) -> Unit = { expandedValue ->
        internalExpanded = expandedValue
        onToggleMenu?.invoke(expandedValue)
    }

    LearnHeaderMoreMenuButton(
        onClick = { setExpanded(!expanded) },
        isExpanded = expanded,
        modifier = modifier,
        backdrop = backdrop
    )

    LearnHeaderMoreMenuOverlay(
        isVisible = expanded,
        onDismiss = { setExpanded(false) },
        remainingCount = remainingCount,
        onSuspend = onSuspend,
        onBury = onBury,
        onReportError = onReportError,
        backdrop = backdrop,
        onShowRatingGuide = onShowRatingGuide,
        isAutoAudioEnabled = isAutoAudioEnabled,
        onToggleAutoAudio = onToggleAutoAudio,
        isShowAnswerDelayEnabled = isShowAnswerDelayEnabled,
        onToggleShowAnswerDelay = onToggleShowAnswerDelay,
        showAnswerDelayDurationLabel = showAnswerDelayDurationLabel,
        onCycleShowAnswerDelayDuration = onCycleShowAnswerDelayDuration,
        onOpenShowAnswerDelayDialog = onOpenShowAnswerDelayDialog,
        isAutoRevealAnswerEnabled = isAutoRevealAnswerEnabled,
        onToggleAutoRevealAnswer = onToggleAutoRevealAnswer,
        autoRevealAnswerDurationLabel = autoRevealAnswerDurationLabel,
        onCycleAutoRevealAnswerDuration = onCycleAutoRevealAnswerDuration,
        onOpenAutoRevealAnswerDialog = onOpenAutoRevealAnswerDialog,
        isWhiteboardEnabled = isWhiteboardEnabled,
        onToggleWhiteboard = onToggleWhiteboard,
        isDarkMode = isDarkMode,
        onCycleDarkMode = onCycleDarkMode
    )
}
