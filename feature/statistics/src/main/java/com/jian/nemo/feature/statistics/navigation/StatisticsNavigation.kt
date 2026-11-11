package com.jian.nemo.feature.statistics.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.jian.nemo.core.ui.navigation.NavDestination
import com.jian.nemo.feature.statistics.StatisticsScreen
import com.jian.nemo.feature.statistics.HistoricalStatisticsScreen
import com.jian.nemo.feature.statistics.presentation.dashboard.LeechManagementScreen

/**
 * 注册今日统计界面到导航图
 */
fun NavGraphBuilder.statisticsScreen(navController: NavHostController) {
    composable(route = NavDestination.STATISTICS) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = if (transition.currentState != transition.targetState) {
                        CompositingStrategy.Offscreen
                    } else {
                        CompositingStrategy.Auto
                    }
                }
        ) {
            StatisticsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToWordDetail = { wordId ->
                    navController.navigate(NavDestination.wordDetail(wordId))
                },
                onNavigateToGrammarDetail = { grammarId ->
                    navController.navigate(NavDestination.grammarDetail(grammarId))
                }
            )
        }
    }
}

/**
 * 注册历史统计界面到导航图
 */
fun NavGraphBuilder.historicalStatisticsScreen(navController: NavHostController) {
    composable(route = NavDestination.HISTORICAL_STATISTICS) {
        HistoricalStatisticsScreen(
            onBack = { navController.popBackStack() },
            onNavigateToWordDetail = { wordId ->
                navController.navigate(NavDestination.wordDetail(wordId))
            },
            onNavigateToGrammarDetail = { grammarId ->
                navController.navigate(NavDestination.grammarDetail(grammarId))
            }
        )
    }
}

/**
 * 注册到期复习统计界面到导航图
 */
fun NavGraphBuilder.leechManagementScreen(navController: NavHostController) {
    composable(route = NavDestination.LEECH_MANAGEMENT) {
        LeechManagementScreen(
            onBack = { navController.popBackStack() },
            onNavigateToWordDetail = { wordId ->
                navController.navigate(NavDestination.wordDetail(wordId))
            },
            onNavigateToGrammarDetail = { grammarId ->
                navController.navigate(NavDestination.grammarDetail(grammarId))
            }
        )
    }
}

/**
 * 导航到今日统计界面
 */
fun NavHostController.navigateToStatistics() {
    navigate(NavDestination.STATISTICS)
}

/**
 * 导航到历史统计界面
 */
fun NavHostController.navigateToHistoricalStatistics() {
    navigate(NavDestination.HISTORICAL_STATISTICS)
}

/**
 * 导航到封禁管理界面
 */
fun NavHostController.navigateToLeechManagement() {
    navigate(NavDestination.LEECH_MANAGEMENT)
}

/**
 * 注册遗忘曲线界面到导航图
 */
fun NavGraphBuilder.forgettingCurveScreen(navController: NavHostController) {
    composable(route = NavDestination.FORGETTING_CURVE) {
        com.jian.nemo.feature.statistics.presentation.curve.ForgettingCurveScreen(
            onBack = { navController.popBackStack() }
        )
    }
}

/**
 * 导航到遗忘曲线界面
 */
fun NavHostController.navigateToForgettingCurve() {
    navigate(NavDestination.FORGETTING_CURVE)
}
