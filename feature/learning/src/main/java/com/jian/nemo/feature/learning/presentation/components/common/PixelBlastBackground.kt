package com.jian.nemo.feature.learning.presentation.components.common

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

// ==============================================================================
// 100% 原版 WebGL 2 PixelBlast 粒子波纹背景引擎
// 架构完全对齐登录界面 KanaWaveBackground.kt：纯静态常量 HTML + evaluateJavascript
// 1. 包含 Bayer 8x8 空间抖动、FBM 流体噪点、粒子网格变幻以及物理波动衰减管线
// 2. 具备触控波纹互动能力，可根据点击位置与时间进行物理波纹扩散
// 3. 支持深色/浅色模式实时调色无缝同步
// ==============================================================================

private const val PIXEL_BLAST_HTML = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no" />
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
    html, body {
      width: 100%;
      height: 100%;
      overflow: hidden;
      background: transparent !important;
      user-select: none;
      -webkit-user-select: none;
      touch-action: none;
    }
    .pixel-blast-container {
      position: absolute;
      inset: 0;
      width: 100%;
      height: 100%;
      overflow: hidden;
    }
    .pixel-blast-canvas {
      position: absolute;
      inset: 0;
      display: block;
      width: 100%;
      height: 100%;
    }
  </style>
</head>
<body>
  <div id="pixelBlastContainer" class="pixel-blast-container">
    <canvas id="pixelBlastCanvas" class="pixel-blast-canvas"></canvas>
  </div>
  <script>
    const MAX_CLICKS = 10;
    const INTRO_SECONDS = 1.8;
    const SHAPES = { square: 0, circle: 1, triangle: 2, diamond: 3, cross: 4, ring: 5 };
    const HOVERS = { none: 0, grow: 1, reveal: 2 };

    const bayer2 = (x, y) => (Math.floor(x) / 2 + Math.floor(y) * Math.floor(y) * 0.75) % 1;
    const bayer8 = (x, y) => (bayer2(x / 4, y / 4) * 0.25 + bayer2(x / 2, y / 2)) * 0.25 + bayer2(x, y);
    const BAYER_TABLE = Array.from({ length: 64 }, (_, i) => bayer8(i % 8, Math.floor(i / 8)).toFixed(6)).join(', ');

    const VERTEX = '#version 300 es\n' +
      'void main() {\n' +
      '  vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2));\n' +
      '  gl_Position = vec4(p * 2.0 - 1.0, 0.0, 1.0);\n' +
      '}';

    const PACKING = '' +
      'const float RANGE_MIN = -6.0;\n' +
      'const float RANGE_MAX = 10.0;\n' +
      'vec2 pack16(float value) {\n' +
      '  float x = floor(clamp((value - RANGE_MIN) / (RANGE_MAX - RANGE_MIN), 0.0, 1.0) * 65535.0 + 0.5);\n' +
      '  float hi = floor(x / 256.0);\n' +
      '  return vec2(hi, x - hi * 256.0) / 255.0;\n' +
      '}\n' +
      'float unpack16(vec2 c) {\n' +
      '  float x = floor(c.x * 255.0 + 0.5) * 256.0 + floor(c.y * 255.0 + 0.5);\n' +
      '  return RANGE_MIN + x / 65535.0 * (RANGE_MAX - RANGE_MIN);\n' +
      '}\n';

    const CELL_FRAGMENT = '#version 300 es\n' +
      'precision highp float;\n' +
      'precision highp int;\n' +
      'uniform vec2 uResolution;\n' +
      'uniform float uTime;\n' +
      'uniform float uPixelSize;\n' +
      'uniform float uScale;\n' +
      'uniform float uDensity;\n' +
      'uniform int uEnableRipples;\n' +
      'uniform float uRippleSpeed;\n' +
      'uniform float uRippleThickness;\n' +
      'uniform float uRippleIntensity;\n' +
      'uniform vec2 uClickPos[10];\n' +
      'uniform float uClickAge[10];\n' +
      'uniform ivec2 uOrigin;\n' +
      'uniform vec2 uDrift;\n' +
      'uniform int uHover;\n' +
      'uniform vec2 uMouse;\n' +
      'uniform float uMouseRadius;\n' +
      'uniform float uMousePresence;\n' +
      'uniform float uIntro;\n' +
      'uniform sampler2D uPrev;\n' +
      'uniform float uDecay;\n' +
      'uniform float uFresh;\n' +
      'out vec4 outCell;\n' +
      PACKING +
      'float hash11(float n) { return fract(sin(n) * 43758.5453); }\n' +
      'float vnoise(vec3 p) {\n' +
      '  vec3 ip = floor(p);\n' +
      '  vec3 fp = fract(p);\n' +
      '  float n000 = hash11(dot(ip + vec3(0.0, 0.0, 0.0), vec3(1.0, 57.0, 113.0)));\n' +
      '  float n100 = hash11(dot(ip + vec3(1.0, 0.0, 0.0), vec3(1.0, 57.0, 113.0)));\n' +
      '  float n010 = hash11(dot(ip + vec3(0.0, 1.0, 0.0), vec3(1.0, 57.0, 113.0)));\n' +
      '  float n110 = hash11(dot(ip + vec3(1.0, 1.0, 0.0), vec3(1.0, 57.0, 113.0)));\n' +
      '  float n001 = hash11(dot(ip + vec3(0.0, 0.0, 1.0), vec3(1.0, 57.0, 113.0)));\n' +
      '  float n101 = hash11(dot(ip + vec3(1.0, 0.0, 1.0), vec3(1.0, 57.0, 113.0)));\n' +
      '  float n011 = hash11(dot(ip + vec3(0.0, 1.0, 1.0), vec3(1.0, 57.0, 113.0)));\n' +
      '  float n111 = hash11(dot(ip + vec3(1.0, 1.0, 1.0), vec3(1.0, 57.0, 113.0)));\n' +
      '  vec3 w = fp * fp * fp * (fp * (fp * 6.0 - 15.0) + 10.0);\n' +
      '  float x00 = mix(n000, n100, w.x);\n' +
      '  float x10 = mix(n010, n110, w.x);\n' +
      '  float x01 = mix(n001, n101, w.x);\n' +
      '  float x11 = mix(n011, n111, w.x);\n' +
      '  float y0 = mix(x00, x10, w.y);\n' +
      '  float y1 = mix(x01, x11, w.y);\n' +
      '  return mix(y0, y1, w.z) * 2.0 - 1.0;\n' +
      '}\n' +
      'float fbm2(vec2 uv, float t) {\n' +
      '  vec3 p = vec3(uv * uScale, t);\n' +
      '  float amp = 1.0;\n' +
      '  float freq = 1.0;\n' +
      '  float sum = 1.0;\n' +
      '  for (int i = 0; i < 5; ++i) {\n' +
      '    sum += amp * vnoise(p * freq);\n' +
      '    freq *= 1.25;\n' +
      '    amp *= 1.0;\n' +
      '  }\n' +
      '  return sum * 0.5 + 0.5;\n' +
      '}\n' +
      'void main() {\n' +
      '  ivec2 texel = ivec2(gl_FragCoord.xy);\n' +
      '  float aspectRatio = uResolution.x / uResolution.y;\n' +
      '  float cellPixelSize = 8.0 * uPixelSize;\n' +
      '  vec2 cellId = vec2(texel + uOrigin);\n' +
      '  vec2 cellCoord = cellId * cellPixelSize;\n' +
      '  vec2 uv = cellCoord / uResolution * vec2(aspectRatio, 1.0);\n' +
      '  float base = fbm2(uv + uDrift, uTime * 0.025);\n' +
      '  base = (base - 0.5) * 0.28 - 0.12;\n' +
      '  float feed = base + (uDensity - 0.5) * 0.25;\n' +
      '  if (uEnableRipples == 1) {\n' +
      '    for (int i = 0; i < 10; ++i) {\n' +
      '      vec2 pos = uClickPos[i];\n' +
      '      if (pos.x < 0.0) continue;\n' +
      '      vec2 cuv = (((pos - uResolution * 0.5 - cellPixelSize * 0.5) / uResolution)) * vec2(aspectRatio, 1.0);\n' +
      '      float t = max(uClickAge[i], 0.0);\n' +
      '      if (t > 2.2) continue;\n' +
      '      float r = distance(uv, cuv);\n' +
      '      float waveR = uRippleSpeed * t;\n' +
      '      float ring = exp(-pow((r - waveR) / max(uRippleThickness, 0.035), 2.0));\n' +
      '      float atten = exp(-0.85 * t) * (1.0 / (1.0 + 2.0 * r));\n' +
      '      feed = max(feed, base + ring * atten * uRippleIntensity);\n' +
      '    }\n' +
      '  }\n' +
      '  if (uIntro < 1.0) {\n' +
      '    vec2 center = (-cellPixelSize * 0.5 / uResolution) * vec2(aspectRatio, 1.0);\n' +
      '    float reach = length(vec2(aspectRatio, 1.0)) * 0.5 + 0.2;\n' +
      '    float eased = 1.0 - pow(1.0 - uIntro, 2.4);\n' +
      '    float front = eased * reach;\n' +
      '    float r = distance(uv, center);\n' +
      '    float shown = smoothstep(front + 0.04, front - 0.06, r);\n' +
      '    float ring = exp(-pow((r - front) / 0.07, 2.0)) * (1.0 - uIntro);\n' +
      '    feed = mix(-1.0, feed, shown);\n' +
      '    feed = max(feed, ring * 1.4);\n' +
      '  }\n' +
      '  float brush = 0.0;\n' +
      '  if (uHover > 0) {\n' +
      '    vec2 d = (cellCoord + cellPixelSize * 0.5 + uResolution * 0.5 - uMouse) / max(uMouseRadius, 1.0);\n' +
      '    brush = exp(-dot(d, d)) * uMousePresence;\n' +
      '  }\n' +
      '  vec4 prev = texelFetch(uPrev, texel, 0) * (1.0 - uFresh);\n' +
      '  float trail = max(brush, max(prev.a * uDecay - 1.0 / 255.0, 0.0));\n' +
      '  if (uHover == 2) feed += trail * 0.9;\n' +
      '  outCell = vec4(pack16(feed), clamp((feed + 0.5) * 0.5, 0.0, 1.0), trail);\n' +
      '}';

    const MAIN_FRAGMENT = '#version 300 es\n' +
      'precision highp float;\n' +
      'precision highp int;\n' +
      'uniform vec3 uColor;\n' +
      'uniform vec3 uAccent;\n' +
      'uniform vec2 uResolution;\n' +
      'uniform float uTime;\n' +
      'uniform float uClock;\n' +
      'uniform float uPixelSize;\n' +
      'uniform float uPixelJitter;\n' +
      'uniform float uEdgeFade;\n' +
      'uniform int uShapeType;\n' +
      'uniform sampler2D uCells;\n' +
      'uniform ivec2 uOrigin;\n' +
      'uniform ivec2 uCellCount;\n' +
      'uniform float uSoftness;\n' +
      'uniform float uSmoothing;\n' +
      'uniform float uGlow;\n' +
      'uniform float uTwinkle;\n' +
      'uniform float uLightMode;\n' +
      'uniform float uTransparent;\n' +
      'uniform float uNoise;\n' +
      'uniform int uHover;\n' +
      'uniform mat3 uPlane;\n' +
      'uniform float uTilted;\n' +
      'out vec4 fragColor;\n' +
      PACKING +
      'const float BAYER[64] = float[64](' + BAYER_TABLE + ');\n' +
      'vec2 gDx;\n' +
      'vec2 gDy;\n' +
      'float aaFrom(vec2 grad) { return abs(dot(grad, gDx)) + abs(dot(grad, gDy)); }\n' +
      'float maskSquare(vec2 p, float cov, float soft) {\n' +
      '  if (soft <= 0.0) return cov;\n' +
      '  float side = 0.5 * sqrt(clamp(cov, 0.0, 1.0));\n' +
      '  vec2 c = p - 0.5;\n' +
      '  vec2 q = abs(c) - side;\n' +
      '  float d = max(q.x, q.y);\n' +
      '  vec2 grad = q.x > q.y ? vec2(sign(c.x), 0.0) : vec2(0.0, sign(c.y));\n' +
      '  float aa = max(aaFrom(grad), 1e-4);\n' +
      '  return 1.0 - smoothstep(-aa * 0.5, aa * 0.5, d);\n' +
      '}\n' +
      'float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }\n' +
      'void main() {\n' +
      '  float pixelSize = uPixelSize;\n' +
      '  vec2 screen = gl_FragCoord.xy;\n' +
      '  vec4 empty = uTransparent < 0.5 ? vec4(vec3(uLightMode > 0.5 ? 1.0 : 0.0), 1.0) : vec4(0.0);\n' +
      '  vec2 fragCoord = screen - uResolution * 0.5;\n' +
      '  gDx = dFdx(fragCoord) / pixelSize;\n' +
      '  gDy = dFdy(fragCoord) / pixelSize;\n' +
      '  float cellPixelSize = 8.0 * pixelSize;\n' +
      '  vec2 cellId = floor(fragCoord / cellPixelSize);\n' +
      '  ivec2 texel = clamp(ivec2(cellId) - uOrigin, ivec2(0), uCellCount - 1);\n' +
      '  vec2 smoothAt = (fragCoord / cellPixelSize - vec2(uOrigin)) / vec2(uCellCount);\n' +
      '  vec4 blended = texture(uCells, smoothAt);\n' +
      '  float smoothFeed = blended.b * 2.0 - 0.5;\n' +
      '  float feed = smoothFeed;\n' +
      '  float trail = uHover == 1 ? blended.a : 0.0;\n' +
      '  if (uSmoothing < 1.0) {\n' +
      '    vec4 cell = texelFetch(uCells, texel, 0);\n' +
      '    feed = mix(unpack16(cell.rg), smoothFeed, uSmoothing);\n' +
      '    if (uHover == 1) trail = mix(cell.a, blended.a, uSmoothing);\n' +
      '  }\n' +
      '  if (feed < -0.65 && smoothFeed < 0.05) {\n' +
      '    fragColor = empty;\n' +
      '    return;\n' +
      '  }\n' +
      '  vec2 pixelId = floor(fragCoord / pixelSize);\n' +
      '  vec2 pixelUV = fragCoord / pixelSize - pixelId;\n' +
      '  ivec2 cellBits = ivec2(mod(pixelId, 8.0));\n' +
      '  float bayer = BAYER[cellBits.y * 8 + cellBits.x] - 0.5;\n' +
      '  float margin = feed + bayer - 0.5;\n' +
      '  float bw = uSoftness > 0.0 ? smoothstep(-0.12 * uSoftness, 0.12 * uSoftness, margin) : step(0.0, margin);\n' +
      '  float h = uPixelJitter > 0.0 || uTwinkle > 0.0 ? fract(sin(dot(pixelId, vec2(127.1, 311.7))) * 43758.5453) : 0.5;\n' +
      '  float jitterScale = 1.0 + (h - 0.5) * uPixelJitter;\n' +
      '  float heat = max(trail, clamp((feed - 1.0) * 2.5, 0.0, 1.0) * min(uGlow * 2.0, 1.0));\n' +
      '  float coverage = bw * jitterScale * (1.0 + heat * 0.35);\n' +
      '  float M = maskSquare(pixelUV, coverage, uSoftness);\n' +
      '  if (uTwinkle > 0.0) {\n' +
      '    float rate = 1.5 + 3.0 * fract(h * 7.31);\n' +
      '    float wave = 0.5 + 0.5 * sin(uClock * rate + h * 37.0);\n' +
      '    M *= 1.0 - uTwinkle * 0.55 * wave * wave;\n' +
      '  }\n' +
      '  float density = clamp(smoothFeed, 0.0, 1.0);\n' +
      '  float glow = 0.0;\n' +
      '  if (uGlow > 0.0) {\n' +
      '    float bloom = smoothstep(0.2, 1.0, density);\n' +
      '    glow = bloom * bloom * uGlow * (uLightMode > 0.5 ? 0.12 : 0.22);\n' +
      '  }\n' +
      '  float hot = clamp(heat, 0.0, 1.0);\n' +
      '  float tone = smoothstep(0.3, 1.0, density) * 0.85;\n' +
      '  vec3 color = mix(uColor, uAccent, max(tone, hot));\n' +
      '  vec3 lit = uLightMode > 0.5 ? color * (1.0 - 0.35 * hot) : mix(color, vec3(1.0), 0.45 * hot);\n' +
      '  vec3 haze = uLightMode > 0.5 ? mix(color, vec3(1.0), 0.35) : color;\n' +
      '  float alpha = M + glow * (1.0 - M);\n' +
      '  vec3 premul = lit * M + haze * glow * (1.0 - M);\n' +
      '  fragColor = vec4(premul, alpha);\n' +
      '}';

    const clamp = (val, min, max) => Math.min(max, Math.max(min, val));

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

    const parseHex = hex => {
      let h = String(hex || '#ffffff').replace('#', '').trim();
      if (h.length === 3 || h.length === 4) {
        h = h.slice(0, 3).split('').map(c => c + c).join('');
      }
      const num = parseInt(h.slice(0, 6), 16);
      if (Number.isNaN(num)) return [1, 1, 1];
      return [((num >> 16) & 255) / 255, ((num >> 8) & 255) / 255, (num & 255) / 255];
    };

    class PixelBlastEngine {
      constructor(container, canvas, options = {}) {
        this.container = container;
        this.canvas = canvas;
        this.updateSettings(options);
        this.initWebGL();
      }

      updateSettings(opts = {}) {
        const prev = this.settings || {};
        const merged = {
          variant: 'square',
          pixelSize: 3,
          color: '#6D28D9',
          accentColor: '#C4B5FD',
          patternScale: 2.2,
          patternDensity: 0.65,
          pixelSizeJitter: 0.15,
          enableRipples: true,
          rippleIntensityScale: 2.2,
          rippleThickness: 0.07,
          rippleSpeed: 0.65,
          speed: 0.45,
          transparent: true,
          softness: 0.65,
          smoothing: 1,
          glow: 0.55,
          twinkle: 0.35,
          drift: 0.2,
          driftAngle: 35,
          hoverEffect: 'reveal',
          hoverRadius: 140,
          autoRipple: 3.2,
          intro: true,
          lightMode: false,
          ...prev,
          ...opts
        };
        this.settings = {
          ...merged,
          variantId: SHAPES[merged.variant] ?? 0,
          pixelSize: Math.max(0.5, merged.pixelSize),
          rippleThickness: Math.max(0.001, merged.rippleThickness),
          softness: clamp(merged.softness, 0, 1),
          smoothing: clamp(merged.smoothing, 0, 1),
          glow: clamp(merged.glow, 0, 2),
          twinkle: clamp(merged.twinkle, 0, 1),
          hoverId: HOVERS[merged.hoverEffect] ?? 2,
          hoverRadius: Math.max(10, merged.hoverRadius),
          autoRipple: Math.max(0, merged.autoRipple)
        };
        if (this.wake) this.wake();
      }

      initWebGL() {
        const gl = this.canvas.getContext('webgl2', {
          alpha: true,
          premultipliedAlpha: true,
          antialias: false,
          depth: false,
          stencil: false
        });
        if (!gl) return;
        this.gl = gl;

        const cellShader = link(gl, CELL_FRAGMENT);
        const mainShader = link(gl, MAIN_FRAGMENT);
        if (!cellShader || !mainShader) return;

        const cu = cellShader.uniforms;
        const mu = mainShader.uniforms;
        const vao = gl.createVertexArray();

        const makeTexture = () => {
          const texture = gl.createTexture();
          gl.bindTexture(gl.TEXTURE_2D, texture);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
          gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
          gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA8, 1, 1, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
          return texture;
        };

        const targets = [0, 1].map(() => {
          const texture = makeTexture();
          const framebuffer = gl.createFramebuffer();
          gl.bindFramebuffer(gl.FRAMEBUFFER, framebuffer);
          gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, texture, 0);
          return { texture, framebuffer };
        });
        gl.bindFramebuffer(gl.FRAMEBUFFER, null);

        const view = { width: 1, height: 1, ratio: 1 };
        const cells = { x: 0, y: 0, w: 1, h: 1, key: '' };
        const pointer = { x: 0, y: 0, inside: false };
        const mouse = { x: 0, y: 0, ready: false };
        const clickPos = new Float32Array(MAX_CLICKS * 2).fill(-1);
        const clickAt = new Float32Array(MAX_CLICKS);
        const clickAge = new Float32Array(MAX_CLICKS);

        let clickIndex = 0;
        let current = 0;
        let fresh = true;
        const timeOffset = Math.random() * 1000;
        let elapsed = 0;
        let clock = 0;
        let presence = 0;
        let driftX = 0;
        let driftY = 0;
        let introClock = this.settings.intro ? 0 : 1;
        let nextAuto = 1.5;
        let isPressing = false;
        let raf = 0;
        let last = 0;

        const patternTime = s => timeOffset + elapsed * s.speed;

        const layoutCells = s => {
          const ps = s.pixelSize * view.ratio;
          const cps = 8 * ps;
          const w = this.canvas.width;
          const h = this.canvas.height;
          const pad = cps;
          const x0 = Math.floor((-w / 2 - pad) / cps);
          const x1 = Math.floor((w / 2 + pad) / cps);
          const y0 = Math.floor((-h / 2 - pad) / cps);
          const y1 = Math.floor((h / 2 + pad) / cps);
          const width = Math.max(1, Math.min(2048, x1 - x0 + 1));
          const height = Math.max(1, Math.min(2048, y1 - y0 + 1));
          const key = x0 + ':' + y0 + ':' + width + ':' + height;
          if (key === cells.key) return;
          cells.key = key;
          cells.x = x0; cells.y = y0; cells.w = width; cells.h = height;
          for (const target of targets) {
            gl.bindTexture(gl.TEXTURE_2D, target.texture);
            gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA8, width, height, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
          }
          fresh = true;
        };

        const stepCells = (s, decay) => {
          const source = targets[current];
          const target = targets[1 - current];
          gl.bindFramebuffer(gl.FRAMEBUFFER, target.framebuffer);
          gl.viewport(0, 0, cells.w, cells.h);
          gl.disable(gl.BLEND);
          gl.useProgram(cellShader.program);
          gl.activeTexture(gl.TEXTURE0);
          gl.bindTexture(gl.TEXTURE_2D, source.texture);
          gl.uniform1i(cu.uPrev, 0);
          gl.uniform2f(cu.uResolution, this.canvas.width, this.canvas.height);
          gl.uniform1f(cu.uTime, patternTime(s));
          gl.uniform1f(cu.uPixelSize, s.pixelSize * view.ratio);
          gl.uniform1f(cu.uScale, s.patternScale);
          gl.uniform1f(cu.uDensity, s.patternDensity);
          gl.uniform1i(cu.uEnableRipples, s.enableRipples ? 1 : 0);
          gl.uniform1f(cu.uRippleSpeed, s.rippleSpeed);
          gl.uniform1f(cu.uRippleThickness, s.rippleThickness);
          gl.uniform1f(cu.uRippleIntensity, s.rippleIntensityScale);
          gl.uniform2fv(cu.uClickPos, clickPos);
          gl.uniform1fv(cu.uClickAge, clickAge);
          gl.uniform2i(cu.uOrigin, cells.x, cells.y);
          gl.uniform2f(cu.uDrift, driftX, driftY);
          gl.uniform1i(cu.uHover, s.hoverId);
          gl.uniform2f(cu.uMouse, mouse.x, mouse.y);
          gl.uniform1f(cu.uMouseRadius, s.hoverRadius * view.ratio);
          gl.uniform1f(cu.uMousePresence, presence);
          gl.uniform1f(cu.uIntro, introClock);
          gl.uniform1f(cu.uDecay, decay);
          gl.uniform1f(cu.uFresh, fresh ? 1 : 0);
          gl.bindVertexArray(vao);
          gl.drawArrays(gl.TRIANGLES, 0, 3);
          gl.bindFramebuffer(gl.FRAMEBUFFER, null);
          current = 1 - current;
          fresh = false;
        };

        const draw = () => {
          const s = this.settings;
          gl.viewport(0, 0, this.canvas.width, this.canvas.height);
          gl.clearColor(0, 0, 0, 0);
          gl.clear(gl.COLOR_BUFFER_BIT);
          gl.useProgram(mainShader.program);
          gl.activeTexture(gl.TEXTURE0);
          gl.bindTexture(gl.TEXTURE_2D, targets[current].texture);
          gl.uniform1i(mu.uCells, 0);
          gl.uniform3fv(mu.uColor, parseHex(s.color));
          gl.uniform3fv(mu.uAccent, parseHex(s.accentColor));
          gl.uniform2f(mu.uResolution, this.canvas.width, this.canvas.height);
          gl.uniform1f(mu.uTime, patternTime(s));
          gl.uniform1f(mu.uClock, clock);
          gl.uniform1f(mu.uPixelSize, s.pixelSize * view.ratio);
          gl.uniform1f(mu.uPixelJitter, s.pixelSizeJitter);
          gl.uniform1f(mu.uEdgeFade, 0.0);
          gl.uniform1i(mu.uShapeType, s.variantId);
          gl.uniform2i(mu.uOrigin, cells.x, cells.y);
          gl.uniform2i(mu.uCellCount, cells.w, cells.h);
          gl.uniform1f(mu.uSoftness, s.softness);
          gl.uniform1f(mu.uSmoothing, s.smoothing);
          gl.uniform1f(mu.uGlow, s.glow);
          gl.uniform1f(mu.uTwinkle, s.twinkle);
          gl.uniform1f(mu.uLightMode, s.lightMode ? 1 : 0);
          gl.uniform1f(mu.uTransparent, 1.0);
          gl.uniform1f(mu.uNoise, 0.02);
          gl.uniform1i(mu.uHover, s.hoverId);
          gl.bindVertexArray(vao);
          gl.drawArrays(gl.TRIANGLES, 0, 3);
          gl.bindVertexArray(null);
        };

        this.addRipple = (x, y) => {
          clickPos[clickIndex * 2] = x * view.ratio;
          clickPos[clickIndex * 2 + 1] = (view.height - y) * view.ratio;
          clickAt[clickIndex] = clock;
          clickIndex = (clickIndex + 1) % MAX_CLICKS;
          nextAuto = (this.settings.autoRipple || 3.2) * 1.5;
          this.wake();
        };

        this.triggerCenterBlast = () => {
          this.addRipple(view.width / 2, view.height * 0.40);
        };

        const frame = now => {
          raf = 0;
          if (document.hidden) return;
          const s = this.settings;
          const dt = last ? clamp((now - last) / 1000, 0, 1 / 20) : 0;
          last = now;
          elapsed += dt;
          clock += dt;

          if (s.drift) {
            const angle = (s.driftAngle * Math.PI) / 180;
            driftX += Math.cos(angle) * s.drift * dt * 0.05;
            driftY += Math.sin(angle) * s.drift * dt * 0.05;
          }

          if (s.enableRipples && s.autoRipple > 0 && introClock >= 1 && !isPressing) {
            nextAuto -= dt;
            if (nextAuto <= 0) {
              nextAuto = s.autoRipple * (0.8 + Math.random() * 0.5);
              this.addRipple((0.2 + Math.random() * 0.6) * view.width, (0.2 + Math.random() * 0.6) * view.height);
            }
          }

          for (let k = 0; k < MAX_CLICKS; k++) clickAge[k] = clock - clickAt[k];

          const targetX = pointer.x * view.ratio;
          const targetY = (view.height - pointer.y) * view.ratio;
          const follow = 1 - Math.exp(-dt / 0.08);
          if (pointer.inside) {
            mouse.x += (targetX - mouse.x) * follow;
            mouse.y += (targetY - mouse.y) * follow;
          }
          presence += ((pointer.inside ? 1 : 0) - presence) * (1 - Math.exp(-dt / 0.25));

          layoutCells(s);
          const decay = s.hoverId ? Math.exp(-dt / 0.45) : 0;
          stepCells(s, decay);
          draw();

          if (introClock < 1) introClock = Math.min(1, introClock + dt / INTRO_SECONDS);
          raf = requestAnimationFrame(frame);
        };

        this.wake = () => {
          if (!raf && !document.hidden) {
            last = 0;
            raf = requestAnimationFrame(frame);
          }
        };

        const resize = () => {
          const s = this.settings;
          view.width = Math.max(1, this.container.clientWidth);
          view.height = Math.max(1, this.container.clientHeight);
          view.ratio = Math.min(window.devicePixelRatio || 1, 2);
          this.canvas.width = Math.max(1, Math.round(view.width * view.ratio));
          this.canvas.height = Math.max(1, Math.round(view.height * view.ratio));
          if (!mouse.ready) {
            mouse.ready = true;
            mouse.x = this.canvas.width / 2;
            mouse.y = this.canvas.height / 2;
          }
          layoutCells(s);
          stepCells(s, 0);
          draw();
          this.wake();
        };

        const locate = event => {
          const rect = this.container.getBoundingClientRect();
          const x = event.clientX - rect.left;
          const y = event.clientY - rect.top;
          return { x, y, inside: x >= 0 && y >= 0 && x <= rect.width && y <= rect.height };
        };

        window.addEventListener('pointermove', event => {
          const { x, y, inside } = locate(event);
          const was = pointer.inside;
          pointer.inside = inside;
          if (inside) {
            pointer.x = x;
            pointer.y = y;
            if (!was) {
              mouse.x = x * view.ratio;
              mouse.y = (view.height - y) * view.ratio;
            }
          }
          if (inside || was) this.wake();
        }, { passive: true });

        window.addEventListener('pointerdown', event => {
          const { x, y, inside } = locate(event);
          if (!inside) return;
          isPressing = true;
          this.addRipple(x, y);
        }, { capture: true, passive: true });

        window.addEventListener('pointerup', () => { isPressing = false; }, { capture: true, passive: true });
        window.addEventListener('pointercancel', () => { isPressing = false; }, { capture: true, passive: true });

        new ResizeObserver(resize).observe(this.container);
        resize();
      }
    }

    let globalEngine = null;
    document.addEventListener('DOMContentLoaded', () => {
      const container = document.getElementById('pixelBlastContainer');
      const canvas = document.getElementById('pixelBlastCanvas');
      globalEngine = new PixelBlastEngine(container, canvas, {
        variant: 'square',
        pixelSize: 3,
        color: '#6D28D9',
        accentColor: '#C4B5FD',
        patternScale: 2.0,
        patternDensity: 0.65,
        softness: 0.6,
        glow: 0.45,
        twinkle: 0.28,
        drift: 0.2,
        hoverEffect: 'reveal',
        enableRipples: true,
        rippleIntensityScale: 2.2,
        rippleThickness: 0.07,
        rippleSpeed: 0.65,
        lightMode: false
      });
    });

    window.setTheme = function(isDark) {
      if (!globalEngine) return;
      globalEngine.updateSettings({
        lightMode: !isDark,
        color: isDark ? '#6D28D9' : '#7C3AED',
        accentColor: isDark ? '#C4B5FD' : '#8B5CF6',
        glow: isDark ? 0.45 : 0.35
      });
    };

    window.triggerCenterBlast = function() {
      if (globalEngine && typeof globalEngine.triggerCenterBlast === 'function') {
        globalEngine.triggerCenterBlast();
      }
    };
  </script>
</body>
</html>"""

/**
 * 原生 WebGL 2 PixelBlast 粒子流光波纹背景引擎
 * 完全对齐 gemini-code-1792823982566.html 设计规范
 * 采用与登录界面 KanaWaveBackground 完全一致的生产级架构与主题通信通道
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PixelBlastBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onControllerReady: ((triggerCenterBlast: () -> Unit) -> Unit)? = null
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
                        onControllerReady?.invoke {
                            view?.evaluateJavascript("if (window.triggerCenterBlast) window.triggerCenterBlast();", null)
                        }
                    }
                }
                loadDataWithBaseURL("https://nemo.local/", PIXEL_BLAST_HTML, "text/html", "UTF-8", null)
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
