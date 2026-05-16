package com.jian.nemo.feature.learning.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.data.util.AIClient
import com.jian.nemo.core.data.local.dao.GrammarDao
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

/**
 * 工坊模式枚举
 */
enum class WorkshopMode {
    FREE,    // 自由模式：AI 随机生成翻译题
    GRAMMAR  // 语法专项模式：基于本地数据库语法点生成
}

data class AIWorkshopUiState(
    val currentExercise: AIExercise? = null,
    val userAnswer: String = "",
    val gradeResult: AIGradeResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConfigured: Boolean = false,
    val difficulty: String = "N5",
    val workshopMode: WorkshopMode = WorkshopMode.FREE,
    // 语法专项模式相关
    val currentGrammarPoint: String? = null,     // 当前抽取的语法点名称
    val currentGrammarSubtype: String? = null,   // 当前用法子类型
    val hasGrammarData: Boolean = true,           // 当前等级是否有语法数据
    val aiPlatform: String = "openai",
    val aiModel: String = ""
)

sealed interface AIWorkshopEvent {
    object GenerateNewExercise : AIWorkshopEvent
    data class UpdateUserAnswer(val answer: String) : AIWorkshopEvent
    object SubmitAnswer : AIWorkshopEvent
    object ClearError : AIWorkshopEvent
    data class UpdateDifficulty(val difficulty: String) : AIWorkshopEvent
    data class UpdateWorkshopMode(val mode: WorkshopMode) : AIWorkshopEvent
    object QuickSwitchPlatform : AIWorkshopEvent
}

@HiltViewModel
class AIWorkshopViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiWorkshopRepository: AIWorkshopRepository,
    private val aiClient: AIClient,
    private val grammarDao: GrammarDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIWorkshopUiState())
    val uiState: StateFlow<AIWorkshopUiState> = _uiState.asStateFlow()

    private var aiPlatform = ""
    private var aiApiKey = ""
    private var aiBaseUrl = ""
    private var aiModel = ""
    
    // 缓存所有平台的 API Key 状态，用于智能切换
    private var platformKeys = mutableMapOf<String, String>()
    private val platformsToCycle = listOf("gemini", "deepseek", "openai", "custom")

    // 当前语法专项模式的 AI 上下文（用于出题和评分）
    private var currentGrammarInfo: AIClient.GrammarInfo? = null
    private var currentUsageId: Int? = null

    // 标志位：确保从本地存储恢复答案的操作仅在初始化时执行一次
    private var isAnswerRestored = false

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
                settingsRepository.aiCurrentExerciseFlow,
                settingsRepository.aiCurrentAnswerFlow,
                settingsRepository.aiWorkshopModeFlow,
                settingsRepository.getAiApiKeyFlow("openai"),
                settingsRepository.getAiApiKeyFlow("gemini"),
                settingsRepository.getAiApiKeyFlow("deepseek"),
                settingsRepository.getAiApiKeyFlow("custom")
            ) { values: Array<String> ->
                val platform = values[0]
                val apiKey = values[1]
                val baseUrl = values[2]
                val model = values[3]
                val difficulty = values[4]
                val currentExerciseJson = values[5]
                val currentAnswer = values[6]
                val workshopModeStr = values[7]
                
                // 更新 Key 缓存
                platformKeys["openai"] = values[8]
                platformKeys["gemini"] = values[9]
                platformKeys["deepseek"] = values[10]
                platformKeys["custom"] = values[11]

                val restoredMode = try {
                    WorkshopMode.valueOf(workshopModeStr)
                } catch (e: Exception) {
                    WorkshopMode.FREE
                }

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
                        workshopMode = restoredMode,
                        currentExercise = restoredExercise,
                        userAnswer = if (!isAnswerRestored && state.userAnswer.isBlank()) {
                            if (currentAnswer.isNotBlank()) isAnswerRestored = true
                            currentAnswer
                        } else state.userAnswer,
                        aiPlatform = platform,
                        aiModel = model
                    )
                }
            }.collect()
        }
    }

    fun onEvent(event: AIWorkshopEvent) {
        when (event) {
            is AIWorkshopEvent.GenerateNewExercise -> generateExercise()
            is AIWorkshopEvent.UpdateUserAnswer -> {
                isAnswerRestored = true
                _uiState.update { it.copy(userAnswer = event.answer) }
                viewModelScope.launch {
                    settingsRepository.setAiCurrentAnswer(event.answer)
                }
            }
            is AIWorkshopEvent.SubmitAnswer -> submitAnswer()
            is AIWorkshopEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
            is AIWorkshopEvent.UpdateDifficulty -> {
                viewModelScope.launch {
                    settingsRepository.setAiWorkshopDifficulty(event.difficulty)
                    // 切换等级时检查该等级是否有语法数据
                    checkGrammarDataAvailability(event.difficulty)
                }
            }
            is AIWorkshopEvent.UpdateWorkshopMode -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(workshopMode = event.mode) }
                    settingsRepository.setAiWorkshopMode(event.mode.name)
                    // 切换模式时检查语法数据可用性
                    if (event.mode == WorkshopMode.GRAMMAR) {
                        checkGrammarDataAvailability(_uiState.value.difficulty)
                    }
                }
            }
            is AIWorkshopEvent.QuickSwitchPlatform -> {
                viewModelScope.launch {
                    val currentPlatform = _uiState.value.aiPlatform
                    val currentIndex = platformsToCycle.indexOf(currentPlatform).takeIf { it != -1 } ?: 0
                    
                    // 寻找下一个有 Key 的平台
                    var found = false
                    for (i in 1 until platformsToCycle.size) {
                        val nextIndex = (currentIndex + i) % platformsToCycle.size
                        val nextPlatform = platformsToCycle[nextIndex]
                        if (platformKeys[nextPlatform]?.isNotBlank() == true) {
                            settingsRepository.setAiPlatform(nextPlatform)
                            found = true
                            break
                        }
                    }
                    
                    if (!found) {
                        _uiState.update { it.copy(error = "没有其他已配置的 AI 平台") }
                    }
                }
            }
        }
    }

    /**
     * 检查当前等级是否有可用的语法数据
     */
    private suspend fun checkGrammarDataAvailability(level: String) {
        val count = grammarDao.getGrammarCountByLevel(level)
        _uiState.update { it.copy(hasGrammarData = count > 0) }
    }

    private fun generateExercise() {
        if (aiApiKey.isBlank()) {
            _uiState.update { it.copy(error = "请先在设置中配置 API Key") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    gradeResult = null,
                    userAnswer = "",
                    currentGrammarPoint = null,
                    currentGrammarSubtype = null
                )
            }
            isAnswerRestored = true
            // 清除答案持久化缓存
            settingsRepository.setAiCurrentAnswer("")

            val mode = _uiState.value.workshopMode
            val difficulty = _uiState.value.difficulty

            // 语法专项模式：先从数据库随机抽取语法
            var grammarInfo: AIClient.GrammarInfo? = null
            if (mode == WorkshopMode.GRAMMAR) {
                val grammarWithUsages = grammarDao.getRandomGrammarWithUsagesByLevel(difficulty)
                if (grammarWithUsages == null || grammarWithUsages.usages.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            error = "本地暂无 $difficulty 等级的语法数据，请先同步或切换到自由模式",
                            isLoading = false,
                            hasGrammarData = false
                        )
                    }
                    return@launch
                }

                // 从用法列表中随机抽取一个分支
                val randomUsage = grammarWithUsages.usages.random()
                grammarInfo = AIClient.GrammarInfo(
                    name = grammarWithUsages.grammar.grammar,
                    connection = randomUsage.usage.connection,
                    explanation = randomUsage.usage.explanation,
                    subtype = randomUsage.usage.subtype,
                    notes = randomUsage.usage.notes
                )
                currentGrammarInfo = grammarInfo
                currentUsageId = randomUsage.usage.id

                _uiState.update {
                    it.copy(
                        currentGrammarPoint = grammarWithUsages.grammar.grammar,
                        currentGrammarSubtype = randomUsage.usage.subtype
                    )
                }
            } else {
                currentGrammarInfo = null
                currentUsageId = null
            }

            val result = aiClient.generateExercise(
                platform = aiPlatform,
                apiKey = aiApiKey,
                baseUrl = aiBaseUrl,
                model = aiModel,
                difficulty = difficulty,
                grammarInfo = grammarInfo
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
                userAnswer = answer,
                grammarInfo = currentGrammarInfo
            )
            
            result.onSuccess { grade ->
                _uiState.update { it.copy(gradeResult = grade, isLoading = false) }
                // 保存到历史记录（包含语法点信息）
                aiWorkshopRepository.saveExercise(
                    question = exercise.question,
                    type = exercise.type,
                    difficulty = exercise.difficulty,
                    standardAnswer = grade.standard_answer ?: exercise.answer,
                    userAnswer = answer,
                    score = grade.score,
                    feedback = grade.feedback,
                    grammarPoint = _uiState.value.currentGrammarPoint,
                    usageId = currentUsageId
                )
                // 清除当前题目与答案缓存
                viewModelScope.launch {
                    settingsRepository.setAiCurrentExercise("")
                    settingsRepository.setAiCurrentAnswer("")
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "评分失败: ${e.message}", isLoading = false) }
            }
        }
    }
}
