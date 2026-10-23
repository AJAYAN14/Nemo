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
import com.jian.nemo.core.ui.component.liquid.LiquidSlider
import com.jian.nemo.core.ui.component.liquid.LiquidGlassButton
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.feature.settings.components.LocalSettingIconStyle
import com.jian.nemo.feature.settings.components.PlumpIcon
import com.jian.nemo.feature.settings.components.PlumpDuoIcon
import com.jian.nemo.feature.settings.components.IsoluminantPalette
import java.util.Locale

import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

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

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val backgroundColor = MaterialTheme.colorScheme.background

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
                SettingsSectionTitle("语音来源")

                PremiumCard {
                    Column {
                        SquircleSettingItem(
                            title = "选择日语语音",
                            subtitle = uiState.ttsVoiceName ?: "系统默认",
                            icon = Icons.Rounded.RecordVoiceOver,
                            iconColor = IosColors.Red,
                            plumpIcon = PlumpIcon.Mic,
                            plumpColor = IsoluminantPalette.RoseRuby,
                            onClick = { viewModel.onEvent(SettingsEvent.ShowVoiceSelectionDialog(true)) },
                            showDivider = true
                        )
                        SquircleSettingItem(
                            title = "选择中文语音",
                            subtitle = uiState.ttsChineseVoiceName ?: "系统默认",
                            icon = Icons.Rounded.RecordVoiceOver,
                            iconColor = IosColors.Orange,
                            plumpIcon = PlumpIcon.Languages,
                            plumpColor = IsoluminantPalette.WarmAmber,
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
                        plumpIcon = PlumpIcon.Gauge,
                        plumpColor = IsoluminantPalette.LilacViolet,
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
                        plumpIcon = PlumpIcon.MusicNote,
                        plumpColor = IsoluminantPalette.LakesideCyan,
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
                        val isPlayingJp = uiState.previewAudioType == PreviewAudioType.JAPANESE
                        val isPlayingCn = uiState.previewAudioType == PreviewAudioType.CHINESE

                        Box(modifier = Modifier.weight(1f)) {
                            // 试听按钮 (日语)
                            LiquidPreviewButton(
                                text = "试听 (日语)",
                                playingText = "停止 (日语)",
                                isPlaying = isPlayingJp,
                                color = accentColor,
                                onClick = {
                                    if (isPlayingJp) {
                                        viewModel.onEvent(SettingsEvent.StopPreviewTts)
                                    } else {
                                        viewModel.onEvent(SettingsEvent.PreviewTts("这是日语语音测试。".let { "これは音声テストです。" }))
                                    }
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            // 试听按钮 (中文)
                            LiquidPreviewButton(
                                text = "试听 (中文)",
                                playingText = "停止 (中文)",
                                isPlaying = isPlayingCn,
                                color = IosColors.Orange,
                                onClick = {
                                    if (isPlayingCn) {
                                        viewModel.onEvent(SettingsEvent.StopPreviewTts)
                                    } else {
                                        viewModel.onEvent(SettingsEvent.PreviewChineseVoice(uiState.ttsChineseVoiceName ?: "", "这是中文语音测试。"))
                                    }
                                }
                            )
                        }
                    }

                    // 重置按钮 - 文字按钮
                    FlatTextButton(
                        text = "重置为默认值",
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
                title = "语音设置",
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
}

// =============================================================================
// Flat UI 组件
// =============================================================================

/**
 * Flat UI 风格 Slider 设置项
 */
@Composable
private fun FlatSliderSettingItem(
    icon: ImageVector? = null,
    plumpIcon: PlumpIcon? = null,
    iconColor: Color,
    plumpColor: Color = iconColor,
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
            // 图标渲染：根据当前风格渲染 MD3（带 42dp 圆角背景卡片与 22dp 单色图标）或 Plump Duo（通透无框双色线面）
            if (iconStyle == SettingIconStyle.MD3) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (plumpIcon != null) {
                        PlumpDuoIcon(
                            icon = plumpIcon,
                            color = plumpColor,
                            size = 28.dp
                        )
                    }
                }
            } else {
                // Plump Duo 模式：无背景框通透呈现
                Box(
                    modifier = Modifier.size(42.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (plumpIcon != null) {
                        PlumpDuoIcon(
                            icon = plumpIcon,
                            color = plumpColor,
                            size = 30.dp
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
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
        shape = RoundedCornerShape(16.dp),
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
                contentDescription = if (isPlaying) "停止" else "试听",
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    )
}
