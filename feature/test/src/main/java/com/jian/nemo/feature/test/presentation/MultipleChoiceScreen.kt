package com.jian.nemo.feature.test.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.feature.test.TestViewModel
import com.jian.nemo.feature.test.components.MultipleChoiceTestContent
import com.jian.nemo.feature.test.components.SimpleProgressIndicator
import com.jian.nemo.feature.test.components.TestFooter
import com.jian.nemo.feature.test.components.TestHeader
import com.jian.nemo.feature.test.components.UnifiedTestScreen

/**
 * 选择题界面
 * 完全复刻旧项目 com.jian.nemo.ui.screen.test.MultipleChoiceScreen
 *
 * 依据：E:\AndroidProjects\Nemo\_reference\old-nemo\app\src\main\java\com\jian\nemo\ui\screen\test\MultipleChoiceScreen.kt
 */
@OptIn(ExperimentalAnimationApi::class)
@Suppress("UnusedContentLambdaTargetStateParameter")
@Composable
fun MultipleChoiceQuestionPage(viewModel: TestViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val currentQuestion = uiState.currentQuestion as? TestQuestion.MultipleChoice ?: return

    // 使用 AnimatedContent 为选择题切换添加动画效果（复刻旧项目 L41-55）
    AnimatedContent(
        targetState = uiState.currentIndex,
        transitionSpec = {
            // 根据题目索引的变化方向来决定动画方向
            if (targetState > initialState) {
                // 下一题：从右侧滑入
                slideInHorizontally(animationSpec = tween(300)) { width -> width } togetherWith
                        slideOutHorizontally(animationSpec = tween(300)) { width -> -width }
            } else {
                // 上一题：从左侧滑入
                slideInHorizontally(animationSpec = tween(300)) { width -> -width } togetherWith
                        slideOutHorizontally(animationSpec = tween(300)) { width -> width }
            }
        },
        label = "multiple_choice_transition"
    ) { _ ->
        UnifiedTestScreen(
            headerContent = {
                TestHeader(
                    onBack = { viewModel.confirmExitTest() },
                    timeLimitSeconds = uiState.timeLimitSeconds,
                    timeRemainingSeconds = uiState.timeRemainingSeconds,
                    word = currentQuestion.word,
                    grammar = currentQuestion.grammar,
                    onToggleFavorite = { itemId, isFavorite -> viewModel.toggleFavorite(itemId, isFavorite) },
                    onPause = { viewModel.pauseTest() }
                )
            },
            progressContent = {
                SimpleProgressIndicator(
                    current = uiState.questions.count { it.isAnswered },
                    total = uiState.questions.size
                )
            },
            testContent = {
                MultipleChoiceTestContent(
                    question = currentQuestion,
                    selectedOptionIndex = uiState.selectedOptionIndex,
                    onOptionSelect = { index -> viewModel.selectOption(index) }
                )
            },
            footerContent = {
                TestFooter(
                    onPrev = { viewModel.previousQuestion() },
                    onNext = { viewModel.nextQuestion() },
                    onSubmit = { viewModel.submitAnswer() },
                    onFinish = { viewModel.finishTest() },
                    canGoPrev = uiState.currentIndex > 0,
                    // 未回答时必须选择选项才能提交，已回答后可以提交（用于下一题按钮）（复刻旧项目 L93-96）
                    canSubmit = if (!currentQuestion.isAnswered) {
                        uiState.selectedOptionIndex >= 0 && uiState.selectedOptionIndex < currentQuestion.options.size
                    } else {
                        true
                    },
                    isAnswered = currentQuestion.isAnswered,
                    isLastQuestion = uiState.currentIndex == uiState.questions.size - 1,
                    submitText = "提交",
                    isAutoAdvancing = uiState.isAutoAdvancing
                )
            }
        )
    }
}
