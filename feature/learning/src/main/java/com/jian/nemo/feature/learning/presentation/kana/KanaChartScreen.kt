package com.jian.nemo.feature.learning.presentation.kana

import android.view.HapticFeedbackConstants
import com.jian.nemo.core.designsystem.theme.NemoCategoryColors
import com.jian.nemo.core.designsystem.theme.LearningPalette
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.collectAsState
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.feature.learning.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private data class KanaCell(
    val hiragana: String,
    val katakana: String?,
    val romaji: String,
    val isPlaceholder: Boolean = false,
    val isEmpty: Boolean = false
)

private enum class KanaType {
    Hiragana,
    Katakana
}

private data class KanaCardColorPair(
    val bg: Color,
    val text: Color
)

private val PremiumCategoryPalette = listOf(
    KanaCardColorPair(NemoCategoryColors.CardVerbBgLight, NemoCategoryColors.CardVerbTextLight),
    KanaCardColorPair(NemoCategoryColors.CardAdjIBgLight, NemoCategoryColors.CardAdjITextLight),
    KanaCardColorPair(NemoCategoryColors.CardAdjNaBgLight, NemoCategoryColors.CardAdjNaTextLight),
    KanaCardColorPair(NemoCategoryColors.CardNounBgLight, NemoCategoryColors.CardNounTextLight),
    KanaCardColorPair(NemoCategoryColors.CardAdvBgLight, NemoCategoryColors.CardAdvTextLight),
    KanaCardColorPair(NemoCategoryColors.CardConjBgLight, NemoCategoryColors.CardConjTextLight),
    KanaCardColorPair(NemoCategoryColors.CardFixBgLight, NemoCategoryColors.CardFixTextLight),
    KanaCardColorPair(NemoCategoryColors.CardKataBgLight, NemoCategoryColors.CardKataTextLight),
    KanaCardColorPair(NemoCategoryColors.CardKeigoBgLight, NemoCategoryColors.CardKeigoTextLight),
    KanaCardColorPair(NemoCategoryColors.CardSoundBgLight, NemoCategoryColors.CardSoundTextLight)
)

private val IsoluminantCategoryColors = listOf(
    IsoluminantPalette.SkyIndigo,     // あ行 (255°)
    IsoluminantPalette.EmeraldGreen,  // か行 (145°)
    IsoluminantPalette.WarmAmber,     // さ行 (70°)
    IsoluminantPalette.LilacViolet,   // た行 (295°)
    IsoluminantPalette.LakesideCyan,  // な行 (195°)
    IsoluminantPalette.RoseRuby,      // は行 (15°)
    IsoluminantPalette.SeaMarine,     // ま行 (225°)
    IsoluminantPalette.CoralFlame,    // や行 (30°)
    IsoluminantPalette.GlazeMint,     // ら行 (175°)
    IsoluminantPalette.OceanBlue      // わ行 (240°)
)

private val EP = KanaCell("", "", "", isEmpty = true)

private object KanaSectionIndex {
    const val Seion = 0
    const val Dakuon = 2
    const val Yoon = 5
}

private val LightPrimaryChip = LearningPalette.KanaChipBgLight
private val LightAccent = LearningPalette.KanaAccentLight
private val LightQuickNavBase = LearningPalette.KanaQuickNavBaseLight
private val LightQuickNavSelected = LearningPalette.KanaQuickNavSelectedLight

@Composable
fun KanaChartScreen(
    onNavigateBack: () -> Unit,
    viewModel: KanaChartViewModel = hiltViewModel()
) {
    val iconStyle by viewModel.iconStyle.collectAsState()
    val isPlumpDuo = iconStyle == SettingIconStyle.PLUMP_DUO

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val surfaceColor = if (isDark) colorScheme.surfaceContainer else colorScheme.surface
    val textMain = if (isDark) colorScheme.onSurface else LearningPalette.KanaSlateDark
    val textSub = if (isDark) colorScheme.onSurfaceVariant else LearningPalette.KanaSlateSub
    var currentType by remember { mutableIntStateOf(0) }
    var currentSection by remember { mutableStateOf("seion") }
    val isKatakana = currentType == 1

    val accentColor = if (isPlumpDuo) {
        when (currentSection) {
            "dakuon" -> IsoluminantPalette.EmeraldGreen
            "yoon" -> IsoluminantPalette.LilacViolet
            else -> IsoluminantPalette.SkyIndigo
        }
    } else {
        if (isDark) colorScheme.primary.copy(alpha = 0.7f) else LightAccent
    }
    val tabContainerColor = if (isDark) colorScheme.surfaceContainer else LearningPalette.KanaTabBgLight
    val tabSelectedColor = if (isDark) colorScheme.surfaceContainerHigh else Color.White
    val quickNavBaseColor = if (isDark) surfaceColor else LightQuickNavBase
    val quickNavSelectedColor = if (isDark) colorScheme.primary.copy(alpha = 0.28f) else LightQuickNavSelected

    val scope = rememberCoroutineScope()
    val playingAudioId by viewModel.playingAudioId.collectAsState()

    val sectionOrder = remember { mapOf("seion" to 0, "dakuon" to 1, "yoon" to 2) }
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalSettingIconStyle provides iconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
        // 1. 底层页面内容（被 backdrop 采样源录制，全屏贯穿至状态栏）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(backgroundColor)
        ) {
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    val oldIndex = sectionOrder[initialState] ?: 0
                    val newIndex = sectionOrder[targetState] ?: 0
                    if (newIndex > oldIndex) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "kanaPagination"
            ) { section ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 顶部弹性占位：未滑动时避开顶栏，滑动时无阻碍滑入顶栏遮罩
                    Spacer(modifier = Modifier.height(statusBarTop + 60.dp + 12.dp))

                    when (section) {
                        "seion" -> {
                            SectionTitle(
                                text = stringResource(R.string.kana_section_seion),
                                accentColor = accentColor,
                                textMain = textMain,
                                flashing = false
                            )
                            KanaGrid(
                                cells = seionData,
                                columns = 5,
                                isKatakana = isKatakana,
                                surfaceColor = surfaceColor,
                                textMain = textMain,
                                textSub = textSub,
                                isDark = isDark,
                                playingAudioId = playingAudioId,
                                playingBorderColor = colorScheme.primary,
                                onSpeak = { speakText, id -> viewModel.speakKana(speakText, id) },
                                onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                            )
                        }
                        "dakuon" -> {
                            SectionTitle(
                                text = stringResource(R.string.kana_section_dakuon),
                                accentColor = accentColor,
                                textMain = textMain,
                                flashing = false
                            )
                            SectionSubtitle(text = stringResource(R.string.kana_desc_dakuon), textSub = textSub)
                            KanaGrid(
                                cells = dakuonData,
                                columns = 5,
                                isKatakana = isKatakana,
                                surfaceColor = surfaceColor,
                                textMain = textMain,
                                textSub = textSub,
                                isDark = isDark,
                                playingAudioId = playingAudioId,
                                playingBorderColor = colorScheme.primary,
                                onSpeak = { speakText, id -> viewModel.speakKana(speakText, id) },
                                onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                            )
                        }
                        "yoon" -> {
                            SectionTitle(
                                text = stringResource(R.string.kana_section_yoon),
                                accentColor = accentColor,
                                textMain = textMain,
                                flashing = false
                            )
                            SectionSubtitle(text = stringResource(R.string.kana_desc_yoon), textSub = textSub)
                            KanaGrid(
                                cells = yoonData,
                                columns = 5,
                                isKatakana = isKatakana,
                                surfaceColor = surfaceColor,
                                textMain = textMain,
                                textSub = textSub,
                                isDark = isDark,
                                playingAudioId = playingAudioId,
                                playingBorderColor = colorScheme.primary,
                                onSpeak = { speakText, id -> viewModel.speakKana(speakText, id) },
                                onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(88.dp))
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
                            backgroundColor.copy(alpha = 0.98f),
                            backgroundColor.copy(alpha = 0.88f),
                            backgroundColor.copy(alpha = 0.55f),
                            backgroundColor.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
                .padding(bottom = 16.dp)
        ) {
            CommonHeader(
                title = stringResource(R.string.kana_chart_title),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                actions = {
                    val toggleFg = if (isPlumpDuo) {
                        if (currentType == 0) IsoluminantPalette.SkyIndigo else IsoluminantPalette.RoseRuby
                    } else {
                        if (currentType == 0) LearningPalette.KanaIndigoAccent else LearningPalette.KanaPinkAccent
                    }
                    FrostedLiquidGlassButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentType = if (currentType == 0) 1 else 0
                        },
                        backdrop = backdrop,
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        isInteractive = true,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isPlumpDuo) {
                                PlumpDuoIcon(
                                    icon = PlumpIcon.RandomShuffle,
                                    color = toggleFg,
                                    size = 18.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = stringResource(R.string.kana_btn_toggle_type),
                                    modifier = Modifier.size(20.dp),
                                    tint = toggleFg
                                )
                            }
                            Text(
                                text = if (currentType == 0) stringResource(R.string.kana_tab_hiragana) else stringResource(R.string.kana_tab_katakana),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = toggleFg
                            )
                        }
                    }
                }
            )
        }

        // 3. 底部悬浮液态玻璃分段切换栏（单手操作舒适区，读取 backdrop 产生透镜折射）
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarBottom + 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sections = listOf(
                Triple("seion", stringResource(R.string.kana_quick_nav_seion), IsoluminantPalette.SkyIndigo),
                Triple("dakuon", stringResource(R.string.kana_quick_nav_dakuon), IsoluminantPalette.EmeraldGreen),
                Triple("yoon", stringResource(R.string.kana_quick_nav_yoon), IsoluminantPalette.LilacViolet)
            )
            sections.forEach { (sectionKey, label, sectionColor) ->
                val isSelected = currentSection == sectionKey
                val activeThemeColor = if (isPlumpDuo) sectionColor else LearningPalette.KanaIndigoAccent

                val selectedSurface = if (isDark) {
                    if (isPlumpDuo) activeThemeColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                } else {
                    Color.White
                }
                val unselectedSurface = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.35f)

                val textFg = if (isSelected) {
                    if (isDark) Color.White else activeThemeColor
                } else {
                    if (isDark) Color.White.copy(alpha = 0.7f) else LearningPalette.KanaSlateDark
                }

                FrostedLiquidGlassButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        currentSection = sectionKey
                    },
                    backdrop = backdrop,
                    surfaceColor = if (isSelected) selectedSurface else unselectedSurface,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    isInteractive = true,
                    modifier = Modifier.height(46.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isPlumpDuo && isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(textFg)
                            )
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = textFg
                        )
                    }
                }
            }
        }
    }
}
}


@Composable
private fun QuickNavButton(
    label: String,
    surfaceColor: Color,
    selectedSurfaceColor: Color,
    textMain: Color,
    selected: Boolean,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
    val activeColor = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else LearningPalette.KanaIndigoAccent
    val fgColor by animateColorAsState(
        targetValue = if (selected) activeColor else textMain.copy(alpha = 0.6f),
        animationSpec = tween(180),
        label = "quickNavFg"
    )

    Column(
        modifier = Modifier
            .noRippleClickable(onClick = onClick)
            .padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = fgColor
        )
        if (selected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(width = 20.dp, height = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(activeColor)
            )
        } else {
            Spacer(modifier = Modifier.height(7.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String, accentColor: Color, textMain: Color, flashing: Boolean) {
    val borderColor by animateColorAsState(
        targetValue = if (flashing) accentColor.copy(alpha = 0.75f) else Color.Transparent,
        animationSpec = tween(220),
        label = "sectionFlashBorder"
    )
    Surface(
        modifier = Modifier.padding(top = 8.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = textMain
            )
        }
    }
}

@Composable
private fun SectionSubtitle(text: String, textSub: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = textSub
    )
}

@Composable
private fun KanaGrid(
    cells: List<KanaCell?>,
    columns: Int,
    isKatakana: Boolean,
    surfaceColor: Color,
    textMain: Color,
    textSub: Color,
    isDark: Boolean,
    playingAudioId: String?,
    playingBorderColor: Color,
    onSpeak: (String, String) -> Unit,
    onHaptic: () -> Unit
) {
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(columns).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { cell ->
                    val (cardColor, cardTextMain, cardBorderColor) = if (isPlumpDuo) {
                        val isoluminantColor = IsoluminantCategoryColors[rowIndex % IsoluminantCategoryColors.size]
                        if (cell?.isPlaceholder == false && cell.isEmpty == false) {
                            val bg = isoluminantColor.copy(alpha = if (isDark) 0.12f else 0.08f)
                            Triple(bg, isoluminantColor, isoluminantColor)
                        } else {
                            Triple(surfaceColor, textMain, playingBorderColor)
                        }
                    } else {
                        val colorPair = PremiumCategoryPalette[rowIndex % PremiumCategoryPalette.size]
                        val bg = if (!isDark && cell?.isPlaceholder == false && cell.isEmpty == false) colorPair.bg else surfaceColor
                        val fg = if (!isDark && cell?.isPlaceholder == false && cell.isEmpty == false) colorPair.text else textMain
                        Triple(bg, fg, playingBorderColor)
                    }

                    KanaCard(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 0.dp),
                        cell = cell,
                        isKatakana = isKatakana,
                        surfaceColor = cardColor,
                        textMain = cardTextMain,
                        textSub = textSub,
                        isDark = isDark,
                        playingAudioId = playingAudioId,
                        playingBorderColor = cardBorderColor,
                        onSpeak = onSpeak,
                        onHaptic = onHaptic
                    )
                }
                repeat(columns - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun KanaCard(
    modifier: Modifier = Modifier,
    cell: KanaCell?,
    isKatakana: Boolean,
    surfaceColor: Color,
    textMain: Color,
    textSub: Color,
    isDark: Boolean,
    playingAudioId: String?,
    playingBorderColor: Color,
    onSpeak: (String, String) -> Unit,
    onHaptic: () -> Unit
) {
    if (cell == null || (isKatakana && cell.katakana == null)) {
        Spacer(modifier = modifier)
        return
    }

    if (cell.isEmpty) {
        val emptyBorderColor = if (isDark) NemoNeutrals.Gray700 else NemoNeutrals.Gray200
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .background(Color.Transparent)
                .drawWithCache {
                    val stroke = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
                    )
                    onDrawBehind {
                        drawRoundRect(
                            color = emptyBorderColor,
                            style = stroke,
                            cornerRadius = CornerRadius(20.dp.toPx())
                        )
                    }
                }
        )
        return
    }

    val kanaText = if (isKatakana) cell.katakana.orEmpty() else cell.hiragana
    val cardAudioId = "kana_${kanaText.hashCode()}"
    val isPlaying = playingAudioId == cardAudioId
    
    val defaultBorderColor = if (cell.isPlaceholder) NemoNeutrals.Gray300 else Color.Transparent
    val borderColor by animateColorAsState(
        targetValue = if (isPlaying) playingBorderColor.copy(alpha = if (isDark) 0.88f else 0.78f) else defaultBorderColor,
        animationSpec = tween(180),
        label = "cardPlayingBorder"
    )


    val actualSurfaceColor = if (cell.isPlaceholder) Color.Transparent else surfaceColor
    val textOpacity = if (cell.isPlaceholder) 0.3f else 1f
    val cardShape = RoundedCornerShape(20.dp)
    val cardBorder = BorderStroke(if (cell.isPlaceholder) 2.dp else 1.dp, borderColor)

    if (cell.isPlaceholder) {
        Surface(
            modifier = modifier.aspectRatio(1f),
            shape = cardShape,
            color = Color.Transparent,
            border = cardBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val kanaSize = if (kanaText.length > 2) 18.sp else 24.sp

                Text(
                    text = kanaText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = NotoSerifJP,
                        fontWeight = FontWeight.Black,
                        fontSize = kanaSize
                    ),
                    color = textMain.copy(alpha = textOpacity)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cell.romaji,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = textSub.copy(alpha = textOpacity)
                )
            }
        }
    } else {
        LiquidButton(
            onClick = {
                onHaptic()
                val speakText = cell.speakText(isKatakana)
                onSpeak(speakText, cardAudioId)
            },
            modifier = modifier
                .softCardShadow(borderRadius = 20.dp, isDark = isDark)
                .aspectRatio(1f),
            backgroundColor = actualSurfaceColor,
            shape = cardShape,
            border = cardBorder,
            elevation = 0.dp,
            isInteractive = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val kanaSize = if (kanaText.length > 2) 18.sp else 24.sp

                Text(
                    text = kanaText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = NotoSerifJP,
                        fontWeight = FontWeight.Black,
                        fontSize = kanaSize
                    ),
                    color = textMain.copy(alpha = textOpacity)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cell.romaji,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = textSub.copy(alpha = textOpacity)
                )
            }
        }
    }
}

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    return noRippleClickable(interactionSource = null, onClick = onClick)
}

private fun Modifier.noRippleClickable(
    interactionSource: MutableInteractionSource?,
    onClick: () -> Unit
): Modifier {
    return composed {
        val source = interactionSource ?: remember { MutableInteractionSource() }
        this.clickable(
            interactionSource = source,
            indication = null,
            onClick = onClick
        )
    }
}

private fun KanaCell.speakText(isKatakana: Boolean): String {
    val text = if (isKatakana) katakana ?: hiragana else hiragana
    return when (text) {
        "っ+k", "ッ+k" -> if (isKatakana) "ガッコウ" else "がっこう"
        "っ+s", "ッ+s" -> if (isKatakana) "カッサ" else "かっさ"
        "っ+t", "ッ+t" -> if (isKatakana) "キッテ" else "きって"
        "っ+p", "ッ+p" -> if (isKatakana) "カップ" else "かっぷ"
        else -> text
    }
}

private val seionData = listOf(
    KanaCell("あ", "ア", "a"), KanaCell("い", "イ", "i"), KanaCell("う", "ウ", "u"), KanaCell("え", "エ", "e"), KanaCell("お", "オ", "o"),
    KanaCell("か", "カ", "ka"), KanaCell("き", "キ", "ki"), KanaCell("く", "ク", "ku"), KanaCell("け", "ケ", "ke"), KanaCell("こ", "コ", "ko"),
    KanaCell("さ", "サ", "sa"), KanaCell("し", "シ", "shi"), KanaCell("す", "ス", "su"), KanaCell("せ", "セ", "se"), KanaCell("そ", "ソ", "so"),
    KanaCell("た", "タ", "ta"), KanaCell("ち", "チ", "chi"), KanaCell("つ", "ツ", "tsu"), KanaCell("て", "テ", "te"), KanaCell("と", "ト", "to"),
    KanaCell("な", "ナ", "na"), KanaCell("に", "ニ", "ni"), KanaCell("ぬ", "ヌ", "nu"), KanaCell("ね", "ネ", "ne"), KanaCell("の", "ノ", "no"),
    KanaCell("は", "ハ", "ha"), KanaCell("ひ", "ヒ", "hi"), KanaCell("ふ", "フ", "fu"), KanaCell("へ", "ヘ", "he"), KanaCell("ほ", "ホ", "ho"),
    KanaCell("ま", "マ", "ma"), KanaCell("み", "ミ", "mi"), KanaCell("む", "ム", "mu"), KanaCell("め", "メ", "me"), KanaCell("も", "モ", "mo"),
    KanaCell("や", "ヤ", "ya"), KanaCell("(い)", "(イ)", "(i)", isPlaceholder = true), KanaCell("ゆ", "ユ", "yu"), KanaCell("(え)", "(エ)", "(e)", isPlaceholder = true), KanaCell("よ", "ヨ", "yo"),
    KanaCell("ら", "ラ", "ra"), KanaCell("り", "リ", "ri"), KanaCell("る", "ル", "ru"), KanaCell("れ", "レ", "re"), KanaCell("ろ", "ロ", "ro"),
    KanaCell("わ", "ワ", "wa"), KanaCell("(い)", "(イ)", "(i)", isPlaceholder = true), KanaCell("(う)", "(ウ)", "(u)", isPlaceholder = true), KanaCell("(え)", "(エ)", "(e)", isPlaceholder = true), KanaCell("を", "ヲ", "wo"),
    KanaCell("ん", "ン", "n"), null, null, null, null
)

private val dakuonData = listOf(
    KanaCell("が", "ガ", "ga"), KanaCell("ぎ", "ギ", "gi"), KanaCell("ぐ", "グ", "gu"), KanaCell("げ", "ゲ", "ge"), KanaCell("ご", "ゴ", "go"),
    KanaCell("ざ", "ザ", "za"), KanaCell("じ", "ジ", "ji"), KanaCell("ず", "ズ", "zu"), KanaCell("ぜ", "ゼ", "ze"), KanaCell("ぞ", "ゾ", "zo"),
    KanaCell("だ", "ダ", "da"), KanaCell("ぢ", "ヂ", "ji"), KanaCell("づ", "ヅ", "zu"), KanaCell("で", "デ", "de"), KanaCell("ど", "ド", "do"),
    KanaCell("ば", "バ", "ba"), KanaCell("び", "ビ", "bi"), KanaCell("ぶ", "ブ", "bu"), KanaCell("べ", "ベ", "be"), KanaCell("ぼ", "ボ", "bo"),
    KanaCell("ぱ", "パ", "pa"), KanaCell("ぴ", "ピ", "pi"), KanaCell("ぷ", "プ", "pu"), KanaCell("ぺ", "ペ", "pe"), KanaCell("ぽ", "ポ", "po")
)

private val yoonData = listOf(
    KanaCell("きゃ", "キャ", "kya"), EP, KanaCell("きゅ", "キュ", "kyu"), EP, KanaCell("きょ", "キョ", "kyo"),
    KanaCell("しゃ", "シャ", "sha"), EP, KanaCell("しゅ", "シュ", "shu"), EP, KanaCell("しょ", "ショ", "sho"),
    KanaCell("ちゃ", "チャ", "cha"), EP, KanaCell("ちゅ", "チュ", "chu"), EP, KanaCell("ちょ", "チョ", "cho"),
    KanaCell("にゃ", "ニャ", "nya"), EP, KanaCell("にゅ", "ニュ", "nyu"), EP, KanaCell("にょ", "ニョ", "nyo"),
    KanaCell("ひゃ", "ヒャ", "hya"), EP, KanaCell("ひゅ", "ヒュ", "hyu"), EP, KanaCell("ひょ", "ヒョ", "hyo"),
    KanaCell("みゃ", "ミャ", "mya"), EP, KanaCell("みゅ", "ミュ", "myu"), EP, KanaCell("みょ", "ミョ", "myo"),
    KanaCell("りゃ", "リャ", "rya"), EP, KanaCell("りゅ", "リュ", "ryu"), EP, KanaCell("りょ", "リョ", "ryo"),
    KanaCell("ぎゃ", "ギャ", "gya"), EP, KanaCell("ぎゅ", "ギュ", "gyu"), EP, KanaCell("ぎょ", "ギョ", "gyo"),
    KanaCell("じゃ", "ジャ", "ja"), EP, KanaCell("じゅ", "ジュ", "ju"), EP, KanaCell("じょ", "ジョ", "jo"),
    KanaCell("びゃ", "ビャ", "bya"), EP, KanaCell("びゅ", "ビュ", "byu"), EP, KanaCell("びょ", "ビョ", "byo"),
    KanaCell("ぴゃ", "ピャ", "pya"), EP, KanaCell("ぴゅ", "ピュ", "pyu"), EP, KanaCell("ぴょ", "ピョ", "pyo")
)


