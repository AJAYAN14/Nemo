package com.jian.nemo.feature.library.presentation.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jian.nemo.core.designsystem.theme.SystemColors
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.ui.animation.animateListItem
import com.jian.nemo.core.ui.animation.rememberIsTransitionSettled
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.feature.library.R
import com.jian.nemo.feature.library.presentation.components.CollapsibleLibraryHeader
import com.jian.nemo.feature.library.presentation.components.LibraryEmptyState
import com.jian.nemo.feature.library.presentation.components.LibraryScrollToTopButton
import com.jian.nemo.feature.library.presentation.components.WordListItemPremium
import com.jian.nemo.feature.library.presentation.list.StudyFilter
import com.jian.nemo.feature.library.presentation.list.WordListUiState
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * 分类单词列表界面。
 *
 * @param category 词性分类标识。
 * @param categoryTitle 分类展示标题。
 * @param onNavigateBack 返回上一页回调。
 * @param onNavigateToWordDetail 点击单词进入详情页回调。
 * @param viewModel 分类单词状态视图模型。
 */
@Composable
fun CategoryWordsScreen(
    category: String,
    categoryTitle: String,
    onNavigateBack: () -> Unit,
    onNavigateToWordDetail: (Int) -> Unit = {},
    viewModel: CategoryWordsViewModel = hiltViewModel()
) {
    LaunchedEffect(category) {
        viewModel.loadWords(category)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val iconStyle by viewModel.iconStyle.collectAsStateWithLifecycle()

    CategoryWordsContent(
        categoryTitle = categoryTitle,
        uiState = uiState,
        searchQuery = searchQuery,
        iconStyle = iconStyle,
        onNavigateBack = onNavigateBack,
        onWordClick = onNavigateToWordDetail,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onFilterSelected = viewModel::onFilterStateChanged,
        onLevelSelected = viewModel::onSelectedLevelChanged
    )
}

@Composable
internal fun CategoryWordsContent(
    categoryTitle: String,
    uiState: CategoryWordsUiState,
    searchQuery: String,
    iconStyle: SettingIconStyle,
    onNavigateBack: () -> Unit,
    onWordClick: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (StudyFilter) -> Unit,
    onLevelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isSoftTranslucent = iconStyle == SettingIconStyle.SOFT_TRANSLUCENT
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    CompositionLocalProvider(LocalSettingIconStyle provides iconStyle) {
        val backdrop = rememberLayerBackdrop()
        val isTransitionSettled = rememberIsTransitionSettled()
        val activeBackdrop = if (isTransitionSettled) backdrop else null
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        val layoutDirection = LocalLayoutDirection.current
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding()
        val horizontalSafeInsets = WindowInsets.displayCutout.union(WindowInsets.navigationBars).asPaddingValues()
        val safeStart = horizontalSafeInsets.calculateStartPadding(layoutDirection)
        val safeEnd = horizontalSafeInsets.calculateEndPadding(layoutDirection)

        val selectedLevel = uiState.selectedLevel
        var isMenuExpanded by rememberSaveable { mutableStateOf(false) }

        val currentScrollResetKey = "${uiState.searchQuery}|${uiState.filterState.name}"
        var lastScrollResetKey by rememberSaveable { mutableStateOf(currentScrollResetKey) }
        LaunchedEffect(currentScrollResetKey) {
            if (currentScrollResetKey != lastScrollResetKey) {
                lastScrollResetKey = currentScrollResetKey
                lazyListState.scrollToItem(0)
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop = backdrop, enabled = isTransitionSettled)
            ) {
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
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = safeStart,
                            end = safeEnd,
                            top = statusBarTop + 128.dp + 16.dp,
                            bottom = bottomInset + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (uiState.filteredWords.isEmpty()) {
                            item(key = "empty_state", contentType = "category_word_empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp, bottom = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val emptyIcon = if (searchQuery.isNotEmpty()) {
                                        Icons.Rounded.Search
                                    } else {
                                        Icons.Rounded.Inbox
                                    }
                                    val emptyMessage = when {
                                        searchQuery.isNotEmpty() ->
                                            stringResource(R.string.lib_category_words_empty_search)
                                        selectedLevel != WordListUiState.LEVEL_ALL ->
                                            stringResource(R.string.lib_category_words_empty_level)
                                        else ->
                                            stringResource(R.string.lib_category_words_empty_category)
                                    }
                                    LibraryEmptyState(
                                        title = emptyMessage,
                                        icon = emptyIcon,
                                        accentColor = SystemColors.Blue,
                                        isSoftTranslucent = isSoftTranslucent
                                    )
                                }
                            }
                        } else {
                            items(
                                items = uiState.filteredWords,
                                key = { it.id },
                                contentType = { "category_word_item" }
                            ) { word ->
                                WordListItemPremium(
                                    word = word,
                                    onClick = { onWordClick(word.id) },
                                    isSoftTranslucent = isSoftTranslucent,
                                    isDark = isDark,
                                    modifier = Modifier
                                        .animateListItem()
                                        .padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            val titleText = if (selectedLevel == WordListUiState.LEVEL_ALL) {
                if (uiState.isLoading) {
                    categoryTitle
                } else {
                    stringResource(
                        R.string.lib_category_words_title_with_count,
                        categoryTitle,
                        uiState.filteredWords.size
                    )
                }
            } else {
                stringResource(
                    R.string.lib_category_words_title_with_level_count,
                    categoryTitle,
                    selectedLevel,
                    uiState.filteredWords.size
                )
            }

            CollapsibleLibraryHeader(
                title = titleText,
                onBack = onNavigateBack,
                lazyListState = lazyListState,
                selectedLevel = selectedLevel,
                countsByLevel = uiState.countsByLevel,
                onLevelSelected = onLevelSelected,
                onScrollToTop = {
                    coroutineScope.launch { lazyListState.scrollToItem(0) }
                },
                isMenuExpanded = isMenuExpanded,
                onMenuExpandedChange = { isMenuExpanded = it },
                backdrop = activeBackdrop,
                selectedFilter = uiState.filterState,
                onFilterSelected = onFilterSelected,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                searchPlaceholder = stringResource(R.string.lib_category_words_search_placeholder),
                isDark = isDark,
                backgroundColor = backgroundColor
            )

            LibraryScrollToTopButton(
                lazyListState = lazyListState,
                backdrop = activeBackdrop,
                isMenuExpanded = isMenuExpanded,
                bottomPadding = bottomInset + 20.dp
            )
        }
    }
}
