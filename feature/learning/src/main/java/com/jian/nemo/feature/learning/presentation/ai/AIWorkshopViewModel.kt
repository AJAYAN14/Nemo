package com.jian.nemo.feature.learning.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.data.util.AIClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import com.jian.nemo.core.domain.model.AIExercise
import com.jian.nemo.core.domain.model.AIGradeResult
import com.jian.nemo.core.domain.model.AIExerciseHistory
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.AIWorkshopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AIWorkshopUiState(
    val currentExercise: AIExercise? = null,
    val userAnswer: String = "",
    val gradeResult: AIGradeResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConfigured: Boolean = false,
    val difficulty: String = "N5"
)

sealed interface AIWorkshopEvent {
    object GenerateNewExercise : AIWorkshopEvent
    data class UpdateUserAnswer(val answer: String) : AIWorkshopEvent
    object SubmitAnswer : AIWorkshopEvent
    object ClearError : AIWorkshopEvent
    data class UpdateDifficulty(val difficulty: String) : AIWorkshopEvent
}

@HiltViewModel
class AIWorkshopViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiWorkshopRepository: AIWorkshopRepository,
    private val aiClient: AIClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIWorkshopUiState())
    val uiState: StateFlow<AIWorkshopUiState> = _uiState.asStateFlow()

    private var aiPlatform = ""
    private var aiApiKey = ""
    private var aiBaseUrl = ""
    private var aiModel = ""

    init {
        observeSettings()
        cleanupOldHistory()
    }


    private fun cleanupOldHistory() {
        viewModelScope.launch {
            aiWorkshopRepository.deleteOldHistory(30)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.aiPlatformFlow,
                settingsRepository.aiApiKeyFlow,
                settingsRepository.aiBaseUrlFlow,
                settingsRepository.aiModelFlow,
                settingsRepository.aiWorkshopDifficultyFlow,
                settingsRepository.aiCurrentExerciseFlow
            ) { values: Array<String> ->
                val platform = values[0]
                val apiKey = values[1]
                val baseUrl = values[2]
                val model = values[3]
                val difficulty = values[4]
                val currentExerciseJson = values[5]

                aiPlatform = platform
                aiApiKey = apiKey
                aiBaseUrl = baseUrl
                aiModel = model
                
                _uiState.update { state ->
                    val restoredExercise = if (state.currentExercise == null && currentExerciseJson.isNotBlank()) {
                        try {
                            Json.decodeFromString<AIExercise>(currentExerciseJson)
                        } catch (_: Exception) {
                            null
                        }
                    } else state.currentExercise

                    state.copy(
                        isConfigured = apiKey.isNotBlank(),
                        difficulty = difficulty,
                        currentExercise = restoredExercise
                    )
                }
            }.collect()
        }
    }

    fun onEvent(event: AIWorkshopEvent) {
        when (event) {
            is AIWorkshopEvent.GenerateNewExercise -> generateExercise()
            is AIWorkshopEvent.UpdateUserAnswer -> {
                _uiState.update { it.copy(userAnswer = event.answer) }
            }
            is AIWorkshopEvent.SubmitAnswer -> submitAnswer()
            is AIWorkshopEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
            is AIWorkshopEvent.UpdateDifficulty -> {
                viewModelScope.launch {
                    settingsRepository.setAiWorkshopDifficulty(event.difficulty)
                }
            }
        }
    }

    private fun generateExercise() {
        if (aiApiKey.isBlank()) {
            _uiState.update { it.copy(error = "请先在设置中配置 API Key") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, gradeResult = null, userAnswer = "") }
            val result = aiClient.generateExercise(
                platform = aiPlatform,
                apiKey = aiApiKey,
                baseUrl = aiBaseUrl,
                model = aiModel,
                difficulty = _uiState.value.difficulty
            )
            
            result.onSuccess { exercise ->
                _uiState.update { it.copy(currentExercise = exercise, isLoading = false) }
                // 持久化保存当前题目
                viewModelScope.launch {
                    settingsRepository.setAiCurrentExercise(Json.encodeToString(exercise))
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "获取题目失败: ${e.message}", isLoading = false) }
            }
        }
    }

    private fun submitAnswer() {
        val exercise = _uiState.value.currentExercise ?: return
        val answer = _uiState.value.userAnswer
        if (answer.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = aiClient.gradeAnswer(
                platform = aiPlatform,
                apiKey = aiApiKey,
                baseUrl = aiBaseUrl,
                model = aiModel,
                exercise = exercise,
                userAnswer = answer
            )
            
            result.onSuccess { grade ->
                _uiState.update { it.copy(gradeResult = grade, isLoading = false) }
                // 保存到历史记录
                aiWorkshopRepository.saveExercise(
                    question = exercise.question,
                    type = exercise.type,
                    difficulty = exercise.difficulty,
                    standardAnswer = exercise.answer,
                    userAnswer = answer,
                    score = grade.score,
                    feedback = grade.feedback
                )
                // 清除当前题目缓存
                viewModelScope.launch {
                    settingsRepository.setAiCurrentExercise("")
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "评分失败: ${e.message}", isLoading = false) }
            }
        }
    }
}
