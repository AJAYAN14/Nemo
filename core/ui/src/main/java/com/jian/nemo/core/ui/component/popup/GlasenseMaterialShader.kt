package com.jian.nemo.core.ui.component.popup

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import org.intellij.lang.annotations.Language

@Immutable
data class GlasenseVec4(
    val p0: Float,
    val p1: Float,
    val p2: Float,
    val p3: Float
)

@Immutable
data class GlasenseMaterialRecipe(
    val luminanceMapCurve: GlasenseVec4,
    val luminanceMapIntensity: Float,
    val saturation: Float,
    val extraBrightness: Float
)

private val CrystalLightCurve = GlasenseVec4(0.92f, 0.95f, 0.98f, 0.96f)
private val CrystalDarkCurve = GlasenseVec4(0.15f, 0.22f, 0.12f, 0.12f)

val GlasenseMenuRegularLight = GlasenseMaterialRecipe(CrystalLightCurve, 0.50f, 1.45f, 0.16f)
val GlasenseMenuRegularDark = GlasenseMaterialRecipe(CrystalDarkCurve, 0.55f, 1.35f, 0.04f)

@Language("AGSL")
private const val GLASENSE_MATERIAL_AGSL = """
uniform shader image;
uniform float p0, p1, p2, p3;
uniform float mapIntensity;
uniform float saturation;
uniform float brightness;
uniform float ditherStrength;

const vec3 LUMA_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

float bezierMap(float x) {
    float invX = 1.0 - x;
    float invX2 = invX * invX;
    float invX3 = invX2 * invX;
    float x2 = x * x;
    float x3 = x2 * x;

    return invX3 * p0
        + 3.0 * invX2 * x * p1
        + 3.0 * invX * x2 * p2
        + x3 * p3;
}

float interleavedGradientNoise(vec2 position) {
    return fract(
        52.9829189 * fract(
            dot(position, vec2(0.06711056, 0.00583715))
        )
    );
}

vec4 main(vec2 fragCoord) {
    vec3 rgb = image.eval(fragCoord).rgb;

    float luma1 = dot(rgb, LUMA_WEIGHTS);
    float mappedLuma = bezierMap(luma1);
    vec3 colorMapped = mix(rgb, vec3(mappedLuma), mapIntensity);

    float luma2 = dot(colorMapped, LUMA_WEIGHTS);
    vec3 colorSaturated = mix(vec3(luma2), colorMapped, saturation);

    float dither = interleavedGradientNoise(fragCoord) - 0.5;
    dither *= ditherStrength / 255.0;

    vec3 finalColor = colorSaturated + vec3(brightness + dither);

    return vec4(clamp(finalColor, 0.0, 1.0), 1.0);
}
"""

fun isRuntimeShaderSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
fun rememberGlasenseMaterialRenderEffectOrNull(recipe: GlasenseMaterialRecipe): ComposeRenderEffect? {
    if (!isRuntimeShaderSupported()) return null
    return remember(recipe) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            recipe.toRenderEffect()
        } else {
            null
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun GlasenseMaterialRecipe.toRenderEffect(): ComposeRenderEffect {
    val shader = RuntimeShader(GLASENSE_MATERIAL_AGSL)
    shader.setFloatUniform("p0", luminanceMapCurve.p0)
    shader.setFloatUniform("p1", luminanceMapCurve.p1)
    shader.setFloatUniform("p2", luminanceMapCurve.p2)
    shader.setFloatUniform("p3", luminanceMapCurve.p3)
    shader.setFloatUniform("mapIntensity", luminanceMapIntensity)
    shader.setFloatUniform("saturation", saturation)
    shader.setFloatUniform("brightness", extraBrightness)
    shader.setFloatUniform("ditherStrength", 1.0f)

    return RenderEffect.createRuntimeShaderEffect(
        shader,
        "image"
    ).asComposeRenderEffect()
}
