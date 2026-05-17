package com.jian.nemo.feature.test.presentation.ability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.data.audio.TtsManager
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.data.local.dao.TestRecordDao
import com.jian.nemo.core.data.local.entity.TestRecordEntity
import com.jian.nemo.core.data.util.AIClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

import android.content.Context
import com.jian.nemo.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Serializable
data class VerbConjugationQuestion(
    val word: String,
    val furigana: String,
    val meaning: String,
    val qText: String, // 带有 FuriganaText 格式 [kana] 的文本
    val translation: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

@Serializable
data class VerbConjugationSession(
    val level: String,
    val questions: List<VerbConjugationQuestion>,
    val currentIndex: Int,
    val correctCount: Int,
    val userAnswers: List<Int?> = emptyList()
)

sealed interface VerbUiState {
    data object Loading : VerbUiState
    data object ApiNotConfigured : VerbUiState
    data object LevelSelecting : VerbUiState
    data object Generating : VerbUiState
    data class Ready(
        val questions: List<VerbConjugationQuestion>,
        val currentIndex: Int = 0,
        val userAnswers: List<Int?> = List(questions.size) { null },
        val correctCount: Int = 0
    ) : VerbUiState {
        val selectedOptionIndex: Int? get() = userAnswers.getOrNull(currentIndex)
        val isAnswered: Boolean get() = selectedOptionIndex != null
    }
    data class Finished(val correctCount: Int, val totalCount: Int) : VerbUiState
    data class Error(val message: String) : VerbUiState
}

@HiltViewModel
class VerbConjugationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsManager: TtsManager,
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val testRecordDao: TestRecordDao,
    private val aiClient: AIClient,
    @ApplicationScope private val externalScope: CoroutineScope
) : ViewModel() {

    sealed interface GlobalGenStatus {
        object Idle : GlobalGenStatus
        object Generating : GlobalGenStatus
        data class Success(val questions: List<VerbConjugationQuestion>, val level: String) : GlobalGenStatus
        data class Error(val message: String) : GlobalGenStatus
    }

    companion object {
        private val _globalGenStatus = MutableStateFlow<GlobalGenStatus>(GlobalGenStatus.Idle)
        val globalGenStatus: StateFlow<GlobalGenStatus> = _globalGenStatus.asStateFlow()

        // 追踪正在进行的生成协程 Job，以便于在卡死时手动中断/取消
        private var activeGenerationJob: kotlinx.coroutines.Job? = null
    }

    private val _uiState = MutableStateFlow<VerbUiState>(VerbUiState.Loading)
    val uiState: StateFlow<VerbUiState> = _uiState.asStateFlow()

    val historyRecords: Flow<List<TestRecordEntity>> = testRecordDao.getRecordsByMode("verb_conjugation_ai", 50)

    // 缓存机制与本地会话
    private val questionsCache = mutableMapOf<String, List<VerbConjugationQuestion>>()
    private val prefs = context.getSharedPreferences("verb_ai_session", Context.MODE_PRIVATE)
    var currentLevel: String = ""
        private set

    init {
        checkApiConfigured()
        observeGlobalGeneration()
    }

    private fun observeGlobalGeneration() {
        viewModelScope.launch {
            globalGenStatus.collect { status ->
                when (status) {
                    is GlobalGenStatus.Generating -> {
                        _uiState.value = VerbUiState.Generating
                    }
                    is GlobalGenStatus.Success -> {
                        val current = _uiState.value
                        if (current is VerbUiState.Generating || current is VerbUiState.Loading) {
                            currentLevel = status.level
                            val readyState = VerbUiState.Ready(questions = status.questions)
                            _uiState.value = readyState
                            saveSession(readyState)
                            _globalGenStatus.value = GlobalGenStatus.Idle
                        }
                    }
                    is GlobalGenStatus.Error -> {
                        val current = _uiState.value
                        if (current is VerbUiState.Generating) {
                            _uiState.value = VerbUiState.Error(status.message)
                            _globalGenStatus.value = GlobalGenStatus.Idle
                        }
                    }
                    is GlobalGenStatus.Idle -> {
                        // 闲置状态，无需处理
                    }
                }
            }
        }
    }

    private fun checkApiConfigured() {
        viewModelScope.launch {
            val platform = settingsRepository.aiPlatformFlow.first()
            val apiKey = settingsRepository.getAiApiKeyFlow(platform).first()
            if (platform.isBlank() || apiKey.isBlank()) {
                _uiState.value = VerbUiState.ApiNotConfigured
            } else {
                val session = restoreSession()
                if (session != null) {
                    currentLevel = session.level
                    _uiState.value = VerbUiState.Ready(
                        questions = session.questions,
                        currentIndex = session.currentIndex,
                        correctCount = session.correctCount,
                        userAnswers = session.userAnswers.ifEmpty { List(session.questions.size) { null } }
                    )
                } else {
                    // 核心修复：防止异步竞态覆盖。重新进入界面时，若全局后台正在生成，绝不能强行打回 LevelSelecting，而是接管 Generating 状态
                    if (globalGenStatus.value is GlobalGenStatus.Generating) {
                        _uiState.value = VerbUiState.Generating
                    } else {
                        _uiState.value = VerbUiState.LevelSelecting
                    }
                }
            }
        }
    }

    private fun saveSession(state: VerbUiState.Ready) {
        val session = VerbConjugationSession(
            level = currentLevel,
            questions = state.questions,
            currentIndex = state.currentIndex,
            correctCount = state.correctCount,
            userAnswers = state.userAnswers
        )
        prefs.edit().putString("current_session", Json.encodeToString(session)).apply()
    }

    private fun clearSession() {
        prefs.edit().remove("current_session").apply()
    }

    private fun restoreSession(): VerbConjugationSession? {
        val json = prefs.getString("current_session", null) ?: return null
        return try {
            Json.decodeFromString<VerbConjugationSession>(json)
        } catch (e: Exception) {
            clearSession()
            null
        }
    }

    fun forceRegenerate() {
        clearSession()
        onLevelSelected(currentLevel, forceRegenerate = true)
    }

    fun onLevelSelected(level: String, forceRegenerate: Boolean = false) {
        currentLevel = level
        
        // 如果全局已经有人在生成了，直接进入接管状态，不发起重复请求
        if (_globalGenStatus.value is GlobalGenStatus.Generating) {
            _uiState.value = VerbUiState.Generating
            return
        }

        _uiState.value = VerbUiState.Generating
        _globalGenStatus.value = GlobalGenStatus.Generating

        // 核心优化：使用 externalScope 保证退出页面不被打断，支持后台持久化
        // 追踪该后台 Job 以便手动取消/重试
        activeGenerationJob?.cancel()
        val job = externalScope.launch {
            try {
                // 从本地词库抽取 5 个该等级的动词
                val allVerbs = wordRepository.getWordsByPartOfSpeech(PartOfSpeech.VERB)
                val targetVerbs = allVerbs.filter { it.level.equals(level, ignoreCase = true) && !it.isDelisted }
                
                val selectedWords = if (targetVerbs.size >= 5) {
                    targetVerbs.shuffled().take(5)
                } else {
                    val fallback = allVerbs.filter { !it.isDelisted }
                    (targetVerbs + fallback).distinctBy { it.id }.shuffled().take(5)
                }

                if (selectedWords.isEmpty()) {
                    _globalGenStatus.value = GlobalGenStatus.Error("词库中没有动词数据")
                    return@launch
                }

                val wordInfos = selectedWords.map { 
                    AIClient.VerbWordInfo(it.japanese, it.hiragana, it.chinese) 
                }

                // 检查缓存
                val cacheKey = generateCacheKey(level, selectedWords.map { it.id.toString() })
                val cached = questionsCache[cacheKey]
                if (!forceRegenerate && cached != null && cached.size == 5) {
                    // 全局广播缓存数据
                    _globalGenStatus.value = GlobalGenStatus.Success(cached, level)
                    // 同时持久化存入SharedPreferences，供新进入界面的ViewModel能秒级重连
                    val session = VerbConjugationSession(
                        level = level,
                        questions = cached,
                        currentIndex = 0,
                        correctCount = 0,
                        userAnswers = List(cached.size) { null }
                    )
                    context.getSharedPreferences("verb_ai_session", Context.MODE_PRIVATE)
                        .edit()
                        .putString("current_session", Json.encodeToString(session))
                        .apply()
                    return@launch
                }

                // 请求 AI
                val platform = settingsRepository.aiPlatformFlow.first()
                val apiKey = settingsRepository.getAiApiKeyFlow(platform).first()
                val baseUrl = settingsRepository.getAiBaseUrlFlow(platform).first()
                val model = settingsRepository.getAiModelFlow(platform).first()
                
                val result = aiClient.generateVerbConjugationQuestions(
                    platform = platform,
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    model = model,
                    difficulty = level.uppercase(),
                    words = wordInfos,
                    targetCount = 5,
                    oversampleCount = 8
                )

                result.onSuccess { aiQuestions ->
                    val questions = aiQuestions.map {
                        VerbConjugationQuestion(
                            word = it.word,
                            furigana = it.furigana,
                            meaning = it.meaning,
                            qText = it.qText,
                            translation = it.translation,
                            options = it.options,
                            correctIndex = it.correctIndex,
                            explanation = it.explanation
                        )
                    }
                    questionsCache[cacheKey] = questions
                    
                    // 1. 全局广播
                    _globalGenStatus.value = GlobalGenStatus.Success(questions, level)
                    // 2. 静默持久化写入本地会话，使用户即便当时在前台没有任何ViewModel存活，下一次进来的ViewModel也能立刻秒级加载！
                    val session = VerbConjugationSession(
                        level = level,
                        questions = questions,
                        currentIndex = 0,
                        correctCount = 0,
                        userAnswers = List(questions.size) { null }
                    )
                    context.getSharedPreferences("verb_ai_session", Context.MODE_PRIVATE)
                        .edit()
                        .putString("current_session", Json.encodeToString(session))
                        .apply()
                }.onFailure { e ->
                    _globalGenStatus.value = GlobalGenStatus.Error(e.message ?: "生成失败")
                }

            } catch (e: Exception) {
                _globalGenStatus.value = GlobalGenStatus.Error("系统错误: ${e.message}")
            } finally {
                // 如果是当前 Job 完成，重置 Job 引用
                if (activeGenerationJob == coroutineContext[kotlinx.coroutines.Job]) {
                    activeGenerationJob = null
                }
            }
        }
        activeGenerationJob = job
    }

    fun selectOption(index: Int) {
        val currentState = _uiState.value as? VerbUiState.Ready ?: return
        if (currentState.isAnswered) return
        
        val currentQ = currentState.questions[currentState.currentIndex]
        val isCorrect = index == currentQ.correctIndex
        
        val newUserAnswers = currentState.userAnswers.toMutableList().apply {
            set(currentState.currentIndex, index)
        }
        
        val newState = currentState.copy(
            userAnswers = newUserAnswers,
            correctCount = if (isCorrect) currentState.correctCount + 1 else currentState.correctCount
        )
        _uiState.value = newState
        saveSession(newState)
    }

    fun nextQuestion() {
        val currentState = _uiState.value as? VerbUiState.Ready ?: return
        if (currentState.currentIndex < currentState.questions.size - 1) {
            val newState = currentState.copy(
                currentIndex = currentState.currentIndex + 1
            )
            _uiState.value = newState
            saveSession(newState)
        } else {
            // 答题结束，落库并流转到 Finished，清除本地会话
            clearSession()
            saveRecord(currentState.correctCount, currentState.questions.size)
            _uiState.value = VerbUiState.Finished(
                correctCount = currentState.correctCount,
                totalCount = currentState.questions.size
            )
        }
    }

    fun previousQuestion() {
        val currentState = _uiState.value as? VerbUiState.Ready ?: return
        if (currentState.currentIndex > 0) {
            val newState = currentState.copy(
                currentIndex = currentState.currentIndex - 1
            )
            _uiState.value = newState
            saveSession(newState)
        }
    }

    private fun saveRecord(correct: Int, total: Int) {
        viewModelScope.launch {
            try {
                val entity = TestRecordEntity(
                    testMode = "verb_conjugation_ai",
                    totalQuestions = total,
                    correctAnswers = correct,
                    date = System.currentTimeMillis()
                )
                testRecordDao.insert(entity)
            } catch (e: Exception) {
                // 仅打日志，不阻塞 UI
            }
        }
    }

    fun playWordTts() {
        val currentState = _uiState.value as? VerbUiState.Ready ?: return
        val currentQuestion = currentState.questions[currentState.currentIndex]
        viewModelScope.launch {
            try {
                // 核心修复：现场懒加载初始化 TTS 引擎，确保即便用户直接点入本界面，朗读依然 100% 成功
                ttsManager.initialize()
                ttsManager.speak(currentQuestion.word)
            } catch (e: Exception) {
                android.util.Log.e("VerbConjugationVM", "TTS 初始化或播放失败", e)
            }
        }
    }

    fun cancelGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _globalGenStatus.value = GlobalGenStatus.Idle
        _uiState.value = VerbUiState.LevelSelecting
    }

    fun regenerateCurrentLevel() {
        if (currentLevel.isBlank()) {
            cancelGeneration()
            return
        }
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _globalGenStatus.value = GlobalGenStatus.Idle
        onLevelSelected(currentLevel, forceRegenerate = true)
    }

    fun restart() {
        clearSession()
        checkApiConfigured() // 重新开始，退回等级选择界面
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }

    private fun generateCacheKey(level: String, wordIds: List<String>): String {
        val raw = level + wordIds.sorted().joinToString()
        val bytes = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
