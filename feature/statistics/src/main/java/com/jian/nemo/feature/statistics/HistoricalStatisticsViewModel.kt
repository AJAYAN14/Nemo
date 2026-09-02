package com.jian.nemo.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.ceil
import com.jian.nemo.feature.statistics.model.StatisticDisplayItem

/**
 * 各等级预测与达成信息
 */
data class LevelPredictionInfo(
    val level: String,
    val isCompleted: Boolean,
    val completionDateText: String?, // 已达成时的显示文本，如 "2026年5月12日 达成" 或 "已达成"
    val predictedDateText: String?,  // 未达成时的预测日期，如 "2026年9月11日"
    val remainingDays: Int,          // 距离今天还需累计的天数 (已达成则为 0)
    val remainingCount: Int          // 剩余未学数量
)

/**
 * 通关预测概览
 */
data class OverallPredictionSummary(
    val totalRemainingDays: Int,
    val estimatedCompletionDateText: String?, // 如 "2027年2月14日"
    val isAllCompleted: Boolean,
    val dailyGoal: Int
)

/**
 * 历史统计界面 UI 状态
 */
data class HistoricalStatisticsUiState(
    val learnedWords: List<StatisticDisplayItem> = emptyList(),
    val learnedGrammars: List<StatisticDisplayItem> = emptyList(),
    val wordTotalCountMap: Map<String, Int> = emptyMap(),
    val grammarTotalCountMap: Map<String, Int> = emptyMap(),
    val wordDailyGoal: Int = 50,
    val grammarDailyGoal: Int = 10,
    val wordPredictions: Map<String, LevelPredictionInfo> = emptyMap(),
    val grammarPredictions: Map<String, LevelPredictionInfo> = emptyMap(),
    val wordOverallPrediction: OverallPredictionSummary? = null,
    val grammarOverallPrediction: OverallPredictionSummary? = null,
    val isLoading: Boolean = true
)

private data class CombinedStatistics(
    val wordItems: List<StatisticDisplayItem>,
    val grammarItems: List<StatisticDisplayItem>,
    val wordTotalMap: Map<String, Int>,
    val grammarTotalMap: Map<String, Int>,
    val wordDailyGoal: Int,
    val grammarDailyGoal: Int,
    val wordPredictions: Map<String, LevelPredictionInfo>,
    val grammarPredictions: Map<String, LevelPredictionInfo>,
    val wordOverallPrediction: OverallPredictionSummary,
    val grammarOverallPrediction: OverallPredictionSummary
)

/**
 * 历史统计界面 ViewModel
 *
 * 负责获取和展示所有已学习的单词和语法，并推算各等级与全等级通关预测
 */
@HiltViewModel
class HistoricalStatisticsViewModel @Inject constructor(
    private val getAllLearnedWordsUseCase: GetAllLearnedWordsUseCase,
    private val getAllLearnedGrammarsUseCase: GetAllLearnedGrammarsUseCase,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository
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
                grammarRepository.getAllGrammars(),
                settingsRepository.dailyGoalFlow,
                settingsRepository.grammarDailyGoalFlow,
                settingsRepository.wordLevelCompletionDatesFlow,
                settingsRepository.grammarLevelCompletionDatesFlow
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
                val wordDailyGoal = flows[8] as Int
                val grammarDailyGoal = flows[9] as Int
                @Suppress("UNCHECKED_CAST")
                val wordCompletionDates = flows[10] as Map<String, Long>
                @Suppress("UNCHECKED_CAST")
                val grammarCompletionDates = flows[11] as Map<String, Long>

                val wordTotalMap = mapOf(
                    "N1" to n1.size,
                    "N2" to n2.size,
                    "N3" to n3.size,
                    "N4" to n4.size,
                    "N5" to n5.size
                )

                val grammarTotalMap = allGrammars.groupBy { it.grammarLevel.uppercase() }
                    .mapValues { it.value.size }

                val wordLearnedMap = words.groupBy { it.level.uppercase() }
                    .mapValues { it.value.size }

                val grammarLearnedMap = grammars.groupBy { it.grammarLevel.uppercase() }
                    .mapValues { it.value.size }

                val standardLevels = listOf("N5", "N4", "N3", "N2", "N1")

                // 提取单词降级达成日期 (针对上线前已完成用户的最后学习日期)
                val wordFallbackDates = standardLevels.associateWith { level ->
                    words.filter { it.level.equals(level, ignoreCase = true) }
                        .mapNotNull { it.lastReviewedDate ?: it.firstLearnedDate }
                        .maxOrNull() ?: 0L
                }

                // 提取语法降级达成日期
                val grammarFallbackDates = standardLevels.associateWith { level ->
                    grammars.filter { it.grammarLevel.equals(level, ignoreCase = true) }
                        .mapNotNull { it.lastReviewedDate ?: it.firstLearnedDate }
                        .maxOrNull() ?: 0L
                }

                // 计算单词预测
                val (wordPredictions, wordOverall) = calculatePredictions(
                    levels = standardLevels,
                    learnedCountMap = wordLearnedMap,
                    totalCountMap = wordTotalMap,
                    recordedCompletionDates = wordCompletionDates,
                    fallbackCompletionDates = wordFallbackDates,
                    dailyGoal = wordDailyGoal
                ) { level, epochDay ->
                    viewModelScope.launch {
                        settingsRepository.setWordLevelCompletionDate(level, epochDay)
                    }
                }

                // 计算语法预测
                val (grammarPredictions, grammarOverall) = calculatePredictions(
                    levels = standardLevels,
                    learnedCountMap = grammarLearnedMap,
                    totalCountMap = grammarTotalMap,
                    recordedCompletionDates = grammarCompletionDates,
                    fallbackCompletionDates = grammarFallbackDates,
                    dailyGoal = grammarDailyGoal
                ) { level, epochDay ->
                    viewModelScope.launch {
                        settingsRepository.setGrammarLevelCompletionDate(level, epochDay)
                    }
                }

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

                CombinedStatistics(
                    wordItems = wordItems,
                    grammarItems = grammarItems,
                    wordTotalMap = wordTotalMap,
                    grammarTotalMap = grammarTotalMap,
                    wordDailyGoal = wordDailyGoal,
                    grammarDailyGoal = grammarDailyGoal,
                    wordPredictions = wordPredictions,
                    grammarPredictions = grammarPredictions,
                    wordOverallPrediction = wordOverall,
                    grammarOverallPrediction = grammarOverall
                )
            }.collect { stats ->
                _uiState.update {
                    it.copy(
                        learnedWords = stats.wordItems,
                        learnedGrammars = stats.grammarItems,
                        wordTotalCountMap = stats.wordTotalMap,
                        grammarTotalCountMap = stats.grammarTotalMap,
                        wordDailyGoal = stats.wordDailyGoal,
                        grammarDailyGoal = stats.grammarDailyGoal,
                        wordPredictions = stats.wordPredictions,
                        grammarPredictions = stats.grammarPredictions,
                        wordOverallPrediction = stats.wordOverallPrediction,
                        grammarOverallPrediction = stats.grammarOverallPrediction,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun calculatePredictions(
        levels: List<String>,
        learnedCountMap: Map<String, Int>,
        totalCountMap: Map<String, Int>,
        recordedCompletionDates: Map<String, Long>,
        fallbackCompletionDates: Map<String, Long>,
        dailyGoal: Int,
        onLevelCompletedFirstTime: (String, Long) -> Unit
    ): Pair<Map<String, LevelPredictionInfo>, OverallPredictionSummary> {
        val effectiveDailyGoal = if (dailyGoal > 0) dailyGoal else 1
        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        val formatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

        val predictionMap = mutableMapOf<String, LevelPredictionInfo>()
        var cumulativeDays = 0
        var hasAnyUncompleted = false

        for (level in levels) {
            val learned = learnedCountMap[level] ?: 0
            val total = totalCountMap[level] ?: 0
            val isCompleted = total > 0 && learned >= total
            val remaining = (total - learned).coerceAtLeast(0)

            if (isCompleted) {
                var epochDay = recordedCompletionDates[level]
                if (epochDay == null) {
                    epochDay = fallbackCompletionDates[level]
                    if (epochDay != null && epochDay > 0) {
                        onLevelCompletedFirstTime(level, epochDay)
                    } else {
                        epochDay = todayEpochDay
                        onLevelCompletedFirstTime(level, epochDay)
                    }
                }

                val dateText = if (epochDay > 0) {
                    try {
                        "${LocalDate.ofEpochDay(epochDay).format(formatter)} 达成"
                    } catch (_: Exception) {
                        "已达成"
                    }
                } else {
                    "已达成"
                }

                predictionMap[level] = LevelPredictionInfo(
                    level = level,
                    isCompleted = true,
                    completionDateText = dateText,
                    predictedDateText = null,
                    remainingDays = 0,
                    remainingCount = 0
                )
            } else {
                hasAnyUncompleted = true
                val daysNeeded = ceil(remaining.toDouble() / effectiveDailyGoal).toInt().coerceAtLeast(1)
                cumulativeDays += daysNeeded

                val predictedDate = today.plusDays(cumulativeDays.toLong())
                val predictedText = predictedDate.format(formatter)

                predictionMap[level] = LevelPredictionInfo(
                    level = level,
                    isCompleted = false,
                    completionDateText = null,
                    predictedDateText = predictedText,
                    remainingDays = cumulativeDays,
                    remainingCount = remaining
                )
            }
        }

        val overallSummary = OverallPredictionSummary(
            totalRemainingDays = cumulativeDays,
            estimatedCompletionDateText = if (hasAnyUncompleted) today.plusDays(cumulativeDays.toLong()).format(formatter) else null,
            isAllCompleted = !hasAnyUncompleted,
            dailyGoal = effectiveDailyGoal
        )

        return predictionMap to overallSummary
    }
}

