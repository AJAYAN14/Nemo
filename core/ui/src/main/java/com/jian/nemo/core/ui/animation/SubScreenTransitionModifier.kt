package com.jian.nemo.core.ui.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val isTransitioning = transition.currentState != transition.targetState
    
    // 动画平滑过渡或转场期间保持卡片圆角
    val currentRadius by transition.animateDp(
        label = "subScreenCornerRadius",
        transitionSpec = { tween(durationMillis = 300, easing = FastOutSlowInEasing) }
    ) { state ->
        if (state == EnterExitState.Visible) 0.dp else cornerRadius
    }

    val currentElevation by transition.animateDp(
        label = "subScreenElevation",
        transitionSpec = { tween(durationMillis = 300, easing = FastOutSlowInEasing) }
    ) { state ->
        if (state == EnterExitState.Visible) 0.dp else elevation
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val radius = if (isTransitioning) cornerRadius else currentRadius
                val elev = if (isTransitioning) elevation else currentElevation
                this.shadowElevation = elev.toPx()
                this.shape = RoundedCornerShape(radius)
                this.clip = isTransitioning || radius > 0.dp
            }
            .background(backgroundColor),
        content = content
    )
}
