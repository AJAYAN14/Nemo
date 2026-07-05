package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.data.manager.ImportPreview
import com.jian.nemo.core.data.manager.ImportStrategy

@Composable
fun RestorePreviewDialog(
    preview: ImportPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (preview.strategy == ImportStrategy.MERGE) "合并恢复预览" else "覆盖恢复预览",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (preview.validationSummary.isNotEmpty()) {
                    Text(
                        text = "数据校验: ${preview.validationSummary}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (preview.strategy == ImportStrategy.MERGE) {
                    Text("词库合并：")
                    Text("• 新增: ${preview.wordInsertCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• 更新: ${preview.wordUpdateCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• 跳过: ${preview.wordSkipCount}", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("语法合并：")
                    Text("• 新增: ${preview.grammarInsertCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• 更新: ${preview.grammarUpdateCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• 跳过: ${preview.grammarSkipCount}", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("原有数据将受影响：", color = MaterialTheme.colorScheme.error)
                    Text("• 本地进度将被清空(${preview.localWordStateCount + preview.localGrammarStateCount}条)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("将导入：")
                    Text("• 词库: ${preview.wordInsertCount}", style = MaterialTheme.typography.bodySmall)
                    Text("• 语法: ${preview.grammarInsertCount}", style = MaterialTheme.typography.bodySmall)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text("其他数据：")
                Text("• 新增错题: ${preview.wrongAnswerNewCount}", style = MaterialTheme.typography.bodySmall)
                Text("• 新增测验记录: ${preview.testRecordNewCount}", style = MaterialTheme.typography.bodySmall)
                Text("• 新增学习记录: ${preview.studyRecordNewCount}", style = MaterialTheme.typography.bodySmall)
                Text("• 新增收藏: ${preview.favoriteNewCount}", style = MaterialTheme.typography.bodySmall)

                if (preview.settingsWillChange) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "注：本次恢复将覆盖应用设置",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确认恢复")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
