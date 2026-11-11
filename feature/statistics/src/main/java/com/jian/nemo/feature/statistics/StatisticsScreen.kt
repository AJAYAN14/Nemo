package com.jian.nemo.feature.statistics

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    val backdrop = rememberLayerBackdrop {
        drawRect(
            color = backgroundColor,
            size = Size(this.size.width * 3, this.size.height * 3),
            topLeft = Offset(-this.size.width, -this.size.height)
        )
        drawContent()
    }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var isTransitionSettled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(320L)
        isTransitionSettled = true
    }

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
        // 1. 底层滚动内容（转场就位后再激活背景离屏录制）
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(
                    backdrop = backdrop,
                    enabled = isTransitionSettled
                ),
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
                    StatisticsSectionTitle(stringResource(R.string.stats_screen_words_count_format, words.size))
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
                    } else if (uiState.isListsLoading) {
                        StatisticsListSkeletonCard()
                    } else {
                        EmptyStatisticsState(
                            message = stringResource(R.string.stats_screen_empty_words_message),
                            subtitle = stringResource(R.string.stats_screen_empty_words_subtitle),
                            icon = Icons.Rounded.Translate,
                            accentColor = SystemColors.Green
                        )
                    }
                }
            }

            // 2. 语法列表
            item {
                Column {
                    StatisticsSectionTitle(stringResource(R.string.stats_screen_grammars_count_format, grammars.size))
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
                    } else if (uiState.isListsLoading) {
                        StatisticsListSkeletonCard()
                    } else {
                        EmptyStatisticsState(
                            message = stringResource(R.string.stats_screen_empty_grammars_message),
                            subtitle = stringResource(R.string.stats_screen_empty_grammars_subtitle),
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            accentColor = SystemColors.Green
                        )
                    }
                }
            }
        }

        // 2. 悬浮回顶按钮（单词列表同款液态玻璃回顶）
        LiquidScrollToTopButton(
            listState = listState,
            backdrop = if (isTransitionSettled) backdrop else null,
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
                title = stringResource(R.string.stats_screen_title_screen),
                onBack = onBack,
                backgroundColor = Color.Transparent,
                backdrop = if (isTransitionSettled) backdrop else null,
                backButtonSize = 48.dp
            )
        }
    }
}
}

// 预定义的一组高级柔和色彩，用于循环显示
// 使用语义化颜色定义，避免硬编码
private val AvatarColors = listOf(
    SystemColors.Blue,
    SystemColors.Orange,
    SystemColors.Green,
    SystemColors.Indigo,
    SystemColors.Teal,
    SystemColors.Purple,
    SystemColors.Pink,
    SystemColors.Cyan
)

@Composable
fun StatisticsSectionTitle(
    text: String,
    modifier: Modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp)
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
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
    val defaultShowCount = 5
    val pageSize = 30
    var visibleCount by remember(items) { mutableStateOf(defaultShowCount) }

    // 如果数量少于等于 defaultShowCount + 1，直接全部显示，避免出现"展开查看剩余 1 项"的尴尬
    val shouldCollapse = items.size > defaultShowCount + 1
    val showItems = if (!shouldCollapse) items else items.take(visibleCount)
    val remainingCount = items.size - visibleCount

    PremiumCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            showItems.forEachIndexed { index, item ->
                // 根据索引循环获取颜色
                val color = AvatarColors[index % AvatarColors.size]

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
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isFullyExpanded) {
                                stringResource(R.string.stats_screen_btn_collapse)
                            } else {
                                stringResource(R.string.stats_screen_btn_expand_format, remainingCount)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isFullyExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class CollapsedListMetrics(
    val titleHeight: Dp,
    val bodyHeight: Dp,
    val labelHeight: Dp,
    val collapsedContentMinHeight: Dp
)

@Composable
private fun rememberCollapsedListMetrics(): CollapsedListMetrics {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.titleMedium
    val bodyStyle = MaterialTheme.typography.bodyMedium
    val labelStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)

    return remember(density, titleStyle, bodyStyle, labelStyle) {
        val titleHeightPx = textMeasurer.measure(
            text = "あA",
            style = titleStyle,
            maxLines = 1
        ).size.height
        val bodyHeightPx = textMeasurer.measure(
            text = "あA",
            style = bodyStyle,
            maxLines = 1
        ).size.height
        val labelHeightPx = textMeasurer.measure(
            text = "あA",
            style = labelStyle,
            maxLines = 1
        ).size.height

        with(density) {
            val titleDp = titleHeightPx.toDp()
            val bodyDp = bodyHeightPx.toDp()
            val labelDp = labelHeightPx.toDp()
            val singleRowHeight = 12.dp + maxOf(48.dp, titleDp + 2.dp + bodyDp) + 12.dp
            val expandButtonHeight = 12.dp + maxOf(16.dp, labelDp) + 4.dp
            // 5 行 + 4 条行间分割区(4 * (4dp + 0.5dp + 4dp)) + 展开区间隔与分割线(3 * 4dp + 8dp + 0.5dp) + 展开按钮区
            val totalContentMinHeight = singleRowHeight * 5 + 34.dp + 20.5.dp + expandButtonHeight
            CollapsedListMetrics(
                titleHeight = titleDp,
                bodyHeight = bodyDp,
                labelHeight = labelDp,
                collapsedContentMinHeight = totalContentMinHeight
            )
        }
    }
}

/**
 * 统计列表默认折叠态静态骨架屏
 */
@Composable
fun StatisticsListSkeletonCard() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isDark) 0.08f else 0.06f
    )
    val subtitleSkeletonColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isDark) 0.055f else 0.04f
    )
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
    val metrics = rememberCollapsedListMetrics()
    val titleFractions = remember { listOf(0.42f, 0.52f, 0.36f, 0.48f, 0.40f) }
    val subtitleFractions = remember { listOf(0.68f, 0.58f, 0.74f, 0.62f, 0.70f) }

    PremiumCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(5) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(skeletonColor, RoundedRectangle(14.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.height(metrics.titleHeight),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(titleFractions[index])
                                    .height(15.dp)
                                    .background(skeletonColor, RoundedRectangle(6.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier.height(metrics.bodyHeight),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(subtitleFractions[index])
                                    .height(12.dp)
                                    .background(subtitleSkeletonColor, RoundedRectangle(5.dp))
                            )
                        }
                    }
                }

                if (index < 4) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 64.dp),
                        color = dividerColor,
                        thickness = 0.5.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                color = dividerColor,
                thickness = 0.5.dp
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.height(maxOf(16.dp, metrics.labelHeight)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(14.dp)
                            .background(skeletonColor, RoundedRectangle(7.dp))
                    )
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
    accentColor: Color = SystemColors.Green
) {
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val containerBgColor = if (isSoftTranslucent) accentColor.copy(alpha = 0.15f) else accentColor
    val iconTint = if (isSoftTranslucent) accentColor else Color.White
    val metrics = rememberCollapsedListMetrics()

    PremiumCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = metrics.collapsedContentMinHeight)
                .padding(vertical = 28.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(containerBgColor, RoundedRectangle(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(32.dp)
                    )
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
        val avatarBgAlpha = if (isDark) 0.15f else 0.10f

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(avatarColor.copy(alpha = avatarBgAlpha), RoundedRectangle(14.dp)),
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
            val newLearnBadgeText = stringResource(R.string.stats_screen_badge_learned)
            val reviewBadgeText = stringResource(R.string.stats_screen_badge_reviewed)
            val badgeInfo = remember(item.source, newLearnBadgeText, reviewBadgeText) {
                when (item.source) {
                    StatisticSource.LEARNED -> {
                        val color = SystemColors.Blue
                        Triple(
                            newLearnBadgeText,
                            color.copy(alpha = 0.12f),
                            color
                        )
                    }
                    StatisticSource.REVIEWED -> {
                        val color = SystemColors.Green
                        Triple(
                            reviewBadgeText,
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
                            .clip(RoundedRectangle(6.dp))
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
                            .clip(RoundedRectangle(6.dp))
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

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedRectangle(26.dp),
        color = containerColor,

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
                title = stringResource(R.string.stats_screen_stat_new_words),
                count = newWords,
                color = SystemColors.Blue,
                icon = Icons.Rounded.DoneAll,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.stats_screen_stat_review_words),
                count = reviewWords,
                color = SystemColors.Green,
                icon = Icons.Rounded.Layers,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = stringResource(R.string.stats_screen_stat_new_grammars),
                count = newGrammars,
                color = SystemColors.Orange,
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = stringResource(R.string.stats_screen_stat_review_grammars),
                count = reviewGrammars,
                color = SystemColors.Indigo,
                icon = Icons.Rounded.History,
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
    modifier: Modifier = Modifier
) {
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val containerBgColor = if (isSoftTranslucent) color.copy(alpha = 0.15f) else color
    val iconTint = if (isSoftTranslucent) color else Color.White

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) color.copy(alpha = 0.12f) else color.copy(alpha = 0.06f)

    Surface(
        shape = RoundedRectangle(16.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(containerBgColor, RoundedRectangle(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
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

