package com.jian.nemo.feature.learning.presentation.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader

import com.jian.nemo.feature.learning.presentation.components.srs.SRSActionArea
import com.jian.nemo.feature.learning.presentation.components.cards.SRSGrammarCard
import com.jian.nemo.feature.learning.presentation.components.cards.SRSLearningCard

@Composable
fun ReviewSessionScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.status) {
        ReviewStatus.SessionCompleted -> {
            ReviewCompletedScreen(onNavigateBack)
            return
        }
        ReviewStatus.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                NemoChasingDotsLoader()
            }
            return
        }
        else -> { /* Continue to Scaffold */ }
    }

    Scaffold(
        topBar = {
            CommonHeader(
                title = "今日复习 (${uiState.currentIndex + 1}/${uiState.reviewItems.size})",
                onBack = onNavigateBack,
                backgroundColor = MaterialTheme.colorScheme.background
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.status == ReviewStatus.Waiting) {
                com.jian.nemo.feature.learning.presentation.components.common.WaitingContent(
                    until = uiState.waitingUntil,
                    onContinue = { viewModel.resumeFromWaiting() }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. Content Area
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        uiState.currentItem?.let { item ->
                            when (item) {
                                is ReviewPreviewItem.WordItem -> {
                                    SRSLearningCard(
                                        word = item.word,
                                        isAnswerShown = uiState.isAnswerShown,
                                        modifier = Modifier.fillMaxSize(),
                                        onPracticeClick = { /* Not implemented in Mock */ }
                                    )
                                }
                                is ReviewPreviewItem.GrammarItem -> {
                                    SRSGrammarCard(
                                        grammar = item.grammar,
                                        isAnswerShown = uiState.isAnswerShown,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // 2. Action Area
                    SRSActionArea(
                        isAnswerShown = uiState.isAnswerShown,
                        ratingIntervals = uiState.ratingIntervals,
                        onShowAnswer = { viewModel.showAnswer() },
                        onRate = { viewModel.rateItem(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}


@Composable
private fun ReviewCompletedScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🎉 复习完成!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("返回")
            }
        }
    }
}
