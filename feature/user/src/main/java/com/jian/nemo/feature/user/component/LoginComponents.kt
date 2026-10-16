package com.jian.nemo.feature.user.component

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.graphics.BlendMode as AndroidBlendMode
import android.graphics.RuntimeShader
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.FrostedLiquidGlassButton
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.kyant.backdrop.Backdrop
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 当前获得输入焦点的表单输入框类型枚举
 */
enum class FocusedAuthField {
    LOGIN_EMAIL,
    LOGIN_PASSWORD,
    REGISTER_USERNAME,
    REGISTER_EMAIL,
    REGISTER_PASSWORD,
    REGISTER_CONFIRM_PASSWORD
}

// ==============================================================================
// Oklab 感知均匀色彩空间转换与插值算法（1:1 对齐 TSX 原型 Shader 算法）
// ==============================================================================

private data class OklabColor(val l: Float, val a: Float, val b: Float, val alpha: Float)

private fun toLinear(c: Float): Float {
    return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
}

private fun fromLinear(c: Float): Float {
    val clamped = c.coerceIn(0f, 1f)
    return if (clamped <= 0.0031308f) clamped * 12.92f else 1.055f * clamped.pow(1.0f / 2.4f) - 0.055f
}

private fun colorToOklab(color: Color): OklabColor {
    val lr = toLinear(color.red)
    val lg = toLinear(color.green)
    val lb = toLinear(color.blue)
    val l = java.lang.Math.cbrt((0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb).toDouble()).toFloat()
    val m = java.lang.Math.cbrt((0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb).toDouble()).toFloat()
    val s = java.lang.Math.cbrt((0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb).toDouble()).toFloat()
    return OklabColor(
        l = 0.2104542553f * l + 0.793617785f * m - 0.0040720468f * s,
        a = 1.9779984951f * l - 2.428592205f * m + 0.4505937099f * s,
        b = 0.0259040371f * l + 0.7827717662f * m - 0.808675766f * s,
        alpha = color.alpha
    )
}

private fun oklabToColor(lab: OklabColor): Color {
    val l = lab.l + 0.3963377774f * lab.a + 0.2158037573f * lab.b
    val m = lab.l - 0.1055613458f * lab.a - 0.0638541728f * lab.b
    val s = lab.l - 0.0894841775f * lab.a - 1.2914855480f * lab.b
    val l3 = l * l * l
    val m3 = m * m * m
    val s3 = s * s * s
    val rLinear = 4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3
    val gLinear = -1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3
    val bLinear = -0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3
    return Color(
        red = fromLinear(rLinear).coerceIn(0f, 1f),
        green = fromLinear(gLinear).coerceIn(0f, 1f),
        blue = fromLinear(bLinear).coerceIn(0f, 1f),
        alpha = lab.alpha.coerceIn(0f, 1f)
    )
}

private fun interpolateOklab(c1: OklabColor, c2: OklabColor, fraction: Float): OklabColor {
    return OklabColor(
        l = c1.l + (c2.l - c1.l) * fraction,
        a = c1.a + (c2.a - c1.a) * fraction,
        b = c1.b + (c2.b - c1.b) * fraction,
        alpha = c1.alpha + (c2.alpha - c1.alpha) * fraction
    )
}

/**
 * 在感知均匀的 Oklab 色彩空间内对渐变色盘进行多色阶均匀插值，彻底消除 sRGB 线性渐变产生的暗区与失真
 */
private fun buildOklabGradient(colors: List<Color>, steps: Int = 32): List<Color> {
    if (colors.size <= 1) return colors
    val oklabRamp = colors.map { colorToOklab(it) }
    val result = ArrayList<Color>(steps)
    val maxIdx = colors.size - 1
    for (i in 0 until steps) {
        val t = i.toFloat() / (steps - 1)
        val pos = t * maxIdx
        val idx = pos.toInt().coerceIn(0, maxIdx - 1)
        val fract = pos - idx
        val interpolated = interpolateOklab(oklabRamp[idx], oklabRamp[idx + 1], fract)
        result.add(oklabToColor(interpolated))
    }
    return result
}

/**
 * 经典 Dave Hoskins 2D 散列算法，对齐 TSX 原型着色器中的 hash12 函数
 */
private fun hash12(x: Float, y: Float): Float {
    var qx = (x * 0.1031f)
    var qy = (y * 0.1031f)
    var qz = (x * 0.1031f)
    qx -= kotlin.math.floor(qx)
    qy -= kotlin.math.floor(qy)
    qz -= kotlin.math.floor(qz)
    val dot = qx * (qy + 33.33f) + qy * (qz + 33.33f) + qz * (qx + 33.33f)
    qx += dot
    qy += dot
    qz += dot
    val res = (qx + qy) * qz
    return res - kotlin.math.floor(res)
}

// ==============================================================================
// 100% 原版 WebGL2 假名极光天幕引擎（1:1 像素级复刻 React Bits 官方着色器管线）
// 包含 4 重 RenderPass（FIELD, HALO 1/4降采样, BLUR 双向高斯模糊, COMPOSITE 物理加法发光）
// 胶片颗粒噪点、手势滑动波纹与触控交互，与原版 demo HTML 绝对 100% 毫无二致
// ==============================================================================

private const val KANA_WAVE_HTML = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no" />
  <style>
    * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
    html, body {
      margin: 0;
      padding: 0;
      width: 100%;
      height: 100%;
      overflow: hidden;
      background: transparent !important;
      user-select: none;
      -webkit-user-select: none;
      touch-action: none;
    }
    .gradient-ascii-container {
      position: relative;
      width: 100%;
      height: 100%;
      overflow: hidden;
    }
    .gradient-ascii-canvas {
      position: absolute;
      inset: 0;
      display: block;
      width: 100%;
      height: 100%;
    }
    #ascii-fade {
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      height: 144px;
      pointer-events: none;
      transition: background 0.4s ease;
      background: linear-gradient(to bottom, rgba(13,12,17,0) 0%, #0d0c11 100%);
    }
  </style>
</head>
<body>
  <div id="ascii-container" class="gradient-ascii-container">
    <canvas id="ascii-canvas" class="gradient-ascii-canvas" aria-hidden="true"></canvas>
    <div id="ascii-fade"></div>
  </div>
  <script>
    const MAX_COLORS = 8;
    const MAX_CLICKS = 4;
    const MAX_TRAIL = 24;
    const CLICK_LIFE = 1.2;
    const TRAIL_LIFE = 0.7;
    const TRAIL_STEP = 0.015;
    const INTRO_SECONDS = 2.4;
    const ATLAS_CELL_W = 84;
    const ATLAS_CELL_H = 100;
    const ATLAS_COLUMNS = 16;
    const FONT_SCALE = 0.66;
    const SHAPES = { horizon: 0, pinch: 1, rise: 2, eclipse: 3 };
    const CLICK_EFFECTS = { none: 0, wave: 1, burst: 2 };

    const VERTEX = '#version 300 es\n' +
      'out vec2 vUv;\n' +
      'void main() {\n' +
      '  vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));\n' +
      '  vUv = p;\n' +
      '  gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n' +
      '}';

    const COMMON = '\n' +
      'float hash12(vec2 p) {\n' +
      '  vec3 q = fract(vec3(p.xyx) * 0.1031);\n' +
      '  q += dot(q, q.yzx + 33.33);\n' +
      '  return fract((q.x + q.y) * q.z);\n' +
      '}\n';

    const FIELD = '#version 300 es\n' +
      'precision highp float;\n' +
      'precision highp int;\n' +
      'uniform float uAspect;\n' +
      'uniform float uTime;\n' +
      'uniform int uShape;\n' +
      'uniform vec3 uRamp[8];\n' +
      'uniform int uCount;\n' +
      'uniform float uScale;\n' +
      'uniform float uSoftness;\n' +
      'uniform float uFlow;\n' +
      'uniform vec2 uSeed;\n' +
      'uniform vec4 uTrail[24];\n' +
      'uniform float uRadius;\n' +
      'uniform vec4 uClicks[4];\n' +
      'uniform int uClickEffect;\n' +
      'uniform float uIntro;\n' +
      'uniform float uFrame;\n' +
      'in vec2 vUv;\n' +
      'layout(location = 0) out vec4 outColor;\n' +
      'layout(location = 1) out vec4 outAux;\n' +
      COMMON +
      'vec2 hash22(vec2 p) {\n' +
      '  vec3 q = fract(vec3(p.xyx) * vec3(0.1031, 0.103, 0.0973));\n' +
      '  q += dot(q, q.yzx + 33.33);\n' +
      '  return fract((q.xx + q.yz) * q.zy) * 2.0 - 1.0;\n' +
      '}\n' +
      'float noise(vec2 p) {\n' +
      '  vec2 i = floor(p);\n' +
      '  vec2 f = fract(p);\n' +
      '  vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);\n' +
      '  float a = dot(hash22(i), f);\n' +
      '  float b = dot(hash22(i + vec2(1.0, 0.0)), f - vec2(1.0, 0.0));\n' +
      '  float c = dot(hash22(i + vec2(0.0, 1.0)), f - vec2(0.0, 1.0));\n' +
      '  float d = dot(hash22(i + vec2(1.0, 1.0)), f - vec2(1.0, 1.0));\n' +
      '  return 0.5 + 0.5 * mix(mix(a, b, u.x), mix(c, d, u.x), u.y);\n' +
      '}\n' +
      'vec3 labToRgb(vec3 lab) {\n' +
      '  float l = lab.x + 0.3963377774 * lab.y + 0.2158037573 * lab.z;\n' +
      '  float m = lab.x - 0.1055613458 * lab.y - 0.0638541728 * lab.z;\n' +
      '  float s = lab.x - 0.0894841775 * lab.y - 1.2914855480 * lab.z;\n' +
      '  l = l * l * l; m = m * m * m; s = s * s * s;\n' +
      '  return vec3(\n' +
      '    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,\n' +
      '    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,\n' +
      '    -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s\n' +
      '  );\n' +
      '}\n' +
      'vec3 encode(vec3 c) {\n' +
      '  c = clamp(c, 0.0, 1.0);\n' +
      '  return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, c));\n' +
      '}\n' +
      'vec3 horizon(vec2 p, float time, out float order) {\n' +
      '  float span = 0.5 * max(uAspect, 1.0);\n' +
      '  float x = p.x / span;\n' +
      '  float arch = -0.17 - 0.2 * x * x + 0.02 * sin(time * 0.6);\n' +
      '  float soft = mix(0.05, 0.22, uSoftness);\n' +
      '  float edge = p.y - arch;\n' +
      '  float rim = 0.38 * smoothstep(-soft, 0.42, edge);\n' +
      '  vec2 sun = vec2(0.08 * sin(time * 0.31) * span, 0.58);\n' +
      '  float spread = length((p - sun) / vec2(0.95 * span, 0.75));\n' +
      '  float sky = mix(0.55, 1.0, smoothstep(0.2, 0.95, spread));\n' +
      '  order = clamp(edge * 1.3, 0.0, 1.0);\n' +
      '  return vec3(rim, sky, smoothstep(0.18, 0.62, edge));\n' +
      '}\n' +
      'vec4 tone(float t) {\n' +
      '  float x = smoothstep(0.3, 1.0, t) * float(uCount - 1);\n' +
      '  int index = int(floor(x));\n' +
      '  return vec4(mix(uRamp[index], uRamp[min(index + 1, uCount - 1)], fract(x)), smoothstep(0.0, 0.38, t));\n' +
      '}\n' +
      'void main() {\n' +
      '  vec2 p = (vUv - 0.5) * vec2(uAspect, 1.0);\n' +
      '  float time = uTime;\n' +
      '  vec2 q = p / uScale;\n' +
      '  vec2 warp = vec2(\n' +
      '    noise(q * 1.6 + uSeed + vec2(0.0, time * 0.3)),\n' +
      '    noise(q * 1.6 + uSeed.yx + vec2(4.3, -time * 0.24))\n' +
      '  ) - 0.5;\n' +
      '  q += warp * uFlow * 0.18;\n' +
      '  float order = 0.0;\n' +
      '  vec3 shape = horizon(q, time, order);\n' +
      '  float reveal = clamp((uIntro * 1.45 - order) / 0.45, 0.0, 1.0);\n' +
      '  reveal = reveal * reveal * (3.0 - 2.0 * reveal);\n' +
      '  float scramble = 0.0;\n' +
      '  for (int i = 0; i < 24; i++) {\n' +
      '    vec4 point = uTrail[i];\n' +
      '    if (point.z <= 0.0) continue;\n' +
      '    vec2 d = p - point.xy;\n' +
      '    scramble = max(scramble, point.z * exp(-dot(d, d) / (uRadius * uRadius)));\n' +
      '  }\n' +
      '  for (int i = 0; i < 4; i++) {\n' +
      '    vec4 c = uClicks[i];\n' +
      '    if (c.w <= 0.0) continue;\n' +
      '    float d = length(p - c.xy);\n' +
      '    if (uClickEffect == 1) {\n' +
      '      scramble = max(scramble, exp(-pow((d - c.z * 0.75) / 0.06, 2.0)) * c.w);\n' +
      '    } else if (uClickEffect == 2) {\n' +
      '      float size = 0.08 + 0.22 * (1.0 - exp(-c.z * 5.0));\n' +
      '      scramble = max(scramble, (1.0 - smoothstep(size * 0.6, size, d)) * c.w);\n' +
      '    }\n' +
      '  }\n' +
      '  vec2 t = clamp(shape.xy * mix(0.7, 1.0, reveal), 0.0, 1.0);\n' +
      '  vec4 mixed = mix(tone(t.x), tone(t.y), shape.z);\n' +
      '  vec3 color = encode(labToRgb(mixed.rgb));\n' +
      '  float alpha = mixed.a * reveal;\n' +
      '  float dither = (hash12(gl_FragCoord.xy + uFrame * 13.7) - 0.5) / 255.0;\n' +
      '  outColor = clamp(vec4(color * alpha + dither, alpha + dither), 0.0, 1.0);\n' +
      '  outAux = vec4(reveal, scramble, 0.0, 1.0);\n' +
      '}';

    const GLYPHS = '\n' +
      'uniform sampler2D uField;\n' +
      'uniform sampler2D uAux;\n' +
      'uniform sampler2D uAtlas;\n' +
      'uniform vec2 uResolution;\n' +
      'uniform vec2 uCell;\n' +
      'uniform vec2 uOffset;\n' +
      'uniform vec2 uAtlasGrid;\n' +
      'uniform float uCount;\n' +
      'uniform float uDensity;\n' +
      'uniform float uOpacity;\n' +
      'uniform float uFlicker;\n' +
      'uniform float uDepth;\n' +
      'uniform float uGlyphTime;\n' +
      'uniform float uSeedValue;\n' +
      'uniform float uLight;\n' +
      COMMON +
      'float sampleGlyph(float index, vec2 local, vec2 coord, float soft) {\n' +
      '  float column = mod(index, uAtlasGrid.x);\n' +
      '  float row = floor(index / uAtlasGrid.x);\n' +
      '  vec2 atlasUv = vec2((column + local.x) / uAtlasGrid.x, (row + 1.0 - local.y) / uAtlasGrid.y);\n' +
      '  vec2 continuous = coord / uAtlasGrid;\n' +
      '  float scale = exp2(soft);\n' +
      '  return textureGrad(uAtlas, atlasUv, dFdx(continuous) * scale, dFdy(continuous) * scale).a;\n' +
      '}\n' +
      'vec4 glyphAt(vec2 px) {\n' +
      '  vec2 coord = (px - uOffset) / uCell;\n' +
      '  vec2 cell = floor(coord);\n' +
      '  vec2 local = coord - cell;\n' +
      '  vec2 uv = clamp(((cell + 0.5) * uCell + uOffset) / uResolution, vec2(0.0), vec2(1.0));\n' +
      '  vec4 field = texture(uField, uv);\n' +
      '  vec4 aux = texture(uAux, uv);\n' +
      '  vec3 visible = uLight > 0.5 ? field.rgb + (1.0 - field.a) : field.rgb;\n' +
      '  float luma = dot(visible, vec3(0.2126, 0.7152, 0.0722));\n' +
      '  float value = uLight > 0.5 ? clamp((1.0 - luma) * 1.5, 0.0, 1.0) : luma;\n' +
      '  float pick = hash12(cell + uSeedValue * 7.31);\n' +
      '  float level = hash12(cell * 1.73 + 4.1 + uSeedValue);\n' +
      '  float tick = floor(uGlyphTime * 9.0 + level * 9.0);\n' +
      '  float wobble = (hash12(cell * 2.31 + tick * 17.7) - 0.5) * uFlicker * 0.4 + (level - 0.5) * 0.1;\n' +
      '  float tone = clamp(pow(value, 0.75) + wobble, 0.0, 1.0);\n' +
      '  float index = floor(tone * (uCount - 1.0) + 0.5);\n' +
      '  float decode = max(aux.g, 1.0 - aux.r);\n' +
      '  if (decode > 0.02) {\n' +
      '    float frame = floor(uGlyphTime * 20.0);\n' +
      '    if (hash12(cell * 3.7 + frame * 5.3) < decode * 1.25) index = 1.0 + floor(hash12(cell + frame * 1.37) * (uCount - 1.0));\n' +
      '  }\n' +
      '  float present = step(pick, uDensity * mix(1.0, 0.3 + tone * 1.1, 0.55));\n' +
      '  float cutoff = smoothstep(0.03, 0.1, value);\n' +
      '  float focus = step(hash12(cell * 0.71 + 9.2 + uSeedValue), uDepth);\n' +
      '  float coverage = sampleGlyph(index, local, coord, focus * 1.2);\n' +
      '  float alpha = uOpacity * present * cutoff * coverage * mix(1.0, 0.35 + 0.65 * level, 0.55) * (1.0 - focus * 0.5);\n' +
      '  if (uLight < 0.5) {\n' +
      '    vec3 light = field.rgb * 0.55 + vec3(0.03) * field.a;\n' +
      '    return vec4(light * alpha, 0.6 * field.a * alpha);\n' +
      '  }\n' +
      '  vec3 ink = visible * 0.8;\n' +
      '  return vec4(ink * alpha, alpha);\n' +
      '}\n';

    const HALO = '#version 300 es\n' +
      'precision highp float;\n' +
      'uniform float uDownscale;\n' +
      'in vec2 vUv;\n' +
      'out vec4 outColor;\n' +
      GLYPHS +
      'void main() {\n' +
      '  outColor = glyphAt(gl_FragCoord.xy * uDownscale);\n' +
      '}';

    const BLUR = '#version 300 es\n' +
      'precision highp float;\n' +
      'uniform sampler2D uSource;\n' +
      'uniform vec2 uStep;\n' +
      'in vec2 vUv;\n' +
      'out vec4 outColor;\n' +
      'void main() {\n' +
      '  vec4 sum = texture(uSource, vUv) * 0.2270270270;\n' +
      '  sum += texture(uSource, vUv + uStep * 1.3846153846) * 0.3162162162;\n' +
      '  sum += texture(uSource, vUv - uStep * 1.3846153846) * 0.3162162162;\n' +
      '  sum += texture(uSource, vUv + uStep * 3.2307692308) * 0.0702702703;\n' +
      '  sum += texture(uSource, vUv - uStep * 3.2307692308) * 0.0702702703;\n' +
      '  outColor = sum;\n' +
      '}';

    const COMPOSITE = '#version 300 es\n' +
      'precision highp float;\n' +
      'uniform sampler2D uHalo;\n' +
      'uniform float uGlow;\n' +
      'uniform float uGradient;\n' +
      'uniform float uGrain;\n' +
      'uniform float uFrame;\n' +
      'in vec2 vUv;\n' +
      'out vec4 outColor;\n' +
      GLYPHS +
      'void main() {\n' +
      '  vec4 base = texture(uField, vUv) * uGradient;\n' +
      '  vec4 glyph = glyphAt(gl_FragCoord.xy);\n' +
      '  vec4 halo = texture(uHalo, vUv) * uGlow;\n' +
      '  vec4 color;\n' +
      '  if (uLight < 0.5) {\n' +
      '    color = base + glyph + halo;\n' +
      '  } else {\n' +
      '    color = glyph + base * (1.0 - glyph.a);\n' +
      '    color += halo * 0.6 * (1.0 - color.a);\n' +
      '  }\n' +
      '  float grain = (hash12(gl_FragCoord.xy + uFrame * vec2(17.0, 59.0)) + hash12(gl_FragCoord.xy * 1.31 + uFrame * 7.0) - 1.0) * uGrain;\n' +
      '  float body = clamp(color.a, 0.0, 1.0);\n' +
      '  color.rgb += grain * (0.25 + body) * 0.5;\n' +
      '  color.a = clamp(color.a, 0.0, 1.0);\n' +
      '  color.rgb = clamp(color.rgb, 0.0, color.a);\n' +
      '  outColor = color;\n' +
      '}';

    const clamp = (value, min, max) => Math.min(max, Math.max(min, value));
    const toLinear = value => (value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4));

    const toLab = ([r, g, b]) => {
      const lr = toLinear(r);
      const lg = toLinear(g);
      const lb = toLinear(b);
      const l = Math.cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb);
      const m = Math.cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb);
      const s = Math.cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb);
      return [
        0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s
      ];
    };

    const compile = (gl, type, source) => {
      const shader = gl.createShader(type);
      if (!shader) return null;
      gl.shaderSource(shader, source);
      gl.compileShader(shader);
      if (gl.getShaderParameter(shader, gl.COMPILE_STATUS)) return shader;
      gl.deleteShader(shader);
      return null;
    };

    const link = (gl, fragmentSource) => {
      const vertex = compile(gl, gl.VERTEX_SHADER, VERTEX);
      const fragment = compile(gl, gl.FRAGMENT_SHADER, fragmentSource);
      const program = gl.createProgram();
      if (!vertex || !fragment || !program) return null;
      gl.attachShader(program, vertex);
      gl.attachShader(program, fragment);
      gl.linkProgram(program);
      gl.deleteShader(vertex);
      gl.deleteShader(fragment);
      if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
        gl.deleteProgram(program);
        return null;
      }
      const uniforms = {};
      const count = gl.getProgramParameter(program, gl.ACTIVE_UNIFORMS);
      for (let i = 0; i < count; i++) {
        const info = gl.getActiveUniform(program, i);
        if (!info) continue;
        const name = info.name.replace(/\[0\]$/, '');
        uniforms[name] = gl.getUniformLocation(program, name);
      }
      return { program, uniforms };
    };

    const createTarget = (gl, count) => {
      const textures = Array.from({ length: count }, () => {
        const texture = gl.createTexture();
        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        return texture;
      });
      return { textures, framebuffer: gl.createFramebuffer(), width: 0, height: 0 };
    };

    const sizeTarget = (gl, target, width, height) => {
      if (target.width === width && target.height === height) return;
      target.width = width;
      target.height = height;
      gl.bindFramebuffer(gl.FRAMEBUFFER, target.framebuffer);
      target.textures.forEach((texture, i) => {
        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA8, width, height, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
        gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0 + i, gl.TEXTURE_2D, texture, 0);
      });
      gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    };

    const buildAtlas = (gl, characters, fontFamily) => {
      const list = [...new Set([...characters].filter(char => char !== '\n' && char !== '\r'))];
      if (!list.length) list.push(' ');
      const font = '500 ' + Math.round(ATLAS_CELL_H * FONT_SCALE) + 'px ' + fontFamily;
      const columns = Math.min(ATLAS_COLUMNS, list.length);
      const rows = Math.ceil(list.length / columns);
      const canvas = document.createElement('canvas');
      canvas.width = ATLAS_CELL_W * columns;
      canvas.height = ATLAS_CELL_H * rows;
      const context = canvas.getContext('2d');
      if (context) {
        context.font = font;
        context.textAlign = 'center';
        context.textBaseline = 'middle';
        context.fillStyle = '#fff';
        list.forEach((char, index) => {
          const column = index % columns;
          const row = Math.floor(index / columns);
          context.fillText(char, column * ATLAS_CELL_W + ATLAS_CELL_W / 2, row * ATLAS_CELL_H + ATLAS_CELL_H / 2 + 3);
        });
      }
      const texture = gl.createTexture();
      gl.bindTexture(gl.TEXTURE_2D, texture);
      gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
      gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA8, gl.RGBA, gl.UNSIGNED_BYTE, canvas);
      gl.generateMipmap(gl.TEXTURE_2D);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR_MIPMAP_LINEAR);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
      gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
      return { texture, count: list.length, grid: [columns, rows] };
    };

    const asciiSettings = {
      colors: ['#1e3fd8', '#f6f8ff', '#a5d8ff', '#38bdf8'],
      shape: 0,
      characters: ' ・ノくしこてなのあまめぬ',
      cellSize: 24,
      density: 0.86,
      glyphOpacity: 0.92,
      gradientOpacity: 0.92,
      glow: 0.22,
      depthOfField: 0,
      flicker: 0.16,
      softness: 0.5,
      scale: 1,
      flow: 0.5,
      speed: 0.85,
      grain: 0.08,
      variation: 0,
      mouseInteraction: true,
      scrambleRadius: 0.12,
      clickEffect: 1,
      intro: true,
      lightMode: false,
      fontFamily: '"Hiragino Sans", "Yu Gothic", "Noto Sans JP", "Meiryo", ui-monospace, monospace'
    };

    let asciiWake = null;

    window.setTheme = function(isDark) {
      if (isDark) {
        asciiSettings.colors = ['#1e3fd8', '#f6f8ff', '#a5d8ff', '#38bdf8'];
        asciiSettings.lightMode = false;
        document.getElementById('ascii-fade').style.background = 'linear-gradient(to bottom, rgba(13,12,17,0) 0%, #0d0c11 100%)';
      } else {
        asciiSettings.colors = ['#1d4ed8', '#3b82f6', '#93c5fd', '#38bdf8'];
        asciiSettings.lightMode = true;
        document.getElementById('ascii-fade').style.background = 'linear-gradient(to bottom, rgba(247,249,252,0) 0%, #f7f9fc 100%)';
      }
      if (asciiWake) asciiWake();
    };

    function initGradientASCII() {
      const container = document.getElementById('ascii-container');
      const canvas = document.getElementById('ascii-canvas');
      const gl = canvas.getContext('webgl2', {
        alpha: true,
        premultipliedAlpha: true,
        antialias: false,
        depth: false,
        stencil: false
      });
      if (!container || !canvas || !gl) return;

      const field = link(gl, FIELD);
      const halo = link(gl, HALO);
      const blur = link(gl, BLUR);
      const composite = link(gl, COMPOSITE);
      if (!field || !halo || !blur || !composite) return;

      const vao = gl.createVertexArray();
      const fieldTarget = createTarget(gl, 2);
      const haloA = createTarget(gl, 1);
      const haloB = createTarget(gl, 1);

      const probe = document.createElement('canvas').getContext('2d', { willReadFrequently: true });
      const colorCache = new Map();
      const parse = value => {
        const key = String(value);
        if (colorCache.has(key)) return colorCache.get(key);
        let rgb = [0, 0, 0];
        if (probe) {
          probe.clearRect(0, 0, 1, 1);
          probe.fillStyle = key;
          probe.fillRect(0, 0, 1, 1);
          const [r, g, b] = probe.getImageData(0, 0, 1, 1).data;
          rgb = [r / 255, g / 255, b / 255];
        }
        colorCache.set(key, rgb);
        return rgb;
      };

      const view = { width: 1, height: 1, ratio: 1 };
      const ramp = new Float32Array(MAX_COLORS * 3);
      const clickData = new Float32Array(MAX_CLICKS * 4);
      const clicks = [];
      const trailData = new Float32Array(MAX_TRAIL * 4);
      const trail = [];
      const pointer = { x: 0, y: 0, inside: false };

      let atlas = null;
      let atlasKey = '';
      let paletteKey = '';
      let time = 0;
      let glyphTime = 0;
      let introClock = 0;
      let frameSeed = 0;
      let grainClock = 0;
      let raf = 0;
      let last = 0;

      const syncAtlas = () => {
        const key = asciiSettings.fontFamily + '|' + asciiSettings.characters;
        if (key === atlasKey && atlas) return;
        atlasKey = key;
        if (atlas) gl.deleteTexture(atlas.texture);
        atlas = buildAtlas(gl, asciiSettings.characters, asciiSettings.fontFamily);
      };

      const syncPalette = () => {
        const key = asciiSettings.colors.join(',');
        if (key === paletteKey) return;
        paletteKey = key;
        asciiSettings.colors.forEach((color, i) => ramp.set(toLab(parse(color)), i * 3));
      };

      const bindGlyphs = (program, width, height) => {
        if (!atlas) return;
        const u = program.uniforms;
        const cellH = asciiSettings.cellSize * view.ratio;
        const cellW = cellH * (ATLAS_CELL_W / ATLAS_CELL_H);
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, fieldTarget.textures[0]);
        gl.uniform1i(u.uField, 0);
        gl.activeTexture(gl.TEXTURE1);
        gl.bindTexture(gl.TEXTURE_2D, fieldTarget.textures[1]);
        gl.uniform1i(u.uAux, 1);
        gl.activeTexture(gl.TEXTURE2);
        gl.bindTexture(gl.TEXTURE_2D, atlas.texture);
        gl.uniform1i(u.uAtlas, 2);
        gl.uniform2f(u.uResolution, width, height);
        gl.uniform2f(u.uCell, cellW, cellH);
        gl.uniform2f(
          u.uOffset,
          (width - Math.ceil(width / cellW) * cellW) / 2,
          (height - Math.ceil(height / cellH) * cellH) / 2
        );
        gl.uniform2f(u.uAtlasGrid, atlas.grid[0], atlas.grid[1]);
        gl.uniform1f(u.uCount, atlas.count);
        gl.uniform1f(u.uDensity, asciiSettings.density);
        gl.uniform1f(u.uOpacity, asciiSettings.glyphOpacity);
        gl.uniform1f(u.uFlicker, asciiSettings.flicker);
        gl.uniform1f(u.uDepth, asciiSettings.depthOfField);
        gl.uniform1f(u.uGlyphTime, glyphTime);
        gl.uniform1f(u.uSeedValue, asciiSettings.variation);
        gl.uniform1f(u.uLight, asciiSettings.lightMode ? 1 : 0);
      };

      const draw = () => {
        const aspect = view.width / view.height;
        syncAtlas();
        syncPalette();
        gl.bindVertexArray(vao);
        gl.disable(gl.BLEND);

        gl.bindFramebuffer(gl.FRAMEBUFFER, fieldTarget.framebuffer);
        gl.drawBuffers([gl.COLOR_ATTACHMENT0, gl.COLOR_ATTACHMENT1]);
        gl.viewport(0, 0, fieldTarget.width, fieldTarget.height);
        gl.useProgram(field.program);
        const fu = field.uniforms;
        gl.uniform1f(fu.uAspect, aspect);
        gl.uniform1f(fu.uTime, time);
        gl.uniform1i(fu.uShape, asciiSettings.shape);
        gl.uniform3fv(fu.uRamp, ramp);
        gl.uniform1i(fu.uCount, asciiSettings.colors.length);
        gl.uniform1f(fu.uScale, asciiSettings.scale);
        gl.uniform1f(fu.uSoftness, asciiSettings.softness);
        gl.uniform1f(fu.uFlow, asciiSettings.flow);
        gl.uniform2f(fu.uSeed, 0, 0);
        gl.uniform4fv(fu.uTrail, trailData);
        gl.uniform1f(fu.uRadius, asciiSettings.scrambleRadius);
        gl.uniform4fv(fu.uClicks, clickData);
        gl.uniform1i(fu.uClickEffect, asciiSettings.clickEffect);
        gl.uniform1f(fu.uIntro, introClock);
        gl.uniform1f(fu.uFrame, frameSeed);
        gl.drawArrays(gl.TRIANGLES, 0, 3);

        gl.bindFramebuffer(gl.FRAMEBUFFER, haloA.framebuffer);
        gl.drawBuffers([gl.COLOR_ATTACHMENT0]);
        gl.viewport(0, 0, haloA.width, haloA.height);
        gl.useProgram(halo.program);
        bindGlyphs(halo, canvas.width, canvas.height);
        gl.uniform1f(halo.uniforms.uDownscale, canvas.width / haloA.width);
        gl.drawArrays(gl.TRIANGLES, 0, 3);

        gl.useProgram(blur.program);
        gl.activeTexture(gl.TEXTURE0);
        gl.uniform1i(blur.uniforms.uSource, 0);
        gl.bindFramebuffer(gl.FRAMEBUFFER, haloB.framebuffer);
        gl.bindTexture(gl.TEXTURE_2D, haloA.textures[0]);
        gl.uniform2f(blur.uniforms.uStep, 1 / haloA.width, 0);
        gl.drawArrays(gl.TRIANGLES, 0, 3);
        gl.bindFramebuffer(gl.FRAMEBUFFER, haloA.framebuffer);
        gl.bindTexture(gl.TEXTURE_2D, haloB.textures[0]);
        gl.uniform2f(blur.uniforms.uStep, 0, 1 / haloA.height);
        gl.drawArrays(gl.TRIANGLES, 0, 3);

        gl.bindFramebuffer(gl.FRAMEBUFFER, null);
        gl.viewport(0, 0, canvas.width, canvas.height);
        gl.useProgram(composite.program);
        bindGlyphs(composite, canvas.width, canvas.height);
        const cu = composite.uniforms;
        gl.activeTexture(gl.TEXTURE3);
        gl.bindTexture(gl.TEXTURE_2D, haloA.textures[0]);
        gl.uniform1i(cu.uHalo, 3);
        gl.uniform1f(cu.uGlow, asciiSettings.glow);
        gl.uniform1f(cu.uGradient, asciiSettings.gradientOpacity);
        gl.uniform1f(cu.uGrain, asciiSettings.grain);
        gl.uniform1f(cu.uFrame, frameSeed);
        gl.drawArrays(gl.TRIANGLES, 0, 3);
      };

      const resize = () => {
        view.width = Math.max(1, container.clientWidth);
        view.height = Math.max(1, container.clientHeight);
        view.ratio = Math.min(window.devicePixelRatio || 1, 2);
        canvas.width = Math.max(1, Math.round(view.width * view.ratio));
        canvas.height = Math.max(1, Math.round(view.height * view.ratio));
        sizeTarget(gl, fieldTarget, Math.max(1, Math.round(canvas.width / 2)), Math.max(1, Math.round(canvas.height / 2)));
        sizeTarget(gl, haloA, Math.max(1, Math.round(canvas.width / 4)), Math.max(1, Math.round(canvas.height / 4)));
        sizeTarget(gl, haloB, Math.max(1, Math.round(canvas.width / 4)), Math.max(1, Math.round(canvas.height / 4)));
        draw();
      };

      const updateInput = dt => {
        trail.forEach(p => (p.age += dt));
        while (trail.length && trail[trail.length - 1].age > TRAIL_LIFE) trail.pop();
        for (let i = 0; i < MAX_TRAIL; i++) {
          const p = trail[i];
          if (!p) {
            trailData[i * 4 + 2] = 0;
            continue;
          }
          const k = 1 - p.age / TRAIL_LIFE;
          trailData.set([p.x, p.y, k * k, 0], i * 4);
        }
        clicks.forEach(c => (c.age += dt));
        while (clicks.length && clicks[clicks.length - 1].age > CLICK_LIFE) clicks.pop();
        for (let i = 0; i < MAX_CLICKS; i++) {
          const c = clicks[i];
          if (!c) {
            clickData[i * 4 + 3] = 0;
            continue;
          }
          const k = c.age / CLICK_LIFE;
          const envelope = Math.min(1, c.age / 0.08) * (1 - k) * (1 - k);
          clickData.set([c.x, c.y, c.age, envelope], i * 4);
        }
      };

      const frame = now => {
        raf = 0;
        const dt = last ? clamp((now - last) / 1000, 0.001, 0.05) : 1 / 60;
        last = now;
        time += dt * asciiSettings.speed * 0.5;
        glyphTime += dt * Math.max(asciiSettings.speed, 0.15);
        if (introClock < 1) introClock = Math.min(1, introClock + dt / INTRO_SECONDS);
        grainClock += dt;
        if (grainClock > 1 / 24) {
          grainClock = 0;
          frameSeed = (frameSeed + 1) % 97;
        }
        updateInput(dt);
        draw();
        raf = requestAnimationFrame(frame);
      };

      const wake = () => {
        if (!raf) raf = requestAnimationFrame(frame);
      };
      asciiWake = wake;

      const locate = e => {
        const rect = container.getBoundingClientRect();
        const x = (e.clientX - rect.left) / Math.max(1, rect.width);
        const y = 1 - (e.clientY - rect.top) / Math.max(1, rect.height);
        const aspect = rect.width / Math.max(1, rect.height);
        pointer.inside = x >= 0 && x <= 1 && y >= 0 && y <= 1;
        pointer.x = (x - 0.5) * aspect;
        pointer.y = y - 0.5;
      };

      window.addEventListener('pointermove', e => {
        locate(e);
        if (!pointer.inside) return;
        const head = trail[0];
        const gap = head ? Math.hypot(pointer.x - head.x, pointer.y - head.y) : Infinity;
        if (gap < TRAIL_STEP) return;
        trail.unshift({ x: pointer.x, y: pointer.y, age: 0 });
        trail.length = Math.min(trail.length, MAX_TRAIL);
      }, { passive: true });

      window.addEventListener('pointerdown', e => {
        locate(e);
        if (!pointer.inside) return;
        clicks.unshift({ x: pointer.x, y: pointer.y, age: 0 });
        if (clicks.length > MAX_CLICKS) clicks.pop();
      }, { passive: true });

      new ResizeObserver(resize).observe(container);
      resize();
      wake();
    }

    window.addEventListener('DOMContentLoaded', initGradientASCII);
  </script>
</body>
</html>"""

/**
 * 顶部假名流动天幕背景
 * 100% 运行原版 WebGL2 原生渲染引擎（包含 4 重 RenderPass、双向高斯模糊辉光与胶片颗粒）
 * 达成绝对 100.00% 像素级无缝复刻
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KanaWaveBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(isDark) {
        webViewRef.value?.evaluateJavascript("if (window.setTheme) window.setTheme($isDark);", null)
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    allowFileAccess = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript("if (window.setTheme) window.setTheme($isDark);", null)
                    }
                }
                loadDataWithBaseURL("https://nemo.local/", KANA_WAVE_HTML, "text/html", "UTF-8", null)
                webViewRef.value = this
            }
        },
        update = { webView ->
            webView.evaluateJavascript("if (window.setTheme) window.setTheme($isDark);", null)
        },
        onRelease = { webView ->
            webView.destroy()
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Material 3 (MD3) 官方标准输入框组件（带 14dp 圆角与项目弥散软阴影）
 */
@Composable
fun AppStyleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    enabled: Boolean = true
) {
    val containerBg = if (isDark) Color(0xFF16151B) else Color.White
    val focusedBorder = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF0F172A)
    val unfocusedBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val placeholderColor = if (isDark) Color.White.copy(alpha = 0.40f) else Color(0xFF94A3B8)
    val iconColor = if (isDark) Color.White.copy(alpha = 0.50f) else Color(0xFF94A3B8)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 14.dp, isDark = isDark),
        enabled = enabled,
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(14.dp),
        textStyle = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        ),
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = placeholderColor
            )
        },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )
        },
        trailingIcon = if (trailingIcon != null) {
            {
                IconButton(
                    onClick = { onTrailingIconClick?.invoke() },
                    enabled = enabled
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        } else null,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerBg,
            unfocusedContainerColor = containerBg,
            disabledContainerColor = containerBg,
            errorContainerColor = containerBg,
            focusedBorderColor = focusedBorder,
            unfocusedBorderColor = unfocusedBorder,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            cursorColor = textColor
        )
    )
}

/**
 * 登录表单内容：邮箱、密码、横向双按钮（忘记密码 + 立即登录）、Google登录
 * 全部按钮均注入弥散软阴影
 */
@Composable
fun AppLoginForm(
    email: String,
    password: String,
    isPasswordVisible: Boolean,
    isDark: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLogin: () -> Unit,
    isLoading: Boolean,
    emailFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    onForgotPasswordClick: () -> Unit,
    emailError: Boolean = false,
    passwordError: Boolean = false,
    onGoogleLoginClick: () -> Unit,
    onFocusFieldChange: (FocusedAuthField?) -> Unit = {},
    backdrop: Backdrop? = null
) {
    val primaryGlassTint = if (isDark) Color(0xFF2563EB) else Color(0xFF0E68FF)

    val secondaryBtnText = if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xFF1E293B)
    val googleBtnText = if (isDark) Color.White else Color(0xFF1E293B)

    val dividerColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0)
    val dividerTextColor = if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF94A3B8)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 电子邮箱输入框（MD3 标准组件 + 弥散阴影）
        AppStyleTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "电子邮箱地址",
            leadingIcon = Icons.Rounded.Mail,
            isDark = isDark,
            isError = emailError,
            enabled = !isLoading,
            modifier = Modifier
                .focusRequester(emailFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.LOGIN_EMAIL) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        // 2. 密码输入框（MD3 标准组件 + 弥散阴影）
        AppStyleTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "输入登录密码",
            leadingIcon = Icons.Rounded.Lock,
            trailingIcon = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            onTrailingIconClick = onTogglePasswordVisibility,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isDark = isDark,
            isError = passwordError,
            enabled = !isLoading,
            modifier = Modifier
                .focusRequester(passwordFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.LOGIN_PASSWORD) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onLogin() }
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 3. 核心横向双按钮：左侧带底色磨砂液态玻璃「忘记密码」，右侧彩色液态玻璃「立即登录 >」
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：没颜色选用「带底色磨砂液态玻璃按钮」
            FrostedLiquidGlassButton(
                onClick = { if (!isLoading) onForgotPasswordClick() },
                backdrop = backdrop,
                isInteractive = !isLoading,
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "忘记密码",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryBtnText
                    )
                }
            }

            // 右侧：有颜色选用「彩色液态玻璃主按钮」
            ColoredLiquidGlassButton(
                onClick = { if (!isLoading) onLogin() },
                backdrop = backdrop,
                tint = primaryGlassTint,
                isInteractive = !isLoading,
                shape = RoundedCornerShape(15.dp),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .weight(1.35f)
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        NemoChasingDotsLoader(size = 22.dp, color = Color.White)
                    } else {
                        Text(
                            text = "立即登录",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 4. 分隔线「或」
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor,
                thickness = 1.dp
            )
            Text(
                text = "或",
                modifier = Modifier.padding(horizontal = 14.dp),
                color = dividerTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor,
                thickness = 1.dp
            )
        }

        // 5. 没颜色选用「带底色磨砂液态玻璃按钮」Google 快捷登录
        FrostedLiquidGlassButton(
            onClick = { if (!isLoading) onGoogleLoginClick() },
            backdrop = backdrop,
            isInteractive = !isLoading,
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = GoogleIcon,
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "使用 Google 账号登录",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = googleBtnText
                )
            }
        }
    }
}

/**
 * 注册表单内容：昵称、邮箱、密码、确认密码、全宽【立即注册 >】、Google快捷注册
 * 全部按钮均注入弥散软阴影
 */
@Composable
fun AppRegisterForm(
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
    isPasswordVisible: Boolean,
    isConfirmPasswordVisible: Boolean,
    isDark: Boolean,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onRegister: () -> Unit,
    isLoading: Boolean,
    usernameFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    confirmPasswordFocusRequester: FocusRequester,
    usernameError: Boolean = false,
    emailError: Boolean = false,
    passwordError: Boolean = false,
    confirmPasswordError: Boolean = false,
    onGoogleLoginClick: () -> Unit,
    onFocusFieldChange: (FocusedAuthField?) -> Unit = {},
    backdrop: Backdrop? = null
) {
    val primaryGlassTint = if (isDark) Color(0xFF2563EB) else Color(0xFF0E68FF)
    val googleBtnText = if (isDark) Color.White else Color(0xFF1E293B)

    val dividerColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0)
    val dividerTextColor = if (isDark) Color.White.copy(alpha = 0.35f) else Color(0xFF94A3B8)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 您的称呼（选填）
        AppStyleTextField(
            value = username,
            onValueChange = onUsernameChange,
            placeholder = "您的称呼（选填）",
            leadingIcon = Icons.Rounded.Person,
            isDark = isDark,
            isError = usernameError,
            enabled = !isLoading,
            modifier = Modifier
                .focusRequester(usernameFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.REGISTER_USERNAME) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        // 2. 电子邮箱地址
        AppStyleTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = "电子邮箱地址",
            leadingIcon = Icons.Rounded.Mail,
            isDark = isDark,
            isError = emailError,
            enabled = !isLoading,
            modifier = Modifier.onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.REGISTER_EMAIL) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )

        // 3. 设置密码
        AppStyleTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "设置密码（至少 6 位）",
            leadingIcon = Icons.Rounded.Lock,
            trailingIcon = if (isPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            onTrailingIconClick = onTogglePasswordVisibility,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isDark = isDark,
            isError = passwordError,
            enabled = !isLoading,
            modifier = Modifier
                .focusRequester(passwordFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.REGISTER_PASSWORD) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            )
        )

        // 4. 确认密码
        AppStyleTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = "确认密码",
            leadingIcon = Icons.Rounded.Lock,
            trailingIcon = if (isConfirmPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            onTrailingIconClick = onToggleConfirmPasswordVisibility,
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isDark = isDark,
            isError = confirmPasswordError,
            enabled = !isLoading,
            modifier = Modifier
                .focusRequester(confirmPasswordFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusFieldChange(FocusedAuthField.REGISTER_CONFIRM_PASSWORD) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onRegister() }
            )
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 5. 有颜色选用「彩色液态玻璃主按钮」立即注册
        ColoredLiquidGlassButton(
            onClick = { if (!isLoading) onRegister() },
            backdrop = backdrop,
            tint = primaryGlassTint,
            isInteractive = !isLoading,
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    NemoChasingDotsLoader(size = 22.dp, color = Color.White)
                } else {
                    Text(
                        text = "立即注册",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 6. 分隔线「或」
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor,
                thickness = 1.dp
            )
            Text(
                text = "或",
                modifier = Modifier.padding(horizontal = 14.dp),
                color = dividerTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = dividerColor,
                thickness = 1.dp
            )
        }

        // 7. 没颜色选用「带底色磨砂液态玻璃按钮」Google 账号快捷注册
        FrostedLiquidGlassButton(
            onClick = { if (!isLoading) onGoogleLoginClick() },
            backdrop = backdrop,
            isInteractive = !isLoading,
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = GoogleIcon,
                    contentDescription = "Google",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "使用 Google 账号快捷注册",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = googleBtnText
                )
            }
        }
    }
}

/**
 * 兼容旧接口的 LoginForm 与 RegisterForm
 */
@Composable
fun LoginForm(
    email: String,
    password: String,
    isPasswordVisible: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLogin: () -> Unit,
    isLoading: Boolean,
    emailFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    onForgotPasswordClick: () -> Unit,
    emailError: Boolean = false,
    passwordError: Boolean = false,
    onGoogleLoginClick: () -> Unit
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    AppLoginForm(
        email = email,
        password = password,
        isPasswordVisible = isPasswordVisible,
        isDark = isDark,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
        onLogin = onLogin,
        isLoading = isLoading,
        emailFocusRequester = emailFocusRequester,
        passwordFocusRequester = passwordFocusRequester,
        onForgotPasswordClick = onForgotPasswordClick,
        emailError = emailError,
        passwordError = passwordError,
        onGoogleLoginClick = onGoogleLoginClick
    )
}

@Composable
fun RegisterForm(
    username: String,
    email: String,
    password: String,
    confirmPassword: String,
    isPasswordVisible: Boolean,
    isConfirmPasswordVisible: Boolean,
    onUsernameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onRegister: () -> Unit,
    isLoading: Boolean,
    usernameFocusRequester: FocusRequester,
    passwordFocusRequester: FocusRequester,
    confirmPasswordFocusRequester: FocusRequester,
    usernameError: Boolean = false,
    emailError: Boolean = false,
    passwordError: Boolean = false,
    confirmPasswordError: Boolean = false
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    AppRegisterForm(
        username = username,
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        isPasswordVisible = isPasswordVisible,
        isConfirmPasswordVisible = isConfirmPasswordVisible,
        isDark = isDark,
        onUsernameChange = onUsernameChange,
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onConfirmPasswordChange = onConfirmPasswordChange,
        onTogglePasswordVisibility = onTogglePasswordVisibility,
        onToggleConfirmPasswordVisibility = onToggleConfirmPasswordVisibility,
        onRegister = onRegister,
        isLoading = isLoading,
        usernameFocusRequester = usernameFocusRequester,
        passwordFocusRequester = passwordFocusRequester,
        confirmPasswordFocusRequester = confirmPasswordFocusRequester,
        usernameError = usernameError,
        emailError = emailError,
        passwordError = passwordError,
        confirmPasswordError = confirmPasswordError,
        onGoogleLoginClick = {}
    )
}
