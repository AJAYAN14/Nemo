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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.common.AnimatedMenuCloseIcon as CoreAnimatedMenuCloseIcon
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuButton
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuDivider
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuItem
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuOverlay
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuToggleItem
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
    buttonSize: Dp = 48.dp
) {
    NemoLiquidMenuButton(
        isExpanded = isExpanded,
        onClick = onClick,
        modifier = modifier,
        backdrop = backdrop,
        buttonSize = buttonSize
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
    NemoLiquidMenuOverlay(
        isVisible = isVisible,
        onDismiss = onDismiss,
        modifier = modifier,
        backdrop = backdrop,
        menuWidth = 260.dp,
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

            NemoLiquidMenuDivider()
        }

        if (remainingCount > 0) {
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_item_suspend),
                leadingIcon = Icons.Rounded.Pause,
                iconColor = IosColors.Orange,
                onClick = {
                    onDismiss()
                    onSuspend()
                }
            )
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
                text = stringResource(R.string.learn_menu_item_report_error),
                leadingIcon = Icons.Rounded.Report,
                iconColor = IosColors.Red,
                isDestructive = true,
                onClick = {
                    onDismiss()
                    onReportError()
                }
            )

            NemoLiquidMenuDivider()
        }

        if (onToggleAutoAudio != null) {
            NemoLiquidMenuToggleItem(
                text = stringResource(R.string.learn_menu_toggle_auto_audio),
                checked = isAutoAudioEnabled,
                onCheckedChange = { onToggleAutoAudio(it) }
            )
        }

        if (onToggleShowAnswerDelay != null) {
            NemoLiquidMenuToggleItem(
                text = stringResource(R.string.learn_menu_toggle_delay),
                checked = isShowAnswerDelayEnabled,
                onCheckedChange = { onToggleShowAnswerDelay(it) }
            )

            if (onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null) {
                NemoLiquidMenuItem(
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
            NemoLiquidMenuToggleItem(
                text = stringResource(R.string.learn_menu_toggle_auto_reveal),
                checked = isAutoRevealAnswerEnabled,
                onCheckedChange = { onToggleAutoRevealAnswer(it) }
            )

            if (onCycleAutoRevealAnswerDuration != null || onOpenAutoRevealAnswerDialog != null) {
                NemoLiquidMenuItem(
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
            NemoLiquidMenuItem(
                text = stringResource(R.string.learn_menu_item_whiteboard, whiteboardLabel),
                leadingIcon = Icons.Rounded.Edit,
                iconColor = IosColors.Blue,
                onClick = {
                    onToggleWhiteboard(!isWhiteboardEnabled)
                }
            )
        }

        NemoLiquidMenuDivider()

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

        NemoLiquidMenuItem(
            text = stringResource(R.string.learn_menu_item_display_mode, themeLabel),
            leadingIcon = themeIcon,
            iconColor = themeColor,
            onClick = {
                onCycleDarkMode()
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
