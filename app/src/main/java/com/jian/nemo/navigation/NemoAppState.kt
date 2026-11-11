package com.jian.nemo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.jian.nemo.core.ui.navigation.NavDestination

/**
 * 创建并记住 [NemoAppState] 实例
 */
@Composable
fun rememberNemoAppState(
    navController: NavHostController = rememberNavController()
): NemoAppState {
    return remember(navController) {
        NemoAppState(navController)
    }
}

/**
 * Nemo 应用全局导航与 UI 状态持有者
 *
 * 统一管理：
 * 1. NavController 导航控制器
 * 2. 当前路由监听与状态判断
 * 3. 底部导航栏可见性判定与沉浸式路由判定
 * 4. 顶级主目的地导航跳转（动态解析 startDestination，避免硬编码）
 */
@Stable
class NemoAppState(
    val navController: NavHostController
) {
    /**
     * 当前路由路径
     */
    val currentRoute: String?
        @Composable get() = navController.currentBackStackEntryAsState().value?.destination?.route

    /**
     * 是否处于启动屏
     */
    val isSplashScreen: Boolean
        @Composable get() = currentRoute == "splash"

    /**
     * 是否处于过渡/临时页面（如启动屏、登录页）
     */
    val isTransientScreen: Boolean
        @Composable get() = isSplashScreen || currentRoute == "login"

    /**
     * 顶级主界面路由白名单（展示底部导航栏）
     */
    val topLevelRoutes: Set<String> = setOf(
        NavDestination.LEARNING,
        NavDestination.PROGRESS,
        NavDestination.TEST,
        NavDestination.SETTINGS
    )

    /**
     * 使用全屏延伸沉浸式布局的路由白名单（禁用标准 Scaffold padding）
     */
    private val edgeToEdgeRoutes: Set<String> = setOf(
        "splash",
        NavDestination.LEARNING,
        NavDestination.PROGRESS,
        NavDestination.SETTINGS,
        NavDestination.TEST
    )

    /**
     * 是否使用全屏延伸沉浸式布局
     */
    val useEdgeToEdge: Boolean
        @Composable get() = currentRoute in edgeToEdgeRoutes

    /**
     * 底部导航栏是否可见
     */
    val shouldShowBottomBar: Boolean
        @Composable get() = currentRoute in topLevelRoutes && !isSplashScreen

    /**
     * 底栏主 Tab 导航跳转逻辑
     * 动态查找图谱的起始目的地，保持单一顶部实例并恢复状态
     */
    fun navigateToTopLevelDestination(route: String) {
        val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }

        navController.navigate(route, topLevelNavOptions)
    }
}
