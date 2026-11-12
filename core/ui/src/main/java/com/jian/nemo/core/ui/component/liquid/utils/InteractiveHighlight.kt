package com.jian.nemo.core.ui.component.liquid.utils

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
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asComposeShader
import com.kyant.backdrop.isRuntimeShaderSupported
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val UtilsPressProgressSpec = spring(0.5f, 300f, 0.001f)
private val UtilsPositionSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

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

    private val shader by lazy(LazyThreadSafetyMode.NONE) {
        if (isRuntimeShaderSupported()) {
            RuntimeShader(
                """
uniform float2 size;
layout(color) uniform half4 color;
uniform float radius;
uniform float2 position;

half4 main(float2 coord) {
    float dist = distance(coord, position);
    float intensity = smoothstep(radius, radius * 0.5, dist);
    return color * intensity;
}"""
            )
        } else {
            null
        }
    }

    private val cachedShaderBrush: ShaderBrush? by lazy(LazyThreadSafetyMode.NONE) {
        shader?.let { ShaderBrush(it.asComposeShader()) }
    }

    val modifier: Modifier =
        Modifier.drawWithContent {
            val progress = pressProgress
            if (progress > 0f) {
                val activeShader = if (isRuntimeShaderSupported()) shader else null
                val brush = if (isRuntimeShaderSupported()) cachedShaderBrush else null
                if (activeShader != null && brush != null) {
                    drawRect(
                        Color.White.copy(0.08f * progress),
                        blendMode = BlendMode.Plus
                    )
                    activeShader.apply {
                        val position = position(size, positionAnimation.value)
                        setFloatUniform("size", size.width, size.height)
                        setColorUniform("color", Color.White.copy(0.15f * progress))
                        setFloatUniform("radius", size.minDimension * 1.5f)
                        setFloatUniform(
                            "position",
                            position.x.fastCoerceIn(0f, size.width),
                            position.y.fastCoerceIn(0f, size.height)
                        )
                    }
                    drawRect(
                        brush,
                        blendMode = BlendMode.Plus
                    )
                } else {
                    drawRect(
                        Color.White.copy(0.25f * progress),
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
                        launch { pressProgressAnimation.animateTo(1f, UtilsPressProgressSpec) }
                        launch { positionAnimation.snapTo(startPosition) }
                    }
                },
                onDragEnd = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, UtilsPressProgressSpec) }
                        launch { positionAnimation.animateTo(startPosition, UtilsPositionSpec) }
                    }
                },
                onDragCancel = {
                    animationScope.launch {
                        launch { pressProgressAnimation.animateTo(0f, UtilsPressProgressSpec) }
                        launch { positionAnimation.animateTo(startPosition, UtilsPositionSpec) }
                    }
                }
            ) { change, _ ->
                animationScope.launch { positionAnimation.snapTo(change.position) }
            }
        }
}
