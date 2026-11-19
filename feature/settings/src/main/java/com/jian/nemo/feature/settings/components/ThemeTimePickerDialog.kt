package com.jian.nemo.feature.settings.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jian.nemo.core.designsystem.shape.RoundedRectangle
import com.jian.nemo.core.designsystem.theme.NemoSurfaceBackground
import com.jian.nemo.core.designsystem.theme.NemoSurfaceCardDark
import com.jian.nemo.core.designsystem.theme.NemoSurfaceSubcardDark
import com.jian.nemo.core.designsystem.theme.NemoTextLight
import com.jian.nemo.core.designsystem.theme.PlusJakartaSans
import com.jian.nemo.core.designsystem.theme.Rubik
import com.jian.nemo.core.ui.component.NemoDialog
import com.jian.nemo.feature.settings.R
import kotlinx.coroutines.launch

private data class TimePreset(
    val hour: Int,
    val minute: Int
)

private val StartTimePresets = listOf(
    TimePreset(18, 0),
    TimePreset(20, 0),
    TimePreset(22, 0)
)

private val EndTimePresets = listOf(
    TimePreset(6, 0),
    TimePreset(7, 0),
    TimePreset(8, 0)
)

private const val WHEEL_VIRTUAL_LOOPS = 200
private val WHEEL_ITEM_HEIGHT = 76.dp
private val WHEEL_VIEWPORT_HEIGHT = WHEEL_ITEM_HEIGHT * 3

@Composable
fun ThemeTimePickerDialog(
    isStartTime: Boolean,
    initialTime: String,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val parts = remember(initialTime) { initialTime.split(":") }
    val initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: if (isStartTime) 20 else 7
    val initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0

    var selectedHour by remember { mutableIntStateOf(initialHour) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }

    val presets = if (isStartTime) StartTimePresets else EndTimePresets
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(Vibrator::class.java)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val performHapticTick: () -> Unit = remember(vibrator, haptic) {
        {
            try {
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(10, 60))
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            } catch (_: Exception) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // 浅灰色背景底槽：对齐 docs/design/color-palette/COLORS.md
    val troughBgColor = if (isDark) NemoSurfaceSubcardDark else NemoSurfaceBackground
    val digitTensColor = if (isDark) Color.White.copy(alpha = 0.55f) else NemoTextLight
    val digitOnesColor = if (isDark) Color.White else NemoSurfaceCardDark
    val colonColor = digitOnesColor

    // 小时与分钟滚轮状态：初始直接定位居中，结合上下对称 76dp contentPadding 确保当前数字绝对垂直居中
    val hourStartVirtualIndex = remember { (WHEEL_VIRTUAL_LOOPS / 2) * 24 + initialHour }
    val minuteStartVirtualIndex = remember { (WHEEL_VIRTUAL_LOOPS / 2) * 60 + initialMinute }

    val hourListState = rememberLazyListState(initialFirstVisibleItemIndex = hourStartVirtualIndex)
    val minuteListState = rememberLazyListState(initialFirstVisibleItemIndex = minuteStartVirtualIndex)

    val hourFlingBehavior = rememberSnapFlingBehavior(lazyListState = hourListState)
    val minuteFlingBehavior = rememberSnapFlingBehavior(lazyListState = minuteListState)

    // 计算当前居中高亮的小时索引
    val centerHourVirtualIndex by remember {
        derivedStateOf {
            val offset = hourListState.firstVisibleItemScrollOffset
            val index = hourListState.firstVisibleItemIndex
            val itemHeightPx = hourListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
            if (offset > itemHeightPx / 2) index + 1 else index
        }
    }

    // 计算当前居中高亮的分钟索引
    val centerMinuteVirtualIndex by remember {
        derivedStateOf {
            val offset = minuteListState.firstVisibleItemScrollOffset
            val index = minuteListState.firstVisibleItemIndex
            val itemHeightPx = minuteListState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
            if (offset > itemHeightPx / 2) index + 1 else index
        }
    }

    val currentRealHour by remember { derivedStateOf { centerHourVirtualIndex % 24 } }
    val currentRealMinute by remember { derivedStateOf { centerMinuteVirtualIndex % 60 } }

    LaunchedEffect(currentRealHour) {
        if (selectedHour != currentRealHour) {
            selectedHour = currentRealHour
            performHapticTick()
        }
    }

    LaunchedEffect(currentRealMinute) {
        if (selectedMinute != currentRealMinute) {
            selectedMinute = currentRealMinute
            performHapticTick()
        }
    }

    NemoDialog(
        onDismissRequest = onDismiss,
        title = if (isStartTime) {
            stringResource(R.string.settings_theme_title_dark_mode_start)
        } else {
            stringResource(R.string.settings_theme_title_dark_mode_end)
        },
        confirmText = stringResource(com.jian.nemo.core.ui.R.string.core_common_btn_confirm),
        dismissText = stringResource(com.jian.nemo.core.ui.R.string.core_common_btn_cancel),
        onConfirm = {
            val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(formattedTime)
        },
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. 双滚轮卡片行：左右两边各独立一个浅灰色背景槽，中间为独立冒号，彻底无大包裹底板
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧小时独立浅灰背景槽卡片（00 ~ 23，无边框，NemoSurfaceBackground）
                    TactileDrumCard(
                        modifier = Modifier.weight(1f),
                        listState = hourListState,
                        flingBehavior = hourFlingBehavior,
                        itemCount = 24,
                        centerVirtualIndex = centerHourVirtualIndex,
                        troughBgColor = troughBgColor,
                        digitTensColor = digitTensColor,
                        digitOnesColor = digitOnesColor,
                        accessibilityFormat = stringResource(R.string.settings_theme_time_picker_hours_accessibility, selectedHour),
                        onItemClick = { clickedVirtualIndex ->
                            performHapticTick()
                            coroutineScope.launch {
                                hourListState.animateScrollToItem(clickedVirtualIndex)
                            }
                        }
                    )

                    // 垂直双圆点冒号（独立位于两槽之间，正中居中，12dp 圆形，14dp 间距）
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .align(Alignment.CenterVertically),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(colonColor)
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(colonColor)
                        )
                    }

                    // 右侧分钟独立浅灰背景槽卡片 (00 ~ 59，无边框，NemoSurfaceBackground)
                    TactileDrumCard(
                        modifier = Modifier.weight(1f),
                        listState = minuteListState,
                        flingBehavior = minuteFlingBehavior,
                        itemCount = 60,
                        centerVirtualIndex = centerMinuteVirtualIndex,
                        troughBgColor = troughBgColor,
                        digitTensColor = digitTensColor,
                        digitOnesColor = digitOnesColor,
                        accessibilityFormat = stringResource(R.string.settings_theme_time_picker_minutes_accessibility, selectedMinute),
                        onItemClick = { clickedVirtualIndex ->
                            performHapticTick()
                            coroutineScope.launch {
                                minuteListState.animateScrollToItem(clickedVirtualIndex)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. 预设选项行：置于滚轮正下方，无边框，底槽颜色与上方滚轮完全一致，纯时间数字，零中文，横向对称居中
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presets.forEach { preset ->
                        val isPresetActive = selectedHour == preset.hour && selectedMinute == preset.minute
                        val presetBg = if (isPresetActive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            troughBgColor
                        }
                        val presetTextColor = if (isPresetActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedRectangle(14.dp))
                                .background(presetBg)
                                .clickable {
                                    performHapticTick()
                                    coroutineScope.launch {
                                        val currentHourCycle = centerHourVirtualIndex / 24
                                        val targetHourIndex = currentHourCycle * 24 + preset.hour
                                        val currentMinuteCycle = centerMinuteVirtualIndex / 60
                                        val targetMinuteIndex = currentMinuteCycle * 60 + preset.minute

                                        hourListState.animateScrollToItem(targetHourIndex)
                                        minuteListState.animateScrollToItem(targetMinuteIndex)
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format("%02d:%02d", preset.hour, preset.minute),
                                style = TextStyle(
                                    fontFamily = PlusJakartaSans,
                                    fontSize = 15.sp,
                                    fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = presetTextColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun TactileDrumCard(
    modifier: Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState,
    flingBehavior: androidx.compose.foundation.gestures.FlingBehavior,
    itemCount: Int,
    centerVirtualIndex: Int,
    troughBgColor: Color,
    digitTensColor: Color,
    digitOnesColor: Color,
    accessibilityFormat: String,
    onItemClick: (Int) -> Unit
) {
    val cardShape = RoundedRectangle(26.dp)

    Box(
        modifier = modifier
            .height(WHEEL_VIEWPORT_HEIGHT)
            .clip(cardShape)
            .background(troughBgColor, cardShape)
            .semantics { contentDescription = accessibilityFormat }
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = WHEEL_ITEM_HEIGHT),
            modifier = Modifier.matchParentSize()
        ) {
            items(
                count = itemCount * WHEEL_VIRTUAL_LOOPS,
                key = { it }
            ) { virtualIndex ->
                val realValue = virtualIndex % itemCount
                val offsetFromCenter = virtualIndex - centerVirtualIndex
                val isCenter = offsetFromCenter == 0

                // 距离中心点插值：中心为 1.0f，相邻项缩放至 0.66f，透明度衰减至 0.44f
                val (scale, alpha) = when (kotlin.math.abs(offsetFromCenter)) {
                    0 -> 1.0f to 1.0f
                    1 -> 0.66f to 0.44f
                    else -> 0.45f to 0.20f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WHEEL_ITEM_HEIGHT)
                        .clickable(enabled = !isCenter) {
                            onItemClick(virtualIndex)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CutoutDigitPair(
                        value = realValue,
                        scale = scale,
                        alpha = alpha,
                        isCenter = isCenter,
                        troughBgColor = troughBgColor,
                        digitTensColor = digitTensColor,
                        digitOnesColor = digitOnesColor
                    )
                }
            }
        }
    }
}

/**
 * 粗几何剪纸错层重叠数字组件（Chunky Retro Overlapping Cutout Numeral）
 * 1. 十位数在底层，采用柔和对比度色彩（digitTensColor）；
 * 2. 个位数向左负偏移重叠在十位数右缘；
 * 3. 个位数底层采用槽底色（troughBgColor）绘制 14f 宽度的外描边（Cutout Stroke），顶层填充实心主色（digitOnesColor）；
 * 形成自然的贴纸层叠与剪纸遮挡空隙效果。
 */
@Composable
private fun CutoutDigitPair(
    value: Int,
    scale: Float,
    alpha: Float,
    isCenter: Boolean,
    troughBgColor: Color,
    digitTensColor: Color,
    digitOnesColor: Color,
    modifier: Modifier = Modifier
) {
    val tensChar = (value / 10).toString()
    val onesChar = (value % 10).toString()

    val baseStyle = TextStyle(
        fontFamily = Rubik,
        fontSize = 58.sp,
        fontWeight = if (isCenter) FontWeight.Black else FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        fontFeatureSettings = "tnum"
    )

    // -0.14em 负间距，在 58sp 下约为 -8.5dp
    val overlapOffset = (-8).dp

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 1. 十位数（位于底层）
        Text(
            text = tensChar,
            style = baseStyle,
            color = digitTensColor
        )

        // 2. 个位数（位于顶层，向左水平重叠）
        Box(
            modifier = Modifier.offset(x = overlapOffset),
            contentAlignment = Alignment.Center
        ) {
            // (1) 剪纸镂空外边框层（Cutout Outline）：以槽底色粗描边遮挡十位数边缘
            Text(
                text = onesChar,
                style = baseStyle.copy(
                    color = troughBgColor,
                    drawStyle = Stroke(
                        width = 14f,
                        join = StrokeJoin.Round,
                        cap = StrokeCap.Round
                    )
                )
            )

            // (2) 实心主色文字层（Fill Content）
            Text(
                text = onesChar,
                style = baseStyle.copy(
                    color = digitOnesColor,
                    drawStyle = Fill
                )
            )
        }
    }
}
