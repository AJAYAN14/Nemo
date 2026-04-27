package com.jian.nemo.feature.library.presentation.list

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jian.nemo.core.ui.animation.animateListItem
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.navigation.NavDestination

/**
 * 单词列表界面 (UI/UX Pro Max)
 *
 * 显示所有单词，按等级分组。
 * 包含高级搜索功能和精致的列表项设计。
 * 优化：
 * 1. 支持点击标题折叠/展开分组 (默认收起)
 * 2. 等级标签颜色差异化 (N5=绿色 -> N1=粉色)
 * 3. 性能优化：搜索逻辑移至 ViewModel 后台线程
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WordListScreen(
    navController: NavController,
    viewModel: WordListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backgroundColor = MaterialTheme.colorScheme.background

    // Expanded State (Track which levels are OPEN)
    // Default: Empty (All Closed)
    val expandedLevels = rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }

    // Use data directly from ViewModel (already filtered)
    val filteredWordsByLevel = uiState.wordsByLevel
    val searchQuery = uiState.searchQuery

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(backgroundColor)) {
                CommonHeader(
                    title = "单词列表",
                    onBack = { navController.navigateUp() }
                )
                // Search Bar in Header Area
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredWordsByLevel.isEmpty() && searchQuery.isNotEmpty()) {
             Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState("未找到相关单词")
            }
        } else if (filteredWordsByLevel.isEmpty()) {
             Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                EmptyState("暂无单词数据")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val sortedLevels = filteredWordsByLevel.keys.sorted()

                sortedLevels.forEach { level ->
                    val words = filteredWordsByLevel[level] ?: emptyList()
                    // Search active -> Always Expanded. Otherwise use manual state.
                    val isExpanded = searchQuery.isNotEmpty() || expandedLevels.contains(level)
                    val levelColor = getLevelColor(level)

                    stickyHeader {
                        LevelHeader(
                            level = level,
                            count = words.size,
                            isExpanded = isExpanded,
                            color = levelColor,
                            onToggle = {
                                if (searchQuery.isEmpty()) {
                                    if (expandedLevels.contains(level)) {
                                        expandedLevels.remove(level)
                                    } else {
                                        expandedLevels.add(level)
                                    }
                                }
                            }
                        )
                    }

                    if (isExpanded) {
                        items(words, key = { it.id }) { word ->
                            Box(modifier = Modifier.animateListItem()) {
                                WordListItemPremium(
                                    word = word,
                                    onClick = { navController.navigate(NavDestination.wordDetail(word.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 预定义的一组高级柔和色彩，用于循环显示
private val AvatarColors = listOf(
    NemoPrimary,   // Blue
    NemoOrange,    // Orange
    NemoSecondary, // Green
    NemoIndigo,    // Indigo
    NemoTeal,      // Teal
    NemoPurple,    // Violet
    IosColors.Pink, // Pink
    NemoCyan       // Cyan
)

private fun getLevelColor(level: String): Color {
    return when (level.uppercase()) {
        "N5" -> NemoSecondary // Green (Easy)
        "N4" -> NemoCyan      // Cyan
        "N3" -> NemoPrimary   // Blue (Medium)
        "N2" -> NemoOrange    // Orange
        "N1" -> IosColors.Pink // Pink/Red (Hard)
        else -> NemoPrimary
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    // 关键修复：使用 TextFieldValue 持有本地状态，避免 IME 在重组时丢失组合信息
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length)))
    }

    // 当外部 query 发生变化时（如点击清除按钮），同步更新本地状态
    LaunchedEffect(query) {
        if (query != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(
                text = query,
                selection = TextRange(query.length)
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(25.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = "搜索：汉字 / 假名 / 释义",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        if (it.text != query) {
                            onQueryChange(it.text)
                        }
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelHeader(
    level: String,
    count: Int,
    isExpanded: Boolean,
    color: Color,
    onToggle: () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    Surface(
        color = backgroundColor.copy(alpha = 0.95f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 16.dp) // Right padding for icon
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = level,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$count 词",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            // Toggle Icon
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        rotationZ = if (isExpanded) 180f else 0f
                    }
            )
        }
    }
}


@Composable
private fun WordListItemPremium(
    word: Word,
    onClick: () -> Unit
) {
    // Determine color based on ID hash to keep it consistent
    val colorIndex = kotlin.math.abs(word.id.hashCode()) % AvatarColors.size
    val avatarColor = AvatarColors[colorIndex]

    PremiumCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            val char = word.japanese.firstOrNull()?.toString() ?: "?"
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(avatarColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    style = MaterialTheme.typography.headlineSmall,
                     fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = avatarColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.japanese,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                }

                Spacer(modifier = Modifier.height(4.dp))

                 // Secondary Line: Hiragana • Chinese
                val secondaryText = buildString {
                    if (word.hiragana.isNotEmpty()) append(word.hiragana)
                    if (word.hiragana.isNotEmpty() && word.chinese.isNotEmpty()) append(" · ")
                    if (word.chinese.isNotEmpty()) append(word.chinese)
                }

                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Rounded.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Premium Card (Local Copy)
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
    val shadowElevation = if (isDark) 2.dp else 4.dp // Slightly less elevation for list items
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.04f)

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(22.dp),
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
                shape = RoundedCornerShape(22.dp),
                spotColor = shadowColor,
                ambientColor = shadowColor
            ),
        interactionSource = interactionSource,
        content = { Column(modifier = Modifier.padding(16.dp), content = content) }
    )
}
