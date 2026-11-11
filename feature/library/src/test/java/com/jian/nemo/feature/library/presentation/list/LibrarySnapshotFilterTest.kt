package com.jian.nemo.feature.library.presentation.list

import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarExample
import com.jian.nemo.core.domain.model.GrammarUsage
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.GrammarListSnapshot
import com.jian.nemo.core.domain.repository.GrammarPreloadItem
import com.jian.nemo.core.domain.repository.WordListSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySnapshotFilterTest {

    @Test
    fun `unready word snapshot keeps loading state`() {
        val state = buildWordListUiState(
            snapshot = WordListSnapshot(isReady = false),
            query = "",
            filter = StudyFilter.ALL,
            isRefreshing = false,
            syncMessage = null
        )

        assertTrue(state.isLoading)
        assertTrue(state.allWords.isEmpty())
    }

    @Test
    fun `ready word snapshot with blank query and ALL filter reuses snapshot collections without copying`() {
        val n1Word = sampleWord(id = 1, japanese = "抽象", hiragana = "ちゅうしょう", chinese = "抽象", level = "N1", repetitionCount = 2)
        val n5Word = sampleWord(id = 2, japanese = "学校", hiragana = "がっこう", chinese = "学校", level = "N5", repetitionCount = 0)
        val snapshot = WordListSnapshot(
            wordsByLevel = linkedMapOf("N1" to listOf(n1Word), "N5" to listOf(n5Word)),
            countsByLevel = linkedMapOf("N1" to 1, "N5" to 1),
            allWords = listOf(n1Word, n5Word),
            isReady = true
        )

        val state = buildWordListUiState(
            snapshot = snapshot,
            query = "",
            filter = StudyFilter.ALL,
            isRefreshing = false,
            syncMessage = null
        )

        assertFalse(state.isLoading)
        assertSame(snapshot.wordsByLevel, state.wordsByLevel)
        assertSame(snapshot.allWords, state.allWords)
        assertSame(snapshot.countsByLevel, state.countsByLevel)
    }

    @Test
    fun `word filtering respects study filter and search query while preserving counts`() {
        val n1Learned = sampleWord(id = 1, japanese = "花見", hiragana = "はなみ", chinese = "赏花", level = "N1", repetitionCount = 1)
        val n5Unlearned = sampleWord(id = 2, japanese = "桜", hiragana = "さくら", chinese = "樱花", level = "N5", repetitionCount = 0)
        val snapshot = WordListSnapshot(
            wordsByLevel = linkedMapOf("N1" to listOf(n1Learned), "N5" to listOf(n5Unlearned)),
            countsByLevel = linkedMapOf("N1" to 1, "N5" to 1),
            allWords = listOf(n1Learned, n5Unlearned),
            isReady = true
        )

        val learnedOnly = buildWordListUiState(snapshot, query = "花", filter = StudyFilter.LEARNED, isRefreshing = false, syncMessage = null)
        assertEquals(listOf(n1Learned), learnedOnly.allWords)
        assertEquals(mapOf("N1" to 1), learnedOnly.countsByLevel)

        val unlearnedOnly = buildWordListUiState(snapshot, query = "さくら", filter = StudyFilter.UNLEARNED, isRefreshing = false, syncMessage = null)
        assertEquals(listOf(n5Unlearned), unlearnedOnly.allWords)
        assertEquals(mapOf("N5" to 1), unlearnedOnly.countsByLevel)
    }

    @Test
    fun `ready grammar snapshot filters by rubi kana, usages and examples`() {
        val item1 = GrammarPreloadItem(
            grammar = Grammar(
                id = 1,
                rawId = "g1",
                grammar = "〜に違[ちが]いない",
                grammarLevel = "N3",
                usages = listOf(
                    GrammarUsage(
                        id = 10,
                        subtype = null,
                        connection = "普通形",
                        explanation = "一定是",
                        notes = null,
                        examples = listOf(
                            GrammarExample(
                                sentence = "彼[かれ]は来[く]るに違いない。",
                                translation = "他一定会来。",
                                source = null,
                                isDialog = false
                            )
                        )
                    )
                ),
                repetitionCount = 1
            ),
            cleanedTitle = "〜に違いない",
            cleanedFirstExplanation = "一定是"
        )
        val snapshot = GrammarListSnapshot(
            grammarsByLevel = linkedMapOf("N3" to listOf(item1)),
            countsByLevel = linkedMapOf("N3" to 1),
            allGrammars = listOf(item1),
            isReady = true
        )

        val directState = buildGrammarListUiState(snapshot, query = "", filter = StudyFilter.ALL, isRefreshing = false, syncMessage = null)
        assertFalse(directState.isLoading)
        assertSame(snapshot.allGrammars, directState.allGrammars)

        val kanaMatch = buildGrammarListUiState(snapshot, query = "ちがいない", filter = StudyFilter.LEARNED, isRefreshing = false, syncMessage = null)
        assertEquals(listOf(item1), kanaMatch.allGrammars)

        val exampleMatch = buildGrammarListUiState(snapshot, query = "一定会来", filter = StudyFilter.ALL, isRefreshing = false, syncMessage = null)
        assertEquals(listOf(item1), exampleMatch.allGrammars)

        val unlearnedMiss = buildGrammarListUiState(snapshot, query = "", filter = StudyFilter.UNLEARNED, isRefreshing = false, syncMessage = null)
        assertTrue(unlearnedMiss.allGrammars.isEmpty())
    }

    private fun sampleWord(
        id: Int,
        japanese: String,
        hiragana: String,
        chinese: String,
        level: String,
        repetitionCount: Int
    ): Word = Word(
        id = id,
        rawId = "raw_$id",
        japanese = japanese,
        hiragana = hiragana,
        chinese = chinese,
        level = level,
        repetitionCount = repetitionCount
    )
}
