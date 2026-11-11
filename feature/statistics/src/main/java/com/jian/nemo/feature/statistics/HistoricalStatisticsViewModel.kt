package com.jian.nemo.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.feature.statistics.model.StatisticDisplayItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val settingIconStyle: SettingIconStyle = SettingIconStyle.VIBRANT_SOLID,
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

/**
 * 历史统计界面 ViewModel
 *
 * 负责获取和展示所有已学习的单词和语法，并推算各等级与全等级通关预测
 */
@HiltViewModel
class HistoricalStatisticsViewModel @Inject constructor(
    private val preloadCache: StatisticsPreloadCache,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        preloadCache.historicalState.value?.toUiState(preloadCache.settingIconStyleState.value)
            ?: HistoricalStatisticsUiState(settingIconStyle = preloadCache.settingIconStyleState.value)
    )
    val uiState: StateFlow<HistoricalStatisticsUiState> = _uiState.asStateFlow()

    init {
        preloadCache.ensureStartedNow()
        observeHistoricalStatistics()
        observeSettingIconStyle()
    }

    private fun observeSettingIconStyle() {
        viewModelScope.launch {
            settingsRepository.settingIconStyleFlow.collect { style ->
                _uiState.update { it.copy(settingIconStyle = style) }
            }
        }
    }

    private fun observeHistoricalStatistics() {
        viewModelScope.launch {
            preloadCache.historicalState
                .filterNotNull()
                .collect { snapshot ->
                    _uiState.update { current ->
                        snapshot.toUiState(current.settingIconStyle)
                    }
                }
        }
    }

    private fun HistoricalStatisticsSnapshot.toUiState(
        settingIconStyle: SettingIconStyle
    ): HistoricalStatisticsUiState {
        return HistoricalStatisticsUiState(
            settingIconStyle = settingIconStyle,
            learnedWords = learnedWords,
            learnedGrammars = learnedGrammars,
            wordTotalCountMap = wordTotalCountMap,
            grammarTotalCountMap = grammarTotalCountMap,
            wordDailyGoal = wordDailyGoal,
            grammarDailyGoal = grammarDailyGoal,
            wordPredictions = wordPredictions,
            grammarPredictions = grammarPredictions,
            wordOverallPrediction = wordOverallPrediction,
            grammarOverallPrediction = grammarOverallPrediction,
            isLoading = false
        )
    }
}

