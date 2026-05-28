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
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.navigation.NavDestination
import com.jian.nemo.core.common.util.GrammarSearchUtils
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader


/**
 * 语法列表界面 (UI/UX Pro Max)
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GrammarListScreen(
    navController: NavController,
    viewModel: GrammarListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = isSystemInDarkTheme()
    val backgroundColor = MaterialTheme.colorScheme.background

    val expandedLevels = rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }

    val filteredGrammarsByLevel = uiState.grammarsByLevel
    val searchQuery = uiState.searchQuery
    var showFilterMenu by remember { mutableStateOf(false) }

    // 当搜索词改变且不为空时，自动展开所有有结果的分类
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            filteredGrammarsByLevel.keys.forEach { level ->
                if (!expandedLevels.contains(level)) {
                    expandedLevels.add(level)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(backgroundColor)) {
                CommonHeader(
                    title = "语法列表",
                    onBack = { navController.navigateUp() },
                    backgroundColor = backgroundColor,
                    actions = {
                        Box {
                            IconButton(onClick = { showFilterMenu = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.Layers,
                                    contentDescription = "筛选",
                                    tint = if (uiState.filterState != StudyFilter.ALL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = showFilterMenu,
                                onDismissRequest = { showFilterMenu = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = if (isDark) Color.Black else Color.White
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "全部",
                                            fontWeight = if (uiState.filterState == StudyFilter.ALL) FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.filterState == StudyFilter.ALL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.onFilterStateChanged(StudyFilter.ALL)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(NemoIndigo.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Apps,
                                                contentDescription = null,
                                                tint = NemoIndigo,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (uiState.filterState == StudyFilter.ALL) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "已学",
                                            fontWeight = if (uiState.filterState == StudyFilter.LEARNED) FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.filterState == StudyFilter.LEARNED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.onFilterStateChanged(StudyFilter.LEARNED)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(NemoSecondary.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DoneAll,
                                                contentDescription = null,
                                                tint = NemoSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (uiState.filterState == StudyFilter.LEARNED) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "未学",
                                            fontWeight = if (uiState.filterState == StudyFilter.UNLEARNED) FontWeight.Bold else FontWeight.Normal,
                                            color = if (uiState.filterState == StudyFilter.UNLEARNED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.onFilterStateChanged(StudyFilter.UNLEARNED)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(NemoOrange.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.HourglassEmpty,
                                                contentDescription = null,
                                                tint = NemoOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        if (uiState.filterState == StudyFilter.UNLEARNED) {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                )
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = "搜索：语法 / 解释",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                NemoChasingDotsLoader()
            }
        } else {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.onRefresh() },
                modifier = Modifier.padding(innerPadding)
            ) {
                if (filteredGrammarsByLevel.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState("未找到相关语法")
                    }
                } else if (filteredGrammarsByLevel.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState("暂无语法数据")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val sortedLevels = filteredGrammarsByLevel.keys.sorted()
                        sortedLevels.forEach { level ->
                            val grammars = filteredGrammarsByLevel[level] ?: emptyList()
                            val isExpanded = expandedLevels.contains(level)
                            val levelColor = getLevelColor(level)

                            stickyHeader {
                                LevelHeader(
                                    level = level,
                                    count = grammars.size,
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
                                items(grammars, key = { it.id }) { grammar ->
                                    Box(modifier = Modifier.animateListItem().padding(horizontal = 16.dp)) {
                                        GrammarListItemPremium(
                                            grammar = grammar,
                                            onClick = { navController.navigate(NavDestination.grammarDetail(grammar.id)) }
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
}

private val AvatarColors = listOf(
    NemoPrimary, NemoOrange, NemoSecondary, NemoIndigo,
    NemoTeal, NemoPurple, IosColors.Pink, NemoCyan
)

private fun getLevelColor(level: String): Color {
    return when (level.uppercase()) {
        "N5" -> NemoSecondary
        "N4" -> NemoCyan
        "N3" -> NemoPrimary
        "N2" -> NemoOrange
        "N1" -> IosColors.Pink
        else -> NemoPrimary
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surface
    
    // 使用本地 String 状态管理，获得极致打字体验
    var text by remember { mutableStateOf(query) }

    // 仅在必要时（如清除或初始值加载）同步外部 query
    LaunchedEffect(query) {
        if (query != text) {
            if (query.isEmpty() || text.isEmpty()) {
                text = query
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(25.dp),
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                BasicTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onQueryChange(it)
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (text.isNotEmpty()) {
                IconButton(onClick = { 
                    text = ""
                    onQueryChange("") 
                }) {
                    Icon(Icons.Rounded.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(level, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("$count 条", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                null,
                modifier = Modifier.graphicsLayer { rotationZ = if (isExpanded) 180f else 0f }
            )
        }
    }
}

@Composable
private fun GrammarListItemPremium(grammar: Grammar, onClick: () -> Unit) {
    val avatarColor = AvatarColors[kotlin.math.abs(grammar.id.hashCode()) % AvatarColors.size]
    PremiumCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(50.dp).background(avatarColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "文",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NotoSerifJP,
                        localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                    ),
                    fontWeight = FontWeight.W900,
                    color = avatarColor
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = formatMixedCnHnString(GrammarSearchUtils.cleanRubi(grammar.grammar)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W800,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = GrammarSearchUtils.cleanRubi(grammar.getFirstExplanation()),
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.Inbox, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PremiumCard(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(22.dp),
        color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MaterialTheme.colorScheme.surfaceContainer else Color.White,
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.shadow(4.dp, RoundedCornerShape(22.dp)),
        interactionSource = interactionSource
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

private fun formatMixedCnHnString(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val codePoint = text.codePointAt(i)
            val charCount = Character.charCount(codePoint)
            val nextStr = text.substring(i, i + charCount)
            val isKana = nextStr.any { c ->
                (c in '\u3040'..'\u309F') || (c in '\u30A0'..'\u30FF')
            }
            if (isKana) {
                withStyle(
                    SpanStyle(
                        fontFamily = NotoSerifJP,
                        localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                    )
                ) {
                    append(nextStr)
                }
            } else {
                withStyle(
                    SpanStyle(
                        fontFamily = NotoSerifSC,
                        localeList = androidx.compose.ui.text.intl.LocaleList("zh")
                    )
                ) {
                    append(nextStr)
                }
            }
            i += charCount
        }
    }
}
