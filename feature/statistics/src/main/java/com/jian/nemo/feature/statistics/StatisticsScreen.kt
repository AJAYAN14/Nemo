package com.jian.nemo.feature.statistics

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
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

    val backgroundColor = MaterialTheme.colorScheme.background

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
    var isExpanded by remember { mutableStateOf(false) }

    // 如果数量少于等于 defaultShowCount + 1，直接全部显示，避免出现"展开查看剩余 1 项"的尴尬
    val shouldCollapse = items.size > defaultShowCount + 1
    val showItems = if (!shouldCollapse || isExpanded) items else items.take(defaultShowCount)
    val remainingCount = items.size - defaultShowCount

    PremiumCard {
        Column(
            modifier = Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            showItems.forEachIndexed { index, item ->
                // 根据索引循环获取颜色
                val color = AvatarColors[index % AvatarColors.size]

                StatisticsItemRow(
                    item = item,
                    avatarColor = color,
                    onClick = { onItemClick(item.id) },
                    showDivider = index < showItems.size - 1,
                    showSourceBadge = showSourceBadge
                )
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
                        ) { isExpanded = !isExpanded }
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isExpanded) "收起" else "展开查看剩余 $remainingCount 项",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
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
    showSourceBadge: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = avatarColor
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            val (badgeText, badgeBg, badgeTextColor) = when (item.source) {
                StatisticSource.LEARNED -> Triple(
                    "新学",
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    MaterialTheme.colorScheme.primary
                )
                StatisticSource.REVIEWED -> Triple(
                    "复习",
                    NemoSecondary.copy(alpha = 0.15f),
                    NemoSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showSourceBadge) {
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = item.japanese,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (item.level.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.level,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Construct meaningful secondary text
            val secondaryText = buildString {
                if (item.hiragana.isNotEmpty()) append(item.hiragana)
                if (item.hiragana.isNotEmpty() && item.chinese.isNotEmpty()) append(" · ")
                if (item.chinese.isNotEmpty()) append(item.chinese)
            }

            if (secondaryText.isNotEmpty()) {
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1
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
