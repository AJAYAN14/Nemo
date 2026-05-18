package com.jian.nemo.feature.statistics.presentation.curve

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
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
    val backgroundColor = MaterialTheme.colorScheme.background

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
            text = "艾宾浩斯遗忘曲线 The Ebbinghaus Forgetting Curve"
        )
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    text: String
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
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(2.dp)
                .background(color, RoundedCornerShape(1.dp))
        )
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
    val isDark = isSystemInDarkTheme()
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
 * 时间范围选择器
 *
 * 使用 FilterChip 行排列，对应 HTML 中的下拉选择器功能
 */
@Composable
private fun TimeRangeSelector(
    selectedRange: CurveTimeRange,
    onRangeSelected: (CurveTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) NemoNeutrals.Gray800 else NemoNeutrals.Gray100
    val activeBgColor = if (isDark) NemoNeutrals.Gray700 else Color.White
    val activeTextColor = ChartColors.FreshGreen
    val inactiveTextColor = if (isDark) NemoNeutrals.Gray400 else NemoNeutrals.Gray500

    // 外层凹槽
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(color = bgColor, shape = RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurveTimeRange.entries.forEach { range ->
                val isSelected = range == selectedRange
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) activeBgColor else Color.Transparent)
                        .clickable { onRangeSelected(range) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${range.days}天",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) activeTextColor else inactiveTextColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
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
