package com.jian.nemo.feature.library.presentation.list

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.jian.nemo.core.ui.component.common.NemoSnackbar
import com.jian.nemo.core.ui.component.common.NemoSnackbarType
import com.jian.nemo.core.ui.compositionlocal.LocalSettingIconStyle
import com.jian.nemo.feature.library.R
import com.jian.nemo.feature.library.presentation.components.CollapsibleLibraryHeader
import com.jian.nemo.feature.library.presentation.components.LibraryEmptyState
import com.jian.nemo.feature.library.presentation.components.LibraryItemSkeletonCard
import com.jian.nemo.feature.library.presentation.components.LibraryScrollToTopButton
import com.jian.nemo.feature.library.presentation.components.WordListItemPremium
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * 单词列表界面。
 *
 * @param onNavigateBack 返回上一页回调。
 * @param onNavigateToWordDetail 点击单词进入详情页回调。
 * @param viewModel 词库单词列表状态视图模型。
 */
@Composable
fun WordListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWordDetail: (Int) -> Unit,
    viewModel: WordListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val iconStyle by viewModel.iconStyle.collectAsStateWithLifecycle()

    WordListContent(
        uiState = uiState,
        searchQuery = searchQuery,
        iconStyle = iconStyle,
        onNavigateBack = onNavigateBack,
        onWordClick = onNavigateToWordDetail,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onFilterSelected = viewModel::onFilterStateChanged,
        onLevelSelected = viewModel::onSelectedLevelChanged,
        onRefresh = viewModel::onRefresh,
        onClearSyncMessage = viewModel::clearSyncMessage
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun WordListContent(
    uiState: WordListUiState,
    searchQuery: String,
    iconStyle: SettingIconStyle,
    onNavigateBack: () -> Unit,
    onWordClick: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (StudyFilter) -> Unit,
    onLevelSelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onClearSyncMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isSoftTranslucent = iconStyle == SettingIconStyle.SOFT_TRANSLUCENT
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    CompositionLocalProvider(LocalSettingIconStyle provides iconStyle) {
        val coroutineScope = rememberCoroutineScope()
        val lazyListState = rememberLazyListState()
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

        val currentWords = remember(uiState.allWords, uiState.wordsByLevel, selectedLevel) {
            if (selectedLevel == WordListUiState.LEVEL_ALL) {
                uiState.allWords
            } else {
                uiState.wordsByLevel[selectedLevel] ?: emptyList()
            }
        }

        val backdrop = rememberLayerBackdrop()
        val isTransitionSettled = rememberIsTransitionSettled()
        val activeBackdrop = if (isTransitionSettled) backdrop else null
        val layoutDirection = LocalLayoutDirection.current
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val bottomInset = WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues().calculateBottomPadding()
        val horizontalSafeInsets = WindowInsets.displayCutout.union(WindowInsets.navigationBars).asPaddingValues()
        val safeStart = horizontalSafeInsets.calculateStartPadding(layoutDirection)
        val safeEnd = horizontalSafeInsets.calculateEndPadding(layoutDirection)

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

                val pullToRefreshState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = statusBarTop + 132.dp),
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
                            start = safeStart,
                            end = safeEnd,
                            top = statusBarTop + 128.dp + 16.dp,
                            bottom = bottomInset + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (uiState.isLoading) {
                            items(
                                count = 10,
                                key = { index -> -index - 1 },
                                contentType = { "word_skeleton" }
                            ) { index ->
                                LibraryItemSkeletonCard(
                                    index = index,
                                    isDark = isDark,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        } else if (currentWords.isEmpty()) {
                            item(key = "empty_state", contentType = "word_empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 40.dp, bottom = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val (emptyTitle, emptySubtitle) = when {
                                        searchQuery.isNotEmpty() ->
                                            stringResource(R.string.lib_word_list_empty_search_title) to
                                                stringResource(R.string.lib_word_list_empty_search_desc)
                                        uiState.filterState == StudyFilter.UNLEARNED ->
                                            (if (selectedLevel != WordListUiState.LEVEL_ALL) {
                                                stringResource(R.string.lib_word_list_empty_level_unlearned_title)
                                            } else {
                                                stringResource(R.string.lib_word_list_empty_unlearned_title)
                                            }) to stringResource(R.string.lib_word_list_empty_unlearned_desc)
                                        uiState.filterState == StudyFilter.LEARNED ->
                                            (if (selectedLevel != WordListUiState.LEVEL_ALL) {
                                                stringResource(R.string.lib_word_list_empty_level_learned_title)
                                            } else {
                                                stringResource(R.string.lib_word_list_empty_learned_title)
                                            }) to stringResource(R.string.lib_word_list_empty_learned_desc)
                                        selectedLevel != WordListUiState.LEVEL_ALL ->
                                            stringResource(R.string.lib_word_list_empty_level_title) to
                                                stringResource(R.string.lib_word_list_empty_level_desc)
                                        else ->
                                            stringResource(R.string.lib_word_list_empty_default_title) to
                                                stringResource(R.string.lib_word_list_empty_default_desc)
                                    }
                                    val emptyIcon = if (searchQuery.isNotEmpty()) {
                                        Icons.Rounded.Search
                                    } else {
                                        Icons.Rounded.Translate
                                    }
                                    LibraryEmptyState(
                                        title = emptyTitle,
                                        subtitle = emptySubtitle,
                                        icon = emptyIcon,
                                        accentColor = SystemColors.Indigo,
                                        isSoftTranslucent = isSoftTranslucent
                                    )
                                }
                            }
                        } else {
                            items(
                                items = currentWords,
                                key = { it.id },
                                contentType = { "word_item" }
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

            val headerTitle = if (selectedLevel == WordListUiState.LEVEL_ALL) {
                stringResource(R.string.lib_word_list_header_title)
            } else {
                stringResource(R.string.lib_word_list_header_title_with_level, selectedLevel)
            }
            CollapsibleLibraryHeader(
                title = headerTitle,
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
                searchPlaceholder = stringResource(R.string.lib_word_list_search_placeholder),
                isDark = isDark,
                backgroundColor = backgroundColor
            )

            LibraryScrollToTopButton(
                lazyListState = lazyListState,
                backdrop = activeBackdrop,
                isMenuExpanded = isMenuExpanded,
                bottomPadding = bottomInset + 20.dp
            )

            val failedKeyword = stringResource(R.string.library_list_label_sync_failed_keyword)
            val isSyncError = uiState.syncMessage?.contains(failedKeyword) == true
            NemoSnackbar(
                visible = uiState.syncMessage != null,
                message = uiState.syncMessage.orEmpty(),
                type = if (isSyncError) NemoSnackbarType.ERROR else NemoSnackbarType.SUCCESS,
                icon = if (isSyncError) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                onDismiss = onClearSyncMessage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = statusBarTop + 68.dp)
            )
        }
    }
}
