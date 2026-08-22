package com.jian.nemo.core.ui.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * 容器展开形变过渡修饰符 (Container Transform)
 *
 * @param key 容器唯一标识，前后页面相匹配的 key 之间会执行平滑形变
 * @param shape 容器在当前状态的圆角形状，过渡期间会与目标页面的 shape 进行平滑插值过渡
 * @param zIndexInOverlay 转场期间在图层上的层级，默认为 1f 确保卡片浮于上层平滑展开
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Modifier.containerTransform(
    key: String,
    shape: Shape = RoundedCornerShape(0.dp),
    zIndexInOverlay: Float = 1f
): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Rect>()

    return if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            this@containerTransform.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = zIndexInOverlay,
                boundsTransform = { _, _ -> spatialSpec }
            ).clip(shape)
        }
    } else {
        this
    }
}
