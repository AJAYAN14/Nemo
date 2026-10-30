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
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.core.ui.modifier.shaderRipple
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.ui.modifier.tiltOnPress
import com.jian.nemo.core.ui.util.AppUtils
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
        colors = listOf(
            0xFFF59E0B.toInt(), // 暖金
            0xFF8B5CF6.toInt(), // 浅紫
            0xFFF43F5E.toInt(), // 蔷薇红
            0xFFEC4899.toInt(), // 浅粉
            0xFF3B82F6.toInt(), // 蔚蓝
            0xFFF97316.toInt(), // 橙金
            0xFF10B981.toInt()  // 翡翠青
        ),
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
 * 1. 顶部融合 Cresto 同款 3D 物理倾斜、AGSL 光波扩散与明度发光边框；
 * 2. 支持连续点击 10 次与【长按 1.5s】触发 Cresto 1:1 双炮筒抛物线喷射彩花；
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
                // 1. 顶部艺术卡片（3D 物理倾斜 + 长按检测 + AGSL 波纹 + 明度发光描边）
                // ==========================================
                item {
                    val density = LocalDensity.current
                    val cardShape = RoundedCornerShape(24.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .aspectRatio(3f / 4f)
                            .softCardShadow(borderRadius = 24.dp, isDark = isDark)
                            .tiltOnPress(
                                maxTilt = 12f,
                                longPressTimeoutMillis = 1500L,
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
                            .shaderRipple(dark = isDark)
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
                    SettingsSectionTitle("更新与维护")
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SquircleSettingItem(
                            icon = Icons.Rounded.RocketLaunch,
                            plumpIcon = PlumpIcon.LiquidLab,
                            iconColor = NemoCyan,
                            plumpColor = IsoluminantPalette.LakesideCyan,
                            title = "启动时检测更新",
                            subtitle = "应用启动时自动检查最新版本",
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
                                    accentColor = NemoPrimary
                                )
                            }
                        )

                        SquircleSettingItem(
                            icon = Icons.Rounded.SystemUpdate,
                            plumpIcon = PlumpIcon.CheckUpdate,
                            iconColor = NemoSecondary,
                            plumpColor = IsoluminantPalette.OceanBlue,
                            title = "检查新版本",
                            subtitle = "当前版本 v$versionName (Build $versionCode)",
                            onClick = {
                                onCheckUpdate()
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
                    SettingsSectionTitle("版本更新日志")
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            val logTitle = "更新日志"

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
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        interactionSource = changelogInteractionSource,
                                        indication = null
                                    ) { isChangelogExpanded = !isChangelogExpanded },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(NemoPrimary.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = NemoPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = logTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (aboutUiState.isLoadingLog) "正在拉取云端更新日志..." else "点击${if (isChangelogExpanded) "收起" else "展开"}详细更新日志",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Icon(
                                    imageVector = if (isChangelogExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = if (isChangelogExpanded) "收起更新日志" else "展开更新日志",
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
                                                    color = NemoPrimary
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "正在同步后端更新日志...",
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
                                                    text = "云端暂未配置本版本更新详情。",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        else -> {
                                            // 加载失败或暂无数据
                                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                                Text(
                                                    text = aboutUiState.errorMessage ?: "未能成功获取云端更新日志",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                TextButton(
                                                    onClick = { viewModel.fetchUpdateLog() },
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Refresh,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("重新拉取日志", fontSize = 13.sp)
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
                    SettingsSectionTitle("开源与社区")
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SquircleSettingItem(
                            icon = ImageVector.vectorResource(DesignR.drawable.ic_github),
                            plumpIcon = PlumpIcon.GitHub,
                            iconColor = NemoPrimary,
                            plumpColor = IsoluminantPalette.OceanBlue,
                            title = "GitHub 项目",
                            subtitle = "访问开源代码仓库，欢迎 Star & Issue",
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
                                    snackbarMessage = "已复制项目地址并打开浏览器"
                                    snackbarType = NemoSnackbarType.SUCCESS
                                    snackbarVisible = true
                                } catch (_: Exception) {
                                    snackbarMessage = "已复制项目地址：$githubUrl"
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
                            plumpIcon = PlumpIcon.User,
                            iconColor = NemoPurple,
                            plumpColor = IsoluminantPalette.LilacViolet,
                            title = "主开发者",
                            subtitle = "AJAYAN14 • 致力于极致体验的日语学习伴侣",
                            onClick = {
                                snackbarMessage = "感谢使用 Nemo！陪伴你的每一次记忆成长。"
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
                    SettingsSectionTitle("版本与构建信息")
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            VersionInfoRow(label = "应用名称", value = "Nemo 记忆伴侣")
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                            VersionInfoRow(label = "当前版本", value = "v$versionName")
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                            VersionInfoRow(label = "内部构建代号 (Build)", value = "$versionCode")
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                            VersionInfoRow(label = "技术架构", value = "Kotlin + Jetpack Compose + AGSL")
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
                    title = "关于 Nemo",
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
                .background(NemoPrimary, CircleShape)
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
 * 版本信息行
 */
@Composable
private fun VersionInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Nemo 专属艺术 Banner 绘制组件
 */
private const val GRAIN_SHADER_SRC = """
uniform shader composable;
uniform float2 resolution;
uniform float intensity;
uniform float isDark;

float hash21(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

half4 main(float2 fragCoord) {
    half4 color = composable.eval(fragCoord);
    float noise = hash21(fragCoord);
    float grain = (noise - 0.5) * intensity;
    if (isDark > 0.5) {
        color.rgb += half3(grain * 1.35);
    } else {
        color.rgb = clamp(color.rgb + half3(grain * 0.95), 0.0, 1.0);
    }
    return color;
}
"""

/**
 * Nemo 专属高阶纯代码艺术卡片 Banner
 *
 * 1. 采用对角线 135° 环境微光与艺术纸张底色；
 * 2. 居中渲染透明青蛙 Logo 背景图；
 * 3. AGSL 微米级胶片喷砂着色器（Film Grain Shader），打造孔版印刷砂纸肌理；
 * 4. 完美融合 3D 物理倾斜微动与明度发光边框。
 */
@Composable
private fun NemoArtisticBanner(isDark: Boolean) {
    val runtimeShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                android.graphics.RuntimeShader(GRAIN_SHADER_SRC)
            } catch (_: Throwable) {
                null
            }
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                if (runtimeShader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runtimeShader.setFloatUniform("resolution", size.width, size.height)
                    runtimeShader.setFloatUniform("intensity", if (isDark) 0.12f else 0.15f)
                    runtimeShader.setFloatUniform("isDark", if (isDark) 1.0f else 0.0f)
                    renderEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(
                        runtimeShader,
                        "composable"
                    ).asComposeRenderEffect()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. 底层艺术纸张背景色
            val paperBg = if (isDark) Color(0xFF131722) else Color(0xFFFAF6EE)
            drawRect(color = paperBg)

            // 2. 135° 对角线环境光束漫反射（贯穿卡片高光氛围）
            val lightBeamBrush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    if (isDark) Color(0xFF38BDF8).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.65f),
                    Color.Transparent
                ),
                start = Offset(0f, h * 0.95f),
                end = Offset(w * 0.95f, 0f)
            )
            drawRect(brush = lightBeamBrush)

            // 3. 低版本/未开启 AGSL 时的兜底微米纸质颗粒漫射层
            if (runtimeShader == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val noiseTint = if (isDark) Color.White.copy(alpha = 0.045f) else Color(0xFF6B4527).copy(alpha = 0.04f)
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
        }

        // 4. 透明青蛙 Logo 放置在卡片中
        Image(
            painter = painterResource(id = DesignR.drawable.img_about_frog_logo),
            contentDescription = "Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
