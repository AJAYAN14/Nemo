package com.jian.nemo.feature.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.feature.settings.R

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
        title = stringResource(R.string.settings_dialog_title_export_options),
        text = stringResource(R.string.settings_dialog_msg_export_options),
        confirmText = stringResource(R.string.settings_dialog_btn_export_zip_recommended),
        dismissText = stringResource(R.string.settings_dialog_btn_export_text),
        onConfirm = { onConfirm(true) },
        onDismiss = { onConfirm(false) }
    )
}
