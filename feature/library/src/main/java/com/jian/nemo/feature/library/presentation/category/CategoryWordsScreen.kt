package com.jian.nemo.feature.library.presentation.category

import com.jian.nemo.core.designsystem.theme.screenBackground

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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jian.nemo.core.ui.animation.animateListItem
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBar
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBarStyle
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryOption
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader


private enum class StudyFilter {
    ALL,      // 全部
    LEARNED,  // 已学
    UNLEARNED // 未学
}


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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

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
    var filterState by rememberSaveable { mutableStateOf(StudyFilter.ALL) }

    // Filter logic
    val filteredWordsByLevel = remember(uiState.wordsByLevel, searchQuery, filterState) {
        uiState.wordsByLevel.mapValues { (_, words) ->
            words.filter { word ->
                // 1. 过滤已学/未学/全部
                val matchesFilter = when (filterState) {
                    StudyFilter.ALL -> true
                    StudyFilter.LEARNED -> word.isLearned
                    StudyFilter.UNLEARNED -> !word.isLearned
                }
                if (!matchesFilter) return@filter false

                // 2. 过滤搜索词
                if (searchQuery.isBlank()) {
                    true
                } else {
                    word.japanese.contains(searchQuery, ignoreCase = true) ||
                    word.hiragana.contains(searchQuery, ignoreCase = true) ||
                    word.chinese.contains(searchQuery, ignoreCase = true)
                }
            }
        }.filterValues { it.isNotEmpty() }
    }

    // 当搜索词改变且不为空时，自动展开所有有结果的分类
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            filteredWordsByLevel.keys.forEach { level ->
                if (!expandedLevels.contains(level)) {
                    expandedLevels.add(level)
                }
            }
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Column(modifier = Modifier.background(backgroundColor)) {
                CommonHeader(
                    title = if(uiState.isLoading) categoryTitle else "$categoryTitle (${uiState.words.size})",
                    onBack = onNavigateBack,
                    backgroundColor = backgroundColor
                )
                val options = remember {
                    listOf(
                        DiscoveryOption(
                            label = "全部",
                            icon = Icons.Default.Book,
                            activeColor = NemoIndigo,
                        ),
                        DiscoveryOption(
                            label = "已学",
                            icon = Icons.Default.CheckCircle,
                            activeColor = NemoSecondary,
                        ),
                        DiscoveryOption(
                            label = "未学",
                            icon = Icons.Default.Error,
                            activeColor = NemoOrange,
                        ),
                    )
                }
                val selectedIndex = when (filterState) {
                    StudyFilter.ALL -> 0
                    StudyFilter.LEARNED -> 1
                    StudyFilter.UNLEARNED -> 2
                }

                DiscoveryBar(
                    options = options,
                    selectedOptionIndex = selectedIndex,
                    onOptionSelected = { index ->
                        val newFilter = when (index) {
                            0 -> StudyFilter.ALL
                            1 -> StudyFilter.LEARNED
                            2 -> StudyFilter.UNLEARNED
                            else -> StudyFilter.ALL
                        }
                        filterState = newFilter
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    searchPlaceholder = "搜索：汉字 / 假名 / 释义",
                    style = DiscoveryBarStyle(
                        backgroundColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White,
                        inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        activeTextStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    ),
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
                        val isExpanded = expandedLevels.contains(level)
                        val levelColor = getLevelColor(level)

                        stickyHeader {
                            LevelHeader(
                                level = level,
                                count = words.size,
                                isExpanded = isExpanded,
                                color = levelColor,
                                onToggle = {
                                    if (expandedLevels.contains(level)) {
                                        expandedLevels.remove(level)
                                    } else {
                                        expandedLevels.add(level)
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
private fun LevelHeader(
    level: String,
    count: Int,
    isExpanded: Boolean,
    color: Color,
    onToggle: () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

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
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
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
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NotoSerifJP,
                        localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                    ),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.W900,
                    color = avatarColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.japanese,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NotoSerifJP,
                            localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                        ),
                        fontWeight = FontWeight.W800,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                }

                Spacer(modifier = Modifier.height(4.dp))

                val secondaryAnnotatedText = buildAnnotatedString {
                    if (word.hiragana.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                fontFamily = NotoSerifJP,
                                localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                            )
                        ) {
                            append(word.hiragana)
                        }
                    }
                    if (word.hiragana.isNotEmpty() && word.chinese.isNotEmpty()) {
                        append(" · ")
                    }
                    if (word.chinese.isNotEmpty()) {
                        withStyle(
                            SpanStyle(
                                fontFamily = NotoSerifSC,
                                localeList = androidx.compose.ui.text.intl.LocaleList("zh")
                            )
                        ) {
                            append(word.chinese)
                        }
                    }
                }

                Text(
                    text = secondaryAnnotatedText,
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

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 22.dp, isDark = isDark)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
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
