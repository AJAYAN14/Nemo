package com.jian.nemo.core.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/**
 * 通过横向平移配合 Z 轴缩放及透明度渐变，在页面切换时构建空间纵深感。
 */
object NemoNavigationAnimations {

    /**
     * 基础转场动画时长 (ms)
     */
    private const val ANIMATION_DURATION = 300

    /**
     * 透明度渐变动画时长 (ms)
     */
    private const val FADE_DURATION = 150

    /**
     * 进场动画的初始缩放比例
     */
    private const val ENTER_INITIAL_SCALE = 1.02f

    /**
     * 退场动画的目标缩放比例
     */
    private const val EXIT_TARGET_SCALE = 0.98f

    /**
     * 页面推入时的进场动画。
     * 从屏幕右侧向左横向滑入，伴随淡入及缩放恢复效果。
     */
    @OptIn(ExperimentalAnimationApi::class)
    fun enterTransition(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FADE_DURATION
            )
        ) + scaleIn(
            initialScale = ENTER_INITIAL_SCALE,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * 页面推入时的旧页面退场动画。
     * 向屏幕左侧完全滑出，伴随缩放缩小效果。
     */
    @OptIn(ExperimentalAnimationApi::class)
    fun exitTransition(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + scaleOut(
            targetScale = EXIT_TARGET_SCALE,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * 页面返回时的进场动画。
     * 从屏幕左侧滑入，伴随淡入及缩放恢复效果。
     */
    @OptIn(ExperimentalAnimationApi::class)
    fun popEnterTransition(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = FADE_DURATION
            )
        ) + scaleIn(
            initialScale = EXIT_TARGET_SCALE,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }

    /**
     * 页面返回时的当前页面退场动画。
     * 向屏幕右侧滑出，伴随淡出及轻微放大效果。
     */
    @OptIn(ExperimentalAnimationApi::class)
    fun popExitTransition(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = FADE_DURATION
            )
        ) + scaleOut(
            targetScale = ENTER_INITIAL_SCALE,
            animationSpec = tween(
                durationMillis = ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        )
    }
}
