package com.jian.nemo.core.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * 界面切换动画：移植自 Open-notes 项目的全局视差平移与淡入淡出动画
 */
object NemoNavigationAnimations {

    /**
     * 基础转场动画时长 (ms)
     */
    private const val ANIMATION_DURATION = 300

    /**
     * 页面推入时的进场动画。
     * 从屏幕右侧向左完全滑入，伴随淡入效果。
     */
    fun enterTransition(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION
            )
        )
    }

    /**
     * 页面推入时的旧页面退场动画。
     * 向屏幕左侧微移 1/3 屏幕宽度（视差重叠感），伴随淡出效果。
     */
    fun exitTransition(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { -it / 3 },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION
            )
        )
    }

    /**
     * 页面返回时的进场动画。
     * 从左侧 -1/3 屏幕宽度处滑动复位，伴随淡入效果。
     */
    fun popEnterTransition(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { -it / 3 },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION
            )
        )
    }

    /**
     * 页面返回时的当前页面退场动画。
     * 向屏幕右侧完全滑出，伴随淡出效果。
     */
    fun popExitTransition(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION
            )
        )
    }
}

