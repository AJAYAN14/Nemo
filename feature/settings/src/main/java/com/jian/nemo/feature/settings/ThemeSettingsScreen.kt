package com.jian.nemo.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoRoundCheckbox
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import com.jian.nemo.core.ui.component.NemoDialog
import java.time.LocalTime
import com.jian.nemo.core.designsystem.theme.NemoIndigo
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.AppIconStyle
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.jian.nemo.core.designsystem.R as DesignR
import com.jian.nemo.feature.settings.components.IsoluminantPalette
import com.jian.nemo.feature.settings.components.LocalSettingIconStyle
import com.jian.nemo.feature.settings.components.PlumpIcon
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import com.jian.nemo.feature.settings.components.SquircleSettingItem

import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val backgroundColor = MaterialTheme.colorScheme.background

    // 计算实际生效的深浅色与自动模式状态
    val isSystemInDark = isSystemInDarkTheme()
    val isAuto = uiState.darkMode == DarkModeOption.AUTO
    val isEffectiveDark = when (uiState.darkMode) {
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
        DarkModeOption.AUTO -> {
            if (uiState.darkModeStrategy == DarkModeStrategy.FOLLOW_SYSTEM) {
                isSystemInDark
            } else {
                try {
                    val now = LocalTime.now()
                    val start = LocalTime.parse(uiState.darkModeStartTime)
                    val end = LocalTime.parse(uiState.darkModeEndTime)
                    if (start.isBefore(end)) {
                        now.isAfter(start) && now.isBefore(end)
                    } else {
                        now.isAfter(start) || now.isBefore(end)
                    }
                } catch (_: Exception) {
                    isSystemInDark
                }
            }
        }
    }

    // 弹窗状态
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }
    var showIconStyleDialog by remember { mutableStateOf(false) }
    var snackbarVisible by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var pendingAppIconStyle by remember { mutableStateOf<AppIconStyle?>(null) }

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 1. 底层滚动内容（被 backdrop 采样源录制）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .verticalScroll(scrollState)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = statusBarTop + 60.dp + 16.dp,
                        bottom = navBarBottom + 24.dp
                    )
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                SettingsSectionTitle(
                    text = "显示模式",
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                )
                PremiumCard {
                    Column {
                        ThemeSelectionGrid(
                            selectedMode = if (isAuto) {
                                if (isEffectiveDark) DarkModeOption.DARK else DarkModeOption.LIGHT
                            } else {
                                uiState.darkMode
                            },
                            onModeSelected = { mode ->
                                viewModel.onEvent(SettingsEvent.SetDarkMode(mode))
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val nextAuto = !isAuto
                                    if (nextAuto) {
                                        viewModel.onEvent(SettingsEvent.SetDarkMode(DarkModeOption.AUTO))
                                    } else {
                                        viewModel.onEvent(
                                            SettingsEvent.SetDarkMode(
                                                if (isEffectiveDark) DarkModeOption.DARK else DarkModeOption.LIGHT
                                            )
                                        )
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "自动",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "根据设置在浅色与深色外观间自动切换",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            LiquidToggle(
                                checked = isAuto,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        viewModel.onEvent(SettingsEvent.SetDarkMode(DarkModeOption.AUTO))
                                    } else {
                                        viewModel.onEvent(
                                            SettingsEvent.SetDarkMode(
                                                if (isEffectiveDark) DarkModeOption.DARK else DarkModeOption.LIGHT
                                            )
                                        )
                                    }
                                },
                                accentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.darkMode == DarkModeOption.AUTO,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(20.dp))
                        SettingsSectionTitle(
                            text = "自动切换模式",
                            modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                        )
                        PremiumCard {
                            StrategyOptionItem(
                                title = "跟随系统",
                                subtitle = "根据系统设置自动切换深浅色主题",
                                isSelected = uiState.darkModeStrategy == DarkModeStrategy.FOLLOW_SYSTEM,
                                onClick = { viewModel.onEvent(SettingsEvent.SetDarkModeStrategy(DarkModeStrategy.FOLLOW_SYSTEM)) }
                            )
                            StrategyOptionItem(
                                title = "定时深色模式",
                                subtitle = "在指定时间段开启深色模式",
                                isSelected = uiState.darkModeStrategy == DarkModeStrategy.SCHEDULED,
                                showDivider = false,
                                onClick = { viewModel.onEvent(SettingsEvent.SetDarkModeStrategy(DarkModeStrategy.SCHEDULED)) }
                            )
                        }

                        AnimatedVisibility(
                            visible = uiState.darkModeStrategy == DarkModeStrategy.SCHEDULED,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(20.dp))
                                SettingsSectionTitle(
                                    text = "深色模式定时",
                                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                                )
                                PremiumCard {
                                    SquircleSettingItem(
                                        icon = Icons.Rounded.DarkMode,
                                        plumpIcon = PlumpIcon.Moon,
                                        iconColor = Color(0xFF5856D6),
                                        plumpColor = IsoluminantPalette.LilacViolet,
                                        title = "开启深色模式",
                                        subtitle = uiState.darkModeStartTime,
                                        onClick = { showStartTimePicker = true },
                                        trailing = {
                                            Text(
                                                text = uiState.darkModeStartTime,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    )
                                    SquircleSettingItem(
                                        icon = Icons.Rounded.LightMode,
                                        plumpIcon = PlumpIcon.Sun,
                                        iconColor = Color(0xFFFF9500),
                                        plumpColor = IsoluminantPalette.WarmAmber,
                                        title = "结束深色模式",
                                        subtitle = uiState.darkModeEndTime,
                                        showDivider = false,
                                        onClick = { showEndTimePicker = true },
                                        trailing = {
                                            Text(
                                                text = uiState.darkModeEndTime,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SettingsSectionTitle(
                    text = "个性化",
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                )
                PremiumCard {
                    SquircleSettingItem(
                        icon = Icons.Rounded.Palette,
                        plumpIcon = PlumpIcon.ThemePalette,
                        iconColor = Color(0xFFFF2D55), // MD3 原版粉红
                        plumpColor = IsoluminantPalette.RoseRuby, // Plump 规范等亮度蔷薇绯红
                        title = "主题色彩",
                        subtitle = "选择您喜欢的品牌配色",
                        onClick = { showThemeColorDialog = true },
                        showDivider = true
                    )
                    SquircleSettingItem(
                        icon = Icons.Rounded.Category,
                        plumpIcon = PlumpIcon.IconStyle,
                        iconColor = NemoIndigo,
                        plumpColor = IsoluminantPalette.SkyIndigo,
                        title = "图标风格",
                        subtitle = when (uiState.settingIconStyle) {
                            SettingIconStyle.MD3 -> "Material Design 3 (圆角卡片)"
                            SettingIconStyle.PLUMP_DUO -> "Plump Duo (双色通透)"
                        },
                        onClick = { showIconStyleDialog = true },
                        showDivider = false
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                SettingsSectionTitle(
                    text = "桌面图标",
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                )
                PremiumCard {
                    AppIconSelectionGrid(
                        selectedStyle = uiState.appIconStyle,
                        onStyleSelected = { style ->
                            if (uiState.appIconStyle != style) {
                                pendingAppIconStyle = style
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.95f),
                            backgroundColor.copy(alpha = 0.75f),
                            backgroundColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 8.dp)
        ) {
            CommonHeader(
                title = "主题外观",
                onBack = onBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp
            )
        }

        // 3. 通用顶部通知
        NemoSnackbar(
            visible = snackbarVisible,
            message = snackbarMessage,
            type = NemoSnackbarType.SUCCESS,
            autoDismissMs = 2000L,
            onDismiss = { snackbarVisible = false },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarTop + 8.dp)
        )
    }
}

    if (showIconStyleDialog) {
        IconStyleSelectionDialog(
            currentStyle = uiState.settingIconStyle,
            onDismiss = { showIconStyleDialog = false },
            onStyleSelect = { style ->
                viewModel.onEvent(SettingsEvent.SetSettingIconStyle(style))
                showIconStyleDialog = false
            }
        )
    }

    if (pendingAppIconStyle != null) {
        val targetStyle = pendingAppIconStyle!!
        NemoDialog(
            onDismissRequest = { pendingAppIconStyle = null },
            title = "更换桌面图标",
            confirmText = "确认更换",
            dismissText = "取消",
            onConfirm = {
                pendingAppIconStyle = null
                viewModel.onEvent(SettingsEvent.SetAppIconStyle(targetStyle))
            },
            content = {
                Text(
                    text = "更换桌面图标需要刷新系统快捷方式，应用将自动返回手机桌面以完成生效。\n\n是否确认更换？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        )
    }

    if (showThemeColorDialog) {
        com.jian.nemo.feature.settings.components.ThemeColorDialog(
            currentColor = uiState.themeColor,
            onDismiss = { showThemeColorDialog = false },
            onColorSelect = { color ->
                // 默认蓝 0xFF0E68FF 传 null 以恢复默认
                val defaultBlue = 0xFF0E68FF.toULong()
                val colorArgb = if (color.value == defaultBlue) null else color.value.toLong()
                viewModel.onEvent(SettingsEvent.SetThemeColor(colorArgb))
                showThemeColorDialog = false
            }
        )
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = uiState.darkModeStartTime,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { time ->
                viewModel.onEvent(SettingsEvent.SetDarkModeStartTime(time))
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = uiState.darkModeEndTime,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { time ->
                viewModel.onEvent(SettingsEvent.SetDarkModeEndTime(time))
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun ThemeSelectionGrid(
    selectedMode: DarkModeOption,
    onModeSelected: (DarkModeOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeOption(
            mode = DarkModeOption.LIGHT,
            label = "浅色",
            isSelected = selectedMode == DarkModeOption.LIGHT,
            onClick = { onModeSelected(DarkModeOption.LIGHT) },
            modifier = Modifier.weight(1f)
        )
        ThemeOption(
            mode = DarkModeOption.DARK,
            label = "深色",
            isSelected = selectedMode == DarkModeOption.DARK,
            onClick = { onModeSelected(DarkModeOption.DARK) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ThemeOption(
    mode: DarkModeOption,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        DeviceMockup(mode = mode, isSelected = isSelected)
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun DeviceMockup(
    mode: DarkModeOption,
    isSelected: Boolean
) {
    val borderColor = when (mode) {
        DarkModeOption.DARK -> Color(0xFF3A3A3C)
        else -> Color(0xFFE5E5EA)
    }

    val mockupBackground = when (mode) {
        DarkModeOption.DARK -> Modifier.background(Color(0xFF1C1C1E))
        else -> Modifier.background(Color.White)
    }

    val mockupWidth = 78.dp
    val mockupHeight = 156.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(6.dp) // 为外部轮廓留出呼吸空间
    ) {
        // 外部选中轮廓
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = mockupWidth + 10.dp, height = mockupHeight + 10.dp)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(18.dp)
                    )
            )
        }

        // 手机本体
        Box(
            modifier = Modifier
                .size(mockupWidth, mockupHeight)
                .clip(RoundedCornerShape(14.dp))
                .then(mockupBackground)
                .border(
                    width = 1.5.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(top = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // UI 元素占位
                MockupNav(mode)
                Spacer(modifier = Modifier.height(14.dp))
                MockupCard(mode)
                Spacer(modifier = Modifier.height(10.dp))
                MockupLine(mode, isShort = false)
                MockupLine(mode, isShort = true)
            }
        }
    }
}

@Composable
private fun MockupNav(mode: DarkModeOption) {
    val modifier = when (mode) {
        DarkModeOption.DARK -> Modifier.background(Color(0xFF3A3A3C))
        else -> Modifier.background(Color(0xFFE5E5EA))
    }
    Box(
        modifier = Modifier
            .size(38.dp, 6.dp)
            .clip(RoundedCornerShape(3.dp))
            .then(modifier)
    )
}

@Composable
private fun MockupCard(mode: DarkModeOption) {
    val modifier = when (mode) {
        DarkModeOption.DARK -> Modifier.background(Color(0xFF2C2C2E))
        else -> Modifier.background(Color(0xFFF2F2F7))
    }
    Box(
        modifier = Modifier
            .size(56.dp, 32.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(modifier)
    )
}

@Composable
private fun ColumnScope.MockupLine(mode: DarkModeOption, isShort: Boolean) {
    val modifier = when (mode) {
        DarkModeOption.DARK -> Modifier.background(Color(0xFF3A3A3C))
        else -> Modifier.background(Color(0xFFE5E5EA))
    }
    Box(
        modifier = Modifier
            .padding(start = 11.dp, bottom = 6.dp)
            .align(Alignment.Start)
            .size(if (isShort) 26.dp else 44.dp, 5.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .then(modifier)
    )
}

@Composable
private fun StrategyOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        NemoRoundCheckbox(
            checked = isSelected,
            size = 20.dp,
            checkSize = 13.dp
        )
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 7
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary
    val neutralCardBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val textPrimary = if (isDark) Color.White else Color(0xFF1C1C1E)

    NemoDialog(
        onDismissRequest = onDismiss,
        title = "选择时间",
        confirmText = "确定",
        dismissText = "取消",
        onConfirm = {
            val formattedTime = "${timePickerState.hour.toString().padStart(2, '0')}:${timePickerState.minute.toString().padStart(2, '0')}"
            onTimeSelected(formattedTime)
        },
        content = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = neutralCardBg,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = textPrimary,
                        selectorColor = primaryColor,
                        timeSelectorSelectedContainerColor = primaryColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                        timeSelectorUnselectedContainerColor = neutralCardBg,
                        timeSelectorSelectedContentColor = primaryColor,
                        timeSelectorUnselectedContentColor = textPrimary,
                        containerColor = Color.Transparent
                    )
                )
            }
        }
    )
}

@Composable
private fun IconStyleSelectionDialog(
    currentStyle: SettingIconStyle,
    onDismiss: () -> Unit,
    onStyleSelect: (SettingIconStyle) -> Unit
) {
    var selectedStyle by remember { mutableStateOf(currentStyle) }

    NemoDialog(
        onDismissRequest = onDismiss,
        title = "选择图标风格",
        confirmText = "确定",
        dismissText = "取消",
        onConfirm = { onStyleSelect(selectedStyle) },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconStyleOptionRow(
                    title = "Material Design 3",
                    description = "经典圆角彩色背景底框，单色聚焦图标",
                    isSelected = selectedStyle == SettingIconStyle.MD3,
                    onClick = { selectedStyle = SettingIconStyle.MD3 }
                )
                IconStyleOptionRow(
                    title = "Plump Duo",
                    description = "现代无背景通透设计，双色线面与等亮度调色板",
                    isSelected = selectedStyle == SettingIconStyle.PLUMP_DUO,
                    onClick = { selectedStyle = SettingIconStyle.PLUMP_DUO }
                )
            }
        }
    )
}

@Composable
private fun IconStyleOptionRow(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        NemoRoundCheckbox(
            checked = isSelected,
            size = 20.dp,
            checkSize = 13.dp
        )
    }
}

@Preview(name = "StrategyOptionItem Preview", showBackground = true)
@Composable
private fun StrategyOptionItemPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            StrategyOptionItem(
                title = "跟随系统",
                subtitle = "根据系统设置自动切换深浅色主题",
                isSelected = true,
                onClick = {}
            )
            StrategyOptionItem(
                title = "定时深色模式",
                subtitle = "在指定时间段开启深色模式",
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(name = "StrategyOptionItem Preview - Dark Mode", showBackground = true)
@Composable
private fun StrategyOptionItemDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                StrategyOptionItem(
                    title = "跟随系统",
                    subtitle = "根据系统设置自动切换深浅色主题",
                    isSelected = true,
                    onClick = {}
                )
                StrategyOptionItem(
                    title = "定时深色模式",
                    subtitle = "在指定时间段开启深色模式",
                    isSelected = false,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "IconStyleOptionRow Preview", showBackground = true)
@Composable
private fun IconStyleOptionRowPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            IconStyleOptionRow(
                title = "Material Design 3",
                description = "经典圆角彩色背景底框，单色聚焦图标",
                isSelected = true,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            IconStyleOptionRow(
                title = "Plump Duo",
                description = "现代无背景通透设计，双色线面与等亮度调色板",
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(name = "IconStyleOptionRow Preview - Dark Mode", showBackground = true)
@Composable
private fun IconStyleOptionRowDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                IconStyleOptionRow(
                    title = "Material Design 3",
                    description = "经典圆角彩色背景底框，单色聚焦图标",
                    isSelected = true,
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(8.dp))
                IconStyleOptionRow(
                    title = "Plump Duo",
                    description = "现代无背景通透设计，双色线面与等亮度调色板",
                    isSelected = false,
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "ThemeSelectionCard Preview - Light", showBackground = true)
@Composable
private fun ThemeSelectionCardLightPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            PremiumCard {
                Column {
                    ThemeSelectionGrid(
                        selectedMode = DarkModeOption.LIGHT,
                        onModeSelected = {}
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        thickness = 0.5.dp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "自动",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "根据设置在浅色与深色外观间自动切换",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        LiquidToggle(
                            checked = true,
                            onCheckedChange = {},
                            accentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIconSelectionGrid(
    selectedStyle: AppIconStyle,
    onStyleSelected: (AppIconStyle) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconOption(
            drawableRes = DesignR.drawable.ic_launcher_default_foreground,
            isSelected = selectedStyle == AppIconStyle.DEFAULT,
            onClick = { onStyleSelected(AppIconStyle.DEFAULT) }
        )
        AppIconOption(
            drawableRes = DesignR.drawable.ic_launcher_monochrome_foreground,
            isSelected = selectedStyle == AppIconStyle.MONOCHROME,
            onClick = { onStyleSelected(AppIconStyle.MONOCHROME) }
        )
    }
}

@Composable
private fun AppIconOption(
    @androidx.annotation.DrawableRes drawableRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // 整个图标选择项：高度紧凑轻薄，无多余灰色大盒子
    Box(
        modifier = Modifier
            .clickable(
                onClick = onClick,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // 贴合图标外缘的紧凑选中高亮环
        Box(
            modifier = Modifier
                .size(72.dp)
                .border(
                    width = if (isSelected) 2.5.dp else 0.dp,
                    color = if (isSelected) primaryColor else Color.Transparent,
                    shape = RoundedCornerShape(19.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // 图标容器：64dp 尺寸，通过将内部图像居中放大 1.45 倍，使主体海马充盈饱满，消除多余空白
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(
                        width = 0.5.dp,
                        color = Color.Black.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(scaleX = 1.5f, scaleY = 1.5f)
                )
            }
        }

        // 选中时在右上角精致呈现的对勾微标
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(19.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(primaryColor)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}


