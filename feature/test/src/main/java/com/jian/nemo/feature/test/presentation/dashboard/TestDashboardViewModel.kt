package com.jian.nemo.feature.test.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.usecase.test.LoadTestStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import javax.inject.Inject

data class TestDashboardUiState(
    val todayTestCount: Int = 0,
    val todayAccuracy: Float = 0f,
    val consecutiveTestDays: Int = 0,
    val totalTestCount: Int = 0,
    val overallAccuracy: Float = 0f,
    val maxTestStreak: Int = 0,
    val wrongWordsCount: Int = 0,
    val favoriteWordsCount: Int = 0,
    val settingIconStyle: SettingIconStyle = SettingIconStyle.PLUMP_DUO
)

@HiltViewModel
class TestDashboardViewModel @Inject constructor(
    private val loadTestStatisticsUseCase: LoadTestStatisticsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestDashboardUiState())
    val uiState: StateFlow<TestDashboardUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingIconStyleFlow.collect { style ->
                _uiState.update { it.copy(settingIconStyle = style) }
            }
        }
    }

    private fun loadStatistics() {
        // 基于重置时间的响应式统计 (今日次数和正确率)
        viewModelScope.launch {
            settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
                val today = DateTimeUtils.getLearningDay(resetHour)
                loadTestStatisticsUseCase.getTodayTestCountFlow(today)
            }.collect { count ->
                _uiState.update { it.copy(todayTestCount = count) }
            }
        }

        viewModelScope.launch {
            settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
                val today = DateTimeUtils.getLearningDay(resetHour)
                loadTestStatisticsUseCase.getTodayAccuracyFlow(today)
            }.collect { accuracy ->
                _uiState.update { it.copy(todayAccuracy = accuracy) }
            }
        }

        // 连续测试天数
        viewModelScope.launch {
            loadTestStatisticsUseCase.getConsecutiveTestDaysFlow().collect { count ->
                _uiState.update { it.copy(consecutiveTestDays = count) }
            }
        }

        // 累积测试次数
        viewModelScope.launch {
            loadTestStatisticsUseCase.getTotalTestCountFlow().collect { count ->
                _uiState.update { it.copy(totalTestCount = count) }
            }
        }

        // 总体正确率
        viewModelScope.launch {
            loadTestStatisticsUseCase.getOverallAccuracyFlow().collect { accuracy ->
                _uiState.update { it.copy(overallAccuracy = accuracy) }
            }
        }

        // 最大连续天数
        viewModelScope.launch {
            loadTestStatisticsUseCase.getMaxTestStreakFlow().collect { count ->
                _uiState.update { it.copy(maxTestStreak = count) }
            }
        }

        // 错题数量
        viewModelScope.launch {
            loadTestStatisticsUseCase.getWrongWordsCountFlow().collect { count ->
                _uiState.update { it.copy(wrongWordsCount = count) }
            }
        }

        // 收藏数量
        viewModelScope.launch {
            loadTestStatisticsUseCase.getFavoriteWordsCountFlow().collect { count ->
                _uiState.update { it.copy(favoriteWordsCount = count) }
            }
        }
    }
}
