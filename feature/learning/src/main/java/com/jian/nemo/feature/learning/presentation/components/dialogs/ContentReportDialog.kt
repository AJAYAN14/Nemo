package com.jian.nemo.feature.learning.presentation.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jian.nemo.core.designsystem.theme.NemoIndigo
import com.jian.nemo.feature.learning.presentation.LearningMode

/**
 * 内容报告确认对话框
 */
@Composable
fun ContentReportDialog(
    learningMode: LearningMode,
    onDismiss: () -> Unit,
    onConfirm: (errorType: String, description: String?) -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // 颜色配置
    val primaryColor = NemoIndigo
    val containerColor = MaterialTheme.colorScheme.surface
    val titleColor = MaterialTheme.colorScheme.onSurface
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant

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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = containerColor,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 28.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header Icon With Feedback Background
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = primaryColor.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Report,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Title
                Text(
                    text = "报告内容错误",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = titleColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Description
                val itemName = if (learningMode == LearningMode.Word) "单词" else "语法"
                Text(
                    text = "确定要向开发者报告当前这个 ${itemName} 的内容有误吗？请选择错误类型：",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = bodyColor
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Choice Grid (2 Columns, Selectable Card)
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
                                            if (isSelected) primaryColor.copy(alpha = 0.08f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                        )
                                        .border(
                                            BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) primaryColor
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedType = code }
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.5.sp
                                        ),
                                        color = if (isSelected) primaryColor else titleColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // If row has only one item, fill space
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 4b. Description Input Field (Always visible, mandatory for 'other')
                val isDescriptionRequired = selectedType == "other"
                val isSubmitEnabled = !isDescriptionRequired || otherDescription.trim().isNotEmpty()

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = otherDescription,
                    onValueChange = { if (it.length <= 100) otherDescription = it },
                    placeholder = {
                        Text(
                            text = if (isDescriptionRequired) "请详细描述错误内容（必填，100字以内）" else "请补充具体错误细节（选填，100字以内）",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
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
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 86.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Action Buttons (Standard Pro Max Style)
                Button(
                    onClick = { 
                        val desc = otherDescription.trim().takeIf { it.isNotEmpty() }
                        onConfirm(selectedType, desc) 
                    },
                    enabled = isSubmitEnabled,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White,
                        disabledContainerColor = primaryColor.copy(alpha = 0.38f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("确认反馈", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "我再想想",
                        color = bodyColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
