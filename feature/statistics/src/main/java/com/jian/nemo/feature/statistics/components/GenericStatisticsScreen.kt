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
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.LearningCardBackgroundDark
import com.jian.nemo.feature.statistics.model.StatisticDisplayItem
import com.jian.nemo.core.ui.component.list.LevelHeader
import com.jian.nemo.core.ui.component.card.CommonGrammarCard
import com.jian.nemo.core.ui.component.common.CommonHeader
import kotlinx.coroutines.launch

/**
 * 通用统计界面
 *
 * 用于今日统计和历史统计，显示学习的单词和语法列表
 * 参考旧项目: old-nemo/ui/component/statistics/GenericStatisticsScreen.kt
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
    emptyWordMessage: String = "还没有学习任何单词",
    emptyGrammarMessage: String = "还没有学习任何语法"
) {
    val tabItems = listOf("单词", "语法")
    val pagerState = rememberPagerState(pageCount = { tabItems.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CommonHeader(
                title = title,
                onBack = onBack,
                backgroundColor = Color.Transparent
            )
        }
    ) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
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
            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> StatisticsContent(
                        type = "单词",
                        items = wordItems,
                        onItemClick = onItemClick,
                        emptyMessage = emptyWordMessage
                    )
                    1 -> StatisticsContent(
                        type = "语法",
                        items = grammarItems,
                        onItemClick = onItemClick,
                        emptyMessage = emptyGrammarMessage
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    type: String,
    items: List<StatisticDisplayItem>,
    onItemClick: (type: String, item: StatisticDisplayItem) -> Unit,
    emptyMessage: String
) {
    val groupedItems = items.groupBy { it.level }
    val expandedState = remember { mutableStateMapOf<String, Boolean>() }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val cardColor = if (isDarkTheme) LearningCardBackgroundDark else Color.White

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
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
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
