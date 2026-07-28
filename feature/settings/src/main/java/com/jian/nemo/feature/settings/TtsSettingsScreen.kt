package com.jian.nemo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import com.jian.nemo.feature.settings.components.SquircleSettingItem
import com.jian.nemo.feature.settings.components.ChineseVoiceSelectionBottomSheet
import com.jian.nemo.feature.settings.components.VoiceSelectionBottomSheet
import java.util.Locale

/**
 * TTS 语音设置界面 (Flat UI 风格)
 *
 * 采用项目统一的 PremiumCard + Squircle 布局
 * 使用 Nemo iOS 风格配色，避免 MD3 默认色板
 */
@Composable
fun TtsSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Flat UI 配色定义
    val accentColor = IosColors.Purple  // 主强调色
    val secondaryAccent = IosColors.Cyan // 次要强调色
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CommonHeader(
                title = "语音设置",
                onBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // =====================
            // 语音来源
            // =====================
            SettingsSectionTitle("语音来源")

            PremiumCard {
                Column {
                    SquircleSettingItem(
                        title = "选择日语语音",
                        subtitle = uiState.ttsVoiceName ?: "系统默认",
                        icon = Icons.Rounded.RecordVoiceOver,
                        iconColor = IosColors.Red,
                        onClick = { viewModel.onEvent(SettingsEvent.ShowVoiceSelectionDialog(true)) },
                        showDivider = true
                    )
                    SquircleSettingItem(
                        title = "选择中文语音",
                        subtitle = uiState.ttsChineseVoiceName ?: "系统默认",
                        icon = Icons.Rounded.RecordVoiceOver,
                        iconColor = IosColors.Orange,
                        onClick = { viewModel.onEvent(SettingsEvent.ShowChineseVoiceSelectionDialog(true)) },
                        showDivider = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =====================
            // 语速调节
            // =====================
            SettingsSectionTitle("语速调节")

            PremiumCard {
                FlatSliderSettingItem(
                    icon = Icons.Rounded.Speed,
                    iconColor = accentColor,
                    title = "语速 (Speed)",
                    value = uiState.ttsSpeechRate,
                    valueDisplay = "${String.format(Locale.US, "%.1f", uiState.ttsSpeechRate)}x",
                    onValueChange = { viewModel.onEvent(SettingsEvent.SetTtsSpeechRate(it)) },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    accentColor = accentColor,
                    isDark = isDark,
                    labels = listOf("0.5x", "1.0x", "2.0x")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =====================
            // 音调调节
            // =====================
            SettingsSectionTitle("音调调节")

            PremiumCard {
                FlatSliderSettingItem(
                    icon = Icons.Rounded.MusicNote,
                    iconColor = secondaryAccent,
                    title = "音调 (Pitch)",
                    value = uiState.ttsPitch,
                    valueDisplay = "${String.format(Locale.US, "%.1f", uiState.ttsPitch)}x",
                    onValueChange = { viewModel.onEvent(SettingsEvent.SetTtsPitch(it)) },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    accentColor = secondaryAccent,
                    isDark = isDark,
                    labels = listOf("0.5x", "1.0x", "2.0x")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // =====================
            // 试听 & 重置
            // =====================
            SettingsSectionTitle("试听 & 重置")

            PremiumCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            // 试听按钮 (日语)
                            FlatPrimaryButton(
                                text = "试听 (日语)",
                                icon = Icons.Rounded.PlayArrow,
                                color = accentColor,
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.PreviewTts("これは音声テストです。"))
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            // 试听按钮 (中文)
                            FlatPrimaryButton(
                                text = "试听 (中文)",
                                icon = Icons.Rounded.PlayArrow,
                                color = IosColors.Orange,
                                onClick = {
                                    viewModel.onEvent(SettingsEvent.PreviewChineseVoice(uiState.ttsChineseVoiceName ?: "", "这是中文语音测试。"))
                                }
                            )
                        }
                    }

                    // 重置按钮 - 文字按钮
                    FlatTextButton(
                        text = "重置为默认值",
                        color = NemoNeutrals.Gray500,
                        onClick = {
                            viewModel.onEvent(SettingsEvent.SetTtsSpeechRate(1.0f))
                            viewModel.onEvent(SettingsEvent.SetTtsPitch(1.0f))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // 日语语音选择 BottomSheet
        if (uiState.showVoiceSelectionDialog) {
            VoiceSelectionBottomSheet(
                currentVoiceName = uiState.ttsVoiceName,
                voices = uiState.availableVoices,
                onDismiss = { viewModel.onEvent(SettingsEvent.ShowVoiceSelectionDialog(false)) },
                onVoiceSelected = {
                    viewModel.onEvent(SettingsEvent.SetTtsVoiceName(it))
                    viewModel.onEvent(SettingsEvent.PreviewTts("これは音声テストです。"))
                },
                onPreviewVoice = { voiceName ->
                    viewModel.onEvent(SettingsEvent.PreviewVoice(voiceName, "これは音声テストです。"))
                },
                isLoading = uiState.isLoadingVoices,
                previewingVoiceName = uiState.previewingVoiceName
            )
        }

        // 中文语音选择 BottomSheet
        if (uiState.showChineseVoiceSelectionDialog) {
            ChineseVoiceSelectionBottomSheet(
                currentVoiceName = uiState.ttsChineseVoiceName,
                voices = uiState.availableChineseVoices,
                onDismiss = { viewModel.onEvent(SettingsEvent.ShowChineseVoiceSelectionDialog(false)) },
                onVoiceSelected = { voiceName ->
                    viewModel.onEvent(SettingsEvent.SetTtsChineseVoiceName(voiceName))
                    viewModel.onEvent(SettingsEvent.PreviewChineseVoice(voiceName, "这是中文语音测试。"))
                },
                onPreviewVoice = { voiceName ->
                    viewModel.onEvent(SettingsEvent.PreviewChineseVoice(voiceName, "这是中文语音测试。"))
                },
                isLoading = uiState.isLoadingVoices,
                previewingVoiceName = uiState.previewingChineseVoiceName
            )
        }
    }
}

// =============================================================================
// Flat UI 组件
// =============================================================================

/**
 * Flat UI 风格 Slider 设置项
 */
@Composable
private fun FlatSliderSettingItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: Float,
    valueDisplay: String,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    accentColor: Color,
    isDark: Boolean,
    labels: List<String>
) {
    val haptic = LocalHapticFeedback.current
    var lastStepValue by remember { mutableFloatStateOf(value) }

    Column(modifier = Modifier.padding(16.dp)) {
        // Header Row: Icon + Title + Value
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
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

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Value Badge
            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = valueDisplay,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Flat UI Slider with Haptic Feedback
        Slider(
            value = value,
            onValueChange = { newValue ->
                // 计算步进值间隔
                val stepSize = (valueRange.endInclusive - valueRange.start) / (steps + 1)
                val currentStep = ((newValue - valueRange.start) / stepSize).toInt()
                val lastStep = ((lastStepValue - valueRange.start) / stepSize).toInt()

                // 当跨越步进点时触发轻微震动
                if (currentStep != lastStep) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastStepValue = newValue
                }

                onValueChange(newValue)
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = if (isDark) NemoNeutrals.Gray700 else NemoNeutrals.Gray200,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )

        // Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Flat UI 主按钮
 */
@Composable
private fun FlatPrimaryButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White
        )
    }
}

/**
 * Flat UI 文字按钮
 */
@Composable
private fun FlatTextButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    )
}
