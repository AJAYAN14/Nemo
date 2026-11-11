package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.sheet.NemoModalBottomSheet
import com.jian.nemo.feature.settings.R

/**
 * 独立的中文 TTS 语音选择抽屉弹窗 (Flat UI 风格)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val isDark = isSystemInDarkTheme()
    val accentColor = if (isDark) SystemColors.OrangeDark else SystemColors.Orange
    val haptic = LocalHapticFeedback.current
    val cloudGroupName = stringResource(R.string.settings_tts_group_cloud_chinese)
    val localGroupName = stringResource(R.string.settings_tts_group_local_chinese)

    // 按本地/云端分组
    val groupedVoices = remember(voices, cloudGroupName, localGroupName) {
        voices.groupBy { voice ->
            if (voice.name.lowercase().contains("network")) cloudGroupName else localGroupName
        }
    }

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
                val iconStyle = LocalSettingIconStyle.current
                val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
                val containerBgColor = if (isSoftTranslucent) accentColor.copy(alpha = 0.15f) else accentColor
                val iconTint = if (isSoftTranslucent) accentColor else Color.White

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(containerBgColor, RoundedRectangle(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Translate,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.settings_tts_dialog_title_select_chinese),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_tts_dialog_desc_select_chinese),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Content
            when {
                isLoading -> {
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
                            text = stringResource(R.string.settings_tts_empty_chinese_engine),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        groupedVoices.forEach { (groupName, groupVoices) ->
                            stickyHeader {
                                VoiceGroupHeader(title = groupName, count = groupVoices.size)
                            }

                            items(groupVoices.size) { index ->
                                val voice = groupVoices[index]
                                val displayInfo = formatChineseVoiceName(voice)
                                val isPreviewing = previewingVoiceName == voice.name

                                VoiceSelectionItem(
                                    displayInfo = displayInfo,
                                    isSelected = (voice.name == currentVoiceName),
                                    isPreviewing = isPreviewing,
                                    color = accentColor,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        tag = tag
    )
}
