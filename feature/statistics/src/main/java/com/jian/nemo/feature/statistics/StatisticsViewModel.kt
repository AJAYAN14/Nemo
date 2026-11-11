package com.jian.nemo.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.LearningStats
import com.jian.nemo.core.domain.model.StudyRecord
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.feature.statistics.model.StatisticDisplayItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 统计界面 ViewModel
 *
 * 管理学习统计数据的状态
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val preloadCache: StatisticsPreloadCache,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val initialToday = preloadCache.todayListsState.value
    private val initialStats = preloadCache.learningStatsState.value

    private val _uiState = MutableStateFlow(
        if (initialToday != null) {
            StatisticsUiState(
                settingIconStyle = preloadCache.settingIconStyleState.value,
                isLoading = initialStats == null,
                isListsLoading = false,
                stats = initialStats,
                recentRecords = preloadCache.recent30DaysState.value,
                weekRecords = preloadCache.recent7DaysState.value,
                selectedDate = initialToday.date,
                reviewForecast = preloadCache.reviewForecastState.value,
                selectedDateReviewCount = initialStats?.let { it.dueWords + it.dueGrammars } ?: 0,
                todaysWords = initialToday.words,
                todaysGrammars = initialToday.grammars
            )
        } else {
            StatisticsUiState(
                settingIconStyle = preloadCache.settingIconStyleState.value,
                isLoading = initialStats == null,
                isListsLoading = true,
                stats = initialStats,
                recentRecords = preloadCache.recent30DaysState.value,
                weekRecords = preloadCache.recent7DaysState.value,
                selectedDate = -1L,
                reviewForecast = preloadCache.reviewForecastState.value
            )
        }
    )
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        preloadCache.ensureStartedNow()
        observePreloadedSummary()
        observeSelectedDate()
        syncTodayDate()
        observeSettingIconStyle()
    }

    private fun observeSettingIconStyle() {
        viewModelScope.launch {
            settingsRepository.settingIconStyleFlow.collect { style ->
                _uiState.update { it.copy(settingIconStyle = style) }
            }
        }
    }

    /**
     * 同步今天的初始日期（基于重置时间）
     */
    private fun syncTodayDate() {
        viewModelScope.launch {
            val resetHour = settingsRepository.learningDayResetHourFlow.first()
            val today = DateTimeUtils.getLearningDay(resetHour)
            preloadCache.refreshTodayIfDateChanged(today)
            if (_uiState.value.selectedDate != today) {
                _uiState.update {
                    it.copy(
                        selectedDate = today,
                        isListsLoading = preloadCache.todayListsState.value?.date != today
                    )
                }
            }
        }
    }

    /**
     * 同步预加载与实时统计概览数据
     */
    private fun observePreloadedSummary() {
        viewModelScope.launch {
            preloadCache.learningStatsState
                .filterNotNull()
                .collect { stats ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            stats = stats,
                            error = null
                        )
                    }
                    updateSelectedDateReviewCount(_uiState.value.selectedDate, _uiState.value.reviewForecast)
                }
        }

        viewModelScope.launch {
            preloadCache.recent30DaysState.collect { records ->
                _uiState.update { it.copy(recentRecords = records) }
            }
        }

        viewModelScope.launch {
            preloadCache.recent7DaysState.collect { records ->
                _uiState.update { it.copy(weekRecords = records) }
            }
        }

        viewModelScope.launch {
            preloadCache.reviewForecastState.collect { forecast ->
                _uiState.update { it.copy(reviewForecast = forecast) }
                updateSelectedDateReviewCount(_uiState.value.selectedDate, forecast)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSelectedDate() {
        viewModelScope.launch {
            _uiState.map { it.selectedDate }
                .filter { it >= 0L }
                .distinctUntilChanged()
                .flatMapLatest { date ->
                    val cachedToday = preloadCache.todayListsState.value
                    if (cachedToday != null && cachedToday.date == date) {
                        preloadCache.todayListsState
                            .filterNotNull()
                            .filter { it.date == date }
                            .map { Triple(date, it.words, it.grammars) }
                    } else {
                        preloadCache.observeListsForDate(date)
                            .map { (words, grammars) -> Triple(date, words, grammars) }
                    }
                }
                .collect { (date, wordItems, grammarItems) ->
                    updateSelectedDateReviewCount(date, _uiState.value.reviewForecast)

                    _uiState.update {
                        it.copy(
                            todaysWords = wordItems,
                            todaysGrammars = grammarItems,
                            isListsLoading = false
                        )
                    }
                }
        }
    }

    private fun updateSelectedDateReviewCount(date: Long, forecast: List<com.jian.nemo.core.domain.model.ReviewForecast>) {
        if (date < 0L) return
        viewModelScope.launch {
            val resetHour = settingsRepository.learningDayResetHourFlow.first()
            val today = DateTimeUtils.getLearningDay(resetHour)
            val reviewCount = if (date == today) {
                val stats = _uiState.value.stats
                (stats?.dueWords ?: 0) + (stats?.dueGrammars ?: 0)
            } else if (date > today) {
                forecast.find { it.date == date }?.totalCount ?: 0
            } else {
                0
            }

            _uiState.update { it.copy(selectedDateReviewCount = reviewCount) }
        }
    }

    /**
     * 选择日期
     */
    fun selectDate(date: Long) {
        if (date != _uiState.value.selectedDate) {
            val hasCachedToday = preloadCache.todayListsState.value?.date == date
            _uiState.update {
                it.copy(
                    selectedDate = date,
                    isListsLoading = !hasCachedToday
                )
            }
        }
    }

    /**
     * 刷新统计数据
     */
    fun refresh() {
        preloadCache.ensureStartedNow()
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 统计界面 UI 状态
 */
data class StatisticsUiState(
    /**
     * 图标设计风格
     */
    val settingIconStyle: SettingIconStyle = SettingIconStyle.VIBRANT_SOLID,

    /**
     * 是否正在加载
     */
    val isLoading: Boolean = false,

    /**
     * 当日单词/语法明细列表是否尚处于首次查询中
     */
    val isListsLoading: Boolean = false,

    /**
     * 学习统计数据
     */
    val stats: LearningStats? = null,

    /**
     * 最近30天的学习记录（用于热力图）
     */
    val recentRecords: List<StudyRecord> = emptyList(),

    /**
     * 最近7天的学习记录（用于趋势图）
     */
    val weekRecords: List<StudyRecord> = emptyList(),

    /**
     * 选中日期 (Epoch Day)
     */
    val selectedDate: Long = 0L,

    /**
     * 复习预测数据
     */
    val reviewForecast: List<com.jian.nemo.core.domain.model.ReviewForecast> = emptyList(),

    /**
     * 选中日期的待复习数量
     */
    val selectedDateReviewCount: Int = 0,

    /**
     * 今日学习的单词列表 (实际上是 selectedDate 的单词列表)
     */
    val todaysWords: List<StatisticDisplayItem> = emptyList(),

    /**
     * 今日学习的语法列表 (实际上是 selectedDate 的语法列表)
     */
    val todaysGrammars: List<StatisticDisplayItem> = emptyList(),

    /**
     * 错误消息
     */
    val error: String? = null
)
