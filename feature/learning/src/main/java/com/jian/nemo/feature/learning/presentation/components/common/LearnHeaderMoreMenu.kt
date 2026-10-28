package com.jian.nemo.feature.learning.presentation.components.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.component.common.AppleDropdownMenu
import com.jian.nemo.core.ui.component.common.AppleDropdownMenuItem
import com.jian.nemo.core.ui.component.common.AppleDropdownMenuToggleItem
import com.jian.nemo.core.ui.component.common.AppleMenuSectionDivider
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState

/**
 * 学习界面顶部更多操作菜单 (苹果风格轻量化 Menu 改造)
 *
 * 采用 [AppleDropdownMenu] 进行改造：
 * 1. 视觉：毛玻璃半透明质感 + 漫反射软阴影 + 边缘微光高光描边；
 * 2. 结构：使用 [AppleMenuSectionDivider] 划分为「卡片处置/说明」、「学习设置开关」、「显示模式」三大 Section 分组；
 * 3. 交互：保留全部原有逻辑与事件回调。
 */
@Composable
fun LearnHeaderMoreMenu(
    remainingCount: Int,
    onSuspend: () -> Unit,
    onBury: () -> Unit,
    onReportError: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    hazeState: HazeState? = null,
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

    Box(modifier = modifier) {
        // 触发按钮：采用磨砂液态玻璃按钮，保持与整体风格浑然一体
        FrostedLiquidGlassButton(
            onClick = { setExpanded(!expanded) },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(10.dp),
            backdrop = backdrop
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "更多选项",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // 苹果风格轻量化下拉菜单
        AppleDropdownMenu(
            expanded = expanded,
            onDismissRequest = { setExpanded(false) },
            hazeState = hazeState,
            offset = DpOffset(x = 0.dp, y = 6.dp)
        ) {
            // ──────────────────────────────────────────────────────────
            // Section 1: 评分说明与卡片处置 (Suspend / Bury / Report)
            // ──────────────────────────────────────────────────────────
            val hasCardActions = remainingCount > 0
            val hasSection1 = onShowRatingGuide != null || hasCardActions

            if (onShowRatingGuide != null) {
                AppleDropdownMenuItem(
                    text = "评分说明（新学/复习）",
                    leadingIcon = Icons.Rounded.CheckCircle,
                    plumpIcon = PlumpIcon.BadgeCheck,
                    iconColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else null,
                    onClick = {
                        setExpanded(false)
                        onShowRatingGuide()
                    }
                )
            }

            if (hasCardActions) {
                AppleDropdownMenuItem(
                    text = "暂停此卡片 (Suspend)",
                    leadingIcon = Icons.Rounded.Pause,
                    plumpIcon = PlumpIcon.Pause,
                    iconColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else null,
                    onClick = {
                        setExpanded(false)
                        onSuspend()
                    }
                )
                AppleDropdownMenuItem(
                    text = "今日暂缓此项 (Bury)",
                    leadingIcon = Icons.Rounded.AccessTime,
                    plumpIcon = PlumpIcon.StudyClock,
                    iconColor = if (isPlumpDuo) IsoluminantPalette.LilacViolet else null,
                    onClick = {
                        setExpanded(false)
                        onBury()
                    }
                )
                AppleDropdownMenuItem(
                    text = "报告条目错误",
                    leadingIcon = Icons.Rounded.Report,
                    plumpIcon = PlumpIcon.CircleX,
                    iconColor = if (isPlumpDuo) IsoluminantPalette.CoralFlame else null,
                    onClick = {
                        setExpanded(false)
                        onReportError()
                    }
                )
            }

            // ──────────────────────────────────────────────────────────
            // Section 2: 学习辅助开关 (自动朗读 / 答案等待 / 自动翻面 / 白板)
            // ──────────────────────────────────────────────────────────
            val hasSection2 = onToggleAutoAudio != null ||
                onToggleShowAnswerDelay != null ||
                onToggleAutoRevealAnswer != null ||
                onToggleWhiteboard != null

            if (hasSection1 && hasSection2) {
                AppleMenuSectionDivider()
            }

            if (onToggleAutoAudio != null) {
                AppleDropdownMenuToggleItem(
                    text = "翻面自动朗读",
                    checked = isAutoAudioEnabled,
                    onCheckedChange = { onToggleAutoAudio(it) }
                )
            }

            if (onToggleShowAnswerDelay != null) {
                AppleDropdownMenuToggleItem(
                    text = "显示答案等待",
                    checked = isShowAnswerDelayEnabled,
                    onCheckedChange = { onToggleShowAnswerDelay(it) }
                )

                if (onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null) {
                    AppleDropdownMenuItem(
                        text = "等待时长",
                        leadingIcon = Icons.Rounded.Timer,
                        plumpIcon = PlumpIcon.StudyClock,
                        iconColor = if (isPlumpDuo) IsoluminantPalette.LilacViolet else null,
                        trailingText = showAnswerDelayDurationLabel,
                        onClick = {
                            setExpanded(false)
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
                AppleDropdownMenuToggleItem(
                    text = "自动翻面",
                    checked = isAutoRevealAnswerEnabled,
                    onCheckedChange = { onToggleAutoRevealAnswer(it) }
                )

                if (onCycleAutoRevealAnswerDuration != null || onOpenAutoRevealAnswerDialog != null) {
                    AppleDropdownMenuItem(
                        text = "翻面时长",
                        leadingIcon = Icons.Rounded.Timer,
                        plumpIcon = PlumpIcon.StudyClock,
                        iconColor = if (isPlumpDuo) IsoluminantPalette.LilacViolet else null,
                        trailingText = autoRevealAnswerDurationLabel,
                        onClick = {
                            setExpanded(false)
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
                AppleDropdownMenuToggleItem(
                    text = "手写白板",
                    checked = isWhiteboardEnabled,
                    onCheckedChange = { onToggleWhiteboard(it) }
                )
            }

            // ──────────────────────────────────────────────────────────
            // Section 3: 显示模式切换 (跟随系统 / 深色 / 浅色)
            // ──────────────────────────────────────────────────────────
            if (hasSection1 || hasSection2) {
                AppleMenuSectionDivider()
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

            AppleDropdownMenuItem(
                text = "显示模式",
                leadingIcon = themeIcon,
                plumpIcon = themePlumpIcon,
                iconColor = if (isPlumpDuo) themeColor else null,
                trailingText = themeLabel,
                onClick = {
                    onCycleDarkMode()
                }
            )
        }
    }
}
