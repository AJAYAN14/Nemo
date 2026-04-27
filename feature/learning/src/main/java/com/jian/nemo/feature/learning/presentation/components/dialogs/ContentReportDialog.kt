package com.jian.nemo.feature.learning.presentation.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.jian.nemo.core.designsystem.theme.NemoIndigo
import com.jian.nemo.feature.learning.presentation.LearningMode

/**
 * 内容报告确认对话框 (Premium Style)
 * 采用 Nemo 统一的对话框设计语言：Squircle 圆角 + 头部图标 + 沉浸式排版
 */
@Composable
fun ContentReportDialog(
    learningMode: LearningMode,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    // 颜色配置
    val primaryColor = NemoIndigo
    val containerColor = MaterialTheme.colorScheme.surface
    val titleColor = MaterialTheme.colorScheme.onSurface
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = containerColor,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header Icon With Feedback Background
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            color = primaryColor.copy(alpha = 0.12f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Report,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Dynamic Description
                val itemName = if (learningMode == LearningMode.Word) "单词" else "语法"
                Text(
                    text = "确定要向开发者报告当前这个 ${itemName} 的内容有误吗？您的反馈对我们非常重要。",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = bodyColor
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 4. Action Buttons (Standard Pro Max Style)
                Button(
                    onClick = onConfirm,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("确认反馈", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

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
