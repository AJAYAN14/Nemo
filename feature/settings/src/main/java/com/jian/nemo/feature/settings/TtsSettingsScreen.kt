package com.jian.nemo.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jian.nemo.feature.settings.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import com.jian.nemo.feature.settings.components.SquircleSettingItem
import com.jian.nemo.feature.settings.components.ChineseVoiceSelectionBottomSheet
import com.jian.nemo.feature.settings.components.VoiceSelectionBottomSheet
import com.jian.nemo.core.ui.component.liquid.LiquidSlider
import com.jian.nemo.core.ui.component.liquid.LiquidGlassButton
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.feature.settings.components.LocalSettingIconStyle
import java.util.Locale

import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

/**
 * TTS 语音设置界面 (Flat UI 风格)
 *
 * 采用项目统一的 PremiumCard + Squircle 布局
 * 采用工程标准语义调色盘，避免 MD3 默认生成色
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

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val backgroundColor = MaterialTheme.colorScheme.background
    val sampleChineseText = stringResource(R.string.settings_tts_sample_chinese_text)
    val sampleJapaneseText = stringResource(R.string.settings_tts_sample_japanese_text)

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 1. 底层滚动内容（被 backdrop 采样源录制）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .verticalScroll(scrollState)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = statusBarTop + 60.dp + 16.dp,
                        bottom = navBarBottom + 24.dp
                    )
            ) {
                // =====================
                // 语音来源
                // =====================
                SettingsSectionTitle(
                    text = stringResource(R.string.settings_tts_section_source),
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                )

                PremiumCard {
                    Column {
                        SquircleSettingItem(
                            title = stringResource(R.string.settings_tts_item_title_select_japanese),
                            subtitle = uiState.ttsVoiceName ?: stringResource(R.string.settings_tts_value_system_default),
                            icon = Icons.Rounded.RecordVoiceOver,
                            iconColor = IosColors.Red,
                            onClick = { viewModel.onEvent(SettingsEvent.ShowVoiceSelectionDialog(true)) },
                            showDivider = true
                        )
                        SquircleSettingItem(
                            title = stringResource(R.string.settings_tts_item_title_select_chinese),
                            subtitle = uiState.ttsChineseVoiceName ?: stringResource(R.string.settings_tts_value_system_default),
                            icon = Icons.Rounded.Translate,
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
                SettingsSectionTitle(
                    text = stringResource(R.string.settings_tts_section_speed),
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                )

                PremiumCard {
                    FlatSliderSettingItem(
                        icon = Icons.Rounded.Speed,
                        iconColor = accentColor,
                        title = stringResource(R.string.settings_tts_item_title_speed),
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
                SettingsSectionTitle(
                    text = stringResource(R.string.settings_tts_section_pitch),
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                )

                PremiumCard {
                    FlatSliderSettingItem(
                        icon = Icons.Rounded.MusicNote,
                        iconColor = secondaryAccent,
                        title = stringResource(R.string.settings_tts_item_title_pitch),
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
            SettingsSectionTitle(
                text = stringResource(R.string.settings_tts_section_preview_reset),
                modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
            )

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
                        val isPlayingJp = uiState.previewAudioType == PreviewAudioType.JAPANESE
                        val isPlayingCn = uiState.previewAudioType == PreviewAudioType.CHINESE

                        Box(modifier = Modifier.weight(1f)) {
                            // 试听按钮 (日语)
                            LiquidPreviewButton(
                                text = stringResource(R.string.settings_tts_btn_preview_japanese),
                                playingText = stringResource(R.string.settings_tts_btn_stop_japanese),
                                isPlaying = isPlayingJp,
                                color = accentColor,
                                onClick = {
                                    if (isPlayingJp) {
                                        viewModel.onEvent(SettingsEvent.StopPreviewTts)
                                    } else {
                                        viewModel.onEvent(SettingsEvent.PreviewTts(sampleJapaneseText))
                                    }
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            // 试听按钮 (中文)
                            LiquidPreviewButton(
                                text = stringResource(R.string.settings_tts_btn_preview_chinese),
                                playingText = stringResource(R.string.settings_tts_btn_stop_chinese),
                                isPlaying = isPlayingCn,
                                color = IosColors.Orange,
                                onClick = {
                                    if (isPlayingCn) {
                                        viewModel.onEvent(SettingsEvent.StopPreviewTts)
                                    } else {
                                        viewModel.onEvent(SettingsEvent.PreviewChineseVoice(uiState.ttsChineseVoiceName ?: "", sampleChineseText))
                                    }
                                }
                            )
                        }
                    }

                    // 重置按钮 - 文字按钮
                    FlatTextButton(
                        text = stringResource(R.string.settings_tts_btn_reset_default),
                        color = NemoNeutrals.Gray500,
                        onClick = {
                            viewModel.onEvent(SettingsEvent.StopPreviewTts)
                            viewModel.onEvent(SettingsEvent.SetTtsSpeechRate(1.0f))
                            viewModel.onEvent(SettingsEvent.SetTtsPitch(1.0f))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.95f),
                            backgroundColor.copy(alpha = 0.75f),
                            backgroundColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = stringResource(R.string.settings_tts_title_screen),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp
            )
        }

        // 日语语音选择 BottomSheet
        if (uiState.showVoiceSelectionDialog) {
            VoiceSelectionBottomSheet(
                currentVoiceName = uiState.ttsVoiceName,
                voices = uiState.availableVoices,
                onDismiss = { viewModel.onEvent(SettingsEvent.ShowVoiceSelectionDialog(false)) },
                onVoiceSelected = {
                    viewModel.onEvent(SettingsEvent.SetTtsVoiceName(it))
                    viewModel.onEvent(SettingsEvent.PreviewTts(sampleJapaneseText))
                },
                onPreviewVoice = { voiceName ->
                    viewModel.onEvent(SettingsEvent.PreviewVoice(voiceName, sampleJapaneseText))
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
                    viewModel.onEvent(SettingsEvent.PreviewChineseVoice(voiceName, sampleChineseText))
                },
                onPreviewVoice = { voiceName ->
                    viewModel.onEvent(SettingsEvent.PreviewChineseVoice(voiceName, sampleChineseText))
                },
                isLoading = uiState.isLoadingVoices,
                previewingVoiceName = uiState.previewingChineseVoiceName
            )
        }
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
    iconStyle: SettingIconStyle = LocalSettingIconStyle.current,
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
    Column(modifier = Modifier.padding(16.dp)) {
        // Header Row: Icon + Title + Value
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    modifier = Modifier.size(28.dp)
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
                    .background(accentColor.copy(alpha = 0.12f), RoundedRectangle(8.dp))
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

        // 液态玻璃滑动条 (LiquidSlider) 具备阻尼拖拽与光影折射（无震动干扰）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            LiquidSlider(
                value = { value },
                onValueChange = onValueChange,
                valueRange = valueRange,
                visibilityThreshold = 0.01f,
                accentColor = accentColor
            )
        }

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
 * 液态玻璃试听按钮 (方案 A：播放/停止状态切换)
 */
@Composable
private fun LiquidPreviewButton(
    text: String,
    playingText: String,
    isPlaying: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 播放中采用高亮发光对比态，未播放时为通透温润的液态玻璃彩色微透底色
    val buttonSurfaceColor = if (isPlaying) {
        color.copy(alpha = if (isDark) 0.9f else 0.85f)
    } else {
        color.copy(alpha = if (isDark) 0.25f else 0.16f)
    }

    val contentColor = if (isPlaying) {
        Color.White
    } else {
        if (isDark) color.copy(alpha = 0.95f) else color
    }

    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        surfaceColor = buttonSurfaceColor,
        shape = RoundedRectangle(16.dp),
        border = null,
        hasLens = true,
        isInteractive = true,
        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) stringResource(R.string.settings_tts_cd_stop) else stringResource(R.string.settings_tts_cd_play),
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isPlaying) playingText else text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1
            )
        }
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
            .clip(RoundedRectangle(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    )
}
