package com.jian.nemo.feature.statistics.presentation.curve

/**
 * 遗忘曲线单个数据点
 *
 * @param dayIndex 天数索引 (0=今天, 1=明天, 2=后天, 3+=N天后)
 * @param retentionRate 记忆留存率 (0.0 ~ 1.0，其中 1.0 代表 100%)
 */
data class CurvePoint(
    val dayIndex: Int,
    val retentionRate: Float
)

/**
 * 遗忘曲线数据模型
 *
 * 允许外部传入两组数据源，实现标准曲线与用户曲线的对比展示。
 *
 * @param standardCurve FSRS 标准遗忘曲线数据（对应图中的绿色线）
 * @param userCurve 用户实际记忆曲线数据（对应图中的橙色线）
 */
data class ForgettingCurveData(
    val standardCurve: List<CurvePoint>,
    val userCurve: List<CurvePoint>
)
