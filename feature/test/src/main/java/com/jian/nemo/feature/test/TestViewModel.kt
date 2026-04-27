package com.jian.nemo.feature.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.QuestionType
import com.jian.nemo.core.domain.model.TestMode
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.domain.model.TestResult
import com.jian.nemo.core.domain.model.TestRecord
import com.jian.nemo.core.domain.usecase.test.GenerateTestQuestionsUseCase
import com.jian.nemo.core.domain.usecase.test.SubmitTestResultUseCase
import com.jian.nemo.core.domain.usecase.test.QueryLevelsUseCase
import com.jian.nemo.core.domain.usecase.test.LoadTestStatisticsUseCase
import com.jian.nemo.feature.test.domain.handler.SortingHandler
import com.jian.nemo.feature.test.domain.handler.CardMatchingHandler
import com.jian.nemo.feature.test.domain.timer.TestTimerManager
import com.jian.nemo.feature.test.domain.orchestrator.CardMatchingOrchestrator
import com.jian.nemo.feature.test.domain.orchestrator.CardMatchingNavigation
import com.jian.nemo.core.domain.service.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import com.jian.nemo.core.common.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.jian.nemo.core.common.util.DateTimeUtils
import javax.inject.Inject

/**
 * 测试ViewModel
 */
@HiltViewModel

class TestViewModel @Inject constructor(
    private val generateTestQuestionsUseCase: GenerateTestQuestionsUseCase,
    private val submitTestResultUseCase: SubmitTestResultUseCase,
    private val queryLevelsUseCase: QueryLevelsUseCase,
    private val loadTestStatisticsUseCase: LoadTestStatisticsUseCase,
    private val sortingHandler: SortingHandler,
    private val cardMatchingHandler: CardMatchingHandler,
    private val timerManager: TestTimerManager,
    private val wordRepository: com.jian.nemo.core.domain.repository.WordRepository,
    private val settingsRepository: com.jian.nemo.core.domain.repository.SettingsRepository,
    private val wrongAnswerRepository: com.jian.nemo.core.domain.repository.WrongAnswerRepository,
    private val grammarRepository: com.jian.nemo.core.domain.repository.GrammarRepository,
    private val studyRecordRepository: com.jian.nemo.core.domain.repository.StudyRecordRepository,
        // Add TestSessionManager
    private val testSessionManager: com.jian.nemo.feature.test.domain.manager.TestSessionManager,
        // Sync
    private val syncService: SyncService,
    private val cardMatchingOrchestrator: CardMatchingOrchestrator,
    private val favoriteQuestionRepository: com.jian.nemo.core.domain.repository.FavoriteQuestionRepository,
    @ApplicationScope private val externalScope: CoroutineScope
    ) : ViewModel() {

    // ... (keep existing properties) ...





    // 防重复提交标志位（复刻旧项目逻辑）
    private val isSubmitting = java.util.concurrent.atomic.AtomicBoolean(false)

    // 防止快速切换题目标志位（复刻旧项目changeQuestion防抖）
    private val isChangingQuestion = java.util.concurrent.atomic.AtomicBoolean(false)

    // 所有等级列表 (N1-N5)
    private val allLevels = listOf("N5", "N4", "N3", "N2", "N1")

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<TestEffect>()
    val effect: SharedFlow<TestEffect> = _effect.asSharedFlow()


    init {
        // 加载统计数据
        loadStatistics()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadStatistics() {
        // 基于重置时间的响应式统计 (今日次数和正确率)
        viewModelScope.launch {
            settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
                val today = DateTimeUtils.getLearningDay(resetHour)
                loadTestStatisticsUseCase.getTodayTestCountFlow(today)
            }.collect { count ->
                _uiState.update { it.copy(todayTestCount = count) }
            }
        }

        viewModelScope.launch {
            settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
                val today = DateTimeUtils.getLearningDay(resetHour)
                loadTestStatisticsUseCase.getTodayAccuracyFlow(today)
            }.collect { accuracy ->
                _uiState.update { it.copy(todayAccuracy = accuracy) }
            }
        }

        viewModelScope.launch {
            loadTestStatisticsUseCase.getWrongWordsCountFlow().collect { count ->
                _uiState.update { it.copy(wrongWordsCount = count) }
            }
        }

        viewModelScope.launch {
            loadTestStatisticsUseCase.getFavoriteWordsCountFlow().collect { count ->
                _uiState.update { it.copy(favoriteWordsCount = count) }
            }
        }

        viewModelScope.launch {
            loadTestStatisticsUseCase.getConsecutiveTestDaysFlow().collect { streak ->
                _uiState.update { it.copy(consecutiveTestDays = streak) }
            }
        }

        viewModelScope.launch {
            loadTestStatisticsUseCase.getMaxTestStreakFlow().collect { maxStreak ->
                _uiState.update { it.copy(maxTestStreak = maxStreak) }
            }
        }

        viewModelScope.launch {
            loadTestStatisticsUseCase.getTotalTestCountFlow().collect { count ->
                _uiState.update { it.copy(totalTestCount = count) }
            }
        }

        viewModelScope.launch {
            loadTestStatisticsUseCase.getOverallAccuracyFlow().collect { accuracy ->
                _uiState.update { it.copy(overallAccuracy = accuracy) }
            }
        }
    }

    /**
     * 开始测试
     */
    fun startTest(
        level: String,
        mode: TestMode,
        count: Int = -1,
        questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
        contentType: String = "words",
        source: String = "today"
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, questionType = questionType) }

            // 强制修正 contentType：手打题和卡片题不支持语法，强制转为 "words"
            val effectiveContentType = if (questionType == QuestionType.TYPING || questionType == QuestionType.CARD_MATCHING) {
                "words"
            } else {
                contentType
            }

            try {
                // 1. Check for pre-generated questions from Session Manager
                val preGeneratedQuestions = testSessionManager.getQuestions(clearAfterGet = true)

                if (preGeneratedQuestions != null && preGeneratedQuestions.isNotEmpty()) {
                    // Use pre-generated questions
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            questions = preGeneratedQuestions,
                            currentIndex = 0,
                            selectedOptionIndex = -1,
                            userTypingInput = "",
                            testStartTimeMs = System.currentTimeMillis(),
                            isTestActive = true,
                            userAnswerChars = emptyList()
                        )
                    }
                    // Start Timer
                    val timeLimitMinutes = settingsRepository.testTimeLimitMinutesFlow.first()
                    if (timeLimitMinutes > 0) {
                        startTimer(timeLimitMinutes * 60)
                    }
                    return@launch
                }

                // Determine effective count
                val effectiveCount = if (count == -1) {
                    settingsRepository.testQuestionCountFlow.first()
                } else {
                    count
                }

                // 2. Fallback: Generate if not found (e.g. process death restore or deep link)
                // Pass contentType and source to UseCase
                var typeCounts: Map<String, Int>? = null
                if (effectiveContentType == "mixed") {
                     // 读取综合测试题型分布 (From SettingsRepository)
                     // Note: We need to combine individual flows or rely on a helper if available.
                     // Since we don't have a direct "getMap" in repo, let's read individual flows.
                     val mcCount = settingsRepository.comprehensiveTestMultipleChoiceCountFlow.first()
                     val typingCount = settingsRepository.comprehensiveTestTypingCountFlow.first()
                     val cardCount = settingsRepository.comprehensiveTestCardMatchingCountFlow.first()
                     val sortingCount = settingsRepository.comprehensiveTestSortingCountFlow.first()

                     typeCounts = mapOf(
                         "multiple_choice" to mcCount,
                         "typing" to typingCount,
                         "card_matching" to cardCount,
                         "sorting" to sortingCount
                     )
                }

                val questions = generateTestQuestionsUseCase(
                    level = level,
                    mode = mode,
                    count = effectiveCount,
                    questionType = questionType,
                    contentType = effectiveContentType,
                    source = source,
                    typeCounts = typeCounts,
                    shuffleQuestions = settingsRepository.testShuffleQuestionsFlow.first(),
                    shuffleOptions = settingsRepository.testShuffleOptionsFlow.first(),
                    prioritizeWrong = settingsRepository.testPrioritizeWrongFlow.first(),
                    prioritizeNew = settingsRepository.testPrioritizeNewFlow.first()
                )

                if (questions.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "没有足够的单词来开始测试，请先学习一些单词"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            questions = questions,
                            currentIndex = 0,
                            selectedOptionIndex = -1,
                            userTypingInput = "", // 重置输入
                            testStartTimeMs = System.currentTimeMillis(),
                            isTestActive = true,  // 测试开始时激活
                            userAnswerChars = emptyList() // 重置排序题答案
                        )
                    }

                    // 获取配置以设置时间限制
                    val timeLimitMinutes = settingsRepository.testTimeLimitMinutesFlow.first()
                    if (timeLimitMinutes > 0) {
                        startTimer(timeLimitMinutes * 60)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "生成测试题目失败：${e.message}"
                    )
                }
            }
        }
    }



    // ========== 等级查询 ==========

    /**
     * 查询今日学习内容的等级分布
     */
    suspend fun queryTodayLearnedLevels(contentType: String): Pair<List<String>, List<String>> {
        return queryLevelsUseCase.queryTodayLearnedLevels(contentType)
    }

    /**
     * 查询错题的等级分布
     */
    suspend fun queryWrongAnswerLevels(contentType: String): Pair<List<String>, List<String>> {
        return queryLevelsUseCase.queryWrongAnswerLevels(contentType)
    }

    /**
     * 查询收藏的等级分布
     */
    suspend fun queryFavoriteLevels(contentType: String): Pair<List<String>, List<String>> {
        return queryLevelsUseCase.queryFavoriteLevels(contentType)
    }

    /**
     * 查询已学习内容的等级分布
     */
    suspend fun queryLearnedLevels(contentType: String): Pair<List<String>, List<String>> {
        return queryLevelsUseCase.queryLearnedLevels(contentType)
    }

    /**
     * 查询今日复习内容的等级分布
     */
    suspend fun queryTodayReviewedLevels(contentType: String): Pair<List<String>, List<String>> {
        return queryLevelsUseCase.queryTodayReviewedLevels(contentType)
    }

    /**
     * 选择选项 (仅选择题)
     */
    fun selectOption(index: Int) {
        val state = _uiState.value
        val question = state.currentQuestion as? TestQuestion.MultipleChoice ?: return

        // 已回答的题目不能再选择
        if (question.isAnswered) return

        // 验证索引有效性
        if (index < 0 || index >= question.options.size) return

        _uiState.update { it.copy(selectedOptionIndex = index) }
    }

    /**
     * 打字输入变化 (仅打字题)
     */
    fun onTypingInputChange(input: String) {
        _uiState.update { it.copy(userTypingInput = input) }
    }

    /**
     * 选择可排序字符 (仅排序题)
     *
     * 使用 SortingHandler 处理
     */
    fun selectSortableChar(char: com.jian.nemo.core.domain.model.SortableChar) {
        val state = _uiState.value
        val question = state.currentQuestion as? TestQuestion.Sorting ?: return

        val result = sortingHandler.selectChar(char, question, state.userAnswerChars)
        if (result.isDebounced) return

        // 更新题目列表
        result.updatedQuestion?.let { updatedQuestion ->
            val updatedQuestions = state.questions.toMutableList()
            updatedQuestions[state.currentIndex] = updatedQuestion

            _uiState.update {
                it.copy(
                    questions = updatedQuestions,
                    userAnswerChars = result.selectedChars
                )
            }
        }
    }

    /**
     * 取消选择可排序字符 (仅排序题)
     *
     * 使用 SortingHandler 处理
     */
    fun deselectSortableChar(char: com.jian.nemo.core.domain.model.SortableChar) {
        val state = _uiState.value
        val question = state.currentQuestion as? TestQuestion.Sorting ?: return

        val result = sortingHandler.deselectChar(char, question, state.userAnswerChars)
        if (result.isDebounced) return

        // 更新题目列表
        result.updatedQuestion?.let { updatedQuestion ->
            val updatedQuestions = state.questions.toMutableList()
            updatedQuestions[state.currentIndex] = updatedQuestion

            _uiState.update {
                it.copy(
                    questions = updatedQuestions,
                    userAnswerChars = result.selectedChars
                )
            }
        }
    }

    /**
     * 提交答案
     * 完全复刻旧项目的submitAnswer逻辑，包括防重复提交和状态保护
     *
     * 依据：E:\AndroidProjects\Nemo\_reference\old-nemo\app\src\main\java\com\jian\nemo\ui\viewmodel\logic\QuestionLogic.kt (L41-438)
     */
    fun submitAnswer() {
        // 1. 防重复提交（复刻旧项目L43）
        if (!isSubmitting.compareAndSet(false, true)) return

        val state = _uiState.value

        // 2. 检查测试是否激活（复刻旧项目L48）
        if (!state.isTestActive) {
            isSubmitting.set(false)
            return
        }

        val question = state.currentQuestion

        // 3. 状态检查（复刻旧项目L47-65）
        if (question == null || question.isAnswered) {
            isSubmitting.set(false)
            return
        }

        // 4. 答案判定
        val updatedQuestion = when (question) {
            is TestQuestion.MultipleChoice -> {
                // 验证索引有效性（复刻旧项目L75-79）
                if (state.selectedOptionIndex < 0 || state.selectedOptionIndex >= question.options.size) {
                    isSubmitting.set(false)
                    return
                }

                val selectedOption = question.options[state.selectedOptionIndex]
                val isCorrect = (selectedOption == question.correctAnswer)

                question.copy(
                    isAnswered = true,
                    isCorrect = isCorrect,
                    userAnswerIndex = state.selectedOptionIndex
                )
            }
            is TestQuestion.Typing -> {
                // 根据不同的题型验证答案（复刻旧项目QuestionLogic.kt L89-130）
                val trimmedInput = state.userTypingInput.trim()
                if (trimmedInput.isEmpty()) {
                    isSubmitting.set(false)
                    return
                }

                // 根据 questionType 判断正确答案
                val isCorrect = when (question.questionType) {
                    1 -> {
                        // 题目：释义 要求输入：假名
                        question.word.hiragana.equals(trimmedInput, ignoreCase = true)
                    }
                    2 -> {
                        // 题目：释义 要求输入：汉字
                        question.word.japanese.equals(trimmedInput, ignoreCase = true)
                    }
                    3 -> {
                        // 题目：假名 要求输入：汉字
                        question.word.japanese.equals(trimmedInput, ignoreCase = true)
                    }
                    4 -> {
                        // 题目：汉字 要求输入：假名
                        question.word.hiragana.equals(trimmedInput, ignoreCase = true)
                    }
                    5 -> {
                        // 题目：假名 要求输入：释义
                        question.word.chinese.equals(trimmedInput, ignoreCase = true)
                    }
                    6 -> {
                        // 题目：汉字 要求输入：释义
                        question.word.chinese.equals(trimmedInput, ignoreCase = true)
                    }
                    else -> {
                        // 默认情况（兼容旧数据）
                        question.word.hiragana.equals(trimmedInput, ignoreCase = true)
                    }
                }

                question.copy(
                    isAnswered = true,
                    isCorrect = isCorrect,
                    userAnswer = trimmedInput
                )
            }
            is TestQuestion.Sorting -> {
                // 排序题判题逻辑（复刻旧项目QuestionLogic.kt行134-139）
                val userAnswerText = state.userAnswerChars.joinToString("") { it.char.toString() }
                val isCorrect = userAnswerText == question.word.hiragana

                question.copy(
                    isAnswered = true,
                    isCorrect = isCorrect,
                    userAnswer = state.userAnswerChars
                )
            }
            is TestQuestion.CardMatching -> {
                // 卡片题不使用submitAnswer方法，直接返回
                isSubmitting.set(false)
                return
            }
        }

        // 5. 更新WPM (仅Typing模式)
        var newWpm = state.wpm
        var newTotalChars = state.totalCorrectCharacters

        if (question is TestQuestion.Typing && updatedQuestion.isCorrect) {
            newTotalChars += question.correctAnswer.length
            val elapsedMin = (System.currentTimeMillis() - state.testStartTimeMs) / 60000.0
            if (elapsedMin > 0.01) {
                newWpm = ((newTotalChars / 5.0) / elapsedMin).toInt()
            }
        }

        // 6. 播放音效与震动（仅错误震动）（复刻旧项目L330-343）
        viewModelScope.launch {
            if (updatedQuestion.isCorrect) {
                _effect.emit(TestEffect.PlaySound(isCorrect = true))
            } else {
                _effect.emit(TestEffect.PlaySound(isCorrect = false))
                _effect.emit(TestEffect.Vibrate)
            }
        }

        // 7. 更新题目状态
        val updatedQuestions = state.questions.toMutableList()
        updatedQuestions[state.currentIndex] = updatedQuestion

        _uiState.update {
            it.copy(
                questions = updatedQuestions,
                wpm = newWpm,
                totalCorrectCharacters = newTotalChars
            )
        }

        // 8. 自动跳转逻辑（答对且不是最后一题）（复刻旧项目L392-437）
        if (updatedQuestion.isCorrect && !state.isLastQuestion) {
            viewModelScope.launch {
                // 检查自动跳转设置
                val isAutoAdvanceEnabled = settingsRepository.testAutoAdvanceFlow.first()
                if (!isAutoAdvanceEnabled) {
                    isSubmitting.set(false)
                    return@launch
                }

                // 设置自动跳转状态
                _uiState.update { it.copy(isAutoAdvancing = true) }
                kotlinx.coroutines.delay(500)

                // 重新获取状态，避免闭包问题（复刻旧项目L398）
                val currentState = _uiState.value

                // 状态检查（复刻旧项目L401-422）
                if (currentState.showResult) {
                    isSubmitting.set(false)
                    _uiState.update { it.copy(isAutoAdvancing = false) }
                    return@launch
                }

                if (currentState.questions.isEmpty() || currentState.currentQuestion == null) {
                    isSubmitting.set(false)
                    _uiState.update { it.copy(isAutoAdvancing = false) }
                    return@launch
                }

                // 执行跳转
                if (currentState.currentIndex >= currentState.questions.size - 1) {
                    finishTest()
                } else {
                    nextQuestion()
                }

                // 重置状态
                isSubmitting.set(false)
                _uiState.update { it.copy(isAutoAdvancing = false) }
            }
        } else {
            // 没有自动跳转，立即重置提交状态（复刻旧项目L435）
            isSubmitting.set(false)
        }
    }

    /**
     * 下一题（添加防抖保护，复刻旧项目changeQuestion逻辑）
     */
    fun nextQuestion() {
        // 防抖：如果正在切换题目，忽略此次请求
        if (!isChangingQuestion.compareAndSet(false, true)) return

        val state = _uiState.value
        if (state.currentIndex < state.questions.size - 1) {
            _uiState.update { currentState ->
                // 保存当前题目草稿 (Fix: 导航导致输入丢失)
                val updatedQuestions = currentState.questions.toMutableList()
                val currentQuestion = updatedQuestions[currentState.currentIndex]
                if (currentQuestion is TestQuestion.Typing) {
                    updatedQuestions[currentState.currentIndex] = currentQuestion.copy(userAnswer = currentState.userTypingInput)
                }

                val nextIndex = currentState.currentIndex + 1
                val nextQuestion = updatedQuestions[nextIndex]
                val nextUserAnswerChars = (nextQuestion as? TestQuestion.Sorting)?.userAnswer ?: emptyList()

                currentState.copy(
                    questions = updatedQuestions,
                    currentIndex = nextIndex,
                    selectedOptionIndex = -1,
                    userTypingInput = (nextQuestion as? TestQuestion.Typing)?.userAnswer ?: "",
                    userAnswerChars = nextUserAnswerChars
                )
            }
        }

        // 延迟释放防抖标志，确保动画完成
        viewModelScope.launch {
            kotlinx.coroutines.delay(300) // 等待切换动画完成
            isChangingQuestion.set(false)
        }
    }

    /**
     * 上一题（添加防抖保护，复刻旧项目changeQuestion逻辑）
     */
    fun previousQuestion() {
        // 防抖：如果正在切换题目，忽略此次请求
        if (!isChangingQuestion.compareAndSet(false, true)) return

        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.update { currentState ->
                // 保存当前题目草稿 (Fix: 导航导致输入丢失)
                val updatedQuestions = currentState.questions.toMutableList()
                val currentQuestion = updatedQuestions[currentState.currentIndex]
                if (currentQuestion is TestQuestion.Typing) {
                    updatedQuestions[currentState.currentIndex] = currentQuestion.copy(userAnswer = currentState.userTypingInput)
                }

                val prevIndex = currentState.currentIndex - 1
                val prevQuestion = updatedQuestions[prevIndex]
                val prevUserAnswerChars = (prevQuestion as? TestQuestion.Sorting)?.userAnswer ?: emptyList()

                currentState.copy(
                    questions = updatedQuestions,
                    currentIndex = prevIndex,
                    selectedOptionIndex = -1,
                    userTypingInput = (prevQuestion as? TestQuestion.Typing)?.userAnswer ?: "",
                    userAnswerChars = prevUserAnswerChars
                )
            }
        }

        // 延迟释放防抖标志，确保动画完成
        viewModelScope.launch {
            kotlinx.coroutines.delay(300) // 等待切换动画完成
            isChangingQuestion.set(false)
        }
    }

    /**
     * 完成测试
     */
    fun finishTest() {
        val state = _uiState.value
        val endTimeMs = System.currentTimeMillis()

        // 创建测试结果
        val result = TestResult.create(
            questions = state.questions,
            startTimeMs = state.testStartTimeMs,
            endTimeMs = endTimeMs
        )

        // 提交错题
        viewModelScope.launch {
            try {
                submitTestResultUseCase(result)

                // 保存测试记录
                val resetHour = settingsRepository.learningDayResetHourFlow.first()
                val today = com.jian.nemo.core.common.util.DateTimeUtils.getLearningDay(resetHour)
                val record = TestRecord(
                    date = today,
                    totalQuestions = result.totalQuestions,
                    correctAnswers = result.correctCount,
                    testMode = when(state.questionType) {
                        QuestionType.MULTIPLE_CHOICE -> "multiple_choice"
                        QuestionType.TYPING -> "typing"
                        QuestionType.CARD_MATCHING -> "card_matching"
                        QuestionType.SORTING -> "sorting"
                        else -> "unknown"
                    }
                )
                wordRepository.saveTestRecord(record)

                // 更新连续测试天数 (复刻旧项目)
                settingsRepository.updateTestStreak()

                // 更新学习记录中的测试次数 (复刻旧项目)
                studyRecordRepository.incrementTestCount(1)

                // 触发自动同步
                syncService.onTestCompleted()

            } catch (e: Exception) {
                // 记录错题失败不影响显示结果
                e.printStackTrace()
            }
        }

        // 显示结果
        _uiState.update {
            it.copy(
                showResult = true,
                testResult = result
            )
        }
    }

    /**
     * 重新测试
     */
    fun retakeTest() {
        val state = _uiState.value
        if (state.questions.isEmpty()) return

        // 重置当前题目状态 - 需要根据类型分别处理 copy
        val resetQuestions = state.questions.map { question ->
            when (question) {
                is TestQuestion.MultipleChoice -> question.copy(
                    isAnswered = false,
                    isCorrect = false,
                    userAnswerIndex = null
                )
                is TestQuestion.Typing -> question.copy(
                    isAnswered = false,
                    isCorrect = false,
                    userAnswer = ""
                )
                is TestQuestion.CardMatching -> question.copy(
                    isAnswered = false,
                    isCorrect = false
                )
                is TestQuestion.Sorting -> question.copy(
                    isAnswered = false,
                    isCorrect = false,
                    userAnswer = emptyList()
                )
            }
        }.shuffled()  // 打乱题目顺序

        _uiState.update {
            it.copy(
                questions = resetQuestions,
                currentIndex = 0,
                selectedOptionIndex = -1,
                userTypingInput = "",
                showResult = false,
                testResult = null,
                testStartTimeMs = System.currentTimeMillis(),
                isTestActive = true, // 测试开始时激活（复刻旧项目）
                userAnswerChars = emptyList() // 重置排序题答案
            )
        }
    }

    /**
     * 清除错误信息
     */


    /**
     * 切换单词收藏状态
     */
    /**
     * 切换收藏状态 (处理单词和语法)
     */
    fun toggleFavorite(itemId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            val currentQ = _uiState.value.currentQuestion
            if (currentQ != null) {
                if (currentQ.word?.id == itemId) {
                     // 单词收藏：保持不变
                     wordRepository.updateFavoriteStatus(itemId, isFavorite)
                } else if (currentQ.grammar?.id == itemId) {
                     // 语法题目收藏：保存/删除整道题的快照
                     if (isFavorite) {
                         val mcQuestion = currentQ as? TestQuestion.MultipleChoice
                         if (mcQuestion != null) {
                             val favoriteQ = com.jian.nemo.core.domain.model.FavoriteQuestion(
                                 grammarId = mcQuestion.grammar?.id,
                                 questionType = "multiple_choice",
                                 questionText = mcQuestion.questionText,
                                 options = mcQuestion.options,
                                 correctAnswer = mcQuestion.correctAnswer,
                                 explanation = mcQuestion.explanation
                             )
                             favoriteQuestionRepository.insertFavoriteQuestion(favoriteQ)
                         }
                     } else {
                         favoriteQuestionRepository.removeFavorite(
                             grammarId = itemId, jsonId = null
                         )
                     }
                }
            }

            // 更新当前题目中的状态以即时刷新UI
            val state = _uiState.value
            val updatedQuestions = state.questions.map { question ->
                if (question.word?.id == itemId) {
                    // Update Word
                    val updatedWord = question.word!!.copy(isFavorite = isFavorite)
                    when (question) {
                        is TestQuestion.MultipleChoice -> question.copy(word = updatedWord)
                        is TestQuestion.Typing -> question.copy(word = updatedWord)
                        is TestQuestion.CardMatching -> question
                        is TestQuestion.Sorting -> question.copy(word = updatedWord)
                    }
                } else if (question.grammar?.id == itemId) {
                    val updatedGrammar = question.grammar!!.copy(isFavorite = isFavorite)
                    when (question) {
                        is TestQuestion.MultipleChoice -> question.copy(grammar = updatedGrammar)
                        else -> question
                    }
                } else {
                    question
                }
            }

            _uiState.update { it.copy(questions = updatedQuestions) }
        }
    }

    /**
     * 暂停测试
     */
    fun pauseTest() {
        if (_uiState.value.showResult || !_uiState.value.isTestActive) return
        _uiState.update { it.copy(showPauseDialog = true) }
        timerManager.pause()
    }

    /**
     * 恢复测试
     */
    fun resumeTest() {
        _uiState.update { it.copy(showPauseDialog = false) }
        // 恢复计时器
        val timeLimit = _uiState.value.timeLimitSeconds
        if (timeLimit > 0) {
            timerManager.resume(
                scope = viewModelScope,
                onTimeUp = {
                    if (!_uiState.value.showResult) {
                        finishTest()
                    }
                }
            )
        }
    }

    /**
     * 显示退出确认对话框（复刻旧项目TestScreen.kt L107-109）
     */
    fun confirmExitTest() {
        _uiState.update { it.copy(showExitConfirmation = true) }
    }

    /**
     * 取消退出，关闭对话框（复刻旧项目TestScreen.kt L98）
     */
    fun cancelExitTest() {
        _uiState.update { it.copy(showExitConfirmation = false) }
    }

    /**
     * 实际退出测试，停止计时器
     */
    fun actualExitTest() {
        stopTimer()
        cardMatchingOrchestrator.cancelCurrentJob()

        // 中途退出时，保存已回答的题目中的错题和正确记录
        val state = _uiState.value
        // 筛选出已回答的题目
        val answeredQuestions = state.questions.filter { it.isAnswered }

        if (answeredQuestions.isNotEmpty()) {
            externalScope.launch {
                // 使用 NonCancellable 确保即使 ViewModel 被销毁（界面退出），保存操作也能完成
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    try {
                        val endTimeMs = System.currentTimeMillis()
                        // 创建临时结果只包含已答题目
                        val partialResult = TestResult.create(
                            questions = answeredQuestions,
                            startTimeMs = state.testStartTimeMs,
                            endTimeMs = endTimeMs
                        )

                        // 提交结果到错题本（不保存 TestRecord 统计，只更新错题状态）
                        submitTestResultUseCase(partialResult)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // ========== 卡片题相关方法 (参考旧项目 TestViewModel.kt 行420-651) ==========

    /**
     * 选择卡片
     * 使用 CardMatchingHandler 处理
     */
    fun selectCard(card: com.jian.nemo.core.domain.model.MatchableCard) {
        cardMatchingOrchestrator.onCardSelected(
            scope = viewModelScope,
            card = card,
            stateProvider = { _uiState.value },
            stateUpdater = { reducer -> _uiState.update(reducer) },
            effectEmitter = { _effect.emit(it) },
            navigation = object : CardMatchingNavigation {
                override fun nextQuestion() {
                    this@TestViewModel.nextQuestion()
                }

                override fun finishTest() {
                    this@TestViewModel.finishTest()
                }
            }
        )
    }

    /**
     * 处理卡片配对
     * 参考: 旧项目 TestViewModel.kt 行465-487
     */
    // Logic delegated to CardMatchingOrchestrator

    /**
     * 初始化卡片题的卡片
     * 使用 CardMatchingHandler 处理
     */
    fun initializeCardMatchingCards(question: TestQuestion.CardMatching) {
        val result = cardMatchingHandler.initializeCards(question)

        _uiState.update {
            it.copy(
                termCards = result.termCards,
                definitionCards = result.definitionCards,
                selectedCard = null,
                isBoardLocked = false,
                matchedPairsCount = 0,
                cardMatchingWrongCount = 0,
                feedbackPanelState = com.jian.nemo.core.domain.model.FeedbackPanelState.HIDDEN
            )
        }
    }


    private fun startTimer(seconds: Int) {
        stopTimer()
        _uiState.update {
            it.copy(
                timeLimitSeconds = seconds,
                timeRemainingSeconds = seconds
            )
        }

        // 使用 TestTimerManager 处理计时
        timerManager.start(
            scope = viewModelScope,
            seconds = seconds,
            onTimeUp = {
                // 时间到，自动结束
                if (!_uiState.value.showResult) {
                    finishTest()
                }
            }
        )

        // 监听计时器状态更新 UI
        viewModelScope.launch {
            timerManager.timerState.collect { timerState ->
                _uiState.update {
                    it.copy(timeRemainingSeconds = timerState.timeRemainingSeconds)
                }
            }
        }
    }

    private fun stopTimer() {
        timerManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        timerManager.reset()
        cardMatchingOrchestrator.cancelCurrentJob()
    }
}

/**
 * 测试UI状态
 */
data class TestUiState(
    val isLoading: Boolean = false,
    val questions: List<TestQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int = -1,
    val userTypingInput: String = "", // 打字题输入
    val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE, // 当前题目类型
    val showResult: Boolean = false,
    val testResult: TestResult? = null,
    val error: String? = null,
    val testStartTimeMs: Long = 0L,
    val wpm: Int = 0,
    val isAutoAdvancing: Boolean = false,  // 是否正在自动跳转（复刻旧项目状态）
    val isTestActive: Boolean = false,     // 测试是否激活（复刻旧项目状态）
    val showExitConfirmation: Boolean = false,  // 是否显示退出确认对话框（复刻旧项目）
    val showPauseDialog: Boolean = false,  // 是否显示暂停对话框

    // 统计数据
    val todayTestCount: Int = 0,
    val todayAccuracy: Float = 0f,
    val wrongWordsCount: Int = 0,
    val favoriteWordsCount: Int = 0,

    // 轮播统计数据 (复刻旧项目)
    val consecutiveTestDays: Int = 0,
    val totalTestCount: Int = 0,
    val overallAccuracy: Float = 0f,
    val maxTestStreak: Int = 0,

    val totalCorrectCharacters: Int = 0,
    val timeLimitSeconds: Int = 0, // 0表示无限制
    val timeRemainingSeconds: Int = 0,

    // 卡片题相关状态 (参考旧项目 TestModels.kt 行193-246)
    val termCards: List<com.jian.nemo.core.domain.model.MatchableCard> = emptyList(),       // 左列卡片(日文/假名)
    val definitionCards: List<com.jian.nemo.core.domain.model.MatchableCard> = emptyList(), // 右列卡片(中文)
    val selectedCard: com.jian.nemo.core.domain.model.MatchableCard? = null,                // 当前选中的卡片
    val isBoardLocked: Boolean = false,                     // 是否锁定面板，防止重复点击
    val matchedPairsCount: Int = 0,                         // 已匹配的卡片对数(0-5)
    val cardMatchingWrongCount: Int = 0,                    // 错误次数(0-3)
    val feedbackPanelState: com.jian.nemo.core.domain.model.FeedbackPanelState =
        com.jian.nemo.core.domain.model.FeedbackPanelState.HIDDEN,

    // 排序题相关状态 (参考旧项目 TestModels.kt + QuestionLogic.kt)
    val userAnswerChars: List<com.jian.nemo.core.domain.model.SortableChar> = emptyList()  // 排序题用户答案

) {
    /** 当前题目 */
    val currentQuestion: TestQuestion?
        get() = questions.getOrNull(currentIndex)

    /** 是否是最后一题 */
    val isLastQuestion: Boolean
        get() = currentIndex == questions.size - 1

    /** 进度文本 */
    val progressText: String
        get() = "${currentIndex + 1}/${questions.size}"
}

/**
 * UI Side Effects
 */
sealed class TestEffect {
    data class PlaySound(val isCorrect: Boolean) : TestEffect()
    data object Vibrate : TestEffect()
}
