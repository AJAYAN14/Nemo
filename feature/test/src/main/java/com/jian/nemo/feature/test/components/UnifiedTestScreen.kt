package com.jian.nemo.feature.test.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.screenBackground
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 测试界面 Menu 展开状态全局 Provider
 */
val LocalTestMenuExpanded = compositionLocalOf<MutableState<Boolean>?> { null }

@Composable
fun UnifiedTestScreen(
    headerContent: @Composable () -> Unit,
    progressContent: @Composable () -> Unit,
    testContent: @Composable () -> Unit,
    footerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    UnifiedTestScreen(
        headerContentWithBackdrop = { _ -> headerContent() },
        progressContent = progressContent,
        testContent = testContent,
        footerContentWithBackdrop = { _ -> footerContent() },
        modifier = modifier
    )
}

@Composable
fun UnifiedTestScreen(
    headerContentWithBackdrop: @Composable (Backdrop) -> Unit,
    progressContent: @Composable () -> Unit,
    testContent: @Composable () -> Unit,
    footerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    UnifiedTestScreen(
        headerContentWithBackdrop = headerContentWithBackdrop,
        progressContent = progressContent,
        testContent = testContent,
        footerContentWithBackdrop = { _ -> footerContent() },
        modifier = modifier
    )
}

@Composable
fun UnifiedTestScreen(
    headerContent: @Composable () -> Unit,
    progressContent: @Composable () -> Unit,
    testContent: @Composable () -> Unit,
    footerContentWithBackdrop: @Composable (Backdrop) -> Unit,
    modifier: Modifier = Modifier
) {
    UnifiedTestScreen(
        headerContentWithBackdrop = { _ -> headerContent() },
        progressContent = progressContent,
        testContent = testContent,
        footerContentWithBackdrop = footerContentWithBackdrop,
        modifier = modifier
    )
}

@Composable
fun UnifiedTestScreen(
    headerContentWithBackdrop: @Composable (Backdrop) -> Unit,
    progressContent: @Composable () -> Unit,
    testContent: @Composable () -> Unit,
    footerContentWithBackdrop: @Composable (Backdrop) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val isDark = backgroundColor.luminance() < 0.5f

    val menuExpandedState = remember { mutableStateOf(false) }
    val menuDataState = remember { mutableStateOf<TestMenuData?>(null) }
    val backdrop = rememberLayerBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    CompositionLocalProvider(
        LocalTestMenuExpanded provides menuExpandedState,
        LocalTestMenuData provides menuDataState
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 1. 底层全量画布（滚动内容整体录入 backdrop，提供真实折射源）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                // 底层实色背景（录入 backdrop，消除空白区域透明空洞）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                )

                // 可滚动内容区域（动态避让顶部悬浮顶栏与底部悬浮按钮）
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = statusBarTop + 60.dp + 16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        progressContent()
                        Spacer(modifier = Modifier.height(16.dp))
                        testContent()
                    }

                    // 动态底部留白，确保解析卡片全展现，不被悬浮按钮及渐变蒙层遮挡
                    Spacer(modifier = Modifier.height(160.dp))
                }

                // 窗口级底部边缘平滑淡出蒙层（轻量避让与出界内容消隐，自底向上平滑微渐变）
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    backgroundColor.copy(alpha = 0f),
                                    backgroundColor.copy(alpha = 0.15f),
                                    backgroundColor.copy(alpha = if (isDark) 0.45f else 0.75f),
                                    backgroundColor
                                )
                            )
                        )
                )
            }

            // 2. 底部悬浮控制栏 (同级兄弟节点采样 backdrop，彻底激活物理透镜折射)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                footerContentWithBackdrop(backdrop)
            }

            // 3. 顶部悬浮顶栏 (带四阶防发灰遮罩 + 液态玻璃返回按钮，同级兄弟节点采样 backdrop)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor.copy(alpha = 0.60f),
                                backgroundColor.copy(alpha = 0.35f),
                                backgroundColor.copy(alpha = 0.10f),
                                backgroundColor.copy(alpha = 0f)
                            )
                        )
                    )
                    .padding(bottom = 8.dp)
            ) {
                headerContentWithBackdrop(backdrop)
            }

            // 3. 页面同级液态悬浮菜单浮层 (同级兄弟采样 backdrop，彻底激活物理透镜折射，纯透明手势拦截)
            val currentMenuData = menuDataState.value
            TestMenuOverlay(
                isVisible = menuExpandedState.value,
                onDismiss = { menuExpandedState.value = false },
                isFavorite = currentMenuData?.isFavorite == true,
                itemId = currentMenuData?.itemId ?: 0,
                onToggleFavorite = { itemId, fav -> currentMenuData?.onToggleFavorite?.invoke(itemId, fav) },
                onPause = { currentMenuData?.onPause?.invoke() },
                backdrop = backdrop
            )
        }
    }
}
