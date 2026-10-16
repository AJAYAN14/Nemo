package com.jian.nemo.feature.settings.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.text.PlatformTextStyle
import com.jian.nemo.core.ui.component.Capsule
import com.jian.nemo.core.ui.component.liquid.ClearLiquidGlassButton
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.ui.component.liquid.ColoredLiquidGlassButton
import com.jian.nemo.core.ui.component.liquid.LiquidSlider
import com.jian.nemo.core.ui.modifier.softCardShadow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 模块 ①: 流体记忆视界 HUD 仿真卡片 (Liquid Memory Simulation HUD)
 *
 * 结合 react-bits 的 SloshGauge 流体负荷蓄水池概念与 FSRS 动态衰减曲面，
 * 实时推演卡片半衰期与学习复习负荷。
 */
@Composable
fun LiquidMemoryHud(
    targetRetention: Float,
    accentColor: Color = Color(0xFFAF52DE),
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val infiniteTransition = rememberInfiniteTransition(label = "slosh_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // 动态推算虚拟半衰期与负荷指数
    val estimatedHalfLifeDays = remember(targetRetention) {
        val r = targetRetention.coerceIn(0.70f, 0.99f)
        val stability = (10f * (-ln(0.9f) / -ln(r))).coerceIn(3f, 35f)
        String.format("%.1f", stability)
    }

    val burdenLevel = remember(targetRetention) {
        when {
            targetRetention < 0.82f -> "清爽微负荷" to Color(0xFF00C4B5)
            targetRetention <= 0.91f -> "黄金平衡态" to Color(0xFFAF52DE)
            else -> "极客高密度" to Color(0xFFFF9F0A)
        }
    }

    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 头部标题与芯片指标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Waves,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "记忆动力沙盘",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "FSRS 遗忘曲面与复习负荷实时映射",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = burdenLevel.second.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, burdenLevel.second.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = burdenLevel.first,
                        color = burdenLevel.second,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 核心曲面与 SloshGauge 蓄水池自绘容器
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val retentionRatio = ((targetRetention - 0.70f) / 0.29f).coerceIn(0f, 1f)

                    // 1. 绘制底部 SloshGauge 负荷蓄水池波浪 (水面高度随留存率上升)
                    val waterLevel = height * (0.85f - retentionRatio * 0.45f)
                    val wavePath = Path().apply {
                        moveTo(0f, height)
                        lineTo(0f, waterLevel)
                        val stepX = 10f
                        var x = 0f
                        while (x <= width) {
                            val waveY = waterLevel + sin((x / width * 3 * PI.toFloat()) + wavePhase) * 6f
                            lineTo(x, waveY)
                            x += stepX
                        }
                        lineTo(width, height)
                        close()
                    }

                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                burdenLevel.second.copy(alpha = 0.22f),
                                burdenLevel.second.copy(alpha = 0.04f)
                            )
                        )
                    )

                    // 2. 绘制 FSRS 遗忘衰减主曲线
                    val curvePath = Path().apply {
                        val decayFactor = (1.05f - targetRetention).coerceIn(0.06f, 0.40f)
                        moveTo(0f, height * 0.15f)
                        val pointsCount = 40
                        for (i in 1..pointsCount) {
                            val px = (i.toFloat() / pointsCount) * width
                            val t = (i.toFloat() / pointsCount) * 4f
                            val py = (height * 0.15f) + (height * 0.75f) * (1f - exp(-t * decayFactor))
                            lineTo(px, py)
                        }
                    }

                    // 曲线背景渐变发光
                    val glowPath = Path().apply {
                        addPath(curvePath)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = glowPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )

                    // 曲线外描边
                    drawPath(
                        path = curvePath,
                        color = accentColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 90% 期望保留率水平标线
                    val targetY = height * 0.45f
                    drawLine(
                        color = accentColor.copy(alpha = 0.35f),
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 覆盖在图表上的右下角浮动信息
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "参考记忆半衰期",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "≈ $estimatedHalfLifeDays 天",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * 模块 ②: 目标保留率磁吸精密标尺 (Modern Fractional Picker)
 *
 * 借鉴 portal_labs 的水平磁吸刻度微交互，支持 70% ~ 99% 范围，
 * 在 85%、90%、95% 黄金档提供磁吸和平滑阻尼。
 */
@Composable
fun RetentionPrecisionRuler(
    targetRetention: Float,
    onRetentionChange: (Float) -> Unit,
    accentColor: Color = Color(0xFFAF52DE),
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val density = LocalDensity.current

    // 内部平滑动画状态
    var currentRetention by remember(targetRetention) { mutableFloatStateOf(targetRetention) }

    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "目标保留率 (Desired Retention)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "滑动刻度微调 · 建议设定于 85% ~ 92%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 超大数字高光胶囊与双向微调按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            val next = (currentRetention - 0.01f).coerceIn(0.70f, 0.99f)
                            currentRetention = next
                            onRetentionChange(next)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "减少1%",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        AnimatedContent(
                            targetState = (currentRetention * 100).roundToInt(),
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> -height } + fadeOut())
                                } else {
                                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> height } + fadeOut())
                                }.using(SizeTransform(clip = false))
                            },
                            label = "retention_flip_animation",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) { percentage ->
                            Text(
                                text = "$percentage%",
                                color = accentColor,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.5).sp,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val next = (currentRetention + 0.01f).coerceIn(0.70f, 0.99f)
                            currentRetention = next
                            onRetentionChange(next)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "增加1%",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 水平滑动标尺容器 (使用 draggable 锁定水平手势，彻底避免与垂直滚动手势冲突)
            val stepPx = with(density) { 10.dp.toPx() } // 10dp 每 1%，手感更轻巧灵动
            val draggableState = rememberDraggableState { delta ->
                // 向左滑动 (delta < 0)，右侧刻度进入中心，保留率增加
                val deltaRetention = -(delta / (stepPx * 100f))
                currentRetention = (currentRetention + deltaRetention).coerceIn(0.70f, 0.99f)
                onRetentionChange(currentRetention)
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .draggable(
                        state = draggableState,
                        orientation = Orientation.Horizontal,
                        onDragStopped = {
                            // 仅在释放手指时平滑吸附到 85%、90%、95%，滑动过程中保持连续平滑绝不卡顿
                            val magnets = listOf(0.85f, 0.90f, 0.95f)
                            for (target in magnets) {
                                if (abs(currentRetention - target) < 0.008f) {
                                    currentRetention = target
                                    onRetentionChange(target)
                                    break
                                }
                            }
                        }
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2f
                            val deltaPx = offset.x - centerX
                            val stepCount = (deltaPx / stepPx).roundToInt()
                            if (stepCount != 0) {
                                val next = (currentRetention + stepCount * 0.01f).coerceIn(0.70f, 0.99f)
                                currentRetention = next
                                onRetentionChange(next)
                            }
                        }
                    }
            ) {
                val rulerWidth = constraints.maxWidth.toFloat()
                val centerX = rulerWidth / 2f

                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerY = size.height / 2f

                    // 绘制刻度线 (70% 到 99%)
                    for (i in 70..99) {
                        val valRetention = i / 100f
                        val offsetFromCurrent = (valRetention - currentRetention) * 100f
                        val tickX = centerX + offsetFromCurrent * stepPx

                        if (tickX in -20f..(rulerWidth + 20f)) {
                            val isMajor = (i % 5 == 0)
                            val isMagnet = (i == 85 || i == 90 || i == 95)
                            val tickHeight = if (isMajor) 26.dp.toPx() else 14.dp.toPx()
                            val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                            val tickColor = when {
                                isMagnet -> accentColor.copy(alpha = 0.85f)
                                isMajor -> onSurfaceColor.copy(alpha = 0.5f)
                                else -> onSurfaceVariantColor.copy(alpha = 0.25f)
                            }

                            drawLine(
                                color = tickColor,
                                start = Offset(tickX, centerY - tickHeight / 2f),
                                end = Offset(tickX, centerY + tickHeight / 2f),
                                strokeWidth = tickWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // 居中指示指针 (带有发光效果与金属卡尺感)
                    val indicatorHeight = 42.dp.toPx()
                    drawLine(
                        color = accentColor,
                        start = Offset(centerX, centerY - indicatorHeight / 2f),
                        end = Offset(centerX, centerY + indicatorHeight / 2f),
                        strokeWidth = 3.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = accentColor,
                        radius = 4.dp.toPx(),
                        center = Offset(centerX, centerY - indicatorHeight / 2f)
                    )
                    drawCircle(
                        color = accentColor,
                        radius = 4.dp.toPx(),
                        center = Offset(centerX, centerY + indicatorHeight / 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 预设快捷点，方便快速点击选择
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(0.80f to "80% 宽松", 0.85f to "85% 平衡", 0.90f to "90% 黄金", 0.95f to "95% 冲刺").forEach { (valRet, label) ->
                    val isSelected = (currentRetention * 100).roundToInt() == (valRet * 100).roundToInt()
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                currentRetention = valRet
                                onRetentionChange(valRet)
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * 模块 ③: 流体时空胶囊链 (Step Pipeline Capsules)
 *
 * 借鉴 portal_labs 的 Split to Edit 交互，彻底颠覆原本敲字符串的体验，
 * 将学习步长转化为直观的药丸时间节点链。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepPipelineEditor(
    title: String,
    subtitle: String,
    stepsString: String,
    onStepsChange: (String) -> Unit,
    accentColor: Color = Color(0xFFAF52DE),
    modifier: Modifier = Modifier
) {
    val stepsList = remember(stepsString) {
        stepsString.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    var selectedStepIndexForEdit by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 胶囊链流式排列
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stepsList.forEachIndexed { index, stepStr ->
                    StepCapsuleItem(
                        step = stepStr,
                        index = index,
                        accentColor = accentColor,
                        onClick = { selectedStepIndexForEdit = index },
                        onDelete = if (stepsList.size > 1) {
                            {
                                val mutable = stepsList.toMutableList()
                                mutable.removeAt(index)
                                onStepsChange(mutable.joinToString(" "))
                            }
                        } else null
                    )
                }

                // 添加新阶段胶囊 (流体主题色质感，彻底告别 MD3 默认灰)
                Surface(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = accentColor.copy(alpha = if (isDark) 0.12f else 0.06f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.35f else 0.22f)),
                    modifier = Modifier.height(40.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "添加阶段",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "增加阶梯",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = accentColor
                        )
                    }
                }
            }
        }
    }

    // 微调步进弹窗
    selectedStepIndexForEdit?.let { idx ->
        val currentVal = stepsList.getOrNull(idx) ?: "10"
        StepEditDialog(
            currentValue = currentVal,
            accentColor = accentColor,
            onDismiss = { selectedStepIndexForEdit = null },
            onConfirm = { newVal ->
                val mutable = stepsList.toMutableList()
                mutable[idx] = newVal
                onStepsChange(mutable.joinToString(" "))
                selectedStepIndexForEdit = null
            }
        )
    }

    // 添加步进弹窗
    if (showAddDialog) {
        val lastVal = stepsList.lastOrNull()?.toIntOrNull() ?: 10
        val suggestedVal = (lastVal * 2).toString()
        StepEditDialog(
            currentValue = suggestedVal,
            accentColor = accentColor,
            title = "新增复习阶段",
            onDismiss = { showAddDialog = false },
            onConfirm = { newVal ->
                val mutable = stepsList.toMutableList()
                mutable.add(newVal)
                onStepsChange(mutable.joinToString(" "))
                showAddDialog = false
            }
        )
    }
}

/**
 * 单个时间胶囊组件
 */
@Composable
private fun StepCapsuleItem(
    step: String,
    index: Int,
    accentColor: Color,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = accentColor.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = if (onDelete != null) 4.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${index + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeight = 11.sp
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatStepDuration(step),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "删除该阶段",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 步长微调对话框
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepEditDialog(
    currentValue: String,
    accentColor: Color,
    title: String = "调整复习间隔",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textInput by remember { mutableStateOf(currentValue) }
    val quickPresets = listOf("1", "5", "10", "15", "30", "60", "1440")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("输入复习间隔时长（单位：分钟）：", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) textInput = input
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 常用快捷预设
                Text("推荐预设：", style = MaterialTheme.typography.labelSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickPresets.forEach { preset ->
                        Surface(
                            onClick = { textInput = preset },
                            shape = RoundedCornerShape(10.dp),
                            color = if (textInput == preset) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, if (textInput == preset) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = formatStepDuration(preset),
                                fontSize = 12.sp,
                                fontWeight = if (textInput == preset) FontWeight.Bold else FontWeight.Normal,
                                color = if (textInput == preset) accentColor else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (textInput.isNotBlank() && textInput.toIntOrNull() != null) {
                        onConfirm(textInput)
                    }
                }
            ) {
                Text("确定", color = accentColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 格式化分钟为友好显示
 */
private fun formatStepDuration(step: String): String {
    val mins = step.toIntOrNull() ?: return "$step 分钟"
    return when {
        mins < 60 -> "$mins 分钟"
        mins < 1440 -> "${mins / 60} 小时"
        else -> "${mins / 1440} 天"
    }
}

/**
 * 模块 ④: 水蛭哨兵与果冻分段控制器 (Leech Sentinel & Rubber Segment)
 *
 * 融合 portal_labs 的机械翻牌步进器与 react-bits 的果冻分段器。
 */
@Composable
fun LeechSentinelCard(
    leechThreshold: Int,
    onThresholdChange: (Int) -> Unit,
    leechAction: String,
    onActionChange: (String) -> Unit,
    accentColor: Color = Color(0xFFAF52DE),
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 阈值部分 (机械翻牌步进)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "顽固卡片 (Leech) 判定阈值",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "某张卡片累计遗忘此次数后自动触发防爆防护",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 机械翻牌步进器
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { onThresholdChange((leechThreshold - 1).coerceAtLeast(1)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = "减少",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 机械翻牌数字框 (大号黑体 + 向上/向下翻滚动画)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isDark) Color(0xFF1E2028)
                                else Color.White
                            )
                            .border(
                                BorderStroke(
                                    0.5.dp,
                                    if (isDark) Color.White.copy(alpha = 0.08f)
                                    else Color.Black.copy(alpha = 0.05f)
                                ),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = leechThreshold,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> -height } + fadeOut())
                                } else {
                                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                    (slideOutVertically { height -> height } + fadeOut())
                                }.using(SizeTransform(clip = false))
                            },
                            label = "leech_flip_counter"
                        ) { count ->
                            Text(
                                text = "$count 次",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = accentColor,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = { onThresholdChange((leechThreshold + 1).coerceAtMost(12)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "增加",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 处置行为选择 (果冻分段滑槽)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "防爆触发后的处置策略",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 橡胶果冻分段器
                RubberSegmentToggle(
                    options = listOf(
                        "skip" to ("冻结卡片 (Skip)" to "彻底移出复习，沉淀至复学清单"),
                        "bury_today" to ("埋藏今日 (Bury)" to "今日不再提示，明日自动回归")
                    ),
                    selectedKey = leechAction,
                    onSelectedChange = onActionChange,
                    accentColor = accentColor
                )
            }
        }
    }
}

/**
 * 橡胶果冻分段选择器 (Rubber Segment)
 */
@Composable
private fun RubberSegmentToggle(
    options: List<Pair<String, Pair<String, String>>>,
    selectedKey: String,
    onSelectedChange: (String) -> Unit,
    accentColor: Color
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (key, info) ->
            val isSelected = selectedKey == key
            val animatedBorderAlpha by animateFloatAsState(
                targetValue = if (isSelected) 0.45f else 0.15f,
                animationSpec = spring(stiffness = 300f, dampingRatio = 0.75f),
                label = "border_alpha"
            )

            Surface(
                onClick = { onSelectedChange(key) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) accentColor.copy(alpha = 0.10f) else Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) accentColor.copy(alpha = animatedBorderAlpha)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = animatedBorderAlpha)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = info.first,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = info.second,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 模块 ⑤: 提前复习时间视界 (Temporal Horizon Slider)
 */
@Composable
fun TemporalHorizonSection(
    limitMinutes: Float,
    onLimitChange: (Float) -> Unit,
    accentColor: Color = Color(0xFFAF52DE),
    modifier: Modifier = Modifier
) {
    PremiumCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "提前复习容差窗口",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "卡片冷却剩余低于此时间时，允许提前温习",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "${limitMinutes.toInt()} 分钟",
                        color = accentColor,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LiquidSlider(
                value = { limitMinutes },
                onValueChange = onLimitChange,
                valueRange = 0f..60f,
                visibilityThreshold = 0.5f,
                accentColor = accentColor,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 模块 ⑥: 底部流体悬浮操作岛 (Floating Liquid Action Island)
 *
 * 当存在未保存的修改时以果冻弹簧滑出，提供直观的差异感知与优雅的一键应用。
 */
@Composable
fun FloatingLiquidIsland(
    visible: Boolean,
    onApply: () -> Unit,
    backdrop: Backdrop? = null,
    accentColor: Color = Color(0xFFAF52DE),
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(stiffness = 300f, dampingRatio = 0.75f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(stiffness = 300f, dampingRatio = 0.75f)
        ) + fadeOut(),
        modifier = modifier
    ) {
        val islandShape = RoundedCornerShape(32.dp)
        val effectiveBackdrop = backdrop ?: remember { emptyBackdrop() }

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = effectiveBackdrop,
                    shape = { islandShape },
                    effects = {
                        vibrancy()
                        blur(2f.dp.toPx())
                        lens(12f.dp.toPx(), 24f.dp.toPx())
                    }
                )
                .border(
                    BorderStroke(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f)
                    ),
                    islandShape
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "算法参数已更新",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "点击应用以同步生效至引擎",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 实验室同款: 彩色液态玻璃按钮 (ColoredLiquidGlassButton)
                ColoredLiquidGlassButton(
                    onClick = onApply,
                    backdrop = backdrop,
                    tint = accentColor,
                    shape = Capsule(),
                    isInteractive = true,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "立即应用",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
