package com.jian.nemo.feature.library.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.NotoSerifJP
import com.jian.nemo.core.designsystem.theme.NotoSerifSC
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.common.NemoNativeLiquidMenuTrigger
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBar
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBarStyle
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryOption
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.menu.records.GlassMenuItemRecord
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.library.R
import com.jian.nemo.feature.library.presentation.list.StudyFilter
import com.jian.nemo.feature.library.presentation.list.WordListUiState
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val LibraryLevelsOrder = listOf("N5", "N4", "N3", "N2", "N1")
private val JapaneseSpanStyle = SpanStyle(fontFamily = NotoSerifJP, localeList = LocaleList("ja"))
private val ChineseSpanStyle = SpanStyle(fontFamily = NotoSerifSC, localeList = LocaleList("zh"))

/**
 * 词库列表通用 56dp 磨砂回顶悬浮按钮。
 */
@Composable
fun BoxScope.LibraryScrollToTopButton(
    lazyListState: LazyListState,
    backdrop: Backdrop?,
    isMenuExpanded: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember(lazyListState) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 6
        }
    }
    val layoutDirection = LocalLayoutDirection.current
    val horizontalSafePadding = WindowInsets.displayCutout.union(WindowInsets.navigationBars)
        .asPaddingValues()
        .calculateEndPadding(layoutDirection)

    AnimatedVisibility(
        visible = showScrollToTop && !isMenuExpanded,
        enter = fadeIn(spring()) + scaleIn(spring(dampingRatio = 0.75f)),
        exit = fadeOut(tween(150)) + scaleOut(tween(150)),
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(end = horizontalSafePadding + 20.dp, bottom = bottomPadding)
    ) {
        FrostedLiquidGlassButton(
            onClick = {
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            },
            shape = CircleShape,
            backdrop = backdrop,
            hasLens = backdrop != null,
            contentPadding = PaddingValues(0.dp),
            isInteractive = true,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                painter = painterResource(id = com.jian.nemo.core.ui.R.drawable.ic_scroll_to_top),
                contentDescription = stringResource(R.string.lib_components_scroll_to_top_cd),
                modifier = Modifier.size(35.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 统计各等级词条总数，自动排除 "ALL" 汇总键以防重复累加。
 */
internal fun computeAllLevelsCount(countsByLevel: Map<String, Int>): Int {
    var total = 0
    for ((key, value) in countsByLevel) {
        if (!key.equals(WordListUiState.LEVEL_ALL, ignoreCase = true)) {
            total += value
        }
    }
    return total
}

@Composable
private fun rememberLibraryLevelMenuItems(
    selectedLevel: String,
    countsByLevel: Map<String, Int>
): List<GlassMenuItemRecord> {
    val allCount = remember(countsByLevel) { computeAllLevelsCount(countsByLevel) }
    val isAllSelected = selectedLevel.equals(WordListUiState.LEVEL_ALL, ignoreCase = true)
    val allLevelsTitle = stringResource(R.string.lib_components_all_levels_count_format, allCount)
    val n5Title = stringResource(R.string.lib_components_level_count_format, "N5", countsByLevel["N5"] ?: 0)
    val n4Title = stringResource(R.string.lib_components_level_count_format, "N4", countsByLevel["N4"] ?: 0)
    val n3Title = stringResource(R.string.lib_components_level_count_format, "N3", countsByLevel["N3"] ?: 0)
    val n2Title = stringResource(R.string.lib_components_level_count_format, "N2", countsByLevel["N2"] ?: 0)
    val n1Title = stringResource(R.string.lib_components_level_count_format, "N1", countsByLevel["N1"] ?: 0)

    return remember(selectedLevel, allLevelsTitle, n5Title, n4Title, n3Title, n2Title, n1Title) {
        val titleMap = mapOf(
            "N5" to n5Title,
            "N4" to n4Title,
            "N3" to n3Title,
            "N2" to n2Title,
            "N1" to n1Title
        )
        buildList(LibraryLevelsOrder.size + 2) {
            add(
                GlassMenuItemRecord(
                    id = WordListUiState.LEVEL_ALL,
                    title = allLevelsTitle,
                    icon = "grid",
                    checked = isAllSelected,
                    iconColor = SystemColors.Indigo.toArgb()
                )
            )
            add(GlassMenuItemRecord(separator = true))
            LibraryLevelsOrder.forEach { level ->
                add(
                    GlassMenuItemRecord(
                        id = level,
                        title = titleMap[level] ?: level,
                        icon = "filter",
                        checked = selectedLevel.equals(level, ignoreCase = true),
                        iconColor = getLevelColor(level).toArgb()
                    )
                )
            }
        }
    }
}

/**
 * 48dp 词库等级筛选原生液态触发按钮。
 */
@Composable
fun LibraryLevelSelectionButton(
    selectedLevel: String,
    countsByLevel: Map<String, Int>,
    onLevelSelected: (String) -> Unit,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    buttonSize: Dp = 48.dp,
    content: (@Composable () -> Unit)? = null
) {
    val items = rememberLibraryLevelMenuItems(
        selectedLevel = selectedLevel,
        countsByLevel = countsByLevel
    )

    NemoNativeLiquidMenuTrigger(
        items = items,
        modifier = modifier,
        buttonSize = buttonSize,
        colorScheme = if (isDark) "dark" else "light",
        onSelect = { item, _ ->
            onLevelSelected(item.id)
            onExpandedChange(false)
            onScrollToTop()
        },
        onOpenChange = { open ->
            if (open != isExpanded) {
                onExpandedChange(open)
            }
        },
        content = content
    )
}

val AvatarPalette = listOf(
    SystemColors.Blue,
    SystemColors.Orange,
    SystemColors.Green,
    SystemColors.Indigo,
    SystemColors.Teal,
    SystemColors.Purple,
    SystemColors.Pink,
    SystemColors.Cyan
)

fun getLevelColor(level: String): Color {
    return when (level.uppercase()) {
        "N5" -> SystemColors.Green
        "N4" -> SystemColors.Cyan
        "N3" -> SystemColors.Blue
        "N2" -> SystemColors.Orange
        "N1" -> SystemColors.Pink
        else -> SystemColors.Blue
    }
}

/**
 * 词库列表通用 22dp G2 连续平滑圆角卡片容器。
 */
@Composable
fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White

    if (onClick == null) {
        Surface(
            shape = RoundedRectangle(22.dp),
            color = containerColor,
            modifier = modifier
                .fillMaxWidth()
                .softCardShadow(borderRadius = 22.dp, isDark = isDark),
            content = { Column(modifier = Modifier.padding(16.dp), content = content) }
        )
        return
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        label = "cardScale",
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
    )

    Surface(
        onClick = onClick,
        shape = RoundedRectangle(22.dp),
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 22.dp, isDark = isDark)
            .graphicsLayer {
                val s = scaleState.value
                scaleX = s
                scaleY = s
            },
        interactionSource = interactionSource,
        content = { Column(modifier = Modifier.padding(16.dp), content = content) }
    )
}

/**
 * 词库列表通用双轨单容器单词卡片列表项。
 */
@Composable
fun WordListItemPremium(
    word: Word,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSoftTranslucent: Boolean = LocalSettingIconStyle.current == SettingIconStyle.SOFT_TRANSLUCENT,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val colorIndex = (word.id.hashCode() and Int.MAX_VALUE) % AvatarPalette.size
    val avatarColor = AvatarPalette[colorIndex]
    val containerBg = if (isSoftTranslucent) avatarColor.copy(alpha = 0.15f) else avatarColor
    val contentColor = if (isSoftTranslucent) avatarColor else Color.White

    PremiumCard(
        onClick = onClick,
        modifier = modifier,
        isDark = isDark
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(containerBg, RoundedRectangle(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = word.japanese.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NotoSerifJP,
                        localeList = LocaleList("ja")
                    ),
                    fontWeight = FontWeight.W900,
                    color = contentColor
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.japanese,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = NotoSerifJP,
                        localeList = LocaleList("ja")
                    ),
                    fontWeight = FontWeight.W800,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val secondaryAnnotatedText = remember(word.hiragana, word.chinese) {
                    buildAnnotatedString {
                        if (word.hiragana.isNotEmpty()) {
                            withStyle(JapaneseSpanStyle) {
                                append(word.hiragana)
                            }
                        }
                        if (word.hiragana.isNotEmpty() && word.chinese.isNotEmpty()) {
                            append(" · ")
                        }
                        if (word.chinese.isNotEmpty()) {
                            withStyle(ChineseSpanStyle) {
                                append(word.chinese)
                            }
                        }
                    }
                }
                Text(
                    text = secondaryAnnotatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (word.level.isNotBlank()) {
                val levelColor = getLevelColor(word.level)
                val levelBg = if (isSoftTranslucent) levelColor.copy(alpha = 0.15f) else levelColor
                val levelText = if (isSoftTranslucent) levelColor else Color.White
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedRectangle(8.dp))
                        .background(levelBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = word.level.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        color = levelText
                    )
                }
            }
        }
    }
}

/**
 * 词库列表通用情感化空状态视图（保持 100dp 容器与 56dp 图标尺寸）。
 */
@Composable
fun LibraryEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.AutoMirrored.Rounded.MenuBook,
    accentColor: Color = SystemColors.Indigo,
    isSoftTranslucent: Boolean = LocalSettingIconStyle.current == SettingIconStyle.SOFT_TRANSLUCENT
) {
    val containerBg = if (isSoftTranslucent) accentColor.copy(alpha = 0.15f) else accentColor
    val iconTint = if (isSoftTranslucent) accentColor else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(containerBg, RoundedRectangle(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = iconTint
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 按连续假名与非假名段落合并样式构建富文本，避免逐字符分配 SpanStyle。
 */
fun formatMixedCnHnString(text: String): AnnotatedString {
    if (text.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        var runStart = 0
        var runIsKana = isKanaCodePoint(text.codePointAt(0))
        var index = Character.charCount(text.codePointAt(0))
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            val charCount = Character.charCount(codePoint)
            val currentIsKana = isKanaCodePoint(codePoint)
            if (currentIsKana != runIsKana) {
                withStyle(if (runIsKana) JapaneseSpanStyle else ChineseSpanStyle) {
                    append(text, runStart, index)
                }
                runStart = index
                runIsKana = currentIsKana
            }
            index += charCount
        }
        withStyle(if (runIsKana) JapaneseSpanStyle else ChineseSpanStyle) {
            append(text, runStart, text.length)
        }
    }
}

private fun isKanaCodePoint(codePoint: Int): Boolean {
    return (codePoint in 0x3040..0x309F) || (codePoint in 0x30A0..0x30FF)
}

/**
 * 词库列表通用液体弹性折叠顶栏组件。
 */
@Composable
fun CollapsibleLibraryHeader(
    title: String,
    onBack: () -> Unit,
    lazyListState: LazyListState,
    selectedLevel: String,
    countsByLevel: Map<String, Int>,
    onLevelSelected: (String) -> Unit,
    onScrollToTop: () -> Unit,
    isMenuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    backdrop: Backdrop?,
    selectedFilter: StudyFilter,
    onFilterSelected: (StudyFilter) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f,
    backgroundColor: Color = MaterialTheme.colorScheme.background
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val horizontalInsets = WindowInsets.displayCutout.union(WindowInsets.navigationBars).asPaddingValues()
    val safeStart = horizontalInsets.calculateStartPadding(layoutDirection)
    val safeEnd = horizontalInsets.calculateEndPadding(layoutDirection)
    val collapseThresholdPx = with(density) { 50.dp.toPx() }

    val isListScrolled by remember(lazyListState, collapseThresholdPx) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
                lazyListState.firstVisibleItemScrollOffset > collapseThresholdPx
        }
    }

    var wasCollapsedBeforeSearch by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(searchQuery.isEmpty(), isListScrolled) {
        if (searchQuery.isEmpty() && wasCollapsedBeforeSearch != isListScrolled) {
            wasCollapsedBeforeSearch = isListScrolled
        }
    }

    val isCollapsed = if (searchQuery.isNotEmpty()) {
        wasCollapsedBeforeSearch || isListScrolled
    } else {
        isListScrolled
    }

    val collapseProgressState = animateFloatAsState(
        targetValue = if (isCollapsed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = 420f
        ),
        label = "headerCollapseProgress"
    )

    val gradientStops = remember(backgroundColor) {
        arrayOf(
            0.00f to backgroundColor.copy(alpha = 0.98f),
            0.42f to backgroundColor.copy(alpha = 0.95f),
            0.72f to backgroundColor.copy(alpha = 0.75f),
            0.88f to backgroundColor.copy(alpha = 0.35f),
            1.00f to backgroundColor.copy(alpha = 0f)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val maxBgHeight = (statusBarTop + 128.dp).toPx().coerceAtLeast(1f)
                val cachedBrush = Brush.verticalGradient(
                    colorStops = gradientStops,
                    startY = 0f,
                    endY = maxBgHeight
                )
                onDrawBehind {
                    val p = collapseProgressState.value
                    val currentBgHeight = (statusBarTop + lerp(128.dp, 72.dp, p)).toPx()
                    val scaleY = (currentBgHeight / maxBgHeight).coerceAtLeast(0f)
                    scale(scaleX = 1f, scaleY = scaleY, pivot = Offset.Zero) {
                        drawRect(
                            brush = cachedBrush,
                            size = Size(size.width, maxBgHeight)
                        )
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .padding(start = safeStart + 20.dp, top = statusBarTop + 6.dp)
                .align(Alignment.TopStart)
        ) {
            FrostedLiquidGlassButton(
                onClick = onBack,
                backdrop = backdrop,
                shape = CircleShape,
                hasLens = backdrop != null,
                chromaticAberration = false,
                contentPadding = PaddingValues(0.dp),
                isInteractive = true,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(com.jian.nemo.core.ui.R.string.core_common_btn_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(end = safeEnd + 20.dp, top = statusBarTop + 6.dp)
                .align(Alignment.TopEnd)
        ) {
            LibraryLevelSelectionButton(
                selectedLevel = selectedLevel,
                countsByLevel = countsByLevel,
                onLevelSelected = onLevelSelected,
                isExpanded = isMenuExpanded,
                onExpandedChange = onMenuExpandedChange,
                onScrollToTop = onScrollToTop,
                isDark = isDark,
                buttonSize = 48.dp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarTop + 6.dp, start = safeStart + 76.dp, end = safeEnd + 76.dp)
                .height(48.dp)
                .graphicsLayer {
                    val p = collapseProgressState.value
                    alpha = (1f - p * 2.5f).coerceIn(0f, 1f)
                    val s = 1f - 0.12f * p
                    scaleX = s
                    scaleY = s
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = statusBarTop)
                .layout { measurable, constraints ->
                    val p = collapseProgressState.value
                    val scale = lerp(1f, 48f / 56f, p)
                    val fullWidthPx = constraints.maxWidth
                    val safeHorizontalPx = maxOf(safeStart.roundToPx(), safeEnd.roundToPx())
                    val expandedPadPx = safeHorizontalPx + 16.dp.roundToPx()
                    val collapsedVisualPadPx = safeHorizontalPx + 74.dp.roundToPx()
                    val targetVisualWidthPx = lerp(
                        (fullWidthPx - expandedPadPx * 2).toFloat(),
                        (fullWidthPx - collapsedVisualPadPx * 2).toFloat(),
                        p
                    ).coerceAtLeast(0f)
                    val logicalWidthPx = (targetVisualWidthPx / scale).roundToInt().coerceIn(0, fullWidthPx)
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = logicalWidthPx, maxWidth = logicalWidthPx)
                    )
                    val offsetYPx = lerp(60.dp.toPx(), 2.dp.toPx(), p).roundToInt()
                    val offsetXPx = (fullWidthPx - placeable.width) / 2
                    layout(fullWidthPx, (60.dp.toPx() + 56.dp.toPx()).roundToInt()) {
                        placeable.placeRelativeWithLayer(offsetXPx, offsetYPx) {
                            scaleX = scale
                            scaleY = scale
                        }
                    }
                }
        ) {
            LibraryDiscoveryBar(
                selectedFilter = selectedFilter,
                onFilterSelected = onFilterSelected,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                searchPlaceholder = searchPlaceholder,
                collapseProgressProvider = { collapseProgressState.value },
                backdrop = backdrop,
                isDark = isDark,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 词库列表通用搜索与分类胶囊栏。
 */
@Composable
fun LibraryDiscoveryBar(
    selectedFilter: StudyFilter,
    onFilterSelected: (StudyFilter) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    modifier: Modifier = Modifier,
    collapseProgress: Float = 0f,
    collapseProgressProvider: (() -> Float)? = null,
    backdrop: Backdrop? = null,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val tabAllText = stringResource(R.string.lib_word_list_tab_all)
    val tabLearnedText = stringResource(R.string.lib_word_list_tab_learned)
    val tabUnlearnedText = stringResource(R.string.lib_word_list_tab_unlearned)

    val options = remember(tabAllText, tabLearnedText, tabUnlearnedText) {
        listOf(
            DiscoveryOption(
                label = tabAllText,
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                activeColor = SystemColors.Indigo,
            ),
            DiscoveryOption(
                label = tabLearnedText,
                icon = Icons.Rounded.CheckCircle,
                activeColor = SystemColors.Green,
            ),
            DiscoveryOption(
                label = tabUnlearnedText,
                icon = Icons.Rounded.HourglassEmpty,
                activeColor = SystemColors.Orange,
            ),
        )
    }

    val selectedIndex = when (selectedFilter) {
        StudyFilter.ALL -> 0
        StudyFilter.LEARNED -> 1
        StudyFilter.UNLEARNED -> 2
    }

    DiscoveryBar(
        options = options,
        selectedOptionIndex = selectedIndex,
        onOptionSelected = { index ->
            val newFilter = when (index) {
                0 -> StudyFilter.ALL
                1 -> StudyFilter.LEARNED
                2 -> StudyFilter.UNLEARNED
                else -> StudyFilter.ALL
            }
            onFilterSelected(newFilter)
        },
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        searchPlaceholder = searchPlaceholder,
        backdrop = backdrop,
        collapseProgress = collapseProgress,
        collapseProgressProvider = collapseProgressProvider,
        style = DiscoveryBarStyle(
            height = 56.dp,
            iconSize = 20.dp,
            itemSpacing = 8.dp,
            indicatorPadding = 4.dp,
            searchIconSize = 35.dp,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            activeTextStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            ),
            backgroundColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White,
            inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    )
}

private val SkeletonTitleFractions = listOf(0.38f, 0.52f, 0.34f, 0.46f, 0.42f, 0.50f, 0.36f, 0.44f, 0.40f, 0.48f)
private val SkeletonSubtitleFractions = listOf(0.68f, 0.56f, 0.72f, 0.62f, 0.70f, 0.58f, 0.74f, 0.64f, 0.66f, 0.60f)

/**
 * 词库列表静态卡片骨架占位。
 */
@Composable
fun LibraryItemSkeletonCard(
    index: Int,
    modifier: Modifier = Modifier,
    isDark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val primarySkeletonColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isDark) 0.08f else 0.06f
    )
    val secondarySkeletonColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isDark) 0.055f else 0.04f
    )
    val titleFraction = SkeletonTitleFractions[index % SkeletonTitleFractions.size]
    val subtitleFraction = SkeletonSubtitleFractions[index % SkeletonSubtitleFractions.size]

    PremiumCard(
        onClick = null,
        modifier = modifier,
        isDark = isDark
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(primarySkeletonColor, RoundedRectangle(16.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(titleFraction)
                        .height(18.dp)
                        .background(primarySkeletonColor, RoundedRectangle(6.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(subtitleFraction)
                        .height(14.dp)
                        .background(secondarySkeletonColor, RoundedRectangle(5.dp))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(34.dp)
                    .height(22.dp)
                    .background(secondarySkeletonColor, RoundedRectangle(8.dp))
            )
        }
    }
}
