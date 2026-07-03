package com.jian.nemo.feature.settings.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (isCompressed: Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出选项", fontWeight = FontWeight.Bold) },
        text = { Text("压缩导出能大幅减小文件体积并保护数据结构；纯文本导出则文件较大，但可用文本编辑器直接阅读，适合用于调试。") },
        confirmButton = {
            Button(
                onClick = { onConfirm(true) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("压缩导出 (推荐)")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onConfirm(false) }
            ) {
                Text("纯文本导出")
            }
        }
    )
}
