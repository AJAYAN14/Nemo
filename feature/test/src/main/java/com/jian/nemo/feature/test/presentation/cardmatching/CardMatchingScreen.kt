package com.jian.nemo.feature.test.presentation.cardmatching

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jian.nemo.feature.test.TestViewModel
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.feature.test.components.TestHeaderCenterContent
import com.jian.nemo.feature.test.components.TestHeaderActions
import com.jian.nemo.feature.test.components.SimpleProgressIndicator

/**
 * 卡片题主界面
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Suppress("UnusedContentLambdaTargetStateParameter")
@Composable
fun CardMatchingScreen(
    viewModel: TestViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentQuestion = uiState.currentQuestion as? TestQuestion.CardMatching

    // 当题目索引变化时，初始化卡片
    LaunchedEffect(uiState.currentIndex) {
        currentQuestion?.let {
            viewModel.initializeCardMatchingCards(it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 头部 - 统一使用 CommonHeader（与选择题/手打题/排序题一致）
            CommonHeader(
                title = "",
                onBack = { viewModel.confirmExitTest() },
                centerContent = {
                    TestHeaderCenterContent(
                        timeLimitSeconds = uiState.timeLimitSeconds,
                        timeRemainingSeconds = uiState.timeRemainingSeconds
                    )
                },
                actions = {
                    TestHeaderActions(
                        word = null,
                        onToggleFavorite = { _, _ -> },
                        onPause = { viewModel.pauseTest() }
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 进度条
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SimpleProgressIndicator(
                    current = uiState.questions.count { it.isAnswered },
                    total = uiState.questions.size
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 卡片区域 - 分列显示 (带动画)
            AnimatedContent(
                targetState = uiState.currentIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(animationSpec = tween(300)) { width -> width } togetherWith
                                slideOutHorizontally(animationSpec = tween(300)) { width -> -width }
                    } else {
                        slideInHorizontally(animationSpec = tween(300)) { width -> -width } togetherWith
                                slideOutHorizontally(animationSpec = tween(300)) { width -> width }
                    }
                },
                label = "card_matching_transition",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) { targetIndex ->
                CardMatchingContentArea(
                    termCards = uiState.termCards,
                    definitionCards = uiState.definitionCards,
                    onCardClick = { card -> viewModel.selectCard(card) }
                )
            }
        }

        // 底部反馈面板 (固定)
        MatchingFeedbackPanel(
            feedbackState = uiState.feedbackPanelState,
            onFinish = { viewModel.finishTest() },
            onNextGroup = { viewModel.nextQuestion() },
            isLastQuestion = uiState.isLastQuestion,
            autoAdvance = uiState.isAutoAdvancing,
            wrongCount = uiState.cardMatchingWrongCount,
            wrongLimit = 3,
            isAutoAdvancing = uiState.isAutoAdvancing,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
