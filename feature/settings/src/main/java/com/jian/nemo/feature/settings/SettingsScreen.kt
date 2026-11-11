package com.jian.nemo.feature.settings

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.jian.nemo.feature.settings.R
import com.jian.nemo.core.designsystem.R as DesignR
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.AvatarImage
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.core.ui.component.popup.NemoPopupMenu
import com.jian.nemo.core.ui.component.popup.NemoPopupMenuState
import com.jian.nemo.core.ui.component.popup.NemoPopupMenuItem
import com.jian.nemo.core.ui.component.popup.NemoSelectivePopupMenuItem
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.jian.nemo.feature.settings.components.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 设置界面 (V2 Clean & Visual)
 *
 * 采用 Premium Card + List 布局，配合 Squircle 图标
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToTtsSettings: () -> Unit,
    onNavigateToAdvancedLearning: () -> Unit,
    onNavigateToThemeSettings: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToCloudBackupHistory: () -> Unit,
    onNavigateToLiquidLab: () -> Unit,
    onNavigateToCornerLab: () -> Unit,
    onNavigateToAbout: () -> Unit,
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
    var showRandomNewContentConfirmDialog by remember { mutableStateOf(false) }
    var randomNewContentResetSignal by remember { mutableIntStateOf(0) }

    var isResetting by remember { mutableStateOf(false) }
    var resetErrorMessage by remember { mutableStateOf<String?>(null) }
    var isExportCompressed by remember { mutableStateOf(true) }

    var snackbarVisible by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarType by remember { mutableStateOf(NemoSnackbarType.INFO) }

    // 物理折射与气泡悬浮菜单
    val backdrop = rememberLayerBackdrop()
    var popupMenuState by remember { mutableStateOf(NemoPopupMenuState()) }
    var activeMenuType by remember { mutableStateOf("") }

    val currentMenuItems: List<NemoPopupMenuItem> = when (activeMenuType) {
        "dailyGoal" -> {
            listOf(5, 10, 20, 30, 50).map { goal ->
                NemoSelectivePopupMenuItem(
                    text = stringResource(R.string.settings_components_format_word_goal, goal),
                    isSelected = { uiState.dailyGoal == goal },
                    dismissOnClick = true,
                    onClick = {
                        viewModel.onEvent(SettingsEvent.SetDailyGoal(goal))
                    }
                )
            }
        }
        "grammarGoal" -> {
            listOf(5, 10, 15, 20, 25).map { goal ->
                NemoSelectivePopupMenuItem(
                    text = stringResource(R.string.settings_components_format_grammar_goal, goal),
                    isSelected = { uiState.grammarDailyGoal == goal },
                    dismissOnClick = true,
                    onClick = {
                        viewModel.onEvent(SettingsEvent.SetGrammarDailyGoal(goal))
                    }
                )
            }
        }
        "resetHour" -> {
            listOf(0, 2, 4, 5, 6).map { hour ->
                val label = when (hour) {
                    0 -> stringResource(R.string.settings_components_reset_hour_midnight)
                    2 -> stringResource(R.string.settings_components_reset_hour_2am)
                    4 -> stringResource(R.string.settings_components_reset_hour_4am_recommended)
                    5 -> stringResource(R.string.settings_components_reset_hour_5am)
                    6 -> stringResource(R.string.settings_components_reset_hour_6am)
                    else -> "${hour}:00"
                }
                NemoSelectivePopupMenuItem(
                    text = label,
                    isSelected = { uiState.learningDayResetHour == hour },
                    dismissOnClick = true,
                    onClick = {
                        viewModel.onEvent(SettingsEvent.SetLearningDayResetHour(hour))
                    }
                )
            }
        }
        else -> emptyList()
    }

    val keywordFailure = stringResource(R.string.settings_keyword_label_failure)
    val keywordException = stringResource(R.string.settings_keyword_label_exception)

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            snackbarMessage = it
            snackbarType = if (it.contains(keywordFailure) || it.contains(keywordException)) NemoSnackbarType.ERROR else NemoSnackbarType.SUCCESS
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

    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    // Edge-to-Edge
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val configuration = LocalConfiguration.current
    val appContext = LocalContext.current.applicationContext
    var resumeTrigger by remember { mutableIntStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        resumeTrigger++
    }

    // 元数据仅读取已加载的内存级配置与版本信息，进入设置页即刻异步预热，避免快速点击关于页时首帧空白
    LaunchedEffect(appContext) {
        AboutViewModel.preloadMetadata(appContext)
    }

    LaunchedEffect(configuration.screenWidthDp, density.density, resumeTrigger) {
        val targetLogoPx = AboutLogoCache.calculateTargetSizePx(
            screenWidthDp = configuration.screenWidthDp,
            density = density.density
        )
        if (AboutLogoCache.getCached(targetLogoPx) != null) return@LaunchedEffect
        delay(350L)
        AboutLogoCache.preload(appContext, targetLogoPx)
    }

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarHeight + 16.dp,
                    bottom = navigationBarHeight + 104.dp
                )
            ) {
                // 大标题
                item {
                    ImmersiveSettingsHeader(title = stringResource(R.string.settings_title_screen))
                }

                // 账号与同步
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_section_title_account), modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp))
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
                                modifier = Modifier.padding(start = 74.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                thickness = 0.5.dp
                            )
                        } else {
                            // 未登录
                            SquircleSettingItem(
                                icon = Icons.Rounded.Person,
                                iconColor = SystemColors.Blue,
                                title = stringResource(R.string.settings_item_title_login),
                                subtitle = stringResource(R.string.settings_item_subtitle_login),
                                onClick = onNavigateToLogin,
                                showDivider = true
                            )
                        }

                        SquircleSettingItem(
                            icon = Icons.Rounded.CloudUpload,
                            iconColor = SystemColors.Cyan,
                            title = stringResource(R.string.settings_item_title_backup),
                            subtitle = stringResource(R.string.settings_item_subtitle_backup),
                            onClick = { viewModel.onEvent(SettingsEvent.BackupToCloud) },
                            showDivider = true
                        )
                        SquircleSettingItem(
                            icon = Icons.Rounded.History,
                            iconColor = SystemColors.Indigo,
                            title = stringResource(R.string.settings_item_title_backup_history),
                            subtitle = stringResource(R.string.settings_item_subtitle_backup_history),
                            onClick = onNavigateToCloudBackupHistory,
                            showDivider = false
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 外观
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_section_title_appearance), modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp))
                    PremiumCard {
                        // 主题外观
                        SquircleSettingItem(
                            icon = Icons.Rounded.Palette,
                            iconColor = SystemColors.Purple,
                            title = stringResource(R.string.settings_theme_title_screen),
                            subtitle = when (uiState.darkMode) {
                                DarkModeOption.LIGHT -> stringResource(R.string.settings_theme_label_light)
                                DarkModeOption.DARK -> stringResource(R.string.settings_theme_label_dark)
                                DarkModeOption.AUTO -> stringResource(R.string.settings_theme_label_auto)
                            },
                            onClick = onNavigateToThemeSettings,
                            showDivider = false
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 学习
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_section_title_learning), modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp))
                    PremiumCard {
                        SquircleSettingItem(
                            icon = Icons.Rounded.TrackChanges,
                            iconColor = SystemColors.Orange,
                            title = stringResource(R.string.settings_item_title_daily_word_goal),
                            subtitle = stringResource(R.string.settings_item_subtitle_daily_word_goal),
                            onClickWithBounds = { bounds ->
                                activeMenuType = "dailyGoal"
                                popupMenuState = NemoPopupMenuState(
                                    isVisible = true,
                                    anchorBounds = bounds
                                )
                            },
                            showDivider = true,
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.settings_format_count_piece, uiState.dailyGoal),
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
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            iconColor = SystemColors.Green,
                            title = stringResource(R.string.settings_item_title_daily_grammar_goal),
                            subtitle = stringResource(R.string.settings_item_subtitle_daily_grammar_goal),
                            onClickWithBounds = { bounds ->
                                activeMenuType = "grammarGoal"
                                popupMenuState = NemoPopupMenuState(
                                    isVisible = true,
                                    anchorBounds = bounds
                                )
                            },
                            showDivider = true,
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.settings_format_count_item, uiState.grammarDailyGoal),
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
                            icon = Icons.Rounded.Timer,
                            iconColor = SystemColors.Mint,
                            title = stringResource(R.string.settings_item_title_day_reset),
                            subtitle = stringResource(R.string.settings_item_subtitle_day_reset),
                            onClickWithBounds = { bounds ->
                                activeMenuType = "resetHour"
                                popupMenuState = NemoPopupMenuState(
                                    isVisible = true,
                                    anchorBounds = bounds
                                )
                            },
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
                            iconColor = SystemColors.Teal,
                            title = stringResource(R.string.settings_item_title_random_extract),
                            subtitle = if (uiState.isRandomNewContentEnabled) stringResource(R.string.settings_item_subtitle_random_extract_on) else stringResource(R.string.settings_item_subtitle_random_extract_off),
                            onClick = { showRandomNewContentConfirmDialog = true },
                            showDivider = true,
                            trailing = {
                                LiquidToggle(
                                    checked = uiState.isRandomNewContentEnabled,
                                    onCheckedChange = { next ->
                                        if (next != uiState.isRandomNewContentEnabled) {
                                            showRandomNewContentConfirmDialog = true
                                        }
                                    },
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    resetSignal = randomNewContentResetSignal
                                )
                            }
                        )

                        SquircleSettingItem(
                            icon = Icons.Rounded.Memory,
                            iconColor = SystemColors.Purple,
                            title = stringResource(R.string.settings_item_title_algorithm),
                            subtitle = stringResource(R.string.settings_item_subtitle_algorithm),
                            onClick = onNavigateToAdvancedLearning,
                            showDivider = true,
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
                            icon = Icons.Rounded.SmartToy,
                            iconColor = SystemColors.Blue,
                            title = stringResource(R.string.settings_item_title_ai_workshop),
                            subtitle = stringResource(R.string.settings_item_subtitle_ai_workshop),
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
                    SettingsSectionTitle(stringResource(R.string.settings_section_title_voice), modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp))
                    PremiumCard {
                        SquircleSettingItem(
                            icon = Icons.AutoMirrored.Rounded.VolumeUp,
                            iconColor = SystemColors.Pink,
                            title = stringResource(R.string.settings_item_title_tts),
                            subtitle = stringResource(R.string.settings_item_subtitle_tts),
                            onClick = onNavigateToTtsSettings,
                            showDivider = false
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 数据
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_section_title_data), modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp))
                    PremiumCard {
                        SquircleSettingItem(
                            icon = Icons.Rounded.FileDownload,
                            iconColor = SystemColors.Green,
                            title = stringResource(R.string.settings_item_title_export),
                            subtitle = stringResource(R.string.settings_item_subtitle_export),
                            onClick = {
                                viewModel.onEvent(SettingsEvent.RequestExport)
                            },
                            showDivider = true
                        )
                        SquircleSettingItem(
                            icon = Icons.Rounded.FileUpload,
                            iconColor = SystemColors.Orange,
                            title = stringResource(R.string.settings_item_title_import),
                            subtitle = stringResource(R.string.settings_item_subtitle_import),
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/*")) },
                            showDivider = true
                        )

                        SquircleSettingItem(
                            icon = Icons.Rounded.Delete,
                            iconColor = SystemColors.Red,
                            title = stringResource(R.string.settings_item_title_reset),
                            subtitle = stringResource(R.string.settings_item_subtitle_reset),
                            onClick = {
                                isResetting = false
                                showConfirmDialog = true
                            },
                            showDivider = false
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 实验室
                val isLabAccessible = currentUser?.email?.equals("a.jian0629@gmail.com", ignoreCase = true) == true
                if (isLabAccessible) {
                    item {
                        SettingsSectionTitle(stringResource(R.string.settings_section_title_lab), modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp))
                        PremiumCard {
                            SquircleSettingItem(
                                icon = Icons.Rounded.Science,
                                iconColor = SystemColors.Cyan,
                                title = stringResource(R.string.settings_item_title_liquid_lab),
                                subtitle = stringResource(R.string.settings_item_subtitle_liquid_lab),
                                onClick = onNavigateToLiquidLab,
                                showDivider = true
                            )
                            SquircleSettingItem(
                                icon = Icons.Rounded.Category,
                                iconColor = SystemColors.Indigo,
                                title = stringResource(R.string.settings_item_title_corner_lab),
                                subtitle = stringResource(R.string.settings_item_subtitle_corner_lab),
                                onClick = onNavigateToCornerLab,
                                showDivider = false
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // 关于
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_section_title_about), modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp))
                    PremiumCard {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val versionName = remember { com.jian.nemo.core.ui.util.AppUtils.getVersionName(context) }

                        SquircleSettingItem(
                            icon = Icons.Rounded.Info,
                            iconColor = SystemColors.Gray,
                            title = stringResource(R.string.settings_about_title_screen),
                            subtitle = stringResource(R.string.settings_format_about_subtitle, versionName),
                            onClick = onNavigateToAbout,
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
                }
            }


            NemoSnackbar(
                visible = snackbarVisible,
                message = snackbarMessage,
                type = snackbarType,
                autoDismissMs = 2000L,
                onDismiss = { snackbarVisible = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarHeight + 8.dp)
            )

            // 液态磨砂气泡悬浮菜单 (就地锚定、贝塞尔明度映射、物理折射、微透分割线)
            NemoPopupMenu(
                state = popupMenuState.copy(items = currentMenuItems),
                backdrop = backdrop,
                onDismiss = {
                    popupMenuState = popupMenuState.copy(isVisible = false)
                    activeMenuType = ""
                }
            )
        }
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

    if (uiState.restorePreview != null) {
        RestorePreviewDialog(
            preview = uiState.restorePreview!!,
            onConfirm = { viewModel.onEvent(SettingsEvent.ConfirmRestore) },
            onDismiss = { viewModel.onEvent(SettingsEvent.CancelRestorePreview) }
        )
    }

    if (showRandomNewContentConfirmDialog) {
        val willEnable = !uiState.isRandomNewContentEnabled
        NemoDialog(
            onDismissRequest = {
                showRandomNewContentConfirmDialog = false
                randomNewContentResetSignal++
            },
            title = if (willEnable) stringResource(R.string.settings_dialog_title_random_on) else stringResource(R.string.settings_dialog_title_random_off),
            text = if (willEnable) {
                stringResource(R.string.settings_dialog_msg_random_on)
            } else {
                stringResource(R.string.settings_dialog_msg_random_off)
            },
            confirmText = stringResource(R.string.settings_btn_confirm),
            dismissText = stringResource(R.string.settings_btn_cancel),
            onConfirm = {
                viewModel.onEvent(SettingsEvent.SetRandomNewContentEnabled(willEnable))
                showRandomNewContentConfirmDialog = false
            },
            onDismiss = {
                showRandomNewContentConfirmDialog = false
                randomNewContentResetSignal++
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
            size = 42.dp,
            shape = RoundedRectangle(12.dp),
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



