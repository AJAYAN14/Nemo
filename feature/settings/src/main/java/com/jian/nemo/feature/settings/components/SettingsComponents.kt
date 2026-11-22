package com.jian.nemo.feature.settings.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.settings.R
import java.util.Locale
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.sheet.NemoModalBottomSheet
import com.jian.nemo.core.ui.component.sheet.NemoPageBottomSheet
import com.jian.nemo.core.ui.component.sheet.NemoSheetTopBar
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 定义常量
private const val ANIMATION_DURATION = 200
private const val PRESS_SCALE = 0.98f

/**
 * 沉浸式设置页头部 (V2)
 */
@Composable
fun ImmersiveSettingsHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp, top = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * Premium Card (V2: Clean White/Dark + Soft Shadow)
 * 同样的 PremiumCard 设计，保持全 App 统一
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    Surface(
        shape = RoundedRectangle(24.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 24.dp, isDark = isDark),
        content = { Column(content = content) }
    )
}

/**
 * 当前设置界面全局图标视觉风格 CompositionLocal (默认 SOFT_TRANSLUCENT)
 */
val LocalSettingIconStyle = compositionLocalOf { SettingIconStyle.SOFT_TRANSLUCENT }

/**
 * 设置列表项组件 (支持传统单 ImageVector)
 */
@Composable
fun SquircleSettingItem(
    icon: ImageVector,
    iconColor: Color,
    iconStyle: SettingIconStyle = LocalSettingIconStyle.current,
    iconRotation: Float = 0f,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
    onClickWithBounds: ((Rect) -> Unit)? = null,
    showDivider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null // 用于 Switch 或 TextArrow
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESS_SCALE else 1f,
        label = "scale",
        animationSpec = tween(ANIMATION_DURATION)
    )
    var itemBounds by remember { mutableStateOf(Rect.Zero) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    itemBounds = coordinates.boundsInWindow()
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // 自定义按压效果，去除默认 ripple
                    onClick = {
                        if (onClickWithBounds != null) {
                            onClickWithBounds(itemBounds)
                        } else {
                            onClick()
                        }
                    }
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 双轨风格支持 (单容器纯复用架构)：
            // 风格 1 (柔和微透): 15% 半透明底框 + 彩色原色图标
            // 风格 2 (活力纯色): 100% 实心高饱和底框 + 纯白反白图标
            val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
            val containerBgColor = if (isSoftTranslucent) iconColor.copy(alpha = 0.15f) else iconColor
            val iconTint = if (isSoftTranslucent) iconColor else Color.White

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(containerBgColor, RoundedRectangle(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer { rotationZ = iconRotation }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Trailing Content
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 74.dp), // Align with text start
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )
        }
    }
}

/**
 * 分组标题 (V2)
 */
@Composable
fun SettingsSectionTitle(
    text: String,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * 每日单词目标选择 BottomSheet (V2: Premium Squircle Selection)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyGoalSelectionBottomSheet(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onGoalSelected: (Int) -> Unit
) {
    val goals = listOf(5, 10, 20, 30, 50)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = if (isDark) SystemColors.OrangeDark else SystemColors.Orange
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val containerBg = if (isSoftTranslucent) accentColor.copy(alpha = 0.15f) else accentColor
    val iconTint = if (isSoftTranslucent) accentColor else Color.White

    NemoModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(containerBg, RoundedRectangle(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrackChanges,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.settings_components_title_daily_word_goal),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // List
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                goals.forEach { goal ->
                    GoalSelectionItem(
                        text = stringResource(R.string.settings_components_format_word_goal, goal),
                        isSelected = (goal == currentGoal),
                        color = accentColor,
                        onClick = { onGoalSelected(goal) }
                    )
                }
            }
        }
    }
}

/**
 * 每日语法目标选择 BottomSheet (V2: Premium Squircle Selection)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarDailyGoalSelectionBottomSheet(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onGoalSelected: (Int) -> Unit
) {
    val goals = listOf(5, 10, 15, 20, 25)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = if (isDark) SystemColors.GreenDark else SystemColors.Green
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val containerBg = if (isSoftTranslucent) accentColor.copy(alpha = 0.15f) else accentColor
    val iconTint = if (isSoftTranslucent) accentColor else Color.White

    NemoModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(containerBg, RoundedRectangle(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.settings_components_title_daily_grammar_goal),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // List
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                goals.forEach { goal ->
                    GoalSelectionItem(
                        text = stringResource(R.string.settings_components_format_grammar_goal, goal),
                        isSelected = (goal == currentGoal),
                        color = accentColor,
                        onClick = { onGoalSelected(goal) }
                    )
                }
            }
        }
    }
}

/**
 * 通用目标选择项 (Tinted Squircle Style)
 */
@Composable
private fun GoalSelectionItem(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent
    val textColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedRectangle(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


/**
 * 学习日重置时间选择 BottomSheet (V2: Premium Squircle Selection)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningDayResetHourBottomSheet(
    currentHour: Int,
    onDismiss: () -> Unit,
    onHourSelected: (Int) -> Unit
) {
    val hours = listOf(0, 2, 4, 5, 6)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = if (isDark) SystemColors.MintDark else SystemColors.Mint
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val containerBg = if (isSoftTranslucent) accentColor.copy(alpha = 0.15f) else accentColor
    val iconTint = if (isSoftTranslucent) accentColor else Color.White

    NemoModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(containerBg, RoundedRectangle(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_components_title_reset_hour),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_components_desc_reset_hour),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // List
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                hours.forEach { hour ->
                    ResetHourSelectionItem(
                        hour = hour,
                        isSelected = (hour == currentHour),
                        color = accentColor,
                        onClick = { onHourSelected(hour) }
                    )
                }
            }
        }
    }
}

/**
 * 重置时间选择项
 */
@Composable
private fun ResetHourSelectionItem(
    hour: Int,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent
    val textColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface

    val displayText = when (hour) {
        0 -> stringResource(R.string.settings_components_reset_hour_midnight)
        2 -> stringResource(R.string.settings_components_reset_hour_2am)
        4 -> stringResource(R.string.settings_components_reset_hour_4am_recommended)
        5 -> stringResource(R.string.settings_components_reset_hour_5am)
        6 -> stringResource(R.string.settings_components_reset_hour_6am)
        else -> "${hour}:00"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedRectangle(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = textColor
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}


/**
 * 语音选择 BottomSheet (V3: 分组显示 + 加载状态 + 震动反馈 + 播放指示)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VoiceSelectionBottomSheet(
    currentVoiceName: String?,
    voices: List<com.jian.nemo.core.domain.model.TtsVoice>,
    onDismiss: () -> Unit,
    onVoiceSelected: (String) -> Unit,
    onPreviewVoice: (String) -> Unit,
    isLoading: Boolean = false,
    previewingVoiceName: String? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = if (isDark) SystemColors.PinkDark else SystemColors.Pink
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val containerBg = if (isSoftTranslucent) accentColor.copy(alpha = 0.15f) else accentColor
    val iconTint = if (isSoftTranslucent) accentColor else Color.White
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val networkVoiceGroup = stringResource(R.string.settings_components_voice_group_network)
    val localVoiceGroup = stringResource(R.string.settings_components_voice_group_local)
    val selectVoiceTitle = stringResource(R.string.settings_components_title_select_voice)
    val emptyVoiceText = stringResource(R.string.settings_components_empty_voices)

    val tagNetwork = stringResource(R.string.settings_components_voice_tag_network)
    val tagLocal = stringResource(R.string.settings_components_voice_tag_local)
    val defaultVoiceName = stringResource(R.string.settings_components_voice_name_default)
    val jaVoiceFormat = stringResource(R.string.settings_components_voice_name_ja_format)
    val voiceFormat = stringResource(R.string.settings_components_voice_name_format)
    val highQualityText = stringResource(R.string.settings_components_voice_quality_high)

    // 按本地/云端分组
    val groupedVoices = remember(voices, networkVoiceGroup, localVoiceGroup) {
        voices.groupBy { voice ->
            if (voice.name.lowercase().contains("network")) networkVoiceGroup else localVoiceGroup
        }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    NemoPageBottomSheet(
        onDismissed = onDismiss
    ) { slideOut ->
        val sheetBackdrop = rememberLayerBackdrop()
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(sheetBackdrop),
                contentPadding = PaddingValues(
                    top = 72.dp,
                    bottom = navBarBottom + 24.dp
                )
            ) {
                when {
                    isLoading -> {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                repeat(3) {
                                    VoiceSkeletonItem()
                                }
                            }
                        }
                    }
                    voices.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emptyVoiceText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        groupedVoices.forEach { (groupName, groupVoices) ->
                            item(key = groupName) {
                                SettingsSectionTitle(
                                    text = "$groupName (${groupVoices.size})",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            itemsIndexed(
                                items = groupVoices,
                                key = { _, voice -> voice.name }
                            ) { index, voice ->
                                val displayInfo = formatVoiceName(
                                    voice = voice,
                                    tagNetwork = tagNetwork,
                                    tagLocal = tagLocal,
                                    defaultVoiceName = defaultVoiceName,
                                    jaVoiceFormat = jaVoiceFormat,
                                    voiceFormat = voiceFormat,
                                    highQualityText = highQualityText
                                )
                                val isPreviewing = previewingVoiceName == voice.name
                                val isSelected = (voice.name == currentVoiceName)
                                val itemColor = VoicePalette.getVoiceColor(voice.name, index, isDark)

                                VoiceSelectionCardItem(
                                    displayInfo = displayInfo,
                                    isSelected = isSelected,
                                    isPreviewing = isPreviewing,
                                    color = itemColor,
                                    onClick = {
                                        haptic.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                                        )
                                        onVoiceSelected(voice.name)
                                        slideOut()
                                    },
                                    onPreviewClick = { onPreviewVoice(voice.name) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }

            NemoSheetTopBar(
                title = selectVoiceTitle,
                onBack = slideOut,
                backdrop = sheetBackdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

/**
 * 分组标题
 */
@Composable
fun VoiceGroupHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 加载骨架屏项
 */
@Composable
fun VoiceSkeletonItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedRectangle(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon placeholder
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    RoundedRectangle(8.dp)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(16.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        RoundedRectangle(4.dp)
                    )
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Subtitle placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.6f),
                        RoundedRectangle(4.dp)
                    )
            )
        }
    }
}

/**
 * 语音显示信息
 */
data class VoiceDisplayInfo(
    val title: String,
    val subtitle: String,
    val tag: String? = null,
    val code: String = ""
)

/**
 * 格式化语音名称
 */
private fun formatVoiceName(
    voice: com.jian.nemo.core.domain.model.TtsVoice,
    tagNetwork: String,
    tagLocal: String,
    defaultVoiceName: String,
    jaVoiceFormat: String,
    voiceFormat: String,
    highQualityText: String
): VoiceDisplayInfo {
    val rawName = voice.name.lowercase()

    // 1. 识别类型 (Network/Local)
    val isNetwork = rawName.contains("network")
    val tag = if (isNetwork) tagNetwork else tagLocal

    // 2. 生成友好名称
    var friendlyName = ""

    // 提取核心代号
    val idMatch = Regex("ja-jp-x-([a-z]+)").find(rawName)
    val coreId = idMatch?.groupValues?.get(1)?.uppercase() ?: rawName.takeLast(4).uppercase()

    friendlyName = when {
        rawName.contains("language") -> defaultVoiceName
        coreId.isNotEmpty() -> String.format(Locale.getDefault(), jaVoiceFormat, coreId)
        else -> String.format(Locale.getDefault(), voiceFormat, coreId)
    }

    return VoiceDisplayInfo(
        title = friendlyName,
        subtitle = if (voice.quality == "high" || voice.quality == "very_high") highQualityText else "",
        tag = tag,
        code = coreId.take(4).ifEmpty { "JA" }
    )
}

/**
 * 语音选择项 (V3: 支持播放脉冲动画)
 */
@Composable
fun VoiceSelectionItem(
    displayInfo: VoiceDisplayInfo,
    isSelected: Boolean,
    isPreviewing: Boolean,
    color: Color,
    onClick: () -> Unit,
    onPreviewClick: () -> Unit
) {
    val backgroundColor = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
    val textColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) color.copy(alpha = 0.5f) else Color.Transparent

    // 播放脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "preview")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedRectangle(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedRectangle(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Voice Icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedRectangle(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayInfo.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = textColor
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (displayInfo.tag != null) {
                    Text(
                        text = displayInfo.tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    if (displayInfo.subtitle.isNotEmpty()) {
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                if (displayInfo.subtitle.isNotEmpty()) {
                    Text(
                        text = displayInfo.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Preview Button with pulse animation when playing
        IconButton(
            onClick = onPreviewClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isPreviewing) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.settings_components_btn_preview_voice),
                tint = if (isPreviewing) color else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        if (isPreviewing) {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    }
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * TTS 发音人多彩调色盘映射
 * 严格遵循 docs/design/color-palette 规范
 */
object VoicePalette {
    private val paletteColors = listOf(
        SystemColors.Blue to SystemColors.BlueDark,
        SystemColors.Purple to SystemColors.PurpleDark,
        SystemColors.Teal to SystemColors.TealDark,
        SystemColors.Indigo to SystemColors.IndigoDark,
        SystemColors.Orange to SystemColors.OrangeDark,
        SystemColors.Pink to SystemColors.PinkDark,
        SystemColors.Mint to SystemColors.MintDark,
        SystemColors.Cyan to SystemColors.CyanDark
    )

    fun getVoiceColor(voiceName: String, index: Int, isDark: Boolean): Color {
        val hash = (voiceName.hashCode() xor (index * 31)).let { if (it < 0) -it else it }
        val pair = paletteColors[hash % paletteColors.size]
        return if (isDark) pair.second else pair.first
    }
}

/**
 * 苹果极简独立浮空微卡片发音人项
 */
@Composable
fun VoiceSelectionCardItem(
    displayInfo: VoiceDisplayInfo,
    isSelected: Boolean,
    isPreviewing: Boolean,
    color: Color,
    onClick: () -> Unit,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "card_preview_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "card_pulse_scale"
    )

    val cardShape = RoundedRectangle(16.dp)
    val cardBackground = if (isSelected) {
        color.copy(alpha = if (isDark) 0.18f else 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardBorderModifier = if (isSelected) {
        Modifier.border(
            width = 1.2.dp,
            color = color.copy(alpha = if (isDark) 0.7f else 0.45f),
            shape = cardShape
        )
    } else {
        if (isDark) {
            Modifier.border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = cardShape
            )
        } else {
            Modifier.border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = cardShape
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 16.dp, isDark = isDark)
            .clip(cardShape)
            .then(cardBorderModifier)
            .background(cardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 声学铭牌
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) color.copy(alpha = 0.22f) else color.copy(alpha = 0.12f),
                    RoundedRectangle(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayInfo.code.ifEmpty { "VO" },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 发音人信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayInfo.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (displayInfo.tag != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isSelected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                RoundedRectangle(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = displayInfo.tag,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (displayInfo.subtitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                if (displayInfo.subtitle.isNotEmpty()) {
                    Text(
                        text = displayInfo.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // 试听播放按钮
        IconButton(
            onClick = onPreviewClick,
            modifier = Modifier.size(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isPreviewing) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPreviewing) Icons.AutoMirrored.Rounded.VolumeUp else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.settings_components_btn_preview_voice),
                    tint = if (isPreviewing) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            if (isPreviewing) {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                        }
                )
            }
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
