package com.jian.nemo.feature.library.presentation.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch

/**
 * A micro-interaction wrapper that pulses (scales up then down) when tapped.
 *
 * Corresponds to the Flutter `_TapPulseWrapper` widget which uses a
 * `TweenSequence<double>` to animate 1.0 → 1.1 → 1.0 on tap.
 *
 * @param onTap Callback invoked when the wrapper is tapped.
 * @param active Whether the pulse animation is active.
 * @param fullArea Whether the tap target should fill all available space.
 * @param modifier Modifier to be applied to the wrapper.
 * @param content The composable content to wrap.
 */
@Composable
fun TapPulseWrapper(
    onTap: (() -> Unit)?,
    active: Boolean = true,
    fullArea: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // If not active and no tap callback, just render the child directly.
    if (!active && onTap == null) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .then(if (fullArea) Modifier.fillMaxSize() else Modifier)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .pointerInput(onTap, active) {
                detectTapGestures {
                    if (active) {
                        scope.launch {
                            // Animate 1.0 → 1.1 (scale up)
                            scale.animateTo(
                                targetValue = 1.1f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessHigh,
                                ),
                            )
                            // Animate 1.1 → 1.0 (scale back down)
                            scale.animateTo(
                                targetValue = 1.0f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMedium,
                                ),
                            )
                        }
                    }
                    onTap?.invoke()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
