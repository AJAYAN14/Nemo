package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.ui.component.sheet.NemoPageBottomSheet
import com.jian.nemo.core.ui.component.sheet.NemoSheetTopBar
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.jian.nemo.feature.settings.R

/**
 * 中文 TTS 语音选择抽屉弹窗
 */
@Composable
fun ChineseVoiceSelectionBottomSheet(
    currentVoiceName: String?,
    voices: List<com.jian.nemo.core.domain.model.TtsVoice>,
    onDismiss: () -> Unit,
    onVoiceSelected: (String) -> Unit,
    onPreviewVoice: (String) -> Unit,
    isLoading: Boolean = false,
    previewingVoiceName: String? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accentColor = if (isDark) SystemColors.OrangeDark else SystemColors.Orange
    val haptic = LocalHapticFeedback.current
    val cloudGroupName = stringResource(R.string.settings_tts_group_cloud_chinese)
    val localGroupName = stringResource(R.string.settings_tts_group_local_chinese)
    val titleText = stringResource(R.string.settings_tts_dialog_title_select_chinese)
    val emptyText = stringResource(R.string.settings_tts_empty_chinese_engine)

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 按本地/云端分组
    val groupedVoices = remember(voices, cloudGroupName, localGroupName) {
        voices.groupBy { voice ->
            if (voice.name.lowercase().contains("network")) cloudGroupName else localGroupName
        }
    }

    NemoPageBottomSheet(
        onDismissed = onDismiss
    ) { slideOut ->
        val sheetBackdrop = rememberLayerBackdrop()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
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
                                    text = emptyText,
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
                                val displayInfo = formatChineseVoiceName(voice)
                                val isPreviewing = previewingVoiceName == voice.name
                                val isSelected = (voice.name == currentVoiceName)
                                val itemColor = VoicePalette.getVoiceColor(voice.name, index, isDark)

                                VoiceSelectionCardItem(
                                    displayInfo = displayInfo,
                                    isSelected = isSelected,
                                    isPreviewing = isPreviewing,
                                    color = itemColor,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                title = titleText,
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
 * 格式化中文语音名称
 */
@Composable
private fun formatChineseVoiceName(voice: com.jian.nemo.core.domain.model.TtsVoice): VoiceDisplayInfo {
    val rawName = voice.name.lowercase()

    val isNetwork = rawName.contains("network")
    val tag = if (isNetwork) stringResource(R.string.settings_tts_tag_cloud) else stringResource(R.string.settings_tts_tag_local)

    val idMatch = Regex("zh-[a-z]+-x-([a-z]+)").find(rawName)
    val coreId = idMatch?.groupValues?.get(1)?.uppercase() ?: rawName.takeLast(4).uppercase()

    val genderInfo = when {
        voice.gender == "female" || rawName.contains("female") -> stringResource(R.string.settings_tts_voice_female_suffix)
        voice.gender == "male" || rawName.contains("male") -> stringResource(R.string.settings_tts_voice_male_suffix)
        else -> ""
    }

    val friendlyName = when {
        rawName.contains("language") -> stringResource(R.string.settings_tts_voice_system_default_chinese)
        else -> stringResource(R.string.settings_tts_format_chinese_voice_name, coreId, genderInfo)
    }

    return VoiceDisplayInfo(
        title = friendlyName,
        subtitle = if (voice.quality == "high" || voice.quality == "very_high") stringResource(R.string.settings_tts_quality_high) else "",
        tag = tag,
        code = coreId.take(4).ifEmpty { "ZH" }
    )
}
