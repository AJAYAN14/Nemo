package com.jian.nemo.feature.learning.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.data.util.AIClient
import com.jian.nemo.core.domain.model.AIReadingArticle
import com.jian.nemo.core.domain.model.AIReadingHistory
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.AIWorkshopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.TtsEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import com.jian.nemo.core.domain.repository.AIConfig

data class AIReadingUiState(
    val currentArticle: AIReadingArticle? = null,
    val currentArticleHistoryId: Int? = null,
    val selectedAnswers: List<Int?> = listOf(null, null, null),
    val isSubmitted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isConfigured: Boolean = false,
    val difficulty: String = "N5",
    val readingTheme: String = "日常生活",
    val playingAudioId: String? = null,
    val aiPlatform: String = "openai",
    val aiModel: String = "",
    val switchedConfigName: String? = null,
    val readingHistory: List<AIReadingHistory> = emptyList()
)

sealed interface AIReadingEvent {
    object GenerateArticle : AIReadingEvent
    data class SelectAnswer(val questionIndex: Int, val answerIndex: Int) : AIReadingEvent
    object SubmitAnswers : AIReadingEvent
    data class UpdateDifficulty(val difficulty: String) : AIReadingEvent
    data class UpdateTheme(val theme: String) : AIReadingEvent
    data class SpeakText(val text: String, val id: String) : AIReadingEvent
    object ClearError : AIReadingEvent
    object ResetReader : AIReadingEvent
    object QuickSwitchPlatform : AIReadingEvent
    
    // 历史记录事件
    data class LoadHistoryArticle(val history: AIReadingHistory) : AIReadingEvent
    data class DeleteHistory(val id: Int) : AIReadingEvent
    object ClearAllHistory : AIReadingEvent
}

@HiltViewModel
class AIReadingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val aiClient: AIClient,
    private val audioRepository: AudioRepository,
    private val aiWorkshopRepository: AIWorkshopRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIReadingUiState())
    val uiState: StateFlow<AIReadingUiState> = _uiState.asStateFlow()

    private var aiPlatform = ""
    private var aiApiKey = ""
    private var aiBaseUrl = ""
    private var aiModel = ""

    // 缓存配置列表，用于轮转切换
    private var allConfigs: List<AIConfig> = emptyList()

    init {
        observeSettings()
        observeTtsEvents()
        observeReadingHistory()
    }

    private fun observeReadingHistory() {
        viewModelScope.launch {
            aiWorkshopRepository.getReadingHistory().collect { history ->
                _uiState.update { it.copy(readingHistory = history) }
            }
        }
    }

    private fun observeTtsEvents() {
        viewModelScope.launch {
            audioRepository.ttsEvents.collect { event ->
                when (event) {
                    is TtsEvent.OnStart -> {
                        _uiState.update { it.copy(playingAudioId = event.id) }
                    }
                    is TtsEvent.OnDone, is TtsEvent.OnError -> {
                        _uiState.update { state ->
                            if (state.playingAudioId == (event as? TtsEvent.OnDone)?.id || 
                                state.playingAudioId == (event as? TtsEvent.OnError)?.id) {
                                state.copy(playingAudioId = null)
                            } else state
                        }
                    }
                    else -> {}
                }
            }
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

        // 协程2：监听阅读专属的默认难度设置
        viewModelScope.launch {
            settingsRepository.aiWorkshopDifficultyFlow.collect { difficulty ->
                _uiState.update { state ->
                    state.copy(
                        difficulty = if (state.difficulty.isBlank()) difficulty else state.difficulty
                    )
                }
            }
        }
    }

    fun onEvent(event: AIReadingEvent) {
        when (event) {
            is AIReadingEvent.GenerateArticle -> generateArticle()
            is AIReadingEvent.SelectAnswer -> {
                if (!_uiState.value.isSubmitted) {
                    _uiState.update { state ->
                        val newAnswers = state.selectedAnswers.toMutableList()
                        if (event.questionIndex in newAnswers.indices) {
                            newAnswers[event.questionIndex] = event.answerIndex
                        }
                        state.copy(selectedAnswers = newAnswers)
                    }
                    // 同步更新数据库
                    viewModelScope.launch {
                        val currentId = _uiState.value.currentArticleHistoryId ?: return@launch
                        val answers = _uiState.value.selectedAnswers
                        aiWorkshopRepository.updateReadingAnswers(currentId, answers, false)
                    }
                }
            }
            is AIReadingEvent.SubmitAnswers -> {
                if (_uiState.value.selectedAnswers.all { it != null } && !_uiState.value.isSubmitted) {
                    _uiState.update { it.copy(isSubmitted = true) }
                    // 同步更新数据库为已提交状态
                    viewModelScope.launch {
                        val currentId = _uiState.value.currentArticleHistoryId ?: return@launch
                        val answers = _uiState.value.selectedAnswers
                        aiWorkshopRepository.updateReadingAnswers(currentId, answers, true)
                    }
                }
            }
            is AIReadingEvent.UpdateDifficulty -> {
                _uiState.update { it.copy(difficulty = event.difficulty) }
                // 同步保存至本地默认难度
                viewModelScope.launch {
                    settingsRepository.setAiWorkshopDifficulty(event.difficulty)
                }
            }
            is AIReadingEvent.UpdateTheme -> {
                _uiState.update { it.copy(readingTheme = event.theme) }
            }
            is AIReadingEvent.SpeakText -> {
                val currentPlaying = _uiState.value.playingAudioId
                if (currentPlaying == event.id) {
                    // 如果正在播放相同的文本，则停止播放
                    audioRepository.stop()
                    _uiState.update { it.copy(playingAudioId = null) }
                } else {
                    // 播放新的语音文本
                    audioRepository.playTts(event.text, id = event.id)
                }
            }
            is AIReadingEvent.ClearError -> {
                _uiState.update { it.copy(error = null) }
            }
            is AIReadingEvent.ResetReader -> {
                _uiState.update { 
                    it.copy(
                        currentArticle = null,
                        currentArticleHistoryId = null,
                        selectedAnswers = listOf(null, null, null),
                        isSubmitted = false,
                        error = null,
                        playingAudioId = null
                    )
                }
            }
            is AIReadingEvent.QuickSwitchPlatform -> {
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
                    kotlinx.coroutines.delay(2500)
                    _uiState.update { it.copy(switchedConfigName = null) }
                }
            }
            is AIReadingEvent.LoadHistoryArticle -> {
                val article = AIReadingArticle(
                    title = event.history.title,
                    level = event.history.level,
                    contentRaw = event.history.contentRaw,
                    contentHtml = "",
                    translation = event.history.translation,
                    vocabulary = event.history.vocabulary,
                    questions = event.history.questions
                )
                _uiState.update { state ->
                    state.copy(
                        currentArticle = article,
                        currentArticleHistoryId = event.history.id,
                        selectedAnswers = event.history.selectedAnswers,
                        isSubmitted = event.history.isSubmitted,
                        isLoading = false,
                        error = null
                    )
                }
            }
            is AIReadingEvent.DeleteHistory -> {
                viewModelScope.launch {
                    aiWorkshopRepository.deleteReadingHistoryById(event.id)
                }
            }
            is AIReadingEvent.ClearAllHistory -> {
                viewModelScope.launch {
                    aiWorkshopRepository.clearReadingHistory()
                }
            }
        }
    }

    private fun generateArticle() {
        if (aiApiKey.isBlank()) {
            _uiState.update { it.copy(error = "API 密钥未配置，请前往设置配置！") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val difficulty = _uiState.value.difficulty
            val theme = _uiState.value.readingTheme

            val result = aiClient.generateReadingArticle(
                platform = aiPlatform,
                apiKey = aiApiKey,
                baseUrl = aiBaseUrl.takeIf { it.isNotBlank() },
                model = aiModel,
                difficulty = difficulty,
                theme = theme
            )

            result.fold(
                onSuccess = { article ->
                    // 成功生成文章后，立刻写入本地数据库历史，记录其生成的 ID
                    val insertedId = aiWorkshopRepository.saveReadingHistory(
                        article = article,
                        selectedAnswers = listOf(null, null, null),
                        isSubmitted = false
                    )
                    _uiState.update { state ->
                        state.copy(
                            currentArticle = article,
                            currentArticleHistoryId = insertedId.toInt(),
                            selectedAnswers = listOf(null, null, null),
                            isSubmitted = false,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = "生成短文失败，请重试: ${error.localizedMessage ?: error.message}"
                        )
                    }
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRepository.stop()
    }
}
