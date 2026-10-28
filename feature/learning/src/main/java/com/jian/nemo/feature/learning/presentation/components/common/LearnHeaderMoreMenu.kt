package com.jian.nemo.feature.learning.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.component.common.AppleMenuSectionDivider
import com.jian.nemo.core.ui.component.common.NemoMorphMenu
import com.jian.nemo.core.ui.component.common.NemoMorphMenuItem
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import com.kyant.backdrop.Backdrop

/**
 * 学习界面顶部更多操作菜单 (原汁原味圆形按钮 + 右上角弹性动效 + 跟随 App 主题 + 苹果式 Section 分组条)
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

    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

    // 原生圆形触发按钮 + 右上角原点超调弹性展开动画
    NemoMorphMenu(
        expanded = expanded,
        onExpandedChange = setExpanded,
        backdrop = backdrop,
        chromaticAberration = false,
        buttonSize = 48.dp,
        menuWidth = 240.dp,
        icon = Icons.Rounded.MoreVert,
        contentDescription = "更多选项",
        modifier = modifier
    ) {
        val hasCardActions = remainingCount > 0
        val hasSection1 = onShowRatingGuide != null || hasCardActions

        // ──────────────────────────────────────────────────────────
        // Section 1: 说明与卡片处置
        // ──────────────────────────────────────────────────────────
        if (onShowRatingGuide != null) {
            NemoMorphMenuItem(
                text = "评分说明（新学/复习）",
                leadingIcon = Icons.Rounded.CheckCircle,
                plumpIcon = PlumpIcon.BadgeCheck,
                iconColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else null,
                onClick = {
                    close()
                    onShowRatingGuide()
                }
            )
        }

        if (hasCardActions) {
            NemoMorphMenuItem(
                text = "暂停此卡片 (Suspend)",
                leadingIcon = Icons.Rounded.Pause,
                plumpIcon = PlumpIcon.Pause,
                iconColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else null,
                onClick = {
                    close()
                    onSuspend()
                }
            )
            NemoMorphMenuItem(
                text = "今日暂缓此项 (Bury)",
                leadingIcon = Icons.Rounded.AccessTime,
                plumpIcon = PlumpIcon.StudyClock,
                iconColor = if (isPlumpDuo) IsoluminantPalette.LilacViolet else null,
                onClick = {
                    close()
                    onBury()
                }
            )
            NemoMorphMenuItem(
                text = "报告条目错误",
                leadingIcon = Icons.Rounded.Report,
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
            AppleMenuSectionDivider(modifier = Modifier.padding(vertical = 4.dp))
        }

        if (onToggleAutoAudio != null) {
            MorphMenuToggleItem(
                text = "翻面自动朗读",
                checked = isAutoAudioEnabled,
                onCheckedChange = { onToggleAutoAudio(it) }
            )
        }

        if (onToggleShowAnswerDelay != null) {
            MorphMenuToggleItem(
                text = "显示答案等待",
                checked = isShowAnswerDelayEnabled,
                onCheckedChange = { onToggleShowAnswerDelay(it) }
            )

            if (onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null) {
                NemoMorphMenuItem(
                    text = "等待时长: $showAnswerDelayDurationLabel",
                    leadingIcon = Icons.Rounded.Timer,
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
            MorphMenuToggleItem(
                text = "自动翻面",
                checked = isAutoRevealAnswerEnabled,
                onCheckedChange = { onToggleAutoRevealAnswer(it) }
            )

            if (onCycleAutoRevealAnswerDuration != null || onOpenAutoRevealAnswerDialog != null) {
                NemoMorphMenuItem(
                    text = "翻面时长: $autoRevealAnswerDurationLabel",
                    leadingIcon = Icons.Rounded.Timer,
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
            NemoMorphMenuItem(
                text = "手写白板: $whiteboardLabel",
                leadingIcon = Icons.Rounded.Edit,
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
            AppleMenuSectionDivider(modifier = Modifier.padding(vertical = 4.dp))
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

        NemoMorphMenuItem(
            text = "显示模式: $themeLabel",
            leadingIcon = themeIcon,
            plumpIcon = themePlumpIcon,
            iconColor = if (isPlumpDuo) themeColor else null,
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
        shape = RoundedCornerShape(12.dp),
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
