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
import androidx.compose.ui.graphics.Color
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
 * 内容报告确认对话框 - 采用纯净中性灰质感设计，完美兼容深色与浅色模式（告别 MD3 彩色色相偏色）
 */
@Composable
fun ContentReportDialog(
    learningMode: LearningMode,
    onDismiss: () -> Unit,
    onConfirm: (errorType: String, description: String?) -> Unit,
    useDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val isDark = useDarkTheme
    val haptic = LocalHapticFeedback.current

    // 纯净中性灰色设计系统（双模自适应，去除 MD3 主色调侵入）
    val chipSelectedBg = if (isDark) Color(0xFF3F3F46) else Color(0xFF18181B)
    val chipSelectedBorder = if (isDark) Color(0xFF71717A) else Color(0xFF18181B)
    val chipSelectedText = Color.White
    val chipUnselectedBg = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)
    val chipUnselectedBorder = if (isDark) Color(0xFF3F3F46) else Color(0xFFE4E4E7)
    val chipUnselectedText = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)

    val inputBg = if (isDark) Color(0xFF27272A) else Color(0xFFF4F4F5)
    val inputBorderFocused = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val inputBorderUnfocused = if (isDark) Color(0xFF3F3F46) else Color(0xFFE4E4E7)
    val inputCursor = if (isDark) Color.White else Color(0xFF18181B)
    val inputTextColor = if (isDark) Color(0xFFF4F4F5) else Color(0xFF18181B)
    val hintTextColor = if (isDark) Color(0xFFA1A1AA).copy(alpha = 0.6f) else Color(0xFF71717A).copy(alpha = 0.7f)
    val promptTextColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)

    val confirmButtonColor = if (isDark) Color(0xFF3F3F46) else Color(0xFF18181B)

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
        confirmButtonColor = confirmButtonColor,
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
                    color = promptTextColor
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
                                        .background(if (isSelected) chipSelectedBg else chipUnselectedBg)
                                        .border(
                                            BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) chipSelectedBorder else chipUnselectedBorder
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
                                        color = if (isSelected) chipSelectedText else chipUnselectedText,
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
                            style = MaterialTheme.typography.bodySmall.copy(color = hintTextColor)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = inputTextColor),
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
                                    promptTextColor.copy(alpha = 0.6f)
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = inputTextColor,
                        unfocusedTextColor = inputTextColor,
                        focusedBorderColor = inputBorderFocused,
                        unfocusedBorderColor = inputBorderUnfocused,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        cursorColor = inputCursor
                    )
                )
            }
        }
    )
}

