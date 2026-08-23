package com.jian.nemo.core.ui.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

/**
 * Nemo 统一高斯毛玻璃沉浸式脚手架 (NemoScaffold)
 *
 * 核心特性：
 * 1. 顶部栏 (CommonHeader) 与内容完全贯通 (Edge-to-Edge)，自带悬浮高斯毛玻璃与饱和度增强效果；
 * 2. 内部自动管理 [HazeState]，并为底层内容挂载 [Modifier.haze]；
 * 3. 自动计算状态栏与导航栏高度，并通过 [content] 参数安全传递 [PaddingValues]；
 * 4. 向上滑动列表时，内容自然滑入 TopAppBar 背后，呈现统一的高级毛玻璃穿透质感。
 *
 * @param title 页面标题
 * @param onBack 返回回调，为 null 时不显示返回按钮
 * @param modifier 外部修饰符
 * @param backgroundColor 页面背景色
 * @param actions 顶部栏右侧操作插槽
 * @param centerContent 顶部栏中间自定义插槽
 * @param avatarUrl 顶部栏头像 URL
 * @param username 顶部栏用户名
 * @param onAvatarClick 顶部栏头像点击回调
 * @param floatingActionButton 悬浮按钮插槽
 * @param snackbarHost 提示条插槽
 * @param content 主体内容插槽，接收已预留出顶栏和系统栏的 [PaddingValues]
 */
@Composable
fun NemoScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    actions: @Composable (RowScope.() -> Unit)? = null,
    centerContent: @Composable (() -> Unit)? = null,
    avatarUrl: String? = null,
    username: String? = null,
    onAvatarClick: (() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    // 顶部 AppBar 包含状态栏 padding 与 56dp 内部高
    val topBarTotalHeight = statusBarHeight + 56.dp

    val safePaddingValues = remember(statusBarHeight, navigationBarHeight) {
        PaddingValues(
            top = topBarTotalHeight,
            bottom = navigationBarHeight,
            start = 0.dp,
            end = 0.dp
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. 底层可滚动内容区域（全屏延伸并应用 haze）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
        ) {
            content(safePaddingValues)
        }

        // 2. 顶栏悬浮于最上层（纯色实体背景，遮挡滑动内容）
        CommonHeader(
            title = title,
            onBack = onBack ?: {},
            hazeState = hazeState,
            avatarUrl = avatarUrl,
            username = username,
            onAvatarClick = onAvatarClick,
            centerContent = centerContent,
            actions = actions,
            backgroundColor = backgroundColor
        )

        // 3. 悬浮按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = navigationBarHeight + 16.dp, end = 16.dp)
        ) {
            floatingActionButton()
        }

        // 4. 提示条宿主
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navigationBarHeight + 16.dp)
        ) {
            snackbarHost()
        }
    }
}

/**
 * Nemo 自定义顶部栏沉浸式脚手架 (支持自定义 topBar)
 */
@Composable
fun NemoScaffold(
    topBar: @Composable (HazeState) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues, HazeState) -> Unit
) {
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }

    val topBarTotalHeight = statusBarHeight + 56.dp

    val safePaddingValues = remember(statusBarHeight, navigationBarHeight) {
        PaddingValues(
            top = topBarTotalHeight,
            bottom = navigationBarHeight,
            start = 0.dp,
            end = 0.dp
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 1. 底层内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
        ) {
            content(safePaddingValues, hazeState)
        }

        // 2. 顶部栏
        topBar(hazeState)

        // 3. 悬浮按钮
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = navigationBarHeight + 16.dp, end = 16.dp)
        ) {
            floatingActionButton()
        }

        // 4. 提示条
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navigationBarHeight + 16.dp)
        ) {
            snackbarHost()
        }
    }
}
