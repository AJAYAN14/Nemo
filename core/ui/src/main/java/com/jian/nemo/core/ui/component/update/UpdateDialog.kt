package com.jian.nemo.core.ui.component.update

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.domain.model.AppUpdateConfig
import com.jian.nemo.core.ui.component.sheet.NemoModalBottomSheet

/**
 * 应用更新弹窗 (Modern Flat UI Style)
 *
 * 严格遵循 UI/UX PRO MAX 规范：
 * 1. 扁平化设计 (Flat UI)：无阴影、无渐变、无模糊。
 * 2. 高对比度配色：深色模式使用 Slate 900，交互使用纯蓝/纯黑。
 * 3. 硬朗几何感：1.5px 线条描边，分块明确。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    config: AppUpdateConfig,
    downloadProgress: Float,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onUpdateClick: () -> Unit,
    onInstallClick: () -> Unit,
    onExitClick: () -> Unit,
    onDismissRequest: () -> Unit
) {
    // 状态与环境
    val scrollState = rememberScrollState()

    // 强制更新策略：已移除，现在通过标准 BottomSheet 展示，始终允许关闭
    // 但为了防止下载意外中断，正在下载时禁止关闭
    val canDismiss = !isDownloading

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { sheetValue ->
            if (!canDismiss && sheetValue == SheetValue.Hidden) {
                false
            } else {
                true
            }
        }
    )

    // UI/UX Pro Max 风格：Clean Surface
    val containerColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface

    NemoModalBottomSheet(
        onDismissRequest = {
            if (canDismiss) {
                onDismissRequest()
            }
        },
        sheetState = sheetState,
        dragHandle = {
            if (canDismiss) {
                BottomSheetDefaults.DragHandle()
            }
        },
        containerColor = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) {
        // 防止下载过程中意外触碰返回键关闭
        if (!canDismiss) {
            androidx.activity.compose.BackHandler(enabled = true) {
                // Do nothing
            }
        }

        UpdateDialogContent(
            config = config,
            downloadProgress = downloadProgress,
            isDownloading = isDownloading,
            isDownloaded = isDownloaded,
            onUpdateClick = onUpdateClick,
            onInstallClick = onInstallClick,
            onDismissRequest = onDismissRequest
        )
    }
}
