package com.jian.nemo.feature.statistics.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.LearningStats
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.usecase.statistics.GetLearningStatsUseCase
import com.jian.nemo.feature.statistics.StatisticsPreloadCache
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProgressDashboardUiState(
    val stats: LearningStats? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val settingIconStyle: SettingIconStyle = SettingIconStyle.VIBRANT_SOLID
) {
    val totalProgress: Float
        get() {
            if (stats == null) return 0f
            val totalItems = stats.totalWords + stats.totalGrammars
            return if (totalItems > 0) stats.totalMastered.toFloat() / totalItems else 0f
        }
}

@HiltViewModel
class ProgressDashboardViewModel @Inject constructor(
    private val getLearningStatsUseCase: GetLearningStatsUseCase,
    private val settingsRepository: SettingsRepository,
    private val statisticsPreloadCache: StatisticsPreloadCache
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressDashboardUiState())
    val uiState: StateFlow<ProgressDashboardUiState> = _uiState.asStateFlow()

    init {
        loadStats()
        observeSettings()
        statisticsPreloadCache.warmUpAfterDelay(380L)
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getLearningStatsUseCase().collect { stats ->
                _uiState.update {
                    it.copy(
                        stats = stats,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingIconStyleFlow.collect { style ->
                _uiState.update { it.copy(settingIconStyle = style) }
            }
        }
    }
}

