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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.designsystem.shape.Capsule
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.core.ui.modifier.softShadow
import com.jian.nemo.feature.statistics.R

/**
 * 复学清单 (Leech Management)
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
        val wordAccentColor = IosColors.Orange
        val grammarAccentColor = IosColors.Green

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CommonHeader(
                    title = stringResource(R.string.stats_leech_title_screen),
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
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LeechPillTab(
                        title = stringResource(R.string.stats_leech_tab_words),
                        count = uiState.skippedWords.size,
                        isSelected = uiState.selectedTab == LeechTab.Word,
                        selectedColor = wordAccentColor,
                        onClick = { viewModel.onEvent(LeechEvent.TabChanged(LeechTab.Word)) }
                    )

                    LeechPillTab(
                        title = stringResource(R.string.stats_leech_tab_grammars),
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
                                    emptyTitle = stringResource(R.string.stats_leech_empty_words_title),
                                    emptySubtitle = stringResource(R.string.stats_leech_empty_words_desc),
                                    onItemKey = { it.id },
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
                                    emptyTitle = stringResource(R.string.stats_leech_empty_grammars_title),
                                    emptySubtitle = stringResource(R.string.stats_leech_empty_grammars_desc),
                                    onItemKey = { it.id },
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
    val shadowColor = if (isSelected) {
        if (isDark) selectedColor.copy(alpha = 0.35f) else selectedColor.copy(alpha = 0.28f)
    } else {
        if (isDark) Color.Black.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.04f)
    }

    ColoredLiquidGlassButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .softShadow(
                color = shadowColor,
                borderRadius = 22.dp,
                blurRadius = if (isSelected) 14.dp else 8.dp,
                offsetY = if (isSelected) 3.dp else 1.dp
            ),
        tint = if (isSelected) selectedColor else Color.Unspecified,
        tintAlpha = 0.85f,
        surfaceColor = if (isSelected) Color.Unspecified else if (isDark) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.65f),
        shape = Capsule(),
        border = if (isSelected) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_leech_tab_count_format, title, count),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    accentColor: Color = MaterialTheme.colorScheme.primary,
    itemContent: @Composable (T, () -> Unit) -> Unit,
    onRecover: (Int) -> Unit
) {
    if (items.isEmpty()) {
        EmptyLeechView(
            title = emptyTitle,
            subtitle = emptySubtitle,
            accentColor = accentColor
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = items,
                key = { onItemKey(it) }
            ) { item ->
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
 * 基础卡片 UI
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
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val contentColor = MaterialTheme.colorScheme.onSurface

    var isRecovering by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 24.dp, isDark = isDark),
        shape = RoundedRectangle(24.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .background(tagColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(20.dp))

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

            // 单容器计算属性架构：紧凑级 36dp 底块搭配 24dp 图标 (严格 2:3 黄金比例)
            val iconStyle = LocalSettingIconStyle.current
            val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
            val containerBg = if (isSoftTranslucent) tagColor.copy(alpha = 0.15f) else tagColor
            val iconTint = if (isSoftTranslucent) tagColor else Color.White

            IconButton(
                onClick = {
                    isRecovering = true
                    onRecover()
                },
                enabled = !isRecovering,
                modifier = Modifier
                    .size(36.dp)
                    .background(containerBg, RoundedRectangle(10.dp))
            ) {
                if (isRecovering) {
                    NemoChasingDotsLoader(size = 20.dp)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Restore,
                        contentDescription = stringResource(R.string.stats_leech_btn_recover),
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * 空状态视图 (单容器计算属性 + 情感化占位容器)
 */
@Composable
private fun EmptyLeechView(
    title: String,
    subtitle: String,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
    val containerBg = if (isSoftTranslucent) accentColor.copy(alpha = 0.15f) else accentColor.copy(alpha = 0.2f)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(containerBg, RoundedRectangle(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = accentColor
                )
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
