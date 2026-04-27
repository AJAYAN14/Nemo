package com.jian.nemo.feature.statistics.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.LearningStats
import com.jian.nemo.core.domain.model.StudyRecord
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.usecase.statistics.GetLearningStatsUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetReviewForecastUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetHeatmapDataUseCase
import com.jian.nemo.core.domain.usecase.statistics.HeatmapDay
import com.jian.nemo.core.domain.model.ReviewForecast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class LearningCalendarViewModel @Inject constructor(
    private val getLearningStatsUseCase: GetLearningStatsUseCase,
    private val getReviewForecastUseCase: GetReviewForecastUseCase,
    private val getHeatmapDataUseCase: GetHeatmapDataUseCase,
    private val studyRecordRepository: StudyRecordRepository,
    private val settingsRepository: com.jian.nemo.core.domain.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearningCalendarUiState())
    val uiState: StateFlow<LearningCalendarUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(getDateWithZeroTime(Date()))

    init {
        loadInitialData()
        observeSelectedDate()
        syncSelectedDate()
    }

    private fun syncSelectedDate() {
        viewModelScope.launch {
            val resetHour = settingsRepository.learningDayResetHourFlow.first()
            val todayEpoch = DateTimeUtils.getLearningDay(resetHour)
            val todayDate = Date(todayEpoch * 86400000L)
            _selectedDate.value = getDateWithZeroTime(todayDate)
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load Today Stats
            getLearningStatsUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { stats ->
                    _uiState.update { it.copy(todayStats = stats) }
                }
        }

        viewModelScope.launch {
            // Load Week Forecast
            getReviewForecastUseCase()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { forecastList ->
                    val forecastMap = forecastList.associateBy { it.date }
                    _uiState.update { it.copy(weekForecast = forecastMap) }
                }
        }

        viewModelScope.launch {
             // Load Heatmap Data
             getHeatmapDataUseCase()
                .collect { data ->
                    _uiState.update { it.copy(heatmapData = data) }
                }
        }

        viewModelScope.launch {
            settingsRepository.learningDayResetHourFlow.collect { resetHour ->
                val today = DateTimeUtils.getLearningDay(resetHour)
                _uiState.update { it.copy(todayEpochDay = today) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSelectedDate() {
        viewModelScope.launch {
            _selectedDate
                .flatMapLatest { date ->
                    val epochDay = DateTimeUtils.timestampToEpochDay(date.time)

                    val resetHour = settingsRepository.learningDayResetHourFlow.first()
                    val todayEpochDay = DateTimeUtils.getLearningDay(resetHour)

                    if (epochDay < todayEpochDay) {
                        // Past date: load from repository
                        studyRecordRepository.getRecordByDate(epochDay)
                    } else {
                        // Today or Future: return null flow (we rely on todayStats or forecast)
                        flowOf(null)
                    }
                }
                .collect { record ->
                    _uiState.update { it.copy(selectedDateRecord = record) }
                }
        }

        viewModelScope.launch {
            _selectedDate.collect { date ->
                _uiState.update { it.copy(selectedDate = date) }
            }
        }
    }

    fun onDateSelected(date: Date) {
        _selectedDate.value = getDateWithZeroTime(date)
    }

    private fun getDateWithZeroTime(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
}

data class LearningCalendarUiState(
    val isLoading: Boolean = false,
    val selectedDate: Date = Date(),
    val todayStats: LearningStats? = null,
    val weekForecast: Map<Long, ReviewForecast> = emptyMap(), // EpochDay -> ReviewForecast
    val heatmapData: List<HeatmapDay> = emptyList(), // Added
    val selectedDateRecord: StudyRecord? = null,
    val todayEpochDay: Long = DateTimeUtils.getCurrentEpochDay(),
    val error: String? = null
)
