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
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.luminance
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = MaterialTheme.colorScheme.background

    val expandedLevels = rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<String>() }

    val filteredGrammarsByLevel = uiState.grammarsByLevel
    val searchQuery = uiState.searchQuery

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
                val selectedIndex = when (uiState.filterState) {
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
                        viewModel.onFilterStateChanged(newFilter)
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                    searchPlaceholder = "搜索：语法 / 解释",
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
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NemoChasingDotsLoader()
                }
            } else {
                val pullToRefreshState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.onRefresh() },
                    state = pullToRefreshState,
                    indicator = {
                        androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                            modifier = Modifier.align(Alignment.TopCenter),
                            isRefreshing = uiState.isRefreshing,
                            state = pullToRefreshState,
                            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxSize()
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
            

            // 同步通知组件
            com.jian.nemo.core.ui.component.common.NemoSnackbar(
                visible = uiState.syncMessage != null,
                message = uiState.syncMessage ?: "",
                type = if (uiState.syncMessage?.contains("失败") == true)
                    com.jian.nemo.core.ui.component.common.NemoSnackbarType.ERROR
                else
                    com.jian.nemo.core.ui.component.common.NemoSnackbarType.SUCCESS,
                icon = if (uiState.syncMessage?.contains("失败") == true)
                    Icons.Rounded.Warning
                else
                    Icons.Rounded.CheckCircle,
                onDismiss = { viewModel.clearSyncMessage() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
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
            Text(level, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = RoundedCornerShape(22.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 22.dp, isDark = isDark)
            .graphicsLayer { scaleX = scale; scaleY = scale },
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
