package com.jian.nemo.feature.library.presentation.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.jian.nemo.core.designsystem.theme.NemoTheme
import com.jian.nemo.core.domain.model.Expression
import com.jian.nemo.core.domain.model.ExpressionCategory
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarUsage
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.GrammarPreloadItem
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.feature.library.presentation.category.CategoryClassificationContent
import com.jian.nemo.feature.library.presentation.category.CategoryWordsContent
import com.jian.nemo.feature.library.presentation.category.CategoryWordsUiState
import com.jian.nemo.feature.library.presentation.expression.ExpressionContent
import com.jian.nemo.feature.library.presentation.expression.ExpressionUiState
import com.jian.nemo.feature.library.presentation.list.GrammarListContent
import com.jian.nemo.feature.library.presentation.list.GrammarListUiState
import com.jian.nemo.feature.library.presentation.list.StudyFilter
import com.jian.nemo.feature.library.presentation.list.WordListContent
import com.jian.nemo.feature.library.presentation.list.WordListUiState

private val PreviewWords = listOf(
    Word(
        id = 1,
        rawId = "w_1",
        japanese = "桜",
        hiragana = "さくら",
        chinese = "樱花",
        level = "N5",
        repetitionCount = 2
    ),
    Word(
        id = 2,
        rawId = "w_2",
        japanese = "図書館",
        hiragana = "としょかん",
        chinese = "图书馆",
        level = "N4",
        repetitionCount = 0
    ),
    Word(
        id = 3,
        rawId = "w_3",
        japanese = "抽象的",
        hiragana = "ちゅうしょうてき",
        chinese = "抽象的",
        level = "N1",
        repetitionCount = 1
    )
)

private val PreviewGrammars = listOf(
    GrammarPreloadItem(
        grammar = Grammar(
            id = 1,
            rawId = "g_1",
            grammar = "〜に違[ちが]いない",
            grammarLevel = "N3",
            usages = listOf(
                GrammarUsage(
                    id = 1,
                    subtype = null,
                    connection = "普通形",
                    explanation = "一定是、肯定",
                    notes = null,
                    examples = emptyList()
                )
            ),
            repetitionCount = 1
        ),
        cleanedTitle = "〜に違いない",
        cleanedFirstExplanation = "一定是、肯定"
    )
)

@Preview(name = "WordList - Light", showBackground = true)
@Preview(name = "WordList - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "WordList - Large Font", showBackground = true, fontScale = 1.5f)
@Composable
private fun WordListContentPreview() {
    NemoTheme {
        WordListContent(
            uiState = WordListUiState(
                isLoading = false,
                wordsByLevel = mapOf(
                    "N5" to listOf(PreviewWords[0]),
                    "N4" to listOf(PreviewWords[1]),
                    "N1" to listOf(PreviewWords[2])
                ),
                allWords = PreviewWords,
                countsByLevel = mapOf("N5" to 1, "N4" to 1, "N1" to 1),
                filterState = StudyFilter.ALL
            ),
            searchQuery = "",
            iconStyle = SettingIconStyle.SOFT_TRANSLUCENT,
            onNavigateBack = {},
            onWordClick = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onLevelSelected = {},
            onRefresh = {},
            onClearSyncMessage = {}
        )
    }
}

@Preview(name = "GrammarList - Light", showBackground = true)
@Preview(name = "GrammarList - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GrammarListContentPreview() {
    NemoTheme {
        GrammarListContent(
            uiState = GrammarListUiState(
                isLoading = false,
                grammarsByLevel = mapOf("N3" to PreviewGrammars),
                allGrammars = PreviewGrammars,
                countsByLevel = mapOf("N3" to 1),
                filterState = StudyFilter.ALL
            ),
            searchQuery = "",
            iconStyle = SettingIconStyle.SOFT_TRANSLUCENT,
            onNavigateBack = {},
            onGrammarClick = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onLevelSelected = {},
            onRefresh = {},
            onClearSyncMessage = {}
        )
    }
}

@Preview(name = "CategoryClassification - Light", showBackground = true)
@Preview(name = "CategoryClassification - Large Font", showBackground = true, fontScale = 1.5f)
@Preview(name = "CategoryClassification - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryClassificationContentPreview() {
    NemoTheme {
        CategoryClassificationContent(
            source = "vocabulary",
            settingIconStyle = SettingIconStyle.SOFT_TRANSLUCENT,
            onNavigateBack = {},
            onCategorySelected = { _, _ -> }
        )
    }
}

@Preview(name = "CategoryWords - Light", showBackground = true)
@Preview(name = "CategoryWords - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CategoryWordsContentPreview() {
    NemoTheme {
        CategoryWordsContent(
            categoryTitle = "名詞",
            uiState = CategoryWordsUiState(
                isLoading = false,
                words = PreviewWords,
                filteredWords = PreviewWords,
                countsByLevel = mapOf("N5" to 1, "N4" to 1, "N1" to 1)
            ),
            searchQuery = "",
            iconStyle = SettingIconStyle.SOFT_TRANSLUCENT,
            onNavigateBack = {},
            onWordClick = {},
            onSearchQueryChange = {},
            onFilterSelected = {},
            onLevelSelected = {}
        )
    }
}

@Preview(name = "Expression - Light", showBackground = true)
@Preview(name = "Expression - Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Expression - Large Font", showBackground = true, fontScale = 1.5f)
@Composable
private fun ExpressionContentPreview() {
    val sampleExpression = Expression(
        id = "col_001",
        level = "N3",
        japanese = "気にする",
        furigana = "きにする",
        chinese = "在意，介意",
        clozeShow = "そんなに{{input}}しないで",
        clozeAnswers = listOf("気に"),
        synonyms = listOf("気にかける", "心配する", "こだわる")
    )
    val sampleCategory = ExpressionCategory(
        levelId = 1,
        categoryKey = "collocation",
        levelName = "固定搭配",
        levelEnglish = "Collocation",
        definition = "高频动词与名词固定组合表达",
        formula = "名词 + 助词 + 动词",
        items = listOf(sampleExpression)
    )
    NemoTheme {
        ExpressionContent(
            uiState = ExpressionUiState(
                categories = listOf(sampleCategory),
                selectedCategory = sampleCategory,
                filteredExpressions = listOf(sampleExpression),
                isLoading = false
            ),
            searchQuery = "",
            onNavigateBack = {},
            onSelectCategory = {},
            onSearchChange = {},
            onLevelSelect = {}
        )
    }
}
