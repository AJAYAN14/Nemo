package com.jian.nemo.core.domain.algorithm

import java.util.logging.Level
import java.util.logging.Logger
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.SrsItem
import com.jian.nemo.core.domain.model.SrsUpdateResult
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.ReviewLogRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 基于 FSRS 6 的 SRS 计算器实现
 *
 * 评分映射 (Nemo 0-5 → FSRS 1-4):
 * - quality 0-2 → Again (1)
 * - quality 3   → Hard (2)
 * - quality 4   → Good (3)
 * - quality 5   → Easy (4)
 */
@Singleton
class SrsCalculatorImpl @Inject constructor(
    private val reviewLogRepository: ReviewLogRepository,
    private val settingsRepository: SettingsRepository
) : SrsCalculator {

    companion object {
        private val logger = Logger.getLogger("FSRS")

        /** 触发重优化的复习次数阈值 */
        private const val RE_OPTIMIZE_THRESHOLD = 200
    }

    @Volatile
    private var currentParameters: FloatArray = FsrsAlgorithm.DEFAULT_PARAMETERS.clone()

    @Volatile
    private var currentRetention: Float = 0.9f

    @Volatile
    private var fsrs = FsrsAlgorithm(currentParameters, currentRetention)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 自上次优化以来的复习计数 */
    private val reviewsSinceLastOptimization = AtomicInteger(0)

    /** 是否正在执行优化（防止并发重复触发） */
    @Volatile
    private var optimizing = false

    init {
        // 监听目标保留率设置变化，实时同步更新 FSRS 实例
        scope.launch {
            try {
                settingsRepository.targetRetentionFlow.collect { retention ->
                    val clamped = retention.coerceIn(0.70f, 0.97f)
                    currentRetention = clamped
                    fsrs = FsrsAlgorithm(currentParameters, clamped)
                    logger.info("FSRS 目标保留率更新: $clamped")
                }
            } catch (e: Exception) {
                logger.log(Level.WARNING, "监听 targetRetention 异常", e)
            }
        }

        // 兼容优先：默认参数立即可用，个性化在后台异步加载。
        scope.launch { tryOptimize() }
    }

    /**
     * 尝试执行参数优化（init 和周期性触发共用）
     */
    private suspend fun tryOptimize() {
        if (optimizing) return
        optimizing = true
        try {
            val logs = reviewLogRepository.getRecentLogs(limit = 1500)
            val optimized = FsrsParameterOptimizer.optimize(logs)
            if (optimized != null) {
                currentParameters = optimized.parameters
                fsrs = FsrsAlgorithm(parameters = optimized.parameters, desiredRetention = currentRetention)
                logger.info(
                    "personalization enabled, samples=${optimized.sampleSize}, " +
                        "againRate=${"%.3f".format(optimized.againRate)}, " +
                        "hardRate=${"%.3f".format(optimized.hardRate)}"
                )
            } else {
                logger.info("personalization skipped, insufficient logs (samples=${logs.size})")
            }
        } catch (e: Exception) {
            // 保持默认参数，不影响线上主流程。
            logger.log(Level.WARNING, "personalization skipped due to error", e)
        } finally {
            optimizing = false
        }
    }

    override fun notifyReviewCompleted() {
        val count = reviewsSinceLastOptimization.incrementAndGet()
        if (count >= RE_OPTIMIZE_THRESHOLD && !optimizing) {
            reviewsSinceLastOptimization.set(0)
            scope.launch { tryOptimize() }
        }
    }

    override fun calculate(
        item: SrsItem,
        quality: Int,
        today: Long
    ): SrsUpdateResult {
        require(quality in 0..5) {
            "Quality must be between 0 and 5, got $quality"
        }

        // 1. 映射评分
        val rating = mapQualityToRating(quality)

        // 2. 构建当前记忆状态
        val currentState = if (item.stability > 0f && item.repetitionCount > 0) {
            MemoryState(stability = item.stability, difficulty = item.difficulty)
        } else {
            null // 新卡
        }

        // 3. 计算经过天数
        val elapsedDays = if (item.lastReviewedDate != null && item.lastReviewedDate!! > 0) {
            (today - item.lastReviewedDate!!).toFloat().coerceAtLeast(0f)
        } else {
            0f
        }

        // 4. 执行 FSRS step
        val newState = fsrs.step(currentState, rating, elapsedDays)

        // 5. 计算新间隔与单调性保证
        val newInterval: Int
        val newRepetitionCount: Int

        if (quality < 3) {
            // 失败: 不增加复习次数，间隔由 FSRS stability 决定
            newRepetitionCount = item.repetitionCount // 不扣减
            newInterval = fsrs.nextIntervalDays(newState.stability)
        } else {
            // 成功: 必须严格保证 Hard <= Good <= Easy 的单调性约束 (对齐 Anki 官方调度器)
            newRepetitionCount = item.repetitionCount + 1
            val seed = buildFuzzSeed(item, today, newRepetitionCount)

            // 联合推演成功各档状态
            val stateHard = if (rating == FsrsRating.Hard) newState else fsrs.step(currentState, FsrsRating.Hard, elapsedDays)
            val stateGood = if (rating == FsrsRating.Good) newState else fsrs.step(currentState, FsrsRating.Good, elapsedDays)
            val stateEasy = if (rating == FsrsRating.Easy) newState else fsrs.step(currentState, FsrsRating.Easy, elapsedDays)

            val rawHard = fsrs.nextIntervalDays(stateHard.stability)
            val rawGood = fsrs.nextIntervalDays(stateGood.stability)
            val rawEasy = fsrs.nextIntervalDays(stateEasy.stability)

            var fuzzedHard = fsrs.fuzzIntervalDays(rawHard, seed)
            var fuzzedGood = fsrs.fuzzIntervalDays(rawGood, seed)
            var fuzzedEasy = fsrs.fuzzIntervalDays(rawEasy, seed)

            // 施加严格单调性截断约束：Hard <= Good <= Easy
            if (fuzzedHard > fuzzedGood) {
                fuzzedHard = fuzzedGood
            }
            if (fuzzedEasy < fuzzedGood) {
                fuzzedEasy = fuzzedGood
            }

            newInterval = when (rating) {
                FsrsRating.Hard -> fuzzedHard
                FsrsRating.Good -> fuzzedGood
                FsrsRating.Easy -> fuzzedEasy
                else -> fuzzedGood
            }
        }

        // 6. 计算日期
        val nextReviewDate = if (newRepetitionCount == 0 && quality < 3) {
            0L // 新卡失败，不设置复习日期
        } else {
            today + newInterval
        }

        val firstLearnedDate = when {
            item.repetitionCount == 0 && quality >= 3 -> today // 新卡首次通过
            else -> item.firstLearnedDate
        }

        val lastReviewedDate = today

        // 计算类型 (对齐 Anki)
        val type = if (newRepetitionCount > 0 && quality >= 3) {
            2 // Review
        } else if (quality < 3 && item.repetitionCount > 0) {
            3 // Relearning
        } else if (quality < 3) {
            1 // Learning
        } else {
            item.type // 保持现状或由 Scheduler 进一步处理
        }

        return SrsUpdateResult(
            repetitionCount = newRepetitionCount,
            stability = newState.stability,
            difficulty = newState.difficulty,
            interval = newInterval,
            nextReviewDate = nextReviewDate,
            lastReviewedDate = lastReviewedDate,
            firstLearnedDate = firstLearnedDate,
            type = type
        )
    }

    private fun mapQualityToRating(quality: Int): FsrsRating {
        return when {
            quality < 3 -> FsrsRating.Again
            quality == 3 -> FsrsRating.Hard
            quality == 4 -> FsrsRating.Good
            else -> FsrsRating.Easy
        }
    }

    private fun buildFuzzSeed(item: SrsItem, today: Long, repetitions: Int): Long {
        val itemId = when (item) {
            is Word -> item.id
            is Grammar -> item.id
            else -> 0
        }
        return (itemId.toLong() shl 32) xor (today shl 8) xor repetitions.toLong()
    }

    override fun forgettingCurve(elapsedDays: Float, stability: Float): Float {
        return fsrs.forgettingCurve(elapsedDays, stability)
    }

    override fun nextInterval(stability: Float, retention: Float): Float {
        return fsrs.nextInterval(stability, retention)
    }
}

