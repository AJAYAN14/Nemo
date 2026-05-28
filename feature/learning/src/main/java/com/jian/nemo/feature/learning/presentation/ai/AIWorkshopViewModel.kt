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
import com.jian.nemo.core.domain.model.AIReadingArticle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.AIWorkshopRepository
import com.jian.nemo.core.domain.repository.AIConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.TtsEvent


@Serializable
data class AIPersistedGrammarState(
    val name: String,
    val connection: String,
    val explanation: String,
    val subtype: String?,
    val notes: String?,
    val usageId: Int
)


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
    val aiModel: String = "",
    val playingAudioId: String? = null,           // 当前正在播放的音频 ID
    val switchedConfigName: String? = null        // 切换配置时短暂显示的别名
)


sealed interface AIWorkshopEvent {
    object GenerateNewExercise : AIWorkshopEvent
    data class UpdateUserAnswer(val answer: String) : AIWorkshopEvent
    object SubmitAnswer : AIWorkshopEvent
    object ClearError : AIWorkshopEvent
    data class UpdateDifficulty(val difficulty: String) : AIWorkshopEvent
    data class UpdateWorkshopMode(val mode: WorkshopMode) : AIWorkshopEvent
    object QuickSwitchPlatform : AIWorkshopEvent
    data class SpeakText(val text: String, val id: String) : AIWorkshopEvent
}



@HiltViewModel
class AIWorkshopViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiWorkshopRepository: AIWorkshopRepository,
    private val aiClient: AIClient,
    private val grammarDao: GrammarDao,
    private val audioRepository: AudioRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(AIWorkshopUiState())
    val uiState: StateFlow<AIWorkshopUiState> = _uiState.asStateFlow()

    private var aiPlatform = ""
    private var aiApiKey = ""
    private var aiBaseUrl = ""
    private var aiModel = ""

    // 缓存配置列表，用于轮转切换
    private var allConfigs: List<AIConfig> = emptyList()

    // 当前语法专项模式的 AI 上下文（用于出题和评分）
    private var currentGrammarInfo: AIClient.GrammarInfo? = null
    private var currentUsageId: Int? = null

    // 标志位：确保从本地存储恢复答案的操作仅在初始化时执行一次
    private var isAnswerRestored = false

    // 标志位：确保从本地存储恢复状态的操作仅在初始化时执行一次
    private var isStateRestored = false

    init {
        observeSettings()
        cleanupOldHistory()
        observeTtsEvents()
    }

    private fun observeTtsEvents() {
        viewModelScope.launch {
            audioRepository.ttsEvents.collect { event ->
                when (event) {
                    is TtsEvent.OnStart -> {
                        _uiState.update { it.copy(playingAudioId = event.id) }
                    }
                    is TtsEvent.OnDone, is TtsEvent.OnError -> {
                        _uiState.update { 
                            if (it.playingAudioId == (event as? TtsEvent.OnDone)?.id || 
                                it.playingAudioId == (event as? TtsEvent.OnError)?.id) {
                                it.copy(playingAudioId = null)
                            } else it
                        }
                    }
                    else -> {}
                }
            }
        }
    }



    private fun cleanupOldHistory() {
        viewModelScope.launch {
            aiWorkshopRepository.deleteOldHistory(30)
        }
    }

    private fun observeSettings() {
        // 协程1：监听配置中心的配置列表和当前激活配置
        viewModelScope.launch {
            combine(
                settingsRepository.aiConfigListFlow,
                settingsRepository.aiActiveConfigIdFlow
            ) { configListJson, activeId ->
                val configs = try {
                    Json.decodeFromString<List<AIConfig>>(configListJson)
                } catch (e: Exception) {
                    emptyList()
                }
                allConfigs = configs
                configs.find { it.id == activeId } ?: configs.firstOrNull()
            }.collect { activeConfig ->
                aiPlatform = activeConfig?.platform ?: ""
                aiApiKey = activeConfig?.apiKey ?: ""
                aiBaseUrl = activeConfig?.baseUrl ?: ""
                aiModel = activeConfig?.model ?: ""
                _uiState.update { state ->
                    state.copy(
                        isConfigured = aiApiKey.isNotBlank(),
                        aiPlatform = activeConfig?.platform ?: "openai",
                        aiModel = activeConfig?.model ?: ""
                    )
                }
            }
        }

        // 协程2：监听 Workshop 专属设置（难度/题目/答案/模式/评分结果/语法状态）
        viewModelScope.launch {
            combine(
                settingsRepository.aiWorkshopDifficultyFlow,
                settingsRepository.aiCurrentExerciseFlow,
                settingsRepository.aiCurrentAnswerFlow,
                settingsRepository.aiWorkshopModeFlow,
                settingsRepository.aiCurrentGradeResultFlow,
                settingsRepository.aiCurrentGrammarStateFlow
            ) { flowsArray ->
                flowsArray
            }.collect { values ->
                val difficulty = values[0]
                val currentExerciseJson = values[1]
                val currentAnswer = values[2]
                val workshopModeStr = values[3]
                val currentGradeResultJson = values[4]
                val currentGrammarStateJson = values[5]

                val restoredMode = try {
                    WorkshopMode.valueOf(workshopModeStr)
                } catch (e: Exception) {
                    WorkshopMode.FREE
                }

                val restoredGrammarState = if (!isStateRestored && currentGrammarStateJson.isNotBlank()) {
                    try {
                        Json.decodeFromString<AIPersistedGrammarState>(currentGrammarStateJson)
                    } catch (_: Exception) {
                        null
                    }
                } else null

                // 还原内存中的语法点详情和用法ID
                if (!isStateRestored) {
                    if (restoredGrammarState != null) {
                        currentGrammarInfo = AIClient.GrammarInfo(
                            name = restoredGrammarState.name,
                            connection = restoredGrammarState.connection,
                            explanation = restoredGrammarState.explanation,
                            subtype = restoredGrammarState.subtype,
                            notes = restoredGrammarState.notes
                        )
                        currentUsageId = restoredGrammarState.usageId
                    } else {
                        currentGrammarInfo = null
                        currentUsageId = null
                    }
                }

                _uiState.update { state ->
                    val restoredExercise = if (!isStateRestored && currentExerciseJson.isNotBlank()) {
                        try {
                            Json.decodeFromString<AIExercise>(currentExerciseJson)
                        } catch (_: Exception) {
                            null
                        }
                    } else state.currentExercise

                    val restoredGradeResult = if (!isStateRestored && currentGradeResultJson.isNotBlank()) {
                        try {
                            Json.decodeFromString<AIGradeResult>(currentGradeResultJson)
                        } catch (_: Exception) {
                            null
                        }
                    } else state.gradeResult

                    val restoredGrammarPoint = if (!isStateRestored) {
                        restoredGrammarState?.name
                    } else state.currentGrammarPoint

                    val restoredGrammarSubtype = if (!isStateRestored) {
                        restoredGrammarState?.subtype
                    } else state.currentGrammarSubtype

                    state.copy(
                        difficulty = difficulty,
                        workshopMode = restoredMode,
                        currentExercise = restoredExercise,
                        gradeResult = restoredGradeResult,
                        currentGrammarPoint = restoredGrammarPoint,
                        currentGrammarSubtype = restoredGrammarSubtype,
                        userAnswer = if (!isAnswerRestored && state.userAnswer.isBlank()) {
                            if (currentAnswer.isNotBlank()) isAnswerRestored = true
                            currentAnswer
                        } else state.userAnswer
                    )
                }

                isStateRestored = true
            }
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
                    val configs = allConfigs
                    if (configs.size <= 1) {
                        _uiState.update {
                            it.copy(error = if (configs.isEmpty()) "请先在配置中心添加 AI 模型" else "只有一个配置，无法切换")
                        }
                        return@launch
                    }
                    val activeId = settingsRepository.aiActiveConfigIdFlow.first()
                    val currentIndex = configs.indexOfFirst { it.id == activeId }.takeIf { it != -1 } ?: 0
                    val nextConfig = configs[(currentIndex + 1) % configs.size]
                    // 切换到下一个配置
                    settingsRepository.setAiActiveConfigId(nextConfig.id)
                    // 短暂显示切换的别名气泡
                    _uiState.update { it.copy(switchedConfigName = nextConfig.name) }
                    delay(2500)
                    _uiState.update { it.copy(switchedConfigName = null) }
                }
            }
            is AIWorkshopEvent.SpeakText -> {
                audioRepository.playTts(event.text, id = event.id)
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
            // 清除答案、题目、评分及语法状态的持久化缓存
            settingsRepository.setAiCurrentAnswer("")
            settingsRepository.setAiCurrentExercise("")
            settingsRepository.setAiCurrentGradeResult("")
            settingsRepository.setAiCurrentGrammarState("")

            val mode = _uiState.value.workshopMode
            val difficulty = _uiState.value.difficulty



            // 语法专项模式：先从数据库随机抽取语法
            var grammarInfo: AIClient.GrammarInfo? = null
            if (mode == WorkshopMode.GRAMMAR) {
                // 1. 从 DataStore 获取最近已抽取的 ID 列表
                val recentIdsJson = settingsRepository.aiRecentGrammarIdsFlow.first()
                val recentIds = try {
                    Json.decodeFromString<List<Int>>(recentIdsJson)
                } catch (e: Exception) {
                    emptyList()
                }

                // 2. 优先进行去重抽取
                var grammarWithUsages = if (recentIds.isNotEmpty()) {
                    grammarDao.getRandomGrammarWithUsagesByLevelExcept(difficulty, recentIds)
                } else null

                // 3. 降级兜底：若去重抽取返回空，退回普通抽取
                if (grammarWithUsages == null) {
                    grammarWithUsages = grammarDao.getRandomGrammarWithUsagesByLevel(difficulty)
                }

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

                // 4. 更新已使用 ID 队列（限定最多保留最近 10 个 ID）
                val currentId = grammarWithUsages.grammar.id
                val updatedRecentIds = (listOf(currentId) + recentIds.filter { it != currentId }).take(10)
                viewModelScope.launch {
                    settingsRepository.setAiRecentGrammarIds(Json.encodeToString(updatedRecentIds))
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
                // 保存语法状态到 DataStore
                val grammarState = AIPersistedGrammarState(
                    name = grammarWithUsages.grammar.grammar,
                    connection = randomUsage.usage.connection,
                    explanation = randomUsage.usage.explanation,
                    subtype = randomUsage.usage.subtype,
                    notes = randomUsage.usage.notes,
                    usageId = randomUsage.usage.id
                )
                viewModelScope.launch {
                    settingsRepository.setAiCurrentGrammarState(Json.encodeToString(grammarState))
                }
            } else {
                currentGrammarInfo = null
                currentUsageId = null
                viewModelScope.launch {
                    settingsRepository.setAiCurrentGrammarState("")
                }
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
                // 持久化当前评分反馈结果（AI例文）
                viewModelScope.launch {
                    settingsRepository.setAiCurrentGradeResult(Json.encodeToString(grade))
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = "评分失败: ${e.message}", isLoading = false) }
            }
        }
    }
}
