package com.jian.nemo.feature.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.Capsule
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
    isFromLogin: Boolean = false,
    onSkipRestore: (() -> Unit)? = null,
    onRestoreSuccess: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var snackbarVisible by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarType by remember { mutableStateOf(NemoSnackbarType.INFO) }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            val isSuccess = !it.contains("失败") && !it.contains("异常")
            snackbarMessage = it
            snackbarType = if (isSuccess) NemoSnackbarType.SUCCESS else NemoSnackbarType.ERROR
            snackbarVisible = true
            viewModel.onEvent(SettingsEvent.ClearToast)
            if (isSuccess && it.contains("导入成功") && onRestoreSuccess != null) {
                kotlinx.coroutines.delay(1200)
                onRestoreSuccess()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(SettingsEvent.ShowCloudBackupList)
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isDictionaryPreparing = !uiState.isDictionaryReady || uiState.isDictionarySyncing
    val backdrop = rememberLayerBackdrop()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.layerBackdrop(backdrop),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                val isRefreshing = uiState.isLoading || uiState.isCloudSyncing
                val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
                val rotation by if (isRefreshing) {
                    infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotation"
                    )
                } else {
                    remember { mutableStateOf(0f) }
                }

                val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val navGroupBg = if (isDarkTheme) Color.White.copy(alpha = 0.15f) else Color.White

                if (isFromLogin) {
                    // 首次启动 / 登录引导模式下的沉浸式向导顶栏 (Hero Header)
                    // 彻底移除二级返回键，大标题从 24dp 边距完全左对齐起步，垂直方向自然紧贴状态栏
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 24.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Cloud,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "云端档案同步",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Text(
                                    text = "恢复你的学习档案",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "检测到云端历史记录，选择一份备份同步至本机",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            LiquidButton(
                                onClick = {
                                    if (!isRefreshing) {
                                        viewModel.onEvent(SettingsEvent.ShowCloudBackupList)
                                    }
                                },
                                backgroundColor = navGroupBg,
                                shape = CircleShape,
                                isInteractive = true,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "刷新列表",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                                )
                            }
                        }
                    }
                } else {
                    // 从设置页进入的二级管理模式：保留左侧返回键
                    CommonHeader(
                        title = "云端备份历史",
                        onBack = onNavigateBack,
                        actions = {
                            LiquidButton(
                                onClick = {
                                    if (!isRefreshing) {
                                        viewModel.onEvent(SettingsEvent.ShowCloudBackupList)
                                    }
                                },
                                backgroundColor = navGroupBg,
                                shape = CircleShape,
                                isInteractive = true,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "刷新列表",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                                )
                            }
                        }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 如果是从登录页面首次进入且词库仍在同步中，展示全屏准备状态
                if (isFromLogin && isDictionaryPreparing && uiState.cloudBackupList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NemoChasingDotsLoader()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "正在准备基础词库...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请稍候，基础词库下载完成后将自动展示历史备份",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (uiState.isLoading || uiState.isCloudSyncing) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        NemoChasingDotsLoader()
                    }
                } else if (uiState.cloudBackupList.isEmpty()) {
                    EmptyBackupState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = if (isFromLogin) {
                            PaddingValues(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 20.dp)
                        } else {
                            PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)
                        },
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 词库准备中提示条
                        if (isDictionaryPreparing) {
                            item(key = "dictionary_syncing_banner") {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "基础词库正在准备中，请稍候恢复...",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(uiState.cloudBackupList, key = { _, backup -> backup.fileName }) { index, backup ->
                            BackupItemCard(
                                backup = backup,
                                isLatest = isFromLogin && index == 0,
                                isRestoreEnabled = !isDictionaryPreparing,
                                modifier = Modifier.animateItem(),
                                onRestoreClicked = { 
                                    viewModel.onEvent(SettingsEvent.SelectRestoreFile(backup.fileName))
                                }
                            )
                        }

                        if (isFromLogin) {
                            item(key = "bottom_floating_spacer") {
                                Spacer(modifier = Modifier.height(100.dp))
                            }
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

        // ===== 悬浮跳过按钮 (Floating Overlay - 1:1 对标 Demo 项目标准规范) =====
        if (isFromLogin) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                ColoredLiquidGlassButton(
                    onClick = { onSkipRestore?.invoke() ?: onNavigateBack() },
                    backdrop = backdrop,
                    tint = Color(0xFF0088FF),
                    shape = Capsule(),
                    isInteractive = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "跳过，稍后在设置中恢复",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }

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
    isLatest: Boolean = false,
    isRestoreEnabled: Boolean = true,
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
        shape = RoundedCornerShape(16.dp),
        border = if (isLatest) {
            androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        } else null
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isLatest) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "最新进度",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "文件大小: $sizeKb KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            FilledTonalIconButton(
                onClick = onRestoreClicked,
                enabled = isRestoreEnabled,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isRestoreEnabled) 0.12f else 0.04f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.CloudDownload,
                    contentDescription = if (isRestoreEnabled) "恢复此备份" else "词库准备中，暂不可恢复"
                )
            }
        }
    }
}
