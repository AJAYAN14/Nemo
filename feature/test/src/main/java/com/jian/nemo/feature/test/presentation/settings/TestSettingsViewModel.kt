package com.jian.nemo.feature.test.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.feature.test.domain.model.QuestionSource
import com.jian.nemo.feature.test.domain.model.TestConfig
import com.jian.nemo.feature.test.domain.model.TestContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.jian.nemo.feature.test.domain.usecase.QueryAvailableLevelsUseCase
import com.jian.nemo.feature.test.domain.usecase.QueryAvailableDataCountUseCase
import com.jian.nemo.feature.test.domain.usecase.ValidateTestConfigUseCase
import android.util.Log
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository

data class TestSettingsUiState(
    val testConfig: TestConfig = TestConfig(),
    val todayLearnedCount: Int = 0,
    val todayLearnedGrammarCount: Int = 0,
    val todayReviewedCount: Int = 0,
    val todayReviewedGrammarCount: Int = 0,
    val wrongWordsCount: Int = 0,
    val wrongGrammarsCount: Int = 0,
    val favoriteWordsCount: Int = 0,
    val favoriteGrammarsCount: Int = 0,
    val allLearnedWordsCount: Int = 0,
    val allLearnedGrammarsCount: Int = 0,
    val totalWordsCount: Int = 0,
    val totalGrammarsCount: Int = 0,
    val availableWordLevels: List<Pair<String, Int>> = emptyList(), // Level to Count
    val availableGrammarLevels: List<Pair<String, Int>> = emptyList(),

    // 可用数据量（用于判断是否有足够数据生成题目）
    val availableDataCount: Pair<Int, Int>? = null, // (单词数, 语法数)
    val isLoadingDataCount: Boolean = false,

    // 选择状态标识 (用于 UI 解耦)
    val isAllWordLevelsSelected: Boolean = false,
    val isAllGrammarLevelsSelected: Boolean = false,

    val error: String? = null,
    val messages: List<UIMessage> = emptyList() // 消息列表
) {
    fun withSyncedFlags(): TestSettingsUiState {
        val wordAvailable = availableWordLevels.map { it.first }
        val grammarAvailable = availableGrammarLevels.map { it.first }
        return copy(
            isAllWordLevelsSelected = wordAvailable.isNotEmpty() &&
                testConfig.selectedWordLevels.containsAll(wordAvailable),
            isAllGrammarLevelsSelected = grammarAvailable.isNotEmpty() &&
                testConfig.selectedGrammarLevels.containsAll(grammarAvailable)
        )
    }
}

enum class MessageType { Info, Success, Warning, Error }
enum class MessagePriority { Low, Medium, High }

data class UIMessage(
    val id: Long,
    val message: String,
    val type: MessageType = MessageType.Info,
    val priority: MessagePriority = MessagePriority.Low,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

/**
 * 测试设置 ViewModel
 */
@HiltViewModel
class TestSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val wordRepository: WordRepository,
    private val grammarRepository: com.jian.nemo.core.domain.repository.GrammarRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val grammarWrongAnswerRepository: GrammarWrongAnswerRepository,
    private val allocateQuestionTypesUseCase: com.jian.nemo.feature.test.domain.usecase.AllocateQuestionTypesUseCase,
    private val queryAvailableLevelsUseCase: QueryAvailableLevelsUseCase,
    private val queryAvailableDataCountUseCase: QueryAvailableDataCountUseCase,
    private val validateTestConfigUseCase: ValidateTestConfigUseCase
) : ViewModel() {

    // 当前测试模式 ID (用于受限模式校验)
    private var currentTestModeId: String? = null

    private val _uiState = MutableStateFlow(TestSettingsUiState())
    val uiState: StateFlow<TestSettingsUiState> = _uiState.asStateFlow()

    init {
        // loadConfig() 由 setTestModeId() 触发，不在 init 中调用
        observeTodayStats()
        observeQuestionSourceStats()
        observeAvailableDataCount()
    }

    /**
     * 响应式观察今日统计数据
     * 使用 flatMapLatest 确保当重置时间或日期变化时，自动重启对应的采集器，避免协程泄露。
     */
    private fun observeTodayStats() {
        viewModelScope.launch {
            // 每分钟触发一次刷新检查（用于检测是否跨过了重置小时）
            val tickFlow = kotlinx.coroutines.flow.flow {
                while (true) {
                    emit(Unit)
                    kotlinx.coroutines.delay(60_000L)
                }
            }

            kotlinx.coroutines.flow.combine(
                settingsRepository.learningDayResetHourFlow,
                tickFlow
            ) { resetHour, _ ->
                DateTimeUtils.getLearningDay(resetHour)
            }.collectLatest { today ->
                // 在此处启动并行采集
                kotlinx.coroutines.coroutineScope {
                    launch {
                        wordRepository.getTodayLearnedWords(today).collect { words ->
                            _uiState.update { it.copy(todayLearnedCount = words.size) }
                        }
                    }
                    launch {
                        grammarRepository.getTodayLearnedGrammars(today).collect { grammars ->
                            _uiState.update { it.copy(todayLearnedGrammarCount = grammars.size) }
                        }
                    }
                    launch {
                        wordRepository.getTodayReviewedWords(today).collect { words ->
                            _uiState.update { it.copy(todayReviewedCount = words.size) }
                        }
                    }
                    launch {
                        grammarRepository.getTodayReviewedGrammars(today).collect { grammars ->
                            _uiState.update { it.copy(todayReviewedGrammarCount = grammars.size) }
                        }
                    }
                }
            }
        }
    }

    /**
     * 响应式观察所有题源的数据量统计（错题、收藏、全部已学、总库数据）
     */
    private fun observeQuestionSourceStats() {
        viewModelScope.launch {
            // 错题统计
            launch {
                wrongAnswerRepository.getAllWrongAnswers().collect { wrongList ->
                    _uiState.update { it.copy(wrongWordsCount = wrongList.size) }
                }
            }
            launch {
                grammarWrongAnswerRepository.getAllWrongAnswers().collect { wrongList ->
                    _uiState.update { it.copy(wrongGrammarsCount = wrongList.size) }
                }
            }
            // 收藏统计
            launch {
                wordRepository.getFavoriteWords().collect { favorites ->
                    _uiState.update { it.copy(favoriteWordsCount = favorites.size) }
                }
            }
            launch {
                grammarRepository.getFavoriteGrammars().collect { favorites ->
                    _uiState.update { it.copy(favoriteGrammarsCount = favorites.size) }
                }
            }
            // 全部已学统计
            launch {
                wordRepository.getAllLearnedWords().collect { learned ->
                    _uiState.update { it.copy(allLearnedWordsCount = learned.size) }
                }
            }
            launch {
                grammarRepository.getAllLearnedGrammars().collect { learned ->
                    _uiState.update { it.copy(allLearnedGrammarsCount = learned.size) }
                }
            }
            // 总库统计
            launch {
                val levels = listOf("N5", "N4", "N3", "N2", "N1")
                val totalW = levels.sumOf { level ->
                    try {
                        wordRepository.getAllWordsByLevel(level).first().size
                    } catch (e: Exception) {
                        0
                    }
                }
                val totalG = try {
                    grammarRepository.getAllGrammars().first().size
                } catch (e: Exception) {
                    0
                }
                _uiState.update {
                    it.copy(
                        totalWordsCount = totalW,
                        totalGrammarsCount = totalG
                    )
                }
            }
        }
    }

    /**
     * 响应式监听影响数据量的配置字段变化，自动刷新可用数据量
     * 监听字段：questionSource, testContentType, selectedWordLevels, selectedGrammarLevels
     * 使用 debounce 防抖 + collectLatest 确保快速连续变更只触发一次查询
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun observeAvailableDataCount() {
        viewModelScope.launch {
            _uiState
                .map { state ->
                    val config = state.testConfig
                    listOf(
                        config.questionSource,
                        config.testContentType,
                        config.selectedWordLevels.sorted(),
                        config.selectedGrammarLevels.sorted()
                    )
                }
                .distinctUntilChanged()
                .debounce(300L)
                .collectLatest {
                    queryAvailableDataCount()
                    loadAvailableLevels()
                }
        }
    }

    /**
     * 一次性从 DataStore 加载配置到 _uiState，不持续监听。
     * 加载后执行初始化校验（等级有效性、模式约束）。
     */
    private fun loadConfig() {
        viewModelScope.launch {
            val prefs = settingsRepository.testPreferencesFlow.first()
            val config = TestConfig(
                questionCount = prefs.questionCount,
                timeLimitMinutes = prefs.timeLimitMinutes,
                shuffleQuestions = prefs.shuffleQuestions,
                shuffleOptions = prefs.shuffleOptions,
                autoAdvance = prefs.autoAdvance,
                prioritizeWrong = prefs.prioritizeWrong,
                prioritizeNew = prefs.prioritizeNew,
                questionSource = QuestionSource.fromKey(prefs.questionSource),
                wrongAnswerRemovalThreshold = prefs.wrongAnswerRemovalThreshold,
                testContentType = TestContentType.fromKey(prefs.testContentType),
                selectedWordLevels = prefs.selectedWordLevels,
                selectedGrammarLevels = prefs.selectedGrammarLevels,
                autoPlayAudio = prefs.autoPlayAudio
            )
            _uiState.update { it.copy(testConfig = config).withSyncedFlags() }

            // 一次性初始化校验
            ensureValidLevels(isInit = true)
            enforceModeConstraints(currentTestModeId, _uiState.value.testConfig)
            loadAvailableLevels()
        }
    }



    // 旧的 refreshTodayCounts 已被响应式的 observeTodayStats 替代

    fun updateConfig(newConfig: TestConfig) {
        val currentUiState = _uiState.value
        val oldConfig = currentUiState.testConfig
        val oldContentType = oldConfig.testContentType
        val newContentType = newConfig.testContentType

        var finalConfig = newConfig.copy(
            selectedWordLevels = validateAndCleanLevels(newConfig.selectedWordLevels),
            selectedGrammarLevels = validateAndCleanLevels(newConfig.selectedGrammarLevels)
        )


        // 2. 原子化更新 UI 状态
        if (currentUiState.testConfig != finalConfig) {
            _uiState.update { it.copy(testConfig = finalConfig).withSyncedFlags() }
        }

        // 3. 单次异步持久化所有配置（含综合题型计数）
        viewModelScope.launch {
            settingsRepository.saveTestConfig(
                questionCount = finalConfig.questionCount,
                timeLimitMinutes = finalConfig.timeLimitMinutes,
                shuffleQuestions = finalConfig.shuffleQuestions,
                shuffleOptions = finalConfig.shuffleOptions,
                autoAdvance = finalConfig.autoAdvance,
                prioritizeWrong = finalConfig.prioritizeWrong,
                prioritizeNew = finalConfig.prioritizeNew,
                questionSource = finalConfig.questionSource.key,
                wrongAnswerRemovalThreshold = finalConfig.wrongAnswerRemovalThreshold,
                testContentType = finalConfig.testContentType.key,
                selectedWordLevels = finalConfig.selectedWordLevels.toSet(),
                selectedGrammarLevels = finalConfig.selectedGrammarLevels.toSet(),
                autoPlayAudio = finalConfig.autoPlayAudio
            )
        }
    }

    /**
     * 切换单个等级的选中状态
     */
    fun toggleLevel(level: String, isGrammar: Boolean) {
        val currentConfig = _uiState.value.testConfig
        val currentLevels = if (isGrammar) currentConfig.selectedGrammarLevels else currentConfig.selectedWordLevels

        val nextLevels = if (currentLevels.contains(level)) {
            currentLevels - level
        } else {
            currentLevels + level
        }

        updateConfig(
            if (isGrammar) currentConfig.copy(selectedGrammarLevels = nextLevels)
            else currentConfig.copy(selectedWordLevels = nextLevels)
        )
    }

    /**
     * 确保至少选择了一个等级。如果列表为空，则自动重置为“全选”。
     * @param isInit 是否为初始化调用（如果是则根据当前可用等级重置）
     */
    fun ensureValidLevels(isInit: Boolean = false) {
        val state = _uiState.value
        val config = state.testConfig

        var updatedConfig = config

        // 校验单词等级
        if (config.selectedWordLevels.isEmpty()) {
            val availableWordKeys = state.availableWordLevels.map { it.first }
            val defaultWordLevels = if (availableWordKeys.isNotEmpty()) availableWordKeys else listOf("N5", "N4", "N3", "N2", "N1")
            updatedConfig = updatedConfig.copy(selectedWordLevels = defaultWordLevels)
        }

        // 校验语法等级
        if (config.selectedGrammarLevels.isEmpty()) {
            val availableGrammarKeys = state.availableGrammarLevels.map { it.first }
            val defaultGrammarLevels = if (availableGrammarKeys.isNotEmpty()) availableGrammarKeys else listOf("N5", "N4", "N3", "N2", "N1")
            updatedConfig = updatedConfig.copy(selectedGrammarLevels = defaultGrammarLevels)
        }

        if (updatedConfig != config) {
            if (isInit) {
                // 初始化时直接更新 UI 状态，不必再次持久化（因为持久化源已经在 collect）
                _uiState.update { it.copy(testConfig = updatedConfig).withSyncedFlags() }
            } else {
                updateConfig(updatedConfig)
            }
        }
    }

    /**
     * 智能全选/取消全选 (仅选中当前有数据的等级)
     */
    fun toggleAllLevels(isGrammar: Boolean) {
        val currentConfig = _uiState.value.testConfig
        val currentLevels = if (isGrammar) currentConfig.selectedGrammarLevels else currentConfig.selectedWordLevels
        val availableLevels = if (isGrammar) _uiState.value.availableGrammarLevels else _uiState.value.availableWordLevels
        val availableKeys = availableLevels.map { it.first }

        if (availableKeys.isEmpty()) return

        val isAllSelected = currentLevels.containsAll(availableKeys)
        val nextLevels = if (isAllSelected) emptyList() else availableKeys

        updateConfig(
            if (isGrammar) currentConfig.copy(selectedGrammarLevels = nextLevels)
            else currentConfig.copy(selectedWordLevels = nextLevels)
        )
    }

    /**
     * 仅选中此等级 (Exclusive Select)
     */
    fun exclusiveSelectLevel(level: String, isGrammar: Boolean) {
        val currentConfig = _uiState.value.testConfig
        val nextLevels = listOf(level)

        updateConfig(
            if (isGrammar) currentConfig.copy(selectedGrammarLevels = nextLevels)
            else currentConfig.copy(selectedWordLevels = nextLevels)
        )
    }

    private fun loadAvailableLevels() {
        viewModelScope.launch(Dispatchers.IO) {
            val config = _uiState.value.testConfig
            try {
                val (wordLevels, grammarLevels) = queryAvailableLevelsUseCase(
                    source = config.questionSource,
                    contentType = config.testContentType
                )

                _uiState.update {
                    it.copy(
                        availableWordLevels = wordLevels.sortedBy { p -> p.first },
                        availableGrammarLevels = grammarLevels.sortedBy { p -> p.first }
                    ).withSyncedFlags()
                }
                
                // 加载完成后执行一致性校验
                enforceLevelConsistency()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    // Messages
    fun showMessage(type: MessageType = MessageType.Info, priority: MessagePriority = MessagePriority.Low, message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        val newMessage = UIMessage(
            id = System.currentTimeMillis(),
            message = message,
            type = type,
            priority = priority,
            actionLabel = actionLabel,
            onAction = onAction
        )
        _uiState.update { it.copy(messages = it.messages + newMessage) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissMessage(id: Long) {
        _uiState.update { state ->
            state.copy(messages = state.messages.filter { it.id != id })
        }
    }

    /**
     * 查询可用数据量
     * @return Pair<单词数量, 语法数量>
     */
    suspend fun queryAvailableDataCount(): Pair<Int, Int> {
        _uiState.update { it.copy(isLoadingDataCount = true) }

        return try {
            val result = queryAvailableDataCountUseCase(_uiState.value.testConfig)
            _uiState.update { it.copy(availableDataCount = result, isLoadingDataCount = false) }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.update { it.copy(isLoadingDataCount = false) }
            Pair(0, 0)
        }
    }

    /**
     * 判断某个题型是否支持当前的测试内容类型
     * @param questionType 题型ID
     * @return 是否支持
     */
    fun isQuestionTypeSupported(questionType: String): Boolean {
        val config = _uiState.value.testConfig

        // 1. 基础类型检查
        val isTypeSupported = when (config.testContentType) {
            TestContentType.GRAMMAR -> questionType == "multiple_choice"
            else -> true
        }

        if (!isTypeSupported) return false

        // 2. 混合模式下的数据可用性检查 (仅针对需要单词的题型)
        if (config.testContentType == TestContentType.MIXED) {
            val wordDependentTypes = listOf("typing", "card_matching", "sorting")
            if (questionType in wordDependentTypes) {
                // 如果已加载数据且单词数量为0，则不支持
                val dataCount = _uiState.value.availableDataCount
                if (dataCount != null && dataCount.first == 0) {
                    return false
                }
            }
        }

        return true
    }

    /**
     * 验证和清理等级列表：过滤无效值，确保至少有一个有效等级
     */
    private fun validateAndCleanLevels(levels: List<String>): List<String> {
        val allLevels = listOf("N5", "N4", "N3", "N2", "N1")
        return levels.filter { it in allLevels }.distinct()
    }




    /**
     * 根据测试模式ID预设题型配置（仅在需要时调用，不在每次进入界面时调用）
     * @param testModeId 测试模式ID（如"typing", "multiple_choice", "comprehensive"等）
     * @param forceApply 是否强制应用预设（默认false，只在明确需要时设为true）
     */
    /**
     * 强制执行模式约束 (替代原本的 applyTestModePreset)
     * 在配置加载后自动检查，确保当前模式下的配置合法
     */
    private fun enforceModeConstraints(modeId: String?, config: TestConfig) {
        if (modeId == null) return

        var newConfig = config
        var changed = false

        // 1. 内容类型约束
        if (modeId in listOf("typing", "card_matching", "sorting")) {
            if (config.testContentType != TestContentType.WORDS) {
                newConfig = newConfig.copy(testContentType = TestContentType.WORDS)
                changed = true
            }
        }



        if (changed) {
            // 只更新本地 UI 状态，不写 DataStore（避免反馈环路）
            _uiState.update { it.copy(testConfig = newConfig).withSyncedFlags() }
        }
    }




    /**
     * 设置当前测试模式ID，并手动重新加载配置和执行校验
     */
    fun setTestModeId(id: String?) {
        if (currentTestModeId == id) return
        currentTestModeId = id
        
        // 切换 Repository 上下文后手动加载一次配置
        settingsRepository.setContextTestMode(id)
        loadConfig()
    }

    /**
     * 强制等级一致性：确保选中的等级在可用等级范围内
     * (逻辑迁移自 TestSettingsScreen)
     */
    private fun enforceLevelConsistency() {
        // 只修改本地 UI 状态，不写 DataStore（避免反馈环路）
        viewModelScope.launch {
            val state = _uiState.value
            val config = state.testConfig
            val needsRestriction = config.questionSource in listOf(
                QuestionSource.TODAY, QuestionSource.WRONG, QuestionSource.FAVORITE,
                QuestionSource.LEARNED, QuestionSource.TODAY_REVIEWED
            )

            if (!needsRestriction) return@launch

            var updatedConfig = config

            // 1. 单词等级校验
            if (state.availableWordLevels.isNotEmpty()) {
                val availableKeys = state.availableWordLevels.map { it.first }
                val validLevels = updatedConfig.selectedWordLevels.filter { it in availableKeys }
                
                if (validLevels != updatedConfig.selectedWordLevels || (validLevels.isEmpty() && availableKeys.isNotEmpty())) {
                    val newLevels = validLevels.ifEmpty { availableKeys }
                    if (newLevels.toSet() != updatedConfig.selectedWordLevels.toSet()) {
                        updatedConfig = updatedConfig.copy(selectedWordLevels = newLevels)
                    }
                }
            }

            // 2. 语法等级校验
            if (state.availableGrammarLevels.isNotEmpty()) {
                val availableKeys = state.availableGrammarLevels.map { it.first }
                val validLevels = updatedConfig.selectedGrammarLevels.filter { it in availableKeys }
                
                if (validLevels != updatedConfig.selectedGrammarLevels || (validLevels.isEmpty() && availableKeys.isNotEmpty())) {
                    val newLevels = validLevels.ifEmpty { availableKeys }
                    if (newLevels.toSet() != updatedConfig.selectedGrammarLevels.toSet()) {
                        updatedConfig = updatedConfig.copy(selectedGrammarLevels = newLevels)
                    }
                }
            }

            // 统一更新 UI 状态（不写 DataStore）
            if (updatedConfig != config) {
                _uiState.update { it.copy(testConfig = updatedConfig).withSyncedFlags() }
            }
        }
    }

}
