package com.jian.nemo.feature.learning.presentation.components.common

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.common.AnimatedMenuCloseIcon as CoreAnimatedMenuCloseIcon
import com.jian.nemo.core.ui.component.common.LocalLiquidMenuAnchorBounds
import com.jian.nemo.core.ui.component.common.LocalLiquidMenuMotionState
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuButton
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuDivider
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuItem
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuLabel
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuOverlay
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuToggleItem
import com.jian.nemo.core.ui.component.liquid.menu.rememberLiquidMenuMotionState
import com.jian.nemo.feature.learning.R
import com.kyant.backdrop.Backdrop

/**
 * 具有连续平滑弹性形变动效的菜单/关闭图标（圆润三横 <-> ✕）
 * 兼容性转发至通用底座组件
 */
@Composable
fun AnimatedMenuCloseIcon(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 22.dp
) {
    CoreAnimatedMenuCloseIcon(
        isExpanded = isExpanded,
        modifier = modifier,
        color = color,
        size = size
    )
}

/**
 * 学习界面顶部操作菜单触发按钮
 */
@Composable
fun LearnHeaderMoreMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    backdrop: Backdrop? = null,
    buttonSize: Dp = 48.dp,
    content: (@Composable () -> Unit)? = null
) {
    NemoLiquidMenuButton(
        isExpanded = isExpanded,
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        buttonSize = buttonSize,
        content = content
    )
}

/**
 * 学习界面液态玻璃悬浮操作菜单浮层
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
    anchorBounds: androidx.compose.ui.geometry.Rect? = null,
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
    onCycleDarkMode: () -> Unit = {},
    onSelectDarkMode: ((Boolean?) -> Unit)? = null
) {
    val estimatedMenuHeight = remember(onToggleAutoAudio, onShowRatingGuide, remainingCount, onToggleWhiteboard) {
        var h = 24.dp
        if (onShowRatingGuide != null) h += 48.dp
        if (remainingCount > 0) h += 48.dp * 3
        if (onShowRatingGuide != null || remainingCount > 0) h += 8.dp
        if (onToggleAutoAudio != null) h += 48.dp
        h += 60.dp * 2
        if (onToggleWhiteboard != null) h += 60.dp
        h += 8.dp
        h += 32.dp
        h += 48.dp * 3
        h
    }

    NemoLiquidMenuOverlay(
        isVisible = isVisible,
        onDismiss = onDismiss,
        modifier = modifier,
        backdrop = backdrop,
        anchorBounds = anchorBounds,
        menuWidth = 260.dp,
        estimatedHeight = estimatedMenuHeight,
        blurRadius = blurRadius,
        refractionHeightRatio = refractionHeightRatio,
        refractionAmountRatio = refractionAmountRatio
    ) {
        if (onShowRatingGuide != null) {
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_item_rating_guide),
                leadingIcon = Icons.Rounded.CheckCircle,
                iconColor = IosColors.Green,
                onClick = {
                    onDismiss()
                    onShowRatingGuide()
                }
            )
        }

        if (remainingCount > 0) {
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_item_bury),
                leadingIcon = Icons.Rounded.AccessTime,
                iconColor = IosColors.Indigo,
                onClick = {
                    onDismiss()
                    onBury()
                }
            )
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_item_suspend),
                leadingIcon = Icons.Rounded.Pause,
                iconColor = IosColors.Orange,
                isDestructive = true,
                onClick = {
                    onDismiss()
                    onSuspend()
                }
            )
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_item_report_error),
                leadingIcon = Icons.Rounded.Report,
                iconColor = IosColors.Red,
                onClick = {
                    onDismiss()
                    onReportError()
                }
            )
        }

        if (onShowRatingGuide != null || remainingCount > 0) {
            NemoLiquidMenuDivider()
        }

        if (onToggleAutoAudio != null) {
            NemoLiquidMenuToggleItem(
                text = stringResource(R.string.learn_menu_toggle_auto_audio),
                checked = isAutoAudioEnabled,
                onCheckedChange = { onToggleAutoAudio(it) }
            )
        }

        if (onToggleShowAnswerDelay != null || onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null) {
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_toggle_delay),
                subtitle = stringResource(R.string.learn_menu_subtitle_current_duration, showAnswerDelayDurationLabel),
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

        if (onToggleAutoRevealAnswer != null || onCycleAutoRevealAnswerDuration != null || onOpenAutoRevealAnswerDialog != null) {
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_toggle_auto_reveal),
                subtitle = stringResource(R.string.learn_menu_subtitle_current_duration, autoRevealAnswerDurationLabel),
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

        if (onToggleWhiteboard != null) {
            val whiteboardLabel = if (isWhiteboardEnabled) {
                stringResource(R.string.learn_menu_status_enabled)
            } else {
                stringResource(R.string.learn_menu_status_disabled)
            }
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_title_whiteboard),
                subtitle = whiteboardLabel,
                leadingIcon = Icons.Rounded.Edit,
                iconColor = IosColors.Blue,
                onClick = {
                    onToggleWhiteboard(!isWhiteboardEnabled)
                }
            )
        }

        val hasPreferenceItems = onToggleAutoAudio != null ||
            onToggleShowAnswerDelay != null || onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null ||
            onToggleAutoRevealAnswer != null || onCycleAutoRevealAnswerDuration != null || onOpenAutoRevealAnswerDialog != null ||
            onToggleWhiteboard != null

        if (hasPreferenceItems) {
            NemoLiquidMenuDivider()
        }

        NemoLiquidMenuLabel(text = stringResource(R.string.learn_menu_section_display_mode))

        NemoLiquidMenuItem(
            text = stringResource(R.string.learn_menu_theme_system),
            checked = isDarkMode == null,
            reservesCheck = true,
            leadingIcon = Icons.Rounded.SettingsBrightness,
            iconColor = IosColors.Blue,
            onClick = {
                onDismiss()
                if (onSelectDarkMode != null) {
                    onSelectDarkMode(null)
                } else {
                    onCycleDarkMode()
                }
            }
        )

        NemoLiquidMenuItem(
            text = stringResource(R.string.learn_menu_theme_dark),
            checked = isDarkMode == true,
            reservesCheck = true,
            leadingIcon = Icons.Rounded.DarkMode,
            iconColor = IosColors.Indigo,
            onClick = {
                onDismiss()
                if (onSelectDarkMode != null) {
                    onSelectDarkMode(true)
                } else {
                    onCycleDarkMode()
                }
            }
        )

        NemoLiquidMenuItem(
            text = stringResource(R.string.learn_menu_theme_light),
            checked = isDarkMode == false,
            reservesCheck = true,
            leadingIcon = Icons.Rounded.LightMode,
            iconColor = IosColors.Orange,
            onClick = {
                onDismiss()
                if (onSelectDarkMode != null) {
                    onSelectDarkMode(false)
                } else {
                    onCycleDarkMode()
                }
            }
        )
    }
}

/**
 * 聚合组件
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
    onSelectDarkMode: ((Boolean?) -> Unit)? = null,
    isMenuExpanded: Boolean = false,
    onToggleMenu: ((Boolean) -> Unit)? = null
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val expanded = if (onToggleMenu != null) isMenuExpanded else internalExpanded
    val setExpanded: (Boolean) -> Unit = { expandedValue ->
        internalExpanded = expandedValue
        onToggleMenu?.invoke(expandedValue)
    }

    val anchorBoundsState = remember { mutableStateOf<Rect?>(null) }
    val motionState = rememberLiquidMenuMotionState()

    CompositionLocalProvider(
        LocalLiquidMenuAnchorBounds provides anchorBoundsState,
        LocalLiquidMenuMotionState provides motionState
    ) {
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
            anchorBounds = anchorBoundsState.value,
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
            onCycleDarkMode = onCycleDarkMode,
            onSelectDarkMode = onSelectDarkMode
        )
    }
}
