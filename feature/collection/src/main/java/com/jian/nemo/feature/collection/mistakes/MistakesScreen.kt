package com.jian.nemo.feature.collection.mistakes

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.progress.NemoCircularProgress
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.ui.animation.containerTransform

// 临时本地定义 (对应 Color.kt 中的值)，解决模块间引用编译问题
private val NemoSecondary = Color(0xFF4CAF50)
private val NemoOrange = Color(0xFFFF9500) // IosColors.Orange
private val NemoDanger = Color(0xFFE53935)

/**
 * 我的错题界面 - UI/UX Pro Max 风格
 *
 * 风格统一：
 * - Background: Solid (MaterialTheme.colorScheme.background)
 * - Card: Premium Card (White/SurfaceContainer + Shadow + 26dp Rounded)
 * - Icon: Squircle (Rounded 14dp)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesScreen(
    viewModel: MistakesViewModel = hiltViewModel(),
    onNavigateToWordMistakes: () -> Unit = {},
    onNavigateToGrammarMistakes: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()



    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    var isMenuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .containerTransform(
                key = "container_mistakes",
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                com.jian.nemo.core.ui.component.common.CommonHeader(
                    title = "我的错题",
                    onBack = onNavigateBack,
                    backgroundColor = backgroundColor,
                    actions = {
                        com.jian.nemo.feature.collection.components.CollectionActionMenu(
                            wordCount = uiState.wrongWordsCount,
                            grammarCount = uiState.wrongGrammarsCount,
                            titleSuffix = "错题",
                            onClearAll = viewModel::clearAllWrongAnswers,
                            onClearWords = viewModel::clearAllWordMistakes,
                            onClearGrammars = viewModel::clearAllGrammarMistakes,
                            isExpanded = isMenuExpanded,
                            onToggleMenu = { isMenuExpanded = it }
                        )
                    }
                )
            },
            containerColor = backgroundColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // 统计总览卡片
                StatsOverviewCard(
                    totalLearnedCount = uiState.totalLearnedCount,
                    wrongWordsCount = uiState.wrongWordsCount,
                    wrongGrammarsCount = uiState.wrongGrammarsCount
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 错题分类列表
                WrongAnswersList(
                    wrongWordsCount = uiState.wrongWordsCount,
                    wrongGrammarsCount = uiState.wrongGrammarsCount,
                    onWordMistakesClick = onNavigateToWordMistakes,
                    onGrammarMistakesClick = onNavigateToGrammarMistakes
                )
            }
        }

        // 页面全屏聚焦遮罩 (与学习界面完全一致的 28% 黑色半透明 + 200ms 淡入淡出)
        androidx.compose.animation.AnimatedVisibility(
            visible = isMenuExpanded,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { isMenuExpanded = false }
                    )
            )
        }
    }
}

@Composable
fun StatsOverviewCard(
    totalLearnedCount: Int,
    wrongWordsCount: Int,
    wrongGrammarsCount: Int
) {
    // 逻辑：(当前错题本数量 / 已学习的知识总量)
    val gapRate = if (totalLearnedCount > 0) {
        val currentWrongCount = wrongWordsCount + wrongGrammarsCount
        val rate = (currentWrongCount.toFloat() / totalLearnedCount.toFloat() * 100).toInt()
        rate.coerceAtMost(100)
    } else {
        0
    }

    // 状态逻辑
    val encouragementText = when {
        totalLearnedCount == 0 -> "开始学习，消灭知识盲点！"
        gapRate == 0 -> "完美！所有知识点都已掌握"
        gapRate <= 5 -> "状态极佳，继续保持！"
        gapRate <= 15 -> "有些小漏洞，及时复习哦"
        gapRate <= 30 -> "盲点较多，建议专项突破"
        else -> "基础不牢，地动山摇，快去复习！"
    }

    // 颜色状态 (仅影响文字和装饰，不影响Card背景)
    val stateColor = when {
        gapRate <= 5 -> NemoSecondary
        gapRate <= 15 -> NemoOrange
        else -> NemoDanger
    }

    PremiumCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "知识盲点率",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$gapRate",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Black,
                        color = stateColor,
                        lineHeight = 48.sp
                    )
                    Text(
                        text = "%",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = stateColor,
                        modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = encouragementText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    NemoCircularProgress(
                        progress = gapRate / 100f,
                        modifier = Modifier.fillMaxSize(),
                        color = stateColor,
                        strokeWidth = 8.dp,
                        trackColor = stateColor.copy(alpha = 0.1f)
                    )
                }
        }
    }
}

@Composable
fun WrongAnswersList(
    wrongWordsCount: Int,
    wrongGrammarsCount: Int,
    onWordMistakesClick: () -> Unit,
    onGrammarMistakesClick: () -> Unit
) {
    // 与其把两个放在一个Card，不如拆分成两个Premium Card，视觉更强
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        WrongAnswerCategoryCard(
            title = "错误的单词",
            subtitle = "学习和测试中答错的单词",
            count = wrongWordsCount,
            iconColor = NemoSecondary,
            onClick = onWordMistakesClick
        )

        WrongAnswerCategoryCard(
            title = "错误的语法",
            subtitle = "学习和测试中答错的语法",
            count = wrongGrammarsCount,
            iconColor = NemoOrange,
            onClick = onGrammarMistakesClick
        )
    }
}

@Composable
fun WrongAnswerCategoryCard(
    title: String,
    subtitle: String,
    count: Int,
    iconColor: Color,
    onClick: () -> Unit
) {
    PremiumCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.rotate(180f).size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Premium Card Composable
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
        content = { Column(modifier = Modifier.padding(24.dp), content = content) }
    )
}
