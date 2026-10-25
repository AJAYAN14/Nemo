package com.jian.nemo.feature.statistics.presentation.dashboard

import com.jian.nemo.core.designsystem.theme.screenBackground

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.designsystem.icon.IsoluminantPalette
import com.jian.nemo.core.designsystem.icon.PlumpDuoIcon
import com.jian.nemo.core.designsystem.icon.PlumpIcon
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle


/**
 * 复学清单 (Leech Management) - Hybrid Design
 *
 * 结合了用户喜爱的设计元素：
 * 1. Tabs: Floating Liquid Pills (Clean & Airy with Soft Shadow)
 * 2. Cards: Premium Shadow (Consistent with App)
 */
@Composable
fun LeechManagementScreen(
    onBack: () -> Unit,
    onNavigateToWordDetail: (Int) -> Unit = {},
    onNavigateToGrammarDetail: (Int) -> Unit = {},
    viewModel: LeechManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Background color
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(message = it, withDismissAction = true)
            viewModel.onEvent(LeechEvent.ClearMessages)
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(message = it, withDismissAction = true)
            viewModel.onEvent(LeechEvent.ClearMessages)
        }
    }

    CompositionLocalProvider(LocalSettingIconStyle provides uiState.settingIconStyle) {
        val isPlumpDuo = uiState.settingIconStyle == SettingIconStyle.PLUMP_DUO
        val wordAccentColor = if (isPlumpDuo) IsoluminantPalette.WarmAmber else MaterialTheme.colorScheme.primary
        val grammarAccentColor = if (isPlumpDuo) IsoluminantPalette.EmeraldGreen else MaterialTheme.colorScheme.primary
        val currentAccentColor = if (uiState.selectedTab == LeechTab.Word) wordAccentColor else grammarAccentColor

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CommonHeader(
                    title = "复学清单",
                    onBack = onBack,
                    backgroundColor = backgroundColor
                )
            },
            containerColor = backgroundColor
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {

            // 1. Floating Liquid Pill Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LeechPillTab(
                    title = "单词",
                    count = uiState.skippedWords.size,
                    isSelected = uiState.selectedTab == LeechTab.Word,
                    selectedColor = wordAccentColor,
                    onClick = { viewModel.onEvent(LeechEvent.TabChanged(LeechTab.Word)) }
                )

                LeechPillTab(
                    title = "语法",
                    count = uiState.skippedGrammars.size,
                    isSelected = uiState.selectedTab == LeechTab.Grammar,
                    selectedColor = grammarAccentColor,
                    onClick = { viewModel.onEvent(LeechEvent.TabChanged(LeechTab.Grammar)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Content Area
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NemoChasingDotsLoader()
                }
            } else {
                AnimatedContent(
                    targetState = uiState.selectedTab,
                    transitionSpec = {
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                    },
                    label = "LeechListTransition"
                ) { targetTab ->
                    when (targetTab) {
                        LeechTab.Word -> {
                            LeechList(
                                items = uiState.skippedWords,
                                emptyTitle = "暂无待复学单词",
                                emptySubtitle = "当前没有被冻结的难点单词，继续保持哦",
                                onItemKey = { it.id },
                                isPlumpDuo = isPlumpDuo,
                                accentColor = wordAccentColor,
                                itemContent = { word, onRecover ->
                                    LeechWordCard(
                                        word = word, 
                                        tagColor = wordAccentColor,
                                        onRecover = onRecover,
                                        onClick = { onNavigateToWordDetail(word.id) }
                                    )
                                },
                                onRecover = { id -> viewModel.onEvent(LeechEvent.RecoverWord(id)) }
                            )
                        }
                        LeechTab.Grammar -> {
                            LeechList(
                                items = uiState.skippedGrammars,
                                emptyTitle = "暂无待复学语法",
                                emptySubtitle = "当前没有被冻结的难点语法，继续保持哦",
                                onItemKey = { it.id },
                                isPlumpDuo = isPlumpDuo,
                                accentColor = grammarAccentColor,
                                itemContent = { grammar, onRecover ->
                                    LeechGrammarCard(
                                        grammar = grammar, 
                                        tagColor = grammarAccentColor,
                                        onRecover = onRecover,
                                        onClick = { onNavigateToGrammarDetail(grammar.id) }
                                    )
                                },
                                onRecover = { id -> viewModel.onEvent(LeechEvent.RecoverGrammar(id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Liquid Floating Pill Tab with Soft Shadow
 */
@Composable
private fun RowScope.LeechPillTab(
    title: String,
    count: Int,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerColor = if (isSelected) {
        selectedColor
    } else {
        if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    }
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

    LiquidButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(44.dp),
        backgroundColor = containerColor,
        shape = RoundedCornerShape(22.dp),
        border = borderColor,
        elevation = if (isSelected) 6.dp else 2.dp,
        useSoftShadow = true
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$title ($count)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor
            )
        }
    }
}


/**
 * 通用复学列表
 */
@Composable
private fun <T> LeechList(
    items: List<T>,
    emptyTitle: String,
    emptySubtitle: String,
    onItemKey: (T) -> Int,
    isPlumpDuo: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    itemContent: @Composable (T, () -> Unit) -> Unit,
    onRecover: (Int) -> Unit
) {
    if (items.isEmpty()) {
        EmptyLeechView(
            title = emptyTitle,
            subtitle = emptySubtitle,
            isPlumpDuo = isPlumpDuo,
            accentColor = accentColor
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 80.dp), // More horizontal padding
            verticalArrangement = Arrangement.spacedBy(16.dp), // More spacing for airy feel
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = items,
                key = { onItemKey(it) }
            ) { item ->
                // Item Entrance Animation
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { isVisible = true }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    itemContent(item) { onRecover(onItemKey(item)) }
                }
            }
        }
    }
}

/**
 * 单词卡片
 */
@Composable
private fun LeechWordCard(
    word: Word,
    tagColor: Color = MaterialTheme.colorScheme.primary,
    onRecover: () -> Unit,
    onClick: () -> Unit = {}
) {
    LeechItemCardBase(
        title = word.japanese,
        subtitle = "${word.hiragana ?: ""} ${word.chinese}",
        tagColor = tagColor,
        onRecover = onRecover,
        onClick = onClick
    )
}

/**
 * 语法卡片
 */
@Composable
private fun LeechGrammarCard(
    grammar: Grammar,
    tagColor: Color = MaterialTheme.colorScheme.primary,
    onRecover: () -> Unit,
    onClick: () -> Unit = {}
) {
    LeechItemCardBase(
        title = grammar.grammar,
        subtitle = grammar.getFirstExplanation(),
        tagColor = tagColor,
        onRecover = onRecover,
        onClick = onClick
    )
}

/**
 * 基础卡片 UI (Premium Style: Consistent with App)
 */
@Composable
private fun LeechItemCardBase(
    title: String,
    subtitle: String,
    tagColor: Color,
    onRecover: () -> Unit,
    onClick: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // Premium Style Colors match ProgressComponents / WordList
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val contentColor = MaterialTheme.colorScheme.onSurface

    var isRecovering by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 24.dp, isDark = isDark),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp) // Premium cards usually have slightly more padding
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill Indicator (Preserved from V2 as it's cleaner)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .background(tagColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(20.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            val isPlumpDuo = LocalSettingIconStyle.current == SettingIconStyle.PLUMP_DUO

            // Recover Button
            IconButton(
                onClick = {
                    isRecovering = true
                    onRecover()
                },
                enabled = !isRecovering,
                modifier = Modifier
                    .then(
                        if (!isPlumpDuo) {
                            Modifier.background(
                                color = tagColor.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    )
            ) {
                if (isRecovering) {
                    NemoChasingDotsLoader(size = 20.dp)
                } else {
                    if (isPlumpDuo) {
                        PlumpDuoIcon(
                            icon = PlumpIcon.RotateCcw,
                            color = tagColor,
                            size = 20.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Restore,
                            contentDescription = "Recover",
                            tint = tagColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Premium Empty State (Consistent with Favorites / Collection)
 */
@Composable
private fun EmptyLeechView(
    title: String,
    subtitle: String,
    isPlumpDuo: Boolean = false,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isPlumpDuo) {
                // Plump Duo 规范：彻底取消外层纯色方块/圆角底框，采用纯透明对齐容器，图标通透居中呈现
                Box(
                    modifier = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PlumpDuoIcon(
                        icon = PlumpIcon.Flame,
                        color = accentColor,
                        size = 56.dp
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
