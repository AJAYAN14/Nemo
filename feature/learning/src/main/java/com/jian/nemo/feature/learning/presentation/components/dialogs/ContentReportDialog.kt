package com.jian.nemo.feature.learning.presentation.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.feature.learning.presentation.LearningMode

/**
 * 内容报告确认对话框 - 完全基于项目统一的 NemoDialog 组件实现
 */
@Composable
fun ContentReportDialog(
    learningMode: LearningMode,
    onDismiss: () -> Unit,
    onConfirm: (errorType: String, description: String?) -> Unit,
    useDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val isDark = useDarkTheme
    val primaryColor = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current

    // 错误选项配置
    val errorOptions = remember(learningMode) {
        if (learningMode == LearningMode.Word) {
            listOf(
                "meaning_error" to "释义错误",
                "furigana_error" to "注音错误",
                "example_error" to "例句错误",
                "spelling_error" to "拼写错误",
                "pos_error" to "词性错误",
                "other" to "其他问题"
            )
        } else {
            listOf(
                "meaning_error" to "解释错误",
                "connection_error" to "接续错误",
                "furigana_error" to "注音错误",
                "example_error" to "例句错误",
                "level_error" to "级别错误",
                "other" to "其他问题"
            )
        }
    }

    var selectedType by remember { mutableStateOf(errorOptions.first().first) }
    var otherDescription by remember { mutableStateOf("") }

    val isDescriptionRequired = selectedType == "other"
    val isSubmitEnabled = !isDescriptionRequired || otherDescription.trim().isNotEmpty()
    val itemName = if (learningMode == LearningMode.Word) "单词" else "语法"

    NemoDialog(
        onDismissRequest = onDismiss,
        title = "报告内容错误",
        confirmText = "确认反馈",
        dismissText = "我再想想",
        confirmEnabled = isSubmitEnabled,
        onConfirm = {
            val desc = otherDescription.trim().takeIf { it.isNotEmpty() }
            onConfirm(selectedType, desc)
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "请选择当前${itemName}内容的错误类型：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Choice Grid (2 Columns)
                val rows = errorOptions.chunked(2)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { (code, label) ->
                                val isSelected = selectedType == code
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) primaryColor.copy(alpha = if (isDark) 0.16f else 0.08f)
                                            else MaterialTheme.colorScheme.surfaceContainer
                                        )
                                        .border(
                                            BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) primaryColor
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedType = code
                                        }
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Description Input Field
                OutlinedTextField(
                    value = otherDescription,
                    onValueChange = { if (it.length <= 100) otherDescription = it },
                    placeholder = {
                        Text(
                            text = if (isDescriptionRequired) "请详细描述错误内容（必填，100字以内）" else "补充具体错误细节（选填，100字以内）",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    maxLines = 3,
                    supportingText = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = "${otherDescription.length}/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDescriptionRequired && otherDescription.trim().isEmpty()) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        cursorColor = primaryColor
                    )
                )
            }
        }
    )
}

