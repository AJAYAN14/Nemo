package com.jian.nemo.core.ui.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 二级子界面卡片转场容器：
 * 在页面推入（Enter）和退出（Pop Exit）的转场滑动过程中，为子页面呈现与 Android 原生 Activity 窗口一致的
 * 硬件设备物理圆角（默认 28.dp）与边缘立体阴影；
 * 当页面完全就位展示（Visible 静止）后，圆角平滑融入全屏，确保无截断、边缘滚动和手势完全正常。
 */
@Composable
fun AnimatedVisibilityScope.SubScreenContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    elevation: Dp = 10.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable BoxScope.() -> Unit
) {
    val cardShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    val isTransitioning = transition.currentState != transition.targetState
                    if (isTransitioning) {
                        this.shadowElevation = elevation.toPx()
                        this.shape = cardShape
                        this.clip = true
                    } else {
                        this.shadowElevation = 0f
                        this.shape = RectangleShape
                        this.clip = false
                    }
                }
                .then(
                    if (backgroundColor != Color.Transparent) {
                        Modifier.background(backgroundColor)
                    } else {
                        Modifier
                    }
                ),
            content = content
        )
    }
}

/**
 * 提供全局装饰性循环动画暂停状态，用于底部导航交互等高优先级手势期间减少无关后台重绘。
 */
val LocalDecorativeAnimationsPaused = staticCompositionLocalOf<State<Boolean>?> { null }

/**
 * 感知当前页面的转场就位状态：
 * 仅在页面完全滑入就位且处于静止展示态时返回 true，
 * 在推入与退出动画执行期间瞬时返回 false。
 */
@Composable
fun rememberIsTransitionSettled(): Boolean {
    val navScope = LocalNavAnimatedVisibilityScope.current ?: return true
    return navScope.transition.currentState == EnterExitState.Visible &&
            navScope.transition.targetState == EnterExitState.Visible
}
