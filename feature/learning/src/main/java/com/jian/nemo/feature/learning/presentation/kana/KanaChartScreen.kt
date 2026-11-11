package com.jian.nemo.feature.learning.presentation.kana

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
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
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jian.nemo.core.designsystem.theme.SystemColors
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
    val iconStyle by viewModel.iconStyle.collectAsState()

    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

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

    val scope = rememberCoroutineScope()
    val playingAudioId by viewModel.playingAudioId.collectAsState()

    val sectionOrder = remember { mapOf("seion" to 0, "dakuon" to 1, "yoon" to 2) }
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val effectiveBackdrop = backdrop

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
                val (data, titleRes, descRes) = when (section) {
                    "dakuon" -> Triple(dakuonData, R.string.kana_section_dakuon, R.string.kana_desc_dakuon)
                    "yoon" -> Triple(yoonData, R.string.kana_section_yoon, R.string.kana_desc_yoon)
                    else -> Triple(seionData, R.string.kana_section_seion, null)
                }
                val rows = remember(data) { data.chunked(5) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "top_spacer") {
                        Spacer(modifier = Modifier.height(statusBarTop + 60.dp + 16.dp))
                    }

                    item(key = "title") {
                        SectionTitle(
                            text = stringResource(titleRes),
                            accentColor = accentColor,
                            textMain = textMain
                        )
                    }

                    if (descRes != null) {
                        item(key = "desc") {
                            SectionSubtitle(text = stringResource(descRes), textSub = textSub)
                        }
                    }

                    itemsIndexed(
                        items = rows,
                        key = { index, _ -> "${section}_row_$index" }
                    ) { rowIndex, row ->
                        val rowColor = getCategoryRowColor(rowIndex, isDark)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { cell ->
                                KanaCard(
                                    modifier = Modifier
                                        .weight(1f)
                                        .widthIn(min = 0.dp),
                                    cell = cell,
                                    isKatakana = isKatakana,
                                    rowColor = rowColor,
                                    isDark = isDark,
                                    playingAudioId = playingAudioId,
                                    onSpeak = { speakText, id -> viewModel.speakKana(speakText, id) },
                                    onHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                                )
                            }
                            repeat(5 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(88.dp))
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
                title = stringResource(R.string.kana_chart_title),
                onBack = onNavigateBack,
                backgroundColor = Color.Transparent,
                backdrop = effectiveBackdrop,
                backButtonSize = 48.dp,
                actions = {
                    val toggleFg = if (currentType == 0) {
                        if (isDark) SystemColors.IndigoDark else SystemColors.Indigo
                    } else {
                        if (isDark) SystemColors.Pink else SystemColors.Pink
                    }
                    FrostedLiquidGlassButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentType = if (currentType == 0) 1 else 0
                        },
                        backdrop = effectiveBackdrop,
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
                    backdrop = effectiveBackdrop,
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
private fun SectionSubtitle(text: String, textSub: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = textSub,
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 2.dp)
    )
}


@Composable
private fun KanaCard(
    modifier: Modifier = Modifier,
    cell: KanaCell?,
    isKatakana: Boolean,
    rowColor: Color,
    isDark: Boolean,
    playingAudioId: String?,
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

    val solidTextColor = Color.White
    val solidSubTextColor = Color.White.copy(alpha = 0.85f)

    val playingBorderColor = Color.White
    val defaultBorderColor = if (cell.isPlaceholder) {
        rowColor.copy(alpha = if (isDark) 0.40f else 0.25f)
    } else {
        rowColor.copy(alpha = if (isDark) 0.30f else 0.12f)
    }

    val borderColor by animateColorAsState(
        targetValue = if (isPlaying) playingBorderColor else defaultBorderColor,
        animationSpec = tween(180),
        label = "cardPlayingBorder"
    )

    val cardShape = RoundedRectangle(20.dp)
    val cardBorder = BorderStroke(if (isPlaying) 2.dp else 1.dp, borderColor)

    if (cell.isPlaceholder) {
        val placeholderBg = rowColor.copy(alpha = if (isDark) 0.15f else 0.10f)
        val placeholderTextColor = if (isDark) Color.White.copy(alpha = 0.50f) else rowColor.copy(alpha = 0.70f)
        val placeholderSubTextColor = if (isDark) Color.White.copy(alpha = 0.35f) else rowColor.copy(alpha = 0.50f)

        Surface(
            modifier = modifier.aspectRatio(1f),
            shape = cardShape,
            color = placeholderBg,
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
                    color = placeholderTextColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cell.romaji,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = placeholderSubTextColor
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
            modifier = modifier.aspectRatio(1f),
            backgroundColor = rowColor,
            shape = cardShape,
            border = cardBorder,
            elevation = 0.dp,
            useSoftShadow = false,
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
                    color = solidTextColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cell.romaji,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = solidSubTextColor
                )
            }
        }
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


