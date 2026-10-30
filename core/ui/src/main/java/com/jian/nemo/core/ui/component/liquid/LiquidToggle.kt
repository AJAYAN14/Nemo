package com.jian.nemo.core.ui.component.liquid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.jian.nemo.core.ui.component.liquid.utils.DampedDragAnimation
import com.jian.nemo.core.ui.component.Capsule
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

/**
 * 液态玻璃开关组件 (LiquidToggle)
 * 包含底轨与胶囊滑块，支持光影折射、物理弹性拖拽以及防手势冲突处理。
 */
@Composable
fun LiquidToggle(
    selected: () -> Boolean,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    accentColor: Color? = null
) {
    val isLightTheme = !isSystemInDarkTheme()
    val actualAccentColor = accentColor ?: MaterialTheme.colorScheme.primary
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val dragWidth = with(density) { 20f.dp.toPx() }
    val animationScope = rememberCoroutineScope()
    val currentSelected by rememberUpdatedState(selected)
    var fraction by remember { mutableFloatStateOf(if (selected()) 1f else 0f) }

    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = fraction,
            valueRange = 0f..1f,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.5f,
            onDragStarted = {},
            onDragStopped = {},
            onDrag = { _, _ -> }
        )
    }

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { fraction }
            .collectLatest { frac ->
                dampedDragAnimation.updateValue(frac)
            }
    }

    LaunchedEffect(selected()) {
        val target = if (selected()) 1f else 0f
        if (target != fraction) {
            fraction = target
            dampedDragAnimation.animateToValue(target)
        }
    }

    val trackBackdrop = rememberLayerBackdrop()
    val effectiveBackdrop = backdrop ?: rememberCanvasBackdrop { drawRect(surfaceColor) }

    // 组合容器：支持原版胶囊尺寸，统一全区域手势（捕获按下、拖动与点击，并在拖拽时消费事件锁定外层滚动）
    Box(
        modifier = modifier
            .size(64f.dp, 28f.dp)
            .semantics {
                role = Role.Switch
            }
            .pointerInput(dragWidth, isLtr) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    dampedDragAnimation.press()

                    var isDragging = false
                    val touchSlop = viewConfiguration.touchSlop
                    var accumulatedDragX = 0f
                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.fastFirstOrNull { it.id == pointerId }

                        if (change == null || change.changedToUpIgnoreConsumed()) {
                            change?.consume()
                            if (!isDragging) {
                                // 点击 (Tap)：切换状态并触发弹簧弹性滑动
                                val nextState = !currentSelected()
                                fraction = if (nextState) 1f else 0f
                                dampedDragAnimation.animateToValue(fraction)
                                onSelect(nextState)
                            } else {
                                // 拖拽结束：吸附到 0 或 1
                                val finalSelected = fraction >= 0.5f
                                fraction = if (finalSelected) 1f else 0f
                                dampedDragAnimation.animateToValue(fraction)
                                onSelect(finalSelected)
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
                            if (abs(accumulatedDragX) > touchSlop) {
                                isDragging = true
                                change.consume()
                                val delta = (dragDelta.x) / dragWidth
                                fraction =
                                    if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                                    else (fraction - delta).fastCoerceIn(0f, 1f)
                                dampedDragAnimation.updateValue(fraction)
                            }
                        } else {
                            // 拖拽中持续消费事件，彻底锁定外层滚动
                            change.consume()
                            val delta = dragDelta.x / dragWidth
                            fraction =
                                if (isLtr) (fraction + delta).fastCoerceIn(0f, 1f)
                                else (fraction - delta).fastCoerceIn(0f, 1f)
                            dampedDragAnimation.updateValue(fraction)
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. 底轨 (64dp × 28dp 胶囊)
        Box(
            Modifier
                .layerBackdrop(trackBackdrop)
                .clip(Capsule())
                .drawBehind {
                    val currentFraction = dampedDragAnimation.value
                    drawRect(lerp(trackColor, actualAccentColor, currentFraction))
                }
                .size(64f.dp, 28f.dp)
        )

        // 2. 原版液态玻璃胶囊滑块 (40dp × 24dp 饱满胶囊)
        Box(
            Modifier
                .graphicsLayer {
                    val currentFraction = dampedDragAnimation.value
                    val padding = 2f.dp.toPx()
                    translationX =
                        if (isLtr) lerp(padding, padding + dragWidth, currentFraction)
                        else lerp(-padding, -(padding + dragWidth), currentFraction)
                }
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(
                        effectiveBackdrop,
                        rememberBackdrop(trackBackdrop) { drawBackdrop ->
                            val progress = dampedDragAnimation.pressProgress
                            val scaleX = lerp(2f / 3f, 0.75f, progress)
                            val scaleY = lerp(0f, 0.75f, progress)
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
                            5f.dp.toPx() * progress,
                            10f.dp.toPx() * progress,
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
                        val velocity = dampedDragAnimation.velocity / 50f
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

/**
 * 方便项目中现有开关无缝接入的便利重载
 */
@Composable
fun LiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    accentColor: Color? = null
) {
    LiquidToggle(
        selected = { checked },
        onSelect = onCheckedChange,
        modifier = modifier,
        backdrop = backdrop,
        accentColor = accentColor
    )
}
