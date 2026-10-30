package com.jian.nemo.core.ui.modifier

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 3D 按压倾斜效果 Modifier
 *
 * 触摸卡片不同区域时，卡片向手指方向呈现 3D 悬浮倾斜，松开时带有物理弹性回弹。
 * 支持常规点击及长按手势（达到长按阈值后直接触发回调）。
 *
 * @param maxTilt 最大倾斜角度（度）
 * @param maxScale 按压时的整体缩放比例
 * @param onLongClick 长按达标时的回调（触发后松开不会重复触发 onClick）
 * @param onClick 触控在卡片范围内松开时触发的点击回调
 */
fun Modifier.tiltOnPress(
    maxTilt: Float = 12f,
    maxScale: Float = 0.98f,
    longPressTimeoutMillis: Long? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit = {}
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var tapOffset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    var longClickJob by remember { mutableStateOf<Job?>(null) }
    var longClickTriggered by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            longClickJob?.cancel()
        }
    }

    val tiltX by animateFloatAsState(
        targetValue = if (isPressed && size.height > 0) {
            val centerY = size.height / 2f
            val deltaY = (tapOffset.y - centerY) / centerY
            -deltaY.coerceIn(-1f, 1f) * maxTilt
        } else 0f,
        animationSpec = if (isPressed) {
            spring(dampingRatio = 0.85f, stiffness = 400f)
        } else {
            spring(dampingRatio = 0.55f, stiffness = 300f)
        },
        label = "tiltX"
    )

    val tiltY by animateFloatAsState(
        targetValue = if (isPressed && size.width > 0) {
            val centerX = size.width / 2f
            val deltaX = (tapOffset.x - centerX) / centerX
            deltaX.coerceIn(-1f, 1f) * maxTilt
        } else 0f,
        animationSpec = if (isPressed) {
            spring(dampingRatio = 0.85f, stiffness = 400f)
        } else {
            spring(dampingRatio = 0.55f, stiffness = 300f)
        },
        label = "tiltY"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) maxScale else 1f,
        animationSpec = if (isPressed) {
            spring(dampingRatio = 0.85f, stiffness = 400f)
        } else {
            spring(dampingRatio = 0.55f, stiffness = 300f)
        },
        label = "scale"
    )

    this
        .onGloballyPositioned { size = it.size }
        .pointerInput(onClick, onLongClick, longPressTimeoutMillis) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                down.consume()
                isPressed = true
                tapOffset = down.position
                longClickTriggered = false
                longClickJob?.cancel()

                if (onLongClick != null) {
                    val timeout = longPressTimeoutMillis ?: viewConfiguration.longPressTimeoutMillis
                    longClickJob = scope.launch {
                        delay(timeout)
                        if (isPressed) {
                            longClickTriggered = true
                            onLongClick()
                        }
                    }
                }

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (change.pressed) {
                        tapOffset = change.position
                        change.consume()
                    } else {
                        longClickJob?.cancel()
                        val releasedInside = change.position.x in 0f..size.width.toFloat() &&
                                change.position.y in 0f..size.height.toFloat()
                        if (isPressed && releasedInside && !longClickTriggered) {
                            onClick()
                        }
                        isPressed = false
                        change.consume()
                        break
                    }
                }
                isPressed = false
                longClickJob?.cancel()
            }
        }
        .graphicsLayer {
            rotationX = tiltX
            rotationY = tiltY
            scaleX = scale
            scaleY = scale
            cameraDistance = 12f * density
        }
}
