package com.jian.nemo.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.settings.R
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import com.jian.nemo.core.designsystem.theme.SettingsPalette
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.R as DesignR
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.ui.modifier.tiltOnPress
import com.jian.nemo.core.ui.util.AppUtils
import com.jian.nemo.feature.settings.components.LocalSettingIconStyle
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import com.jian.nemo.feature.settings.components.SquircleSettingItem
import nl.dionsegijn.konfetti.compose.KonfettiView
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val PREF_NAME = "nemo_app_prefs"
private const val KEY_CHECK_UPDATE_ON_STARTUP = "check_update_on_startup"

/**
 * Cresto 同款游行礼炮彩花配置 (双向对喷高抛物线发射)
 */
private fun parade(): List<Party> {
    val party = Party(
        speed = 10f,
        maxSpeed = 30f,
        damping = 0.91f,
        angle = Angle.RIGHT - 45, // 向右上放射喷射
        spread = 270,
        colors = NemoKonfettiColors,
        timeToLive = 4000L,
        emitter = Emitter(duration = 800, TimeUnit.MILLISECONDS).perSecond(150),
        position = Position.Relative(-0.1, 0.1) // 屏幕左侧上方偏外
    )

    return listOf(
        party,
        party.copy(
            angle = party.angle - 90, // 镜向向左上放射喷射
            position = Position.Relative(1.1, 0.1) // 屏幕右侧上方偏外
        )
    )
}

/**
 * 关于 Nemo 界面 (AboutScreen)
 *
 * 1. 顶部融合 Cresto 同款 3D 物理倾斜与明度发光边框；
 * 2. 支持连续点击 10 次与【长按 0.8s】触发 Cresto 1:1 双炮筒抛物线喷射彩花；
 * 3. 启动更新检测开关持久化控制冷启动更新；
 * 4. 更新日志对接 Supabase 后端 (ConfigRepository)，拒绝硬编码；
 * 5. 全面收拢 GitHub 开源项目、版本构建信息与检查新版本。
 */
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onCheckUpdate: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val aboutUiState by viewModel.uiState.collectAsState()
    val settingIconStyle by viewModel.settingIconStyle.collectAsState()

    val prefs = remember { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }
    var checkUpdateOnStartup by remember {
        mutableStateOf(prefs.getBoolean(KEY_CHECK_UPDATE_ON_STARTUP, true))
    }

    var cardTapCount by remember { mutableIntStateOf(0) }
    var confettiBurstKey by remember { mutableIntStateOf(0) }

    var snackbarVisible by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarType by remember { mutableStateOf(NemoSnackbarType.INFO) }

    var isChangelogExpanded by remember { mutableStateOf(true) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "checkUpdateTransition")
    val checkUpdateRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "checkUpdateRotation"
    )

    val versionName = remember { AppUtils.getVersionName(context) }
    val versionCode = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (_: PackageManager.NameNotFoundException) {
            1L
        }
    }

    // 触发彩蛋方法 (长按或连击满 10 次调用，仅展示全屏彩花与振动触感，不弹出提示)
    fun triggerEasterEgg() {
        confettiBurstKey++
        scope.launch {
            repeat(8) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(Random.nextLong(45, 90).milliseconds)
            }
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(
        LocalSettingIconStyle provides settingIconStyle
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
        // 1. 底层滚动内容（被 backdrop 采样源录制）
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentPadding = PaddingValues(
                top = statusBarTop + 60.dp + 8.dp,
                bottom = navBarBottom + 40.dp
            )
        ) {
                // ==========================================
                // 1. 顶部艺术卡片（3D 物理倾斜 + 长按检测 + 明度发光描边）
                // ==========================================
                item {
                    val density = LocalDensity.current
                    val cardShape = RoundedRectangle(24.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .aspectRatio(3f / 4f)
                            .softCardShadow(borderRadius = 24.dp, isDark = isDark)
                            .tiltOnPress(
                                maxTilt = 12f,
                                longPressTimeoutMillis = 800L,
                                onLongClick = {
                                    triggerEasterEgg()
                                },
                                onClick = {
                                    cardTapCount++
                                    if (cardTapCount >= 10) {
                                        cardTapCount = 0
                                        triggerEasterEgg()
                                    }
                                }
                            )
                            .clip(cardShape)
                            .drawWithContent {
                                drawContent()
                                val outline = cardShape.createOutline(
                                    size = size,
                                    layoutDirection = LayoutDirection.Ltr,
                                    density = density
                                )
                                drawOutline(
                                    outline = outline,
                                    style = Stroke(3.dp.toPx()),
                                    color = if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.08f),
                                    blendMode = BlendMode.Luminosity
                                )
                            }
                    ) {
                        NemoArtisticBanner(isDark = isDark)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ==========================================
                // 2. 更新设置与检查更新
                // ==========================================
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_about_section_maintenance))
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SquircleSettingItem(
                            icon = Icons.Rounded.RocketLaunch,
                            iconColor = SystemColors.Cyan,
                            title = stringResource(R.string.settings_about_title_auto_check),
                            subtitle = stringResource(R.string.settings_about_subtitle_auto_check),
                            onClick = {
                                val next = !checkUpdateOnStartup
                                checkUpdateOnStartup = next
                                prefs.edit().putBoolean(KEY_CHECK_UPDATE_ON_STARTUP, next).apply()
                            },
                            showDivider = true,
                            trailing = {
                                LiquidToggle(
                                    checked = checkUpdateOnStartup,
                                    onCheckedChange = { next ->
                                        checkUpdateOnStartup = next
                                        prefs.edit().putBoolean(KEY_CHECK_UPDATE_ON_STARTUP, next).apply()
                                    },
                                    accentColor = SystemColors.Blue
                                )
                            }
                        )

                        SquircleSettingItem(
                            icon = Icons.Rounded.Cached,
                            iconColor = SystemColors.Green,
                            iconRotation = if (isCheckingUpdate) checkUpdateRotation else 0f,
                            title = stringResource(R.string.settings_about_title_check_update),
                            subtitle = if (isCheckingUpdate) stringResource(R.string.settings_about_subtitle_checking) else stringResource(R.string.settings_about_subtitle_current_version_format, versionName, versionCode),
                            onClick = {
                                if (!isCheckingUpdate) {
                                    isCheckingUpdate = true
                                    onCheckUpdate()
                                    scope.launch {
                                        delay(1500L)
                                        isCheckingUpdate = false
                                    }
                                }
                            },
                            showDivider = false,
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ==========================================
                // 3. 更新日志（后端 Supabase 真实数据动态加载）
                // ==========================================
                item {
                    val iconStyle = LocalSettingIconStyle.current
                    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
                    val changelogBg = if (isSoftTranslucent) SystemColors.Blue.copy(alpha = 0.15f) else SystemColors.Blue
                    val changelogTint = if (isSoftTranslucent) SystemColors.Blue else Color.White

                    SettingsSectionTitle(stringResource(R.string.settings_about_section_changelog))
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            val logTitle = stringResource(R.string.settings_about_title_changelog)

                            val changelogInteractionSource = remember { MutableInteractionSource() }
                            val isChangelogPressed by changelogInteractionSource.collectIsPressedAsState()
                            val changelogScale by animateFloatAsState(
                                targetValue = if (isChangelogPressed) 0.98f else 1f,
                                label = "changelogHeaderScale",
                                animationSpec = tween(150)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = changelogScale
                                        scaleY = changelogScale
                                    }
                                    .clip(RoundedRectangle(12.dp))
                                    .clickable(
                                        interactionSource = changelogInteractionSource,
                                        indication = null
                                    ) { isChangelogExpanded = !isChangelogExpanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(changelogBg, RoundedRectangle(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = changelogTint,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = logTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (aboutUiState.isLoadingLog) stringResource(R.string.settings_about_log_loading) else if (isChangelogExpanded) stringResource(R.string.settings_about_log_collapse_tip) else stringResource(R.string.settings_about_log_expand_tip),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Icon(
                                    imageVector = if (isChangelogExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = if (isChangelogExpanded) stringResource(R.string.settings_about_cd_collapse_log) else stringResource(R.string.settings_about_cd_expand_log),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            AnimatedVisibility(visible = isChangelogExpanded) {
                                Column(modifier = Modifier.padding(top = 14.dp, start = 4.dp)) {
                                    when {
                                        aboutUiState.isLoadingLog -> {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = SystemColors.Blue
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = stringResource(R.string.settings_about_log_syncing),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        aboutUiState.updateConfig != null -> {
                                            val rawLog = aboutUiState.updateConfig?.updateLog.orEmpty()
                                            val logLines = rawLog.split("\n").filter { it.isNotBlank() }

                                            if (logLines.isNotEmpty()) {
                                                logLines.forEach { line ->
                                                    val cleanLine = line.trimStart('-', '*', '•', ' ')
                                                    ChangelogLineItem(text = cleanLine)
                                                }
                                            } else {
                                                Text(
                                                    text = stringResource(R.string.settings_about_log_empty),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        else -> {
                                            // 加载失败或暂无数据
                                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                                Text(
                                                    text = aboutUiState.errorMessage ?: stringResource(R.string.settings_about_log_error_fallback),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                TextButton(
                                                    onClick = { viewModel.fetchUpdateLog(forceRefresh = true) },
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Refresh,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(stringResource(R.string.settings_about_btn_fetch_log), fontSize = 13.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ==========================================
                // 4. 项目开源与开发者
                // ==========================================
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_about_section_opensource))
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SquircleSettingItem(
                            icon = ImageVector.vectorResource(DesignR.drawable.ic_github),
                            iconColor = SystemColors.Blue,
                            title = stringResource(R.string.settings_about_title_github),
                            subtitle = stringResource(R.string.settings_about_subtitle_github),
                            onClick = {
                                val githubUrl = "https://github.com/AJAYAN14/Nemo"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("GitHub URL", githubUrl)
                                clipboard?.setPrimaryClip(clip)

                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    snackbarMessage = context.getString(R.string.settings_about_toast_copied_open_browser)
                                    snackbarType = NemoSnackbarType.SUCCESS
                                    snackbarVisible = true
                                } catch (_: Exception) {
                                    snackbarMessage = context.getString(R.string.settings_about_toast_copied_format, githubUrl)
                                    snackbarType = NemoSnackbarType.INFO
                                    snackbarVisible = true
                                }
                            },
                            showDivider = true,
                            trailing = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        SquircleSettingItem(
                            icon = Icons.Rounded.Person,
                            iconColor = SystemColors.Purple,
                            title = stringResource(R.string.settings_about_title_developer),
                            subtitle = stringResource(R.string.settings_about_subtitle_developer),
                            onClick = {
                                snackbarMessage = context.getString(R.string.settings_about_toast_developer_cheer)
                                snackbarType = NemoSnackbarType.INFO
                                snackbarVisible = true
                            },
                            showDivider = false
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ==========================================
                // 5. 版本与构建规格
                // ==========================================
                item {
                    SettingsSectionTitle(stringResource(R.string.settings_about_section_build_info))
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            VersionInfoRow(
                                label = stringResource(R.string.settings_about_label_app_name),
                                value = stringResource(R.string.settings_about_value_app_name)
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                            VersionInfoRow(
                                label = stringResource(R.string.settings_about_label_version),
                                value = "v$versionName"
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                            VersionInfoRow(
                                label = stringResource(R.string.settings_about_label_build),
                                value = "$versionCode"
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                            VersionInfoRow(
                                label = stringResource(R.string.settings_about_label_tech_stack),
                                value = "Kotlin + Jetpack Compose"
                            )
                        }
                    }
                }
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
                    title = stringResource(R.string.settings_about_title_screen),
                    onBack = onNavigateBack,
                    backgroundColor = Color.Transparent,
                    backdrop = backdrop,
                    backButtonSize = 48.dp
                )
            }

            NemoSnackbar(
                visible = snackbarVisible,
                message = snackbarMessage,
                type = snackbarType,
                autoDismissMs = 2500L,
                onDismiss = { snackbarVisible = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
            )

            // ==========================================
            // 6. Cresto 同款游行礼炮彩花层 (KonfettiView)
            // ==========================================
            if (confettiBurstKey > 0) {
                key(confettiBurstKey) {
                    KonfettiView(
                        modifier = Modifier.fillMaxSize(),
                        parties = parade()
                    )
                }
            }
        }
    }
}

/**
 * 真实更新日志单行条目
 */
@Composable
private fun ChangelogLineItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 7.dp, end = 8.dp)
                .size(6.dp)
                .background(SystemColors.Blue, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp
        )
    }
}

/**
 * 版本与规格键值行
 */
@Composable
private fun VersionInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp,
            letterSpacing = (-0.41).sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp,
            letterSpacing = (-0.41).sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}

/**
 * Nemo 专属高阶纯代码艺术卡片 Banner
 *
 * 1. 采用对角线 135° 环境微光与艺术纸张底色；
 * 2. 居中渲染透明青蛙 Logo 背景图；
 * 3. 完美融合 3D 物理倾斜微动与明度发光边框。
 */
@Composable
private fun NemoArtisticBanner(isDark: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. 底层艺术纸张背景色
            val paperBg = if (isDark) SettingsPalette.AboutPaperBgDark else SettingsPalette.AboutPaperBgLight
            drawRect(color = paperBg)

            // 2. 135° 对角线环境光束漫反射（贯穿卡片高光氛围）
            val lightBeamBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    if (isDark) SettingsPalette.AboutLightBeamDark.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.65f),
                    Color.Transparent
                ),
                start = Offset(0f, h * 0.95f),
                end = Offset(w * 0.95f, 0f)
            )
            drawRect(brush = lightBeamBrush)

            // 3. 微米纸质颗粒漫射层
            val noiseTint = if (isDark) Color.White.copy(alpha = 0.045f) else SettingsPalette.AboutNoiseLight.copy(alpha = 0.04f)
            val step = 12f
            var y = 0f
            var r = 0
            while (y < h) {
                var x = 0f
                var c = 0
                while (x < w) {
                    val pseudo = (r * 43 + c * 89) % 97
                    if (pseudo > 50) {
                        drawCircle(
                            color = noiseTint,
                            radius = if (pseudo > 88) 1.5f else 0.8f,
                            center = Offset(x + (pseudo % 7) - 3f, y + ((pseudo / 7) % 7) - 3f)
                        )
                    }
                    x += step
                    c++
                }
                y += step
                r++
            }
        }

        // 4. 透明青蛙 Logo 放置在卡片中（居中留白，呈现高级艺术呼吸感）
        Image(
            painter = painterResource(id = DesignR.drawable.img_about_frog_logo),
            contentDescription = stringResource(R.string.settings_about_cd_logo),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            contentScale = ContentScale.Fit
        )
    }
}
