package com.jian.nemo.feature.library.presentation.expression

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.jian.nemo.core.domain.model.Expression
import com.jian.nemo.core.domain.model.ExpressionCategory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

@OptIn(ExperimentalCoroutinesApi::class)
class ExpressionStateTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `buildExpressionUiState filters by selected category, level, and query`() {
        val exp1 = Expression(
            id = "e1",
            level = "N3",
            japanese = "気にする",
            furigana = "きにする",
            chinese = "在意",
            clozeShow = "そんなに{{input}}しないで",
            clozeAnswers = listOf("気に")
        )
        val exp2 = Expression(
            id = "e2",
            level = "N2",
            japanese = "役に立つ",
            furigana = "やくにたつ",
            chinese = "有用",
            clozeShow = "仕事に{{input}}",
            clozeAnswers = listOf("役に立つ")
        )
        val cat1 = ExpressionCategory(
            levelId = 1,
            categoryKey = "collocation",
            levelName = "固定搭配",
            levelEnglish = "Collocation",
            definition = "desc",
            formula = "formula",
            items = listOf(exp1, exp2)
        )

        val allState = buildExpressionUiState(
            categories = listOf(cat1),
            isLoading = false,
            error = null,
            selectedCategoryKey = "collocation",
            searchQuery = "",
            selectedLevel = ExpressionUiState.LEVEL_ALL
        )
        assertEquals(cat1, allState.selectedCategory)
        assertEquals(listOf(exp1, exp2), allState.filteredExpressions)

        val n2Only = buildExpressionUiState(
            categories = listOf(cat1),
            isLoading = false,
            error = null,
            selectedCategoryKey = "collocation",
            searchQuery = "",
            selectedLevel = "N2"
        )
        assertEquals(listOf(exp2), n2Only.filteredExpressions)

        val queryMatch = buildExpressionUiState(
            categories = listOf(cat1),
            isLoading = false,
            error = null,
            selectedCategoryKey = "collocation",
            searchQuery = "在意",
            selectedLevel = ExpressionUiState.LEVEL_ALL
        )
        assertEquals(listOf(exp1), queryMatch.filteredExpressions)

        val dashboardState = buildExpressionUiState(
            categories = listOf(cat1),
            isLoading = false,
            error = null,
            selectedCategoryKey = null,
            searchQuery = "",
            selectedLevel = ExpressionUiState.LEVEL_ALL
        )
        assertNull(dashboardState.selectedCategory)
        assertEquals(2, dashboardState.filteredExpressions.size)
    }

    @Test
    fun `selectCategory resets stale searchQuery and selectedLevel across categories`() = runTest(testDispatcher) {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        try {
            val collocationJson = """
                {
                  "levelId": 1,
                  "categoryKey": "collocation",
                  "levelName": "固定搭配",
                  "levelEnglish": "Collocation",
                  "definition": "d1",
                  "formula": "f1",
                  "items": [
                    {
                      "id": "e1",
                      "level": "N3",
                      "japanese": "気にする",
                      "furigana": "きにする",
                      "chinese": "在意",
                      "clozeShow": "そんなに{{input}}しないで",
                      "clozeAnswers": ["気に"]
                    }
                  ]
                }
            """.trimIndent()
            val idiomJson = """
                {
                  "levelId": 2,
                  "categoryKey": "idiom",
                  "levelName": "惯用句",
                  "levelEnglish": "Idiom",
                  "definition": "d2",
                  "formula": "f2",
                  "items": [
                    {
                      "id": "e2",
                      "level": "N2",
                      "japanese": "耳が痛い",
                      "furigana": "みみがいたい",
                      "chinese": "刺耳",
                      "clozeShow": "{{input}}",
                      "clozeAnswers": ["耳が痛い"]
                    }
                  ]
                }
            """.trimIndent()

            val assetManager = mockk<AssetManager>()
            every { assetManager.open("expressions/collocation.json") } answers { ByteArrayInputStream(collocationJson.toByteArray()) }
            every { assetManager.open("expressions/idiom.json") } answers { ByteArrayInputStream(idiomJson.toByteArray()) }
            every { assetManager.open(match { it != "expressions/collocation.json" && it != "expressions/idiom.json" }) } throws java.io.FileNotFoundException()
            val context = mockk<Context>(relaxed = true)
            every { context.assets } returns assetManager

            val savedStateHandle = SavedStateHandle()
            val viewModel = ExpressionViewModel(
                context = context,
                savedStateHandle = savedStateHandle,
                ioDispatcher = testDispatcher,
                filterDispatcher = testDispatcher
            )

            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiState.collect {}
            }
            advanceUntilIdle()

            val cat1 = viewModel.uiState.value.categories.first { it.categoryKey == "collocation" }
            val cat2 = viewModel.uiState.value.categories.first { it.categoryKey == "idiom" }

            viewModel.selectCategory(cat1)
            viewModel.changeSearchQuery("在意")
            viewModel.changeSelectedLevel("N3")
            advanceTimeBy(200)
            advanceUntilIdle()
            assertEquals("在意", viewModel.searchQuery.value)
            assertEquals("N3", viewModel.uiState.value.selectedLevel)

            viewModel.selectCategory(null)
            advanceUntilIdle()
            assertEquals("", viewModel.searchQuery.value)
            assertEquals(ExpressionUiState.LEVEL_ALL, viewModel.uiState.value.selectedLevel)

            viewModel.selectCategory(cat2)
            advanceUntilIdle()
            assertEquals("idiom", viewModel.uiState.value.selectedCategory?.categoryKey)
            assertEquals(1, viewModel.uiState.value.filteredExpressions.size)

            collectJob.cancel()
        } finally {
            unmockkStatic(Log::class)
        }
    }

    @Test
    fun `buildExpressionUiState returns empty results when loading or error`() {
        val state = buildExpressionUiState(
            categories = emptyList(),
            isLoading = false,
            error = "io error",
            selectedCategoryKey = "collocation",
            searchQuery = "",
            selectedLevel = ExpressionUiState.LEVEL_ALL
        )
        assertEquals("io error", state.error)
        assertTrue(state.filteredExpressions.isEmpty())
    }
}
