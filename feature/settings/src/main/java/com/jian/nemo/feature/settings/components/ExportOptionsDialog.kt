package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (isCompressed: Boolean) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    // 浅色纯白，深色纯净深灰，避免 MD3 默认自带的 primary tint
    val dialogBgColor = if (isDark) Color(0xFF202020) else Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 280.dp, max = 340.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp), // 现代大圆角
            color = dialogBgColor,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // 标题
                Text(
                    text = "导出选项",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 正文
                Text(
                    text = "压缩导出能大幅减小文件体积并保护数据结构；纯文本导出则文件较大，但可用文本编辑器直接阅读，适合用于调试。",
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // 底部水平按钮组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 次要操作
                    TextButton(
                        onClick = { onConfirm(false) },
                        shape = CircleShape // 两边圆形
                    ) {
                        Text(
                            text = "纯文本导出",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // 主要操作
                    Button(
                        onClick = { onConfirm(true) },
                        shape = CircleShape, // 两边圆形
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "压缩导出 (推荐)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
