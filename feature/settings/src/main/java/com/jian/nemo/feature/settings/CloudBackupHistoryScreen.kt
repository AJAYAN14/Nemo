package com.jian.nemo.feature.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.data.manager.BackupInfo
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.jian.nemo.feature.settings.components.RestoreStrategyDialog
import com.jian.nemo.feature.settings.components.RestorePreviewDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CloudBackupHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var snackbarVisible by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarType by remember { mutableStateOf(NemoSnackbarType.INFO) }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarMessage = it
            snackbarType = if (it.contains("失败") || it.contains("异常")) NemoSnackbarType.ERROR else NemoSnackbarType.SUCCESS
            snackbarVisible = true
            viewModel.onEvent(SettingsEvent.ClearToast)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(SettingsEvent.ShowCloudBackupList)
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        NemoScaffold(
            title = "云端备份历史",
            onBack = onNavigateBack,
            actions = {
                IconButton(onClick = { viewModel.onEvent(SettingsEvent.ShowCloudBackupList) }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "刷新列表"
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.isLoading || uiState.isCloudSyncing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NemoChasingDotsLoader()
                    }
                } else if (uiState.cloudBackupList.isEmpty()) {
                    EmptyBackupState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = paddingValues.calculateTopPadding() + 12.dp,
                            bottom = paddingValues.calculateBottomPadding() + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                    items(uiState.cloudBackupList, key = { it.fileName }) { backup ->
                        BackupItemCard(
                            backup = backup,
                            modifier = Modifier.animateItem(),
                            onRestoreClicked = { 
                                viewModel.onEvent(SettingsEvent.SelectRestoreFile(backup.fileName))
                            }
                        )
                    }
                }
            }
            // 恢复策略选择弹窗留在这里处理
            if (uiState.showRestoreStrategyDialog && uiState.pendingRestoreFileName != null) {
                RestoreStrategyDialog(
                    fileName = uiState.pendingRestoreFileName!!,
                    onConfirm = { strategy ->
                        viewModel.onEvent(SettingsEvent.RestoreFromCloud(uiState.pendingRestoreFileName!!, strategy))
                    },
                    onDismiss = {
                        viewModel.onEvent(SettingsEvent.CancelRestore)
                    }
                )
            }
            
            if (uiState.restorePreview != null) {
                RestorePreviewDialog(
                    preview = uiState.restorePreview!!,
                    onConfirm = { viewModel.onEvent(SettingsEvent.ConfirmRestore) },
                    onDismiss = { viewModel.onEvent(SettingsEvent.CancelRestorePreview) }
                )
            }
        } // 闭合内层 Box
    } // 闭合 Scaffold

        NemoSnackbar(
            message = snackbarMessage,
            visible = snackbarVisible,
            type = snackbarType,
            onDismiss = { snackbarVisible = false },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarHeight + 8.dp)
        )
    }
}

@Composable
private fun EmptyBackupState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "暂无云端备份",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "返回上一页点击“备份到云端”将数据安全地保存在云端",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BackupItemCard(
    backup: BackupInfo,
    modifier: Modifier = Modifier,
    onRestoreClicked: () -> Unit
) {
    val formatter = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault())
    val dateString = formatter.format(Date(backup.createdAt))
    val sizeKb = String.format("%.1f", backup.sizeBytes / 1024f)

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 16.dp, isDark = isDark),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "文件大小: $sizeKb KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            FilledTonalIconButton(
                onClick = onRestoreClicked,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = "恢复此备份"
                )
            }
        }
    }
}
