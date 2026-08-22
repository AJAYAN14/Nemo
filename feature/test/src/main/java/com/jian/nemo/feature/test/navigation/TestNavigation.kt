package com.jian.nemo.feature.test.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.CompositionLocalProvider
import com.jian.nemo.core.ui.animation.LocalNavAnimatedVisibilityScope
import com.jian.nemo.core.domain.model.QuestionType
import com.jian.nemo.core.domain.model.TestMode
import com.jian.nemo.feature.test.TestScreen

/**
 * 测试导航
 */
import com.jian.nemo.core.ui.navigation.NavDestination

/**
 * 添加测试界面路由
 */
fun NavGraphBuilder.testScreen(navController: NavHostController) {
    // 测试设置界面
    composable(
        route = NavDestination.TEST_SETTINGS,
        arguments = listOf(
            navArgument("testModeId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        ),
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) { backStackEntry ->
        val testModeId = backStackEntry.arguments?.getString("testModeId")

        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            com.jian.nemo.feature.test.presentation.settings.TestSettingsScreen(
                testModeId = testModeId,
                onBack = { navController.popBackStack() },
                onNavigate = { event ->
                    when(event) {
                        is com.jian.nemo.feature.test.presentation.settings.model.TestNavigationEvent.NavigateToTest -> {
                            navController.navigateToTest(
                                level = event.level,
                                mode = event.mode,
                                questionType = event.questionType,
                                contentType = event.contentType,
                                source = event.source
                            )
                        }
                        is com.jian.nemo.feature.test.presentation.settings.model.TestNavigationEvent.NavigateToTypingTest -> {
                            navController.navigateToTypingTest(event.level)
                        }
                    }
                }
            )
        }
    }

    // 测试界面
    composable(
        route = NavDestination.TEST_EXECUTION,
        arguments = listOf(
            navArgument("level") { type = NavType.StringType },
            navArgument("mode") { type = NavType.StringType },
            navArgument("questionType") {
                type = NavType.StringType
                defaultValue = QuestionType.MULTIPLE_CHOICE.name
            },
            navArgument("contentType") {
                type = NavType.StringType
                defaultValue = "words"
            },
            navArgument("source") {
                type = NavType.StringType
                defaultValue = "today"
            }
        )
    ) { backStackEntry ->
        val level = backStackEntry.arguments?.getString("level") ?: "N5"
        val modeString = backStackEntry.arguments?.getString("mode") ?: "JP_TO_CN"
        val questionTypeString = backStackEntry.arguments?.getString("questionType") ?: "MULTIPLE_CHOICE"
        val contentType = backStackEntry.arguments?.getString("contentType") ?: "words"
        val source = backStackEntry.arguments?.getString("source") ?: "today"

        val mode = try {
            TestMode.valueOf(modeString)
        } catch (e: Exception) {
            TestMode.JP_TO_CN
        }

        val questionType = try {
            QuestionType.valueOf(questionTypeString)
        } catch (e: Exception) {
            QuestionType.MULTIPLE_CHOICE
        }

        TestScreen(
            level = level,
            mode = mode,
            questionType = questionType,
            contentType = contentType,
            source = source,
            onNavigateBack = { navController.popBackStack() }
        )
    }
}

/**
 * 导航到测试界面 (默认选择题)
 */
fun NavHostController.navigateToTest(
    level: String,
    mode: TestMode,
    questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    contentType: String = "words",
    source: String = "today"
) {
    navigate(
        NavDestination.testExecution(
            level = level,
            mode = mode.name,
            questionType = questionType.name,
            contentType = contentType,
            source = source
        )
    )
}

/**
 * 导航到打字测试
 */
fun NavHostController.navigateToTypingTest(
    level: String
) {
    navigate(
        NavDestination.testExecution(
            level = level,
            mode = TestMode.JP_TO_CN.name,
            questionType = QuestionType.TYPING.name
        )
    )
}

/**
 * 导航到测试设置界面
 * @param testModeId 可选的测试模式ID（如"typing", "multiple_choice"等），用于预设题型
 */
fun NavHostController.navigateToTestSettings(testModeId: String? = null) {
    navigate(NavDestination.testSettings(testModeId))
}
