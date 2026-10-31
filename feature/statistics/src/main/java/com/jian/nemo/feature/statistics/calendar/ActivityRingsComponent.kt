// Lucian UX/UI Prompt #099 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#099
package com.jian.nemo.feature.statistics.calendar

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.theme.Rubik
import com.jian.nemo.core.designsystem.theme.ActivityRingPalette
import com.jian.nemo.core.domain.model.LearningStats
import com.jian.nemo.core.domain.model.ReviewForecast
import com.jian.nemo.core.domain.model.StudyRecord
import com.jian.nemo.core.ui.modifier.softCardShadow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import kotlin.math.cos
import kotlin.math.sin

/**
 * 规避时区与除法偏移的安全 EpochDay 转换函数
 */
fun Calendar.toEpochDay(): Long {
    return LocalDate.of(
        get(Calendar.YEAR),
        get(Calendar.MONTH) + 1,
        get(Calendar.DAY_OF_MONTH)
    ).toEpochDay()
}

fun Date.toEpochDay(): Long {
    val cal = Calendar.getInstance().apply { time = this@toEpochDay }
    return cal.toEpochDay()
}

/**
 * 活动环调色板
 */
object ActivityRingColors {
    val MovePink = ActivityRingPalette.MovePink      // 外环: 新学单词
    val ExerciseGreen = ActivityRingPalette.ExerciseGreen // 中环: 新学语法
    val StandCyan = ActivityRingPalette.StandCyan     // 内环: 复习达成
}

/**
 * 活动环单项数据
 */
data class ActivityRingItemData(
    val label: String,
    val color: Color,
    val actual: Int,
    val goal: Int,
    val unit: String,
    val progress: Float // actual / goal
)

/**
 * 将某一天的各项数据转换为 3 个环的数据
 */
fun calculateRingsForDay(
    epochDay: Long,
    todayEpochDay: Long,
    todayStats: LearningStats?,
    record: StudyRecord?,
    forecast: ReviewForecast?
): List<ActivityRingItemData> {
    val wordGoal = todayStats?.wordDailyGoal?.coerceAtLeast(1) ?: 50
    val grammarGoal = todayStats?.grammarDailyGoal?.coerceAtLeast(1) ?: 10

    val isToday = epochDay == todayEpochDay
    val isPast = epochDay < todayEpochDay

    val (wordsLearned, wordsGoal) = when {
        isToday -> (todayStats?.todayLearnedWords ?: 0) to wordGoal
        isPast -> (record?.learnedWords ?: 0) to wordGoal
        else -> 0 to wordGoal
    }

    val (grammarsLearned, grammarsGoal) = when {
        isToday -> (todayStats?.todayLearnedGrammars ?: 0) to grammarGoal
        isPast -> (record?.learnedGrammars ?: 0) to grammarGoal
        else -> 0 to grammarGoal
    }

    val (reviewed, reviewGoal) = when {
        isToday -> {
            val due = (todayStats?.dueWords ?: 0) + (todayStats?.dueGrammars ?: 0)
            val done = (todayStats?.todayReviewedWords ?: 0) + (todayStats?.todayReviewedGrammars ?: 0)
            val totalGoal = if (due + done > 0) due + done else 30
            done to totalGoal
        }
        isPast -> {
            val done = (record?.reviewedWords ?: 0) + (record?.reviewedGrammars ?: 0)
            val baselineGoal = 30
            done to baselineGoal
        }
        else -> {
            val forecasted = (forecast?.wordCount ?: 0) + (forecast?.grammarCount ?: 0)
            0 to forecasted.coerceAtLeast(1)
        }
    }

    val wordsProgress = if (wordsGoal > 0) wordsLearned.toFloat() / wordsGoal.toFloat() else 0f
    val grammarsProgress = if (grammarsGoal > 0) grammarsLearned.toFloat() / grammarsGoal.toFloat() else 0f
    val reviewProgress = if (reviewGoal > 0) reviewed.toFloat() / reviewGoal.toFloat() else 0f

    return listOf(
        ActivityRingItemData(
            label = "新学单词",
            color = ActivityRingColors.MovePink,
            actual = wordsLearned,
            goal = wordsGoal,
            unit = " 个",
            progress = wordsProgress
        ),
        ActivityRingItemData(
            label = "新学语法",
            color = ActivityRingColors.ExerciseGreen,
            actual = grammarsLearned,
            goal = grammarsGoal,
            unit = " 条",
            progress = grammarsProgress
        ),
        ActivityRingItemData(
            label = "复习达成",
            color = ActivityRingColors.StandCyan,
            actual = reviewed,
            goal = reviewGoal,
            unit = " 次",
            progress = reviewProgress
        )
    )
}

/**
 * #099 大三环画布 (支持超圈Lap投影、合环高亮微闪、多环联动高亮)
 */
@Composable
fun ActivityRingsCanvas(
    rings: List<ActivityRingItemData>,
    animProgress: List<Float>,
    highlightedIndex: Int?,
    flashIndex: Int?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val minDim = minOf(w, h)

        val strokeWidth = minDim * 0.105f
        // 外、中、内三环半径比例（对应 70, 51, 32 / 164）
        val radii = floatArrayOf(
            minDim * 0.427f,
            minDim * 0.311f,
            minDim * 0.195f
        )

        for (i in rings.indices) {
            val ring = rings[i]
            val r = radii[i]
            val p = animProgress.getOrElse(i) { 0f }

            val isDimmed = highlightedIndex != null && highlightedIndex != i
            val ringAlpha = if (isDimmed) 0.22f else 1.0f

            // 1. 底环 (Track)
            drawCircle(
                color = ring.color.copy(alpha = 0.20f * ringAlpha),
                radius = r,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 2. 第一圈 (Layer A: 0 ~ 1.0)
            val p1 = p.coerceIn(0f, 1f)
            if (p1 > 0.002f) {
                // 当进度收缩至接近 0 时平滑渐隐，规避 StrokeCap.Round 在 12 点钟位置形成突兀的端帽小药丸残留
                val fadeAlpha = if (p1 < 0.05f) (p1 / 0.05f) else 1.0f
                val effectiveAlpha = ringAlpha * fadeAlpha

                // 若触发合环微闪，在底层先绘制一层同色柔光晕
                if (flashIndex == i) {
                    drawArc(
                        color = ring.color.copy(alpha = 0.35f * effectiveAlpha),
                        startAngle = -90f,
                        sweepAngle = p1 * 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = strokeWidth * 1.35f, cap = StrokeCap.Round)
                    )
                }

                // 本体圆弧始终保持饱和原色，并在贴近 0 时自然消融
                drawArc(
                    color = ring.color.copy(alpha = effectiveAlpha),
                    startAngle = -90f,
                    sweepAngle = p1 * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. 第二圈 (Layer B: 超圈 Lap > 1.0)
            if (p > 1.002f) {
                val p2 = (p - 1.0f).coerceIn(0f, 1f)
                if (p2 > 0.002f) {
                    val fadeAlpha2 = if (p2 < 0.05f) (p2 / 0.05f) else 1.0f
                    drawArc(
                        color = ring.color.copy(alpha = ringAlpha * fadeAlpha2),
                        startAngle = -90f,
                        sweepAngle = p2 * 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - r, center.y - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // 4. 超圈端点圆帽与阴影 (Shadowed Tip)
                if (p > 1.02f) {
                    val angleDeg = -90.0 + (p.coerceAtMost(2.0f) * 360.0)
                    val angleRad = Math.toRadians(angleDeg)
                    val capX = center.x + (r * cos(angleRad)).toFloat()
                    val capY = center.y + (r * sin(angleRad)).toFloat()

                    // 末端阴影 (投在第一圈上)
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.40f * ringAlpha),
                        radius = (strokeWidth / 2f) + 2.5f,
                        center = Offset(capX, capY)
                    )

                    // 末端圆帽
                    drawCircle(
                        color = ring.color.copy(alpha = ringAlpha),
                        radius = strokeWidth / 2f,
                        center = Offset(capX, capY)
                    )
                }
            }
        }
    }
}

/**
 * #099 微型三环 (用于周选择器胶囊和月历网格小单元格)
 */
@Composable
fun MiniActivityRings(
    progresses: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val minDim = minOf(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val strokeWidth = minDim * 0.115f

        val radii = floatArrayOf(
            minDim * 0.43f,
            minDim * 0.30f,
            minDim * 0.17f
        )
        val colors = arrayOf(
            ActivityRingColors.MovePink,
            ActivityRingColors.ExerciseGreen,
            ActivityRingColors.StandCyan
        )

        for (i in 0 until 3) {
            val r = radii[i]
            val color = colors[i]
            val p = (progresses.getOrNull(i) ?: 0f).coerceIn(0f, 1f)

            // 底环
            drawCircle(
                color = color.copy(alpha = 0.22f),
                radius = r,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 进度弧
            if (p > 0.005f) {
                val fade = if (p < 0.06f) (p / 0.06f) else 1.0f
                drawArc(
                    color = color.copy(alpha = fade),
                    startAngle = -90f,
                    sweepAngle = p * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * 右侧指标项按钮组件 (可点击聚焦高亮某环)
 */
@Composable
fun ActivityRingItemRow(
    item: ActivityRingItemData,
    animatedActual: Int,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val surface2 = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        ActivityRingPalette.SurfaceWarmLight
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (isHighlighted) surface2 else Color.Transparent)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(item.color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = animatedActual.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Rubik,
                        fontWeight = FontWeight.Black
                    ),
                    color = item.color
                )
                Text(
                    text = " / ${item.goal}${item.unit}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 1.dp, start = 2.dp)
                )
            }
        }
    }
}

/**
 * 周模式 7 天胶囊选择器 (带微型三环)
 */
@Composable
fun ActivityWeekPicker(
    daysData: List<DayRingSummary>,
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val containerBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    } else {
        ActivityRingPalette.SurfaceWarmLight
    }

    Surface(
        color = containerBg,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysData.forEachIndexed { index, day ->
                val isSelected = index == selectedIndex
                val activeBg = if (isDark) MaterialTheme.colorScheme.surface else Color.White

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (isSelected) activeBg else Color.Transparent)
                        .then(
                            if (isSelected) {
                                Modifier.softCardShadow(borderRadius = 13.dp, isDark = isDark)
                            } else Modifier
                        )
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onSelectDay(index)
                        }
                        .padding(vertical = 7.dp, horizontal = 2.dp)
                ) {
                    MiniActivityRings(
                        progresses = day.progresses,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = day.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 月模式 7x5/6 微型三环月历网格 (Apple Fitness 经典风格)
 */
@Composable
fun ActivityMonthGrid(
    displayedCal: Calendar,
    selectedEpochDay: Long,
    todayEpochDay: Long,
    recordsMap: Map<Long, StudyRecord>,
    forecastMap: Map<Long, ReviewForecast>,
    todayStats: LearningStats?,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val surfaceBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
    } else {
        ActivityRingPalette.SurfaceWarmLight
    }

    val weekDayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(modifier = modifier.fillMaxWidth()) {
        // 月份切换控制栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onPrevMonth()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "上一月",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${displayedCal.get(Calendar.YEAR)}年 ${displayedCal.get(Calendar.MONTH) + 1}月",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onNextMonth()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "下一月",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 月历网格卡片
        Surface(
            color = surfaceBg,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // 表头
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDayLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                val firstDayOfWeek = displayedCal.get(Calendar.DAY_OF_WEEK)
                val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY
                val daysInMonth = displayedCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                var dayCounter = 1
                for (row in 0 until 6) {
                    if (dayCounter > daysInMonth && row > 0) break

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            if (cellIndex < offset || dayCounter > daysInMonth) {
                                Box(modifier = Modifier.weight(1f))
                            } else {
                                val currentDay = dayCounter
                                val cellCal = (displayedCal.clone() as Calendar).apply {
                                    set(Calendar.DAY_OF_MONTH, currentDay)
                                }
                                val cellDate = cellCal.time
                                val epochDay = cellCal.toEpochDay()

                                val isSelected = epochDay == selectedEpochDay
                                val isToday = epochDay == todayEpochDay

                                val rings = calculateRingsForDay(
                                    epochDay = epochDay,
                                    todayEpochDay = todayEpochDay,
                                    todayStats = todayStats,
                                    record = recordsMap[epochDay],
                                    forecast = forecastMap[epochDay]
                                )
                                val progresses = rings.map { it.progress }

                                val cellBg = when {
                                    isSelected -> if (isDark) MaterialTheme.colorScheme.surface else Color.White
                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    else -> Color.Transparent
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(1.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cellBg)
                                        .then(
                                            if (isSelected) {
                                                Modifier.softCardShadow(borderRadius = 10.dp, isDark = isDark)
                                            } else Modifier
                                        )
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            onSelectDate(cellDate)
                                        }
                                        .padding(vertical = 4.dp, horizontal = 1.dp)
                                ) {
                                    MiniActivityRings(
                                        progresses = progresses,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentDay.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else if (isToday) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }

                                dayCounter++
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 每一天微型环摘要
 */
data class DayRingSummary(
    val label: String,        // "一", "二", "三"...
    val dayNumber: String,    // "4", "5", "6"...
    val fullDate: Date,
    val epochDay: Long,
    val progresses: List<Float>
)

/**
 * #099 核心活动卡片 (一体化包含大三环、指标高亮联动、周/月模式切换、周期总结)
 */
@Composable
fun ActivityRingsCard(
    isMonthMode: Boolean,
    selectedDate: Date,
    todayEpochDay: Long,
    todayStats: LearningStats?,
    recordsMap: Map<Long, StudyRecord>,
    forecastMap: Map<Long, ReviewForecast>,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier,
    onMonthChanged: ((startEpoch: Long, endEpoch: Long) -> Unit)? = null
) {
    val view = LocalView.current
    val context = view.context

    val isReducedMotion = remember {
        try {
            val scale = android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            scale == 0f
        } catch (_: Exception) {
            false
        }
    }
    val animDuration = if (isReducedMotion) 1 else 1100
    val stepDelay = if (isReducedMotion) 0L else 110L

    val selectedEpochDay = remember(selectedDate) { selectedDate.toEpochDay() }

    // 计算选中当天的三环数据
    val currentRings = remember(selectedEpochDay, todayStats, recordsMap, forecastMap) {
        calculateRingsForDay(
            epochDay = selectedEpochDay,
            todayEpochDay = todayEpochDay,
            todayStats = todayStats,
            record = recordsMap[selectedEpochDay],
            forecast = forecastMap[selectedEpochDay]
        )
    }

    var highlightedIndex by remember { mutableStateOf<Int?>(null) }
    var flashIndex by remember { mutableStateOf<Int?>(null) }

    // 三环动画 (1100ms 立方缓动 + 阶梯延迟)
    val animR0 = remember { Animatable(0f) }
    val animR1 = remember { Animatable(0f) }
    val animR2 = remember { Animatable(0f) }

    val easeOutCubic = remember { CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f) }

    LaunchedEffect(selectedEpochDay, currentRings) {
        val target0 = currentRings[0].progress
        val target1 = currentRings[1].progress
        val target2 = currentRings[2].progress

        val triggeredFlash = if (!isReducedMotion) {
            when {
                animR0.value < 1f && target0 >= 1f -> 0
                animR1.value < 1f && target1 >= 1f -> 1
                animR2.value < 1f && target2 >= 1f -> 2
                else -> null
            }
        } else null

        coroutineScope {
            if (triggeredFlash != null) {
                launch {
                    flashIndex = triggeredFlash
                    delay(450)
                    flashIndex = null
                }
            } else {
                flashIndex = null
            }
            launch {
                animR0.animateTo(
                    targetValue = target0,
                    animationSpec = tween(durationMillis = animDuration, delayMillis = 0, easing = easeOutCubic)
                )
            }
            launch {
                if (stepDelay > 0) delay(stepDelay)
                animR1.animateTo(
                    targetValue = target1,
                    animationSpec = tween(durationMillis = animDuration, delayMillis = 0, easing = easeOutCubic)
                )
            }
            launch {
                if (stepDelay > 0) delay(stepDelay * 2)
                animR2.animateTo(
                    targetValue = target2,
                    animationSpec = tween(durationMillis = animDuration, delayMillis = 0, easing = easeOutCubic)
                )
            }
        }

        flashIndex = null
    }

    // 周数据生成 (本周一到周日 7 天)
    val weekDays = remember(todayEpochDay, recordsMap, forecastMap, todayStats) {
        val todayLocalDate = LocalDate.ofEpochDay(todayEpochDay)
        val cal = Calendar.getInstance().apply {
            set(todayLocalDate.year, todayLocalDate.monthValue - 1, todayLocalDate.dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diffToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
        cal.add(Calendar.DAY_OF_MONTH, diffToMonday)

        val labels = listOf("一", "二", "三", "四", "五", "六", "日")
        val list = mutableListOf<DayRingSummary>()

        for (i in 0 until 7) {
            val epoch = cal.toEpochDay()
            val rings = calculateRingsForDay(
                epochDay = epoch,
                todayEpochDay = todayEpochDay,
                todayStats = todayStats,
                record = recordsMap[epoch],
                forecast = forecastMap[epoch]
            )
            list.add(
                DayRingSummary(
                    label = labels[i],
                    dayNumber = cal.get(Calendar.DAY_OF_MONTH).toString(),
                    fullDate = cal.time,
                    epochDay = epoch,
                    progresses = rings.map { it.progress }
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    // 月模式浏览月份状态
    var displayedCal by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                time = selectedDate
                set(Calendar.DAY_OF_MONTH, 1)
            }
        )
    }

    LaunchedEffect(displayedCal) {
        val startCal = (displayedCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val endCal = (displayedCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        onMonthChanged?.invoke(startCal.toEpochDay(), endCal.toEpochDay())
    }

    // 计算周期内合环天数 (三环均 >= 1.0)
    val closedDaysCount = remember(isMonthMode, weekDays, recordsMap, displayedCal, todayEpochDay, todayStats, forecastMap) {
        if (!isMonthMode) {
            weekDays.count { it.progresses.size == 3 && it.progresses.all { p -> p >= 1.0f } }
        } else {
            val maxDay = displayedCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            var count = 0
            val testCal = (displayedCal.clone() as Calendar)
            for (d in 1..maxDay) {
                testCal.set(Calendar.DAY_OF_MONTH, d)
                val ep = testCal.toEpochDay()
                val r = calculateRingsForDay(
                    epochDay = ep,
                    todayEpochDay = todayEpochDay,
                    todayStats = todayStats,
                    record = recordsMap[ep],
                    forecast = forecastMap[ep]
                )
                if (r.all { it.progress >= 1.0f }) {
                    count++
                }
            }
            count
        }
    }

    val selectedDateText = remember(selectedDate, selectedEpochDay, todayEpochDay) {
        val cal = Calendar.getInstance().apply { time = selectedDate }
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val isToday = selectedEpochDay == todayEpochDay
        if (isToday) "今天 · ${m}月${d}日" else "${m}月${d}日"
    }

    PremiumCard(
        modifier = modifier
    ) {
        // 顶部标题与所选日期
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "活动进度",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.03).sp
            )
            Text(
                text = selectedDateText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 上半部分: 左侧大三环 + 右侧 3 项指标
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 大三环画布
            ActivityRingsCanvas(
                rings = currentRings,
                animProgress = listOf(animR0.value, animR1.value, animR2.value),
                highlightedIndex = highlightedIndex,
                flashIndex = flashIndex,
                modifier = Modifier
                    .size(172.dp)
                    .padding(4.dp)
            )

            // 右侧指标项列表
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                currentRings.forEachIndexed { idx, ringItem ->
                    val animatedActual = when (idx) {
                        0 -> (animR0.value * ringItem.goal).toInt()
                        1 -> (animR1.value * ringItem.goal).toInt()
                        else -> (animR2.value * ringItem.goal).toInt()
                    }

                    ActivityRingItemRow(
                        item = ringItem,
                        animatedActual = animatedActual,
                        isHighlighted = highlightedIndex == idx,
                        onClick = {
                            highlightedIndex = if (highlightedIndex == idx) null else idx
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 下半部分: 周视图 7 天胶囊 或 月视图微型三环网格 (带 AnimatedContent 丝滑平滑过渡)
        AnimatedContent(
            targetState = isMonthMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(200))
            },
            label = "calendarPickerTransition"
        ) { monthMode ->
            if (!monthMode) {
                val selectedWeekIndex = weekDays.indexOfFirst { it.epochDay == selectedEpochDay }.coerceAtLeast(0)
                ActivityWeekPicker(
                    daysData = weekDays,
                    selectedIndex = selectedWeekIndex,
                    onSelectDay = { idx ->
                        onDateSelected(weekDays[idx].fullDate)
                    }
                )
            } else {
                ActivityMonthGrid(
                    displayedCal = displayedCal,
                    selectedEpochDay = selectedEpochDay,
                    todayEpochDay = todayEpochDay,
                    recordsMap = recordsMap,
                    forecastMap = forecastMap,
                    todayStats = todayStats,
                    onPrevMonth = {
                        displayedCal = (displayedCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                    },
                    onNextMonth = {
                        displayedCal = (displayedCal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    },
                    onSelectDate = { date ->
                        onDateSelected(date)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 底部总结信息 (本周/本月合环天数)
        AnimatedContent(
            targetState = isMonthMode,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
            },
            label = "periodClosedDaysSummary"
        ) { monthMode ->
            val periodText = if (!monthMode) "本周" else "本月"
            Text(
                text = "${periodText}三环全合上天数：${closedDaysCount} 天",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
