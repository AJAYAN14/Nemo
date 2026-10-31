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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.LearningPalette
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.feature.learning.R
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
    val chipSelectedBg = if (isDark) LearningPalette.ReportZinc700 else LearningPalette.ReportZinc900
    val chipSelectedBorder = if (isDark) LearningPalette.ReportZinc500 else LearningPalette.ReportZinc900
    val chipSelectedText = Color.White
    val chipUnselectedBg = if (isDark) LearningPalette.ReportZinc800 else LearningPalette.ReportZinc100
    val chipUnselectedBorder = if (isDark) LearningPalette.ReportZinc700 else LearningPalette.ReportZinc200
    val chipUnselectedText = if (isDark) LearningPalette.ReportZinc400 else LearningPalette.ReportZinc600

    val inputBg = if (isDark) LearningPalette.ReportZinc800 else LearningPalette.ReportZinc100
    val inputBorderFocused = if (isDark) LearningPalette.ReportZinc400 else LearningPalette.ReportZinc600
    val inputBorderUnfocused = if (isDark) LearningPalette.ReportZinc700 else LearningPalette.ReportZinc200
    val inputCursor = if (isDark) Color.White else LearningPalette.ReportZinc900
    val inputTextColor = if (isDark) LearningPalette.ReportZinc100 else LearningPalette.ReportZinc900
    val hintTextColor = if (isDark) LearningPalette.ReportZinc400.copy(alpha = 0.6f) else LearningPalette.ReportZinc500.copy(alpha = 0.7f)
    val promptTextColor = if (isDark) LearningPalette.ReportZinc400 else LearningPalette.ReportZinc500

    val confirmButtonColor = if (isDark) LearningPalette.ReportZinc700 else LearningPalette.ReportZinc900

    // 错误选项配置
    val errMeaning = stringResource(R.string.report_err_meaning)
    val errFurigana = stringResource(R.string.report_err_furigana)
    val errExample = stringResource(R.string.report_err_example)
    val errSpelling = stringResource(R.string.report_err_spelling)
    val errPos = stringResource(R.string.report_err_pos)
    val errOther = stringResource(R.string.report_err_other)
    val errExplanation = stringResource(R.string.report_err_explanation)
    val errConnection = stringResource(R.string.report_err_connection)
    val errLevel = stringResource(R.string.report_err_level)

    val errorOptions = remember(learningMode, errMeaning, errFurigana, errExample, errSpelling, errPos, errOther, errExplanation, errConnection, errLevel) {
        if (learningMode == LearningMode.Word) {
            listOf(
                "meaning_error" to errMeaning,
                "furigana_error" to errFurigana,
                "example_error" to errExample,
                "spelling_error" to errSpelling,
                "pos_error" to errPos,
                "other" to errOther
            )
        } else {
            listOf(
                "meaning_error" to errExplanation,
                "connection_error" to errConnection,
                "furigana_error" to errFurigana,
                "example_error" to errExample,
                "level_error" to errLevel,
                "other" to errOther
            )
        }
    }

    var selectedType by remember { mutableStateOf(errorOptions.first().first) }
    var otherDescription by remember { mutableStateOf("") }

    val isDescriptionRequired = selectedType == "other"
    val isSubmitEnabled = !isDescriptionRequired || otherDescription.trim().isNotEmpty()
    val itemName = if (learningMode == LearningMode.Word) stringResource(R.string.mode_word) else stringResource(R.string.mode_grammar)

    NemoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.title_content_report),
        confirmText = stringResource(R.string.btn_confirm_feedback),
        dismissText = stringResource(R.string.btn_think_again),
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
                    text = stringResource(R.string.desc_select_error_type, itemName),
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
                            text = if (isDescriptionRequired) stringResource(R.string.report_other_desc_required) else stringResource(R.string.report_other_desc_placeholder),
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

