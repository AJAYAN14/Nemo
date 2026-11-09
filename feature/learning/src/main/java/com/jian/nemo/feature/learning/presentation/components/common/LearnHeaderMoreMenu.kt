package com.jian.nemo.feature.learning.presentation.components.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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

internal fun calculateLearnMenuHeight(
    hasRatingGuide: Boolean,
    hasRemainingActions: Boolean,
    hasAutoAudio: Boolean,
    hasDelay: Boolean,
    hasAutoReveal: Boolean,
    hasWhiteboard: Boolean
): Dp {
    var height = 20.dp

    var hasTopSection = false
    if (hasRatingGuide) {
        height += 42.dp
        hasTopSection = true
    }
    if (hasRemainingActions) {
        height += 42.dp * 3
        hasTopSection = true
    }
    if (hasTopSection) {
        height += 21.dp
    }

    var hasPreferences = false
    if (hasAutoAudio) {
        height += 42.dp
        hasPreferences = true
    }
    if (hasDelay) {
        height += 58.dp
        hasPreferences = true
    }
    if (hasAutoReveal) {
        height += 58.dp
        hasPreferences = true
    }
    if (hasWhiteboard) {
        height += 42.dp
        hasPreferences = true
    }
    if (hasPreferences) {
        height += 21.dp
    }

    height += 30.dp
    height += 42.dp * 3

    return height
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
    val hasDelay = onToggleShowAnswerDelay != null || onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null
    val hasAutoReveal = onToggleAutoRevealAnswer != null || onCycleAutoRevealAnswerDuration != null || onOpenAutoRevealAnswerDialog != null
    val estimatedMenuHeight = remember(onToggleAutoAudio, onShowRatingGuide, remainingCount, onToggleWhiteboard, hasDelay, hasAutoReveal) {
        calculateLearnMenuHeight(
            hasRatingGuide = onShowRatingGuide != null,
            hasRemainingActions = remainingCount > 0,
            hasAutoAudio = onToggleAutoAudio != null,
            hasDelay = hasDelay,
            hasAutoReveal = hasAutoReveal,
            hasWhiteboard = onToggleWhiteboard != null
        )
    }

    NemoLiquidMenuOverlay(
        isVisible = isVisible,
        onDismiss = onDismiss,
        modifier = modifier,
        backdrop = backdrop,
        anchorBounds = anchorBounds,
        menuWidth = 295.dp,
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
                reservesCheck = true,
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
                reservesCheck = true,
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
                reservesCheck = true,
                onClick = {
                    onDismiss()
                    onSuspend()
                }
            )
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_item_report_error),
                leadingIcon = Icons.Rounded.Report,
                iconColor = IosColors.Red,
                reservesCheck = true,
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
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_toggle_auto_audio),
                checked = isAutoAudioEnabled,
                reservesCheck = true,
                leadingIcon = Icons.AutoMirrored.Rounded.VolumeUp,
                iconColor = IosColors.Teal,
                keepOpen = true,
                onClick = {
                    onToggleAutoAudio(!isAutoAudioEnabled)
                }
            )
        }

        if (onToggleShowAnswerDelay != null || onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null) {
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_toggle_delay),
                subtitle = stringResource(R.string.learn_menu_subtitle_current_duration, showAnswerDelayDurationLabel),
                reservesCheck = true,
                leadingIcon = Icons.Rounded.Timer,
                iconColor = IosColors.Purple,
                keepOpen = onOpenShowAnswerDelayDialog == null,
                onClick = {
                    if (onOpenShowAnswerDelayDialog != null) {
                        onDismiss()
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
                reservesCheck = true,
                leadingIcon = Icons.Rounded.Timer,
                iconColor = IosColors.Purple,
                keepOpen = onOpenAutoRevealAnswerDialog == null,
                onClick = {
                    if (onOpenAutoRevealAnswerDialog != null) {
                        onDismiss()
                        onOpenAutoRevealAnswerDialog()
                    } else {
                        onCycleAutoRevealAnswerDuration?.invoke()
                    }
                }
            )
        }

        if (onToggleWhiteboard != null) {
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_title_whiteboard),
                checked = isWhiteboardEnabled,
                reservesCheck = true,
                leadingIcon = Icons.Rounded.Edit,
                iconColor = IosColors.Blue,
                keepOpen = true,
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
