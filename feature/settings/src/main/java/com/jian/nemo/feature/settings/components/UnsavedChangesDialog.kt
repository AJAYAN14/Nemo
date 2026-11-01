package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.settings.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 未保存更改提示弹窗 (Flat UI 风格)
 * 当用户在离开设置页面且存在未应用修改时弹出
 */
@Composable
fun UnsavedChangesDialog(
    onSaveAndExit: () -> Unit,
    onDiscardChanges: () -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    com.jian.nemo.core.ui.component.NemoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_dialog_title_unsaved_changes),
        text = stringResource(R.string.settings_dialog_msg_unsaved_changes),
        confirmText = stringResource(R.string.settings_dialog_btn_apply_changes),
        dismissText = stringResource(R.string.settings_dialog_btn_discard_changes),
        confirmButtonColor = MaterialTheme.colorScheme.primary,
        onConfirm = onSaveAndExit,
        onDismiss = onDiscardChanges
    )
}
