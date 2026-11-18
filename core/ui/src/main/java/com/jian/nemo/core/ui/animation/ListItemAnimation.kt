package com.jian.nemo.core.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidatePlacement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 列表项入场动画修饰符。
 *
 * 通过 [LayoutModifierNode] 在节点挂载时驱动一次淡入、微缩放与上移动效，
 * 动画期间仅在图层属性阶段更新变换，完成后立即摘除离屏层并回退至直接摆放。
 */
fun Modifier.animateListItem(): Modifier = this then ListItemEntranceElement

private data object ListItemEntranceElement : ModifierNodeElement<ListItemEntranceNode>() {
    override fun create(): ListItemEntranceNode = ListItemEntranceNode()

    override fun update(node: ListItemEntranceNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "animateListItem"
    }
}

private class ListItemEntranceNode : Modifier.Node(), LayoutModifierNode {
    private val animatable = Animatable(1f)
    private val progressState = mutableFloatStateOf(1f)
    private var isAnimating = false
    private var animationJob: Job? = null

    override fun onAttach() {
        startEntranceAnimation()
    }

    override fun onDetach() {
        stopAnimation()
    }

    override fun onReset() {
        stopAnimation()
    }

    private fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
        isAnimating = false
        progressState.floatValue = 1f
    }

    private fun startEntranceAnimation() {
        animationJob?.cancel()
        isAnimating = true
        progressState.floatValue = INITIAL_PROGRESS
        invalidatePlacement()
        animationJob = coroutineScope.launch {
            try {
                animatable.snapTo(INITIAL_PROGRESS)
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) {
                    progressState.floatValue = value
                }
            } finally {
                progressState.floatValue = 1f
                isAnimating = false
                if (isAttached) {
                    invalidatePlacement()
                }
            }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            if (!isAnimating) {
                placeable.placeRelative(0, 0)
            } else {
                placeable.placeRelativeWithLayer(0, 0) {
                    val progress = progressState.floatValue
                    alpha = progress.coerceIn(0f, 1f)
                    val scale = lerp(0.85f, 1f, progress)
                    scaleX = scale
                    scaleY = scale
                    translationY = lerp(30f, 0f, progress)
                }
            }
        }
    }

    private companion object {
        const val INITIAL_PROGRESS = 0.08f
    }
}
