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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.jian.nemo.feature.statistics.model.StatisticDisplayItem
import com.jian.nemo.feature.statistics.model.StatisticSource

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

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(backgroundColor)) {
                CommonHeader(
                    title = "今日统计",
                    onBack = onBack
                )
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 0. 顶部统计面板
            item {
                OverallStatisticsDashboard(words = words, grammars = grammars)
            }

            // 1. 单词列表
            item {
                StatisticsSectionTitle("单词 (${words.size})")
            }

            if (words.isNotEmpty()) {
                item {
                    StatisticsListCard(
                        items = words,
                        onItemClick = onNavigateToWordDetail,
                        isWord = true
                    )
                }
            } else {
                item {
                    EmptyStatisticsState("该日期没有学习任何单词")
                }
            }

            // 2. 语法列表
            item {
                StatisticsSectionTitle("语法 (${grammars.size})")
            }

            if (grammars.isNotEmpty()) {
                item {
                    StatisticsListCard(
                        items = grammars,
                        onItemClick = onNavigateToGrammarDetail,
                        isWord = false
                    )
                }
            } else {
                item {
                    EmptyStatisticsState("该日期没有学习任何语法")
                }
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

@Composable
fun StatisticsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
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
            modifier = Modifier.animateContentSize(),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isFullyExpanded) "收起" else "展开查看更多 (剩余 $remainingCount 项)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isFullyExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStatisticsState(message: String) {
    PremiumCard {
         Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Rounded.Inbox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(avatarColor.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
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
            val badgeInfo = remember(item.source, primaryColor) {
                when (item.source) {
                    StatisticSource.LEARNED -> Triple(
                        "新学",
                        primaryColor.copy(alpha = 0.12f),
                        primaryColor
                    )
                    StatisticSource.REVIEWED -> Triple(
                        "复习",
                        NemoSecondary.copy(alpha = 0.15f),
                        NemoSecondary
                    )
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

                if (item.level.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
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
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(26.dp),
                spotColor = shadowColor,
                ambientColor = shadowColor
            ),
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
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "今日复习 (词)",
                count = reviewWords,
                color = NemoSecondary,
                icon = Icons.Rounded.Layers,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = "今日新学 (语法)",
                count = newGrammars,
                color = NemoOrange,
                icon = Icons.Rounded.DoneAll,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "今日复习 (语法)",
                count = reviewGrammars,
                color = NemoIndigo,
                icon = Icons.Rounded.Layers,
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
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
