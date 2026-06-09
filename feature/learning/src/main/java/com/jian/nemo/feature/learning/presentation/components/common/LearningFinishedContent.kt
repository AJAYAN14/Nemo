package com.jian.nemo.feature.learning.presentation.components.common

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.NemoText
import com.jian.nemo.core.designsystem.theme.NemoTextLight
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

// 今日学习任务完成内容组件 (Premium Design)
@Composable
fun LearningFinishedContent(
    title: String = "今日任务达成！",
    subtitle: String = "坚持就是胜利，明天继续加油",
    completedToday: Int = 0,
    dailyGoal: Int = 20,
    sessionDurationSeconds: Long = 0L,
    sessionMaxCombo: Int = 0,
    sessionNewCount: Int = 0,
    sessionReviewCount: Int = 0,
    sessionRelearnCount: Int = 0,
    tomorrowReviewForecastCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // 🆕 结算页专属多色循环渐变过渡动画（薄荷绿 -> 珊瑚橙 -> 海蓝 -> 紫罗兰）
    val infiniteTransition = rememberInfiniteTransition(label = "morphColorTransition")
    val morphColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF10B981),
        targetValue = Color(0xFF10B981),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 6000
                Color(0xFF10B981) at 0 with LinearEasing
                Color(0xFFF97316) at 1500 with LinearEasing
                Color(0xFF0EA5E9) at 3000 with LinearEasing
                Color(0xFFA855F7) at 4500 with LinearEasing
                Color(0xFF10B981) at 6000 with LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "morphColor"
    )

    // 动画状态定义
    val outerScale = remember { Animatable(0f) }
    val innerScale = remember { Animatable(0f) }
    val centerScale = remember { Animatable(0.5f) }
    val centerAlpha = remember { Animatable(0f) }
    
    val titleAlpha = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(30f) }
    
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffsetY = remember { Animatable(30f) }

    // 🆕 4组扁平卡片各自独立的动效状态
    val card1Alpha = remember { Animatable(0f) }
    val card1OffsetY = remember { Animatable(30f) }

    val card2Alpha = remember { Animatable(0f) }
    val card2OffsetY = remember { Animatable(30f) }

    val card3Alpha = remember { Animatable(0f) }
    val card3OffsetY = remember { Animatable(30f) }

    val card4Alpha = remember { Animatable(0f) }
    val card4OffsetY = remember { Animatable(30f) }
    
    val quoteAlpha = remember { Animatable(0f) }
    val quoteOffsetY = remember { Animatable(30f) }
    
    var showConfetti by remember { mutableStateOf(false) }

    // 震动辅助函数 (分级强度)
    @android.annotation.SuppressLint("MissingPermission")
    fun triggerVibrate(duration: Long, amplitude: Int) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(duration, amplitude.coerceIn(1, 255))
                    it.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(duration)
                }
            }
        } catch (e: Exception) {}
    }

    LaunchedEffect(Unit) {
        // T+100ms: 外环弹出 (Level 1)
        launch {
            delay(100)
            triggerVibrate(20, 50)
            outerScale.animateTo(1f, tween(400))
        }

        // T+200ms: 内环弹出 (Level 2)
        launch {
            delay(200)
            triggerVibrate(20, 100)
            innerScale.animateTo(1f, tween(400))
        }

        // T+400ms: 中心图标回弹入场 (Level 3)
        launch {
            delay(400)
            triggerVibrate(40, 180)
            launch { centerAlpha.animateTo(1f, tween(200)) }
            centerScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
        }

        // T+800ms: 文字滑入 (Level 4)
        launch {
            delay(800)
            triggerVibrate(15, 255)
            launch { titleAlpha.animateTo(1f, tween(600)) }
            titleOffsetY.animateTo(0f, tween(600))
        }

        launch {
            delay(900)
            launch { subtitleAlpha.animateTo(1f, tween(600)) }
            subtitleOffsetY.animateTo(0f, tween(600))
        }

        // 🆕 T+950ms: 卡片1滑入 (薄荷绿)
        launch {
            delay(950)
            launch { card1Alpha.animateTo(1f, tween(500)) }
            card1OffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }

        // 🆕 T+1050ms: 卡片2滑入 (珊瑚橙)
        launch {
            delay(1050)
            launch { card2Alpha.animateTo(1f, tween(500)) }
            card2OffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }

        // 🆕 T+1150ms: 卡片3滑入 (海蓝色)
        launch {
            delay(1150)
            launch { card3Alpha.animateTo(1f, tween(500)) }
            card3OffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }

        // 🆕 T+1250ms: 卡片4滑入 (紫罗兰)
        launch {
            delay(1250)
            launch { card4Alpha.animateTo(1f, tween(500)) }
            card4OffsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow))
        }

        // T+1400ms: 极简名言卡片滑入
        launch {
            delay(1400)
            launch { quoteAlpha.animateTo(1f, tween(600)) }
            quoteOffsetY.animateTo(0f, tween(600))
        }

        // T+1500ms: 启动彩花与终极震动
        launch {
            delay(1500)
            triggerVibrate(50, 200)
            showConfetti = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Hero Icon with Animation (Morph Loading with Check)
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = outerScale.value
                        scaleY = outerScale.value
                        alpha = outerScale.value.coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                // 播放平滑变色（多彩渐变）形变动画的加载器作为底座
                NemoChasingDotsLoader(
                    size = 120.dp,
                    color = morphColor,
                    modifier = Modifier.fillMaxSize()
                )
                
                // 中间的打勾✓图标
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "完成",
                    tint = Color.White,
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer {
                            scaleX = centerScale.value
                            scaleY = centerScale.value
                            alpha = centerAlpha.value
                        }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. Title & Subtitle with Animation
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = titleAlpha.value
                        translationY = with(density) { titleOffsetY.value.dp.toPx() }
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = NemoTextLight,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = subtitleAlpha.value
                        translationY = with(density) { subtitleOffsetY.value.dp.toPx() }
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🆕 3. 2x2 多彩色块扁平网格布局 (Flat Grid Card)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 第一行卡片 (时长 & 连击)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 卡片 1：学习用时 (清新薄荷绿)
                    FlatCard(
                        backgroundColor = Color(0xFF10B981),
                        alpha = card1Alpha.value,
                        offsetY = with(density) { card1OffsetY.value.dp.toPx() },
                        modifier = Modifier.weight(1f)
                    ) {
                        CardHeaderIcon(icon = Icons.Rounded.AccessTime)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "学习用时", 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            val minutes = (sessionDurationSeconds / 60).coerceAtLeast(1)
                            Text(
                                text = "$minutes",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "分钟",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 3.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 卡片 2：最高连击 (活力珊瑚橙)
                    FlatCard(
                        backgroundColor = Color(0xFFF97316),
                        alpha = card2Alpha.value,
                        offsetY = with(density) { card2OffsetY.value.dp.toPx() },
                        modifier = Modifier.weight(1f)
                    ) {
                        CardHeaderIcon(icon = Icons.Rounded.Star)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "最高连击", 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$sessionMaxCombo",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "次",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 3.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 第二行卡片 (预测 & 详情)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 卡片 3：明日复习 (明快海蓝色)
                    FlatCard(
                        backgroundColor = Color(0xFF0EA5E9),
                        alpha = card3Alpha.value,
                        offsetY = with(density) { card3OffsetY.value.dp.toPx() },
                        modifier = Modifier.weight(1f)
                    ) {
                        CardHeaderIcon(icon = Icons.Rounded.CalendarToday)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "明日复习", 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$tomorrowReviewForecastCount",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "个",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 3.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 卡片 4：今日学习详情 (柔和紫罗兰)
                    FlatCard(
                        backgroundColor = Color(0xFFA855F7),
                        alpha = card4Alpha.value,
                        offsetY = with(density) { card4OffsetY.value.dp.toPx() },
                        modifier = Modifier.weight(1f)
                    ) {
                        CardHeaderIcon(icon = Icons.Rounded.List)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "今日学习详情", 
                            fontSize = 12.sp, 
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DetailItem("新学", sessionNewCount)
                            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.3f)))
                            DetailItem("复习", sessionReviewCount)
                            Box(modifier = Modifier.width(1.dp).height(16.dp).background(Color.White.copy(alpha = 0.3f)))
                            DetailItem("重学", sessionRelearnCount)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. 极简名言卡片
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5
            val quoteBgColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                Color.White
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = quoteBgColor
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .graphicsLayer {
                        alpha = quoteAlpha.value
                        translationY = with(density) { quoteOffsetY.value.dp.toPx() }
                    }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "“温故而知新，可以为师矣。”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = if (isDark) NemoText else Color(0xFF64748B),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // 彩花特效层
        AnimatedVisibility(
            visible = showConfetti,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(1500)),
            modifier = Modifier.fillMaxSize()
        ) {
            val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
            val party = Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x10B981, primaryColorArgb),
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                position = Position.Relative(0.5, 0.3)
            )

            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(party)
            )
        }
    }
}

// 🆕 扁平色块战报网格辅助组件
@Composable
private fun FlatCard(
    backgroundColor: Color,
    alpha: Float,
    offsetY: Float,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .height(152.dp)
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            content()
        }
    }
}

@Composable
private fun CardHeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun RowScope.DetailItem(label: String, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = label, 
            fontSize = 10.sp, 
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$count", 
            fontSize = 18.sp, 
            fontWeight = FontWeight.Bold, 
            color = Color.White
        )
    }
}

// 兼容旧调用
@Composable
fun DailyGoalMetContent() {
    LearningFinishedContent()
}
