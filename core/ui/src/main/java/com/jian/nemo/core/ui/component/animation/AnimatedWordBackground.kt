package com.jian.nemo.core.ui.component.animation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringArrayResource
import com.jian.nemo.core.ui.R
import kotlin.math.PI
import kotlin.math.sin

/**
 * 动画背景组件 - 高性能优化完整版
 *
 * 优化原理：
 * 利用 Modifier.graphicsLayer {} 将动画状态的读取推迟到 "绘制阶段 (Draw Phase)"。
 * 这样可以避免 Compose 在每一帧动画时都进行 "组合 (Composition)" 和 "布局 (Layout)"，
 * 从而彻底消除掉帧和卡顿，即使在低端设备上也能保持 60/120 FPS。
 */
@Composable
fun AnimatedWordBackground(
    modifier: Modifier = Modifier,
    containerSize: IntSize,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onPrimary // Added customizable color
) {
    // 词库
    val words = stringArrayResource(R.array.core_ui_animation_label_floating_words).toList()

    // 动画配置，避免每次渲染时都重复计算
    val padding = containerSize.height * 0.1f
    val safeHeight = (containerSize.height - padding * 2).toInt()
    val range = if (safeHeight > 0) safeHeight else 100

    val infiniteTransition = rememberInfiniteTransition(label = "WordFishTransition")

    // 渲染单词
    Box(modifier = modifier) {
        words.forEachIndexed { index, word ->
            WordFish(
                word = word,
                index = index,
                containerWidth = containerSize.width.toFloat(),
                containerHeight = containerSize.height.toFloat(),
                infiniteTransition = infiniteTransition,
                padding = padding,
                safeHeight = safeHeight,
                range = range,
                contentColor = contentColor // Pass color down
            )
        }
    }
}

/**
 * 单个单词游动组件
 */
@Composable
private fun WordFish(
    word: String,
    index: Int,
    containerWidth: Float,
    containerHeight: Float,
    infiniteTransition: androidx.compose.animation.core.InfiniteTransition,
    padding: Float,
    safeHeight: Int,
    range: Int,
    contentColor: androidx.compose.ui.graphics.Color // Receive color
) {
    // 只有当容器有实际大小时才渲染
    if (containerWidth <= 0 || containerHeight <= 0) return

    // --- 1. 静态配置区 (使用 remember 锁定，不参与重组) ---

    // 垂直位置：基于容器高度动态计算，上下各留出 10% 安全区
    val randomYPosition = remember(word) {
        (padding.toInt()..(padding.toInt() + range)).random().toFloat()
    }

    // ... (rest of configuration)

    val randomDirection = remember(word) { (0..1).random() }
    val isLeftToRight = randomDirection == 0

    // 外观参数
    val randomSize = remember(word) { (18..34).random() } // 字号
    val randomAlphaBase = remember(word) { (4..8).random() / 10f } // 基础最大透明度 (0.4-0.8)

    // 波动参数 (正弦波)
    val randomWaveAmplitude = remember(word) { (15..45).random().toFloat() } // 振幅
    val randomWaveSpeed = remember(word) { (3000..6000).random() } // 波动周期

    // 速度分层
    val speedTier = remember(word) {
        when ((index + word.hashCode()) % 4) {
            0 -> "slow"      // 慢速背景
            1 -> "medium"    // 标准
            2 -> "fast"      // 活跃
            else -> "veryFast"
        }
    }

    val baseDuration = remember(word) {
        when (speedTier) {
            "slow" -> (25000..35000).random()     // 极慢，营造深海感
            "medium" -> (20000..25000).random()
            "fast" -> (15000..20000).random()
            else -> (10000..15000).random()       // 最快
        }
    }

    // 物理微调：大字惯性大游得慢
    val sizeSpeedModifier = remember(word, randomSize) {
        when {
            randomSize >= 30 -> 1.2f
            randomSize <= 22 -> 0.9f
            else -> 1.0f
        }
    }

    val finalDuration = (baseDuration * sizeSpeedModifier).toInt()

    // 随机起始偏移，让单词分散在屏幕各处
    val startOffsetMillis = remember(word) { (0..finalDuration).random() }

    // --- 2. 动画状态定义 (关键：不要解包 State) ---

    // 水平位移状态 (注意：这里返回的是 State<Float> 对象，我们不读它的值)
    val xOffsetState = infiniteTransition.animateFloat(
        initialValue = -200f, // 从屏幕外更远一点开始
        targetValue = containerWidth + 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = finalDuration,
                easing = LinearEasing // 线性匀速，最平滑
            ),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(startOffsetMillis, StartOffsetType.FastForward)
        ),
        label = "WordX"
    )

    // 垂直波动相位状态
    val wavePhaseState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = randomWaveSpeed,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "WordWave"
    )

    // --- 3. UI 渲染 (关键：属性在 graphicsLayer 中应用) ---

    Text(
        text = word,
        // 颜色固定，不要在这里设置 alpha，否则会触发重绘
        color = contentColor, // Use passed color
        fontSize = randomSize.sp,
        // 使用艺术字体 (Yuji Syuku)
        fontFamily = androidx.compose.ui.text.font.FontFamily(
            androidx.compose.ui.text.font.Font(com.jian.nemo.core.designsystem.R.font.yuji)
        ),
        // fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
        fontWeight = FontWeight.Bold,
        style = TextStyle(
            shadow = Shadow(
                color = contentColor.copy(alpha = 0.3f), // Shadow uses same color base
                offset = Offset(2f, 2f),
                blurRadius = 3f
            )
        ),
        modifier = Modifier
            .graphicsLayer {
                // === GPU 绘制阶段 (Draw Phase) ===
                // 在这里读取 State 的值，不会触发 Composable 重组 (Recomposition)
                // 这是性能优化的核心！

                val xAnimValue = xOffsetState.value
                val waveAnimValue = wavePhaseState.value

                // 计算位置
                val currentX = if (isLeftToRight) xAnimValue else (containerWidth - xAnimValue)
                val waveY = (sin(waveAnimValue.toDouble()) * randomWaveAmplitude).toFloat()
                val currentY = randomYPosition + waveY

                // 应用位置变换
                translationX = currentX
                translationY = currentY

                // 计算边缘淡入淡出 (Fade In/Out)
                val fadeZone = 200f // 边缘 200px 渐变区域
                val calculatedAlpha = when {
                    currentX < 0 -> 0f
                    currentX < fadeZone -> (currentX / fadeZone) * randomAlphaBase
                    currentX > containerWidth -> 0f
                    currentX > containerWidth - fadeZone -> ((containerWidth - currentX) / fadeZone) * randomAlphaBase
                    else -> randomAlphaBase
                }

                // 直接设置图层透明度，性能极高
                this.alpha = calculatedAlpha.coerceIn(0f, 1f)
            }
    )
}
