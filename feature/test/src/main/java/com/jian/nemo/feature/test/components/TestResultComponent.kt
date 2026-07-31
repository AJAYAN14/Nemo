package com.jian.nemo.feature.test.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.shadow
import com.jian.nemo.core.ui.modifier.softCardShadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.annotation.SuppressLint
import androidx.compose.material.icons.automirrored.filled.MenuBook
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.designsystem.theme.TestResultPalette
import com.jian.nemo.core.ui.component.liquid.LiquidButton
import androidx.compose.ui.unit.sp
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter

/**
 * 通用测试结果展示组件
 * @param correctAnswers 答对题目数
 * @param totalQuestions 总题目数
 * @param wrongAnswers 错题列表
 * @param startTimeMillis 测试开始时间戳（毫秒）
 * @param endTimeMillis 测试结束时间戳（毫秒）
 * @param actualWordCount 实际使用的单词数量（混合测试时使用）
 * @param actualGrammarCount 实际使用的语法数量（混合测试时使用）
 * @param onRetakeTest 重新测试回调
 * @param onExitTest 退出测试回调
 */
@SuppressLint("MissingPermission")
@Composable
fun TestResultComponent(
    correctAnswers: Int,
    totalQuestions: Int,
    wrongAnswers: List<TestQuestion>,
    startTimeMillis: Long,
    endTimeMillis: Long,
    actualWordCount: Int = 0,
    actualGrammarCount: Int = 0,
    todayTestCount: Int = 0,
    todayAccuracy: Float = 0f,
    onRetakeTest: () -> Unit,
    onExitTest: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val accuracy = if (totalQuestions > 0) (correctAnswers * 100) / totalQuestions else 0
    val wrongAnswersCount = totalQuestions - correctAnswers
    val timeElapsedMillis = kotlin.math.max(0L, endTimeMillis - startTimeMillis)
    val timeElapsedSeconds = (timeElapsedMillis / 1000).toInt()
    val minutes = timeElapsedSeconds / 60
    val seconds = timeElapsedSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    val accuracyColor = getAccuracyColor(accuracy)
    val isDark = colorScheme.background.luminance() < 0.5f
    val textSub = if (isDark) TestResultPalette.SecondaryTextDark else TestResultPalette.SecondaryTextLight
    val context = LocalContext.current
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (accuracy > 0) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            delay(1500) // 等待圆环动画结束

            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            }

            if (accuracy > 85) {
                // >85分时提供第二次心跳震动
                delay(150)
                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(80)
                    }
                }

                // 触发满分彩纸特效
                showConfetti = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 感性光晕背景层 (方案二：Apple Aurora 玻璃极光渐变)
        // 使用两个不同位置带透明度的 RadialGradient 模拟超大范围和高性能的散光模糊，适配所有版本系统
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 左上角主光晕 (颜色随用户的测试正确率成绩好坏变化)
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(accuracyColor.copy(alpha = if (isDark) 0.35f else 0.25f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.1f),
                    radius = size.minDimension * 0.8f
                ),
                radius = size.minDimension * 0.8f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.1f)
            )

            // 右下角辅助光晕 (紫色或蓝色，用来丰富背景层的冷暖对比与空间通透感)
            val secondaryAuraColor = if (isDark) Color(0xFF8B5CF6) else Color(0xFF60A5FA)
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(secondaryAuraColor.copy(alpha = if (isDark) 0.25f else 0.15f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.9f),
                    radius = size.minDimension * 0.9f
                ),
                radius = size.minDimension * 0.9f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.9f)
            )
        }

        // 上层内容列表
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                item {
                    // 标题和激励语
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            val gradeIcon = when {
                                accuracy > 85 -> Icons.Filled.EmojiEvents // 原生奖杯 SVG
                                accuracy >= 70 -> Icons.Filled.Star // 原生星星 SVG
                                accuracy >= 60 -> Icons.Filled.CheckCircle // 原生达标 SVG
                                else -> Icons.AutoMirrored.Filled.MenuBook // 原生书本 SVG
                            }
                            Icon(
                                imageVector = gradeIcon,
                                contentDescription = "评级图标",
                                modifier = Modifier
                                    .padding(16.dp)
                                    .size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "测试完成！",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getMotivationalQuote(accuracy),
                            style = MaterialTheme.typography.titleMedium,
                            color = textSub,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                item {
                    var targetProgress by remember { mutableFloatStateOf(0f) }
                    val currentProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                        label = "progress_anim"
                    )

                    LaunchedEffect(Unit) {
                        targetProgress = accuracy / 100f
                    }

                    val animatedAccuracy = (currentProgress * 100).toInt()

                    // 环形进度条
                    CircularProgressBar(
                        progress = currentProgress,
                        progressColor = accuracyColor,
                        modifier = Modifier.size(192.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "${animatedAccuracy}%",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = accuracyColor
                            )
                            Text(
                                text = "正确率",
                                style = MaterialTheme.typography.titleMedium,
                                color = textSub
                            )
                        }
                    }
                }

                item {
                    // 关键指标卡片 (答对、答错、用时)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 答对卡片 - 采用清新的亮绿色系
                        StatCard(
                            icon = Icons.Default.Check,
                            value = correctAnswers.toString(),
                            label = "答对",
                            backgroundColor = if (isDark) TestResultPalette.CorrectCardBgDark else TestResultPalette.CorrectCardBgLight,
                            contentColor = if (isDark) TestResultPalette.CorrectCardContentDark else TestResultPalette.CorrectCardContentLight,
                            modifier = Modifier.weight(1f)
                        )

                        // 答错卡片 - 采用更柔和的亮粉色系
                        StatCard(
                            icon = Icons.Default.Close,
                            value = wrongAnswersCount.toString(),
                            label = "答错",
                            backgroundColor = if (isDark) TestResultPalette.WrongCardBgDark else TestResultPalette.WrongCardBgLight,
                            contentColor = if (isDark) TestResultPalette.WrongCardContentDark else TestResultPalette.WrongCardContentLight,
                            modifier = Modifier.weight(1f)
                        )

                        // 用时卡片 - 采用温暖的明黄色系
                        StatCard(
                            icon = Icons.Default.AccessTime,
                            value = timeFormatted,
                            label = "用时",
                            backgroundColor = if (isDark) TestResultPalette.TimeCardBgDark else TestResultPalette.TimeCardBgLight,
                            contentColor = if (isDark) TestResultPalette.TimeCardContentDark else TestResultPalette.TimeCardContentLight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 显示混合测试的实际单词/语法比例 - 重新设计为精简的比例条
                if (actualWordCount > 0 && actualGrammarCount > 0) {
                    item {
                        ContentAnalysisSection(
                            wordCount = actualWordCount,
                            grammarCount = actualGrammarCount,
                            isDark = isDark
                        )
                    }
                }

                // 今日战报 - 新增模块
                item {
                    TodaySummarySection(
                        todayTestCount = todayTestCount,
                        todayAccuracy = todayAccuracy,
                        isDark = isDark
                    )
                }
            }
        }

        // 底部操作按钮 - 使用项目通用 LiquidButton 液态按钮
        val indigo600 = Color(0xFF4F46E5)
        val slate700 = Color(0xFF334155)
        val slate200 = Color(0xFFE2E8F0)
        val buttonShape = RoundedCornerShape(24.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LiquidButton(
                onClick = onExitTest,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                backgroundColor = if (isDark) Color(0xFF334155) else slate200,
                shape = buttonShape,
                isInteractive = true,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "返回菜单",
                    color = if (isDark) Color.White else slate700,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }

            LiquidButton(
                onClick = onRetakeTest,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                backgroundColor = indigo600,
                shape = buttonShape,
                isInteractive = true,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "再来一次",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }
    }

        // 满分五彩纸屑特效层
        AnimatedVisibility(
            visible = showConfetti,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(1500)),
            modifier = Modifier.fillMaxSize()
        ) {
            val party = Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                spread = 360,
                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, 0x10B981, 0x3B82F6),
                emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
                position = Position.Relative(0.5, 0.3)
            )

            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf<Party>(party)
            )
        }
    }
}

/**
 * 环形进度条组件
 */
@Composable
fun CircularProgressBar(
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 16.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(192.dp)) {
            val canvasSize = size.minDimension
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val startAngle = -90f
            val sweepAngle = 360f * progress

            // 绘制背景圆环
            drawArc(
                color = backgroundColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = Size(canvasSize - strokeWidth.toPx(), canvasSize - strokeWidth.toPx()),
                topLeft = androidx.compose.ui.geometry.Offset(
                    strokeWidth.toPx() / 2,
                    strokeWidth.toPx() / 2
                )
            )

            // 绘制进度圆环
            drawArc(
                color = progressColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = stroke,
                size = Size(canvasSize - strokeWidth.toPx(), canvasSize - strokeWidth.toPx()),
                topLeft = androidx.compose.ui.geometry.Offset(
                    strokeWidth.toPx() / 2,
                    strokeWidth.toPx() / 2
                )
            )
        }

        // 中心内容
        content()
    }
}

/**
 * 统计卡片组件
 * 完全复刻旧项目 StatCard
 */
@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = backgroundColor.luminance() < 0.5f
    Surface(
        modifier = modifier.softCardShadow(borderRadius = 24.dp, isDark = isDark),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor
            )
        }
    }
}

/**
 * 内容分布分析组件 - 重新设计为分段比例条
 */
@Composable
fun ContentAnalysisSection(
    wordCount: Int,
    grammarCount: Int,
    isDark: Boolean
) {
    val total = wordCount + grammarCount
    val wordWeight = wordCount.toFloat() / total
    val wordPercent = (wordWeight * 100).toInt()
    val grammarPercent = 100 - wordPercent

    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 24.dp, isDark = isDark),
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "测试内容分布",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 分段比例条
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // 单词部分
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(wordWeight)
                        .background(if (isDark) TestResultPalette.WordAccentDark else TestResultPalette.WordAccentLight)
                )
                // 语法部分
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f - wordWeight)
                        .background(if (isDark) TestResultPalette.GrammarAccentDark else TestResultPalette.GrammarAccentLight)
                )
            }

            // 比例标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (isDark) TestResultPalette.WordAccentDark else TestResultPalette.WordAccentLight))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "单词 $wordPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "语法 $grammarPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (isDark) TestResultPalette.GrammarAccentDark else TestResultPalette.GrammarAccentLight))
                }
            }
        }
    }
}

/**
 * 今日战报组件
 */
@Composable
fun TodaySummarySection(
    todayTestCount: Int,
    todayAccuracy: Float,
    isDark: Boolean
) {
    val containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .softCardShadow(borderRadius = 24.dp, isDark = isDark),
        shape = RoundedCornerShape(24.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今日累计战报",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "已完成 $todayTestCount 次测试",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${(todayAccuracy * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "平均正确率",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 根据正确率获取颜色 (现代高对比色彩配色)
 */
@Composable
fun getAccuracyColor(accuracy: Int): Color {
    // 使用更高饱和度与明度来解决原先暗淡的问题
    return when {
        accuracy >= 90 -> Color(0xFF10B981) // 翡翠绿 (Emerald 500)
        accuracy >= 80 -> Color(0xFF34D399) // 翠绿 (Emerald 400)
        accuracy >= 60 -> Color(0xFFFBBF24) // 琥珀黄 (Amber 400)
        else -> Color(0xFFEF4444) // 警示红 (Red 500)
    }
}

/**
 * 获取激励性语句
 */
fun getMotivationalQuote(accuracy: Int): String {
    return when {
        accuracy == 100 -> "完美！你是学习大师！"
        accuracy >= 80 -> "太棒了！继续保持！"
        accuracy >= 60 -> "不错！离成功又近了一步！"
        else -> "别灰心，温故而知新！"
    }
}
