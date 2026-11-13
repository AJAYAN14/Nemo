package com.jian.nemo.core.ui.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * 展开弹簧：展开自然舒展，适度微弹性
 * 阻尼比 0.85f，刚度 500f
 */
fun <T> spatialExpandSpring() = spring<T>(
    dampingRatio = 0.85f,
    stiffness = 500f
)

/**
 * 收缩返回弹簧：极度跟手干脆，高刚度快速响应，彻底消除拖沓与漂移
 * 阻尼比 0.90f（高吸能无晃动），刚度 900f（响应周期约 180~220ms）
 */
fun <T> spatialCollapseSpring() = spring<T>(
    dampingRatio = 0.90f,
    stiffness = 900f
)

/**
 * 非空间极速弹簧（用于透明度与遮罩极速淡入淡出）
 * 临界阻尼 1.0f，刚度 3000f，保证内容在毫秒级瞬间隐匿，杜绝残影
 */
fun <T> nonSpatialExpressiveSpring() = spring<T>(
    dampingRatio = 1.0f,
    stiffness = 3000f
)

/**
 * 智能双向容器形变边界变换：
 * 动态识别当前是“卡片膨胀展开”还是“全屏收缩飞回”，精准分发动效模型
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val nemoContainerBoundsTransform = BoundsTransform { initialBounds, targetBounds ->
    val isShrinking = (targetBounds.width * targetBounds.height) < (initialBounds.width * initialBounds.height)
    if (isShrinking) {
        spatialCollapseSpring()
    } else {
        spatialExpandSpring()
    }
}

/**
 * 卡片源端容器变换修饰符 (Card Origin)
 *
 * @param key 容器唯一标识
 * @param cornerRadius 卡片物理圆角（默认 24.dp）
 * @param zIndexInOverlay 转场期间在共享图层上的层级
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.nemoCardContainer(
    key: String,
    cornerRadius: Dp = 24.dp,
    zIndexInOverlay: Float = 1f
): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    if (sharedTransitionScope == null || animatedVisibilityScope == null) {
        return this
    }

    val overlayShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    return with(sharedTransitionScope) {
        this@nemoCardContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = nemoContainerBoundsTransform,
            clipInOverlayDuringTransition = OverlayClip(overlayShape),
            enter = fadeIn(nonSpatialExpressiveSpring()),
            exit = fadeOut(nonSpatialExpressiveSpring()),
            zIndexInOverlay = zIndexInOverlay,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
        )
    }
}

/**
 * 目标屏幕端容器变换修饰符 (Screen Destination)
 *
 * @param key 与卡片端严格对应的容器唯一标识
 * @param cardCornerRadius 卡片起始态的圆角（默认 24.dp）
 * @param zIndexInOverlay 转场期间在共享图层上的层级
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.nemoScreenContainer(
    key: String,
    cardCornerRadius: Dp = 24.dp,
    zIndexInOverlay: Float = 1f
): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    if (sharedTransitionScope == null || animatedVisibilityScope == null) {
        return this
    }

    val overlayShape = remember(cardCornerRadius) { RoundedCornerShape(cardCornerRadius) }

    return with(sharedTransitionScope) {
        this@nemoScreenContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = nemoContainerBoundsTransform,
            clipInOverlayDuringTransition = OverlayClip(overlayShape),
            enter = fadeIn(nonSpatialExpressiveSpring()),
            exit = fadeOut(nonSpatialExpressiveSpring()),
            zIndexInOverlay = zIndexInOverlay,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
        )
    }
}

/**
 * 通用容器形变过渡修饰符
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.containerTransform(
    key: String,
    shape: Shape = RoundedCornerShape(0.dp),
    zIndexInOverlay: Float = 1f
): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    if (sharedTransitionScope == null || animatedVisibilityScope == null) {
        return this
    }

    return with(sharedTransitionScope) {
        this@containerTransform.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
            boundsTransform = nemoContainerBoundsTransform,
            clipInOverlayDuringTransition = OverlayClip(shape),
            enter = fadeIn(nonSpatialExpressiveSpring()),
            exit = fadeOut(nonSpatialExpressiveSpring()),
            zIndexInOverlay = zIndexInOverlay,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
        )
    }
}
