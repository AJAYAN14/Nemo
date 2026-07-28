package com.jian.nemo.core.designsystem.component

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

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

    // 监听状态改变并回调
    LaunchedEffect(currentCenteredIndex) {
        if (currentCenteredIndex != lastReportedIndex) {
            lastReportedIndex = currentCenteredIndex
            val selectedValue = min + currentCenteredIndex
            performHapticTick()
            onValueChange(selectedValue)
        }
    }

    // 当外部传入的 value 发生变动且不同于当前居中 index 时平滑滚动对齐（例如点击了快捷预设气泡）
    LaunchedEffect(value) {
        val targetIndex = (value - min).coerceIn(0, totalItems - 1)
        if (targetIndex != currentCenteredIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    val containerBg = if (isDark) Color(0xFF1E1F22) else Color(0xFFF8FAFC)
    val containerBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.06f)
    val indicatorColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            )
            .clip(RoundedCornerShape(24.dp))
            .background(containerBg)
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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LazyRow(
                    state = listState,
                    flingBehavior = snapFlingBehavior,
                    contentPadding = PaddingValues(horizontal = 140.dp), // Padding 让两端项目能滑动到绝对居中
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
            }
        }
    }
}
