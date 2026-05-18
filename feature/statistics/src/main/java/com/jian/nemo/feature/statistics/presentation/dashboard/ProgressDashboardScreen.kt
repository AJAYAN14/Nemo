package com.jian.nemo.feature.statistics.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader


/**
 * 进度复习界面
 *
 * 结构：
 * - 学习摘要卡片（3页轮播：日间概览/学习轨迹/成长总览）
 * - 复习与训练卡片
 * - 数据与资料卡片
 */
@Composable
fun ProgressDashboardScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDueReview: () -> Unit = {},
    onNavigateToCategoryPractice: () -> Unit = {},
    onNavigateToLearningCalendar: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToHistoricalStatistics: () -> Unit = {},
    onNavigateToWordList: () -> Unit = {},
    onNavigateToGrammarList: () -> Unit = {},
    onNavigateToCategoryVocabulary: () -> Unit = {},
    onNavigateToLeechManagement: () -> Unit = {},
    onNavigateToForgettingCurve: () -> Unit = {},
    viewModel: ProgressDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.stats

    // MD3 规范：使用 MaterialTheme.colorScheme 语义化颜色
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = backgroundColor.luminance < 0.5f

    // Edge-to-Edge: 背景延伸到系统栏下方，内容可以滚动到状态栏下方
    // 获取状态栏和导航栏高度，用于 contentPadding
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor) // 背景延伸到全屏（edge-to-edge）
    ) {
        if (stats != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarHeight + 16.dp,
                    bottom = navigationBarHeight + 104.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 页面标题
                item {
                    ImmersiveDashboardHeader(title = "进度")
                }

                // 学习摘要卡片（3页轮播）
                item {
                    LearningSummaryCard(
                        progress = uiState.totalProgress,
                        masteredCount = stats.totalMastered,
                        totalWords = stats.totalWords + stats.totalGrammars,
                        todayLearned = stats.todayTotalLearned,
                        dailyGoal = stats.wordDailyGoal + stats.grammarDailyGoal,
                        unmasteredCount = (stats.totalWords + stats.totalGrammars) - stats.totalMastered,
                        studyStreak = stats.dailyStreak,
                        dueCount = stats.totalDue,
                        totalStudyDays = stats.totalStudyDays,
                        weekStudyDays = stats.weekStudyDays,
                        cardColor = cardColor
                    )
                }


                // 复习与训练部分
                item {
                    ReviewSection(
                        isDarkTheme = isDarkTheme,
                        cardColor = cardColor,
                        onDueReviewClick = onNavigateToDueReview,
                        onCategoryPracticeClick = onNavigateToCategoryPractice
                    )
                }

                // 数据与资料部分
                item {
                    DataAndResourcesSection(
                        isDarkTheme = isDarkTheme,
                        cardColor = cardColor,
                        onStatisticsClick = onNavigateToStatistics,
                        onLearningCalendarClick = onNavigateToLearningCalendar,
                        onHistoricalStatisticsClick = onNavigateToHistoricalStatistics,
                        onWordListClick = onNavigateToWordList,
                        onGrammarListClick = onNavigateToGrammarList,
                        onCategoryClassificationClick = onNavigateToCategoryVocabulary,
                        onLeechManagementClick = onNavigateToLeechManagement,
                        onForgettingCurveClick = onNavigateToForgettingCurve
                    )
                }
            }
        } else if (uiState.isLoading) {
            NemoChasingDotsLoader(modifier = Modifier.align(Alignment.Center))
        }
    }
}
private val Color.luminance: Float
    get() = (0.299f * red + 0.587f * green + 0.114f * blue)
