package com.jian.nemo.feature.library.presentation.category

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
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader


/**
 * 分类单词列表界面 (Refactored to match WordListScreen UI)
 *
 * 100% 还原 WordListScreen 的高级 UI 设计：
 * - Sticky Headers for Levels
 * - Premium Card Items
 * - Smooth Animations
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryWordsScreen(
    category: String,
    categoryTitle: String,
    onNavigateBack: () -> Unit,
    onNavigateToWordDetail: (Int) -> Unit = {},
    viewModel: CategoryWordsViewModel = hiltViewModel()
) {
    // 加载单词数据
    LaunchedEffect(category) {
        viewModel.loadWords(category)
    }

    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = MaterialTheme.colorScheme.background

    // Expanded State (Track which levels are OPEN)
    // Default: Empty (All Closed). Logic copied from WordListScreen
    val expandedLevels = rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }

    // Local Search State (If ViewModel doesn't have it, we handle locally or use VM if capable)
    // Since CategoryWordsViewModel typically just loads, we'll add a local search query state for UI filtering
    // effectively mirroring WordListScreen's behavior.
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Filter logic
    val filteredWordsByLevel = remember(uiState.wordsByLevel, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.wordsByLevel
        } else {
            uiState.wordsByLevel.mapValues { (_, words) ->
                words.filter { word ->
                    word.japanese.contains(searchQuery, ignoreCase = true) ||
                    word.hiragana.contains(searchQuery, ignoreCase = true) ||
                    word.chinese.contains(searchQuery, ignoreCase = true)
                }
            }.filterValues { it.isNotEmpty() }
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Column(modifier = Modifier.background(backgroundColor)) {
                CommonHeader(
                    title = if(uiState.isLoading) categoryTitle else "$categoryTitle (${uiState.words.size})",
                    onBack = onNavigateBack
                )

                // Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    NemoChasingDotsLoader()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uiState.error ?: "加载失败", color = MaterialTheme.colorScheme.error)
                }
            }
            filteredWordsByLevel.isEmpty() && searchQuery.isNotEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    EmptyState(message = "未找到相关单词")
                }
            }
            filteredWordsByLevel.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    EmptyState(message = "该分类下暂无词汇")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding() + 24.dp
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
                            items(words, key = { "category_word_${it.id}" }) { word ->
                                Box(modifier = Modifier.animateListItem()) {
                                    WordListItemPremium(
                                        word = word,
                                        onClick = { onNavigateToWordDetail(word.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Local Copies of Helper Components (To avoid modifying WordListScreen.kt) ---

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

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
                if (query.isEmpty()) {
                    Text(
                        text = "搜索：汉字 / 假名 / 释义",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
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
    // Determine color based on ID hash
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
    val shadowElevation = if (isDark) 2.dp else 4.dp
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

private val AvatarColors = listOf(
    NemoPrimary,
    NemoOrange,
    NemoSecondary,
    NemoIndigo,
    NemoTeal,
    NemoPurple,
    IosColors.Pink,
    NemoCyan
)
