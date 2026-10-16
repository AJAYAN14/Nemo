package com.jian.nemo.feature.library.presentation.list

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
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBar
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBarStyle
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryOption
import com.jian.nemo.core.ui.navigation.NavDestination
import com.jian.nemo.core.common.util.GrammarSearchUtils
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import androidx.compose.ui.graphics.Brush
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.component.common.NemoMorphMenu
import com.jian.nemo.core.ui.component.common.NemoMorphMenuItem
import com.jian.nemo.feature.library.presentation.components.FloatingHeaderGradientContainer
import com.jian.nemo.feature.library.presentation.components.LibraryLevelSelectionMenu
import com.jian.nemo.feature.library.presentation.components.LibraryMenuScrimOverlay
import com.jian.nemo.feature.library.presentation.components.LibraryScrollToTopButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch


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
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    var selectedLevel by rememberSaveable { mutableStateOf("ALL") }
    var isMenuExpanded by remember { mutableStateOf(false) }

    // 动态高斯模糊半径 (0dp -> 14dp 平滑过渡，对齐学习界面景深规范)
    val animatedBlurRadius by animateDpAsState(
        targetValue = if (isMenuExpanded) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "grammarListBlur"
    )

    // 回顶悬浮按钮触发状态（滑出首屏后优雅显现）
    val showScrollToTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 200
        }
    }

    val filteredGrammarsByLevel = uiState.grammarsByLevel
    val searchQuery = uiState.searchQuery

    // 当前选中的语法扁平化列表（直接展示所有语法，无手风琴折叠阻碍）
    val currentGrammars = remember(filteredGrammarsByLevel, selectedLevel) {
        if (selectedLevel == "ALL") {
            val sortedKeys = filteredGrammarsByLevel.keys.sorted()
            sortedKeys.flatMap { filteredGrammarsByLevel[it] ?: emptyList() }
        } else {
            filteredGrammarsByLevel[selectedLevel] ?: emptyList()
        }
    }

    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
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
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = statusBarTop + 60.dp),
                        isRefreshing = uiState.isRefreshing,
                        state = pullToRefreshState,
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (animatedBlurRadius > 0.dp) Modifier.blur(animatedBlurRadius) else Modifier
                    )
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(backdrop),
                    contentPadding = PaddingValues(
                        top = statusBarTop + 60.dp,
                        bottom = navBarBottom + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. 搜索与筛选栏（移入列表作为第一项，随列表顺畅滚动）
                    item(key = "discovery_bar") {
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
                    }

                    // 2. 语法直接平铺列表
                    if (currentGrammars.isEmpty()) {
                        item(key = "empty_state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyState(
                                    if (searchQuery.isNotEmpty()) "未找到相关语法"
                                    else if (selectedLevel != "ALL") "当前等级暂无语法"
                                    else "暂无语法数据"
                                )
                            }
                        }
                    } else {
                        items(currentGrammars, key = { it.id }) { grammar ->
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

        // 1.5 页面全屏高斯模糊景深遮罩 (对齐学习界面：12% 柔光暗化 + 200ms 淡入淡出，点击外部收起菜单)
        LibraryMenuScrimOverlay(
            visible = isMenuExpanded,
            onDismiss = { isMenuExpanded = false }
        )

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
        FloatingHeaderGradientContainer(backgroundColor = backgroundColor) {
            val headerTitle = if (selectedLevel == "ALL") "语法列表" else "$selectedLevel 语法"
            CommonHeader(
                title = headerTitle,
                onBack = { navController.navigateUp() },
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                actions = {
                    LibraryLevelSelectionMenu(
                        expanded = isMenuExpanded,
                        onExpandedChange = { isMenuExpanded = it },
                        selectedLevel = selectedLevel,
                        onLevelSelected = { selectedLevel = it },
                        countsByLevel = remember(filteredGrammarsByLevel) {
                            filteredGrammarsByLevel.mapValues { it.value.size }
                        },
                        backdrop = backdrop,
                        onScrollToTop = {
                            coroutineScope.launch { lazyListState.scrollToItem(0) }
                        }
                    )
                }
            )
        }

        // 2.5 悬浮回顶按钮（严格锁定 30/48 黄金比例：56.dp 按钮对应 35.dp 图标，纯净磨砂底色，无偏红）
        LibraryScrollToTopButton(
            lazyListState = lazyListState,
            backdrop = backdrop,
            isMenuExpanded = isMenuExpanded,
            bottomPadding = navBarBottom + 20.dp
        )

        // 3. 同步通知组件
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
                .padding(top = statusBarTop + 68.dp)
        )
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
            Column(modifier = Modifier.weight(1f)) {
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
            if (grammar.grammarLevel.isNotBlank()) {
                val levelColor = getLevelColor(grammar.grammarLevel)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(levelColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = grammar.grammarLevel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = levelColor
                    )
                }
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
