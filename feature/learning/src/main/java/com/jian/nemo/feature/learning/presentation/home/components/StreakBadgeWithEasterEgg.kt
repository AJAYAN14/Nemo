package com.jian.nemo.feature.learning.presentation.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// --- 数据模型 ---

// 爆炸粒子
data class ExplodingParticle(
    val emoji: String,
    val angle: Float,
    val maxDistance: Float,
    val scaleModifier: Float,
    val rotationModifier: Float
)

// 上升粒子
data class RisingParticle(
    val emoji: String,
    val startX: Float,
    val targetX: Float,
    val targetY: Float,
    val scaleModifier: Float,
    val delayProgress: Float
)

@Composable
fun StreakBadgeWithEasterEgg(
    streakDays: Int,
    badgeBgColor: Color,
    textColor: Color
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    
    // --- 动画状态 ---
    
    // 特效类型：0 = 无，1 = 烟花爆炸，2 = 粒子上升
    var currentEffect by remember { mutableIntStateOf(0) }
    val explosionProgress = remember { Animatable(0f) }
    
    // 徽章通用动画状态
    val scale = remember { Animatable(1f) }          // 用于方案一的弹簧缩放
    val badgeOffsetY = remember { Animatable(0f) }   // 用于方案二的悬浮
    val fireScale = remember { Animatable(1f) }      // 用于方案二的火苗闪烁
    val fireRotation = remember { Animatable(0f) }

    // --- 粒子生成 ---
    
    val explodingParticles = remember {
        val emojis = listOf("✨", "💫", "🌟", "🔥", "✨")
        List(15) {
            ExplodingParticle(
                emoji = emojis.random(),
                angle = Random.nextFloat() * 2 * Math.PI.toFloat(),
                maxDistance = with(density) { (Random.nextFloat() * 80f + 60f).dp.toPx() },
                scaleModifier = Random.nextFloat() * 0.8f + 0.6f,
                rotationModifier = Random.nextFloat() * 360f
            )
        }
    }

    val risingParticles = remember {
        val emojis = listOf("✨", "🔥", "🟠", "🟡", "🔸") 
        List(25) {
            RisingParticle(
                emoji = emojis.random(),
                startX = with(density) { (Random.nextFloat() * 40f - 20f).dp.toPx() },
                targetX = with(density) { (Random.nextFloat() * 120f - 60f).dp.toPx() },
                targetY = with(density) { -(Random.nextFloat() * 80f + 80f).dp.toPx() },
                scaleModifier = Random.nextFloat() * 0.7f + 0.5f,
                delayProgress = Random.nextFloat() * 0.4f
            )
        }
    }

    Box(contentAlignment = Alignment.Center) {
        
        // --- 粒子渲染层 ---
        if (currentEffect == 1) {
            // 方案一：烟花爆炸
            explodingParticles.forEach { particle ->
                Text(
                    text = particle.emoji,
                    fontSize = 16.sp,
                    modifier = Modifier.graphicsLayer {
                        val currentDistance = particle.maxDistance * explosionProgress.value
                        translationX = cos(particle.angle) * currentDistance
                        translationY = sin(particle.angle) * currentDistance
                        
                        val currentScale = particle.scaleModifier * (1f - explosionProgress.value * 0.3f)
                        scaleX = currentScale
                        scaleY = currentScale
                        rotationZ = particle.rotationModifier + (explosionProgress.value * 180f)
                        alpha = if (explosionProgress.value < 0.6f) 1f else 1f - ((explosionProgress.value - 0.6f) / 0.4f)
                    }
                )
            }
        } else if (currentEffect == 2) {
            // 方案二：上升漂浮
            risingParticles.forEach { particle ->
                Text(
                    text = particle.emoji,
                    fontSize = 12.sp,
                    modifier = Modifier.graphicsLayer {
                        val effectiveProgress = ((explosionProgress.value - particle.delayProgress) / (1f - particle.delayProgress)).coerceIn(0f, 1f)
                        if (effectiveProgress <= 0f) {
                            alpha = 0f
                        } else {
                            val xProgress = effectiveProgress * effectiveProgress
                            translationX = particle.startX + (particle.targetX - particle.startX) * xProgress
                            val yProgress = 1f - (1f - effectiveProgress) * (1f - effectiveProgress)
                            translationY = particle.targetY * yProgress
                            
                            val currentScale = if (effectiveProgress < 0.2f) {
                                particle.scaleModifier * (effectiveProgress / 0.2f)
                            } else {
                                particle.scaleModifier * (1f - (effectiveProgress - 0.2f) / 0.8f)
                            }
                            scaleX = currentScale
                            scaleY = currentScale
                            alpha = 1f - (effectiveProgress * effectiveProgress)
                        }
                    }
                )
            }
        }

        // --- 徽章本体层 ---
        Surface(
            color = badgeBgColor,
            shape = CircleShape,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    translationY = badgeOffsetY.value
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (currentEffect != 0) return@detectTapGestures // 防止重复触发
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            
                            // 随机选择方案 1 还是 2
                            currentEffect = if (Random.nextBoolean()) 1 else 2
                            
                            coroutineScope.launch {
                                if (currentEffect == 1) {
                                    // 方案一的徽章动画：弹簧按压
                                    launch {
                                        scale.animateTo(0.85f, tween(150, easing = FastOutSlowInEasing))
                                        scale.animateTo(1.1f, spring(dampingRatio = 0.5f, stiffness = 400f))
                                        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow))
                                    }
                                    // 方案一的粒子动画：快速炸开
                                    launch {
                                        explosionProgress.snapTo(0f)
                                        explosionProgress.animateTo(1f, tween(1000, easing = LinearOutSlowInEasing))
                                        currentEffect = 0
                                    }
                                } else {
                                    // 方案二的徽章动画：悬浮
                                    launch {
                                        val liftPx = with(density) { -6.dp.toPx() }
                                        badgeOffsetY.animateTo(liftPx, tween(200, easing = FastOutSlowInEasing))
                                        badgeOffsetY.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 400f))
                                    }
                                    // 方案二的火苗动画：闪烁抖动
                                    launch {
                                        repeat(2) {
                                            fireScale.animateTo(1.3f, tween(100))
                                            fireRotation.animateTo(-15f, tween(100))
                                            fireScale.animateTo(0.9f, tween(100))
                                            fireRotation.animateTo(15f, tween(100))
                                        }
                                        fireScale.animateTo(1f, tween(150))
                                        fireRotation.animateTo(0f, tween(150))
                                    }
                                    // 方案二的粒子动画：缓慢上升
                                    launch {
                                        explosionProgress.snapTo(0f)
                                        explosionProgress.animateTo(1f, tween(1500, easing = LinearOutSlowInEasing))
                                        currentEffect = 0
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .padding(4.dp)
                    .height(30.dp)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "🔥", 
                    fontSize = 14.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = fireScale.value
                        scaleY = fireScale.value
                        rotationZ = fireRotation.value
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$streakDays 天",
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
