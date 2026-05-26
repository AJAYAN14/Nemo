package com.jian.nemo.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.usecase.statistics.GetAllLearnedGrammarsUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetAllLearnedWordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.jian.nemo.feature.statistics.model.StatisticDisplayItem

/**
 * 历史统计界面 UI 状态
 */
data class HistoricalStatisticsUiState(
    val learnedWords: List<StatisticDisplayItem> = emptyList(),
    val learnedGrammars: List<StatisticDisplayItem> = emptyList(),
    val wordTotalCountMap: Map<String, Int> = emptyMap(),
    val grammarTotalCountMap: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = true
)

private data class CombinedStatistics(
    val wordItems: List<StatisticDisplayItem>,
    val grammarItems: List<StatisticDisplayItem>,
    val wordTotalMap: Map<String, Int>,
    val grammarTotalMap: Map<String, Int>
)

/**
 * 历史统计界面 ViewModel
 *
 * 负责获取和展示所有已学习的单词和语法
 */
@HiltViewModel
class HistoricalStatisticsViewModel @Inject constructor(
    private val getAllLearnedWordsUseCase: GetAllLearnedWordsUseCase,
    private val getAllLearnedGrammarsUseCase: GetAllLearnedGrammarsUseCase,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoricalStatisticsUiState())
    val uiState: StateFlow<HistoricalStatisticsUiState> = _uiState.asStateFlow()

    init {
        loadHistoricalStatistics()
    }

    private fun loadHistoricalStatistics() {
        viewModelScope.launch {
            combine(
                getAllLearnedWordsUseCase(),
                getAllLearnedGrammarsUseCase(),
                wordRepository.getAllWordsByLevel("N1"),
                wordRepository.getAllWordsByLevel("N2"),
                wordRepository.getAllWordsByLevel("N3"),
                wordRepository.getAllWordsByLevel("N4"),
                wordRepository.getAllWordsByLevel("N5"),
                grammarRepository.getAllGrammars()
            ) { flows ->
                @Suppress("UNCHECKED_CAST")
                val words = flows[0] as List<com.jian.nemo.core.domain.model.Word>
                @Suppress("UNCHECKED_CAST")
                val grammars = flows[1] as List<com.jian.nemo.core.domain.model.Grammar>
                @Suppress("UNCHECKED_CAST")
                val n1 = flows[2] as List<com.jian.nemo.core.domain.model.Word>
                @Suppress("UNCHECKED_CAST")
                val n2 = flows[3] as List<com.jian.nemo.core.domain.model.Word>
                @Suppress("UNCHECKED_CAST")
                val n3 = flows[4] as List<com.jian.nemo.core.domain.model.Word>
                @Suppress("UNCHECKED_CAST")
                val n4 = flows[5] as List<com.jian.nemo.core.domain.model.Word>
                @Suppress("UNCHECKED_CAST")
                val n5 = flows[6] as List<com.jian.nemo.core.domain.model.Word>
                @Suppress("UNCHECKED_CAST")
                val allGrammars = flows[7] as List<com.jian.nemo.core.domain.model.Grammar>

                val wordTotalMap = mapOf(
                    "N1" to n1.size,
                    "N2" to n2.size,
                    "N3" to n3.size,
                    "N4" to n4.size,
                    "N5" to n5.size
                )

                val grammarTotalMap = allGrammars.groupBy { it.grammarLevel.uppercase() }
                    .mapValues { it.value.size }

                val wordItems = words.map { word ->
                    StatisticDisplayItem(
                        id = word.id,
                        japanese = word.japanese,
                        hiragana = word.hiragana,
                        chinese = word.chinese,
                        level = word.level.uppercase()
                    )
                }

                val grammarItems = grammars.map { grammar ->
                    StatisticDisplayItem(
                        id = grammar.id,
                        japanese = grammar.grammar,
                        hiragana = grammar.getFirstConjunction() ?: "",
                        chinese = grammar.getFirstExplanation(),
                        level = grammar.grammarLevel.uppercase()
                    )
                }

                CombinedStatistics(wordItems, grammarItems, wordTotalMap, grammarTotalMap)
            }.collect { stats ->
                _uiState.update {
                    it.copy(
                        learnedWords = stats.wordItems,
                        learnedGrammars = stats.grammarItems,
                        wordTotalCountMap = stats.wordTotalMap,
                        grammarTotalCountMap = stats.grammarTotalMap,
                        isLoading = false
                    )
                }
            }
        }
    }
}
