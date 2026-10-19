package com.jian.nemo.feature.settings.components

import androidx.compose.runtime.Composable
import com.jian.nemo.core.ui.component.NemoDialog

/**
 * 导出选项对话框
 * 全面接入项目通用的 NemoDialog，自适应深浅色模式与 48dp squircle 统一设计
 */
@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: (isCompressed: Boolean) -> Unit
) {
    NemoDialog(
        onDismissRequest = onDismiss,
        title = "导出选项",
        text = "压缩导出能大幅减小文件体积并保护数据结构；纯文本导出则文件较大，但可用文本编辑器直接阅读，适合用于调试。",
        confirmText = "压缩导出 (推荐)",
        dismissText = "纯文本导出",
        onConfirm = { onConfirm(true) },
        onDismiss = { onConfirm(false) }
    )
}
