package com.jian.nemo.core.ui.component.liquid

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastCoerceIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val PressProgressSpec = spring(0.5f, 300f, 0.001f)
private val PositionSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

private const val HIGHLIGHT_AGSL = """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""

fun createInteractiveHighlightRuntimeShader(): android.graphics.RuntimeShader? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return try {
        android.graphics.RuntimeShader(HIGHLIGHT_AGSL)
    } catch (_: Throwable) {
        null
    }
}

class InteractiveHighlight(
    val animationScope: CoroutineScope,
    val position: (size: Size, offset: Offset) -> Offset = { _, offset -> offset }
) {

    private var hasInteracted by mutableStateOf(false)

    private val pressProgressAnimation by lazy(LazyThreadSafetyMode.NONE) {
        Animatable(0f, 0.001f)
    }
    private val positionAnimation by lazy(LazyThreadSafetyMode.NONE) {
        Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)
    }

    private var startPosition = Offset.Zero
    val pressProgress: Float get() = if (hasInteracted) pressProgressAnimation.value else 0f
    val offset: Offset get() = if (hasInteracted) positionAnimation.value - startPosition else Offset.Zero

    private val runtimeShader: android.graphics.RuntimeShader? by lazy(LazyThreadSafetyMode.NONE) {
        createInteractiveHighlightRuntimeShader()
    }

    private val cachedShaderBrush: ShaderBrush? by lazy(LazyThreadSafetyMode.NONE) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimeShader?.let { ShaderBrush(it) }
        } else {
            null
        }
    }

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgress
            if (progress > 0f) {
                val shader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) runtimeShader else null
                val brush = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) cachedShaderBrush else null
                if (shader != null && brush != null) {
                    drawRect(
                        Color.White.copy(0.02f * progress),
                        blendMode = BlendMode.Plus
                    )
                    shader.apply {
                        val pos = position(size, positionAnimation.value)
                        setFloatUniform("size", size.width, size.height)
                        setColorUniform("color", android.graphics.Color.valueOf(1f, 1f, 1f, 0.05f * progress).toArgb())
                        setFloatUniform("radius", size.minDimension * 1.5f)
                        setFloatUniform(
                            "position",
                            pos.x.fastCoerceIn(0f, size.width),
                            pos.y.fastCoerceIn(0f, size.height)
                        )
                    }
                    drawRect(
                        brush,
                        blendMode = BlendMode.Plus
                    )
                } else {
                    drawRect(
                        Color.White.copy(0.03f * progress),
                        blendMode = BlendMode.Plus
                    )
                }
            }

            drawContent()
        }

    val gestureModifier: Modifier =
        Modifier.pointerInput(animationScope) {
            inspectDragGestures(
                onDragStart = { down ->
                    hasInteracted = true
                    startPosition = down.position
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(1f, PressProgressSpec) }
                        launch { positionAnimation.snapTo(startPosition) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, PressProgressSpec) }
                        launch { positionAnimation.animateTo(startPosition, PositionSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, PressProgressSpec) }
                        launch { positionAnimation.animateTo(startPosition, PositionSpec) }
                    }
                }
            ) { change, _ ->
                animationScope.launch { positionAnimation.snapTo(change.position) }
            }
        }
}
