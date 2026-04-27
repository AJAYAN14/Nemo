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

        // 1) 忘记率偏高：收紧间隔增长；忘记率偏低：适度放宽。
        val againDrift = againRate - 0.25f
        tuned[11] = tuned[11] * clamp(1f + againDrift * 0.50f, 0.92f, 1.08f) // failure base
        tuned[8] = tuned[8] * clamp(1f - againDrift * 0.35f, 0.92f, 1.08f)   // success growth exp
        tuned[16] = tuned[16] * clamp(1f - againDrift * 0.25f, 0.94f, 1.06f)  // easy bonus

        // 2) Hard 比例偏高：略微增加 hard penalty（更保守）；反之放宽。
        val hardDrift = hardRate - 0.20f
        tuned[15] = tuned[15] * clamp(1f - hardDrift * 0.40f, 0.90f, 1.10f)

        // 3) 稳定性保护，避免参数异常导致极端结果。
        tuned[11] = max(0.5f, tuned[11])
        tuned[16] = max(1.1f, tuned[16])

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
