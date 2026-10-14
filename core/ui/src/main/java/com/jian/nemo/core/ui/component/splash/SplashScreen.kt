package com.jian.nemo.core.ui.component.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 全新品牌启动屏
 * 严格按照 HTML 网页设计稿与物理动效 1:1 还原：
 * 1. 背景色自适应系统（浅色纯白 #FFFFFF，深色纯黑 #000000）
 * 2. 居中深海呼吸环境光晕 (Ambient Aurora Glow)，全屏自然消融弥散，绝无圆形硬边
 * 3. 6 组四周漂浮彩色小几何图形（心形、三角、圆角方框等）入场 Pop 与永续浮动
 * 4. 中央 "Welcome!" 逐字弹性弹出与下方微笑弧线顺滑绘制
 * 5. 屏幕底部品牌动效：饱满蛙 Logo 弹出 -> Push 抖动 -> 身后吐出 Nemo 英文并竖起分割线
 * 6. 动效完成后微缩放淡出（Scale 1.05 + FadeOut）平滑衔接跳转
 */
@Composable
fun SplashScreen(
    isAuthReady: Boolean = true,
    onTimeout: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val fgColor = if (isDark) Color(0xFFF5F5F7) else Color(0xFF0A0A0A)

    var animationFinished by remember { mutableStateOf(false) }

    // --- 启动屏整体退场转场动画 ---
    val splashScale = remember { Animatable(1f) }
    val splashAlpha = remember { Animatable(1f) }

    // --- 底部品牌锁（Lockup）动画状态 ---
    val tileScale = remember { Animatable(0f) }
    val lockupGap = remember { Animatable(0f) }
    val nameWidthFraction = remember { Animatable(0f) }
    val nameTranslateXFraction = remember { Animatable(-1f) }
    val dividerScaleY = remember { Animatable(0f) }
    val dividerAlpha = remember { Animatable(0f) }

    // --- 中央舞台（Welcome! + 光晕 + 几何图形）入场状态 ---
    val stageAlpha = remember { Animatable(0f) }

    // Welcome! 8个字符独立弹性动画状态 (translateY, scale, rotation, alpha)
    val charTranslateY = remember { List(8) { Animatable(18f) } }
    val charScale = remember { List(8) { Animatable(0.82f) } }
    val charRotation = remember { List(8) { Animatable(4f) } }
    val charAlpha = remember { List(8) { Animatable(0f) } }

    // 微笑弧线绘制进度 (0f -> 1f)
    val arcProgress = remember { Animatable(0f) }

    // 6组小几何图形入场状态 (scale, alpha, rotation)
    val shapeScales = remember { List(6) { Animatable(0f) } }
    val shapeAlphas = remember { List(6) { Animatable(0f) } }
    val shapeRotations = remember { List(6) { Animatable(-35f) } }

    // 缓动曲线定义（与 HTML CSS 严格对齐）
    val springPopEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    val easeOutQuart = CubicBezierEasing(0.25f, 1f, 0.3f, 1f)
    val nameSlideEasing = CubicBezierEasing(0.2f, 0.9f, 0.25f, 1.08f)

    // --- 无限循环动效（呼吸光晕与小几何图形浮动） ---
    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")

    // 深海光晕 4.2s 循环呼吸动效
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheScale"
    )
    val breatheRotation by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreatheRotation"
    )

    // 6 个几何图形各自的微悬浮浮动（3.2s ~ 4.0s 各自独立周期）
    val shapeFloats = listOf(
        rememberShapeFloat(infiniteTransition, 3200),
        rememberShapeFloat(infiniteTransition, 3800),
        rememberShapeFloat(infiniteTransition, 3500),
        rememberShapeFloat(infiniteTransition, 4000),
        rememberShapeFloat(infiniteTransition, 3400),
        rememberShapeFloat(infiniteTransition, 3700)
    )

    // --- 主动画时间线序列 ---
    LaunchedEffect(key1 = true) {
        // 1. 100ms: 蛙 Logo 弹出
        launch {
            delay(100)
            tileScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = springPopEasing)
            )
        }

        // 2. 550ms: 蛙 Logo 执行 Push 抖动反馈，同时展开间隙并吐出 Nemo 英文
        launch {
            delay(550)
            // Push 抖动动画
            launch {
                tileScale.animateTo(0.9f, tween(175, easing = LinearEasing))
                tileScale.animateTo(1.05f, tween(175, easing = LinearEasing))
                tileScale.animateTo(1f, tween(150, easing = LinearEasing))
            }
            // 间隙展开
            launch {
                lockupGap.animateTo(14f, tween(820, easing = easeOutQuart))
            }
            // Nemo 遮罩展开 (0 -> 1)
            launch {
                nameWidthFraction.animateTo(1f, tween(820, easing = easeOutQuart))
            }
            // Nemo 文本从身后平滑滑出 (-100% -> 0)
            launch {
                nameTranslateXFraction.animateTo(0f, tween(820, easing = nameSlideEasing))
            }
        }

        // 3. 1050ms: 分割线竖向生长拉伸展开
        launch {
            delay(1050)
            launch {
                dividerScaleY.animateTo(1f, tween(380, easing = easeOutQuart))
            }
            launch {
                dividerAlpha.animateTo(0.35f, tween(380, easing = LinearEasing))
            }
        }

        // 4. 1100ms: 中央舞台淡入，同时激发 Welcome! 字母弹跳、光晕与几何体
        launch {
            delay(1100)
            launch {
                stageAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            }

            // Welcome! 8个字母弹跳依次出场
            for (i in 0 until 8) {
                launch {
                    delay(i * 55L + 80L)
                    launch {
                        charTranslateY[i].animateTo(0f, tween(620, easing = springPopEasing))
                    }
                    launch {
                        charScale[i].animateTo(1f, tween(620, easing = springPopEasing))
                    }
                    launch {
                        charRotation[i].animateTo(0f, tween(620, easing = springPopEasing))
                    }
                    launch {
                        charAlpha[i].animateTo(1f, tween(620, easing = springPopEasing))
                    }
                }
            }

            // 微笑弧线绘制（延时 550ms）
            launch {
                delay(550)
                arcProgress.animateTo(1f, tween(700, easing = easeOutQuart))
            }

            // 6个漂浮小几何图形弹性弹出入场
            val targetAlphas = listOf(0.85f, 0.85f, 0.82f, 0.85f, 0.78f, 0.72f)
            val targetRotations = listOf(-16f, 18f, 22f, -20f, 12f, -28f)
            val delays = listOf(120L, 200L, 280L, 340L, 400L, 460L)

            for (i in 0 until 6) {
                launch {
                    delay(delays[i])
                    launch {
                        shapeScales[i].animateTo(1f, tween(720, easing = springPopEasing))
                    }
                    launch {
                        shapeAlphas[i].animateTo(targetAlphas[i], tween(720, easing = springPopEasing))
                    }
                    launch {
                        shapeRotations[i].animateTo(targetRotations[i], tween(720, easing = springPopEasing))
                    }
                }
            }
        }

        // 5. 2400ms: 所有品牌与欢迎动效完整展现后，整体微缩放淡出
        delay(2400)
        launch {
            splashScale.animateTo(1.05f, tween(400, easing = FastOutSlowInEasing))
        }
        launch {
            splashAlpha.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
        }

        // 6. 2800ms: 动画播放结束
        delay(400)
        animationFinished = true
    }

    // 监听：动画完成且登录认证就绪后，执行跳转回调
    LaunchedEffect(animationFinished, isAuthReady) {
        if (animationFinished && isAuthReady) {
            onTimeout()
        }
    }

    // --- 界面总布局 ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .graphicsLayer {
                scaleX = splashScale.value
                scaleY = splashScale.value
                alpha = splashAlpha.value
            }
    ) {
        // ================= 中央舞台（Welcome! + 呼吸光晕 + 浮动几何图形） =================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(stageAlpha.value),
            contentAlignment = Alignment.Center
        ) {
            // 1. 深海呼吸环境光晕 (Ambient Aurora Glow)
            // 画布尺寸扩大到 460dp，采用多段指数级柔和衰减，外层边缘叠加 blur，消除任何硬边界
            Canvas(
                modifier = Modifier
                    .size(460.dp)
                    .blur(48.dp)
                    .graphicsLayer {
                        scaleX = breatheScale
                        scaleY = breatheScale
                        rotationZ = breatheRotation
                    }
            ) {
                val glowAlpha = (if (isDark) 0.22f else 0.16f) * stageAlpha.value
                val w = size.width
                val h = size.height

                if (isDark) {
                    // 深色模式：深海蓝 + 青绿（多阶平滑指数衰减，彻底无边界消融）
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0xFF0EA5E9).copy(alpha = glowAlpha * 0.95f),
                                0.22f to Color(0xFF0EA5E9).copy(alpha = glowAlpha * 0.60f),
                                0.45f to Color(0xFF0EA5E9).copy(alpha = glowAlpha * 0.28f),
                                0.70f to Color(0xFF0EA5E9).copy(alpha = glowAlpha * 0.08f),
                                0.88f to Color(0xFF0EA5E9).copy(alpha = glowAlpha * 0.02f),
                                1.00f to Color.Transparent
                            ),
                            center = Offset(w * 0.42f, h * 0.42f),
                            radius = w * 0.38f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0xFF2DD4BF).copy(alpha = glowAlpha * 0.75f),
                                0.22f to Color(0xFF2DD4BF).copy(alpha = glowAlpha * 0.48f),
                                0.45f to Color(0xFF2DD4BF).copy(alpha = glowAlpha * 0.22f),
                                0.70f to Color(0xFF2DD4BF).copy(alpha = glowAlpha * 0.06f),
                                0.88f to Color(0xFF2DD4BF).copy(alpha = glowAlpha * 0.015f),
                                1.00f to Color.Transparent
                            ),
                            center = Offset(w * 0.58f, h * 0.58f),
                            radius = w * 0.38f
                        )
                    )
                } else {
                    // 浅色模式：天蓝 + 柔橙（多阶平滑指数衰减，中心开始逐渐消融）
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0xFF38BDF8).copy(alpha = glowAlpha * 0.90f),
                                0.20f to Color(0xFF38BDF8).copy(alpha = glowAlpha * 0.55f),
                                0.45f to Color(0xFF38BDF8).copy(alpha = glowAlpha * 0.25f),
                                0.70f to Color(0xFF38BDF8).copy(alpha = glowAlpha * 0.08f),
                                0.88f to Color(0xFF38BDF8).copy(alpha = glowAlpha * 0.02f),
                                1.00f to Color.Transparent
                            ),
                            center = Offset(w * 0.40f, h * 0.42f),
                            radius = w * 0.38f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0xFFFB923C).copy(alpha = glowAlpha * 0.78f),
                                0.20f to Color(0xFFFB923C).copy(alpha = glowAlpha * 0.48f),
                                0.45f to Color(0xFFFB923C).copy(alpha = glowAlpha * 0.20f),
                                0.70f to Color(0xFFFB923C).copy(alpha = glowAlpha * 0.06f),
                                0.88f to Color(0xFFFB923C).copy(alpha = glowAlpha * 0.015f),
                                1.00f to Color.Transparent
                            ),
                            center = Offset(w * 0.60f, h * 0.58f),
                            radius = w * 0.38f
                        )
                    )
                }
            }

            // 2. 6 组四周漂浮彩色小几何图形
            Box(
                modifier = Modifier.size(300.dp, 240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Shape 1: 左上粉红心形
                DecoShapeCanvas(
                    offsetX = (-102).dp,
                    offsetY = (-60).dp,
                    size = 20.dp,
                    scale = shapeScales[0].value * shapeFloats[0].scale,
                    alpha = shapeAlphas[0].value,
                    rotation = shapeRotations[0].value + shapeFloats[0].rotation,
                    floatTranslateY = shapeFloats[0].translateY
                ) {
                    drawHeart(color = Color(0xFFFB7185), isFilled = true)
                }

                // Shape 2: 右上浅蓝空心三角
                DecoShapeCanvas(
                    offsetX = 104.dp,
                    offsetY = (-54).dp,
                    size = 21.dp,
                    scale = shapeScales[1].value * shapeFloats[1].scale,
                    alpha = shapeAlphas[1].value,
                    rotation = shapeRotations[1].value + shapeFloats[1].rotation,
                    floatTranslateY = shapeFloats[1].translateY
                ) {
                    drawTriangle(color = Color(0xFF38BDF8), isFilled = false, strokeWidth = 2.6f)
                }

                // Shape 3: 左下橙色圆角线框
                DecoShapeCanvas(
                    offsetX = (-96).dp,
                    offsetY = 54.dp,
                    size = 18.dp,
                    scale = shapeScales[2].value * shapeFloats[2].scale,
                    alpha = shapeAlphas[2].value,
                    rotation = shapeRotations[2].value + shapeFloats[2].rotation,
                    floatTranslateY = shapeFloats[2].translateY
                ) {
                    drawRoundedRect(color = Color(0xFFFB923C), isFilled = false, strokeWidth = 2.6f, cornerRadius = 4.5f)
                }

                // Shape 4: 右下青色实心三角
                DecoShapeCanvas(
                    offsetX = 98.dp,
                    offsetY = 52.dp,
                    size = 17.dp,
                    scale = shapeScales[3].value * shapeFloats[3].scale,
                    alpha = shapeAlphas[3].value,
                    rotation = shapeRotations[3].value + shapeFloats[3].rotation,
                    floatTranslateY = shapeFloats[3].translateY
                ) {
                    drawTriangle(color = Color(0xFF2DD4BF), isFilled = true, strokeWidth = 0f)
                }

                // Shape 5: 顶部偏右浅紫空心心形
                DecoShapeCanvas(
                    offsetX = 48.dp,
                    offsetY = (-78).dp,
                    size = 15.dp,
                    scale = shapeScales[4].value * shapeFloats[4].scale,
                    alpha = shapeAlphas[4].value,
                    rotation = shapeRotations[4].value + shapeFloats[4].rotation,
                    floatTranslateY = shapeFloats[4].translateY
                ) {
                    drawHeart(color = Color(0xFFA78BFA), isFilled = false, strokeWidth = 2.6f)
                }

                // Shape 6: 底部偏左蓝色实心圆角方块
                DecoShapeCanvas(
                    offsetX = (-38).dp,
                    offsetY = 74.dp,
                    size = 12.dp,
                    scale = shapeScales[5].value * shapeFloats[5].scale,
                    alpha = shapeAlphas[5].value,
                    rotation = shapeRotations[5].value + shapeFloats[5].rotation,
                    floatTranslateY = shapeFloats[5].translateY
                ) {
                    drawRoundedRect(color = Color(0xFF60A5FA), isFilled = true, strokeWidth = 0f, cornerRadius = 5f)
                }
            }

            // 3. 中央 "Welcome!" 英文标题与微笑弧线
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val welcomeText = "Welcome!"
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    welcomeText.forEachIndexed { i, char ->
                        Text(
                            text = char.toString(),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = fgColor,
                            letterSpacing = (-0.02).sp,
                            modifier = Modifier.graphicsLayer {
                                translationY = charTranslateY[i].value.dp.toPx()
                                scaleX = charScale[i].value
                                scaleY = charScale[i].value
                                rotationZ = charRotation[i].value
                                alpha = charAlpha[i].value
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 微笑弧线 (M6 10 Q 60 2 114 9)
                Canvas(
                    modifier = Modifier
                        .width(116.dp)
                        .height(14.dp)
                ) {
                    if (arcProgress.value > 0f) {
                        val path = Path().apply {
                            val scaleX = size.width / 120f
                            val scaleY = size.height / 14f
                            moveTo(6f * scaleX, 10f * scaleY)
                            quadraticTo(60f * scaleX, 2f * scaleY, 114f * scaleX, 9f * scaleY)
                        }

                        val pathMeasure = PathMeasure()
                        pathMeasure.setPath(path, false)
                        val totalLen = pathMeasure.length
                        val currentLen = totalLen * arcProgress.value

                        val drawnPath = Path()
                        pathMeasure.getSegment(0f, currentLen, drawnPath, true)

                        drawPath(
                            path = drawnPath,
                            color = fgColor.copy(alpha = 0.22f),
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }
        }

        // ================= 底部品牌区域（饱满蛙 Logo + 分割线 + 从身后吐出 Nemo） =================
        val density = LocalDensity.current
        // 固定 Nemo 英文目标宽度为 96dp，防止被外层裁切约束挤压截断
        val nemoTargetWidth = 96.dp
        val currentClipWidth = nemoTargetWidth * nameWidthFraction.value

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-85).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 1. 蛙 Logo（无多余留白，饱满呈现 60×60dp）
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer {
                        scaleX = tileScale.value
                        scaleY = tileScale.value
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_nemo_splash_logo),
                    contentDescription = "Nemo Frog Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 蛙 Logo 与右侧动态展开间隙
            Spacer(modifier = Modifier.width(lockupGap.value.dp))

            // 2. 竖向分割线（高度 34dp，宽 2dp，圆角 1dp）
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(34.dp)
                    .graphicsLayer {
                        scaleY = dividerScaleY.value
                        alpha = dividerAlpha.value
                    }
                    .clip(RoundedCornerShape(1.dp))
                    .background(fgColor)
            )

            // 分割线与 Nemo 动态展开间隙
            Spacer(modifier = Modifier.width(lockupGap.value.dp))

            // 3. 从青蛙身后吐出的 "Nemo" 英文
            // 关键：使用 requiredWidth 锁定文本自身完整排印宽度，绝不换行，外层使用 width 遮罩展开
            Box(
                modifier = Modifier
                    .width(currentClipWidth)
                    .graphicsLayer { clip = true }
            ) {
                Text(
                    text = "Nemo",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = fgColor,
                    softWrap = false,
                    letterSpacing = 0.01.sp,
                    modifier = Modifier
                        .requiredWidth(nemoTargetWidth)
                        .graphicsLayer {
                            translationX = nameTranslateXFraction.value * with(density) { nemoTargetWidth.toPx() }
                        }
                )
            }
        }
    }
}

/**
 * 几何图形浮动动效状态结构
 */
private class ShapeFloatState(
    val translateY: Float,
    val rotation: Float,
    val scale: Float
)

@Composable
private fun rememberShapeFloat(
    transition: androidx.compose.animation.core.InfiniteTransition,
    durationMs: Int
): ShapeFloatState {
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShapeFloat_$durationMs"
    )

    // floatSpin: translateY 0 -> -8dp, rotation 0 -> 14deg, scale 1 -> 1.08
    return ShapeFloatState(
        translateY = -8f * progress,
        rotation = 14f * progress,
        scale = 1f + 0.08f * progress
    )
}

/**
 * 装饰图形专用绘制容器
 */
@Composable
private fun DecoShapeCanvas(
    offsetX: Dp,
    offsetY: Dp,
    size: Dp,
    scale: Float,
    alpha: Float,
    rotation: Float,
    floatTranslateY: Float,
    drawShape: DrawScope.() -> Unit
) {
    Canvas(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY + floatTranslateY.dp)
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                this.alpha = alpha
            }
    ) {
        drawShape()
    }
}

/**
 * 绘制心形矢量图形（基于 24x24 viewBox 等比适配）
 */
private fun DrawScope.drawHeart(color: Color, isFilled: Boolean, strokeWidth: Float = 0f) {
    val scaleX = size.width / 24f
    val scaleY = size.height / 24f
    val path = Path().apply {
        moveTo(12f * scaleX, 21.35f * scaleY)
        lineTo((12f - 1.45f) * scaleX, (21.35f - 1.32f) * scaleY)
        cubicTo(5.4f * scaleX, 15.36f * scaleY, 2f * scaleX, 12.28f * scaleY, 2f * scaleX, 8.5f * scaleY)
        cubicTo(2f * scaleX, 5.42f * scaleY, 4.42f * scaleX, 3f * scaleY, 7.5f * scaleX, 3f * scaleY)
        cubicTo(9.24f * scaleX, 3f * scaleY, 10.91f * scaleX, 3.81f * scaleY, 12f * scaleX, 5.09f * scaleY)
        cubicTo(13.09f * scaleX, 3.81f * scaleY, 14.76f * scaleX, 3f * scaleY, 16.5f * scaleX, 3f * scaleY)
        cubicTo(19.58f * scaleX, 3f * scaleY, 22f * scaleX, 5.42f * scaleY, 22f * scaleX, 8.5f * scaleY)
        cubicTo(22f * scaleX, 12.28f * scaleY, 18.6f * scaleX, 15.36f * scaleY, 13.45f * scaleX, 19.89f * scaleY)
        close()
    }
    if (isFilled) {
        drawPath(path = path, color = color, style = Fill)
    } else {
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth.dp.toPx(),
                join = StrokeJoin.Round,
                cap = StrokeCap.Round
            )
        )
    }
}

/**
 * 绘制三角形矢量图形（基于 24x24 viewBox 等比适配）
 */
private fun DrawScope.drawTriangle(color: Color, isFilled: Boolean, strokeWidth: Float = 0f) {
    val scaleX = size.width / 24f
    val scaleY = size.height / 24f
    val path = Path().apply {
        if (isFilled) {
            // 实心圆角三角 (M10.3 3.9 ...)
            moveTo(10.3f * scaleX, 3.9f * scaleY)
            cubicTo(11.05f * scaleX, 2.6f * scaleY, 12.95f * scaleX, 2.6f * scaleY, 13.7f * scaleX, 3.9f * scaleY)
            lineTo(21.2f * scaleX, 16.9f * scaleY)
            cubicTo(21.95f * scaleX, 18.2f * scaleY, 21f * scaleX, 19.8f * scaleY, 19.5f * scaleX, 19.8f * scaleY)
            lineTo(4.5f * scaleX, 19.8f * scaleY)
            cubicTo(3f * scaleX, 19.8f * scaleY, 2.05f * scaleX, 18.2f * scaleY, 2.8f * scaleX, 16.9f * scaleY)
            close()
        } else {
            // 空心三角 (points 12 3, 22 20, 2 20)
            moveTo(12f * scaleX, 3f * scaleY)
            lineTo(22f * scaleX, 20f * scaleY)
            lineTo(2f * scaleX, 20f * scaleY)
            close()
        }
    }
    if (isFilled) {
        drawPath(path = path, color = color, style = Fill)
    } else {
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth.dp.toPx(),
                join = StrokeJoin.Round,
                cap = StrokeCap.Round
            )
        )
    }
}

/**
 * 绘制圆角矩形矢量图形
 */
private fun DrawScope.drawRoundedRect(color: Color, isFilled: Boolean, strokeWidth: Float = 0f, cornerRadius: Float) {
    val scaleX = size.width / 24f
    val scaleY = size.height / 24f
    val rectOffset = Offset(4f * scaleX, 4f * scaleY)
    val rectSize = Size(16f * scaleX, 16f * scaleY)
    val cr = CornerRadius(cornerRadius * scaleX, cornerRadius * scaleY)

    if (isFilled) {
        drawRoundRect(
            color = color,
            topLeft = rectOffset,
            size = rectSize,
            cornerRadius = cr,
            style = Fill
        )
    } else {
        drawRoundRect(
            color = color,
            topLeft = rectOffset,
            size = rectSize,
            cornerRadius = cr,
            style = Stroke(
                width = strokeWidth.dp.toPx(),
                join = StrokeJoin.Round,
                cap = StrokeCap.Round
            )
        )
    }
}
