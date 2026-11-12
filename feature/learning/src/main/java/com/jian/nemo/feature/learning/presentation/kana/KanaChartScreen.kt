package com.jian.nemo.feature.learning.presentation.kana

import android.os.Build
import android.util.LruCache
import com.jian.nemo.core.designsystem.theme.LearningPalette
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.createInteractiveHighlightRuntimeShader
import com.jian.nemo.core.ui.component.liquid.resolveShapeBorderMask
import com.jian.nemo.core.ui.component.liquid.resolveShapeFillMask
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jian.nemo.core.designsystem.theme.SystemColors
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp.Companion.Hairline
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.learning.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

private val KanaCardCornerRadius = 14.dp
private val KanaCardShape = RoundedRectangle(KanaCardCornerRadius)

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

private val CategoryRowColors = listOf(
    SystemColors.Indigo,  // あ行
    SystemColors.Green,   // か行
    SystemColors.Orange,  // さ行
    SystemColors.Purple,  // た行
    SystemColors.Cyan,    // な行
    SystemColors.Pink,    // は行
    SystemColors.Blue,    // ま行
    SystemColors.Yellow,  // や行
    SystemColors.Mint,    // ら行
    SystemColors.Teal,    // わ行
    SystemColors.Red      // ん行
)

private fun getCategoryRowColor(rowIndex: Int, isDark: Boolean): Color {
    val index = rowIndex % CategoryRowColors.size
    return if (isDark) {
        when (index) {
            0 -> SystemColors.IndigoDark
            1 -> SystemColors.GreenDark
            2 -> SystemColors.OrangeDark
            3 -> SystemColors.PurpleDark
            4 -> SystemColors.CyanDark
            5 -> SystemColors.PinkDark
            6 -> SystemColors.BlueDark
            7 -> SystemColors.YellowDark
            8 -> SystemColors.MintDark
            9 -> SystemColors.TealDark
            else -> SystemColors.RedDark
        }
    } else {
        CategoryRowColors[index]
    }
}

private val EP = KanaCell("", "", "", isEmpty = true)

@Composable
fun KanaChartScreen(
    onNavigateBack: () -> Unit,
    viewModel: KanaChartViewModel = hiltViewModel()
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current

    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val textMain = if (isDark) colorScheme.onSurface else LearningPalette.KanaSlateDark
    val textSub = if (isDark) colorScheme.onSurfaceVariant else LearningPalette.KanaSlateSub
    var currentType by remember { mutableIntStateOf(0) }
    var currentSection by remember { mutableStateOf("seion") }
    val isKatakana = currentType == 1

    val accentColor = when (currentSection) {
        "dakuon" -> if (isDark) SystemColors.GreenDark else SystemColors.Green
        "yoon" -> if (isDark) SystemColors.PurpleDark else SystemColors.Purple
        else -> if (isDark) SystemColors.IndigoDark else SystemColors.Indigo
    }

    val playingAudioId by viewModel.playingAudioId.collectAsState()

    val sectionOrder = remember { mapOf("seion" to 0, "dakuon" to 1, "yoon" to 2) }
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val textMeasurer = rememberTextMeasurer(cacheSize = 256)
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val typography = MaterialTheme.typography
    val kanaStyleNormal = remember(typography) {
        typography.titleLarge.copy(
            fontFamily = NotoSerifJP,
            fontWeight = FontWeight.Black,
            fontSize = 21.sp
        )
    }
    val kanaStyleLong = remember(typography) {
        typography.titleLarge.copy(
            fontFamily = NotoSerifJP,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp
        )
    }
    val romajiStyle = remember(typography) {
        typography.labelSmall.copy(
            fontWeight = FontWeight.Medium
        )
    }
    val playingCardBorder = remember { BorderStroke(2.dp, Color.White) }

    val onSpeakAction = remember(viewModel) {
        { speakText: String, id: String -> viewModel.speakKana(speakText, id) }
    }
    val onHapticAction = remember(haptic) {
        { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                val (data, titleRes, descRes) = when (section) {
                    "dakuon" -> Triple(dakuonData, R.string.kana_section_dakuon, R.string.kana_desc_dakuon)
                    "yoon" -> Triple(yoonData, R.string.kana_section_yoon, R.string.kana_desc_yoon)
                    else -> Triple(seionData, R.string.kana_section_seion, null)
                }
                val rows = remember(data) { data.chunked(5) }
                val scrollState = rememberScrollState()

                val cardSurfaceColor = if (isDark) colorScheme.surfaceContainer else Color.White

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(statusBarTop + 60.dp + 16.dp))

                    SectionTitle(
                        text = stringResource(titleRes),
                        accentColor = accentColor,
                        textMain = textMain,
                        modifier = Modifier.padding(
                            top = 8.dp,
                            bottom = if (descRes != null) 4.dp else 8.dp
                        )
                    )

                    if (descRes != null) {
                        SectionSubtitle(
                            text = stringResource(descRes),
                            textSub = textSub,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Surface(
                        shape = RoundedRectangle(24.dp),
                        color = cardSurfaceColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .softCardShadow(borderRadius = 24.dp, isDark = isDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rows.forEachIndexed { rowIndex, row ->
                                val rowColor = getCategoryRowColor(rowIndex, isDark)
                                val rowBorder = remember(rowColor, isDark) {
                                    BorderStroke(1.dp, rowColor.copy(alpha = if (isDark) 0.30f else 0.12f))
                                }
                                val placeholderBorder = remember(rowColor, isDark) {
                                    BorderStroke(1.dp, rowColor.copy(alpha = if (isDark) 0.40f else 0.25f))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { cell ->
                                        val kanaText = if (cell == null) "" else if (isKatakana) cell.katakana.orEmpty() else cell.hiragana
                                        val cardAudioId = if (cell == null || cell.isEmpty || cell.isPlaceholder) "" else "kana_${kanaText.hashCode()}"
                                        val isPlaying = playingAudioId != null && cardAudioId.isNotEmpty() && playingAudioId == cardAudioId
                                        KanaCard(
                                            modifier = Modifier.weight(1f),
                                            cell = cell,
                                            isKatakana = isKatakana,
                                            rowColor = rowColor,
                                            isDark = isDark,
                                            isPlaying = isPlaying,
                                            cardAudioId = cardAudioId,
                                            defaultBorder = if (cell?.isPlaceholder == true) placeholderBorder else rowBorder,
                                            playingBorder = playingCardBorder,
                                            kanaStyleNormal = kanaStyleNormal,
                                            kanaStyleLong = kanaStyleLong,
                                            romajiStyle = romajiStyle,
                                            textMeasurer = textMeasurer,
                                            density = density,
                                            layoutDirection = layoutDirection,
                                            onSpeak = onSpeakAction,
                                            onHaptic = onHapticAction
                                        )
                                    }
                                    repeat(5 - row.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
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
                title = stringResource(R.string.kana_chart_title),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                actions = {
                    val toggleFg = if (currentType == 0) {
                        if (isDark) SystemColors.IndigoDark else SystemColors.Indigo
                    } else {
                        if (isDark) SystemColors.Pink else SystemColors.Pink
                    }
                    FrostedLiquidGlassButton(
                        onClick = {
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
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = stringResource(R.string.kana_btn_toggle_type),
                                modifier = Modifier.size(20.dp),
                                tint = toggleFg
                            )
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
                Triple("seion", stringResource(R.string.kana_quick_nav_seion), if (isDark) SystemColors.IndigoDark else SystemColors.Indigo),
                Triple("dakuon", stringResource(R.string.kana_quick_nav_dakuon), if (isDark) SystemColors.GreenDark else SystemColors.Green),
                Triple("yoon", stringResource(R.string.kana_quick_nav_yoon), if (isDark) SystemColors.PurpleDark else SystemColors.Purple)
            )
            sections.forEach { (sectionKey, label, sectionColor) ->
                val isSelected = currentSection == sectionKey
                val activeThemeColor = sectionColor

                val selectedSurface = if (isDark) {
                    activeThemeColor.copy(alpha = 0.35f)
                } else {
                    Color.White
                }
                val unselectedSurface = if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.35f)

                val textFg = if (isSelected) {
                    if (isDark) Color.White else activeThemeColor
                } else {
                    if (isDark) Color.White.copy(alpha = 0.7f) else colorScheme.onSurfaceVariant
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
                        if (isSelected) {
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


@Composable
private fun SectionTitle(
    text: String,
    accentColor: Color,
    textMain: Color,
    modifier: Modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedRectangle(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = textMain
        )
    }
}

@Composable
private fun SectionSubtitle(
    text: String,
    textSub: Color,
    modifier: Modifier = Modifier.padding(horizontal = 0.dp, vertical = 2.dp)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = textSub,
        modifier = modifier
    )
}


private val KanaPressSpec = spring(0.5f, 300f, 0.001f)
private val KanaPositionSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

private data class KanaTextCacheKey(
    val text: String,
    val fontSizeBits: Int,
    val fontWeight: Int,
    val density: Float,
    val fontScale: Float,
    val layoutDirection: LayoutDirection
)

private object KanaTextLayoutCache {
    private val cache = LruCache<KanaTextCacheKey, TextLayoutResult>(384)

    fun getOrMeasure(
        text: String,
        style: TextStyle,
        textMeasurer: TextMeasurer,
        density: Density,
        layoutDirection: LayoutDirection
    ): TextLayoutResult {
        val key = KanaTextCacheKey(
            text = text,
            fontSizeBits = style.fontSize.value.toRawBits(),
            fontWeight = style.fontWeight?.weight ?: 0,
            density = density.density,
            fontScale = density.fontScale,
            layoutDirection = layoutDirection
        )
        cache.get(key)?.let { return it }
        val measured = textMeasurer.measure(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
        cache.put(key, measured)
        return measured
    }
}

@Composable
private fun KanaCard(
    modifier: Modifier = Modifier,
    cell: KanaCell?,
    isKatakana: Boolean,
    rowColor: Color,
    isDark: Boolean,
    isPlaying: Boolean,
    cardAudioId: String,
    defaultBorder: BorderStroke,
    playingBorder: BorderStroke,
    kanaStyleNormal: TextStyle,
    kanaStyleLong: TextStyle,
    romajiStyle: TextStyle,
    textMeasurer: TextMeasurer,
    density: Density,
    layoutDirection: LayoutDirection,
    onSpeak: (String, String) -> Unit,
    onHaptic: () -> Unit
) {
    if (cell == null || (isKatakana && cell.katakana == null)) {
        Spacer(modifier = modifier)
        return
    }

    if (cell.isEmpty) {
        val emptyBorderColor = rowColor.copy(alpha = if (isDark) 0.35f else 0.25f)
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .drawWithCache {
                    val stroke = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
                    )
                    onDrawBehind {
                        drawRoundRect(
                            color = emptyBorderColor,
                            style = stroke,
                            cornerRadius = CornerRadius(KanaCardCornerRadius.toPx())
                        )
                    }
                }
        )
        return
    }

    val kanaText = if (isKatakana) cell.katakana.orEmpty() else cell.hiragana
    val kanaStyle = if (kanaText.length > 2) kanaStyleLong else kanaStyleNormal
    val kanaLayout = remember(kanaText, kanaStyle, textMeasurer, density, layoutDirection) {
        KanaTextLayoutCache.getOrMeasure(kanaText, kanaStyle, textMeasurer, density, layoutDirection)
    }
    val romajiLayout = remember(cell.romaji, romajiStyle, textMeasurer, density, layoutDirection) {
        KanaTextLayoutCache.getOrMeasure(cell.romaji, romajiStyle, textMeasurer, density, layoutDirection)
    }

    val bgColor: Color
    val textColor: Color
    val subTextColor: Color
    val activeBorder: BorderStroke
    val interactive: Boolean
    val speakText: String
    if (cell.isPlaceholder) {
        bgColor = rowColor.copy(alpha = if (isDark) 0.15f else 0.10f)
        textColor = if (isDark) Color.White.copy(alpha = 0.50f) else rowColor.copy(alpha = 0.70f)
        subTextColor = if (isDark) Color.White.copy(alpha = 0.35f) else rowColor.copy(alpha = 0.50f)
        activeBorder = defaultBorder
        interactive = false
        speakText = ""
    } else {
        bgColor = rowColor
        textColor = Color.White
        subTextColor = Color.White.copy(alpha = 0.85f)
        activeBorder = if (isPlaying) playingBorder else defaultBorder
        interactive = true
        speakText = remember(cell, isKatakana) { cell.speakText(isKatakana) }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .kanaCardSurface(
                backgroundColor = bgColor,
                border = activeBorder,
                shape = KanaCardShape,
                kanaLayout = kanaLayout,
                romajiLayout = romajiLayout,
                kanaColor = textColor,
                romajiColor = subTextColor,
                interactive = interactive,
                accessibilityLabel = "$kanaText ${cell.romaji}",
                speakText = speakText,
                cardAudioId = cardAudioId,
                onSpeak = onSpeak,
                onHaptic = onHaptic
            )
    )
}

private fun Modifier.kanaCardSurface(
    backgroundColor: Color,
    border: BorderStroke,
    shape: Shape,
    kanaLayout: TextLayoutResult,
    romajiLayout: TextLayoutResult,
    kanaColor: Color,
    romajiColor: Color,
    interactive: Boolean,
    accessibilityLabel: String,
    speakText: String,
    cardAudioId: String,
    onSpeak: (String, String) -> Unit,
    onHaptic: () -> Unit
): Modifier = this then KanaCardSurfaceElement(
    backgroundColor = backgroundColor,
    border = border,
    shape = shape,
    kanaLayout = kanaLayout,
    romajiLayout = romajiLayout,
    kanaColor = kanaColor,
    romajiColor = romajiColor,
    interactive = interactive,
    accessibilityLabel = accessibilityLabel,
    speakText = speakText,
    cardAudioId = cardAudioId,
    onSpeak = onSpeak,
    onHaptic = onHaptic
)

private data class KanaCardSurfaceElement(
    val backgroundColor: Color,
    val border: BorderStroke,
    val shape: Shape,
    val kanaLayout: TextLayoutResult,
    val romajiLayout: TextLayoutResult,
    val kanaColor: Color,
    val romajiColor: Color,
    val interactive: Boolean,
    val accessibilityLabel: String,
    val speakText: String,
    val cardAudioId: String,
    val onSpeak: (String, String) -> Unit,
    val onHaptic: () -> Unit
) : ModifierNodeElement<KanaCardSurfaceNode>() {
    override fun create(): KanaCardSurfaceNode = KanaCardSurfaceNode(
        backgroundColor = backgroundColor,
        border = border,
        shape = shape,
        kanaLayout = kanaLayout,
        romajiLayout = romajiLayout,
        kanaColor = kanaColor,
        romajiColor = romajiColor,
        interactive = interactive,
        accessibilityLabel = accessibilityLabel,
        speakText = speakText,
        cardAudioId = cardAudioId,
        onSpeak = onSpeak,
        onHaptic = onHaptic
    )

    override fun update(node: KanaCardSurfaceNode) {
        val geometryChanged = node.shape != shape ||
            node.border.width != border.width ||
            (node.border.brush is SolidColor) != (border.brush is SolidColor)
        val drawChanged = geometryChanged ||
            node.backgroundColor != backgroundColor ||
            node.border != border ||
            node.kanaLayout !== kanaLayout ||
            node.romajiLayout !== romajiLayout ||
            node.kanaColor != kanaColor ||
            node.romajiColor != romajiColor
        val semanticsChanged = node.accessibilityLabel != accessibilityLabel ||
            node.interactive != interactive ||
            node.speakText != speakText ||
            node.cardAudioId != cardAudioId

        node.backgroundColor = backgroundColor
        node.border = border
        node.shape = shape
        node.kanaLayout = kanaLayout
        node.romajiLayout = romajiLayout
        node.kanaColor = kanaColor
        node.romajiColor = romajiColor
        node.interactive = interactive
        node.accessibilityLabel = accessibilityLabel
        node.speakText = speakText
        node.cardAudioId = cardAudioId
        node.onSpeak = onSpeak
        node.onHaptic = onHaptic

        if (!interactive) {
            node.resetInteractionImmediate()
        }
        if (geometryChanged) {
            node.invalidateCaches()
        }
        if (drawChanged) {
            node.invalidateDraw()
        }
        if (semanticsChanged) {
            node.invalidateSemantics()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "kanaCardSurface"
        properties["accessibilityLabel"] = accessibilityLabel
        properties["interactive"] = interactive
    }
}

private class KanaCardSurfaceNode(
    var backgroundColor: Color,
    var border: BorderStroke,
    var shape: Shape,
    var kanaLayout: TextLayoutResult,
    var romajiLayout: TextLayoutResult,
    var kanaColor: Color,
    var romajiColor: Color,
    var interactive: Boolean,
    var accessibilityLabel: String,
    var speakText: String,
    var cardAudioId: String,
    var onSpeak: (String, String) -> Unit,
    var onHaptic: () -> Unit
) : DrawModifierNode, PointerInputModifierNode, SemanticsModifierNode, Modifier.Node() {

    override val shouldAutoInvalidate: Boolean = false

    private var cachedWidth = -1
    private var cachedHeight = -1
    private var cachedStrokeBits = 0
    private var cachedLayoutDirection: LayoutDirection? = null
    private var cachedDensity = Float.NaN
    private var cachedShape: Shape? = null
    private var cachedHasSolidBorder = false

    private var fillMask: ImageBitmap? = null
    private var borderMask: ImageBitmap? = null
    private var fallbackFillOutline: Outline? = null
    private var fallbackStrokeOutline: Outline? = null
    private var fallbackStroke: Stroke? = null
    private var fallbackHalfStroke = 0f
    private var cachedBgColor = Color.Unspecified
    private var fillColorFilter: ColorFilter? = null
    private var cachedBorderColor = Color.Unspecified
    private var borderColorFilter: ColorFilter? = null
    private var clipPath: Path? = null

    private var pressProgressAnim: Animatable<Float, AnimationVector1D>? = null
    private var positionAnim: Animatable<Offset, AnimationVector2D>? = null
    private var startPosition = Offset.Zero
    private var activePointerId: PointerId? = null
    private var pressCancelled = false
    private var pressAnimJob: Job? = null
    private var positionAnimJob: Job? = null
    private var highlightShaderInitialized = false
    private var highlightRuntimeShader: android.graphics.RuntimeShader? = null
    private var highlightShaderBrush: ShaderBrush? = null

    fun invalidateCaches() {
        cachedWidth = -1
        cachedHeight = -1
    }

    fun resetInteractionImmediate() {
        val hadActiveAnim = activePointerId != null || pressProgressAnim != null
        activePointerId = null
        pressCancelled = false
        pressAnimJob?.cancel()
        positionAnimJob?.cancel()
        pressAnimJob = null
        positionAnimJob = null
        highlightShaderInitialized = false
        highlightRuntimeShader = null
        highlightShaderBrush = null
        if (hadActiveAnim) {
            pressProgressAnim = null
            positionAnim = null
            if (isAttached) {
                invalidateDraw()
            }
        }
    }

    override fun onDetach() {
        resetInteractionImmediate()
        pressAnimJob = null
        positionAnimJob = null
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        contentDescription = accessibilityLabel
        if (interactive) {
            role = Role.Button
            onClick {
                onHaptic()
                onSpeak(speakText, cardAudioId)
                true
            }
        }
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        if (!interactive) return

        if (pass == PointerEventPass.Main) {
            when (pointerEvent.type) {
                PointerEventType.Press -> {
                    if (activePointerId == null) {
                        val down = pointerEvent.changes.firstOrNull { !it.isConsumed && it.pressed } ?: return
                        activePointerId = down.id
                        pressCancelled = false
                        startPosition = down.position
                        val progressAnim = pressProgressAnim
                            ?: Animatable(0f, 0.001f).also { pressProgressAnim = it }
                        val posAnim = positionAnim
                            ?: Animatable(startPosition, Offset.VectorConverter, Offset.VisibilityThreshold)
                                .also { positionAnim = it }
                        pressAnimJob?.cancel()
                        positionAnimJob?.cancel()
                        pressAnimJob = coroutineScope.launch {
                            progressAnim.animateTo(1f, KanaPressSpec) {
                                invalidateDraw()
                            }
                        }
                        positionAnimJob = coroutineScope.launch {
                            posAnim.snapTo(startPosition)
                            invalidateDraw()
                        }
                        invalidateDraw()
                    }
                }
                PointerEventType.Move -> {
                    val pointerId = activePointerId ?: return
                    val change = pointerEvent.changes.firstOrNull { it.id == pointerId }
                    if (change == null || !change.pressed || change.isConsumed || !isInsideBounds(change.position, bounds)) {
                        cancelActivePress()
                    } else if (!pressCancelled) {
                        val pos = change.position
                        positionAnimJob?.cancel()
                        positionAnimJob = coroutineScope.launch {
                            positionAnim?.snapTo(pos)
                            invalidateDraw()
                        }
                    }
                }
                PointerEventType.Release -> {
                    val pointerId = activePointerId ?: return
                    val change = pointerEvent.changes.firstOrNull { it.id == pointerId }
                    if (change == null) {
                        releaseActivePress()
                    } else if (!change.pressed) {
                        val shouldClick = !pressCancelled &&
                            !change.isConsumed &&
                            isInsideBounds(change.position, bounds)
                        if (shouldClick) {
                            change.consume()
                            onHaptic()
                            onSpeak(speakText, cardAudioId)
                        }
                        releaseActivePress()
                    }
                }
                PointerEventType.Exit -> {
                    if (activePointerId != null) {
                        cancelActivePress()
                    }
                }
            }
        } else if (pass == PointerEventPass.Final && activePointerId != null) {
            val pointerId = activePointerId
            val change = pointerEvent.changes.firstOrNull { it.id == pointerId }
            if (change != null && change.isConsumed && !pressCancelled && pointerEvent.type == PointerEventType.Move) {
                cancelActivePress()
            }
        }
    }

    override fun onCancelPointerInput() {
        if (activePointerId != null) {
            cancelActivePress()
        }
    }

    private fun isInsideBounds(pos: Offset, bounds: IntSize): Boolean {
        return pos.x >= 0f && pos.y >= 0f && pos.x <= bounds.width.toFloat() && pos.y <= bounds.height.toFloat()
    }

    private fun cancelActivePress() {
        pressCancelled = true
        releaseActivePress()
    }

    private fun releaseActivePress() {
        activePointerId = null
        val progressAnim = pressProgressAnim
        val posAnim = positionAnim
        if (progressAnim != null) {
            pressAnimJob?.cancel()
            pressAnimJob = coroutineScope.launch {
                progressAnim.animateTo(0f, KanaPressSpec) {
                    invalidateDraw()
                }
            }
        }
        if (posAnim != null) {
            positionAnimJob?.cancel()
            positionAnimJob = coroutineScope.launch {
                posAnim.animateTo(startPosition, KanaPositionSpec) {
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val widthPx = size.width.roundToInt()
        val heightPx = size.height.roundToInt()
        if (widthPx <= 0 || heightPx <= 0) return

        val strokeWidthPx = if (border.width == Hairline) 1f else border.width.toPx()
        val strokeBits = strokeWidthPx.toRawBits()
        val solidBorderBrush = border.brush as? SolidColor
        val hasSolidBorder = solidBorderBrush != null
        val borderColor = solidBorderBrush?.value ?: Color.Transparent

        if (
            cachedWidth != widthPx ||
            cachedHeight != heightPx ||
            cachedStrokeBits != strokeBits ||
            cachedLayoutDirection != layoutDirection ||
            cachedDensity != density ||
            cachedShape != shape ||
            cachedHasSolidBorder != hasSolidBorder
        ) {
            cachedWidth = widthPx
            cachedHeight = heightPx
            cachedStrokeBits = strokeBits
            cachedLayoutDirection = layoutDirection
            cachedDensity = density
            cachedShape = shape
            cachedHasSolidBorder = hasSolidBorder

            val canUseMask = widthPx in 1..480 && heightPx in 1..480
            fillMask = if (canUseMask) {
                resolveShapeFillMask(shape, widthPx, heightPx, layoutDirection, this)
            } else {
                null
            }
            borderMask = if (canUseMask && strokeWidthPx > 0f && hasSolidBorder) {
                resolveShapeBorderMask(shape, widthPx, heightPx, strokeWidthPx, layoutDirection, this)
            } else {
                null
            }
            val fillOutline = shape.createOutline(size, layoutDirection, this@draw)
            fallbackFillOutline = if (fillMask == null) fillOutline else null
            if (borderMask == null && strokeWidthPx > 0f) {
                val halfStroke = strokeWidthPx / 2f
                val insetSize = Size(
                    width = (size.width - strokeWidthPx).coerceAtLeast(0f),
                    height = (size.height - strokeWidthPx).coerceAtLeast(0f)
                )
                fallbackHalfStroke = halfStroke
                fallbackStrokeOutline = if (insetSize.width > 0f && insetSize.height > 0f) {
                    shape.createOutline(insetSize, layoutDirection, this@draw)
                } else {
                    null
                }
                fallbackStroke = Stroke(width = strokeWidthPx)
            } else {
                fallbackHalfStroke = 0f
                fallbackStrokeOutline = null
                fallbackStroke = null
            }
            clipPath = Path().apply {
                addOutline(fillOutline)
            }
        }

        if (cachedBgColor != backgroundColor) {
            cachedBgColor = backgroundColor
            fillColorFilter = if (backgroundColor != Color.Transparent && backgroundColor.alpha > 0f) {
                ColorFilter.tint(backgroundColor)
            } else {
                null
            }
        }
        if (cachedBorderColor != borderColor) {
            cachedBorderColor = borderColor
            borderColorFilter = if (borderColor != Color.Transparent && borderColor.alpha > 0f) {
                ColorFilter.tint(borderColor)
            } else {
                null
            }
        }

        val progress = pressProgressAnim?.value ?: 0f
        if (progress > 0f) {
            val width = size.width
            val height = size.height
            val scale = lerp(1f, 1f + 4f.dp.toPx() / height, progress)
            val maxOffset = size.minDimension
            val currentPos = positionAnim?.value ?: startPosition
            val delta = currentPos - startPosition
            val tx = maxOffset * tanh(0.05f * delta.x / maxOffset)
            val ty = maxOffset * tanh(0.05f * delta.y / maxOffset)
            val maxDragScale = 4f.dp.toPx() / height
            val offsetAngle = atan2(delta.y, delta.x)
            val sx = scale + maxDragScale * abs(cos(offsetAngle) * delta.x / size.maxDimension) * (width / height).fastCoerceAtMost(1f)
            val sy = scale + maxDragScale * abs(sin(offsetAngle) * delta.y / size.maxDimension) * (height / width).fastCoerceAtMost(1f)

            withTransform({
                translate(left = tx, top = ty)
                scale(scaleX = sx, scaleY = sy, pivot = center)
            }) {
                drawCardVisuals(widthPx, heightPx)
                val highlightPath = clipPath
                if (highlightPath != null) {
                    clipPath(highlightPath) {
                        drawPressedHighlight(progress, currentPos)
                    }
                } else {
                    drawPressedHighlight(progress, currentPos)
                }
            }
        } else {
            drawCardVisuals(widthPx, heightPx)
        }
    }

    private fun DrawScope.drawCardVisuals(widthPx: Int, heightPx: Int) {
        val dstSize = IntSize(widthPx, heightPx)
        val fMask = fillMask
        val fFilter = fillColorFilter
        if (fMask != null && fFilter != null) {
            drawImage(image = fMask, dstSize = dstSize, colorFilter = fFilter)
        } else if (backgroundColor != Color.Transparent && backgroundColor.alpha > 0f) {
            fallbackFillOutline?.let { outline ->
                drawOutline(outline = outline, color = backgroundColor)
            }
        }
        val bMask = borderMask
        val bFilter = borderColorFilter
        if (bMask != null && bFilter != null) {
            drawImage(image = bMask, dstSize = dstSize, colorFilter = bFilter)
        } else {
            val strokeOutline = fallbackStrokeOutline
            val stroke = fallbackStroke
            val solidBrush = border.brush as? SolidColor
            val hasVisibleBorder = solidBrush == null ||
                (solidBrush.value != Color.Transparent && solidBrush.value.alpha > 0f)
            if (strokeOutline != null && stroke != null && hasVisibleBorder) {
                translate(left = fallbackHalfStroke, top = fallbackHalfStroke) {
                    drawOutline(
                        outline = strokeOutline,
                        brush = border.brush,
                        style = stroke
                    )
                }
            }
        }

        val gapPx = 2.dp.toPx()
        val totalHeight = kanaLayout.size.height + gapPx + romajiLayout.size.height
        val topY = (size.height - totalHeight) * 0.5f
        val kanaX = (size.width - kanaLayout.size.width) * 0.5f
        val romajiX = (size.width - romajiLayout.size.width) * 0.5f

        drawText(
            textLayoutResult = kanaLayout,
            color = kanaColor,
            topLeft = Offset(kanaX, topY)
        )
        drawText(
            textLayoutResult = romajiLayout,
            color = romajiColor,
            topLeft = Offset(romajiX, topY + kanaLayout.size.height + gapPx)
        )
    }

    private fun DrawScope.drawPressedHighlight(progress: Float, position: Offset) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!highlightShaderInitialized) {
                highlightShaderInitialized = true
                val shader = createInteractiveHighlightRuntimeShader()
                highlightRuntimeShader = shader
                highlightShaderBrush = shader?.let { ShaderBrush(it) }
            }
            val shader = highlightRuntimeShader
            val brush = highlightShaderBrush
            if (shader != null && brush != null) {
                drawRect(
                    color = Color.White.copy(alpha = 0.02f * progress),
                    blendMode = BlendMode.Plus
                )
                shader.setFloatUniform("size", size.width, size.height)
                shader.setColorUniform(
                    "color",
                    android.graphics.Color.valueOf(1f, 1f, 1f, 0.05f * progress).toArgb()
                )
                shader.setFloatUniform("radius", size.minDimension * 1.5f)
                shader.setFloatUniform(
                    "position",
                    position.x.fastCoerceIn(0f, size.width),
                    position.y.fastCoerceIn(0f, size.height)
                )
                drawRect(
                    brush = brush,
                    blendMode = BlendMode.Plus
                )
                return
            }
        }
        drawRect(
            color = Color.White.copy(alpha = 0.03f * progress),
            blendMode = BlendMode.Plus
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


