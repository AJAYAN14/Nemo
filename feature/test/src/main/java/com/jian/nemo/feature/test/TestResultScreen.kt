package com.jian.nemo.feature.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jian.nemo.feature.test.components.TestResultComponent
import com.jian.nemo.core.domain.model.TestResult

/**
 * 测试结果界面
*/
@Composable
fun TestResultScreen(
    result: TestResult,
    todayTestCount: Int = 0,
    todayAccuracy: Float = 0f,
    onRetakeTest: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    TestResultComponent(
        correctAnswers = result.correctCount,
        totalQuestions = result.totalQuestions,
        wrongAnswers = emptyList(), // 错题清单已移除，传递空列表
        startTimeMillis = result.startTimeMs,
        endTimeMillis = result.endTimeMs,
        actualWordCount = result.questions.count { it is com.jian.nemo.core.domain.model.TestQuestion.MultipleChoice && it.word != null || it is com.jian.nemo.core.domain.model.TestQuestion.Typing || it is com.jian.nemo.core.domain.model.TestQuestion.CardMatching || it is com.jian.nemo.core.domain.model.TestQuestion.Sorting },
        actualGrammarCount = result.questions.count { it is com.jian.nemo.core.domain.model.TestQuestion.MultipleChoice && it.grammar != null },
        todayTestCount = todayTestCount,
        todayAccuracy = todayAccuracy,
        onRetakeTest = onRetakeTest,
        onExitTest = onExit
    )
}
