package com.jian.nemo.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.navigation.NavDestination
import com.jian.nemo.core.ui.component.splash.SplashScreen
import com.jian.nemo.feature.user.AuthViewModel
import com.jian.nemo.core.ui.animation.NemoNavigationAnimations


import com.jian.nemo.feature.learning.presentation.home.HomeScreen


import com.jian.nemo.feature.settings.SettingsScreen
import com.jian.nemo.feature.settings.TtsSettingsScreen
import com.jian.nemo.feature.settings.ThemeSettingsScreen

import com.jian.nemo.feature.learning.presentation.LearningScreen
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.kana.KanaChartScreen
import com.jian.nemo.feature.learning.presentation.review.ReviewScreen
import com.jian.nemo.feature.collection.navigation.favoritesScreen
import com.jian.nemo.feature.collection.navigation.mistakesScreen
import com.jian.nemo.feature.collection.navigation.wrongWordsScreen
import com.jian.nemo.feature.collection.navigation.wrongGrammarsScreen
import com.jian.nemo.feature.collection.navigation.WRONG_WORDS_ROUTE
import com.jian.nemo.feature.collection.navigation.WRONG_GRAMMARS_ROUTE
import com.jian.nemo.feature.collection.navigation.favoriteWordsScreen
import com.jian.nemo.feature.collection.navigation.favoriteGrammarsScreen
import com.jian.nemo.feature.collection.navigation.favoriteQuestionsScreen
import com.jian.nemo.feature.collection.navigation.FAVORITE_WORDS_ROUTE
import com.jian.nemo.feature.collection.navigation.FAVORITE_QUESTIONS_ROUTE
import com.jian.nemo.feature.statistics.navigation.statisticsScreen
import com.jian.nemo.feature.statistics.navigation.historicalStatisticsScreen
import com.jian.nemo.feature.statistics.navigation.leechManagementScreen
import com.jian.nemo.feature.test.navigation.testScreen
import com.jian.nemo.feature.test.navigation.navigateToTestSettings
import com.jian.nemo.feature.user.navigation.userGraph
import com.jian.nemo.feature.user.navigation.ROUTE_LOGIN

// 为底部导航优化的交叉淡入淡出动画
object BottomNavTransition {
    private const val TRANSITION_DURATION = 300

    fun enterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = LinearOutSlowInEasing
            )
        )
    }

    fun exitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = tween(
                durationMillis = TRANSITION_DURATION,
                easing = FastOutLinearInEasing
            )
        )
    }
}

// 基于 Tab 顺序的左右滑动动画
object HorizontalNavTransition {
    private const val ANIMATION_DURATION = 300

    // 获取路由对应的 Tab 索引，用于判断滑动方向
    private fun getTabIndex(route: String?): Int {
        return when (route) {
            NavDestination.LEARNING -> 0
            NavDestination.PROGRESS -> 1
            NavDestination.TEST -> 2
            NavDestination.SETTINGS -> 3
            else -> -1
        }
    }

    fun enterTransition(initialRoute: String?, targetRoute: String?): EnterTransition? {
        val fromIndex = getTabIndex(initialRoute)
        val toIndex = getTabIndex(targetRoute)

        // 只有两个页面都是主 Tab 时才使用左右滑动
        if (fromIndex != -1 && toIndex != -1) {
            return if (toIndex > fromIndex) {
                // 目标在右边 -> 向左滑动（新页面从右侧进入）
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                )
            } else {
                // 目标在左边 -> 向右滑动（新页面从左侧进入）
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                )
            }
        }
        return null // 对于非主 Tab 切换，返回 null 以便使用默认动画
    }

    fun exitTransition(initialRoute: String?, targetRoute: String?): ExitTransition? {
        val fromIndex = getTabIndex(initialRoute)
        val toIndex = getTabIndex(targetRoute)

        if (fromIndex != -1 && toIndex != -1) {
            return if (toIndex > fromIndex) {
                // 目标在右边 -> 向左滑动（旧页面向左侧退出）
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                )
            } else {
                // 目标在左边 -> 向右滑动（旧页面向右侧退出）
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(ANIMATION_DURATION, easing = FastOutSlowInEasing)
                )
            }
        }
        return null
    }
}

/**
 * Nemo应用导航配置
 *
 * 更新: 集成新的 feature:learning 模块
 * 更新: 移植旧版导航动画 (BottomNavTransition + NemoNavigationAnimations)
 */
@Composable
fun NemoNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
    onCheckUpdate: () -> Unit = {}
) {
    // 主界面路由白名单（使用底部导航栏的页面）
    val mainScreenRoutes = remember {
        setOf(
            NavDestination.LEARNING,
            NavDestination.PROGRESS,
            NavDestination.TEST,
            NavDestination.SETTINGS,
            NavDestination.LIBRARY
        )
    }

    NavHost(
        navController = navController,
        startDestination = "splash",  // 启动时先显示启动屏
        modifier = modifier,
        // 设置默认动画为二级页面转场动画 (推箱子效果)
        enterTransition = { NemoNavigationAnimations.enterTransition() },
        exitTransition = { NemoNavigationAnimations.exitTransition() },
        popEnterTransition = { NemoNavigationAnimations.popEnterTransition() },
        popExitTransition = { NemoNavigationAnimations.popExitTransition() }
    ) {
        // 启动屏 (使用缩放并淡出动画)
        composable(
            route = "splash",
            exitTransition = {
                fadeOut(animationSpec = tween(600)) +
                scaleOut(
                    targetScale = 1.2f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            }
        ) {

            val authUiState by authViewModel.uiState.collectAsState()

            SplashScreen(
                isAuthReady = authUiState.isAuthChecked,
                onTimeout = {
                    // 根据登录状态分配起始页面
                    val isLoggedIn = authUiState.isLoggedIn
                    if (isLoggedIn) {
                        navController.navigate(NavDestination.LEARNING) {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate(ROUTE_LOGIN) {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        // 学习页（原首页） - 主界面 (重构为 HomeScreen)
        composable(
            route = NavDestination.LEARNING,
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.enterTransition(initialRoute, targetRoute) ?:
                if (initialRoute !in mainScreenRoutes) NemoNavigationAnimations.popEnterTransition()
                else BottomNavTransition.enterTransition()
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
                if (targetRoute !in mainScreenRoutes) NemoNavigationAnimations.exitTransition()
                else BottomNavTransition.exitTransition()
            },
            popEnterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.enterTransition(initialRoute, targetRoute) ?:
                if (initialRoute !in mainScreenRoutes) NemoNavigationAnimations.popEnterTransition()
                else BottomNavTransition.enterTransition()
            },
            popExitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
                BottomNavTransition.exitTransition()
            }
        ) {
            HomeScreen(
                onNavigateToLearning = { level, mode ->
                    when (mode) {
                        LearningMode.Word -> navController.navigate(NavDestination.wordLearning(level))
                        LearningMode.Grammar -> navController.navigate(NavDestination.grammarLearning(level))
                    }
                },
                onNavigateToKanaChart = {
                    navController.navigate(NavDestination.KANA_CHART)
                },
                onNavigateToGrammarList = {
                    navController.navigate(NavDestination.GRAMMAR_LIST)
                },
                onNavigateToHeatmap = {
                    navController.navigate(NavDestination.ACTIVITY_HEATMAP)
                },
                onNavigateToProfile = {
                    navController.navigate(NavDestination.PROFILE)
                }
            )
        }

        // 进度页 - 主界面
        composable(
            route = NavDestination.PROGRESS,
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.enterTransition(initialRoute, targetRoute) ?:
                if (initialRoute !in mainScreenRoutes) NemoNavigationAnimations.popEnterTransition()
                else BottomNavTransition.enterTransition()
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
                if (targetRoute !in mainScreenRoutes) NemoNavigationAnimations.exitTransition()
                else BottomNavTransition.exitTransition()
            },
            popEnterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.enterTransition(initialRoute, targetRoute) ?:
                if (initialRoute !in mainScreenRoutes) NemoNavigationAnimations.popEnterTransition()
                else BottomNavTransition.enterTransition()
            },
            popExitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
                BottomNavTransition.exitTransition()
            }
        ) {
            com.jian.nemo.feature.statistics.presentation.dashboard.ProgressDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDueReview = {
                    navController.navigate(NavDestination.DUE_REVIEW)
                },
                onNavigateToCategoryPractice = {
                    navController.navigate(NavDestination.categoryClassification("practice"))
                },
                onNavigateToLearningCalendar = {
                    navController.navigate(NavDestination.LEARNING_CALENDAR)
                },
                onNavigateToStatistics = {
                    navController.navigate(NavDestination.STATISTICS)
                },
                onNavigateToHistoricalStatistics = {
                    navController.navigate(NavDestination.HISTORICAL_STATISTICS)
                },
                onNavigateToWordList = {
                    navController.navigate(NavDestination.WORD_LIST)
                },
                onNavigateToGrammarList = {
                    navController.navigate(NavDestination.GRAMMAR_LIST)
                },
                onNavigateToCategoryVocabulary = {
                    navController.navigate(NavDestination.categoryClassification("vocabulary"))
                },
                onNavigateToLeechManagement = {
                    navController.navigate(NavDestination.LEECH_MANAGEMENT)
                }
            )
        }

        // 测试页 - 主界面
        composable(
            route = NavDestination.TEST,
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.enterTransition(initialRoute, targetRoute) ?:
                if (initialRoute !in mainScreenRoutes) NemoNavigationAnimations.popEnterTransition()
                else BottomNavTransition.enterTransition()
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
                if (targetRoute !in mainScreenRoutes) NemoNavigationAnimations.exitTransition()
                else BottomNavTransition.exitTransition()
            },
            popEnterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.enterTransition(initialRoute, targetRoute) ?:
                if (initialRoute !in mainScreenRoutes) NemoNavigationAnimations.popEnterTransition()
                else BottomNavTransition.enterTransition()
            },
            popExitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
                BottomNavTransition.exitTransition()
            }
        ) {
            com.jian.nemo.feature.test.presentation.dashboard.TestDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTestSettings = { testModeId ->
                    navController.navigateToTestSettings(testModeId)
                },
                onNavigateToMistakes = {
                    navController.navigate(NavDestination.MISTAKES)
                },
                onNavigateToFavorites = {
                    navController.navigate(NavDestination.FAVORITES)
                }
            )
        }

        // 词库/搜索 - 主界面
        composable(
            route = NavDestination.LIBRARY,
            enterTransition = {
                val sourceRoute = initialState.destination.route
                if (sourceRoute !in mainScreenRoutes) NemoNavigationAnimations.popEnterTransition()
                else BottomNavTransition.enterTransition()
            },
            exitTransition = {
                val targetRoute = targetState.destination.route
                if (targetRoute !in mainScreenRoutes) NemoNavigationAnimations.exitTransition()
                else BottomNavTransition.exitTransition()
            },
            popEnterTransition = {
                val sourceRoute = initialState.destination.route
                if (sourceRoute !in mainScreenRoutes) NemoNavigationAnimations.popEnterTransition()
                else BottomNavTransition.enterTransition()
            },
            popExitTransition = { BottomNavTransition.exitTransition() }
        ) {
            com.jian.nemo.feature.library.presentation.LibraryScreen()
        }

        // 单词学习 - 使用新的 feature:learning 模块
        composable(
            route = NavDestination.WORD_LEARNING,
            arguments = listOf(navArgument("level") { type = NavType.StringType })
        ) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level") ?: "N5"
            LearningScreen(
                level = level,
                initialMode = LearningMode.Word,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 语法学习 - 复用LearningScreen，传入Grammar模式
        composable(
            route = NavDestination.GRAMMAR_LEARNING,
            arguments = listOf(navArgument("level") { type = NavType.StringType })
        ) { backStackEntry ->
            val level = backStackEntry.arguments?.getString("level") ?: "N5"
            LearningScreen(
                level = level,
                initialMode = LearningMode.Grammar,  // 初始化为语法模式
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 单词复习
        composable(NavDestination.WORD_REVIEW) {
            ReviewScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartReview = { navController.navigate("review_session") }
            )
        }

        // 语法复习
        composable(NavDestination.GRAMMAR_REVIEW) {
            ReviewScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartReview = { navController.navigate("review_session") }
            )
        }

        // 收藏列表 - Note: favoritesScreen is an extension function, likely wraps 'composable'.
        // If it uses default composable, it will inherit NavHost's defaults (Secondary transition), which is correct.
        favoritesScreen(
            onNavigateToWordFavorites = {
                navController.navigate(FAVORITE_WORDS_ROUTE)
            },
            onNavigateToGrammarFavorites = {
                navController.navigate(FAVORITE_QUESTIONS_ROUTE)
            },
            onWordClick = { wordId ->
                navController.navigate(NavDestination.wordDetail(wordId))
            },
            onNavigateBack = { navController.popBackStack() }
        )

        // 收藏单词列表
        favoriteWordsScreen(
            onWordClick = { wordId ->
                navController.navigate(NavDestination.wordDetail(wordId))
            },
            onNavigateBack = { navController.popBackStack() }
        )

        // 收藏语法列表
        favoriteGrammarsScreen(
            onGrammarClick = { grammarId ->
                navController.navigate(NavDestination.grammarDetail(grammarId))
            },
            onNavigateBack = { navController.popBackStack() }
        )

        // 收藏题目列表
        favoriteQuestionsScreen(
            onNavigateBack = { navController.popBackStack() }
        )

        // 错题本 - Note: mistakesScreen is an extension function.
        mistakesScreen(
            onNavigateToWordMistakes = {
                navController.navigate(WRONG_WORDS_ROUTE)
            },
            onNavigateToGrammarMistakes = {
                navController.navigate(WRONG_GRAMMARS_ROUTE)
            },
            onNavigateBack = { navController.popBackStack() }
        )

        // 错误单词列表
        wrongWordsScreen(
            onWordClick = { wordId ->
                navController.navigate(NavDestination.wordDetail(wordId))
            },
            onNavigateBack = { navController.popBackStack() }
        )

        // 错误语法列表
        wrongGrammarsScreen(
            onGrammarClick = { grammarId ->
                navController.navigate(NavDestination.grammarDetail(grammarId))
            },
            onNavigateBack = { navController.popBackStack() }
        )

        // 统计 - Note: statisticsScreen is an extension function.
        statisticsScreen(navController)

        // 待攻坚项 (原封禁管理)
        leechManagementScreen(navController)

        // 测试 - Note: testScreen is an extension function.
        testScreen(navController)

        // 设置（底部导航栏"个人"tab） - 主界面
        composable(
            route = NavDestination.SETTINGS,
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.enterTransition(initialRoute, targetRoute) ?:
                if (initialRoute !in mainScreenRoutes) NemoNavigationAnimations.popEnterTransition()
                else BottomNavTransition.enterTransition()
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
                if (targetRoute !in mainScreenRoutes) NemoNavigationAnimations.exitTransition()
                else BottomNavTransition.exitTransition()
            },
            popEnterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.enterTransition(initialRoute, targetRoute) ?:
                if (initialRoute !in mainScreenRoutes) NemoNavigationAnimations.popEnterTransition()
                else BottomNavTransition.enterTransition()
            },
            popExitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                HorizontalNavTransition.exitTransition(initialRoute, targetRoute) ?:
                BottomNavTransition.exitTransition()
            }
        ) {
            SettingsScreen(
                onNavigateToLogin = {
                    navController.navigate(NavDestination.PROFILE) // userGraph 处理登录/资料切换
                },
                onNavigateToTtsSettings = {
                    navController.navigate(NavDestination.TTS_SETTINGS)
                },
                onNavigateToAdvancedLearning = {
                    navController.navigate(NavDestination.ADVANCED_LEARNING)
                },
                onNavigateToThemeSettings = {
                    navController.navigate(NavDestination.THEME_SETTINGS)
                },
                onCheckUpdate = onCheckUpdate
            )
        }



        // TTS设置
        composable(NavDestination.TTS_SETTINGS) {
             TtsSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 主题设置
        composable(NavDestination.THEME_SETTINGS) {
            ThemeSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
 
        // 记忆算法设置
        composable(NavDestination.ADVANCED_LEARNING) {
            com.jian.nemo.feature.settings.AdvancedLearningSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 词性分类
        composable(NavDestination.PART_OF_SPEECH) {
            com.jian.nemo.feature.library.presentation.partofspeech.PartOfSpeechScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPosWords = { pos ->
                    navController.navigate(NavDestination.posWords(pos.name))
                }
            )
        }

        // 词性单词列表
        composable(
            route = NavDestination.POS_WORDS,
            arguments = listOf(navArgument("pos") { type = NavType.StringType })
        ) {
            com.jian.nemo.feature.library.presentation.category.PosWordsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 专项训练/专项词汇 - 分类选择
        composable(
            route = NavDestination.CATEGORY_CLASSIFICATION,
            arguments = listOf(navArgument("source") {
                type = NavType.StringType
                defaultValue = "practice"
            })
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "practice"
            com.jian.nemo.feature.library.presentation.category.CategoryClassificationScreen(
                source = source,
                onNavigateBack = { navController.popBackStack() },
                onCategorySelected = { categoryId, categoryTitle ->
                    // 根据来源判断导航：专项训练进入卡片学习，专项词汇进入列表
                    if (source == "practice") {
                        navController.navigate(
                            NavDestination.categoryCardLearning(categoryId, categoryTitle)
                        )
                    } else {
                        navController.navigate(
                            NavDestination.categoryWords(categoryId, categoryTitle)
                        )
                    }
                }
            )
        }

        // 专项训练 - 卡片学习
        composable(
            route = NavDestination.CATEGORY_CARD_LEARNING,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("categoryTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            val categoryTitle = backStackEntry.arguments?.getString("categoryTitle") ?: ""

            com.jian.nemo.feature.learning.presentation.category.CategoryCardLearningScreen(
                category = category,
                categoryTitle = categoryTitle,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 专项词汇 - 分类单词列表
        composable(
            route = NavDestination.CATEGORY_WORDS,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("categoryTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            val categoryTitle = backStackEntry.arguments?.getString("categoryTitle") ?: ""
            com.jian.nemo.feature.library.presentation.category.CategoryWordsScreen(
                category = category,
                categoryTitle = categoryTitle,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWordDetail = { wordId ->
                    navController.navigate(NavDestination.wordDetail(wordId))
                }
            )
        }

        // 今日到期复习
        composable(NavDestination.DUE_REVIEW) {
            ReviewScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartReview = {
                    navController.navigate("review_session")
                }
            )
        }

        // 统一复习会话 (Mock)
        composable("review_session") {
            com.jian.nemo.feature.learning.presentation.review.ReviewSessionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 学习日历
        composable(NavDestination.LEARNING_CALENDAR) {
            com.jian.nemo.feature.statistics.calendar.LearningCalendarScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 历史统计
        historicalStatisticsScreen(navController)

        // 学习热力图
        composable(NavDestination.ACTIVITY_HEATMAP) {
            com.jian.nemo.feature.statistics.ActivityHeatmapScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // 五十音图
        composable(NavDestination.KANA_CHART) {
            KanaChartScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 单词列表
        composable(NavDestination.WORD_LIST) {
            com.jian.nemo.feature.library.presentation.list.WordListScreen(
                navController = navController
            )
        }

        // 语法列表
        composable(NavDestination.GRAMMAR_LIST) {
            com.jian.nemo.feature.library.presentation.list.GrammarListScreen(
                navController = navController
            )
        }

        // 单词详情
        composable(
            route = NavDestination.WORD_DETAIL,
            arguments = listOf(navArgument("wordId") { type = NavType.IntType })
        ) {
            com.jian.nemo.feature.library.presentation.detail.WordDetailScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 语法详情
        composable(
            route = NavDestination.GRAMMAR_DETAIL,
            arguments = listOf(navArgument("grammarId") { type = NavType.IntType })
        ) {
            com.jian.nemo.feature.library.presentation.detail.GrammarDetailScreen(
                onNavigateBack = { navController.navigateUp() }
            )
        }

        // 用户中心与认证模块 (Login, Register, Profile)
        userGraph(
            navController = navController,
            onLoginSuccess = {
                // 登录成功后跳转到学习主页
                navController.navigate(NavDestination.LEARNING) {
                    popUpTo(ROUTE_LOGIN) { inclusive = true }
                }
            }
        )
    }
}

/**
 * 占位界面组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        )  {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "功能开发中...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
