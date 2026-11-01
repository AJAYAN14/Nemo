package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.jian.nemo.feature.settings.R


/**
 * 重置确认对话框
 */
@Composable
fun ConfirmResetDialog(
    isResetting: Boolean = false,
    errorMessage: String? = null,
    isLoggedIn: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    useDarkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    com.jian.nemo.core.ui.component.NemoDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.settings_dialog_title_confirm_reset),
        text = stringResource(R.string.settings_dialog_msg_confirm_reset),
        isDangerous = true,
        confirmText = if (isResetting) stringResource(R.string.settings_dialog_btn_resetting) else stringResource(R.string.settings_dialog_btn_confirm_reset),
        dismissText = stringResource(R.string.settings_btn_cancel),
        isLoading = isResetting,
        onConfirm = onConfirm
    )
}

