package com.jian.nemo.core.ui.animation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * 界面切换动画：子界面推入与返回视差转场动效（对齐原生 Activity 实体卡片推拉规范）
 */
object NemoNavigationAnimations {

    /**
     * 基础转场动画时长 (ms)
     */
    private const val ANIMATION_DURATION = 300

    /**
     * 1. 页面推入时的进场动画（子页面）：
     * 从屏幕右侧向左完全滑入，保持 100% 实体卡片（不进行全屏淡入淡出，消除透底闪烁）。
     */
    fun enterTransition(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * 2. 页面推入时的旧页面退场动画（上级页面）：
     * 向屏幕左侧微移 1/4 屏幕宽度，构建标准视差遮挡层次感。
     */
    fun exitTransition(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * 3. 页面返回时的进场动画（上级页面复位）：
     * 从左侧 -1/4 屏幕宽度处滑动复位，平稳回正。
     */
    fun popEnterTransition(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * 4. 页面返回时的当前页面退场动画（子页面退出）：
     * 向屏幕右侧完全滑出，保持 100% 实体卡片，直接露出底下的主页面。
     */
    fun popExitTransition(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }
}



