package com.jian.nemo.feature.learning.presentation.ai.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jian.nemo.core.designsystem.theme.LearningPalette
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * React Bits 官方 Aurora 原型预设色彩库
 */
data class AuroraPreset(
    val id: String,
    val name: String,
    val color1: Color,
    val color2: Color,
    val color3: Color
)

val AuroraPresets = listOf(
    // 1. 经典极光 (Classic Neon) - 官方标志性电光紫与霓虹青绿
    AuroraPreset(
        id = "classic",
        name = "经典极光",
        color1 = LearningPalette.AuroraClassic1,
        color2 = LearningPalette.AuroraClassic2,
        color3 = LearningPalette.AuroraClassic1
    ),
    // 2. 霓虹天际 (Nordic Sky) - React Bits 官方精选 (深紫/樱花洋红/晴空青)
    AuroraPreset(
        id = "nordic",
        name = "霓虹天际",
        color1 = LearningPalette.AuroraNordic1,
        color2 = LearningPalette.AuroraNordic2,
        color3 = LearningPalette.AuroraNordic3
    ),
    // 3. 极昼破晓 (Polar Dawn) - 皇家深蓝/晨曦柔粉/破晓赤红
    AuroraPreset(
        id = "polar_dawn",
        name = "极昼破晓",
        color1 = LearningPalette.AuroraPolar1,
        color2 = LearningPalette.AuroraPolar2,
        color3 = LearningPalette.AuroraPolar3
    ),
    // 4. 冰川青碧 (Glacier Turquoise) - 冰川天蓝/薄荷翡翠/琉璃靛蓝
    AuroraPreset(
        id = "glacier",
        name = "冰川青碧",
        color1 = LearningPalette.AuroraGlacier1,
        color2 = LearningPalette.AuroraGlacier2,
        color3 = LearningPalette.AuroraGlacier3
    ),
    // 5. 暮色紫霞 (Dusk Sunset) - 绚丽玫红/梦幻罗兰/深海蔚蓝
    AuroraPreset(
        id = "dusk",
        name = "暮色紫霞",
        color1 = LearningPalette.AuroraDusk1,
        color2 = LearningPalette.AuroraDusk2,
        color3 = LearningPalette.AuroraDusk3
    )
)

// ==============================================================================
// 100% 原版 WebGL2 极光引擎（完全对齐 code_artifact.html 原型规范）
// 架构完全对齐登录界面 KanaWaveBackground.kt：纯静态常量 HTML + evaluateJavascript
// 1. 包含 OKLCH 色彩空间转换、多层流动噪声与物理光晕渲染管线
// 2. 扎根底部升腾，支持手势滑动与触控跟随交互
// 3. 支持深浅色模式（uLightMode）实时无缝切换
// ==============================================================================

private const val AURORA_HTML = """<!DOCTYPE html>
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
    .aurora-container {
      position: relative;
      width: 100%;
      height: 100%;
      overflow: hidden;
    }
    .aurora-canvas {
      position: absolute;
      inset: 0;
      display: block;
      width: 100%;
      height: 100%;
    }
  </style>
</head>
<body>
  <div id="aurora-container" class="aurora-container">
    <canvas id="aurora-canvas" class="aurora-canvas" aria-hidden="true"></canvas>
  </div>
  <script>
    const MAX_STOPS = 5;
    const MAX_LAYERS = 3;
    const INTRO_SECONDS = 2.4;

    const VERTEX = '#version 300 es\n' +
      'in vec2 aPosition;\n' +
      'void main() {\n' +
      '  gl_Position = vec4(aPosition, 0.0, 1.0);\n' +
      '}';

    const FRAGMENT = '#version 300 es\n' +
      'precision highp float;\n' +
      'precision highp int;\n' +
      'uniform vec2 uResolution;\n' +
      'uniform float uTime;\n' +
      'uniform vec3 uStops[5];\n' +
      'uniform int uStopCount;\n' +
      'uniform float uAmplitude;\n' +
      'uniform float uBlend;\n' +
      'uniform float uHeight;\n' +
      'uniform float uScale;\n' +
      'uniform float uGlow;\n' +
      'uniform float uBrightness;\n' +
      'uniform int uLayers;\n' +
      'uniform float uFlip;\n' +
      'uniform vec3 uPointer;\n' +
      'uniform float uReveal;\n' +
      'uniform float uOpacity;\n' +
      'uniform float uLightMode;\n' +
      'out vec4 outColor;\n' +
      'vec3 permute(vec3 x) {\n' +
      '  return mod(((x * 34.0) + 1.0) * x, 289.0);\n' +
      '}\n' +
      'float snoise(vec2 v) {\n' +
      '  const vec4 C = vec4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);\n' +
      '  vec2 i = floor(v + dot(v, C.yy));\n' +
      '  vec2 x0 = v - i + dot(i, C.xx);\n' +
      '  vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);\n' +
      '  vec4 x12 = x0.xyxy + C.xxzz;\n' +
      '  x12.xy -= i1;\n' +
      '  i = mod(i, 289.0);\n' +
      '  vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0)) + i.x + vec3(0.0, i1.x, 1.0));\n' +
      '  vec3 m = max(0.5 - vec3(dot(x0, x0), dot(x12.xy, x12.xy), dot(x12.zw, x12.zw)), 0.0);\n' +
      '  m = m * m;\n' +
      '  m = m * m;\n' +
      '  vec3 x = 2.0 * fract(p * C.www) - 1.0;\n' +
      '  vec3 h = abs(x) - 0.5;\n' +
      '  vec3 ox = floor(x + 0.5);\n' +
      '  vec3 a0 = x - ox;\n' +
      '  m *= 1.79284291400159 - 0.85373472095314 * (a0 * a0 + h * h);\n' +
      '  vec3 g;\n' +
      '  g.x = a0.x * x0.x + h.x * x0.y;\n' +
      '  g.yz = a0.yz * x12.xz + h.yz * x12.yw;\n' +
      '  return 130.0 * dot(m, g);\n' +
      '}\n' +
      'float smootherstep(float edge0, float edge1, float x) {\n' +
      '  float t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);\n' +
      '  return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);\n' +
      '}\n' +
      'vec3 oklabToLinear(vec3 c) {\n' +
      '  float l = c.x + 0.3963377774 * c.y + 0.2158037573 * c.z;\n' +
      '  float m = c.x - 0.1055613458 * c.y - 0.0638541728 * c.z;\n' +
      '  float s = c.x - 0.0894841775 * c.y - 1.2914855480 * c.z;\n' +
      '  l = l * l * l; m = m * m * m; s = s * s * s;\n' +
      '  return vec3(\n' +
      '    4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,\n' +
      '    -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,\n' +
      '    -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s\n' +
      '  );\n' +
      '}\n' +
      'vec3 toSrgb(vec3 c) {\n' +
      '  c = clamp(c, 0.0, 1.0);\n' +
      '  return mix(c * 12.92, 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055, step(0.0031308, c));\n' +
      '}\n' +
      'vec3 ramp(float t) {\n' +
      '  float x = clamp(t, 0.0, 1.0) * float(uStopCount - 1);\n' +
      '  int i = int(min(floor(x), float(uStopCount - 2)));\n' +
      '  float f = x - float(i);\n' +
      '  f = f * f * (3.0 - 2.0 * f);\n' +
      '  vec3 lch = mix(uStops[i], uStops[i + 1], f);\n' +
      '  lch.x = mix(lch.x, min(lch.x, 0.7), uLightMode);\n' +
      '  return toSrgb(oklabToLinear(vec3(lch.x, lch.y * cos(lch.z), lch.y * sin(lch.z))));\n' +
      '}\n' +
      'float hash(vec2 p) {\n' +
      '  p = fract(p * vec2(123.34, 456.21));\n' +
      '  p += dot(p, p + 45.32);\n' +
      '  return fract(p.x * p.y);\n' +
      '}\n' +
      'void main() {\n' +
      '  vec2 frag = gl_FragCoord.xy;\n' +
      '  float y = 1.0 - frag.y / uResolution.y;\n' +
      '  y = mix(y, 1.0 - y, uFlip);\n' +
      '  float u = frag.x / uResolution.x;\n' +
      '  float aspect = min(uResolution.y, uResolution.x * 1.15);\n' +
      '  float X = (frag.x - uResolution.x * 0.5) / aspect;\n' +
      '  float t = uTime;\n' +
      '  float reach = uPointer.z * exp(-pow((X - uPointer.x) / 0.38, 2.0));\n' +
      '  vec3 glow = vec3(0.0);\n' +
      '  float cover = 0.0;\n' +
      '  for (int k = 0; k < 3; k++) {\n' +
      '    if (k >= uLayers) break;\n' +
      '    float layer = float(k);\n' +
      '    float seed = layer * 13.7;\n' +
      '    float pace = t * (1.0 - layer * 0.18);\n' +
      '    float warp = snoise(vec2(X * 0.45 * uScale + pace * 0.02 + seed, pace * 0.05 + seed));\n' +
      '    float wave = snoise(vec2(X * 0.75 * uScale + warp * 0.35 - pace * 0.03 + seed, pace * 0.07 - seed)) * 0.72\n' +
      '      + snoise(vec2(X * 1.6 * uScale + warp * 0.5 + pace * 0.04 - seed, pace * 0.1 + seed * 0.5)) * 0.28;\n' +
      '    float hem = uHeight + layer * 0.07 + wave * 0.15 * uAmplitude;\n' +
      '    hem += reach * 0.08 * (layer < 0.5 ? 1.0 : 0.4);\n' +
      '    hem = mix(-0.25, hem, uReveal);\n' +
      '    float soft = (0.04 + uBlend * 0.16) * (1.0 + layer * 0.9);\n' +
      '    float rise = hem - y;\n' +
      '    float low = -soft - uGlow * 0.14;\n' +
      '    float high = 0.26 + soft * 0.6;\n' +
      '    float body = pow(smootherstep(low, high, rise), 1.5 + uGlow * 0.6);\n' +
      '    float lobes = 0.86 + 0.14 * snoise(vec2(X * 0.9 * uScale - pace * 0.025 + seed, y * 0.3 + pace * 0.04));\n' +
      '    float alpha = body * lobes * (layer < 0.5 ? 1.0 : 0.32 / layer);\n' +
      '    alpha = clamp(alpha * (1.0 + reach * 0.25 * smootherstep(0.9, 0.0, abs(y - hem))), 0.0, 1.0);\n' +
      '    float colorNoise = snoise(vec2(u * 0.9 - pace * 0.08 + seed, y * 1.2 - pace * 0.06));\n' +
      '    float colorPos = u * 0.75 + (y - 0.5) * 0.45 + wave * 0.28 + rise * 0.35 + colorNoise * 0.28 + 0.22 * sin(pace * 0.18 + y * 2.5 + layer * 2.3) + layer * 0.15;\n' +
      '    vec3 tint = ramp(clamp(colorPos, 0.0, 1.0));\n' +
      '    glow = 1.0 - (1.0 - glow) * (1.0 - tint * alpha);\n' +
      '    cover = 1.0 - (1.0 - cover) * (1.0 - alpha);\n' +
      '  }\n' +
      '  float fade = smootherstep(0.0, 0.7, uReveal) * uOpacity;\n' +
      '  float strength = clamp(uBrightness, 0.0, 2.0);\n' +
      '  glow *= fade * min(strength, 1.0);\n' +
      '  cover *= fade * min(strength, 1.0);\n' +
      '  glow = 1.0 - pow(1.0 - glow, vec3(max(strength, 1.0)));\n' +
      '  float dither = (hash(frag) - 0.5) / 255.0;\n' +
      '  if (uLightMode > 0.5) {\n' +
      '    vec3 tint = cover > 0.0001 ? glow / cover : vec3(0.0);\n' +
      '    cover *= smootherstep(0.0, 0.25, cover);\n' +
      '    float alpha = cover * 0.7;\n' +
      '    outColor = vec4(tint * alpha + dither, clamp(alpha + dither, 0.0, 1.0));\n' +
      '    return;\n' +
      '  }\n' +
      '  outColor = vec4(glow + dither, clamp(cover + dither, 0.0, 1.0));\n' +
      '}';

    const clamp = (value, min, max) => Math.min(max, Math.max(min, value));

    const toOklch = rgb => {
      const [r, g, b] = rgb.map(c => (c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)));
      const l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
      const m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
      const s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
      const lab = [
        0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
        1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
        0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s
      ];
      return [lab[0], Math.hypot(lab[1], lab[2]), Math.atan2(lab[2], lab[1])];
    };

    const unwrapHues = stops => {
      const known = stops.findIndex(stop => stop[1] > 0.02);
      const anchor = known === -1 ? 0 : stops[known][2];
      let previous = anchor;
      return stops.map(stop => {
        let hue = stop[1] > 0.02 ? stop[2] : previous;
        while (hue - previous > Math.PI) hue -= Math.PI * 2;
        while (hue - previous < -Math.PI) hue += Math.PI * 2;
        previous = hue;
        return [stop[0], stop[1], hue];
      });
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

    const link = gl => {
      const vertex = compile(gl, gl.VERTEX_SHADER, VERTEX);
      const fragment = compile(gl, gl.FRAGMENT_SHADER, FRAGMENT);
      const program = gl.createProgram();
      if (!vertex || !fragment || !program) return null;
      gl.attachShader(program, vertex);
      gl.attachShader(program, fragment);
      gl.bindAttribLocation(program, 0, 'aPosition');
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
        if (info) uniforms[info.name.replace('[0]', '')] = gl.getUniformLocation(program, info.name);
      }
      return { program, uniforms };
    };

    let auroraWake = null;
    let auroraSettings = null;

    window.setTheme = function(isDark) {
      if (!auroraSettings) return;
      auroraSettings.lightMode = !isDark;
      if (isDark) {
        auroraSettings.stops = ['#5227FF', '#7cff67', '#5227FF'];
      } else {
        auroraSettings.stops = ['#4f46e5', '#10b981', '#3b82f6'];
      }
      if (auroraWake) auroraWake();
    };

    function initAurora() {
      const container = document.getElementById('aurora-container');
      const canvas = document.getElementById('aurora-canvas');
      if (!container || !canvas) return;

      const settings = {
        stops: ['#5227FF', '#7cff67', '#5227FF'],
        amplitude: 1.0,
        blend: 0.5,
        speed: 1.5,
        time: undefined,
        height: 0.50,
        scale: 1.0,
        glow: 0.6,
        brightness: 1.0,
        layers: 2,
        flip: 1.0, // direction: bottom (扎根底部)
        mouseInteraction: true,
        intro: true,
        opacity: 1.0,
        lightMode: false,
        paused: false
      };
      auroraSettings = settings;

      const gl = canvas.getContext('webgl2', {
        alpha: true,
        premultipliedAlpha: true,
        antialias: false,
        depth: false,
        stencil: false
      });
      if (!gl) return;

      const program = link(gl);
      if (!program) return;
      const u = program.uniforms;

      const buffer = gl.createBuffer();
      const vao = gl.createVertexArray();
      gl.bindVertexArray(vao);
      gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
      gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 3, -1, -1, 3]), gl.STATIC_DRAW);
      gl.enableVertexAttribArray(0);
      gl.vertexAttribPointer(0, 2, gl.FLOAT, false, 0, 0);

      const probe = document.createElement('canvas').getContext('2d', { willReadFrequently: true });
      const cache = new Map();
      const parse = value => {
        if (cache.has(value)) return cache.get(value);
        let rgb = [1, 1, 1];
        if (probe) {
          probe.clearRect(0, 0, 1, 1);
          probe.fillStyle = '#ffffff';
          probe.fillStyle = value;
          probe.fillRect(0, 0, 1, 1);
          const [r, g, b] = probe.getImageData(0, 0, 1, 1).data;
          rgb = [r / 255, g / 255, b / 255];
        }
        cache.set(value, rgb);
        return rgb;
      };
      const palette = new Float32Array(MAX_STOPS * 3);

      const view = { width: 1, height: 1 };
      const pointer = { x: 0, y: 0, inside: false };
      const state = {
        clock: 0,
        px: 0,
        py: 0.5,
        reach: 0,
        reveal: 0
      };
      let raf = 0;
      let last = 0;
      let visible = true;

      const render = () => {
        const s = settings;
        unwrapHues(s.stops.map(value => toOklch(parse(value)))).forEach((stop, index) => palette.set(stop, index * 3));
        const reveal = state.reveal;
        gl.viewport(0, 0, canvas.width, canvas.height);
        gl.useProgram(program.program);
        gl.bindVertexArray(vao);
        gl.uniform2f(u.uResolution, canvas.width, canvas.height);
        gl.uniform1f(u.uTime, s.time !== undefined ? s.time * s.speed * 0.1 : state.clock);
        gl.uniform3fv(u.uStops, palette);
        gl.uniform1i(u.uStopCount, s.stops.length);
        gl.uniform1f(u.uAmplitude, s.amplitude);
        gl.uniform1f(u.uBlend, s.blend);
        gl.uniform1f(u.uHeight, s.height);
        gl.uniform1f(u.uScale, s.scale);
        gl.uniform1f(u.uGlow, s.glow);
        gl.uniform1f(u.uBrightness, s.brightness);
        gl.uniform1i(u.uLayers, s.layers);
        gl.uniform1f(u.uFlip, s.flip);
        gl.uniform3f(u.uPointer, state.px, state.py, state.reach);
        gl.uniform1f(u.uReveal, 1 - Math.pow(1 - reveal, 3));
        gl.uniform1f(u.uOpacity, s.opacity);
        gl.uniform1f(u.uLightMode, s.lightMode ? 1.0 : 0.0);
        gl.drawArrays(gl.TRIANGLES, 0, 3);
      };

      const frame = now => {
        raf = 0;
        if (!visible || document.hidden) return;
        const s = settings;
        const dt = last ? clamp((now - last) / 1000, 0.0005, 0.05) : 1 / 60;
        last = now;
        if (s.paused) {
          state.reveal = 1;
          render();
          last = 0;
          return;
        }
        state.clock += dt * s.speed;
        const engaged = s.mouseInteraction && pointer.inside;
        const follow = 1 - Math.exp(-dt / 0.6);
        if (engaged) {
          const tx = (pointer.x - view.width / 2) / view.height;
          const ty = s.flip ? 1 - pointer.y / view.height : pointer.y / view.height;
          state.px += (tx - state.px) * follow;
          state.py += (ty - state.py) * follow;
        }
        state.reach += ((engaged ? 1 : 0) - state.reach) * (1 - Math.exp(-dt / (engaged ? 0.9 : 1.4)));
        if (!s.intro) state.reveal = 1;
        else if (state.reveal < 1) state.reveal = Math.min(1, state.reveal + dt / INTRO_SECONDS);
        render();
        raf = requestAnimationFrame(frame);
      };

      const wake = () => {
        if (!raf && visible && !document.hidden) raf = requestAnimationFrame(frame);
      };
      auroraWake = wake;

      const resize = () => {
        const s = settings;
        const width = Math.max(1, container.clientWidth);
        const height = Math.max(1, container.clientHeight);
        const ratio = Math.min(window.devicePixelRatio || 1, 2);
        view.width = width;
        view.height = height;
        canvas.width = Math.max(1, Math.round(width * ratio));
        canvas.height = Math.max(1, Math.round(height * ratio));
        render();
        wake();
      };

      const onPointerMove = event => {
        const rect = container.getBoundingClientRect();
        pointer.x = event.clientX - rect.left;
        pointer.y = event.clientY - rect.top;
        const wasInside = pointer.inside;
        pointer.inside = pointer.x >= 0 && pointer.y >= 0 && pointer.x <= rect.width && pointer.y <= rect.height;
        if (pointer.inside && !wasInside) {
          const s = settings;
          state.px = (pointer.x - rect.width / 2) / Math.max(1, rect.height);
          state.py = s.flip ? 1 - pointer.y / Math.max(1, rect.height) : pointer.y / Math.max(1, rect.height);
        }
        if (!settings.paused) wake();
      };

      const onPointerUp = () => { pointer.inside = false; };
      const onPointerLeave = () => { pointer.inside = false; };

      const resizeObserver = new ResizeObserver(resize);
      resizeObserver.observe(container);
      window.addEventListener('pointermove', onPointerMove, { passive: true });
      window.addEventListener('pointerdown', onPointerMove, { passive: true });
      window.addEventListener('pointerup', onPointerUp, { passive: true });
      window.addEventListener('pointercancel', onPointerUp, { passive: true });
      document.documentElement.addEventListener('pointerleave', onPointerLeave);
      document.addEventListener('visibilitychange', () => {
        last = 0;
        wake();
      });
      resize();
    }

    window.addEventListener('DOMContentLoaded', initAurora);
  </script>
</body>
</html>"""

/**
 * AI 思考极光背景组件 (100% 对齐 code_artifact.html 原型规范)
 * 采用与登录界面 KanaWaveBackground 完全一致的生产级 WebView 架构与无缝通信
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AIAuroraBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    containerBg: Color? = null,
    height: Dp? = null
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(isDark) {
        webViewRef.value?.evaluateJavascript("if (window.setTheme) window.setTheme($isDark);", null)
    }

    val finalModifier = if (height != null) {
        modifier.height(height)
    } else {
        modifier.fillMaxSize()
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
                loadDataWithBaseURL("https://nemo.local/", AURORA_HTML, "text/html", "UTF-8", null)
                webViewRef.value = this
            }
        },
        update = { webView ->
            webView.evaluateJavascript("if (window.setTheme) window.setTheme($isDark);", null)
        },
        onRelease = { webView ->
            webView.destroy()
        },
        modifier = finalModifier
    )
}


