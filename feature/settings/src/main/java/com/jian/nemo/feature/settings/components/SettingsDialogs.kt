package com.jian.nemo.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag


/**
 * 重置确认对话框 (V2: Pro Max Style)
 */
@Composable
fun ConfirmResetDialog(
    isResetting: Boolean = false,
    errorMessage: String? = null,
    isLoggedIn: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    useDarkTheme: Boolean = isSystemInDarkTheme()
) {
    com.jian.nemo.core.ui.component.NemoDialog(
        onDismissRequest = onDismiss,
        title = "确认重置",
        text = "您确定要重置所有学习进度吗？此操作将永久删除本地所有进度数据，且无法撤销。",
        isDangerous = true,
        confirmText = if (isResetting) "正在重置..." else "确认重置",
        dismissText = "取消",
        isLoading = isResetting,
        onConfirm = onConfirm
    )
}

