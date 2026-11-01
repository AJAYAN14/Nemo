package com.jian.nemo.feature.statistics.presentation.curve

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.jian.nemo.core.designsystem.theme.StatisticsPalette
import com.jian.nemo.feature.statistics.R

/**
 * 记忆分层阶段
 */
enum class CurveStage(
    @StringRes val labelRes: Int,
    val themeColor: Color,
    @StringRes val descriptionRes: Int
) {
    COMPOSITE(
        R.string.stats_curve_stage_composite_label,
        StatisticsPalette.CompositeOrange,
        R.string.stats_curve_stage_composite_desc
    ),
    YOUNG_EARLY(
        R.string.stats_curve_stage_young_early_label,
        StatisticsPalette.EarlyRed,
        R.string.stats_curve_stage_young_early_desc
    ),
    YOUNG_DEVELOPING(
        R.string.stats_curve_stage_young_dev_label,
        StatisticsPalette.DevelopingBlue,
        R.string.stats_curve_stage_young_dev_desc
    ),
    MATURE(
        R.string.stats_curve_stage_mature_label,
        StatisticsPalette.MatureGreen,
        R.string.stats_curve_stage_mature_desc
    ),
    EXPERT(
        R.string.stats_curve_stage_expert_label,
        StatisticsPalette.ExpertPurple,
        R.string.stats_curve_stage_expert_desc
    );

    val label: String
        @Composable
        get() = stringResource(labelRes)

    val description: String
        @Composable
        get() = stringResource(descriptionRes)
}

/**
 * 阶段聚合统计信息
 */
data class StageStat(
    val stage: CurveStage,
    val count: Int,
    val medianStability: Float,
    val isAvailable: Boolean = count > 0
)

/**
 * 预警等级
 */
enum class AlertLevel {
    CRITICAL,
    WARNING,
    SAFE
}

/**
 * 记忆警戒推演洞察
 */
data class AlertInsight(
    val level: AlertLevel,
    val title: String,
    val message: String,
    val criticalDays: Int,
    val color: Color,
    val alertThreshold: Float = 0.90f,
    val actualRetention: Float? = null,
    val targetRetention: Float = 0.90f
)

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
 * @param standardCurve FSRS 标准遗忘曲线数据（对应图中的绿色线）
 * @param userCurve 当前选中分层的用户记忆曲线数据
 * @param activeStage 当前激活的分层阶段
 * @param stages 各阶段聚合统计信息
 * @param alertInsight 记忆警戒推演卡片内容
 * @param alertThreshold 当前动态警戒线阈值
 * @param actualRetention 当前实际回忆率（null为样本不足冷启动）
 * @param targetRetention 用户设定的目标保留率
 */
data class ForgettingCurveData(
    val standardCurve: List<CurvePoint>,
    val userCurve: List<CurvePoint>,
    val activeStage: CurveStage = CurveStage.COMPOSITE,
    val stages: List<StageStat> = emptyList(),
    val alertInsight: AlertInsight? = null,
    val alertThreshold: Float = 0.90f,
    val actualRetention: Float? = null,
    val targetRetention: Float = 0.90f
)
