package com.jian.nemo.feature.statistics

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.ui.component.common.CommonHeader

import com.jian.nemo.feature.statistics.components.LevelBreakdownDialog

/**
 * 历史统计界面 (UI/UX Pro Max)
 *
 * 展示所有已学习的单词和语法
 * 风格统一：Solid Typography, Premium Card, Squircle Icons
 */
@Composable
fun HistoricalStatisticsScreen(
    onBack: () -> Unit,
    onNavigateToWordDetail: (Int) -> Unit,
    onNavigateToGrammarDetail: (Int) -> Unit,
    viewModel: HistoricalStatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val words = uiState.learnedWords
    val grammars = uiState.learnedGrammars
    val backgroundColor = MaterialTheme.colorScheme.background

    var showWordDialog by remember { mutableStateOf(false) }
    var showGrammarDialog by remember { mutableStateOf(false) }

    // 处理数据分组
    val wordLevelData = remember(words) {
        words.groupBy { it.level }
            .mapValues { it.value.size }
            .toSortedMap()
    }
    val grammarLevelData = remember(grammars) {
        grammars.groupBy { it.level }
            .mapValues { it.value.size }
            .toSortedMap()
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(backgroundColor)) {
                CommonHeader(
                    title = "历史统计",
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

            // 1. 汇总卡片
            item {
                StatisticsSectionTitle("累计学习")
                HistoricalSummaryCard(
                    totalWords = words.size,
                    totalGrammars = grammars.size,
                    onWordsClick = { if (words.isNotEmpty()) showWordDialog = true },
                    onGrammarsClick = { if (grammars.isNotEmpty()) showGrammarDialog = true }
                )
            }

            // 2. 单词列表 (复用 StatisticsListCard)
            item {
                StatisticsSectionTitle("已学单词 (${words.size})")
            }

            if (words.isNotEmpty()) {
                item {
                    // Reuse the public component from StatisticsScreen.kt
                    StatisticsListCard(
                        items = words,
                        onItemClick = onNavigateToWordDetail,
                        isWord = true,
                        showSourceBadge = false
                    )
                }
            } else {
                item {
                    EmptyStatisticsState("暂无单词学习记录")
                }
            }

            // 3. 语法列表 (复用 StatisticsListCard)
            item {
                StatisticsSectionTitle("已学语法 (${grammars.size})")
            }

            if (grammars.isNotEmpty()) {
                item {
                    StatisticsListCard(
                        items = grammars,
                        onItemClick = onNavigateToGrammarDetail,
                        isWord = false,
                        showSourceBadge = false
                    )
                }
            } else {
                item {
                    EmptyStatisticsState("暂无语法学习记录")
                }
            }
        }
    }

    // 单词级别分布弹窗
    if (showWordDialog) {
        LevelBreakdownDialog(
            title = "单词等级分布",
            data = wordLevelData,
            themeColor = NemoPrimary,
            onDismiss = { showWordDialog = false }
        )
    }

    // 语法级别分布弹窗
    if (showGrammarDialog) {
        LevelBreakdownDialog(
            title = "语法等级分布",
            data = grammarLevelData,
            themeColor = NemoSecondary,
            onDismiss = { showGrammarDialog = false }
        )
    }
}


@Composable
fun HistoricalSummaryCard(
    totalWords: Int,
    totalGrammars: Int,
    onWordsClick: () -> Unit,
    onGrammarsClick: () -> Unit
) {
    PremiumCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Words
            StatItem(
                value = totalWords.toString(),
                label = "单词总数",
                color = NemoPrimary,
                modifier = Modifier.weight(1f),
                onClick = onWordsClick
            )

            // Grammar
            StatItem(
                value = totalGrammars.toString(),
                label = "语法总数",
                color = NemoSecondary,
                modifier = Modifier.weight(1f),
                onClick = onGrammarsClick
            )
        }
    }
}

// 统计项组件 (Squircle Style)
@Composable
fun StatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "statItemScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.06f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Premium Card (Local Copy for independence)
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
