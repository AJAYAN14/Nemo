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
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.domain.repository.SettingIconStyle
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
import androidx.compose.ui.zIndex
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
import com.jian.nemo.feature.library.presentation.components.LibraryDiscoveryBar
import com.jian.nemo.feature.library.presentation.components.LibraryLevelSelectionButton
import com.jian.nemo.feature.library.presentation.components.LibraryLevelSelectionMenu
import com.jian.nemo.feature.library.presentation.components.LibraryLevelSelectionOverlay
import com.jian.nemo.feature.library.presentation.components.LibraryMenuScrimOverlay
import com.jian.nemo.feature.library.presentation.components.LibraryScrollToTopButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.library.R
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
    val iconStyle by viewModel.iconStyle.collectAsStateWithLifecycle()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    CompositionLocalProvider(LocalSettingIconStyle provides iconStyle) {
        val coroutineScope = rememberCoroutineScope()
        val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
        var selectedLevel by rememberSaveable { mutableStateOf("ALL") }
        var isMenuExpanded by remember { mutableStateOf(false) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. 底层全量画布（滚动内容整体录入 backdrop，提供真实折射源）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            // 底层实色背景（录入 backdrop，消除空白区域透明空洞）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            )

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
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = statusBarTop + 60.dp + 16.dp,
                        bottom = navBarBottom + 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. 搜索与筛选栏（移入列表作为第一项，随列表顺畅滚动）
                    item(key = "discovery_bar") {
                        LibraryDiscoveryBar(
                            selectedFilter = uiState.filterState,
                            onFilterSelected = { viewModel.onFilterStateChanged(it) },
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                            searchPlaceholder = stringResource(R.string.lib_grammar_list_search_placeholder)
                        )
                    }

                    // 2. 语法直接平铺列表
                    if (currentGrammars.isEmpty()) {
                        item(key = "empty_state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 56.dp, bottom = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val (emptyTitle, emptySubtitle) = when {
                                    searchQuery.isNotEmpty() ->
                                        stringResource(R.string.lib_grammar_list_empty_search_title) to stringResource(R.string.lib_grammar_list_empty_search_desc)
                                    uiState.filterState == StudyFilter.UNLEARNED ->
                                        (if (selectedLevel != "ALL") stringResource(R.string.lib_grammar_list_empty_level_unlearned_title) else stringResource(R.string.lib_grammar_list_empty_unlearned_title)) to stringResource(R.string.lib_grammar_list_empty_unlearned_desc)
                                    uiState.filterState == StudyFilter.LEARNED ->
                                        (if (selectedLevel != "ALL") stringResource(R.string.lib_grammar_list_empty_level_learned_title) else stringResource(R.string.lib_grammar_list_empty_learned_title)) to stringResource(R.string.lib_grammar_list_empty_learned_desc)
                                    selectedLevel != "ALL" ->
                                        stringResource(R.string.lib_grammar_list_empty_level_title) to stringResource(R.string.lib_grammar_list_empty_level_desc)
                                    else ->
                                        stringResource(R.string.lib_grammar_list_empty_default_title) to stringResource(R.string.lib_grammar_list_empty_default_desc)
                                }
                                val emptyIcon = if (searchQuery.isNotEmpty()) {
                                    Icons.Rounded.Search
                                } else {
                                    Icons.AutoMirrored.Rounded.MenuBook
                                }
                                EmptyState(
                                    title = emptyTitle,
                                    subtitle = emptySubtitle,
                                    icon = emptyIcon
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
    }

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
        FloatingHeaderGradientContainer(backgroundColor = backgroundColor) {
            val headerTitle = if (selectedLevel == "ALL") {
                stringResource(R.string.lib_grammar_list_header_title)
            } else {
                stringResource(R.string.lib_grammar_list_header_title_with_level, selectedLevel)
            }
            CommonHeader(
                title = headerTitle,
                onBack = { navController.navigateUp() },
                backgroundColor = Color.Transparent,
                backdrop = backdrop,
                backButtonSize = 48.dp,
                actions = {
                    LibraryLevelSelectionButton(
                        isExpanded = isMenuExpanded,
                        onClick = { isMenuExpanded = !isMenuExpanded },
                        backdrop = backdrop
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

        // 3. 页面全屏同级液态悬浮菜单浮层 (同级兄弟采样 backdrop，彻底激活物理透镜折射)
        LibraryLevelSelectionOverlay(
            isVisible = isMenuExpanded,
            onDismiss = { isMenuExpanded = false },
            selectedLevel = selectedLevel,
            onLevelSelected = { selectedLevel = it },
            countsByLevel = remember(filteredGrammarsByLevel) {
                filteredGrammarsByLevel.mapValues { it.value.size }
            },
            backdrop = backdrop,
            onScrollToTop = {
                coroutineScope.launch { lazyListState.scrollToItem(0) }
            },
            modifier = Modifier.zIndex(10f)
        )

        // 3. 同步通知组件
        com.jian.nemo.core.ui.component.common.NemoSnackbar(
            visible = uiState.syncMessage != null,
            message = uiState.syncMessage ?: "",
            type = if (uiState.syncMessage?.contains(stringResource(R.string.library_list_label_sync_failed_keyword)) == true)
                com.jian.nemo.core.ui.component.common.NemoSnackbarType.ERROR
            else
                com.jian.nemo.core.ui.component.common.NemoSnackbarType.SUCCESS,
            icon = if (uiState.syncMessage?.contains(stringResource(R.string.library_list_label_sync_failed_keyword)) == true)
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
}

private val AvatarPalette = listOf(
    SystemColors.Blue,
    SystemColors.Orange,
    SystemColors.Green,
    SystemColors.Indigo,
    SystemColors.Teal,
    SystemColors.Purple,
    SystemColors.Pink,
    SystemColors.Cyan
)

@Composable
private fun getLevelColor(level: String): Color {
    return when (level.uppercase()) {
        "N5" -> SystemColors.Green
        "N4" -> SystemColors.Cyan
        "N3" -> SystemColors.Blue
        "N2" -> SystemColors.Orange
        "N1" -> SystemColors.Pink
        else -> SystemColors.Blue
    }
}

@Composable
private fun GrammarListItemPremium(grammar: Grammar, onClick: () -> Unit) {
    val iconStyle = LocalSettingIconStyle.current
    val avatarColor = AvatarPalette[kotlin.math.abs(grammar.id.hashCode()) % AvatarPalette.size]
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val containerBg = if (isSoftTranslucent) avatarColor.copy(alpha = 0.15f) else avatarColor
    val contentColor = if (isSoftTranslucent) avatarColor else Color.White

    PremiumCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(containerBg, RoundedRectangle(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.library_grammar_list_label_avatar_badge),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = NotoSerifJP,
                        localeList = androidx.compose.ui.text.intl.LocaleList("ja")
                    ),
                    fontWeight = FontWeight.W900,
                    color = contentColor
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
                val levelBg = if (isSoftTranslucent) levelColor.copy(alpha = 0.15f) else levelColor
                val levelText = if (isSoftTranslucent) levelColor else Color.White
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedRectangle(8.dp))
                        .background(levelBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = grammar.grammarLevel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = levelText
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.AutoMirrored.Rounded.MenuBook
) {
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.SOFT_TRANSLUCENT)
    val accentColor = SystemColors.Indigo
    val containerBg = if (isSoftTranslucent) accentColor.copy(alpha = 0.15f) else accentColor
    val iconTint = if (isSoftTranslucent) accentColor else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(containerBg, RoundedRectangle(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = iconTint
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
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
        shape = RoundedRectangle(22.dp),
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
