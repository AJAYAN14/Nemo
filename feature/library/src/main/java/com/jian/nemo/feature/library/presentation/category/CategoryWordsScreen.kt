package com.jian.nemo.feature.library.presentation.category

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.jian.nemo.core.designsystem.theme.IosColors
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.core.domain.repository.SettingIconStyle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.jian.nemo.feature.library.R
import com.jian.nemo.core.designsystem.theme.*
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.ui.animation.animateListItem
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBar
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryBarStyle
import com.jian.nemo.core.ui.component.discoverybar.DiscoveryOption
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.library.presentation.components.FloatingHeaderGradientContainer
import com.jian.nemo.feature.library.presentation.components.LibraryDiscoveryBar
import com.jian.nemo.feature.library.presentation.components.LibraryLevelSelectionButton
import com.jian.nemo.feature.library.presentation.components.LibraryLevelSelectionMenu
import com.jian.nemo.feature.library.presentation.components.LibraryLevelSelectionOverlay
import com.jian.nemo.feature.library.presentation.components.LibraryMenuScrimOverlay
import com.jian.nemo.feature.library.presentation.components.LibraryScrollToTopButton
import com.jian.nemo.feature.library.presentation.components.WordListItemPremium
import com.jian.nemo.feature.library.presentation.list.StudyFilter
import kotlinx.coroutines.launch

/**
 * 分类单词列表界面 (接入 LibraryListComponents 通用组件)
 */
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
    val iconStyle by viewModel.iconStyle.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    CompositionLocalProvider(LocalSettingIconStyle provides iconStyle) {
        val backdrop = rememberLayerBackdrop()
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        var searchQuery by rememberSaveable { mutableStateOf("") }
        var filterState by rememberSaveable { mutableStateOf(StudyFilter.ALL) }
        var selectedLevel by rememberSaveable { mutableStateOf("ALL") }
        var isMenuExpanded by rememberSaveable { mutableStateOf(false) }

        // 过滤词汇数据
        val currentWords = remember(uiState.words, selectedLevel, searchQuery, filterState) {
            uiState.words.filter { word ->
                val matchesFilter = when (filterState) {
                    StudyFilter.ALL -> true
                    StudyFilter.LEARNED -> word.isLearned
                    StudyFilter.UNLEARNED -> !word.isLearned
                }
                if (!matchesFilter) return@filter false

                val matchesLevel = if (selectedLevel == "ALL") true else word.level.equals(selectedLevel, ignoreCase = true)
                if (!matchesLevel) return@filter false

                if (searchQuery.isBlank()) {
                    true
                } else {
                    word.japanese.contains(searchQuery, ignoreCase = true) ||
                    word.hiragana.contains(searchQuery, ignoreCase = true) ||
                    word.chinese.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        // 计算各等级词汇数量
        val countsByLevel = remember(uiState.words, searchQuery, filterState) {
            val baseFiltered = uiState.words.filter { word ->
                val matchesFilter = when (filterState) {
                    StudyFilter.ALL -> true
                    StudyFilter.LEARNED -> word.isLearned
                    StudyFilter.UNLEARNED -> !word.isLearned
                }
                if (!matchesFilter) return@filter false

                if (searchQuery.isBlank()) {
                    true
                } else {
                    word.japanese.contains(searchQuery, ignoreCase = true) ||
                    word.hiragana.contains(searchQuery, ignoreCase = true) ||
                    word.chinese.contains(searchQuery, ignoreCase = true)
                }
            }

            listOf("ALL", "N1", "N2", "N3", "N4", "N5").associateWith { lvl ->
                if (lvl == "ALL") baseFiltered.size
                else baseFiltered.count { it.level.equals(lvl, ignoreCase = true) }
            }
        }

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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    NemoChasingDotsLoader()
                }
            } else if (uiState.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = uiState.error ?: stringResource(R.string.lib_category_words_error_load_failed), color = MaterialTheme.colorScheme.error)
                }
            } else {
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
                        selectedFilter = filterState,
                        onFilterSelected = { filterState = it },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        searchPlaceholder = stringResource(R.string.lib_category_words_search_placeholder)
                    )
                }

                // 2. 单词平铺列表
                if (currentWords.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val emptyIcon: androidx.compose.ui.graphics.vector.ImageVector =
                                if (searchQuery.isNotEmpty()) Icons.Rounded.Search else Icons.Rounded.Inbox
                            val emptyMessage = when {
                                searchQuery.isNotEmpty() -> stringResource(R.string.lib_category_words_empty_search)
                                selectedLevel != "ALL" -> stringResource(R.string.lib_category_words_empty_level)
                                else -> stringResource(R.string.lib_category_words_empty_category)
                            }
                            EmptyState(
                                message = emptyMessage,
                                icon = emptyIcon
                            )
                        }
                    }
                } else {
                    items(currentWords, key = { "category_word_${it.id}" }) { word ->
                        Box(modifier = Modifier.animateListItem().padding(horizontal = 16.dp)) {
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

        // 2. 悬浮顶栏与液态玻璃按钮（带柔和垂直渐变保护遮罩，读取 backdrop）
        FloatingHeaderGradientContainer(backgroundColor = backgroundColor) {
            val titleText = if (selectedLevel == "ALL") {
                if (uiState.isLoading) categoryTitle else stringResource(R.string.lib_category_words_title_with_count, categoryTitle, currentWords.size)
            } else {
                stringResource(R.string.lib_category_words_title_with_level_count, categoryTitle, selectedLevel, currentWords.size)
            }

            CommonHeader(
                title = titleText,
                onBack = onNavigateBack,
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

        // 2.5 悬浮回顶按钮（严格锁定 30/48 黄金比例：56.dp 按钮对应 35.dp 图标，纯净磨砂底色）
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
            countsByLevel = countsByLevel,
            backdrop = backdrop,
            onScrollToTop = {
                coroutineScope.launch { lazyListState.scrollToItem(0) }
            },
            modifier = Modifier.zIndex(10f)
        )
    }
    }
}

@Composable
private fun EmptyState(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Inbox
) {
    val iconStyle = LocalSettingIconStyle.current
    val isSoftTranslucent = (iconStyle == SettingIconStyle.MD3)
    val accentColor = IosColors.Blue
    val containerBg = if (isSoftTranslucent) accentColor.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.2f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(containerBg, RoundedRectangle(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
