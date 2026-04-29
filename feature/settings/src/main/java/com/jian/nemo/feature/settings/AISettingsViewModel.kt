package com.jian.nemo.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.data.util.AIClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AITestResult(
    val success: Boolean,
    val message: String
)

data class AISettingsUiState(
    val platform: String = "openai",
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val difficulty: String = "N5",
    val isLoading: Boolean = true,
    val isTesting: Boolean = false,
    val testResult: AITestResult? = null
)

sealed interface AISettingsEvent {
    data class SetPlatform(val platform: String) : AISettingsEvent
    data class SetApiKey(val key: String) : AISettingsEvent
    data class SetBaseUrl(val url: String) : AISettingsEvent
    data class SetModel(val model: String) : AISettingsEvent
    data class SetDifficulty(val difficulty: String) : AISettingsEvent
    object TestConnection : AISettingsEvent
}

@HiltViewModel
class AISettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiClient: AIClient
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
            val currentPlatform = _uiState.value.platform
            when (event) {
                is AISettingsEvent.SetPlatform -> settingsRepository.setAiPlatform(event.platform)
                is AISettingsEvent.SetApiKey -> settingsRepository.setAiApiKey(currentPlatform, event.key)
                is AISettingsEvent.SetBaseUrl -> settingsRepository.setAiBaseUrl(currentPlatform, event.url)
                is AISettingsEvent.SetModel -> settingsRepository.setAiModel(currentPlatform, event.model)
                is AISettingsEvent.SetDifficulty -> settingsRepository.setAiWorkshopDifficulty(event.difficulty)
                is AISettingsEvent.TestConnection -> {
                    _uiState.update { it.copy(isTesting = true, testResult = null) }
                    val state = _uiState.value
                    val result = aiClient.generateExercise(
                        platform = state.platform,
                        apiKey = state.apiKey,
                        baseUrl = state.baseUrl.ifBlank { null },
                        model = state.model,
                        difficulty = "N5"
                    )
                    _uiState.update { it.copy(
                        isTesting = false,
                        testResult = if (result.isSuccess) {
                            AITestResult(true, "连接成功")
                        } else {
                            val msg = result.exceptionOrNull()?.message ?: "未知错误"
                            AITestResult(false, msg)
                        }
                    ) }
                }
            }
        }
    }
}
