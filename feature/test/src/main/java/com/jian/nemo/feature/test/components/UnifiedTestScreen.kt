package com.jian.nemo.feature.test.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.jian.nemo.core.designsystem.theme.screenBackground

/**
 * 测试界面 Menu 展开状态全局 Provider（用于触发全屏高斯模糊与景深遮罩）
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
    // 动态背景色：适配深色模式
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = MaterialTheme.colorScheme.screenBackground
    val isDark = backgroundColor.luminance() < 0.5f

    val menuExpandedState = remember { mutableStateOf(false) }
    val isMenuExpanded = menuExpandedState.value

    // 动态高斯模糊半径 (0dp -> 14dp 平滑过渡，对齐学习界面)
    val animatedBlurRadius by animateDpAsState(
        targetValue = if (isMenuExpanded) 14.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "testMenuBlur"
    )

    CompositionLocalProvider(LocalTestMenuExpanded provides menuExpandedState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            // 可滚动内容区域（菜单展开时应用景深模糊）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (animatedBlurRadius > 0.dp) Modifier.blur(radius = animatedBlurRadius)
                        else Modifier
                    )
                    .verticalScroll(rememberScrollState())
            ) {
                headerContent()
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    progressContent()
                    Spacer(modifier = Modifier.height(16.dp))
                    testContent()
                }

                // 动态底部留白，确保解析卡片全展现，不被悬浮按钮及渐变蒙层遮挡
                Spacer(modifier = Modifier.height(160.dp))
            }

            // 底部渐变蒙层：从透明到纯白的平滑过渡
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(180.dp)
                    .then(
                        if (animatedBlurRadius > 0.dp) Modifier.blur(radius = animatedBlurRadius)
                        else Modifier
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                backgroundColor.copy(alpha = if (isDark) 0.6f else 0.9f),
                                backgroundColor
                            )
                        )
                    )
            )

            // 固定底部按钮区域
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .then(
                        if (animatedBlurRadius > 0.dp) Modifier.blur(radius = animatedBlurRadius)
                        else Modifier
                    )
            ) {
                footerContent()
            }

            // 页面全屏高斯模糊景深遮罩 (Z = 1，12% 柔光暗化 + 200ms 淡入淡出，点击遮罩任意处收起菜单，对齐学习界面)
            AnimatedVisibility(
                visible = isMenuExpanded,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.12f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { menuExpandedState.value = false }
                        )
                )
            }
        }
    }
}
