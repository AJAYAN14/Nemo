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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
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
import com.jian.nemo.core.designsystem.theme.*
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
    val borderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f) else NemoNeutrals.Gray100

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 24.dp, isDark = isDark),
        content = { Column(content = content) }
    )
}

/**
 * Setting Item with Squircle Icon (V2)
 * 支持 trailing lambda 用于放置 Switch 或其他自定义控件
 */
@Composable
fun SquircleSettingItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
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

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // 自定义按压效果，去除默认 ripple
                    onClick = onClick
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Icon
            Box(
                modifier = Modifier
                    .size(42.dp) // Standard standard size
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
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
fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
    val accentColor = Color(0xFFFF9500) // NemoOrange

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface, // Use surface for clean look
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
                // Tinted Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Flag,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "每日单词目标",
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
                        text = "$goal 个单词",
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
    val accentColor = Color(0xFF34C759) // NemoGreen

    ModalBottomSheet(
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
                 // Tinted Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Subject, // Safer icon than JoinLeft
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "每日语法目标",
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
                        text = "$goal 条语法",
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
            .clip(RoundedCornerShape(12.dp))
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
    val accentColor = Color(0xFF5856D6) // NemoIndigo

    ModalBottomSheet(
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
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "学习日重置时间",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "每天此时间后开始新的学习日",
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
        0 -> "凌晨 0:00 (午夜)"
        2 -> "凌晨 2:00"
        4 -> "凌晨 4:00 (推荐)"
        5 -> "凌晨 5:00"
        6 -> "凌晨 6:00"
        else -> "${hour}:00"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
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
    val accentColor = Color(0xFFFF2D55) // NemoRed
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // 按本地/云端分组
    val groupedVoices = remember(voices) {
        voices.groupBy { voice ->
            if (voice.name.lowercase().contains("network")) "云端语音" else "本地语音"
        }
    }

    ModalBottomSheet(
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
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "选择语音",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Content
            when {
                isLoading -> {
                    // 加载骨架屏
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        repeat(3) {
                            VoiceSkeletonItem()
                        }
                    }
                }
                voices.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未找到可用语音",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    // 分组列表
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        groupedVoices.forEach { (groupName, groupVoices) ->
                            // Sticky Header
                            stickyHeader {
                                VoiceGroupHeader(title = groupName, count = groupVoices.size)
                            }

                            items(groupVoices.size) { index ->
                                val voice = groupVoices[index]
                                val displayInfo = formatVoiceName(voice)
                                val isPreviewing = previewingVoiceName == voice.name

                                VoiceSelectionItem(
                                    displayInfo = displayInfo,
                                    isSelected = (voice.name == currentVoiceName),
                                    isPreviewing = isPreviewing,
                                    color = accentColor,
                                    onClick = {
                                        // 震动反馈
                                        haptic.performHapticFeedback(
                                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                                        )
                                        onVoiceSelected(voice.name)
                                    },
                                    onPreviewClick = { onPreviewVoice(voice.name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 分组标题
 */
@Composable
private fun VoiceGroupHeader(
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
private fun VoiceSkeletonItem() {
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
            .clip(RoundedCornerShape(12.dp))
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
                    RoundedCornerShape(8.dp)
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
                        RoundedCornerShape(4.dp)
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
                        RoundedCornerShape(4.dp)
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
    val tag: String? = null
)

/**
 * 格式化语音名称
 */
private fun formatVoiceName(voice: com.jian.nemo.core.domain.model.TtsVoice): VoiceDisplayInfo {
    val rawName = voice.name.lowercase()

    // 1. 识别类型 (Network/Local)
    val isNetwork = rawName.contains("network")
    val tag = if (isNetwork) "云端" else "本地"

    // 2. 生成友好名称
    var friendlyName = ""

    // 提取核心代号
    val idMatch = Regex("ja-jp-x-([a-z]+)").find(rawName)
    val coreId = idMatch?.groupValues?.get(1)?.uppercase() ?: rawName.takeLast(4).uppercase()

    friendlyName = when {
        rawName.contains("language") -> "系统默认"
        coreId.isNotEmpty() -> "日语语音 $coreId"
        else -> "语音 ${coreId}"
    }

    return VoiceDisplayInfo(
        title = friendlyName,
        subtitle = if (voice.quality == "high" || voice.quality == "very_high") "高质量" else "",
        tag = tag
    )
}

/**
 * 语音选择项 (V3: 支持播放脉冲动画)
 */
@Composable
private fun VoiceSelectionItem(
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
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
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
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.RecordVoiceOver,
                contentDescription = null,
                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
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
                imageVector = if (isPreviewing) Icons.Rounded.VolumeUp else Icons.Rounded.PlayArrow,
                contentDescription = "试听",
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
