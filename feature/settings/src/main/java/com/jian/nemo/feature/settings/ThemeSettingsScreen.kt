package com.jian.nemo.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import com.jian.nemo.feature.settings.components.SquircleSettingItem
import com.jian.nemo.feature.settings.components.AppIconDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // 弹窗状态
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }
    var showAppIconDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CommonHeader(
                title = "主题外观",
                onBack = onBack,
                backgroundColor = MaterialTheme.colorScheme.background
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SettingsSectionTitle(text = "显示模式")
            ThemeSelectionGrid(
                selectedMode = uiState.darkMode,
                onModeSelected = { viewModel.onEvent(SettingsEvent.SetDarkMode(it)) }
            )

            AnimatedVisibility(
                visible = uiState.darkMode == DarkModeOption.AUTO,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    SettingsSectionTitle(text = "自动切换模式")
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
                            SettingsSectionTitle(text = "深色模式定时")
                            PremiumCard {
                                SquircleSettingItem(
                                    icon = Icons.Rounded.DarkMode,
                                    iconColor = Color(0xFF5856D6),
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
                                    iconColor = Color(0xFFFF9500),
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

            SettingsSectionTitle(text = "个性化")
            PremiumCard {
                SquircleSettingItem(
                    icon = Icons.Rounded.Palette,
                    iconColor = Color(0xFFFF2D55), // Pink/Red
                    title = "主题色彩",
                    subtitle = "选择您喜欢的品牌配色",
                    onClick = { showThemeColorDialog = true },
                    showDivider = true
                )
                SquircleSettingItem(
                    icon = Icons.Rounded.AppShortcut,
                    iconColor = Color(0xFF007AFF), // Blue
                    title = "应用图标",
                    subtitle = "当前: ${uiState.appIcon}",
                    onClick = { showAppIconDialog = true },
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
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

    if (showAppIconDialog) {
        AppIconDialog(
            currentIcon = uiState.appIcon,
            onDismiss = { showAppIconDialog = false },
            onIconSelect = { name ->
                viewModel.onEvent(SettingsEvent.SetAppIcon(name))
                showAppIconDialog = false
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
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        ThemeOption(
            mode = DarkModeOption.LIGHT,
            label = "浅色",
            isSelected = selectedMode == DarkModeOption.LIGHT,
            onClick = { onModeSelected(DarkModeOption.LIGHT) }
        )
        ThemeOption(
            mode = DarkModeOption.DARK,
            label = "深色",
            isSelected = selectedMode == DarkModeOption.DARK,
            onClick = { onModeSelected(DarkModeOption.DARK) }
        )
        ThemeOption(
            mode = DarkModeOption.AUTO,
            label = "自动",
            isSelected = selectedMode == DarkModeOption.AUTO,
            onClick = { onModeSelected(DarkModeOption.AUTO) }
        )
    }
}

@Composable
private fun ThemeOption(
    mode: DarkModeOption,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        DeviceMockup(mode = mode, isSelected = isSelected)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 选中指示器 (圆圈打勾)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary 
                    else Color.Transparent
                )
                .border(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFC7C7CC),
                    shape = RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceMockup(
    mode: DarkModeOption,
    isSelected: Boolean
) {
    val borderColor = when (mode) {
        DarkModeOption.LIGHT -> Color(0xFFC7C7CC)
        DarkModeOption.DARK -> Color(0xFF5C5C5E)
        DarkModeOption.AUTO -> Color(0xFFC7C7CC)
    }

    val mockupBackground = when (mode) {
        DarkModeOption.LIGHT -> Modifier.background(Color.White)
        DarkModeOption.DARK -> Modifier.background(Color(0xFF1C1C1E))
        DarkModeOption.AUTO -> Modifier.background(
            brush = Brush.linearGradient(
                0.0f to Color.White,
                0.48f to Color.White,
                0.52f to Color(0xFF1C1C1E),
                1.0f to Color(0xFF1C1C1E),
                start = Offset(0f, 0f),
                end = Offset.Infinite
            )
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(6.dp) // 为外部轮廓留出呼吸空间
    ) {
        // 外部选中轮廓 (还原 HTML 的 outline 效果)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(width = 72.dp + 10.dp, height = 150.dp + 10.dp)
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
                .size(72.dp, 150.dp)
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
                Spacer(modifier = Modifier.height(12.dp))
                MockupCard(mode)
                Spacer(modifier = Modifier.height(8.dp))
                MockupLine(mode, isShort = false)
                MockupLine(mode, isShort = true)
            }
        }
    }
}

@Composable
private fun MockupNav(mode: DarkModeOption) {
    val color = when (mode) {
        DarkModeOption.LIGHT -> Color(0xFFE5E5EA)
        DarkModeOption.DARK -> Color(0xFF3A3A3C)
        DarkModeOption.AUTO -> Color(0xFF8E8E93).copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .size(36.dp, 6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
    )
}

@Composable
private fun MockupCard(mode: DarkModeOption) {
    val color = when (mode) {
        DarkModeOption.LIGHT -> Color(0xFFF2F2F7)
        DarkModeOption.DARK -> Color(0xFF2C2C2E)
        DarkModeOption.AUTO -> Color(0xFF8E8E93).copy(alpha = 0.3f)
    }
    Box(
        modifier = Modifier
            .size(52.dp, 30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(color)
    )
}

@Composable
private fun ColumnScope.MockupLine(mode: DarkModeOption, isShort: Boolean) {
    val color = when (mode) {
        DarkModeOption.LIGHT -> Color(0xFFE5E5EA)
        DarkModeOption.DARK -> Color(0xFF3A3A3C)
        DarkModeOption.AUTO -> Color(0xFF8E8E93).copy(alpha = 0.4f)
    }
    Box(
        modifier = Modifier
            .padding(start = 10.dp, bottom = 6.dp)
            .align(Alignment.Start)
            .size(if (isShort) 24.dp else 40.dp, 5.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(color)
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
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val formattedTime = "${timePickerState.hour.toString().padStart(2, '0')}:${timePickerState.minute.toString().padStart(2, '0')}"
                onTimeSelected(formattedTime)
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
