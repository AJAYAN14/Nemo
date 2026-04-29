package com.jian.nemo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AISettingsUiState(
    val platform: String = "openai",
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val difficulty: String = "N5",
    val isLoading: Boolean = true
)

sealed interface AISettingsEvent {
    data class SetPlatform(val platform: String) : AISettingsEvent
    data class SetApiKey(val key: String) : AISettingsEvent
    data class SetBaseUrl(val url: String) : AISettingsEvent
    data class SetModel(val model: String) : AISettingsEvent
    data class SetDifficulty(val difficulty: String) : AISettingsEvent
}

@HiltViewModel
class AISettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AISettingsUiState())
    val uiState: StateFlow<AISettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.aiPlatformFlow,
                settingsRepository.aiApiKeyFlow,
                settingsRepository.aiBaseUrlFlow,
                settingsRepository.aiModelFlow,
                settingsRepository.aiWorkshopDifficultyFlow
            ) { platform, apiKey, baseUrl, model, difficulty ->
                AISettingsUiState(
                    platform = platform,
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    model = model,
                    difficulty = difficulty,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onEvent(event: AISettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is AISettingsEvent.SetPlatform -> settingsRepository.setAiPlatform(event.platform)
                is AISettingsEvent.SetApiKey -> settingsRepository.setAiApiKey(event.key)
                is AISettingsEvent.SetBaseUrl -> settingsRepository.setAiBaseUrl(event.url)
                is AISettingsEvent.SetModel -> settingsRepository.setAiModel(event.model)
                is AISettingsEvent.SetDifficulty -> settingsRepository.setAiWorkshopDifficulty(event.difficulty)
            }
        }
    }
}
