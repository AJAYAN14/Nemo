package com.jian.nemo.feature.learning.presentation.components.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.common.NemoAppleMenuItem
import com.jian.nemo.core.ui.component.common.NemoAppleMenuScope
import com.jian.nemo.core.ui.component.common.NemoAppleMenuSectionDivider
import com.jian.nemo.core.ui.component.common.NemoAppleMenuToggleItem
import com.kyant.backdrop.Backdrop

/**
 * 学习界面顶部更多操作触发按钮 (原版 48dp 圆形按钮 + 液态凸透镜物理折射 + 全局坐标捕获)
 */
@Composable
fun LearnHeaderMoreMenuButton(
    onClick: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    chromaticAberration: Boolean = false,
    contentDescription: String = "更多选项"
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
        chromaticAberration = chromaticAberration,
        contentPadding = PaddingValues(0.dp),
        isInteractive = true,
        modifier = modifier
            .size(48.dp)
            .onGloballyPositioned { coordinates ->
                onBoundsChanged(coordinates.boundsInWindow())
            }
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(30.dp)
        )
    }
}

/**
 * 学习界面顶部菜单项列表内容 (完全对齐 Cresto Demo 苹果风格)
 */
@Composable
fun NemoAppleMenuScope.LearnHeaderMoreMenuItems(
    remainingCount: Int,
    onSuspend: () -> Unit,
    onBury: () -> Unit,
    onReportError: () -> Unit,
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
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
    val hasCardActions = remainingCount > 0
    val hasSection1 = onShowRatingGuide != null || hasCardActions

    // ──────────────────────────────────────────────────────────
    // Section 1: 说明与卡片处置
    // ──────────────────────────────────────────────────────────
    if (onShowRatingGuide != null) {
        NemoAppleMenuItem(
            text = "评分说明（新学/复习）",
            icon = Icons.Rounded.CheckCircle,
            plumpIcon = PlumpIcon.BadgeCheck,
            iconColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else null,
            onClick = {
                close()
                onShowRatingGuide()
            }
        )
    }

    if (hasCardActions) {
        NemoAppleMenuItem(
            text = "暂停此卡片 (Suspend)",
            icon = Icons.Rounded.Pause,
            plumpIcon = PlumpIcon.Pause,
            iconColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else null,
            onClick = {
                close()
                onSuspend()
            }
        )
        NemoAppleMenuItem(
            text = "今日暂缓此项 (Bury)",
            icon = Icons.Rounded.AccessTime,
            plumpIcon = PlumpIcon.StudyClock,
            iconColor = if (isPlumpDuo) IsoluminantPalette.LilacViolet else null,
            onClick = {
                close()
                onBury()
            }
        )
        NemoAppleMenuItem(
            text = "报告条目错误",
            icon = Icons.Rounded.Report,
            plumpIcon = PlumpIcon.CircleX,
            iconColor = if (isPlumpDuo) IsoluminantPalette.CoralFlame else null,
            onClick = {
                close()
                onReportError()
            }
        )
    }

    // ──────────────────────────────────────────────────────────
    // Section 2: 学习设置开关 (苹果风格 Section 底槽分隔)
    // ──────────────────────────────────────────────────────────
    val hasSection2 = onToggleAutoAudio != null ||
        onToggleShowAnswerDelay != null ||
        onToggleAutoRevealAnswer != null ||
        onToggleWhiteboard != null

    if (hasSection1 && hasSection2) {
        NemoAppleMenuSectionDivider(modifier = Modifier.padding(vertical = 4.dp))
    }

    if (onToggleAutoAudio != null) {
        NemoAppleMenuToggleItem(
            text = "翻面自动朗读",
            checked = isAutoAudioEnabled,
            onCheckedChange = { onToggleAutoAudio(it) }
        )
    }

    if (onToggleShowAnswerDelay != null) {
        NemoAppleMenuToggleItem(
            text = "显示答案等待",
            checked = isShowAnswerDelayEnabled,
            onCheckedChange = { onToggleShowAnswerDelay(it) }
        )

        if (onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null) {
            NemoAppleMenuItem(
                text = "等待时长: $showAnswerDelayDurationLabel",
                icon = Icons.Rounded.Timer,
                plumpIcon = PlumpIcon.StudyClock,
                iconColor = if (isPlumpDuo) IsoluminantPalette.LilacViolet else null,
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
        NemoAppleMenuToggleItem(
            text = "自动翻面",
            checked = isAutoRevealAnswerEnabled,
            onCheckedChange = { onToggleAutoRevealAnswer(it) }
        )

        if (onCycleAutoRevealAnswerDuration != null || onOpenAutoRevealAnswerDialog != null) {
            NemoAppleMenuItem(
                text = "翻面时长: $autoRevealAnswerDurationLabel",
                icon = Icons.Rounded.Timer,
                plumpIcon = PlumpIcon.StudyClock,
                iconColor = if (isPlumpDuo) IsoluminantPalette.LilacViolet else null,
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
        val whiteboardLabel = if (isWhiteboardEnabled) "已开启" else "已关闭"
        NemoAppleMenuItem(
            text = "手写白板: $whiteboardLabel",
            icon = Icons.Rounded.Edit,
            plumpIcon = PlumpIcon.IconLab,
            iconColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else null,
            onClick = {
                onToggleWhiteboard(!isWhiteboardEnabled)
            }
        )
    }

    // ──────────────────────────────────────────────────────────
    // Section 3: 显示模式切换 (跟随 App 主题)
    // ──────────────────────────────────────────────────────────
    if (hasSection1 || hasSection2) {
        NemoAppleMenuSectionDivider(modifier = Modifier.padding(vertical = 4.dp))
    }

    val themeLabel = when (isDarkMode) {
        null -> "跟随系统"
        true -> "深色模式"
        false -> "浅色模式"
    }
    val themeIcon = when (isDarkMode) {
        null -> Icons.Rounded.SettingsBrightness
        true -> Icons.Rounded.DarkMode
        false -> Icons.Rounded.LightMode
    }
    val themePlumpIcon = when (isDarkMode) {
        null -> PlumpIcon.ThemePalette
        true -> PlumpIcon.Moon
        false -> PlumpIcon.Sun
    }
    val themeColor = when (isDarkMode) {
        null -> IsoluminantPalette.SkyIndigo
        true -> IsoluminantPalette.LilacViolet
        false -> IsoluminantPalette.WarmAmber
    }

    NemoAppleMenuItem(
        text = "显示模式: $themeLabel",
        icon = themeIcon,
        plumpIcon = themePlumpIcon,
        iconColor = if (isPlumpDuo) themeColor else null,
        onClick = {
            onCycleDarkMode()
        }
    )
}
