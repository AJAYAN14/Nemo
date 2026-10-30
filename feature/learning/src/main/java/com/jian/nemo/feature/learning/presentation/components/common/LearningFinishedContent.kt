package com.jian.nemo.feature.learning.presentation.components.common

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.dialog.BonusMode
import com.jian.nemo.core.ui.component.dialog.BonusStudyDialog
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.feature.learning.presentation.components.dialogs.DailyAchievementShareDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * 学习微会话及每日目标达成结算页面组件。
 *
 * 呈现学习汇总数据、出清明细、连击与用时战报，并支持快速开启追加学习。
 *
 * @param title 结算主标题
 * @param subtitle 结算副标题
 * @param completedToday 今日累计完成数
 * @param dailyGoal 每日目标数
 * @param sessionDurationSeconds 学习总用时（秒）
 * @param sessionMaxCombo 本次最高连击数
 * @param sessionNewCount 新词学习数
 * @param sessionReviewCount 复习单词数
 * @param sessionRelearnCount 重学/反复攻克单词数
 * @param tomorrowReviewForecastCount 明日自然到期复习预估数
 * @param showShareDialog 是否展示打卡海报弹窗
 * @param onDismissShareDialog 关闭打卡海报弹窗回调
 * @param onConfirmBonus 追加学习确认回调
 * @param onBackClick 返回事件回调
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    todayDueReviewCount: Int = 0,
    dailyStreak: Int = 1,
    showShareDialog: Boolean = false,
    onDismissShareDialog: () -> Unit = {},
    onConfirmBonus: (Int, BonusMode) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    showBonusCard: Boolean = true,
    showNextGroupButton: Boolean = false,
    onNextGroupClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 动态跟随 App 主题色系统
    val themePrimary = MaterialTheme.colorScheme.primary
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surface
    val cardBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val shadowSpotColor = Color.Black.copy(alpha = if (isDark) 0.32f else 0.05f)
    val shadowAmbientColor = Color.Black.copy(alpha = if (isDark) 0.18f else 0.02f)

    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    // 构成明细配色系统
    val accentGreen = Color(0xFF10B981) // 旧词巩固（复习）
    val accentBlue = Color(0xFF3B82F6)  // 新词拓展（新知）
    val accentGold = Color(0xFFF59E0B)  // 反复攻克 / 最高连击金
    val bonusOrange = Color(0xFFFF5500) // 原版加餐专属橙

    // 互斥三分类计算：旧词总数拆解为「顺畅巩固」与「反复攻克」，杜绝一词两算
    val smoothReviewCount = (sessionReviewCount - sessionRelearnCount).coerceAtLeast(0)
    val relearnCount = sessionRelearnCount
    val sessionTotalCount = (sessionNewCount + maxOf(sessionReviewCount, sessionRelearnCount)).coerceAtLeast(1)

    // 动效控制器
    val countUpNumber = remember { Animatable(0f) }
    val comboAnim = remember { Animatable(0f) }
    val barProgressAnim = remember { Animatable(0f) }
    var showBonusDialog by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }

    // 原版 2.4 秒平滑呼吸的 45° 斜扫光流动着色器 (Shimmer Sweep)
    val shimmerTransition = rememberInfiniteTransition(label = "ShimmerTransition")
    val shimmerProgress by shimmerTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerProgress"
    )

    // 触发结算仪式感动画与轻微触觉反馈
    LaunchedEffect(sessionTotalCount, sessionMaxCombo) {
        // 轻微触觉反馈
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(35)
                }
            }
        } catch (_: Exception) {}

        launch {
            countUpNumber.animateTo(
                targetValue = sessionTotalCount.toFloat(),
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(150)
            comboAnim.animateTo(
                targetValue = sessionMaxCombo.toFloat(),
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
            )
        }
        launch {
            delay(200)
            barProgressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(100)
            showConfetti = true
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // 1. 核心成就区 (Hero Area - 居中基线对齐，现代通透视觉)
            // ==========================================
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp)
            ) {
                // 居中大数字与单位（基线自然对齐，去除沉重方块背景）
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val displayedNumber = countUpNumber.value.roundToInt()
                    Text(
                        text = "$displayedNumber",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp,
                        color = textPrimary,
                        lineHeight = 64.sp
                    )
                    Text(
                        text = "词",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )
                }

                // 主标题
                Text(
                    text = title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.3).sp,
                    color = textPrimary
                )

                // 专注度或副标题动态激励反馈（HTML 同款极简耗时文案）
                val feedbackText = if (sessionDurationSeconds > 0 && sessionTotalCount > 0) {
                    val avgSeconds = String.format(Locale.getDefault(), "%.1f", sessionDurationSeconds.toFloat() / sessionTotalCount)
                    "平均每词 $avgSeconds 秒"
                } else {
                    subtitle
                }
                Text(
                    text = feedbackText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary.copy(alpha = 0.85f),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 2. 今日词量明细卡片（去术语化 + 堆叠比例进度条）
            // ==========================================
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                        spotColor = shadowSpotColor,
                        ambientColor = shadowAmbientColor
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 标头行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "今日词量",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFECFDF5).copy(alpha = if (isDark) 0.22f else 1f)
                        ) {
                            Text(
                                text = "100% 掌握",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // 双色/三色堆叠圆角进度条（严格保持原版：交界处平滑柔和渐变过渡与流动动画）
                    val progressGradientBrush = remember(
                        smoothReviewCount,
                        sessionNewCount,
                        relearnCount,
                        accentGreen,
                        accentBlue,
                        accentGold
                    ) {
                        val items = mutableListOf<Pair<Int, Color>>()
                        if (smoothReviewCount > 0) items.add(smoothReviewCount to accentGreen)
                        if (sessionNewCount > 0) items.add(sessionNewCount to accentBlue)
                        if (relearnCount > 0) items.add(relearnCount to accentGold)

                        if (items.isEmpty()) {
                            Brush.horizontalGradient(listOf(accentGreen, accentGreen))
                        } else if (items.size == 1) {
                            Brush.horizontalGradient(listOf(items[0].second, items[0].second))
                        } else {
                            val total = items.sumOf { it.first }.toFloat()
                            val stops = mutableListOf<Pair<Float, Color>>()
                            var currentProgress = 0f
                            val blendSpan = 0.035f // 3.5% 的平滑过渡带

                            stops.add(0f to items[0].second)

                            for (i in 0 until items.size - 1) {
                                val segmentFraction = items[i].first / total
                                val boundary = currentProgress + segmentFraction
                                val colorA = items[i].second
                                val colorB = items[i + 1].second

                                val transitionStart = (boundary - blendSpan).coerceAtLeast(currentProgress)
                                val transitionEnd = (boundary + blendSpan).coerceAtMost(1f)

                                stops.add(transitionStart to colorA)
                                stops.add(transitionEnd to colorB)

                                currentProgress = boundary
                            }

                            stops.add(1f to items.last().second)
                            Brush.horizontalGradient(colorStops = stops.toTypedArray())
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                    ) {
                        if (barProgressAnim.value > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barProgressAnim.value)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(progressGradientBrush)
                            )
                        }
                    }

                    // 三等分居中结构：与 HTML 设计 1:1 对齐，极简细分割线
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 第 1 等份: 旧词巩固
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentGreen)
                                )
                                Text(
                                    text = "旧词巩固",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$smoothReviewCount",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary,
                                    lineHeight = 22.sp
                                )
                                Text(
                                    text = "词",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    modifier = Modifier.padding(start = 2.dp, end = 6.dp, bottom = 2.dp)
                                )
                                val reviewPercent = ((smoothReviewCount.toFloat() / sessionTotalCount) * 100).roundToInt()
                                Text(
                                    text = "$reviewPercent%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        // 垂直细分割线 1
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(cardBorder.copy(alpha = 0.6f))
                        )

                        // 第 2 等份: 新词拓展
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(accentBlue)
                                )
                                Text(
                                    text = "新词拓展",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "$sessionNewCount",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary,
                                    lineHeight = 22.sp
                                )
                                Text(
                                    text = "词",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    modifier = Modifier.padding(start = 2.dp, end = 6.dp, bottom = 2.dp)
                                )
                                val newPercent = ((sessionNewCount.toFloat() / sessionTotalCount) * 100).roundToInt()
                                Text(
                                    text = "$newPercent%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }

                        // 第 3 等份: 攻克 (如果有反复攻克词数)
                        if (relearnCount > 0) {
                            // 垂直细分割线 2
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(cardBorder.copy(alpha = 0.6f))
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(accentGold)
                                    )
                                    Text(
                                        text = "攻克",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$relearnCount",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = textPrimary,
                                        lineHeight = 22.sp
                                    )
                                    Text(
                                        text = "词",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        modifier = Modifier.padding(start = 2.dp, end = 6.dp, bottom = 2.dp)
                                    )
                                    val relearnPercent = ((relearnCount.toFloat() / sessionTotalCount) * 100).roundToInt()
                                    Text(
                                        text = "$relearnPercent%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // 3. 最高连击与专注用时（双等分居中排版 + 细分割线）
            // ==========================================
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                        spotColor = shadowSpotColor,
                        ambientColor = shadowAmbientColor
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左半部：最高连击
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Whatshot,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "最高连击",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                        }
                        val displayedCombo = comboAnim.value.roundToInt()
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$displayedCombo",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary,
                                lineHeight = 24.sp
                            )
                            Text(
                                text = "次",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.padding(start = 3.dp, bottom = 2.dp)
                            )
                        }
                    }

                    // 中间极简垂直分割线
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(cardBorder.copy(alpha = 0.6f))
                    )

                    // 右半部：专注用时
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "专注用时",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                        }
                        val minutes = sessionDurationSeconds / 60
                        val seconds = sessionDurationSeconds % 60
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (minutes > 0) {
                                Text(
                                    text = "$minutes",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary,
                                    lineHeight = 24.sp
                                )
                                Text(
                                    text = "分",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 2.dp)
                                )
                            }
                            Text(
                                text = if (sessionDurationSeconds > 0) "$seconds" else "1",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary,
                                lineHeight = 24.sp
                            )
                            Text(
                                text = if (sessionDurationSeconds > 0) "秒" else "分",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // 4. 复习与预估卡片（今日待复习与明日自然到期预估）
            // ==========================================
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(18.dp),
                        clip = false,
                        spotColor = shadowSpotColor,
                        ambientColor = shadowAmbientColor
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    // 今日待复习词量
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Autorenew,
                                contentDescription = null,
                                tint = Color(0xFFF97316),
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "今日待复习词量",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${todayDueReviewCount.coerceAtLeast(0)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                            Text(
                                text = "词",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }

                    // 极简水平分割线
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = cardBorder.copy(alpha = 0.5f)
                    )

                    // 明日自然到期预估
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "明日自然到期预估",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textSecondary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${tomorrowReviewForecastCount.coerceAtLeast(0)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary
                            )
                            Text(
                                text = "词",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }

            if (showBonusCard) {
                Spacer(modifier = Modifier.height(14.dp))

                // ==========================================
                // 5. 今日加餐入口卡片（100% 保持原版效果 + 45° 斜扫光动效）
                // ==========================================
                Surface(
                    onClick = { showBonusDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isDark) Color(0xFF2B1F17) else Color(0xFFFFF7ED),
                    border = BorderStroke(
                        width = 1.dp,
                        color = bonusOrange.copy(alpha = if (isDark) 0.35f else 0.20f)
                    ),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(24.dp),
                            clip = false,
                            spotColor = shadowSpotColor,
                            ambientColor = shadowAmbientColor
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .drawWithContent {
                            drawContent()
                            // 原版 45° 斜切流光扫过绘制 (Shimmer Sweep Effect)
                            val width = size.width
                            val height = size.height
                            val xOffset = shimmerProgress * width

                            val shimmerBrush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (isDark) 0.25f else 0.45f),
                                    Color.Transparent
                                ),
                                start = Offset(xOffset, 0f),
                                end = Offset(xOffset + width * 0.4f, height * 1.2f)
                            )
                            drawRect(brush = shimmerBrush)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Flag,
                                contentDescription = null,
                                tint = bonusOrange,
                                modifier = Modifier.size(26.dp)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "今日加餐",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "突破常规，设定追加练习目标",
                                    fontSize = 13.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        // 右侧亮橙全胶囊标签
                        Surface(
                            shape = CircleShape,
                            color = bonusOrange,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "去加餐",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // 6. 底部主操作区 (Primary CTA)
            // ==========================================
            if (showNextGroupButton) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：完成今日学习（次要描边按钮）
                    OutlinedButton(
                        onClick = onBackClick,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, themePrimary.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = themePrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "完成今日学习",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 右侧：学习下一组（核心高亮按钮）
                    ColoredLiquidGlassButton(
                        onClick = onNextGroupClick,
                        tint = themePrimary,
                        shape = RoundedCornerShape(18.dp),
                        border = null,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Text(
                            text = "学习下一组",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                ColoredLiquidGlassButton(
                    onClick = onBackClick,
                    tint = themePrimary,
                    shape = RoundedCornerShape(18.dp),
                    border = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "完成今日学习",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // ==========================================
        // 7. 项目原版 KonfettiView 漫天五彩纸屑庆祝层
        // ==========================================
        AnimatedVisibility(
            visible = showConfetti,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(1200)),
            modifier = Modifier.fillMaxSize()
        ) {
            val partyColors = remember(themePrimary) {
                listOf(
                    themePrimary.toArgb(),
                    0xFF10B981.toInt(),
                    0xFF3B82F6.toInt(),
                    0xFFF59E0B.toInt(),
                    0xFFEC4899.toInt(),
                    0xFF6366F1.toInt()
                )
            }
            val party = remember(partyColors) {
                Party(
                    speed = 8f,
                    maxSpeed = 26f,
                    damping = 0.9f,
                    spread = 360,
                    colors = partyColors,
                    emitter = Emitter(duration = 150, TimeUnit.MILLISECONDS).max(80),
                    position = Position.Relative(0.5, 0.15)
                )
            }

            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(party)
            )
        }

        // 今日加餐配置弹窗（居中挑战配置对话框，传递词数与加餐模式）
        if (showBonusDialog) {
            BonusStudyDialog(
                onDismissRequest = { showBonusDialog = false },
                onConfirmBonus = { bonusCount, bonusMode ->
                    showBonusDialog = false
                    onConfirmBonus(bonusCount, bonusMode)
                }
            )
        }

        // 今日打卡成就海报弹窗
        if (showShareDialog) {
            DailyAchievementShareDialog(
                sessionTotalCount = sessionTotalCount,
                sessionMaxCombo = sessionMaxCombo,
                sessionDurationSeconds = sessionDurationSeconds,
                dailyStreak = dailyStreak,
                onDismissRequest = onDismissShareDialog
            )
        }
    }
}


