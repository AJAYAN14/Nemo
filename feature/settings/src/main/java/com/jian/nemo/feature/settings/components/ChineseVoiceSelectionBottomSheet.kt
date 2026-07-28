package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.IosColors

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
    val accentColor = IosColors.Orange // 中文语音采用优雅的橙色区分于日语红
    val haptic = LocalHapticFeedback.current

    // 按本地/云端分组
    val groupedVoices = remember(voices) {
        voices.groupBy { voice ->
            if (voice.name.lowercase().contains("network")) "云端中文语音" else "本地中文语音"
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
                Column {
                    Text(
                        text = "选择中文语音",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "用于学习例句中的中文翻译朗读",
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
                            text = "未找到可用中文语音引擎",
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
private fun formatChineseVoiceName(voice: com.jian.nemo.core.domain.model.TtsVoice): VoiceDisplayInfo {
    val rawName = voice.name.lowercase()

    val isNetwork = rawName.contains("network")
    val tag = if (isNetwork) "云端" else "本地"

    val idMatch = Regex("zh-[a-z]+-x-([a-z]+)").find(rawName)
    val coreId = idMatch?.groupValues?.get(1)?.uppercase() ?: rawName.takeLast(4).uppercase()

    val genderInfo = when {
        voice.gender == "female" || rawName.contains("female") -> " (女声)"
        voice.gender == "male" || rawName.contains("male") -> " (男声)"
        else -> ""
    }

    val friendlyName = when {
        rawName.contains("language") -> "系统默认中文"
        coreId.isNotEmpty() -> "中文语音 $coreId$genderInfo"
        else -> "中文语音 ${coreId}$genderInfo"
    }

    return VoiceDisplayInfo(
        title = friendlyName,
        subtitle = if (voice.quality == "high" || voice.quality == "very_high") "高质量" else "",
        tag = tag
    )
}
