package com.jian.nemo.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.jian.nemo.core.designsystem.R as DesignR
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.component.common.NemoGooeyToggle
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.feature.settings.components.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 设置界面 (V2 Clean & Visual)
 *
 * 采用 Premium Card + List 布局，配合 Squircle 图标
 */
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToTtsSettings: () -> Unit,
    onNavigateToAdvancedLearning: () -> Unit,
    onNavigateToThemeSettings: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToCloudBackupHistory: () -> Unit,
    onCheckUpdate: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 获取真实登录状态
    val currentUser = uiState.user
    val isLoggedIn = uiState.isLoggedIn
    val avatarPath: String? = uiState.avatarPath

    // 对话框状态
    var showConfirmDialog by remember { mutableStateOf(false) }

    var isResetting by remember { mutableStateOf(false) }
    var resetErrorMessage by remember { mutableStateOf<String?>(null) }
    var isExportCompressed by remember { mutableStateOf(true) }

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

    // 导出文件选择器
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.onEvent(SettingsEvent.ConfirmExport(it, isExportCompressed)) }
    }

    // 导入文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onEvent(SettingsEvent.ImportData(it)) }
    }

    val useDarkTheme = when (uiState.darkMode) {
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
        DarkModeOption.AUTO -> {
            if (uiState.darkModeStrategy == DarkModeStrategy.FOLLOW_SYSTEM) {
                isSystemInDarkTheme()
            } else {
                try {
                    val now = java.time.LocalTime.now()
                    val start = java.time.LocalTime.parse(uiState.darkModeStartTime)
                    val end = java.time.LocalTime.parse(uiState.darkModeEndTime)
                    if (start < end) now in start..<end
                    else now !in end..<start
                } catch (_: Exception) {
                    isSystemInDarkTheme()
                }
            }
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background

    // Edge-to-Edge
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusBarHeight + 16.dp,
                bottom = navigationBarHeight + 104.dp
            )
        ) {
            // 大标题
            item {
                ImmersiveSettingsHeader(title = "设置")
            }

            // 账号与同步
            item {
                SettingsSectionTitle("账号")
                PremiumCard {
                    if (isLoggedIn && currentUser != null) {
                        // 用户信息 (Custom Squircle Implementation for Avatar)
                        UserProfileRow(
                            username = currentUser.username,
                            email = currentUser.email,
                            avatarPath = avatarPath,
                            onClick = onNavigateToLogin
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    } else {
                        // 未登录
                        SquircleSettingItem(
                            icon = Icons.Rounded.Person,
                            iconColor = NemoIndigo,
                            title = "登录/注册",
                            subtitle = "登录以体验完整功能",
                            onClick = onNavigateToLogin,
                            showDivider = true
                        )
                    }

                     SquircleSettingItem(
                        icon = Icons.Rounded.CloudUpload,
                        iconColor = NemoCyan,
                        title = "备份到云端",
                        subtitle = "将当前进度安全保存在云端",
                        onClick = { viewModel.onEvent(SettingsEvent.BackupToCloud) },
                        showDivider = true
                    )
                     SquircleSettingItem(
                        icon = Icons.Rounded.CloudSync,
                        iconColor = NemoPurple,
                        title = "云端备份历史",
                        subtitle = "查看或从云端恢复历史备份",
                        onClick = onNavigateToCloudBackupHistory,
                        showDivider = false
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 外观
            item {
                SettingsSectionTitle("外观") // 使用分组标题
                PremiumCard {
                    // 主题外观
                    SquircleSettingItem(
                        icon = Icons.Rounded.Contrast,
                        iconColor = NemoPurple,
                        title = "主题外观",
                        subtitle = when (uiState.darkMode) {
                            DarkModeOption.LIGHT -> "浅色"
                            DarkModeOption.DARK -> "深色"
                            DarkModeOption.AUTO -> "自动"
                        },
                        onClick = onNavigateToThemeSettings,
                        showDivider = false
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 学习
            item {
                SettingsSectionTitle("学习")
                PremiumCard {
                    SquircleSettingItem(
                        icon = Icons.Rounded.TrackChanges,
                        iconColor = NemoOrange,
                        title = "每日单词目标",
                        subtitle = "设置每天要学习的单词数量",
                        onClick = { viewModel.onEvent(SettingsEvent.ShowDailyGoalDialog(true)) },
                        showDivider = true,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${uiState.dailyGoal}个",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    )

                    SquircleSettingItem(
                        icon = Icons.Rounded.JoinLeft,
                        iconColor = NemoSecondary,
                        title = "每日语法目标",
                        subtitle = "设置每天要学习的语法数量",
                        onClick = { viewModel.onEvent(SettingsEvent.ShowGrammarDailyGoalDialog(true)) },
                        showDivider = true,
                         trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${uiState.grammarDailyGoal}条",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    )

                    SquircleSettingItem(
                        icon = Icons.Rounded.Schedule,
                        iconColor = NemoIndigo,
                        title = "学习日重置时间",
                        subtitle = "零点跨天保护，过了此时间才算新的一天",
                        onClick = { viewModel.onEvent(SettingsEvent.ShowLearningDayResetHourDialog(true)) },
                        showDivider = true,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${uiState.learningDayResetHour}:00",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    )

                    SquircleSettingItem(
                        icon = Icons.Rounded.Shuffle,
                        iconColor = NemoPrimary,
                        title = "新内容乱序抽取",
                        subtitle = if (uiState.isRandomNewContentEnabled) "随机抽取新内容" else "按顺序抽取新内容",
                        onClick = { viewModel.onEvent(SettingsEvent.SetRandomNewContentEnabled(!uiState.isRandomNewContentEnabled)) },
                        showDivider = true,
                        trailing = {
                            NemoGooeyToggle(
                                checked = uiState.isRandomNewContentEnabled,
                                onCheckedChange = { viewModel.onEvent(SettingsEvent.SetRandomNewContentEnabled(it)) },
                                activeColor = MaterialTheme.colorScheme.primary,
                                inactiveColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    )

                    SquircleSettingItem(
                        icon = ImageVector.vectorResource(DesignR.drawable.ic_neurology),
                        iconColor = NemoPurple,
                        title = "记忆算法配置",
                        subtitle = "步进、提前复习与 Leech 策略",
                        onClick = onNavigateToAdvancedLearning,
                        showDivider = false,
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )

                    SquircleSettingItem(
                        icon = Icons.Rounded.AutoAwesome,
                        iconColor = NemoCyan,
                        title = "AI 工坊配置",
                        subtitle = "设置 API Key 与 AI 模型",
                        onClick = onNavigateToAiSettings,
                        showDivider = false,
                        trailing = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 语音
            item {
                SettingsSectionTitle("语音")
                PremiumCard {
                    SquircleSettingItem(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        iconColor = Color(0xFFFF2D55), // NemoRed/Pink
                        title = "语音参数",
                        subtitle = "调节语速和音调",
                        onClick = onNavigateToTtsSettings,
                        showDivider = false
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 数据
            item {
                SettingsSectionTitle("数据")
                PremiumCard {
                    SquircleSettingItem(
                        icon = Icons.Rounded.FileDownload,
                        iconColor = NemoSecondary,
                        title = "导出数据",
                        subtitle = "导出本地数据文件",
                        onClick = {
                            viewModel.onEvent(SettingsEvent.RequestExport)
                        },
                        showDivider = true
                    )
                     SquircleSettingItem(
                        icon = Icons.Rounded.FileUpload,
                        iconColor = NemoPrimary,
                        title = "导入本地数据",
                        subtitle = "从文件恢复进度",
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                        showDivider = true
                    )

                     SquircleSettingItem(
                        icon = Icons.Rounded.Delete,
                        iconColor = NemoDanger, // NemoRed/Danger
                         title = "重置学习进度",
                        subtitle = "清空所有数据 (慎用)",
                        onClick = {
                            isResetting = false
                            showConfirmDialog = true
                        },
                        showDivider = false
                    )

                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 关于
            item {
                SettingsSectionTitle("关于")
                PremiumCard {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val versionName = remember { com.jian.nemo.core.ui.util.AppUtils.getVersionName(context) }
                    

                    SquircleSettingItem(
                        icon = Icons.Rounded.SystemUpdate,
                        iconColor = NemoSecondary,
                        title = "检查更新",
                        subtitle = "获取最新版本",
                        onClick = onCheckUpdate,
                        showDivider = false
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }


        NemoSnackbar(
            visible = snackbarVisible,
            message = snackbarMessage,
            type = snackbarType,
            onDismiss = { snackbarVisible = false },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarHeight + 8.dp)
        )
    }

    // 对话框和 Bottom Sheet 保持不变
    if (uiState.showDailyGoalDialog) {
        DailyGoalSelectionBottomSheet(
            currentGoal = uiState.dailyGoal,
            onDismiss = { viewModel.onEvent(SettingsEvent.ShowDailyGoalDialog(false)) },
            onGoalSelected = { viewModel.onEvent(SettingsEvent.SetDailyGoal(it)) }
        )
    }

    if (uiState.showGrammarDailyGoalDialog) {
        GrammarDailyGoalSelectionBottomSheet(
            currentGoal = uiState.grammarDailyGoal,
            onDismiss = { viewModel.onEvent(SettingsEvent.ShowGrammarDailyGoalDialog(false)) },
            onGoalSelected = { viewModel.onEvent(SettingsEvent.SetGrammarDailyGoal(it)) }
        )
    }

    if (uiState.showLearningDayResetHourDialog) {
        LearningDayResetHourBottomSheet(
            currentHour = uiState.learningDayResetHour,
            onDismiss = { viewModel.onEvent(SettingsEvent.ShowLearningDayResetHourDialog(false)) },
            onHourSelected = { viewModel.onEvent(SettingsEvent.SetLearningDayResetHour(it)) }
        )
    }


    // 重置确认对话框
    if (showConfirmDialog) {
        ConfirmResetDialog(
            isResetting = isResetting,
            errorMessage = resetErrorMessage,
            isLoggedIn = isLoggedIn,
            useDarkTheme = useDarkTheme,
            onDismiss = {
                showConfirmDialog = false
            },
            onConfirm = {
                isResetting = true
                viewModel.onEvent(SettingsEvent.ResetProgress)
                showConfirmDialog = false
            }
        )
    }


    // 导出选项对话框
    if (uiState.showExportOptionsDialog) {
        ExportOptionsDialog(
            onDismiss = { viewModel.onEvent(SettingsEvent.CancelExport) },
            onConfirm = { isCompressed -> 
                isExportCompressed = isCompressed
                viewModel.onEvent(SettingsEvent.CancelExport)
                val ext = if (isCompressed) ".json.gz" else ".json"
                val fileName = "nemo_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}$ext"
                exportLauncher.launch(fileName)
            }
        )
    }

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
}

/**
 * 自定义 User Profile Row (模仿 SquircleSettingItem 但带头像)
 */
@Composable
private fun UserProfileRow(
    username: String,
    email: String?,
    avatarPath: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像代替 Icon
        AvatarImage(
            username = username,
            avatarPath = avatarPath,
            size = 42.dp, // Match squircle icon background size roughly
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
             Text(
                text = username,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (email != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

         Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
    }
}



