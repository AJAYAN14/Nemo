package com.jian.nemo.feature.statistics.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jian.nemo.feature.statistics.R
import com.jian.nemo.feature.statistics.model.StatisticDisplayItem
import com.jian.nemo.core.ui.component.list.LevelHeader
import com.jian.nemo.core.ui.component.card.CommonGrammarCard
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.component.common.CommonHeader
import kotlinx.coroutines.launch

/**
 * 通用统计界面
 *
 * 用于今日统计和历史统计，显示学习的单词和语法列表

 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GenericStatisticsScreen(
    title: String,
    wordItems: List<StatisticDisplayItem>,
    grammarItems: List<StatisticDisplayItem>,
    onBack: () -> Unit,
    onItemClick: (type: String, item: StatisticDisplayItem) -> Unit,
    modifier: Modifier = Modifier,
    emptyWordMessage: String = stringResource(R.string.stats_generic_empty_word_message),
    emptyGrammarMessage: String = stringResource(R.string.stats_generic_empty_grammar_message)
) {
    val wordTab = stringResource(R.string.stats_generic_tab_word)
    val grammarTab = stringResource(R.string.stats_generic_tab_grammar)
    val tabItems = listOf(wordTab, grammarTab)
    val pagerState = rememberPagerState(pageCount = { tabItems.size })
    val scope = rememberCoroutineScope()

    val backgroundColor = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarTop + 60.dp + 8.dp)
        ) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                tabItems.forEachIndexed { index, tabTitle ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = { Text(tabTitle) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> StatisticsContent(
                        type = wordTab,
                        items = wordItems,
                        onItemClick = onItemClick,
                        emptyMessage = emptyWordMessage,
                        backdrop = backdrop,
                        bottomPadding = navBarBottom + 24.dp
                    )
                    1 -> StatisticsContent(
                        type = grammarTab,
                        items = grammarItems,
                        onItemClick = onItemClick,
                        emptyMessage = emptyGrammarMessage,
                        backdrop = backdrop,
                        bottomPadding = navBarBottom + 24.dp
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
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
                title = title,
                onBack = onBack,
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp
            )
        }
    }
}

@Composable
private fun StatisticsContent(
    type: String,
    items: List<StatisticDisplayItem>,
    onItemClick: (type: String, item: StatisticDisplayItem) -> Unit,
    emptyMessage: String,
    backdrop: LayerBackdrop,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val groupedItems = items.groupBy { it.level }
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val cardColor = MaterialTheme.colorScheme.surface

    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = bottomPadding)
        ) {
            groupedItems.forEach { (level, itemsInLevel) ->
                item(key = "header_$level") {
                    LevelHeader(
                        level = level,
                        count = itemsInLevel.size,
                        isExpanded = expandedState[level] ?: false,
                        onClick = { expandedState[level] = !(expandedState[level] ?: false) },
                        containerColor = cardColor,
                        itemType = type
                    )
                }

                if (expandedState[level] == true) {
                    items(itemsInLevel, key = { "${type}_${it.id}" }) { item ->
                        Row(modifier = Modifier.padding(start = 10.dp)) {
                            Box(modifier = Modifier.clickable {
                                onItemClick(type, item)
                            }) {
                                StatisticItem(
                                    japanese = item.japanese,
                                    hiragana = item.hiragana,
                                    chinese = item.chinese,
                                    containerColor = cardColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticItem(
    japanese: String,
    hiragana: String,
    chinese: String,
    containerColor: Color
) {
    CommonGrammarCard(
        grammar = japanese,
        explanation = chinese,
        containerColor = containerColor,
        isFavorite = false
    )
}
