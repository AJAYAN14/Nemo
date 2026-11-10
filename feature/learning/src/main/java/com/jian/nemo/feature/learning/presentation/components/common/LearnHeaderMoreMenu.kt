package com.jian.nemo.feature.learning.presentation.components.common

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.component.common.AnimatedMenuCloseIcon as CoreAnimatedMenuCloseIcon
import com.jian.nemo.core.ui.component.common.LocalLiquidMenuAnchorBounds
import com.jian.nemo.core.ui.component.common.LocalLiquidMenuMotionState
import com.jian.nemo.core.ui.component.common.NemoLiquidMenuButton
import com.jian.nemo.core.ui.component.common.NemoNativeLiquidMenuTrigger
import com.jian.nemo.core.ui.component.liquid.menu.records.GlassMenuItemRecord
import com.jian.nemo.core.ui.component.liquid.menu.rememberLiquidMenuMotionState
import com.jian.nemo.feature.learning.R
import com.kyant.backdrop.Backdrop

/**
 * 全局共享的活跃原生学习菜单项清单
 */
internal val ActiveNativeLearnMenuItems = mutableStateOf<List<GlassMenuItemRecord>>(emptyList())

/**
 * 全局共享的活跃原生学习菜单选项动作分发回调
 */
internal val ActiveNativeLearnMenuActionHandler = mutableStateOf<((String) -> Unit)?>(null)

/**
 * 全局共享的活跃原生学习菜单选项长按动作分发回调
 */
internal val ActiveNativeLearnMenuLongActionHandler = mutableStateOf<((String) -> Unit)?>(null)

/**
 * 全局共享的原生学习菜单展开状态
 */
internal val ActiveNativeLearnMenuOpenState = mutableStateOf(false)

/**
 * 具有连续平滑弹性形变动效的菜单/关闭图标（圆润三横 <-> ✕）
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
 * 学习界面顶部操作菜单触发按钮（底层对接原生 NemoGlassMenuView 引擎）
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
    val haptic = LocalHapticFeedback.current
    val items = ActiveNativeLearnMenuItems.value
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    if (items.isNotEmpty()) {
        NemoNativeLiquidMenuTrigger(
            items = items,
            modifier = modifier,
            buttonSize = buttonSize,
            colorScheme = if (isDarkTheme) "dark" else "light",
            onSelect = { item, _ ->
                ActiveNativeLearnMenuActionHandler.value?.invoke(item.id)
            },
            onLongSelect = { item, _ ->
                ActiveNativeLearnMenuLongActionHandler.value?.invoke(item.id)
            },
            onOpenChange = { open ->
                ActiveNativeLearnMenuOpenState.value = open
                if (open != isExpanded) {
                    onClick()
                }
            },
            content = content
        )
    } else {
        NemoLiquidMenuButton(
            isExpanded = isExpanded,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
            modifier = modifier,
            backdrop = backdrop,
            buttonSize = buttonSize,
            content = content
        )
    }
}

/**
 * 构建原生液态菜单项结构
 */
internal fun buildLearnMenuNativeItems(
    context: Context,
    remainingCount: Int,
    hasRatingGuide: Boolean,
    hasAutoAudio: Boolean,
    isAutoAudioEnabled: Boolean,
    hasDelay: Boolean,
    isShowAnswerDelayEnabled: Boolean,
    showAnswerDelayDurationLabel: String,
    hasAutoReveal: Boolean,
    isAutoRevealAnswerEnabled: Boolean,
    autoRevealAnswerDurationLabel: String,
    hasWhiteboard: Boolean,
    isWhiteboardEnabled: Boolean,
    isDarkMode: Boolean?
): List<GlassMenuItemRecord> {
    val list = mutableListOf<GlassMenuItemRecord>()

    if (hasRatingGuide) {
        list.add(
            GlassMenuItemRecord(
                id = "rating_guide",
                title = context.getString(R.string.learn_menu_item_rating_guide),
                icon = "info",
                iconColor = IosColors.Green.toArgb()
            )
        )
    }

    if (remainingCount > 0) {
        list.add(
            GlassMenuItemRecord(
                id = "bury",
                title = context.getString(R.string.learn_menu_item_bury),
                icon = "calendar",
                iconColor = IosColors.Blue.toArgb()
            )
        )
        list.add(
            GlassMenuItemRecord(
                id = "suspend",
                title = context.getString(R.string.learn_menu_item_suspend),
                icon = "trash",
                destructive = true,
                iconColor = IosColors.Orange.toArgb()
            )
        )
        list.add(
            GlassMenuItemRecord(
                id = "report_error",
                title = context.getString(R.string.learn_menu_item_report_error),
                icon = "report",
                iconColor = IosColors.Red.toArgb()
            )
        )
    }

    if (hasRatingGuide || remainingCount > 0) {
        list.add(GlassMenuItemRecord(separator = true))
    }

    if (hasAutoAudio) {
        list.add(
            GlassMenuItemRecord(
                id = "auto_audio",
                title = context.getString(R.string.learn_menu_toggle_auto_audio),
                icon = "volume",
                checked = isAutoAudioEnabled,
                keepOpen = true,
                iconColor = IosColors.Teal.toArgb()
            )
        )
    }

    if (hasDelay) {
        val delaySubtitle = if (isShowAnswerDelayEnabled) {
            context.getString(R.string.learn_menu_subtitle_current_duration, showAnswerDelayDurationLabel)
        } else {
            context.getString(R.string.learn_menu_status_disabled)
        }
        list.add(
            GlassMenuItemRecord(
                id = "delay",
                title = context.getString(R.string.learn_menu_toggle_delay),
                subtitle = delaySubtitle,
                icon = "clock",
                checked = isShowAnswerDelayEnabled,
                keepOpen = true,
                iconColor = IosColors.Purple.toArgb()
            )
        )
    }

    if (hasAutoReveal) {
        val autoRevealSubtitle = if (isAutoRevealAnswerEnabled) {
            context.getString(R.string.learn_menu_subtitle_current_duration, autoRevealAnswerDurationLabel)
        } else {
            context.getString(R.string.learn_menu_status_disabled)
        }
        list.add(
            GlassMenuItemRecord(
                id = "auto_reveal",
                title = context.getString(R.string.learn_menu_toggle_auto_reveal),
                subtitle = autoRevealSubtitle,
                icon = "flip",
                checked = isAutoRevealAnswerEnabled,
                keepOpen = true,
                iconColor = IosColors.Indigo.toArgb()
            )
        )
    }

    if (hasWhiteboard) {
        list.add(
            GlassMenuItemRecord(
                id = "whiteboard",
                title = context.getString(R.string.learn_menu_title_whiteboard),
                icon = "pencil",
                checked = isWhiteboardEnabled,
                keepOpen = true,
                iconColor = IosColors.Blue.toArgb()
            )
        )
    }

    val hasPreferences = hasAutoAudio || hasDelay || hasAutoReveal || hasWhiteboard
    if (hasPreferences) {
        list.add(GlassMenuItemRecord(separator = true))
    }

    list.add(
        GlassMenuItemRecord(
            id = "heading_display_mode",
            title = context.getString(R.string.learn_menu_section_display_mode),
            heading = true
        )
    )

    list.add(
        GlassMenuItemRecord(
            id = "theme_system",
            title = context.getString(R.string.learn_menu_theme_system),
            icon = "grid",
            checked = isDarkMode == null,
            iconColor = IosColors.Gray.toArgb()
        )
    )

    list.add(
        GlassMenuItemRecord(
            id = "theme_dark",
            title = context.getString(R.string.learn_menu_theme_dark),
            icon = "moon",
            checked = isDarkMode == true,
            iconColor = IosColors.Indigo.toArgb()
        )
    )

    list.add(
        GlassMenuItemRecord(
            id = "theme_light",
            title = context.getString(R.string.learn_menu_theme_light),
            icon = "sun",
            checked = isDarkMode == false,
            iconColor = IosColors.Orange.toArgb()
        )
    )

    return list
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
    anchorBounds: Rect? = null,
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
    val context = LocalContext.current
    val hasDelay = onToggleShowAnswerDelay != null || onCycleShowAnswerDelayDuration != null || onOpenShowAnswerDelayDialog != null
    val hasAutoReveal = onToggleAutoRevealAnswer != null || onCycleAutoRevealAnswerDuration != null || onOpenAutoRevealAnswerDialog != null

    val nativeItems = remember(
        remainingCount, onShowRatingGuide, isAutoAudioEnabled, onToggleAutoAudio,
        hasDelay, isShowAnswerDelayEnabled, showAnswerDelayDurationLabel,
        hasAutoReveal, isAutoRevealAnswerEnabled, autoRevealAnswerDurationLabel,
        isWhiteboardEnabled, onToggleWhiteboard, isDarkMode
    ) {
        buildLearnMenuNativeItems(
            context = context,
            remainingCount = remainingCount,
            hasRatingGuide = onShowRatingGuide != null,
            hasAutoAudio = onToggleAutoAudio != null,
            isAutoAudioEnabled = isAutoAudioEnabled,
            hasDelay = hasDelay,
            isShowAnswerDelayEnabled = isShowAnswerDelayEnabled,
            showAnswerDelayDurationLabel = showAnswerDelayDurationLabel,
            hasAutoReveal = hasAutoReveal,
            isAutoRevealAnswerEnabled = isAutoRevealAnswerEnabled,
            autoRevealAnswerDurationLabel = autoRevealAnswerDurationLabel,
            hasWhiteboard = onToggleWhiteboard != null,
            isWhiteboardEnabled = isWhiteboardEnabled,
            isDarkMode = isDarkMode
        )
    }

    SideEffect {
        ActiveNativeLearnMenuItems.value = nativeItems
        ActiveNativeLearnMenuActionHandler.value = { id ->
            when (id) {
                "rating_guide" -> onShowRatingGuide?.invoke()
                "bury" -> onBury()
                "suspend" -> onSuspend()
                "report_error" -> onReportError()
                "auto_audio" -> onToggleAutoAudio?.invoke(!isAutoAudioEnabled)
                "delay" -> onToggleShowAnswerDelay?.invoke(!isShowAnswerDelayEnabled)
                "auto_reveal" -> onToggleAutoRevealAnswer?.invoke(!isAutoRevealAnswerEnabled)
                "whiteboard" -> onToggleWhiteboard?.invoke(!isWhiteboardEnabled)
                "theme_system" -> if (onSelectDarkMode != null) onSelectDarkMode(null) else onCycleDarkMode()
                "theme_dark" -> if (onSelectDarkMode != null) onSelectDarkMode(true) else onCycleDarkMode()
                "theme_light" -> if (onSelectDarkMode != null) onSelectDarkMode(false) else onCycleDarkMode()
            }
        }
        ActiveNativeLearnMenuLongActionHandler.value = { id ->
            when (id) {
                "delay" -> {
                    if (onOpenShowAnswerDelayDialog != null) {
                        onOpenShowAnswerDelayDialog()
                    } else {
                        onCycleShowAnswerDelayDuration?.invoke()
                    }
                }
                "auto_reveal" -> {
                    if (onOpenAutoRevealAnswerDialog != null) {
                        onOpenAutoRevealAnswerDialog()
                    } else {
                        onCycleAutoRevealAnswerDuration?.invoke()
                    }
                }
            }
        }
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

        LearnHeaderMoreMenuButton(
            onClick = { setExpanded(!expanded) },
            isExpanded = expanded,
            modifier = modifier,
            backdrop = backdrop
        )
    }
}
