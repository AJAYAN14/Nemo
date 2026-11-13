package com.jian.nemo.core.ui.component.update

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.ui.component.dialog.NemoWindowBlurBehind
import com.jian.nemo.core.designsystem.theme.UiPalette
import com.jian.nemo.core.domain.model.AppUpdateConfig

/**
 * 应用版本更新弹窗组件
 */
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
    val isAppDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val dialogShape = RoundedRectangle(40.dp)
    val isMandatory = config.isForce || !config.canClose

    val containerColor = if (isAppDark) UiPalette.DialogAltDarkBg else Color.White
    val borderColor = if (isAppDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    Dialog(
        onDismissRequest = {
            if (!isMandatory) {
                onDismissRequest()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isMandatory,
            dismissOnClickOutside = !isMandatory
        )
    ) {
        NemoWindowBlurBehind()

        Box(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!isAppDark) {
                            Modifier.shadow(16.dp, dialogShape)
                        } else {
                            Modifier
                        }
                    )
                    .clip(dialogShape)
                    .background(containerColor, dialogShape)
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = dialogShape
                    )
                    .padding(24.dp)
            ) {
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
    }
}
