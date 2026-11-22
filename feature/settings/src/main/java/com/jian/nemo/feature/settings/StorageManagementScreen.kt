package com.jian.nemo.feature.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.feature.settings.components.LocalSettingIconStyle
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 存储与内存管理界面
 *
 * 展示应用本体体积、用户数据、缓存空间分布与当前进程运行内存，
 * 并提供与系统「清除缓存」一致的安全清理功能。
 */
@Composable
fun StorageManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: StorageManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backdrop = rememberLayerBackdrop()
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 语义调色盘（深浅模式光学补偿变体）
    val appColor = if (isDark) SystemColors.BlueDark else SystemColors.Blue
    val cyanColor = if (isDark) SystemColors.CyanDark else SystemColors.Cyan
    val tealColor = if (isDark) SystemColors.TealDark else SystemColors.Teal
    val mintColor = if (isDark) SystemColors.MintDark else SystemColors.Mint
    val userDataColor = if (isDark) SystemColors.GreenDark else SystemColors.Green
    val yellowColor = if (isDark) SystemColors.YellowDark else SystemColors.Yellow
    val cacheColor = if (isDark) SystemColors.OrangeDark else SystemColors.Orange
    val memoryColor = if (isDark) SystemColors.PurpleDark else SystemColors.Purple
    val nativeMemColor = tealColor
    val otherMemColor = if (isDark) SystemColors.IndigoDark else SystemColors.Indigo
    val dangerColor = if (isDark) SystemColors.RedDark else SystemColors.Red
    val trackColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.background
    }
    val nestedPillColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    var snackbarVisible by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarType by remember { mutableStateOf(NemoSnackbarType.SUCCESS) }

    LaunchedEffect(uiState.feedbackMessage) {
        val message = uiState.feedbackMessage ?: return@LaunchedEffect
        snackbarMessage = message
        snackbarType = if (uiState.isFeedbackError) NemoSnackbarType.ERROR else NemoSnackbarType.SUCCESS
        snackbarVisible = true
        viewModel.consumeFeedback()
    }

    val snapshot = uiState.snapshot
    val calculatingText = stringResource(R.string.settings_storage_label_calculating)
    val formatBytes: (Long) -> String = { bytes ->
        if (uiState.isLoading) calculatingText else viewModel.formatSize(bytes)
    }

    val refreshTransition = rememberInfiniteTransition(label = "storageRefreshSpin")
    val refreshAngle by refreshTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "storageRefreshAngle"
    )

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
                    top = statusBarTop + 60.dp + 16.dp,
                    bottom = navBarBottom + 24.dp
                )
            ) {
                // 1. 空间总览卡片
                item {
                    SettingsSectionTitle(
                        text = stringResource(R.string.settings_storage_section_overview),
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                    )
                    StorageOverviewCard(
                        totalSizeText = formatBytes(snapshot.totalStorageBytes),
                        appSizeBytes = snapshot.appSizeBytes,
                        userDataBytes = snapshot.userDataBytes,
                        cacheBytes = snapshot.totalCacheBytes,
                        appSizeText = formatBytes(snapshot.appSizeBytes),
                        userDataText = formatBytes(snapshot.userDataBytes),
                        cacheSizeText = formatBytes(snapshot.totalCacheBytes),
                        appColor = appColor,
                        cyanColor = cyanColor,
                        tealColor = tealColor,
                        mintColor = mintColor,
                        userDataColor = userDataColor,
                        yellowColor = yellowColor,
                        cacheColor = cacheColor,
                        trackColor = trackColor
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 2. 存储分类（三张主次分明的独立卡片）
                item {
                    SettingsSectionTitle(
                        text = stringResource(R.string.settings_storage_section_breakdown),
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                    )

                    // 主操作焦点卡：可清理缓存（含三列胶囊明细与一键清除按钮）
                    CacheFocusCard(
                        totalCacheBytes = snapshot.totalCacheBytes,
                        totalCacheText = formatBytes(snapshot.totalCacheBytes),
                        internalCacheText = formatBytes(snapshot.internalCacheBytes),
                        externalCacheText = formatBytes(snapshot.externalCacheBytes),
                        codeCacheText = formatBytes(snapshot.codeCacheBytes),
                        isLoading = uiState.isLoading,
                        isClearing = uiState.isClearingCache,
                        accentColor = cacheColor,
                        secondaryDotColor = yellowColor,
                        tertiaryDotColor = cyanColor,
                        pillContainerColor = nestedPillColor,
                        onClearClick = { showClearCacheDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 核心资产卡：用户数据（带受保护徽标与轻底块全宽明细）
                    UserDataCard(
                        totalUserDataText = formatBytes(snapshot.userDataBytes),
                        subItems = listOf(
                            stringResource(R.string.settings_storage_sub_database) to formatBytes(snapshot.databaseBytes),
                            stringResource(R.string.settings_storage_sub_preferences) to formatBytes(snapshot.preferencesBytes),
                            stringResource(R.string.settings_storage_sub_files) to formatBytes(snapshot.filesBytes + snapshot.otherUserDataBytes)
                        ),
                        accentColor = userDataColor,
                        nestedContainerColor = nestedPillColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 静态基线卡：应用体积（单行紧凑展示）
                    AppSizeCard(
                        appSizeText = formatBytes(snapshot.appSizeBytes),
                        accentColor = appColor
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 3. 运行内存卡片
                item {
                    SettingsSectionTitle(
                        text = stringResource(R.string.settings_storage_section_memory),
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
                    )
                    MemoryMonitorCard(
                        snapshot = snapshot,
                        isLoading = uiState.isLoading,
                        formatBytes = formatBytes,
                        memoryColor = memoryColor,
                        dalvikColor = appColor,
                        nativeColor = nativeMemColor,
                        otherColor = otherMemColor,
                        warningColor = cacheColor,
                        dangerColor = dangerColor,
                        trackColor = trackColor,
                        pillContainerColor = nestedPillColor
                    )
                }
            }

            // 悬浮顶栏与四阶垂直渐变遮罩（对齐 ActivityHeatmapScreen 黄金范式）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor.copy(alpha = 0.95f),
                                backgroundColor.copy(alpha = 0.75f),
                                backgroundColor.copy(alpha = 0.35f),
                                backgroundColor.copy(alpha = 0f)
                            )
                        )
                    )
                    .padding(bottom = 8.dp)
            ) {
                CommonHeader(
                    title = stringResource(R.string.settings_storage_title_screen),
                    onBack = onNavigateBack,
                    backgroundColor = Color.Transparent,
                    backdrop = backdrop,
                    backButtonSize = 48.dp,
                    actions = {
                        FrostedLiquidGlassButton(
                            onClick = { viewModel.refresh(showFeedback = true) },
                            backdrop = backdrop,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            isInteractive = true,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = com.jian.nemo.core.designsystem.R.drawable.ic_refresh_wght700),
                                contentDescription = stringResource(R.string.settings_storage_cd_refresh),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .size(30.dp)
                                    .graphicsLayer {
                                        rotationZ = if (uiState.isRefreshing) refreshAngle else 0f
                                    }
                            )
                        }
                    }
                )
            }

            NemoSnackbar(
                visible = snackbarVisible,
                message = snackbarMessage,
                type = snackbarType,
                autoDismissMs = 2200L,
                onDismiss = { snackbarVisible = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarTop + 8.dp)
            )
        }
    }

    if (showClearCacheDialog) {
        NemoDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = stringResource(R.string.settings_storage_dialog_clear_title),
            text = stringResource(
                R.string.settings_storage_dialog_clear_message,
                formatBytes(snapshot.totalCacheBytes)
            ),
            confirmText = stringResource(R.string.settings_storage_dialog_btn_confirm),
            dismissText = stringResource(R.string.settings_storage_dialog_btn_cancel),
            isDangerous = true,
            onConfirm = {
                showClearCacheDialog = false
                viewModel.clearCache()
            },
            onDismiss = { showClearCacheDialog = false }
        )
    }
}

@Composable
private fun StorageOverviewCard(
    totalSizeText: String,
    appSizeBytes: Long,
    userDataBytes: Long,
    cacheBytes: Long,
    appSizeText: String,
    userDataText: String,
    cacheSizeText: String,
    appColor: Color,
    cyanColor: Color,
    tealColor: Color,
    mintColor: Color,
    userDataColor: Color,
    yellowColor: Color,
    cacheColor: Color,
    trackColor: Color
) {
    val total = (appSizeBytes + userDataBytes + cacheBytes).coerceAtLeast(0L)
    val rawApp = if (total > 0L) appSizeBytes.toFloat() / total else 0f
    val rawData = if (total > 0L) userDataBytes.toFloat() / total else 0f
    val rawCache = if (total > 0L) cacheBytes.toFloat() / total else 0f

    val appWeight by animateFloatAsState(
        targetValue = if (appSizeBytes > 0L) rawApp.coerceAtLeast(0.03f) else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "appSegmentWeight"
    )
    val dataWeight by animateFloatAsState(
        targetValue = if (userDataBytes > 0L) rawData.coerceAtLeast(0.03f) else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "dataSegmentWeight"
    )
    val cacheWeight by animateFloatAsState(
        targetValue = if (cacheBytes > 0L) rawCache.coerceAtLeast(0.03f) else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "cacheSegmentWeight"
    )

    val gradientStops = buildCleanStorageGradientStops(
        appWeight = appWeight,
        dataWeight = dataWeight,
        cacheWeight = cacheWeight,
        blue = appColor,
        cyan = cyanColor,
        teal = tealColor,
        mint = mintColor,
        green = userDataColor,
        yellow = yellowColor,
        orange = cacheColor
    )

    PremiumCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
            Text(
                text = stringResource(R.string.settings_storage_label_total_footprint),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = totalSizeText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 沿色环邻近色连续渐变的存储占比条（通过青/薄荷与亮黄桥接过渡，避免线性插值穿过灰色死区）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedRectangle(6.dp))
                    .background(trackColor)
            ) {
                if (gradientStops.size >= 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(colorStops = gradientStops))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageLegendItem(
                    color = appColor,
                    label = stringResource(R.string.settings_storage_legend_app),
                    value = appSizeText,
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
                StorageLegendItem(
                    color = userDataColor,
                    label = stringResource(R.string.settings_storage_legend_data),
                    value = userDataText,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                )
                StorageLegendItem(
                    color = cacheColor,
                    label = stringResource(R.string.settings_storage_legend_cache),
                    value = cacheSizeText,
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StorageLegendItem(
    color: Color,
    label: String,
    value: String,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CacheFocusCard(
    totalCacheBytes: Long,
    totalCacheText: String,
    internalCacheText: String,
    externalCacheText: String,
    codeCacheText: String,
    isLoading: Boolean,
    isClearing: Boolean,
    accentColor: Color,
    secondaryDotColor: Color,
    tertiaryDotColor: Color,
    pillContainerColor: Color,
    onClearClick: () -> Unit
) {
    val canClear = !isLoading && !isClearing && totalCacheBytes > 0L
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && canClear) 0.98f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "clearCacheBtnScale"
    )

    val buttonBg = if (canClear) accentColor else pillContainerColor
    val buttonContentColor = if (canClear) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    }

    PremiumCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactDualTrackIcon(
                    icon = Icons.Rounded.Cached,
                    iconColor = accentColor
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_storage_item_cache_title),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_storage_item_cache_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = totalCacheText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemoryMetricPill(
                    dotColor = accentColor,
                    label = stringResource(R.string.settings_storage_sub_internal_cache),
                    value = internalCacheText,
                    containerColor = pillContainerColor,
                    modifier = Modifier.weight(1f)
                )
                MemoryMetricPill(
                    dotColor = secondaryDotColor,
                    label = stringResource(R.string.settings_storage_sub_external_cache),
                    value = externalCacheText,
                    containerColor = pillContainerColor,
                    modifier = Modifier.weight(1f)
                )
                MemoryMetricPill(
                    dotColor = tertiaryDotColor,
                    label = stringResource(R.string.settings_storage_sub_code_cache),
                    value = codeCacheText,
                    containerColor = pillContainerColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedRectangle(16.dp))
                    .background(buttonBg)
                    .clickable(
                        enabled = canClear,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClearClick
                    )
                    .padding(vertical = 13.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = buttonContentColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_storage_btn_clearing),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = buttonContentColor
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = null,
                        tint = buttonContentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (canClear) {
                            stringResource(R.string.settings_storage_format_btn_clear_cache, totalCacheText)
                        } else {
                            stringResource(R.string.settings_storage_btn_clear_cache)
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = buttonContentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (!isLoading && totalCacheBytes == 0L) {
                    stringResource(R.string.settings_storage_cache_empty_hint)
                } else {
                    stringResource(R.string.settings_storage_cache_safe_tip)
                },
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UserDataCard(
    totalUserDataText: String,
    subItems: List<Pair<String, String>>,
    accentColor: Color,
    nestedContainerColor: Color
) {
    PremiumCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactDualTrackIcon(
                    icon = Icons.Rounded.Folder,
                    iconColor = accentColor
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.settings_storage_item_user_data_title),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedRectangle(6.dp))
                                .background(accentColor.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_storage_badge_protected),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = accentColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_storage_item_user_data_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = totalUserDataText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedRectangle(14.dp))
                    .background(nestedContainerColor)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                subItems.forEachIndexed { index, (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (index < subItems.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSizeCard(
    appSizeText: String,
    accentColor: Color
) {
    PremiumCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactDualTrackIcon(
                icon = Icons.Rounded.Apps,
                iconColor = accentColor
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_storage_item_app_title),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.settings_storage_item_app_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = appSizeText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MemoryMonitorCard(
    snapshot: StorageMemorySnapshot,
    isLoading: Boolean,
    formatBytes: (Long) -> String,
    memoryColor: Color,
    dalvikColor: Color,
    nativeColor: Color,
    otherColor: Color,
    warningColor: Color,
    dangerColor: Color,
    trackColor: Color,
    pillContainerColor: Color
) {
    val heapFraction by animateFloatAsState(
        targetValue = if (isLoading) 0f else snapshot.jvmHeapUsageFraction,
        animationSpec = tween(durationMillis = 450),
        label = "jvmHeapFraction"
    )
    val heapBarColor = when {
        snapshot.jvmHeapUsageFraction >= 0.85f -> dangerColor
        snapshot.jvmHeapUsageFraction >= 0.65f -> warningColor
        else -> memoryColor
    }

    PremiumCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactDualTrackIcon(
                    icon = Icons.Rounded.Memory,
                    iconColor = memoryColor
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_storage_mem_pss_title),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.settings_storage_mem_pss_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = formatBytes(snapshot.totalPssBytes),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = memoryColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PSS 细项三列展示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemoryMetricPill(
                    dotColor = dalvikColor,
                    label = stringResource(R.string.settings_storage_mem_dalvik),
                    value = formatBytes(snapshot.dalvikPssBytes),
                    containerColor = pillContainerColor,
                    modifier = Modifier.weight(1f)
                )
                MemoryMetricPill(
                    dotColor = nativeColor,
                    label = stringResource(R.string.settings_storage_mem_native),
                    value = formatBytes(snapshot.nativePssBytes),
                    containerColor = pillContainerColor,
                    modifier = Modifier.weight(1f)
                )
                MemoryMetricPill(
                    dotColor = otherColor,
                    label = stringResource(R.string.settings_storage_mem_other),
                    value = formatBytes(snapshot.otherPssBytes),
                    containerColor = pillContainerColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // JVM 堆内存使用率
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_storage_mem_heap_title),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.settings_storage_mem_format_heap_ratio,
                        formatBytes(snapshot.jvmHeapUsedBytes),
                        formatBytes(snapshot.jvmHeapMaxBytes)
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedRectangle(4.dp))
                    .background(trackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(heapFraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedRectangle(4.dp))
                        .background(heapBarColor)
                )
            }

            if (snapshot.deviceTotalRamBytes > 0L) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_storage_mem_device_avail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_storage_mem_format_device_ratio,
                            formatBytes(snapshot.deviceAvailableRamBytes),
                            formatBytes(snapshot.deviceTotalRamBytes)
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryMetricPill(
    dotColor: Color,
    label: String,
    value: String,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedRectangle(14.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/**
 * 按三段真实占比生成色环连续的渐变色标，通过邻近过渡色绕开 RGB 直线灰区。
 */
private fun buildCleanStorageGradientStops(
    appWeight: Float,
    dataWeight: Float,
    cacheWeight: Float,
    blue: Color,
    cyan: Color,
    teal: Color,
    mint: Color,
    green: Color,
    yellow: Color,
    orange: Color
): Array<Pair<Float, Color>> {
    val weightSum = appWeight + dataWeight + cacheWeight
    if (weightSum <= 0.001f) return emptyArray()

    val appFrac = (appWeight / weightSum).coerceIn(0f, 1f)
    val dataFrac = (dataWeight / weightSum).coerceIn(0f, 1f)
    val cacheFrac = (cacheWeight / weightSum).coerceIn(0f, 1f)

    val hasApp = appFrac > 0.001f
    val hasData = dataFrac > 0.001f
    val hasCache = cacheFrac > 0.001f

    val p1 = appFrac
    val p2 = (appFrac + dataFrac).coerceIn(p1, 1f)

    val rawStops = when {
        hasApp && hasData && hasCache -> listOf(
            0f to blue,
            (p1 * 0.72f) to cyan,
            p1 to teal,
            (p1 + dataFrac * 0.28f) to mint,
            (p1 + dataFrac * 0.82f) to green,
            p2 to yellow,
            1f to orange
        )
        hasApp && hasData -> listOf(
            0f to blue,
            (p1 * 0.72f) to cyan,
            p1 to teal,
            (p1 + dataFrac * 0.30f) to mint,
            1f to green
        )
        hasApp && hasCache -> listOf(
            0f to blue,
            (p1 * 0.70f) to cyan,
            (p1 * 0.90f) to mint,
            p1 to yellow,
            1f to orange
        )
        hasData && hasCache -> listOf(
            0f to mint,
            (p2 * 0.78f) to green,
            p2 to yellow,
            1f to orange
        )
        hasApp -> listOf(0f to blue, 1f to cyan)
        hasData -> listOf(0f to mint, 1f to green)
        else -> listOf(0f to yellow, 1f to orange)
    }

    return rawStops
        .map { (pos, color) -> pos.coerceIn(0f, 1f) to color }
        .sortedBy { it.first }
        .toTypedArray()
}

/**
 * 紧凑级双轨单容器图标（36dp G2 平滑圆角底框 + 24dp 官方实心图标，严格遵循 2:3 比例）
 */
@Composable
private fun CompactDualTrackIcon(
    icon: ImageVector,
    iconColor: Color,
    iconStyle: SettingIconStyle = LocalSettingIconStyle.current
) {
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val containerBg = if (isSoftTranslucent) iconColor.copy(alpha = 0.15f) else iconColor
    val iconTint = if (isSoftTranslucent) iconColor else Color.White

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(containerBg, RoundedRectangle(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}
