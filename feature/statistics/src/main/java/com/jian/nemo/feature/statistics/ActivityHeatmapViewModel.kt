package com.jian.nemo.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.usecase.statistics.GetHeatmapDataUseCase
import com.jian.nemo.core.domain.usecase.statistics.HeatmapDay
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.model.SrsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActivityHeatmapUiState(
    val heatmapData: List<HeatmapDay> = emptyList(),
    val panoramaData: MemoryPanoramaData = MemoryPanoramaData(),
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val totalActiveDays: Int = 0,
    val bestDayCount: Int = 0,
    val bestDayDate: Long = 0,
    val dailyAverage: Int = 0,
    val todayCount: Int = 0,
    val isLoading: Boolean = true
)

data class MemoryPanoramaData(
    val totalCount: Int = 0,
    val buckets: List<PanoramaBucket> = emptyList()
)

data class PanoramaBucket(
    val id: String,
    val label: String,
    val count: Int,
    val ratio: Float,
    val color: String
)

@HiltViewModel
class ActivityHeatmapViewModel @Inject constructor(
    private val getHeatmapDataUseCase: GetHeatmapDataUseCase,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityHeatmapUiState())
    val uiState: StateFlow<ActivityHeatmapUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getHeatmapDataUseCase(),
                wordRepository.getAllLearnedWords(),
                grammarRepository.getAllLearnedGrammars()
            ) { heatmap, learnedWords, learnedGrammars ->
                // 1. Calculate Panorama Data
                val allItems = learnedWords.filter { !it.isDelisted } + learnedGrammars.filter { !it.isDelisted }
                val totalCount = allItems.size
                
                val buckets = listOf(
                    createBucket("young_early", "初识", allItems.count { it.stability < 3f }, totalCount, "#EF4444"),
                    createBucket("young_developing", "熟悉", allItems.count { it.stability >= 3f && it.stability < 21f }, totalCount, "#3B82F6"),
                    createBucket("mature", "稳固", allItems.count { it.stability >= 21f && it.stability < 90f }, totalCount, "#22C55E"),
                    createBucket("expert", "长效", allItems.count { it.stability >= 90f }, totalCount, "#8B5CF6")
                )

                val panoramaData = MemoryPanoramaData(totalCount, buckets)

                // 2. Calculate Rich Stats (Existing logic)
                val activeDays = heatmap.filter { it.count > 0 }
                val totalActiveDays = activeDays.size
                val dailyAverage = if (totalActiveDays > 0) activeDays.sumOf { it.count } / totalActiveDays else 0

                val bestDay = activeDays.maxByOrNull { it.count }
                val bestDayCount = bestDay?.count ?: 0
                val bestDayDate = bestDay?.date ?: 0L

                val sortedActiveDates = activeDays.map { it.date }.sorted()
                var currentStreak = 0
                var maxStreak = 0
                var tempStreak = 0
                var lastDate = -1L

                for (date in sortedActiveDates) {
                    if (lastDate == -1L) {
                        tempStreak = 1
                    } else if (date == lastDate + 1) {
                        tempStreak++
                    } else {
                        maxStreak = maxOf(maxStreak, tempStreak)
                        tempStreak = 1
                    }
                    lastDate = date
                }
                maxStreak = maxOf(maxStreak, tempStreak)

                val todayEpoch = DateTimeUtils.timestampToEpochDay(DateTimeUtils.getCurrentCompensatedMillis())
                val todayCount = heatmap.find { it.date == todayEpoch }?.count ?: 0
                val isTodayActive = sortedActiveDates.contains(todayEpoch)
                val isYesterdayActive = sortedActiveDates.contains(todayEpoch - 1)

                if (isTodayActive) {
                    var streak = 0
                    var checkDate = todayEpoch
                    while (sortedActiveDates.contains(checkDate)) {
                        streak++
                        checkDate--
                    }
                    currentStreak = streak
                } else if (isYesterdayActive) {
                    var streak = 0
                    var checkDate = todayEpoch - 1
                    while (sortedActiveDates.contains(checkDate)) {
                        streak++
                        checkDate--
                    }
                    currentStreak = streak
                } else {
                    currentStreak = 0
                }

                Triple(heatmap, panoramaData, object {
                    val streak = currentStreak
                    val longestStreak = maxStreak
                    val totalActiveDays = totalActiveDays
                    val bestDayCount = bestDayCount
                    val bestDayDate = bestDayDate
                    val dailyAverage = dailyAverage
                    val todayCount = todayCount
                })
            }.collect { (heatmap, panorama, stats) ->
                _uiState.update {
                    it.copy(
                        heatmapData = heatmap,
                        panoramaData = panorama,
                        streak = stats.streak,
                        longestStreak = stats.longestStreak,
                        totalActiveDays = stats.totalActiveDays,
                        bestDayCount = stats.bestDayCount,
                        bestDayDate = stats.bestDayDate,
                        dailyAverage = stats.dailyAverage,
                        todayCount = stats.todayCount,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun createBucket(id: String, label: String, count: Int, total: Int, color: String): PanoramaBucket {
        return PanoramaBucket(
            id = id,
            label = label,
            count = count,
            ratio = if (total > 0) count.toFloat() / total else 0f,
            color = color
        )
    }
}
