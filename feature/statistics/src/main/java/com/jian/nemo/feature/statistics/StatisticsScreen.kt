package com.jian.nemo.feature.statistics

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.common.LiquidScrollToTopButton
import androidx.compose.foundation.lazy.rememberLazyListState
import com.jian.nemo.core.ui.navigation.DetailContextHolder
import com.jian.nemo.feature.statistics.model.StatisticDisplayItem
import com.jian.nemo.feature.statistics.model.StatisticSource

import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

/**
 * 今日统计界面 (UI/UX Pro Max)
 *
 * 显示今天学习的单词和语法列表
 * 优化：
 * 1. 支持列表折叠
 * 2. 动态多彩头像 (Dynamic Avatar) - 解决视觉单一问题
 */
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    onNavigateToWordDetail: (Int) -> Unit,
    onNavigateToGrammarDetail: (Int) -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val words = uiState.todaysWords
    val grammars = uiState.todaysGrammars

    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val listState = rememberLazyListState()
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
        // 1. 底层滚动内容（被 backdrop 采样源录制）
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = statusBarTop + 60.dp + 16.dp,
                bottom = navBarBottom + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 0. 顶部统计面板
            item {
                OverallStatisticsDashboard(words = words, grammars = grammars)
            }

            // 1. 单词列表
            item {
                Column {
                    StatisticsSectionTitle("单词 (${words.size})")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (words.isNotEmpty()) {
                        StatisticsListCard(
                            items = words,
                            onItemClick = { wordId ->
                                DetailContextHolder.setWordContext(words.map { it.id })
                                onNavigateToWordDetail(wordId)
                            },
                            isWord = true
                        )
                    } else {
                        EmptyStatisticsState(
                            message = "该日期没有学习任何单词",
                            subtitle = "完成新学或复习后会在此展示",
                            icon = Icons.Rounded.Translate,
                            plumpIcon = PlumpIcon.Languages,
                            accentColor = NemoSecondary,
                            isoluminantColor = IsoluminantPalette.EmeraldGreen
                        )
                    }
                }
            }

            // 2. 语法列表
            item {
                Column {
                    StatisticsSectionTitle("语法 (${grammars.size})")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (grammars.isNotEmpty()) {
                        StatisticsListCard(
                            items = grammars,
                            onItemClick = { grammarId ->
                                DetailContextHolder.setGrammarContext(grammars.map { it.id })
                                onNavigateToGrammarDetail(grammarId)
                            },
                            isWord = false
                        )
                    } else {
                        EmptyStatisticsState(
                            message = "该日期没有学习任何语法",
                            subtitle = "完成语法学习后会在此展示",
                            icon = Icons.Rounded.Checklist,
                            plumpIcon = PlumpIcon.ListChecks,
                            accentColor = NemoSecondary,
                            isoluminantColor = IsoluminantPalette.EmeraldGreen
                        )
                    }
                }
            }
        }

        // 2. 悬浮回顶按钮（单词列表同款液态玻璃回顶）
        LiquidScrollToTopButton(
            listState = listState,
            backdrop = backdrop,
            bottomPadding = navBarBottom + 20.dp
        )

        // 3. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
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
                title = "今日统计",
                onBack = onBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp
            )
        }
    }
}
}

// 预定义的一组高级柔和色彩，用于循环显示
// 使用语义化颜色定义，避免硬编码
private val AvatarColors = listOf(
    NemoPrimary,   // Blue
    NemoOrange,    // Orange
    NemoSecondary, // Green
    NemoIndigo,    // Indigo
    NemoTeal,      // Teal
    NemoPurple,    // Violet/Purple
    IosColors.Pink, // Pink
    NemoCyan       // Cyan
)

private val PlumpAvatarColors = listOf(
    IsoluminantPalette.SkyIndigo,     // 255°
    IsoluminantPalette.LakesideCyan,  // 195°
    IsoluminantPalette.EmeraldGreen,  // 145°
    IsoluminantPalette.WarmAmber,     // 70°
    IsoluminantPalette.CoralFlame,    // 30°
    IsoluminantPalette.RoseRuby,      // 15°
    IsoluminantPalette.LilacViolet,   // 295°
    IsoluminantPalette.GlazeMint      // 175°
)

@Composable
fun StatisticsSectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * 可折叠的统计列表卡片
 */
@Composable
fun StatisticsListCard(
    items: List<StatisticDisplayItem>,
    onItemClick: (Int) -> Unit,
    isWord: Boolean,
    showSourceBadge: Boolean = true
) {
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
    val avatarColors = if (isPlumpDuo) PlumpAvatarColors else AvatarColors
    val defaultShowCount = 5
    val pageSize = 30
    var visibleCount by remember(items) { mutableStateOf(defaultShowCount) }

    // 如果数量少于等于 defaultShowCount + 1，直接全部显示，避免出现"展开查看剩余 1 项"的尴尬
    val shouldCollapse = items.size > defaultShowCount + 1
    val showItems = if (!shouldCollapse) items else items.take(visibleCount)
    val remainingCount = items.size - visibleCount

    PremiumCard {
        Column(
            modifier = Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            showItems.forEachIndexed { index, item ->
                // 根据索引循环获取颜色
                val color = avatarColors[index % avatarColors.size]

                key(item.id) {
                    StatisticsItemRow(
                        item = item,
                        avatarColor = color,
                        onClick = { onItemClick(item.id) },
                        showDivider = index < showItems.size - 1,
                        showSourceBadge = showSourceBadge,
                        isWord = isWord
                    )
                }
            }

            // 展开/收起 按钮
            if (shouldCollapse) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                     color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
                     thickness = 0.5.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (visibleCount < items.size) {
                                visibleCount = minOf(items.size, visibleCount + pageSize)
                            } else {
                                visibleCount = defaultShowCount
                            }
                        }
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isFullyExpanded = visibleCount >= items.size
                    val arrowTint = if (isPlumpDuo) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    val textColor = if (isPlumpDuo) {
                        IsoluminantPalette.SkyIndigo
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isFullyExpanded) "收起" else "展开查看更多 (剩余 $remainingCount 项)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isFullyExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = arrowTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStatisticsState(
    message: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Inbox,
    plumpIcon: PlumpIcon? = null,
    accentColor: Color = NemoSecondary,
    isoluminantColor: Color = IsoluminantPalette.EmeraldGreen
) {
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO && plumpIcon != null
    val effectiveColor = if (isPlumpDuo) isoluminantColor else accentColor

    PremiumCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isPlumpDuo) {
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PlumpDuoIcon(
                            icon = plumpIcon,
                            color = effectiveColor,
                            size = 48.dp
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = accentColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accentColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticsItemRow(
    item: StatisticDisplayItem,
    avatarColor: Color,
    onClick: () -> Unit,
    showDivider: Boolean,
    showSourceBadge: Boolean = true,
    isWord: Boolean = true
) {
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dynamic Text Avatar
        // Prefer first character of Japanese text
        val avatarChar = item.japanese.firstOrNull()?.toString() ?: "?"
        val avatarBgAlpha = if (isDark) 0.15f else (if (isPlumpDuo) 0.08f else 0.1f)

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(avatarColor.copy(alpha = avatarBgAlpha), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarChar,
                style = MaterialTheme.typography.titleLarge.copy(
                    localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                ),
                fontSize = 20.sp,
                fontFamily = NotoSerifJP,
                fontWeight = FontWeight.W900,
                color = avatarColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val badgeInfo = remember(item.source, primaryColor, isPlumpDuo) {
                when (item.source) {
                    StatisticSource.LEARNED -> {
                        val color = if (isPlumpDuo) IsoluminantPalette.SkyIndigo else primaryColor
                        Triple(
                            "新学",
                            color.copy(alpha = 0.12f),
                            color
                        )
                    }
                    StatisticSource.REVIEWED -> {
                        val color = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else NemoSecondary
                        Triple(
                            "复习",
                            color.copy(alpha = 0.15f),
                            color
                        )
                    }
                }
            }
            val (badgeText, badgeBg, badgeTextColor) = badgeInfo

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSourceBadge) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (item.level.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.level,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (isWord) {
                    Text(
                        text = item.japanese,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NotoSerifJP,
                            localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                        ),
                        fontWeight = FontWeight.W800,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = formatMixedCnHnString(item.japanese),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W800,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Construct meaningful secondary text
            val secondaryAnnotatedText = remember(item.hiragana, item.chinese) {
                buildAnnotatedString {
                    if (item.hiragana.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                fontFamily = NotoSerifJP,
                                localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                            )
                        ) {
                            append(item.hiragana)
                        }
                    }
                    if (item.hiragana.isNotEmpty() && item.chinese.isNotEmpty()) {
                        append(" · ")
                    }
                    if (item.chinese.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                fontFamily = NotoSerifSC,
                                localeList = androidx.compose.ui.text.intl.LocaleList("zh")
                            )
                        ) {
                            append(item.chinese)
                        }
                    }
                }
            }

            if (item.hiragana.isNotEmpty() || item.chinese.isNotEmpty()) {
                Text(
                    text = secondaryAnnotatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }

    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(start = 64.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
            thickness = 0.5.dp
        )
    }
}

/**
 * Premium Card (Internal definition to match screen style exactly)
 */
@Composable
private fun PremiumCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale by if(onClick != null) {
        val isPressed by interactionSource.collectIsPressedAsState()
         animateFloatAsState(
            targetValue = if (isPressed) 0.98f else 1f,
            label = "cardScale",
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = MaterialTheme.colorScheme.surface
    val borderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    val shadowElevation = if (isDark) 2.dp else 10.dp
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.04f)

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 26.dp, isDark = isDark)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        interactionSource = interactionSource,
        content = { Column(modifier = Modifier.padding(20.dp), content = content) }
    )
}

private fun formatMixedCnHnString(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)
            val nextStr = text.substring(i, i + charCount)
            val isKana = nextStr.any { c ->
                (c in '\u3040'..'\u309F') || (c in '\u30A0'..'\u30FF')
            }
            if (isKana) {
                withStyle(
                    SpanStyle(
                        fontFamily = NotoSerifJP,
                        localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                    )
                ) {
                    append(nextStr)
                }
            } else {
                withStyle(
                    SpanStyle(
                        fontFamily = NotoSerifSC,
                        localeList = androidx.compose.ui.text.intl.LocaleList("zh")
                    )
                ) {
                    append(nextStr)
                }
            }
            i += charCount
        }
    }
}

@Composable
private fun OverallStatisticsDashboard(
    words: List<StatisticDisplayItem>,
    grammars: List<StatisticDisplayItem>
) {
    val newWords = words.count { it.source == StatisticSource.LEARNED }
    val reviewWords = words.count { it.source == StatisticSource.REVIEWED }
    val newGrammars = grammars.count { it.source == StatisticSource.LEARNED }
    val reviewGrammars = grammars.count { it.source == StatisticSource.REVIEWED }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "今日新学 (词)",
                count = newWords,
                color = NemoPrimary,
                icon = Icons.Rounded.DoneAll,
                plumpIcon = PlumpIcon.BadgeCheck,
                isoluminantColor = IsoluminantPalette.SkyIndigo,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "今日复习 (词)",
                count = reviewWords,
                color = NemoSecondary,
                icon = Icons.Rounded.Layers,
                plumpIcon = PlumpIcon.GrammarLayers,
                isoluminantColor = IsoluminantPalette.EmeraldGreen,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "今日新学 (语法)",
                count = newGrammars,
                color = NemoOrange,
                icon = Icons.Rounded.DoneAll,
                plumpIcon = PlumpIcon.BadgeCheck,
                isoluminantColor = IsoluminantPalette.WarmAmber,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "今日复习 (语法)",
                count = reviewGrammars,
                color = NemoIndigo,
                icon = Icons.Rounded.Layers,
                plumpIcon = PlumpIcon.GrammarLayers,
                isoluminantColor = IsoluminantPalette.LilacViolet,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    plumpIcon: PlumpIcon? = null,
    isoluminantColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO && plumpIcon != null && isoluminantColor != null
    val effectiveColor = if (isPlumpDuo) isoluminantColor!! else color
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (!isPlumpDuo) {
        if (isDark) color.copy(alpha = 0.16f) else color.copy(alpha = 0.08f)
    } else {
        if (isDark) effectiveColor.copy(alpha = 0.10f) else effectiveColor.copy(alpha = 0.06f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .then(
                        if (!isPlumpDuo) {
                            Modifier.background(
                                color = if (isDark) color.copy(alpha = 0.25f) else color.copy(alpha = 0.16f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPlumpDuo) {
                    PlumpDuoIcon(
                        icon = plumpIcon,
                        color = effectiveColor,
                        size = 28.dp
                    )
                } else {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

