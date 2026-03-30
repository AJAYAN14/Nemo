package com.jian.nemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jian.nemo.core.ui.component.NemoBottomBar
import com.jian.nemo.core.data.util.DatabaseInitializer
import com.jian.nemo.core.designsystem.theme.NemoTheme
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.service.SyncService
import com.jian.nemo.navigation.NemoNavHost
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.ui.component.update.UpdateDialog
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.jian.nemo.core.ui.component.dialog.GoogleTtsInstallDialog
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * Nemo 2.0 应用程序入口
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var syncService: SyncService

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // 触发自动同步检查
        syncService.onAppForeground()

        // 在首次启动时强制初始化数据库
        applicationScope.launch {
            databaseInitializer.initialize()
        }

        enableEdgeToEdge()
        // 启动时请求通知权限 (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionLauncher = registerForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // 可选的初始化逻辑
            }
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            // 获取主题配置
            val darkMode by settingsRepository.isDarkModeFlow.collectAsState(initial = null)
            val dynamicColor by settingsRepository.isDynamicColorEnabledFlow.collectAsState(initial = false)

            val isDarkTheme = darkMode ?: isSystemInDarkTheme()

            NemoTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false // 禁用动态取色以强制使用品牌色 #0E68FF
            ) {
                NemoApp()
            }
        }
    }
}

@Composable
fun NemoApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // 检查应用更新
    LaunchedEffect(Unit) {
        viewModel.checkUpdate(BuildConfig.VERSION_CODE)
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 启动页标识
    val isSplashScreen = currentRoute == "splash"
    // 学习界面标识，用于 edge-to-edge 配置
    val isLearningScreen = currentRoute == "learning"
    // 使用 edge-to-edge 布局的路由列表（禁用标准 Scaffold padding）
    val edgeToEdgeRoutes = listOf("splash", "learning", "progress", "settings", "test")
    val useEdgeToEdge = currentRoute in edgeToEdgeRoutes

    // 根据当前路由和主题标准化系统栏配置
    SetupSystemBars(isSplashScreen = isSplashScreen)

    // 需要显示底部导航栏的路由
    val bottomBarRoutes = listOf("learning", "progress", "test", "settings")
    val showBottomBar = currentRoute in bottomBarRoutes && !isSplashScreen

    // 获取系统栏 Insets 以实现动态底部导航高度
    val bottomInsetPx = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    val bottomInsetDp = with(LocalDensity.current) { bottomInsetPx.toDp() }

    // 使用 animateDpAsState 实现底部 padding 的平滑过渡
    val animatedBottomPadding by animateDpAsState(
        targetValue = if (showBottomBar) bottomInsetDp else 0.dp,
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "BottomBarPadding"
    )

    val hazeState = remember { HazeState() }

    Scaffold(
        bottomBar = {
            NemoBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        // 防止重复导航
                        launchSingleTop = true
                        // 恢复 backstack 状态
                        restoreState = true
                        // 清除栈直到初始导航目标
                        popUpTo("learning") {
                            saveState = true
                        }
                    }
                },
                visible = showBottomBar,
                hazeState = hazeState
            )
        },
        // Edge-to-Edge：各页面自行处理系统栏 Insets，禁用 Scaffold 默认 Insets
        contentWindowInsets = if (useEdgeToEdge) WindowInsets(0.dp) else WindowInsets(0.dp)
    ) { innerPadding ->
        // Edge-to-Edge：禁用特定页面的 padding 以允许背景延伸至全屏
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (useEdgeToEdge) {
                        // Edge-to-Edge：背景全屏延伸，不使用 padding
                        Modifier.haze(hazeState)
                    } else {
                        // 默认页面：应用标准 Scaffold 内部 padding
                        Modifier.padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = animatedBottomPadding,
                            start = innerPadding.calculateLeftPadding(LocalLayoutDirection.current),
                            end = innerPadding.calculateRightPadding(LocalLayoutDirection.current)
                        ).haze(hazeState)
                    }
                ),
            // Edge-to-Edge：使用透明背景以允许页面自行渲染背景
            // 标准：使用 MaterialTheme 背景色
            color = if (useEdgeToEdge) Color.Transparent else MaterialTheme.colorScheme.background
        ) {
            NemoNavHost(
                navController = navController,
                onCheckUpdate = {
                    viewModel.checkUpdate(BuildConfig.VERSION_CODE, isManual = true)
                }
            )
        }
    }

    // 监听应用更新检查事件
    LaunchedEffect(viewModel) {
        viewModel.updateCheckEvents.collect { event ->
            when (event) {
                is UpdateCheckEvent.NoUpdateAvailable -> {
                    android.widget.Toast.makeText(context, "已是最新版本", android.widget.Toast.LENGTH_SHORT).show()
                }
                is UpdateCheckEvent.Error -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 集成应用更新弹窗
    val config = uiState.updateConfig
    // 识别当前是否处于“过渡/登录”等关键路径页面
    val isTransientScreen = isSplashScreen || currentRoute == "login"
    
    // 显示逻辑：强制更新无视页面，建议更新则避开登录/启动等关键页面以防中断用户心流
    if (config != null && (config.isForce || !isTransientScreen)) {
        UpdateDialog(
            config = config,
            downloadProgress = uiState.downloadProgress,
            isDownloading = uiState.isDownloading,
            isDownloaded = uiState.isDownloaded,
            onUpdateClick = { viewModel.startDownload(context) },
            onInstallClick = { viewModel.installApk(context) },
            onExitClick = { }, // 退出功能已禁用
            onDismissRequest = {
                viewModel.dismissUpdate()
            }
        )
    }

    // Google TTS 安装提示
    if (uiState.showGoogleTtsDialog) {
        GoogleTtsInstallDialog(
            isDownloading = uiState.isDownloadingGoogleTts,
            downloadProgress = uiState.googleTtsDownloadProgress,
            onInstallClick = {
                viewModel.downloadGoogleTts(context)
            },
            onDismissClick = {
                viewModel.dismissGoogleTtsDialog()
            }
        )
    }
}



/**
 * 配置系统栏以实现沉浸式显示
 * - 设置状态栏和导航栏背景透明
 * - 根据主题和路由动态调整图标对比度
 * - 允许内容延伸至系统栏后方
 *
 */
@Composable
fun SetupSystemBars(isSplashScreen: Boolean = false) {
    val view = LocalView.current
    val window = (view.context as? android.app.Activity)?.window

    // 获取特定主题颜色用于亮度计算
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background

    // 启动页特殊处理：品牌背景使用浅色图标
    val useDarkIcons = if (isSplashScreen) {
        false // 启动页：浅色图标
    } else {
        // 标准页面：图标根据背景亮度进行对比
        val isDarkTheme = surfaceColor.luminance() < 0.5
        !isDarkTheme // 深色背景 -> 浅色图标；浅色背景 -> 深色图标
    }

    // 在主题、颜色或路由变化时更新系统栏
    LaunchedEffect(surfaceColor, backgroundColor, isSplashScreen) {
        window?.let {
            // 使用官方 EdgeToEdge API
            WindowCompat.setDecorFitsSystemWindows(it, false)

            val insetsController = WindowInsetsControllerCompat(it, view)

            // 沉浸式：状态栏透明，控制图标外观
            insetsController.isAppearanceLightStatusBars = useDarkIcons

            // 沉浸式：导航栏透明，控制图标外观
            insetsController.isAppearanceLightNavigationBars = useDarkIcons
        }
    }
}
