package com.jian.nemo.core.domain.algorithm

import com.jian.nemo.core.domain.model.ReviewLog
import kotlin.math.max

/**
 * 轻量个性化参数微调器。
 *
 * 说明：
 * - 目标是上线兼容优先，不做激进重拟合。
 * - 日志不足时直接返回 null（保持默认参数）。
 * - 仅小幅调整少量参数，且有硬性边界。
 */
object FsrsParameterOptimizer {

    private const val MIN_LOGS_FOR_TUNING = 400

    data class OptimizationResult(
        val parameters: FloatArray,
        val sampleSize: Int,
        val againRate: Float,
        val hardRate: Float
    )

    fun optimize(
        logs: List<ReviewLog>,
        base: FloatArray = FsrsAlgorithm.DEFAULT_PARAMETERS
    ): OptimizationResult? {
        if (logs.size < MIN_LOGS_FOR_TUNING) return null

        val tuned = base.clone()
        val total = logs.size.toFloat()
        val againRate = logs.count { it.rating <= 2 }.toFloat() / total
        val hardRate = logs.count { it.rating == 3 }.toFloat() / total

        // ---- 基于遗忘率(Again)的调整 ----
        val againDrift = againRate - 0.25f

        // 1) 失败后稳定性基值: 忘记率高 → 略微增大（给遗忘后更宽的恢复区间）
        tuned[11] = tuned[11] * clamp(1f + againDrift * 0.50f, 0.92f, 1.08f)
        // 2) 成功后稳定性增长指数: 忘记率高 → 收紧增长
        tuned[8] = tuned[8] * clamp(1f - againDrift * 0.35f, 0.92f, 1.08f)
        // 3) Easy 奖励系数: 忘记率高 → 减小
        tuned[16] = tuned[16] * clamp(1f - againDrift * 0.25f, 0.94f, 1.06f)
        // 4) Again 初始稳定性: 忘记率高 → 略微增大（初学阶段给更宽缓冲）
        tuned[0] = tuned[0] * clamp(1f + againDrift * 0.30f, 0.90f, 1.10f)
        // 5) Good 初始稳定性: 忘记率低 → 适当增大（信任用户学习能力）
        tuned[2] = tuned[2] * clamp(1f - againDrift * 0.25f, 0.92f, 1.08f)
        // 6) 成功后 stability power: 忘记率高 → 略微增大（高稳定性卡片增长更慢）
        tuned[9] = tuned[9] * clamp(1f + againDrift * 0.20f, 0.94f, 1.06f)

        // ---- 基于 Hard 率的调整 ----
        val hardDrift = hardRate - 0.20f

        // 7) Hard 惩罚系数: Hard 率偏高 → 更保守
        tuned[15] = tuned[15] * clamp(1f - hardDrift * 0.40f, 0.90f, 1.10f)
        // 8) 难度变化因子: Hard 率偏高 → 难度爬升更快
        tuned[6] = tuned[6] * clamp(1f + hardDrift * 0.25f, 0.94f, 1.06f)

        // ---- 硬性边界保护 ----
        tuned[0] = max(0.05f, tuned[0])    // Again 初始稳定性不低于 0.05 天
        tuned[2] = max(0.5f, tuned[2])     // Good 初始稳定性不低于 0.5 天
        tuned[6] = max(1.0f, tuned[6])     // 难度变化因子不低于 1.0
        tuned[9] = max(0.05f, tuned[9])    // stability power 不低于 0.05
        tuned[11] = max(0.5f, tuned[11])   // 失败基值不低于 0.5
        tuned[16] = max(1.1f, tuned[16])   // Easy 奖励不低于 1.1

        return OptimizationResult(
            parameters = tuned,
            sampleSize = logs.size,
            againRate = againRate,
            hardRate = hardRate
        )
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }
}
