package com.jian.nemo.core.ui.component.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 应用启动屏
 * 移动自 :app 模块以支持跨模块共享
 *
 * 动效序列：
 * 1. 背景波纹扩散 (0.1s / 0.4s)
 * 2. 悬浮几何图形漂浮 (0s~)
 * 3. 日文 "ネモ" 弹性弹出 (0.3s / 0.45s)
 * 4. 白色分割线展开 (0.8s)
 * 5. 英文 "Nemo" 遮罩滑入 (0.9s)
 * 6. 标语 "解锁日语新视界" 遮罩滑入 (1.05s)
 */
@Composable
fun SplashScreen(
    isAuthReady: Boolean = true, // 默认 true 保持兼容，但在 NavHost 中应传入实际状态
    onTimeout: () -> Unit
) {
    var animationFinished by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary

    // --- 波纹动画 ---
    val ripple1Scale = remember { Animatable(0f) }
    val ripple1Alpha = remember { Animatable(1f) }
    val ripple2Scale = remember { Animatable(0f) }
    val ripple2Alpha = remember { Animatable(1f) }

    // --- 几何图形动画 ---
    val circleAlpha = remember { Animatable(0f) }
    val circleTranslateY = remember { Animatable(50f) }
    val rectAlpha = remember { Animatable(0f) }
    val rectTranslateY = remember { Animatable(50f) }
    val rectRotation = remember { Animatable(0f) }

    // --- 日文字符 "ネ" 动画 ---
    val char1Scale = remember { Animatable(0.3f) }
    val char1TranslateY = remember { Animatable(50f) }
    val char1Rotation = remember { Animatable(-15f) }
    val char1Alpha = remember { Animatable(0f) }

    // --- 日文字符 "モ" 动画 ---
    val char2Scale = remember { Animatable(0.3f) }
    val char2TranslateY = remember { Animatable(50f) }
    val char2Rotation = remember { Animatable(-15f) }
    val char2Alpha = remember { Animatable(0f) }

    // --- 分割线动画 ---
    val dividerWidth = remember { Animatable(0f) }
    val dividerAlpha = remember { Animatable(0f) }

    // --- 英文 "Nemo" 滑入动画 ---
    val enTranslateY = remember { Animatable(100f) }
    val enAlpha = remember { Animatable(0f) }

    // --- 标语滑入动画 ---
    val sloganTranslateY = remember { Animatable(100f) }
    val sloganAlpha = remember { Animatable(0f) }

    // 启动动画序列
    LaunchedEffect(key1 = true) {
        // 1. 波纹1扩散 (delay 100ms)
        launch {
            delay(100)
            launch {
                ripple1Scale.animateTo(
                    targetValue = 10f,
                    animationSpec = tween(2500, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(800)
                ripple1Alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(1700, easing = LinearEasing)
                )
            }
        }

        // 2. 波纹2扩散 (delay 400ms)
        launch {
            delay(400)
            launch {
                ripple2Scale.animateTo(
                    targetValue = 10f,
                    animationSpec = tween(2500, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(800)
                ripple2Alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(1700, easing = LinearEasing)
                )
            }
        }

        // 3. 圆形几何图形漂浮
        launch {
            launch {
                circleAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(2000, easing = FastOutSlowInEasing)
                )
            }
            launch {
                circleTranslateY.animateTo(
                    targetValue = -30f,
                    animationSpec = tween(10000, easing = FastOutSlowInEasing)
                )
            }
        }

        // 4. 矩形几何图形漂浮
        launch {
            delay(200)
            launch {
                rectAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(2400, easing = FastOutSlowInEasing)
                )
            }
            launch {
                rectTranslateY.animateTo(
                    targetValue = -40f,
                    animationSpec = tween(12000, easing = FastOutSlowInEasing)
                )
            }
            launch {
                rectRotation.animateTo(
                    targetValue = 90f,
                    animationSpec = tween(12000, easing = FastOutSlowInEasing)
                )
            }
        }

        // 5. 日文字符 "ネ" 弹性弹出 (delay 300ms)
        launch {
            delay(300)
            launch {
                char1Alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            }
            launch {
                char1Scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                char1TranslateY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                char1Rotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }

        // 6. 日文字符 "モ" 弹性弹出 (delay 450ms)
        launch {
            delay(450)
            launch {
                char2Alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            }
            launch {
                char2Scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                char2TranslateY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                char2Rotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }

        // 7. 分割线展开 (delay 800ms)
        launch {
            delay(800)
            launch {
                dividerAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                )
            }
            launch {
                dividerWidth.animateTo(
                    targetValue = 60f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            }
        }

        // 8. 英文 "Nemo" 滑入 (delay 900ms)
        launch {
            delay(900)
            launch {
                enAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            }
            launch {
                enTranslateY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            }
        }

        // 9. 标语滑入 (delay 1050ms)
        launch {
            delay(1050)
            launch {
                sloganAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            }
            launch {
                sloganTranslateY.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            }
        }

        // 等待动画播放完成
        delay(2500)
        animationFinished = true
    }

    // 监听条件：动画完成 且 认证状态已检查
    LaunchedEffect(animationFinished, isAuthReady) {
        if (animationFinished && isAuthReady) {
            onTimeout()
        }
    }

    // 界面布局 - 使用主题色纯色背景
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryColor),
        contentAlignment = Alignment.Center
    ) {
        // --- 背景波纹层 ---
        // 波纹1
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(ripple1Scale.value)
                .alpha(ripple1Alpha.value)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )
        // 波纹2
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(ripple2Scale.value)
                .alpha(ripple2Alpha.value)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        )

        // --- 悬浮几何图形层 ---
        // 圆形 - 左上方
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopStart)
                .offset(x = (-50).dp, y = 120.dp)
                .graphicsLayer {
                    alpha = circleAlpha.value
                    translationY = circleTranslateY.value
                }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
        )
        // 圆角矩形 - 右下方
        Box(
            modifier = Modifier
                .size(100.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = (-160).dp)
                .graphicsLayer {
                    alpha = rectAlpha.value
                    translationY = rectTranslateY.value
                    rotationZ = rectRotation.value
                }
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.08f))
        )

        // --- 核心文字内容层 ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 日文 "ネモ" - 弹性弹出
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "ネ"
                Text(
                    text = "ネ",
                    fontSize = 90.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = char1Scale.value
                            scaleY = char1Scale.value
                            translationY = char1TranslateY.value
                            rotationZ = char1Rotation.value
                            alpha = char1Alpha.value
                        }
                )
                // "モ"
                Text(
                    text = "モ",
                    fontSize = 90.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = char2Scale.value
                            scaleY = char2Scale.value
                            translationY = char2TranslateY.value
                            rotationZ = char2Rotation.value
                            alpha = char2Alpha.value
                        }
                )
            }

            // 白色分割线
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 30.dp)
                    .height(4.dp)
                    .width(dividerWidth.value.dp)
                    .alpha(dividerAlpha.value)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )

            // 英文 "Nemo" - 遮罩滑入
            Box(
                modifier = Modifier.clipToBounds()
            ) {
                Text(
                    text = "Nemo",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = enTranslateY.value
                            alpha = enAlpha.value
                        }
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            // 标语 "解锁日语新视界" - 遮罩滑入
            Box(
                modifier = Modifier.clipToBounds()
            ) {
                Text(
                    text = "解锁日语新视界",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 4.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = sloganTranslateY.value
                            alpha = sloganAlpha.value
                        }
                )
            }
        }
    }
}

private fun Modifier.clipToBounds(): Modifier = this.then(
    Modifier.graphicsLayer { clip = true }
)
