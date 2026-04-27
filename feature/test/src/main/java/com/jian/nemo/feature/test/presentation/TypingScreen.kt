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
import com.jian.nemo.feature.test.components.SimpleProgressIndicator
import com.jian.nemo.feature.test.components.TestFooter
import com.jian.nemo.feature.test.components.TestHeader
import com.jian.nemo.feature.test.components.TypingTestContent
import com.jian.nemo.feature.test.components.UnifiedTestScreen

/**
 * 手打题界面
 * 完全复刻旧项目 com.jian.nemo.ui.screen.test.TypingScreen
 *
 * 依据：E:\AndroidProjects\Nemo\_reference\old-nemo\app\src\main\java\com\jian\nemo\ui\screen\test\TypingScreen.kt
 */
@OptIn(ExperimentalAnimationApi::class)
@Suppress("UnusedContentLambdaTargetStateParameter")
@Composable
fun TypingQuestionPage(viewModel: TestViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val question = uiState.currentQuestion as? TestQuestion.Typing ?: return

    // 使用 AnimatedContent 为手打题切换添加动画效果（复刻旧项目 L29-44）
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
        label = "typing_question_transition"
    ) { _ ->
        UnifiedTestScreen(
            headerContent = {
                TestHeader(
                    onBack = { viewModel.confirmExitTest() },
                    timeLimitSeconds = uiState.timeLimitSeconds,
                    timeRemainingSeconds = uiState.timeRemainingSeconds,
                    word = question.word,
                    onToggleFavorite = { wordId, isFavorite -> viewModel.toggleFavorite(wordId, isFavorite) },
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
                TypingTestContent(
                    question = question,
                    userInput = uiState.userTypingInput,
                    onInputChange = { viewModel.onTypingInputChange(it) }
                )
            },
            footerContent = {
                TestFooter(
                    onPrev = { viewModel.previousQuestion() },
                    onNext = { viewModel.nextQuestion() },
                    onSubmit = { viewModel.submitAnswer() },
                    onFinish = { viewModel.finishTest() },
                    canGoPrev = uiState.currentIndex > 0,
                    canSubmit = uiState.userTypingInput.trim().isNotBlank() || question.isAnswered,
                    isAnswered = question.isAnswered,
                    isLastQuestion = uiState.currentIndex == uiState.questions.size - 1,
                    submitText = "提交",
                    isAutoAdvancing = uiState.isAutoAdvancing
                )
            }
        )
    }
}
