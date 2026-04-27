package com.jian.nemo.feature.test.presentation.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.model.QuestionType
import com.jian.nemo.core.domain.model.TestMode
import com.jian.nemo.core.domain.usecase.test.GenerateTestQuestionsUseCase
import com.jian.nemo.feature.test.domain.manager.TestSessionManager
import com.jian.nemo.feature.test.domain.model.TestConfig
import com.jian.nemo.feature.test.domain.model.TestContentType
import com.jian.nemo.feature.test.domain.usecase.ValidateTestConfigUseCase
import com.jian.nemo.feature.test.presentation.settings.model.TestNavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestStarterViewModel @Inject constructor(
    private val generateTestQuestionsUseCase: GenerateTestQuestionsUseCase,
    private val generateAdaptiveTestUseCase: com.jian.nemo.feature.test.domain.usecase.GenerateAdaptiveTestUseCase,
    private val testSessionManager: TestSessionManager,
    private val validateTestConfigUseCase: ValidateTestConfigUseCase
) : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<TestNavigationEvent>()
    val navigationEvent: SharedFlow<TestNavigationEvent> = _navigationEvent.asSharedFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    /**
     * 开始测试
     * @param config 测试配置
     * @param mode 测试模式
     * @param forcedQuestionType 强制指定的题型（如果有）
     */
    fun startTest(
        config: TestConfig,
        mode: TestMode,
        forcedQuestionType: QuestionType? = null
    ) {
        viewModelScope.launch {
            // 1. 校验配置
            val error = validateTestConfigUseCase(config)
            if (error != null) {
                _errorEvent.emit(error)
                return@launch
            }

            // 2. 设置加载状态
            _isGenerating.update { true }

            try {
                // 3. 准备参数
                val level = if (config.testContentType == TestContentType.GRAMMAR) {
                    config.selectedGrammarLevels.joinToString(",")
                } else {
                    config.selectedWordLevels.joinToString(",")
                }

                // 确定题型：优先使用强制指定的题型，否则默认为选择题
                val questionType = forcedQuestionType ?: QuestionType.MULTIPLE_CHOICE

                // 强制修正 contentType：手打题和卡片题不支持语法，强制转为 "words"
                val effectiveContentType = if (questionType == QuestionType.TYPING || questionType == QuestionType.CARD_MATCHING) {
                    "words"
                } else {
                    config.testContentType.key
                }

                // 准备综合测试题型计数
                val typeCounts = if (config.comprehensiveQuestionCounts.values.sum() > 0 && forcedQuestionType == null) {
                    config.comprehensiveQuestionCounts
                } else {
                    null
                }

                // 4. 生成题目
                // 如果没有强制指定题型，使用自适应测试（综合模式）
                val useAdaptive = forcedQuestionType == null

                val questions = if (useAdaptive) {
                    generateAdaptiveTestUseCase(
                        count = config.questionCount,
                        levels = level.split(",").map{it.trim()}.toSet(),
                        mode = mode,
                        contentType = effectiveContentType
                    )
                } else {
                    generateTestQuestionsUseCase(
                        level = level,
                        mode = mode,
                        count = config.questionCount,
                        questionType = questionType,
                        contentType = effectiveContentType,
                        source = config.questionSource.key,
                        typeCounts = typeCounts,
                        shuffleQuestions = config.shuffleQuestions,
                        shuffleOptions = config.shuffleOptions,
                        prioritizeWrong = config.prioritizeWrong,
                        prioritizeNew = config.prioritizeNew
                    )
                }

                if (questions.isEmpty()) {
                    _isGenerating.update { false }
                    _errorEvent.emit("没有生成任何题目，请检查题目来源或等级设置")
                } else {
                    // 5. 保存题目并跳转
                    testSessionManager.setQuestions(questions)
                    _navigationEvent.emit(
                        TestNavigationEvent.NavigateToTest(
                            level = level,
                            mode = mode,
                            questionType = questionType,
                            contentType = effectiveContentType,
                            source = config.questionSource.key
                        )
                    )
                    _isGenerating.update { false }
                }

            } catch (e: Exception) {
                _isGenerating.update { false }
                _errorEvent.emit("生成题目失败: ${e.message}")
                e.printStackTrace()
                Log.e("TestStarterViewModel", "Error generating test", e)
            }
        }
    }

    // specific start methods
    fun startTypingTest(config: TestConfig) { startTest(config, TestMode.JP_TO_CN, QuestionType.TYPING) }
    fun startMatchingTest(config: TestConfig) { startTest(config, TestMode.JP_TO_CN, QuestionType.CARD_MATCHING) }
    fun startSortingTest(config: TestConfig) { startTest(config, TestMode.JP_TO_CN, QuestionType.SORTING) }
    fun startMultipleChoiceTest(config: TestConfig) { startTest(config, TestMode.RANDOM, QuestionType.MULTIPLE_CHOICE) }

    fun cancelGeneration() {
        _isGenerating.update { false }
    }
}
