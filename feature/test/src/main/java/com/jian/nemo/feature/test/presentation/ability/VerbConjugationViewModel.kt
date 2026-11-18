package com.jian.nemo.feature.test.presentation.ability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.domain.model.TestRecord
import com.jian.nemo.core.domain.repository.TestRecordRepository
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
import com.jian.nemo.feature.test.data.storage.VerbConjugationStorage
import java.util.concurrent.ConcurrentHashMap
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
    private val storage: VerbConjugationStorage,
    private val audioRepository: AudioRepository,
    private val wordRepository: WordRepository,
    private val settingsRepository: SettingsRepository,
    private val testRecordRepository: TestRecordRepository,
    private val aiClient: AIClient,
    @ApplicationScope private val externalScope: CoroutineScope
) : ViewModel() {

    sealed interface GlobalGenStatus {
        object Idle : GlobalGenStatus
        object Generating : GlobalGenStatus
        data class Success(val questions: List<VerbConjugationQuestion>, val level: String) : GlobalGenStatus
        data class Error(val message: String) : GlobalGenStatus
    }

    private val _globalGenStatus = MutableStateFlow<GlobalGenStatus>(GlobalGenStatus.Idle)
    val globalGenStatus: StateFlow<GlobalGenStatus> = _globalGenStatus.asStateFlow()

    // 追踪正在进行的生成协程 Job，以便于在卡死时手动中断/取消
    private var activeGenerationJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow<VerbUiState>(VerbUiState.Loading)
    val uiState: StateFlow<VerbUiState> = _uiState.asStateFlow()

    val historyRecords: Flow<List<TestRecord>> = testRecordRepository.getRecordsByMode("verb_conjugation_ai", 50)

    // 缓存机制与本地会话
    private val questionsCache = ConcurrentHashMap<String, List<VerbConjugationQuestion>>()

    // AI 预加载缓存偏好控制开关 (默认关闭)
    private val _isPregenEnabled = MutableStateFlow(false)
    val isPregenEnabled: StateFlow<Boolean> = _isPregenEnabled.asStateFlow()

    // 能力工坊 5 大题型 AI 预出题缓存题量动态统计流 (最高展示 20)
    private val _gameCacheCounts = MutableStateFlow<Map<String, Int>>(
        mapOf(
            "verb_conjugation" to 0,
            "synonym_connection" to 0,
            "antonym_matching" to 0,
            "collocation" to 0,
            "grammar_correction" to 0
        )
    )
    val gameCacheCounts: StateFlow<Map<String, Int>> = _gameCacheCounts.asStateFlow()

    fun updateGameCacheCounts() {
        viewModelScope.launch {
            _gameCacheCounts.value = storage.getGameCacheCounts()
        }
    }

    fun togglePregenEnabled(enabled: Boolean) {
        _isPregenEnabled.value = enabled
        storage.setPregenEnabled(enabled)
        updateGameCacheCounts()
        if (enabled) {
            preloadCachePoolQuietly()
        }
    }

    // 缓存队列的读取与写入
    suspend fun getCachePool(level: String): List<List<VerbConjugationQuestion>> =
        storage.getCachePool(level)

    private fun checkAndClearExpiredCaches() {
        storage.clearExpiredCaches()
    }

    private fun saveCachePool(level: String, pool: List<List<VerbConjugationQuestion>>) {
        storage.saveCachePool(level, pool)
        updateGameCacheCounts()
    }

    // 后台智能串行自动补水函数
    fun preloadCachePoolQuietly() {
        // 安全锁：由于用户暂时关闭了此功能，直接返回，绝不发起后台 AI 出题请求
        if (true) return

        if (!_isPregenEnabled.value) return

        externalScope.launch {
            val levels = listOf("N5", "N4", "N3", "N2", "N1")
            for (level in levels) {
                // 如果中途关闭开关，立刻切断任务
                if (!_isPregenEnabled.value) break

                val currentPool = getCachePool(level)
                if (currentPool.size < 4) { // 升级为最高缓存 4 套（即 20 道题）
                    try {
                        val allVerbs = wordRepository.getWordsByPartOfSpeech(PartOfSpeech.VERB)
                        val targetVerbs = allVerbs.filter { it.level.equals(level, ignoreCase = true) && !it.isDelisted }
                        val selectedWords = if (targetVerbs.size >= 5) {
                            targetVerbs.shuffled().take(5)
                        } else {
                            val fallback = allVerbs.filter { !it.isDelisted }
                            (targetVerbs + fallback).distinctBy { it.id }.shuffled().take(5)
                        }

                        if (selectedWords.isEmpty()) continue

                        val wordInfos = selectedWords.map { 
                            AIClient.VerbWordInfo(it.japanese, it.hiragana, it.chinese) 
                        }

                        val platform = settingsRepository.aiPlatformFlow.first()
                        val apiKey = settingsRepository.getAiApiKeyFlow(platform).first()
                        val baseUrl = settingsRepository.getAiBaseUrlFlow(platform).first()
                        val model = settingsRepository.getAiModelFlow(platform).first()

                        if (platform.isBlank() || apiKey.isBlank()) continue

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

                            // 极速追加缓存
                            val updatedPool = getCachePool(level).toMutableList()
                            updatedPool.add(questions)
                            saveCachePool(level, updatedPool)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("VerbConjugationViewModel", "自动备货失败: ${level}", e)
                    }

                    // 防高并发封锁，每次补充后静默等待 1.5 秒
                    kotlinx.coroutines.delay(1500L)
                }
            }
        }
    }

    var currentLevel: String = ""
        private set

    init {
        checkAndClearExpiredCaches() // 冷启动率先检查并清除30天前的过期缓存
        viewModelScope.launch {
            _isPregenEnabled.value = storage.isPregenEnabled()
            updateGameCacheCounts()
        }
        observeApiSettings() // 全局 Flow 动态监听 AI 平台配置与协程生命周期斩断
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

    private fun observeApiSettings() {
        viewModelScope.launch {
            settingsRepository.aiPlatformFlow.collect { platform ->
                val apiKey = settingsRepository.getAiApiKeyFlow(platform).first()
                if (platform.isBlank() || apiKey.isBlank()) {
                    _uiState.value = VerbUiState.ApiNotConfigured
                } else {
                    // API 配置正常！
                    // 1. 核心安全机制：侦测平台切换，断流旧任务并清空旧缓存！
                    val lastPlatform = storage.getLastUsedPlatform()
                    if (lastPlatform.isNotBlank() && lastPlatform != platform) {
                        // 1.1 手起刀落：瞬间斩断任何正在后台跑的旧模型出题/补货协程！
                        activeGenerationJob?.cancel()
                        // 1.2 强制将全局 Generating 状态归位 Idle，平息前台转圈竞态！
                        _globalGenStatus.value = GlobalGenStatus.Idle
                        
                        // 1.3 物理彻底擦除 5 大级别的旧缓存与时间戳
                        val levels = listOf("N5", "N4", "N3", "N2", "N1")
                        for (lvl in levels) {
                            storage.saveCachePool(lvl, emptyList())
                        }
                        storage.setLastUsedPlatform(platform)

                        // 1.4 瞬间更新大厅标徽（数据归零）
                        updateGameCacheCounts()

                        // 1.5 开启新一轮全新大模型的静默补水！
                        if (_isPregenEnabled.value) {
                            preloadCachePoolQuietly()
                        }
                    } else if (lastPlatform.isBlank()) {
                        // 首次记录生效平台
                        storage.setLastUsedPlatform(platform)
                    }

                    // 2. 恢复历史会话或置为可选关卡大厅
                    val current = _uiState.value
                    if (current is VerbUiState.Loading || current is VerbUiState.ApiNotConfigured) {
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
                            if (globalGenStatus.value is GlobalGenStatus.Generating) {
                                _uiState.value = VerbUiState.Generating
                            } else {
                                _uiState.value = VerbUiState.LevelSelecting
                            }
                        }
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
        storage.saveSession(session)
    }

    private fun saveSession(session: VerbConjugationSession) {
        storage.saveSession(session)
    }

    private fun clearSession() {
        storage.clearSession()
    }

    private suspend fun restoreSession(): VerbConjugationSession? =
        storage.restoreSession()

    fun forceRegenerate() {
        clearSession()
        onLevelSelected(currentLevel, forceRegenerate = true)
    }

    fun onLevelSelected(level: String, forceRegenerate: Boolean = false) {
        currentLevel = level

        viewModelScope.launch {
            // 1. 优先消费离线预生成的缓存池题目 (即使是重新生成，存粮够也优先从缓存池秒速提货)
            if (_isPregenEnabled.value && !forceRegenerate) {
                val activeVerbs = try {
                    wordRepository.getWordsByPartOfSpeech(PartOfSpeech.VERB)
                } catch (_: Exception) { emptyList() }
                val activeVerbStrings = activeVerbs.filter { !it.isDelisted }.map { it.japanese }.toSet()

                var pool = storage.getCachePool(level)
                while (pool.isNotEmpty()) {
                    val candidate = pool.first()
                    pool = pool.drop(1)
                    storage.saveCachePool(level, pool)

                    val isAllActive = candidate.all { it.word in activeVerbStrings }
                    if (isAllActive || activeVerbStrings.isEmpty()) {
                        val readyState = VerbUiState.Ready(questions = candidate)
                        _uiState.value = readyState
                        saveSession(readyState)
                        preloadCachePoolQuietly()
                        return@launch
                    }
                }
            }

            // --- 2. 降级逻辑/强行重刷/缓存未准备好 ---
            if (_globalGenStatus.value is GlobalGenStatus.Generating) {
                _uiState.value = VerbUiState.Generating
                return@launch
            }

            _uiState.value = VerbUiState.Generating
            _globalGenStatus.value = GlobalGenStatus.Generating

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
                        // 同时持久化存入 Storage
                        val session = VerbConjugationSession(
                            level = level,
                            questions = cached,
                            currentIndex = 0,
                            correctCount = 0,
                            userAnswers = List(cached.size) { null }
                        )
                        saveSession(session)
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
                        // 2. 静默会话持久化
                        val session = VerbConjugationSession(
                            level = level,
                            questions = questions,
                            currentIndex = 0,
                            correctCount = 0,
                            userAnswers = List(questions.size) { null }
                        )
                        saveSession(session)

                        // 出题成功后，顺便在后台补充该级别的缓存池
                        preloadCachePoolQuietly()
                    }.onFailure { e ->
                        _globalGenStatus.value = GlobalGenStatus.Error(e.message ?: "生成失败")
                    }

                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
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
                val record = TestRecord(
                    testMode = "verb_conjugation_ai",
                    totalQuestions = total,
                    correctAnswers = correct,
                    date = System.currentTimeMillis()
                )
                testRecordRepository.insertRecord(record)
            } catch (e: Exception) {
                // 仅打日志，不阻塞 UI
            }
        }
    }

    fun playWordTts() {
        val currentState = _uiState.value as? VerbUiState.Ready ?: return
        val currentQuestion = currentState.questions[currentState.currentIndex]
        audioRepository.playTts(text = currentQuestion.word)
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
        viewModelScope.launch {
            val platform = settingsRepository.aiPlatformFlow.first()
            val apiKey = settingsRepository.getAiApiKeyFlow(platform).first()
            if (platform.isBlank() || apiKey.isBlank()) {
                _uiState.value = VerbUiState.ApiNotConfigured
            } else {
                _uiState.value = VerbUiState.LevelSelecting
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRepository.stop()
        activeGenerationJob?.cancel()
        activeGenerationJob = null
    }

    private fun generateCacheKey(level: String, wordIds: List<String>): String {
        val raw = level + wordIds.sorted().joinToString()
        val bytes = MessageDigest.getInstance("MD5").digest(raw.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
