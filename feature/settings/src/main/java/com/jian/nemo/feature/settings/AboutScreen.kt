package com.jian.nemo.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.designsystem.R as DesignR
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.NemoKonfettiColors
import com.jian.nemo.core.designsystem.theme.SettingsPalette
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.animation.rememberIsTransitionSettled
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.core.ui.component.liquid.LiquidToggle
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.ui.modifier.tiltOnPress
import com.jian.nemo.feature.settings.components.LocalSettingIconStyle
import com.jian.nemo.feature.settings.components.PremiumCard
import com.jian.nemo.feature.settings.components.SettingsSectionTitle
import com.jian.nemo.feature.settings.components.SquircleSettingItem
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_BANNER_TEXTURE_SIDE_PX = 2048
private const val DEFAULT_NOISE_STEP_PX = 12f
private const val MAX_NOISE_CELLS = 2_000_000L

private sealed interface AboutLogoState {
    data object Loading : AboutLogoState
    data class Ready(val bitmap: ImageBitmap) : AboutLogoState
    data object Fallback : AboutLogoState
}

private class ArtisticPaperTextureCache {
    private data class Key(
        val widthPx: Int,
        val heightPx: Int,
        val paperBg: Color,
        val lightBeamHighlight: Color,
        val noiseTint: Color
    )

    private var cachedKey: Key? = null
    private var cachedBitmap: ImageBitmap? = null

    fun getOrCreate(
        widthPx: Int,
        heightPx: Int,
        paperBg: Color,
        lightBeamHighlight: Color,
        noiseTint: Color,
        create: () -> ImageBitmap?
    ): ImageBitmap? {
        val key = Key(widthPx, heightPx, paperBg, lightBeamHighlight, noiseTint)
        val existing = cachedBitmap
        if (cachedKey == key && existing != null) {
            return existing
        }
        val created = create() ?: return null
        cachedKey = key
        cachedBitmap = created
        return created
    }

    fun clear() {
        cachedKey = null
        cachedBitmap = null
    }
}

internal class ArtisticNoisePoints(
    val smallPoints: FloatArray,
    val largePoints: FloatArray
)

internal fun buildArtisticNoisePoints(
    width: Float,
    height: Float,
    step: Float = DEFAULT_NOISE_STEP_PX
): ArtisticNoisePoints {
    if (
        !width.isFinite() ||
        !height.isFinite() ||
        !step.isFinite() ||
        width <= 0f ||
        height <= 0f ||
        step <= 0f
    ) {
        return ArtisticNoisePoints(FloatArray(0), FloatArray(0))
    }
    val cols = ceil((width / step).toDouble()).toLong().coerceAtLeast(1L)
    val rows = ceil((height / step).toDouble()).toLong().coerceAtLeast(1L)
    val totalCells = cols * rows
    if (totalCells <= 0L || totalCells > MAX_NOISE_CELLS) {
        return ArtisticNoisePoints(FloatArray(0), FloatArray(0))
    }
    val estimatedMax = ((cols + 2L) * (rows + 2L)).coerceIn(16L, MAX_NOISE_CELLS).toInt()
    val smallBuffer = FloatArray(estimatedMax * 2)
    val largeBuffer = FloatArray(estimatedMax * 2)
    var smallCount = 0
    var largeCount = 0
    val jitterScale = step / DEFAULT_NOISE_STEP_PX

    var y = 0f
    var r = 0
    while (y < height) {
        var x = 0f
        var c = 0
        while (x < width) {
            val pseudo = (r * 43 + c * 89) % 97
            if (pseudo > 50) {
                val px = x + ((pseudo % 7) - 3f) * jitterScale
                val py = y + (((pseudo / 7) % 7) - 3f) * jitterScale
                if (pseudo > 88) {
                    if (largeCount + 2 <= largeBuffer.size) {
                        largeBuffer[largeCount++] = px
                        largeBuffer[largeCount++] = py
                    }
                } else {
                    if (smallCount + 2 <= smallBuffer.size) {
                        smallBuffer[smallCount++] = px
                        smallBuffer[smallCount++] = py
                    }
                }
            }
            x += step
            c++
        }
        y += step
        r++
    }

    return ArtisticNoisePoints(
        smallPoints = smallBuffer.copyOf(smallCount),
        largePoints = largeBuffer.copyOf(largeCount)
    )
}

/**
 * 游行礼炮彩花配置 (双向对喷高抛物线发射)
 */
private fun parade(): List<Party> {
    val party = Party(
        speed = 10f,
        maxSpeed = 30f,
        damping = 0.91f,
        angle = Angle.RIGHT - 45,
        spread = 270,
        colors = NemoKonfettiColors,
        timeToLive = 4000L,
        emitter = Emitter(duration = 800, TimeUnit.MILLISECONDS).perSecond(150),
        position = Position.Relative(-0.1, 0.1)
    )

    return listOf(
        party,
        party.copy(
            angle = party.angle - 90,
            position = Position.Relative(1.1, 0.1)
        )
    )
}

/**
 * 关于 Nemo 界面
 *
 * 1. 顶部融合 3D 物理倾斜与明度发光边框；
 * 2. 支持连续点击 10 次与长按 0.8s 触发双炮筒抛物线喷射彩花；
 * 3. 启动更新检测开关与版本信息由 ViewModel 统一管理；
 * 4. 更新日志对接 ConfigRepository 缓存与后端数据；
 * 5. 收拢 GitHub 开源项目、版本构建信息与检查新版本。
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

    val aboutUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingIconStyle by viewModel.settingIconStyle.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isInspectionMode = LocalInspectionMode.current
    val targetLogoPx = remember(configuration.screenWidthDp, density.density) {
        AboutLogoCache.calculateTargetSizePx(
            screenWidthDp = configuration.screenWidthDp,
            density = density.density
        )
    }
    val initialLogo = remember(targetLogoPx, isInspectionMode) {
        if (isInspectionMode) null else AboutLogoCache.getCached(targetLogoPx)
    }
    val logoState by produceState<AboutLogoState>(
        initialValue = if (initialLogo != null) AboutLogoState.Ready(initialLogo) else AboutLogoState.Loading,
        key1 = targetLogoPx,
        key2 = isInspectionMode
    ) {
        if (isInspectionMode) {
            value = AboutLogoState.Fallback
            return@produceState
        }
        val cached = AboutLogoCache.getCached(targetLogoPx)
        if (cached != null) {
            value = AboutLogoState.Ready(cached)
            return@produceState
        }
        val decoded = AboutLogoCache.getOrLoad(context, targetLogoPx)
        value = if (decoded != null) AboutLogoState.Ready(decoded) else AboutLogoState.Fallback
    }

    val cardTapCountState = remember { mutableIntStateOf(0) }
    var confettiBurstKey by remember { mutableIntStateOf(0) }

    var snackbarVisible by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var snackbarType by remember { mutableStateOf(NemoSnackbarType.INFO) }

    var isChangelogExpanded by remember { mutableStateOf(true) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    val currentHaptic by rememberUpdatedState(haptic)
    val currentScope by rememberUpdatedState(scope)
    val triggerEasterEgg = remember {
        {
            confettiBurstKey++
            currentScope.launch {
                repeat(8) {
                    currentHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    delay(Random.nextLong(45, 90).milliseconds)
                }
            }
            Unit
        }
    }
    val onCardClick = remember(triggerEasterEgg) {
        {
            val next = cardTapCountState.intValue + 1
            if (next >= 10) {
                cardTapCountState.intValue = 0
                triggerEasterEgg()
            } else {
                cardTapCountState.intValue = next
            }
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val backdrop = rememberLayerBackdrop()
    val isTransitionSettled = rememberIsTransitionSettled()
    val activeBackdrop = if (isTransitionSettled) backdrop else null
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val paperTextureCache = remember { ArtisticPaperTextureCache() }
    DisposableEffect(Unit) {
        onDispose {
            paperTextureCache.clear()
        }
    }

    val cardShape = remember { RoundedRectangle(24.dp) }
    val cardStrokeColor = if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.08f)
    val cardOutlineDrawCache = remember<CacheDrawScope.() -> DrawResult>(cardShape, cardStrokeColor) {
        {
            val outline = cardShape.createOutline(
                size = size,
                layoutDirection = layoutDirection,
                density = this
            )
            val stroke = Stroke(3.dp.toPx())
            onDrawWithContent {
                drawContent()
                drawOutline(
                    outline = outline,
                    style = stroke,
                    color = cardStrokeColor,
                    blendMode = BlendMode.Luminosity
                )
            }
        }
    }

    val versionSubtitle = if (aboutUiState.isMetadataReady) {
        stringResource(
            R.string.settings_about_subtitle_current_version_format,
            aboutUiState.versionName,
            aboutUiState.versionCode
        )
    } else {
        ""
    }

    val rawLog = aboutUiState.updateConfig?.updateLog.orEmpty()
    val logLines = remember(rawLog) {
        rawLog.split("\n")
            .asSequence()
            .filter { it.isNotBlank() }
            .map { it.trimStart('-', '*', '•', ' ') }
            .toList()
    }

    CompositionLocalProvider(
        LocalSettingIconStyle provides settingIconStyle
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop = backdrop, enabled = isTransitionSettled)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = statusBarTop + 60.dp + 8.dp,
                        bottom = navBarBottom + 40.dp
                    )
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .aspectRatio(3f / 4f)
                                .softCardShadow(borderRadius = 24.dp, isDark = isDark)
                                .tiltOnPress(
                                    maxTilt = 12f,
                                    longPressTimeoutMillis = 800L,
                                    onLongClick = triggerEasterEgg,
                                    onClick = onCardClick
                                )
                                .clip(cardShape)
                                .drawWithCache(cardOutlineDrawCache)
                        ) {
                            NemoArtisticBanner(
                                isDark = isDark,
                                logoState = logoState,
                                paperTextureCache = paperTextureCache
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                item {
                    SettingsSectionTitle(stringResource(R.string.settings_about_section_maintenance))
                    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SquircleSettingItem(
                            icon = Icons.Rounded.RocketLaunch,
                            iconColor = SystemColors.Cyan,
                            title = stringResource(R.string.settings_about_title_auto_check),
                            subtitle = stringResource(R.string.settings_about_subtitle_auto_check),
                            onClick = {
                                viewModel.setCheckUpdateOnStartup(!aboutUiState.checkUpdateOnStartup)
                            },
                            showDivider = true,
                            trailing = {
                                LiquidToggle(
                                    checked = aboutUiState.checkUpdateOnStartup,
                                    onCheckedChange = { next ->
                                        viewModel.setCheckUpdateOnStartup(next)
                                    },
                                    accentColor = SystemColors.Blue
                                )
                            }
                        )

                        CheckUpdateSettingItem(
                            isCheckingUpdate = isCheckingUpdate,
                            versionSubtitle = versionSubtitle,
                            onCheckUpdateClick = {
                                if (!isCheckingUpdate) {
                                    isCheckingUpdate = true
                                    onCheckUpdate()
                                    scope.launch {
                                        delay(1500L)
                                        isCheckingUpdate = false
                                    }
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

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
                                            if (logLines.isNotEmpty()) {
                                                logLines.forEach { cleanLine ->
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
                                value = if (aboutUiState.isMetadataReady) {
                                    stringResource(R.string.settings_about_format_version_v, aboutUiState.versionName)
                                } else {
                                    ""
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                            VersionInfoRow(
                                label = stringResource(R.string.settings_about_label_build),
                                value = if (aboutUiState.isMetadataReady) "${aboutUiState.versionCode}" else ""
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
            }

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
                    backdrop = activeBackdrop,
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

            if (confettiBurstKey > 0) {
                val parties = remember(confettiBurstKey) { parade() }
                key(confettiBurstKey) {
                    KonfettiView(
                        modifier = Modifier.fillMaxSize(),
                        parties = parties
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckUpdateSettingItem(
    isCheckingUpdate: Boolean,
    versionSubtitle: String,
    onCheckUpdateClick: () -> Unit
) {
    // 仅在主动检测更新时挂载无限旋转过渡，静止浏览页面时不产生任何帧回调
    val rotation = if (isCheckingUpdate) {
        val infiniteTransition = rememberInfiniteTransition(label = "checkUpdateTransition")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "checkUpdateRotation"
        ).value
    } else {
        0f
    }

    SquircleSettingItem(
        icon = Icons.Rounded.Cached,
        iconColor = SystemColors.Green,
        iconRotation = rotation,
        title = stringResource(R.string.settings_about_title_check_update),
        subtitle = if (isCheckingUpdate) {
            stringResource(R.string.settings_about_subtitle_checking)
        } else {
            versionSubtitle
        },
        onClick = onCheckUpdateClick,
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
 * Nemo 专属纯代码艺术卡片 Banner
 *
 * 将纸张底色、135°对角线环境光束与微米纸质颗粒离屏烘焙为按尺寸和主题缓存的纹理位图，
 * 避免每次重绘或 3D 倾斜时重复提交数千条矢量指令。
 */
@Composable
private fun NemoArtisticBanner(
    isDark: Boolean,
    logoState: AboutLogoState,
    paperTextureCache: ArtisticPaperTextureCache
) {
    val paperBg = if (isDark) SettingsPalette.AboutPaperBgDark else SettingsPalette.AboutPaperBgLight
    val lightBeamHighlight = if (isDark) {
        SettingsPalette.AboutLightBeamDark.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.65f)
    }
    val noiseTint = if (isDark) {
        Color.White.copy(alpha = 0.045f)
    } else {
        SettingsPalette.AboutNoiseLight.copy(alpha = 0.04f)
    }

    val backgroundDrawCache = remember<CacheDrawScope.() -> DrawResult>(
        paperBg,
        lightBeamHighlight,
        noiseTint,
        paperTextureCache
    ) {
        {
            val targetWidthPx = size.width.roundToInt()
            val targetHeightPx = size.height.roundToInt()
            val cachedTexture = paperTextureCache.getOrCreate(
                widthPx = targetWidthPx,
                heightPx = targetHeightPx,
                paperBg = paperBg,
                lightBeamHighlight = lightBeamHighlight,
                noiseTint = noiseTint
            ) {
                createArtisticPaperTexture(
                    targetWidthPx = targetWidthPx,
                    targetHeightPx = targetHeightPx,
                    paperBg = paperBg,
                    lightBeamHighlight = lightBeamHighlight,
                    noiseTint = noiseTint
                )
            }
            onDrawBehind {
                if (cachedTexture != null) {
                    drawImage(
                        image = cachedTexture,
                        dstSize = IntSize(targetWidthPx, targetHeightPx)
                    )
                } else {
                    drawRect(color = paperBg)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache(backgroundDrawCache)
    ) {
        when (logoState) {
            is AboutLogoState.Ready -> {
                Image(
                    bitmap = logoState.bitmap,
                    contentDescription = stringResource(R.string.settings_about_cd_logo),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    contentScale = ContentScale.Fit
                )
            }
            AboutLogoState.Fallback -> {
                Image(
                    painter = painterResource(id = DesignR.drawable.img_about_butterfly_logo),
                    contentDescription = stringResource(R.string.settings_about_cd_logo),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    contentScale = ContentScale.Fit
                )
            }
            AboutLogoState.Loading -> Unit
        }
    }
}

private fun CacheDrawScope.createArtisticPaperTexture(
    targetWidthPx: Int,
    targetHeightPx: Int,
    paperBg: Color,
    lightBeamHighlight: Color,
    noiseTint: Color
): ImageBitmap? {
    if (targetWidthPx <= 0 || targetHeightPx <= 0) return null
    val maxSide = maxOf(targetWidthPx, targetHeightPx)
    val scale = if (maxSide > MAX_BANNER_TEXTURE_SIDE_PX) {
        MAX_BANNER_TEXTURE_SIDE_PX.toFloat() / maxSide.toFloat()
    } else {
        1f
    }
    val texWidth = (targetWidthPx * scale).roundToInt().coerceAtLeast(1)
    val texHeight = (targetHeightPx * scale).roundToInt().coerceAtLeast(1)

    val imageBitmap = try {
        ImageBitmap(texWidth, texHeight, ImageBitmapConfig.Argb8888)
    } catch (_: OutOfMemoryError) {
        return null
    }
    val composeCanvas = Canvas(imageBitmap)
    val w = texWidth.toFloat()
    val h = texHeight.toFloat()
    val drawScope = CanvasDrawScope()

    drawScope.draw(
        density = this,
        layoutDirection = layoutDirection,
        canvas = composeCanvas,
        size = Size(w, h)
    ) {
        drawRect(color = paperBg)

        val lightBeamBrush = Brush.linearGradient(
            colors = listOf(
                Color.Transparent,
                lightBeamHighlight,
                Color.Transparent
            ),
            start = Offset(0f, h * 0.95f),
            end = Offset(w * 0.95f, 0f)
        )
        drawRect(brush = lightBeamBrush)

        val noisePoints = buildArtisticNoisePoints(width = w, height = h, step = 12f * scale)
        val nativeCanvas = drawContext.canvas.nativeCanvas
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            color = noiseTint.toArgb()
        }
        if (noisePoints.smallPoints.isNotEmpty()) {
            paint.strokeWidth = 1.6f * scale
            nativeCanvas.drawPoints(noisePoints.smallPoints, paint)
        }
        if (noisePoints.largePoints.isNotEmpty()) {
            paint.strokeWidth = 3.0f * scale
            nativeCanvas.drawPoints(noisePoints.largePoints, paint)
        }
    }
    imageBitmap.prepareToDraw()
    return imageBitmap
}
