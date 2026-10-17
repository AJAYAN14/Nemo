package com.jian.nemo.core.ui.component.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.component.Capsule
import com.jian.nemo.core.ui.component.liquid.utils.DampedDragAnimation
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

/**
 * 液态玻璃滑动条组件 (LiquidSlider)
 * 包含底轨与胶囊滑块，支持光影折射、速度流体形变、物理弹性拖拽以及防手势冲突处理。
 */
@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    visibilityThreshold: Float = 0.001f,
    backdrop: Backdrop? = null,
    accentColor: Color? = null
) {
    val isLightTheme = !isSystemInDarkTheme()
    val actualAccentColor = accentColor ?: if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    val trackBackdrop = rememberLayerBackdrop()
    val effectiveBackdrop = backdrop ?: rememberCanvasBackdrop { drawRect(surfaceColor) }
    val density = LocalDensity.current
    val thumbTouchRadiusPx = with(density) { 24.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }

        val dampedDragAnimation = remember(animationScope, valueRange, visibilityThreshold) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) {
                        onValueChange(targetValue)
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    onValueChange(
                        if (isLtr) (targetValue + delta).coerceIn(valueRange)
                        else (targetValue - delta).coerceIn(valueRange)
                    )
                }
            )
        }

        val currentValue = value()
        var isInteracting by remember { mutableStateOf(false) }

        LaunchedEffect(currentValue, isInteracting) {
            if (!isInteracting && kotlin.math.abs(dampedDragAnimation.targetValue - currentValue) > visibilityThreshold) {
                dampedDragAnimation.animateToValue(currentValue)
            }
        }

        // 外层全区域交互手势：负责捕获按下、拖动与点击，消费 pointer 事件以彻底锁定外层 verticalScroll
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(trackWidth, valueRange, isLtr) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isInteracting = true
                        try {
                            val downX = down.position.x
                            val currentThumbX = trackWidth * dampedDragAnimation.progress
                            val isHitThumb = abs(downX - currentThumbX) <= thumbTouchRadiusPx

                            dampedDragAnimation.press()

                        var isDragging = false
                        val touchSlop = viewConfiguration.touchSlop
                        var accumulatedDragX = 0f
                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.fastFirstOrNull { it.id == pointerId }

                            if (change == null || change.changedToUpIgnoreConsumed()) {
                                if (!isDragging) {
                                    // 轻触点击 (Tap)：平滑过渡至目标值
                                    val delta = (valueRange.endInclusive - valueRange.start) * (downX / trackWidth)
                                    val targetValue =
                                        (if (isLtr) valueRange.start + delta
                                        else valueRange.endInclusive - delta)
                                            .coerceIn(valueRange)
                                    dampedDragAnimation.animateToValue(targetValue)
                                    onValueChange(targetValue)
                                } else {
                                    onValueChange(dampedDragAnimation.targetValue)
                                }
                                dampedDragAnimation.release()
                                break
                            }

                            if (!change.pressed) {
                                dampedDragAnimation.release()
                                break
                            }

                            val dragDelta = change.positionChange()
                            if (!isDragging) {
                                accumulatedDragX += dragDelta.x
                                if (abs(accumulatedDragX) > touchSlop || (isHitThumb && abs(accumulatedDragX) > 2f)) {
                                    isDragging = true
                                    change.consume()
                                    if (!isHitThumb) {
                                        // 起手在轨道空白处，滑块立刻跟手到当前位置并启动拖拽
                                        val delta = (valueRange.endInclusive - valueRange.start) * (change.position.x / trackWidth)
                                        val targetValue =
                                            (if (isLtr) valueRange.start + delta
                                            else valueRange.endInclusive - delta)
                                                .coerceIn(valueRange)
                                        dampedDragAnimation.updateValue(targetValue)
                                        onValueChange(targetValue)
                                    } else {
                                        val delta = (valueRange.endInclusive - valueRange.start) * (dragDelta.x / trackWidth)
                                        val targetValue =
                                            (if (isLtr) dampedDragAnimation.targetValue + delta
                                            else dampedDragAnimation.targetValue - delta)
                                                .coerceIn(valueRange)
                                        dampedDragAnimation.updateValue(targetValue)
                                        onValueChange(targetValue)
                                    }
                                } else {
                                    // 在横向微动阶段消费事件，避免外层 verticalScroll 抢占
                                    if (abs(dragDelta.x) > 0.5f) {
                                        change.consume()
                                    }
                                }
                            } else {
                                // 拖拽中持续消费事件，彻底锁定外层滚动
                                change.consume()
                                val delta = (valueRange.endInclusive - valueRange.start) * (dragDelta.x / trackWidth)
                                val targetValue =
                                    (if (isLtr) dampedDragAnimation.targetValue + delta
                                    else dampedDragAnimation.targetValue - delta)
                                        .coerceIn(valueRange)
                                dampedDragAnimation.updateValue(targetValue)
                                onValueChange(targetValue)
                            }
                        }
                    } finally {
                        isInteracting = false
                    }
                }
            }
        )

        // 底轨与进度条 (垂直居中于 40dp 容器)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .layerBackdrop(trackBackdrop)
        ) {
            Box(
                Modifier
                    .clip(Capsule())
                    .background(trackColor)
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(Capsule())
                    .background(actualAccentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        // 原生 40dp × 24dp 液态玻璃胶囊滑块 (垂直居中于 40dp 容器)
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX =
                        (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                            .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        effectiveBackdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 1f, progress)
                            val scaleY = lerp(0f, 1f, progress)
                            scale(scaleX, scaleY) {
                                drawBackdrop()
                            }
                        }
                    ),
                    shape = { Capsule() },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        blur(8f.dp.toPx() * (1f - progress))
                        lens(
                            10f.dp.toPx() * progress,
                            14f.dp.toPx() * progress,
                            chromaticAberration = true
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Ambient.copy(
                            width = Highlight.Ambient.width / 1.5f,
                            blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                            alpha = progress
                        )
                    },
                    shadow = {
                        Shadow(
                            radius = 4f.dp,
                            color = Color.Black.copy(alpha = 0.05f)
                        )
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = 4f.dp * progress,
                            alpha = progress
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(Color.White.copy(alpha = 1f - progress))
                    }
                )
                .size(40f.dp, 24f.dp)
        )
    }
}
