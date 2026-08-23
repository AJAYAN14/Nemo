package com.jian.nemo.core.ui.component.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.jian.nemo.core.ui.animation.animateListItem
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.LearningCardBackgroundDark
import com.jian.nemo.core.designsystem.theme.LearningScreenBackground
import com.jian.nemo.core.designsystem.theme.LearningScreenBackgroundDark
import com.jian.nemo.core.ui.component.common.CommonHeader

/**
 * 通用可展开列表界面
 *
 * 用于单词列表、语法列表、跳过词列表等按等级分组显示的界面
 * 参考旧项目: old-nemo/ui/component/lists/GenericExpandableListScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> GenericExpandableListScreen(
    title: String,
    itemsByLevel: Map<String, List<T>>,
    onBack: () -> Unit,
    onSearchFilter: (List<T>, String) -> List<T>,
    itemKey: (T) -> String,
    itemContent: @Composable (T, Color, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    emptyIcon: ImageVector? = Icons.Filled.Search,
    emptyTitle: String = "",
    emptyDescription: String = "",
    onItemClick: (T) -> Unit = {},
    searchPlaceholder: String = "",
    noResultsTitle: String = "",
    noResultsDescription: String = ""
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5
    val backgroundColor = if (isDarkTheme) LearningScreenBackgroundDark else LearningScreenBackground
    val cardColor = if (isDarkTheme) LearningCardBackgroundDark else Color.White

    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val expandedState = remember { mutableStateMapOf<String, Boolean>() }

    // 过滤后的按等级分组数据
    val filteredByLevel = remember(itemsByLevel, query) {
        if (query.isBlank()) {
            itemsByLevel
        } else {
            val q = query.trim()
            itemsByLevel.mapValues { (_, list) ->
                onSearchFilter(list, q)
            }.filterValues { it.isNotEmpty() }
        }
    }

    // 检查是否有数据
    val hasData = filteredByLevel.any { it.value.isNotEmpty() }

    Scaffold(
        topBar = {
            CommonHeader(
                title = title,
                onBack = onBack,
                backgroundColor = Color.Transparent,
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Filled.Search, contentDescription = "搜索")
                    }
                }
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
            if (showSearch) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    placeholder = { Text(searchPlaceholder) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* no-op */ })
                )
            }

            if (hasData) {
                GenericExpandableListContent(
                    itemsByLevel = filteredByLevel,
                    expandedState = expandedState,
                    cardColor = cardColor,
                    itemKey = itemKey,
                    itemContent = itemContent,
                    onItemClick = onItemClick
                )
            } else {
                if (query.isBlank()) {
                    // 空状态但没有搜索
                    EmptyStateView(
                        icon = emptyIcon,
                        title = emptyTitle,
                        description = emptyDescription
                    )
                } else {
                    // 搜索后无结果
                    EmptyStateView(
                        icon = Icons.Filled.Search,
                        title = noResultsTitle,
                        description = noResultsDescription
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector?,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun <T> GenericExpandableListContent(
    itemsByLevel: Map<String, List<T>>,
    expandedState: MutableMap<String, Boolean>,
    cardColor: Color,
    itemKey: (T) -> String,
    itemContent: @Composable (T, Color, () -> Unit) -> Unit,
    onItemClick: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
    ) {
        itemsByLevel.forEach { (level, itemsInLevel) ->
            if (itemsInLevel.isNotEmpty()) {
                item(key = "header_$level") {
                    LevelHeader(
                        level = level,
                        count = itemsInLevel.size,
                        isExpanded = expandedState[level] ?: false,
                        onClick = { expandedState[level] = !(expandedState[level] ?: false) },
                        containerColor = cardColor
                    )
                }

                if (expandedState[level] == true) {
                    items(itemsInLevel, key = { itemKey(it) }) { item ->
                        Box(modifier = Modifier.animateListItem()) {
                            itemContent(item, cardColor) { onItemClick(item) }
                        }
                    }
                }
            }
        }
    }
}
