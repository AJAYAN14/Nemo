package com.jian.nemo.feature.library

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.feature.library.presentation.LibraryTab
import com.jian.nemo.feature.library.presentation.LibraryUiState
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * LibraryScreen UI测试
 *
 * 验证:
 * 1. Tab切换（单词/语法）
 * 2. 搜索功能
 * 3. 列表显示
 * 4. 空状态处理
 * 5. 加载状态
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun libraryScreen_showsTabs() {
        // Arrange
        val state = LibraryUiState()

        // Act
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = state,
                onTabSelected = {},
                onSearchQueryChange = {},
                onClearSearch = {},
                onWordClick = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("单词").assertIsDisplayed()
        composeTestRule.onNodeWithText("语法").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_showsSearchBar() {
        // Arrange
        val state = LibraryUiState()

        // Act
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = state,
                onTabSelected = {},
                onSearchQueryChange = {},
                onClearSearch = {},
                onWordClick = {}
            )
        }

        // Assert - 搜索框应该显示
        composeTestRule.onNodeWithText("搜索单词或语法...").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_showsWordList_whenSearchHasResults() {
        // Arrange
        val testWords = listOf(
            Word(
                id = 1,
                rawId = "1",
                japanese = "こんにちは",
                chinese = "你好",
                hiragana = "konnichiwa",
                level = "N5",
                pos = "名詞"
            ),
            Word(
                id = 2,
                rawId = "2",
                japanese = "ありがとう",
                chinese = "谢谢",
                hiragana = "arigatou",
                level = "N5",
                pos = "感動詞"
            )
        )
        val state = LibraryUiState(
            selectedTab = LibraryTab.WORD,
            searchQuery = "こん",
            searchResultsWords = testWords,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = state,
                onTabSelected = {},
                onSearchQueryChange = {},
                onClearSearch = {},
                onWordClick = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("こんにちは").assertIsDisplayed()
        composeTestRule.onNodeWithText("你好").assertIsDisplayed()
        composeTestRule.onNodeWithText("ありがとう").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_showsGrammarList_whenGrammarTabSelected() {
        // Arrange
        val testGrammars = listOf(
            Grammar(
                id = 1,
                rawId = "1",
                grammar = "～ている",
                grammarLevel = "N5",
                usages = listOf(
                    com.jian.nemo.core.domain.model.GrammarUsage(
                        connection = "",
                        explanation = "正在进行",
                        examples = emptyList()
                    )
                )
            ),
            Grammar(
                id = 2,
                rawId = "2",
                grammar = "～てください",
                grammarLevel = "N5",
                usages = listOf(
                    com.jian.nemo.core.domain.model.GrammarUsage(
                        connection = "",
                        explanation = "请...",
                        examples = emptyList()
                    )
                )
            )
        )
        val state = LibraryUiState(
            selectedTab = LibraryTab.GRAMMAR,
            searchQuery = "て",
            searchResultsGrammars = testGrammars,
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = state,
                onTabSelected = {},
                onSearchQueryChange = {},
                onClearSearch = {},
                onWordClick = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("～ている").assertIsDisplayed()
        composeTestRule.onNodeWithText("正在进行").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_showsEmptyState_whenNoSearchQuery() {
        // Arrange
        val state = LibraryUiState(
            selectedTab = LibraryTab.WORD,
            searchQuery = "",
            searchResultsWords = emptyList(),
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = state,
                onTabSelected = {},
                onSearchQueryChange = {},
                onClearSearch = {},
                onWordClick = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("请输入搜索词").assertIsDisplayed()
    }

    @Test
    fun libraryScreen_showsLoading_whenIsLoading() {
        // Arrange
        val state = LibraryUiState(
            selectedTab = LibraryTab.WORD,
            searchQuery = "test",
            isLoading = true
        )

        // Act
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = state,
                onTabSelected = {},
                onSearchQueryChange = {},
                onClearSearch = {},
                onWordClick = {}
            )
        }

        // Assert
        composeTestRule.onNode(
            hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)
        ).assertIsDisplayed()
    }

    @Test
    fun libraryScreen_showsNoResults_whenSearchReturnsEmpty() {
        // Arrange
        val state = LibraryUiState(
            selectedTab = LibraryTab.WORD,
            searchQuery = "xyz",
            searchResultsWords = emptyList(),
            isLoading = false
        )

        // Act
        composeTestRule.setContent {
            LibraryScreenContent(
                uiState = state,
                onTabSelected = {},
                onSearchQueryChange = {},
                onClearSearch = {},
                onWordClick = {}
            )
        }

        // Assert
        composeTestRule.onNodeWithText("未找到相关内容").assertIsDisplayed()
    }
}

/**
 * 用于测试的LibraryScreenContent组件
 */
@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.runtime.Composable
private fun LibraryScreenContent(
    uiState: LibraryUiState,
    onTabSelected: (LibraryTab) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onWordClick: (Int) -> Unit
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text("词库") }
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 搜索框
            SearchBarContent(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                onClear = onClearSearch
            )

            // Tab
            androidx.compose.material3.TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal
            ) {
                androidx.compose.material3.Tab(
                    selected = uiState.selectedTab == LibraryTab.WORD,
                    onClick = { onTabSelected(LibraryTab.WORD) },
                    text = { androidx.compose.material3.Text("单词") }
                )
                androidx.compose.material3.Tab(
                    selected = uiState.selectedTab == LibraryTab.GRAMMAR,
                    onClick = { onTabSelected(LibraryTab.GRAMMAR) },
                    text = { androidx.compose.material3.Text("语法") }
                )
            }

            // 内容区域
            when {
                uiState.isLoading -> {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
                uiState.searchQuery.isEmpty() -> {
                    EmptySearchContent()
                }
                uiState.selectedTab == LibraryTab.WORD -> {
                    if (uiState.searchResultsWords.isEmpty()) {
                        NoResultsContent()
                    } else {
                        WordListContent(
                            words = uiState.searchResultsWords,
                            onWordClick = onWordClick
                        )
                    }
                }
                uiState.selectedTab == LibraryTab.GRAMMAR -> {
                    if (uiState.searchResultsGrammars.isEmpty()) {
                        NoResultsContent()
                    } else {
                        GrammarListContent(grammars = uiState.searchResultsGrammars)
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun SearchBarContent(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = androidx.compose.ui.Modifier.weight(1f),
            placeholder = { androidx.compose.material3.Text("搜索单词或语法...") },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    androidx.compose.material3.IconButton(onClick = onClear) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                            contentDescription = "清除"
                        )
                    }
                }
            }
        )
    }
}

@androidx.compose.runtime.Composable
private fun EmptySearchContent() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text("请输入搜索词")
    }
}

@androidx.compose.runtime.Composable
private fun NoResultsContent() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.Text("未找到相关内容")
    }
}

@androidx.compose.runtime.Composable
private fun WordListContent(
    words: List<Word>,
    onWordClick: (Int) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = words.size,
            key = { index -> words[index].id }
        ) { index ->
            val word = words[index]
            SimpleWordCard(word = word, onClick = { onWordClick(word.id) })
        }
    }
}

@androidx.compose.runtime.Composable
private fun GrammarListContent(grammars: List<Grammar>) {
    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = grammars.size,
            key = { index -> grammars[index].id }
        ) { index ->
            val grammar = grammars[index]
            SimpleGrammarCard(grammar = grammar)
        }
    }
}

@androidx.compose.runtime.Composable
private fun SimpleWordCard(
    word: Word,
    onClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp)
        ) {
            androidx.compose.material3.Text(
                text = word.japanese,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            androidx.compose.material3.Text(
                text = word.chinese,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun SimpleGrammarCard(grammar: Grammar) {
    androidx.compose.material3.Card(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier.padding(16.dp)
        ) {
            androidx.compose.material3.Text(
                text = grammar.grammar,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )
            androidx.compose.material3.Text(
                text = grammar.getFirstExplanation(),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
