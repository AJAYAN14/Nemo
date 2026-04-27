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

/**
 * 卡片题主界面
 *
 * 参考: 旧项目 CardMatchingPage (CardMatchingScreen.kt 行34-107)
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

    // 使用AnimatedContent为卡片题切换添加动画效果
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
        label = "card_matching_transition"
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 卡片题使用专门的布局
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(16.dp)
            ) {
                // 头部
                CardMatchingTestHeader(
                    onBack = { viewModel.confirmExitTest() },
                    timeLimitSeconds = uiState.timeLimitSeconds,
                    timeRemainingSeconds = uiState.timeRemainingSeconds,
                    onPause = { viewModel.pauseTest() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 进度条
                com.jian.nemo.feature.test.components.SimpleProgressIndicator(
                    current = uiState.questions.count { it.isAnswered },
                    total = uiState.questions.size
                )


                Spacer(modifier = Modifier.height(16.dp))

                // 卡片区域 - 分列显示
                CardMatchingContentArea(
                    termCards = uiState.termCards,
                    definitionCards = uiState.definitionCards,
                    onCardClick = { card -> viewModel.selectCard(card) }
                )
            }

            // 底部反馈面板
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
}
