package com.jian.nemo.feature.user.component

import android.graphics.BlurMaskFilter
import android.graphics.Paint
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
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
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

/**
 * 顶部假名流动天幕背景（100% 纯 Jetpack Compose Canvas 原生高性能绘制）
 * 动态延伸铺展，底部带有深度平滑过渡渐变，彻底消除与表单之间的苍白断层
 */
/**
 * 顶部假名流动天幕背景（100% 纯 Jetpack Compose Canvas 原生高性能绘制）
 * 1:1 像素级复刻原版 nemo_app (1).html 与 React Bits <GradientASCII /> 渲染管线
 */
@Composable
fun KanaWaveBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "KanaWaveFlowTransition")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 62.8318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "kanaTime"
    )

    // 页面初次进入时 2.4 秒波浪式由地平线向上破晓升起展开（1:1 还原原版 INTRO_SECONDS = 2.4）
    val introAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        introAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
        )
    }

    val pageBgColor = if (isDark) Color(0xFF0D0C11) else Color(0xFFF7F9FC)
    val density = LocalDensity.current

    // 原版 13 个从暗到亮、笔画从简到繁严格排序的字形集（索引0为空格，1为点，后续为假名）
    val kanaPool = remember {
        listOf(" ", "・", "ノ", "く", "し", "こ", "て", "な", "の", "あ", "ま", "め", "ぬ")
    }

    // 原版 4 阶基底渐变色
    val skyGradients = if (isDark) {
        listOf(
            Color(0xFF1E3FD8), // 宝蓝
            Color(0xFFF6F8FF), // 晶莹浅白天青
            Color(0xFFA5D8FF), // 浅天蓝
            Color(0xFF38BDF8)  // 明亮天青
        )
    } else {
        listOf(
            Color(0xFF1D4ED8),
            Color(0xFF3B82F6),
            Color(0xFF93C5FD),
            Color(0xFF38BDF8)
        )
    }

    // 局部触控白光高亮交互（仅触碰点周围 130dp 半径内发白光发亮，移开 450ms 淡出）
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var lastTouchPos by remember { mutableStateOf(Offset.Zero) }
    val touchIntensity = remember { Animatable(0f) }

    LaunchedEffect(touchPos) {
        if (touchPos != null) {
            lastTouchPos = touchPos!!
            touchIntensity.animateTo(1f, animationSpec = tween(120))
        } else {
            touchIntensity.animateTo(0f, animationSpec = tween(450, easing = FastOutSlowInEasing))
        }
    }

    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.firstOrNull { it.pressed }
                    touchPos = pressed?.position
                }
            }
        }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val intro = introAnim.value

            val currentTouchAlpha = touchIntensity.value
            val touchRadiusPx = with(density) { 130.dp.toPx() }
            val touchCenter = lastTouchPos

            // 1. 还原原版 Horizon 天幕模型：
            // 深色天幕由上而下充满天青蓝、浅天蓝与深邃宝石蓝的广阔极光光辉，高光柔和弥散，下部平滑淡出
            val sunX = width * 0.5f + sin(time * 0.31f) * (width * 0.08f)
            val sunY = height * (0.45f - 0.20f * intro)
            val sunRadius = width * (0.85f + 0.20f * intro)

            if (isDark) {
                // 背景极光天幕基底：深海宝蓝向天青与暗夜过渡
                val skyBaseGradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E3FD8).copy(alpha = 0.75f + 0.25f * intro), // 顶部边缘：宝蓝天幕
                        Color(0xFF1737B5).copy(alpha = 0.70f + 0.30f * intro), // 深天青蓝
                        Color(0xFF0F267A).copy(alpha = 0.60f),                  // 蔚蓝过渡
                        Color(0xFF0A184D).copy(alpha = 0.35f),                  // 幽蓝
                        pageBgColor                                             // 纯暗夜底色 #0D0C11
                    ),
                    startY = 0f,
                    endY = height * 0.75f
                )
                drawRect(brush = skyBaseGradient)

                // 顶部广阔柔和天幕极光散射（大半径极光场，彻底消除局促的手电筒圆球感）
                val auroraGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF6F8FF).copy(alpha = 0.55f * intro), // 核心柔和月白天青
                        Color(0xFFA5D8FF).copy(alpha = 0.45f * intro), // 浅天蓝极光
                        Color(0xFF38BDF8).copy(alpha = 0.30f * intro), // 明亮天青扩散
                        Color(0xFF1E3FD8).copy(alpha = 0.12f * intro), // 宝蓝外圈
                        Color(0x001E3FD8)                              // 平滑融化在天幕中
                    ),
                    center = Offset(sunX, sunY),
                    radius = sunRadius
                )
                drawRect(brush = auroraGlow)
            } else {
                val currentEndY = height * (0.40f + 0.50f * intro)
                val oklabColors = buildOklabGradient(skyGradients, steps = 32)
                val bgGradient = Brush.verticalGradient(
                    colors = oklabColors,
                    startY = 0f,
                    endY = currentEndY
                )
                drawRect(brush = bgGradient)
            }

            // 2. 严格按原版比例（cellSize=24, ATLAS 84:100, FONT_SCALE 0.66）计算网格
            val cellYPx = with(density) { 24.dp.toPx() }
            val cellXPx = cellYPx * 0.84f // 原版 84/100 宽高比，约 20.16dp
            val fontSizePx = cellYPx * 0.66f // 原版 0.66 字体缩放，约 15.8dp
            val glowRadius = with(density) { 3.5.dp.toPx() }

            val paint = Paint().apply {
                isAntiAlias = true
                textSize = fontSizePx
                textAlign = Paint.Align.CENTER
                isFakeBoldText = false // 原版轻盈细字重
            }

            // Halo 光晕层画笔（模拟原版 GLYPHS + HALO pass）
            val haloPaint = Paint().apply {
                isAntiAlias = true
                textSize = fontSizePx
                textAlign = Paint.Align.CENTER
                isFakeBoldText = false
                maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
            }

            val rows = (height / cellYPx).toInt() + 2
            val cols = (width / cellXPx).toInt() + 3

            for (r in 0 until rows) {
                // 奇偶行交错错位排布
                val rowOffset = if (r % 2 == 1) cellXPx * 0.5f else 0f
                val baseY = (r + 0.75f) * cellYPx
                // 原版展开方向：地平线（下）先升起，向上方天幕席卷
                val rowOrder = 1f - (r.toFloat() / rows.coerceAtLeast(1))

                for (c in -1 until cols) {
                    val posX = c * cellXPx + rowOffset
                    if (posX < -cellXPx || posX > width + cellXPx) continue

                    // 1:1 对齐原版 Horizon 抛物线地平线展开算法
                    val normX = ((posX / width.coerceAtLeast(1f)) - 0.5f) * 2f
                    val arch = 0.16f * (normX * normX) - sin(time * 0.6f) * 0.02f
                    val order = (rowOrder + arch).coerceIn(0f, 1f)
                    val rawReveal = ((intro * 1.45f - order) / 0.45f).coerceIn(0f, 1f)
                    val reveal = rawReveal * rawReveal * (3.0f - 2.0f * rawReveal)

                    if (reveal <= 0.01f) continue

                    val cellX = c.toFloat()
                    val cellY = r.toFloat()

                    // 原版散列与微动扰动 (Wobble & Level)
                    val pick = hash12(cellX + 17.31f, cellY * 2.19f)
                    val level = hash12(cellX * 1.73f + 4.1f, cellY * 1.73f)
                    val tick = kotlin.math.floor(time * 3.0f + level * 9.0f)
                    val wobble = (hash12(cellX * 2.31f + tick * 17.7f, cellY * 2.31f) - 0.5f) * 0.16f * 0.4f + (level - 0.5f) * 0.1f

                    // 微弱正弦呼吸微动
                    val waveY = sin(c * 0.35f + time * 1.2f + r * 0.25f) * 2.5f
                    val riseY = (1f - reveal) * 10f
                    val posY = baseY + waveY + riseY

                    // 计算当前位置在天幕中的物理亮度 (Luma)
                    val dxSun = (posX - sunX) / (sunRadius * 0.95f)
                    val dySun = (posY - sunY) / (sunRadius * 0.80f)
                    val sunDist = sqrt(dxSun * dxSun + dySun * dySun)
                    val sunGlow = (1f - sunDist.coerceIn(0f, 1f)).let { it * it * (3f - 2f * it) }
                    val skyGradFactor = (1f - (posY / (height * 0.70f))).coerceIn(0f, 1f)

                    val luma = if (isDark) {
                        (0.30f * skyGradFactor + 0.65f * sunGlow).coerceIn(0f, 1f)
                    } else {
                        (0.40f * skyGradFactor + 0.55f * sunGlow).coerceIn(0f, 1f)
                    }

                    // 原版音调计算：tone = clamp(luma^0.75 + wobble, 0.0, 1.0)
                    val tone = (luma.pow(0.75f) + wobble).coerceIn(0f, 1f)

                    // 原版密度留白控制 (Density = 0.86)
                    val presentThreshold = 0.86f * (1.0f * 0.45f + (0.3f + tone * 1.1f) * 0.55f)
                    if (pick > presentThreshold) continue

                    // 1:1 对齐原版字形映射：索引根据亮度 tone 自然映射（亮处繁密假名，暗处细点或空格）
                    var charIdx = kotlin.math.floor(tone * (kanaPool.size - 1) + 0.5f).toInt().coerceIn(0, kanaPool.size - 1)

                    // 原版 Decode Scramble 展开乱序解码动效
                    val decode = 1f - reveal
                    if (decode > 0.02f) {
                        val frame = kotlin.math.floor(time * 20.0f)
                        if (hash12(cellX * 3.7f + frame * 5.3f, cellY * 3.7f) < decode * 1.25f) {
                            val scrambleHash = hash12(cellX + frame * 1.37f, cellY + frame * 2.11f)
                            charIdx = 1 + kotlin.math.floor(scrambleHash * (kanaPool.size - 1)).toInt().coerceIn(0, kanaPool.size - 2)
                        }
                    }

                    val charText = kanaPool[charIdx]
                    if (charText == " ") continue

                    // 触控高亮因子（触控点周围 130dp 半径内向纯白高光过渡）
                    val touchFactor = if (currentTouchAlpha > 0.01f) {
                        val dx = posX - touchCenter.x
                        val dy = posY - touchCenter.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < touchRadiusPx) {
                            val norm = 1f - dist / touchRadiusPx
                            (norm * norm * (3f - 2f * norm)) * currentTouchAlpha
                        } else {
                            0f
                        }
                    } else {
                        0f
                    }

                    // 透明度控制：受层级、展开进度及亮度综合调节
                    val baseAlpha = if (isDark) {
                        (0.20f + 0.65f * tone) * (0.45f + 0.55f * level)
                    } else {
                        (0.30f + 0.60f * tone) * (0.45f + 0.55f * level)
                    }
                    val finalAlpha = (baseAlpha * reveal).coerceIn(0f, 0.92f)
                    val alphaInt = ((finalAlpha + (1f - finalAlpha) * touchFactor * 0.4f).coerceIn(0f, 1f) * 255).toInt()

                    // 字形颜色：深色模式来源于天幕天青/宝蓝发光色；触控处过渡为纯白 #FFFFFF
                    val colorInt = if (isDark) {
                        // 亮处为晶白天青 (224, 242, 254)，次亮处为天青蓝 (56, 189, 248)，暗处为宝蓝 (30, 63, 216)
                        val rBase = (30f + 194f * tone).coerceIn(0f, 255f)
                        val gBase = (63f + 179f * tone).coerceIn(0f, 255f)
                        val bBase = (216f + 38f * tone).coerceIn(0f, 255f)

                        val r = ((1f - touchFactor) * rBase + touchFactor * 255f).toInt().coerceIn(0, 255)
                        val g = ((1f - touchFactor) * gBase + touchFactor * 255f).toInt().coerceIn(0, 255)
                        val b = ((1f - touchFactor) * bBase + touchFactor * 255f).toInt().coerceIn(0, 255)
                        android.graphics.Color.argb(alphaInt, r, g, b)
                    } else {
                        val rBase = (30f + 50f * (1f - tone)).coerceIn(0f, 255f)
                        val gBase = (41f + 60f * (1f - tone)).coerceIn(0f, 255f)
                        val bBase = (59f + 80f * (1f - tone)).coerceIn(0f, 255f)

                        val r = ((1f - touchFactor) * rBase + touchFactor * 255f).toInt().coerceIn(0, 255)
                        val g = ((1f - touchFactor) * gBase + touchFactor * 255f).toInt().coerceIn(0, 255)
                        val b = ((1f - touchFactor) * bBase + touchFactor * 255f).toInt().coerceIn(0, 255)
                        android.graphics.Color.argb(alphaInt, r, g, b)
                    }

                    // 光晕颜色与绘制
                    val haloAlphaMultiplier = (1f - touchFactor) * 0.22f + touchFactor * 0.50f
                    val haloAlphaInt = (finalAlpha * haloAlphaMultiplier * 255).toInt().coerceIn(0, 255)
                    val haloColorInt = if (isDark) {
                        val hr = ((1f - touchFactor) * 56f + touchFactor * 255f).toInt().coerceIn(0, 255)
                        val hg = ((1f - touchFactor) * 189f + touchFactor * 255f).toInt().coerceIn(0, 255)
                        val hb = ((1f - touchFactor) * 248f + touchFactor * 255f).toInt().coerceIn(0, 255)
                        android.graphics.Color.argb(haloAlphaInt, hr, hg, hb)
                    } else {
                        android.graphics.Color.argb(haloAlphaInt, 96, 165, 250)
                    }

                    haloPaint.color = haloColorInt
                    drawContext.canvas.nativeCanvas.drawText(
                        charText, posX, posY, haloPaint
                    )

                    paint.color = colorInt
                    drawContext.canvas.nativeCanvas.drawText(
                        charText, posX, posY, paint
                    )
                }
            }

            // 3. 底部自然深度过渡遮罩（1:1 复刻原版 h-36 渐变遮罩，使天幕自然融于页面底色）
            val maskGradient = Brush.verticalGradient(
                colors = listOf(
                    pageBgColor.copy(alpha = 0f),
                    pageBgColor.copy(alpha = 0.35f),
                    pageBgColor.copy(alpha = 0.75f),
                    pageBgColor
                ),
                startY = height * 0.50f,
                endY = height * 0.85f
            )
            drawRect(brush = maskGradient)
        }
    }
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
