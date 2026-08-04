package com.jian.nemo.core.designsystem.component


import android.annotation.SuppressLint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.graphics.BlurMaskFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * 高质感刻度尺滑动选择器 (NemoFractionalPicker)
 *
 * 参照 Portal Labs Fractional Picker 风格实现：
 * 1. 顶部倒三角形悬浮指示指针。
 * 2. 水平标尺拖拽与惯性滑动，停止时自动磁吸（Snap）对齐整秒刻度。
 * 3. 划过每个刻度时触发硬件 Tick 触觉震动。
 * 4. 居中选中数字放大高亮，两旁数字渐变缩小。
 * 5. 全面适配深浅模式。
 *
 * @param value 当前选择的整秒数值
 * @param onValueChange 数值改变时的回调
 * @param min 最小值，默认 1
 * @param max 最大值，默认 20
 * @param modifier 修饰符
 */
@Composable
fun NemoFractionalPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 20
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // 硬件 Vibrator 震动控制
    val vibrator = remember(context) {
        context.getSystemService(Vibrator::class.java)
    }

    @SuppressLint("MissingPermission")
    fun performHapticTick() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(8, 50))
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    val itemWidth = 64.dp
    val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
    val totalItems = max - min + 1

    val initialIndex = (value - min).coerceIn(0, totalItems - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 监听居中对齐的当前选中的 Index
    val currentCenteredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) initialIndex
            else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    abs(itemCenter - viewportCenter)
                }?.index ?: initialIndex
            }
        }
    }

    var lastReportedIndex by remember { mutableIntStateOf(initialIndex) }
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // 监听状态改变并回调
    LaunchedEffect(currentCenteredIndex) {
        if (currentCenteredIndex != lastReportedIndex) {
            lastReportedIndex = currentCenteredIndex
            performHapticTick()
            // 程序化滚动期间不回调中间值，避免干扰目标值
            if (!isProgrammaticScroll) {
                val selectedValue = min + currentCenteredIndex
                onValueChange(selectedValue)
            }
        }
    }

    // 当外部传入的 value 发生变动且不同于当前居中 index 时平滑滚动对齐（例如点击了快捷预设气泡）
    LaunchedEffect(value) {
        val targetIndex = (value - min).coerceIn(0, totalItems - 1)
        if (targetIndex != currentCenteredIndex && !listState.isScrollInProgress) {
            try {
                isProgrammaticScroll = true
                // 以当前 snap 对齐位置为基准，按整数倍 item 宽度滚动，保证目标项精确居中
                val scrollDelta = (targetIndex - currentCenteredIndex) * itemWidthPx
                listState.animateScrollBy(scrollDelta)
                // 滚动完成后报告最终值
                lastReportedIndex = targetIndex
                onValueChange(min + targetIndex)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    val indicatorColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    // 内阴影颜色：深色模式用黑色，浅色模式用深灰
    val innerShadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.12f)
    // 边缘渐变遮罩：从弹窗表面色渐变到透明
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            // 内阴影绘制实现凹陷深度感
            .drawWithContent {
                drawContent()
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        isAntiAlias = true
                        color = innerShadowColor.toArgb()
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                        maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    }
                    val androidCanvas = canvas.nativeCanvas
                    // 顶部内阴影
                    androidCanvas.drawRect(
                        0f, -12.dp.toPx(), size.width, 6.dp.toPx(), paint
                    )
                    // 底部内阴影
                    androidCanvas.drawRect(
                        0f, size.height - 6.dp.toPx(), size.width, size.height + 12.dp.toPx(), paint
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 1. 顶部倒三角形指针与圆形珠
            Box(
                modifier = Modifier
                    .size(width = 24.dp, height = 18.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height * 0.75f)
                        close()
                    }
                    drawPath(path = path, color = indicatorColor)
                    drawCircle(
                        color = indicatorColor,
                        radius = 2.dp.toPx(),
                        center = Offset(size.width / 2f, size.height - 2.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. 核心标尺水平 LazyRow
            @Suppress("UnusedBoxWithConstraintsScope")
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // 动态计算居中内边距，确保任何屏幕宽度下项目都精确居中
                val horizontalPadding = (maxWidth - itemWidth) / 2
                LazyRow(
                    state = listState,
                    flingBehavior = snapFlingBehavior,
                    contentPadding = PaddingValues(horizontal = horizontalPadding),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(totalItems) { index ->
                        val itemValue = min + index
                        val isSelected = index == currentCenteredIndex

                        Column(
                            modifier = Modifier
                                .width(itemWidth)
                                .height(80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // 数字显示
                            Text(
                                text = "$itemValue",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = if (isSelected) 26.sp else 18.sp
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                },
                                textAlign = TextAlign.Center,
                                modifier = Modifier.height(36.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // 刻度线 Canvas 绘制
                            val tickColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                            }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                            ) {
                                val centerX = size.width / 2f
                                // 主刻度线
                                drawLine(
                                    color = tickColor,
                                    start = Offset(centerX, 0f),
                                    end = Offset(centerX, if (isSelected) 28.dp.toPx() else 18.dp.toPx()),
                                    strokeWidth = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx()
                                )

                                // 子刻度虚线/细线 (在刻度之间绘制微小的阻尼刻度点)
                                if (index < totalItems - 1) {
                                    val subTickColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                    val subX = centerX + size.width / 2f
                                    drawLine(
                                        color = subTickColor,
                                        start = Offset(subX, 4.dp.toPx()),
                                        end = Offset(subX, 14.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }
                            }
                        }
                    }
                }

                // 左右边缘渐变遮罩，增强凹槽深度视觉
                Box(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    // 左侧渐变
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(48.dp)
                            .height(80.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        surfaceColor.copy(alpha = 0.8f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    // 右侧渐变
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(48.dp)
                            .height(80.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        surfaceColor.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}
