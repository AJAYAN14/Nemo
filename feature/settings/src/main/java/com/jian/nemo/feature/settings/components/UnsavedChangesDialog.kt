package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        title = "未保存的更改",
        text = "您对参数进行了修改，是否在离开前应用这些配置？",
        confirmText = "应用更改",
        dismissText = "放弃更改",
        confirmButtonColor = MaterialTheme.colorScheme.primary,
        onConfirm = onSaveAndExit,
        onDismiss = onDiscardChanges
    )
}
