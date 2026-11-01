package com.jian.nemo.feature.learning.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.learning.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.common.NemoMorphMenu
import com.jian.nemo.core.ui.component.common.NemoMorphMenuItem
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import com.kyant.backdrop.Backdrop

/**
 * 学习界面顶部更多操作菜单 (包含卡片操作、学习设置开关与模式切换)
 *
 * 遵循单一官方原生图标与调色盘标准规范：
 * - 统一采用 Google Fonts 原生实心 Rounded 图标
 * - 配色体系全面收编至设计调色盘 IosColors (一套颜色体系)
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

    NemoMorphMenu(
        expanded = expanded,
        onExpandedChange = setExpanded,
        backdrop = backdrop,
        chromaticAberration = backdrop != null,
        buttonSize = 48.dp,
        menuWidth = 240.dp,
        icon = Icons.Rounded.MoreVert,
        contentDescription = stringResource(R.string.learn_menu_desc_more),
        modifier = modifier
    ) {
        if (onShowRatingGuide != null) {
            NemoMorphMenuItem(
                text = stringResource(R.string.learn_menu_item_rating_guide),
                leadingIcon = Icons.Rounded.CheckCircle,
                iconColor = IosColors.Green,
                onClick = {
                    close()
                    onShowRatingGuide()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }

        if (remainingCount > 0) {
            NemoMorphMenuItem(
                text = stringResource(R.string.learn_menu_item_suspend),
                leadingIcon = Icons.Rounded.Pause,
                iconColor = IosColors.Orange,
                onClick = {
                    close()
                    onSuspend()
                }
            )
            NemoMorphMenuItem(
                text = stringResource(R.string.learn_menu_item_bury),
                leadingIcon = Icons.Rounded.AccessTime,
                iconColor = IosColors.Indigo,
                onClick = {
                    close()
                    onBury()
                }
            )

            NemoMorphMenuItem(
                text = stringResource(R.string.learn_menu_item_report_error),
                leadingIcon = Icons.Rounded.Report,
                iconColor = IosColors.Red,
                isDestructive = true,
                onClick = {
                    close()
                    onReportError()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }

        // 自动朗读开关
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
                NemoMorphMenuItem(
                    text = stringResource(R.string.learn_menu_item_delay_duration, showAnswerDelayDurationLabel),
                    leadingIcon = Icons.Rounded.Timer,
                    iconColor = IosColors.Purple,
                    onClick = {
                        close()
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
                NemoMorphMenuItem(
                    text = stringResource(R.string.learn_menu_item_auto_reveal_duration, autoRevealAnswerDurationLabel),
                    leadingIcon = Icons.Rounded.Timer,
                    iconColor = IosColors.Purple,
                    onClick = {
                        close()
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
            val whiteboardLabel = if (isWhiteboardEnabled) stringResource(R.string.learn_menu_status_enabled) else stringResource(R.string.learn_menu_status_disabled)
            NemoMorphMenuItem(
                text = stringResource(R.string.learn_menu_item_whiteboard, whiteboardLabel),
                leadingIcon = Icons.Rounded.Edit,
                iconColor = IosColors.Blue,
                onClick = {
                    onToggleWhiteboard(!isWhiteboardEnabled)
                }
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        // 主题切换项 (循环模式)
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

        NemoMorphMenuItem(
            text = stringResource(R.string.learn_menu_item_display_mode, themeLabel),
            leadingIcon = themeIcon,
            iconColor = themeColor,
            onClick = {
                onCycleDarkMode()
            }
        )
    }
}

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
