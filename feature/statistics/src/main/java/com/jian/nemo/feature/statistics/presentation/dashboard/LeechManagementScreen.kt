package com.jian.nemo.feature.statistics.presentation.dashboard

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.common.CommonHeader

/**
 * 复学清单 (Leech Management) - Hybrid Design
 *
 * 结合了用户喜爱的设计元素：
 * 1. Tabs: Floating Pills (Clean & Airy)
 * 2. Cards: Premium Shadow (Consistent with App)
 */
@Composable
fun LeechManagementScreen(
    onBack: () -> Unit,
    viewModel: LeechManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Background color
    val backgroundColor = MaterialTheme.colorScheme.background

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
            // 1. Floating Pill Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LeechPillTab(
                    title = "单词",
                    count = uiState.skippedWords.size,
                    isSelected = uiState.selectedTab == LeechTab.Word,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.onEvent(LeechEvent.TabChanged(LeechTab.Word)) }
                )

                LeechPillTab(
                    title = "语法",
                    count = uiState.skippedGrammars.size,
                    isSelected = uiState.selectedTab == LeechTab.Grammar,
                    selectedColor = MaterialTheme.colorScheme.primary,
                    onClick = { viewModel.onEvent(LeechEvent.TabChanged(LeechTab.Grammar)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Content Area
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                                emptyMessage = "太棒了！\n没有需要复学的单词",
                                onItemKey = { it.id },
                                itemContent = { word, onRecover ->
                                    LeechWordCard(word = word, onRecover = onRecover)
                                },
                                onRecover = { id -> viewModel.onEvent(LeechEvent.RecoverWord(id)) }
                            )
                        }
                        LeechTab.Grammar -> {
                            LeechList(
                                items = uiState.skippedGrammars,
                                emptyMessage = "太棒了！\n没有需要复学的语法",
                                onItemKey = { it.id },
                                itemContent = { grammar, onRecover ->
                                    LeechGrammarCard(grammar = grammar, onRecover = onRecover)
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

/**
 * Clean Floating Pill Tab
 */
@Composable
private fun RowScope.LeechPillTab(
    title: String,
    count: Int,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) selectedColor else Color.Transparent
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(40.dp),
        shape = RoundedCornerShape(20.dp), // Fully rounded pill
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$title ($count)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
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
    emptyMessage: String,
    onItemKey: (T) -> Int,
    itemContent: @Composable (T, () -> Unit) -> Unit,
    onRecover: (Int) -> Unit
) {
    if (items.isEmpty()) {
        EmptyLeechView(message = emptyMessage)
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
private fun LeechWordCard(word: Word, onRecover: () -> Unit) {
    LeechItemCardBase(
        title = word.japanese,
        subtitle = "${word.hiragana ?: ""} ${word.chinese}",
        tagColor = MaterialTheme.colorScheme.primary,
        onRecover = onRecover
    )
}

/**
 * 语法卡片
 */
@Composable
private fun LeechGrammarCard(grammar: Grammar, onRecover: () -> Unit) {
    LeechItemCardBase(
        title = grammar.grammar,
        subtitle = grammar.getFirstExplanation(),
        tagColor = MaterialTheme.colorScheme.primary,
        onRecover = onRecover
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
    onRecover: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // Premium Style Colors match ProgressComponents / WordList
    val containerColor = if(isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val contentColor = MaterialTheme.colorScheme.onSurface

    // Premium Shadow settings
    val shadowElevation = if (isDark) 4.dp else 10.dp
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.05f)

    var isRecovering by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(24.dp),
                spotColor = shadowColor,
                ambientColor = shadowColor
            ),
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

            // Recover Button
            IconButton(
                onClick = {
                    isRecovering = true
                    onRecover()
                },
                enabled = !isRecovering,
                modifier = Modifier
                    .background(
                        color = tagColor.copy(alpha = 0.1f), // Subtle background matching tag
                        shape = CircleShape
                    )
            ) {
                if (isRecovering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = tagColor
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

@Composable
private fun EmptyLeechView(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
