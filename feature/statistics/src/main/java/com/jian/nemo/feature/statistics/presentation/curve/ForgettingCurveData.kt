package com.jian.nemo.feature.statistics.presentation.curve

import androidx.compose.ui.graphics.Color

/**
 * 记忆分层阶段
 */
enum class CurveStage(
    val label: String,
    val themeColor: Color,
    val description: String
) {
    COMPOSITE("全库综合", Color(0xFFFF9800), "全量已学项目的综合记忆抗遗忘水平"),
    YOUNG_EARLY("初识新词", Color(0xFFEF4444), "刚学习1~2次，海马体临时印记，极易遗忘"),
    YOUNG_DEVELOPING("发展巩固", Color(0xFF3B82F6), "建立初步记忆突触，处于快速生长期"),
    MATURE("深度稳固", Color(0xFF22C55E), "已进入长时记忆区，具备极高抗遗忘韧性"),
    EXPERT("永久长效", Color(0xFF8B5CF6), "深度固化为直觉记忆，接近母语级永久留存")
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
