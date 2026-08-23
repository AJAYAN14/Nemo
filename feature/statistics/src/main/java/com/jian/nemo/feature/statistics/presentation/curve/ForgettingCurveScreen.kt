package com.jian.nemo.feature.statistics.presentation.curve

import com.jian.nemo.core.designsystem.theme.screenBackground

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jian.nemo.core.designsystem.theme.ChartColors
import com.jian.nemo.core.designsystem.theme.NemoNeutrals
import com.jian.nemo.core.designsystem.theme.NemoTheme
import com.jian.nemo.core.ui.component.animation.NemoChasingDotsLoader
import com.jian.nemo.core.ui.component.common.CommonHeader
import com.jian.nemo.core.ui.component.discoverybar.TapPulseWrapper
import com.jian.nemo.core.ui.modifier.softCardShadow
import com.jian.nemo.feature.statistics.presentation.curve.components.ForgettingCurveChart

/**
 * 遗忘曲线独立子界面
 *
 * 数据来源：
 * - 标准曲线：FsrsAlgorithm 默认 stability (Good 评分) 生成的基准遗忘曲线
 * - 用户曲线：从所有已学习单词/语法的平均 stability 计算得出
 *
 * 页面结构（自上而下）：
 * 1. 顶部导航栏（CommonHeader）
 * 2. Tab 栏（遗忘曲线 / 学习情况 / 记忆持久度）
 * 3. Canvas 折线图区域
 * 4. 图例区域（颜色标识 + 文本）
 * 5. 底部提示框
 */
@Composable
fun ForgettingCurveScreen(
    onBack: () -> Unit,
    viewModel: ForgettingCurveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundColor = MaterialTheme.colorScheme.screenBackground

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(backgroundColor)) {
                CommonHeader(
                    title = "遗忘曲线",
                    onBack = onBack
                )
            }
        },
        containerColor = backgroundColor
    ) { innerPadding ->
        if (uiState.isLoading || uiState.curveData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                NemoChasingDotsLoader()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {

                // ========== 时间范围选择器 ==========
                TimeRangeSelector(
                    selectedRange = uiState.selectedRange,
                    onRangeSelected = { viewModel.setTimeRange(it) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ========== 折线图区域 ==========
                ForgettingCurveChart(
                    data = uiState.curveData!!,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ========== 图例区域 ==========
                CurveLegend(
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ========== 底部提示框 ==========
                CurveTipCard(
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}


/**
 * 图例组件
 *
 * 以彩色圆点 + 文字说明展示两条曲线的含义
 */
@Composable
private fun CurveLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LegendItem(
            color = ChartColors.FreshOrange,
            text = "你的学习遗忘曲线"
        )
        LegendItem(
            color = ChartColors.FreshGreen,
            text = "艾宾浩斯遗忘曲线",
            isDashed = true
        )
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    text: String,
    isDashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 彩色圆点标识
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        // 横线标识
        if (isDashed) {
            Canvas(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
            ) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                    cap = StrokeCap.Round
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 底部提示框
 *
 * 浅灰色圆角背景 + 提示文本
 */
@Composable
private fun CurveTipCard(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val tipBgColor = if (isDark) {
        NemoNeutrals.Gray800
    } else {
        NemoNeutrals.Gray100
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = tipBgColor
    ) {
        Text(
            text = "学习的时间越久，你的遗忘曲线统计将越精准。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
            lineHeight = 18.sp
        )
    }
}

/**
 * 提供用于 Preview 和初始显示的 Mock 遗忘曲线数据
 *
 * 模拟截图中的下降趋势：
 * - 标准曲线（绿线）从 100% 快速降至约 25% 后趋于平稳
 * - 用户曲线（橙线）从 100% 更快降至接近 0%
 */
@Composable
private fun rememberMockForgettingCurveData(): ForgettingCurveData {
    return remember {
        ForgettingCurveData(
            standardCurve = listOf(
                CurvePoint(0, 1.00f),
                CurvePoint(1, 0.34f),
                CurvePoint(2, 0.29f),
                CurvePoint(3, 0.28f),
                CurvePoint(4, 0.27f),
                CurvePoint(5, 0.27f),
                CurvePoint(6, 0.26f),
                CurvePoint(7, 0.25f),
                CurvePoint(8, 0.25f),
                CurvePoint(9, 0.25f),
                CurvePoint(10, 0.25f),
                CurvePoint(11, 0.24f)
            ),
            userCurve = listOf(
                CurvePoint(0, 1.00f),
                CurvePoint(1, 0.50f),
                CurvePoint(2, 0.30f),
                CurvePoint(3, 0.25f),
                CurvePoint(4, 0.15f),
                CurvePoint(5, 0.08f),
                CurvePoint(6, 0.03f),
                CurvePoint(7, 0.01f),
                CurvePoint(8, 0.01f),
                CurvePoint(9, 0.005f),
                CurvePoint(10, 0.003f),
                CurvePoint(11, 0.002f)
            )
        )
    }
}

/**
 * 时间范围选择器 (DiscoveryBar 同款高质感弹簧滑动分段器)
 *
 * - 56dp 高度胶囊容器 + 柔和卡片阴影
 * - 弹簧物理滑块滑动指示器动画
 * - 7天/30天/90天/365天 4 档差异化色彩过渡 (蓝/绿/橙/紫)
 * - 点击微动效缩放与触感反馈
 */
@Composable
private fun TimeRangeSelector(
    selectedRange: CurveTimeRange,
    onRangeSelected: (CurveTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerBgColor = if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color.White
    val view = LocalView.current
    val ranges = remember { CurveTimeRange.entries }
    val selectedIndex = ranges.indexOf(selectedRange).coerceAtLeast(0)

    // 4 档时间跨度色彩映射
    val rangeColors = remember {
        mapOf(
            CurveTimeRange.SHORT to Color(0xFF007AFF),   // 7天: 经典蓝
            CurveTimeRange.MEDIUM to Color(0xFF34C759),  // 30天: 鲜活绿
            CurveTimeRange.LONG to Color(0xFFFF9500),    // 90天: 活力橙
            CurveTimeRange.EXTENDED to Color(0xFFAF52DE) // 365天: 高级紫
        )
    }

    val currentActiveColor = rangeColors[selectedRange] ?: Color(0xFF34C759)
    val inactiveTextColor = if (isDark) NemoNeutrals.Gray400 else NemoNeutrals.Gray500

    val barHeight = 56.dp
    val cornerRadius = 28.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .softCardShadow(borderRadius = cornerRadius, isDark = isDark)
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerBgColor)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val containerWidth = maxWidth
            val optionWidth = containerWidth / ranges.size
            val indicatorPadding = 4.dp

            // 滑动指示器偏移量 (弹簧动效)
            val indicatorOffset by animateDpAsState(
                targetValue = optionWidth * selectedIndex + indicatorPadding,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 350f,
                ),
                label = "timeRangeIndicatorOffset",
            )

            // 指示器背景色动画
            val indicatorBgColor by animateColorAsState(
                targetValue = currentActiveColor.copy(alpha = 0.12f),
                animationSpec = tween(durationMillis = 300),
                label = "timeRangeIndicatorBgColor",
            )

            // 滑块背景
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(optionWidth - indicatorPadding * 2)
                    .fillMaxHeight()
                    .padding(vertical = indicatorPadding)
                    .clip(RoundedCornerShape(cornerRadius - indicatorPadding))
                    .background(indicatorBgColor)
            )

            // 选项行
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ranges.forEachIndexed { index, range ->
                    val isSelected = range == selectedRange
                    val itemColor = rangeColors[range] ?: Color(0xFF34C759)

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) itemColor else inactiveTextColor,
                        animationSpec = tween(durationMillis = 300),
                        label = "textColor_${range.name}",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        TapPulseWrapper(
                            onTap = {
                                if (range != selectedRange) {
                                    onRangeSelected(range)
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            },
                            fullArea = true
                        ) {
                            Text(
                                text = "${range.days}天",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========== Preview ==========

@Preview(showBackground = true)
@Composable
private fun ForgettingCurveScreenPreview() {
    NemoTheme {
        ForgettingCurveScreen(
            onBack = {}
        )
    }
}
