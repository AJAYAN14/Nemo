package com.jian.nemo.core.domain.algorithm

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import java.util.Random

/**
 * FSRS 6 记忆状态
 *
 * @param stability 记忆稳定性 (单位: 天) — 记忆持续多少天后，回忆概率降至 90%
 * @param difficulty 难度 (范围: 1-10)
 */
data class MemoryState(
    val stability: Float = 0f,
    val difficulty: Float = 0f
)

/**
 * FSRS 评分 (对齐 Anki)
 */
enum class FsrsRating(val value: Int) {
    Again(1),
    Hard(2),
    Good(3),
    Easy(4);
}

/**
 * 每个评分按钮对应的下一状态预览
 */
data class NextStates(
    val again: ItemState,
    val hard: ItemState,
    val good: ItemState,
    val easy: ItemState
)

data class ItemState(
    val memory: MemoryState,
    val interval: Float
)

/**
 * FSRS 6 核心算法
 *
 * 纯 Kotlin 实现，参考 Anki Rust 后端 (fsrs-rs v5.1.0)
 * 包含完整的 21 参数模型
 *
 * 参考:
 * - https://github.com/open-spaced-repetition/fsrs-rs/blob/main/src/model.rs
 * - https://github.com/open-spaced-repetition/fsrs-rs/blob/main/src/inference.rs
 */
class FsrsAlgorithm(
    private val parameters: FloatArray = DEFAULT_PARAMETERS.clone(),
    private val desiredRetention: Float = 0.9f
) {
    companion object {
        /**
         * FSRS 6 默认参数 (21个)
         * 来源: fsrs-rs DEFAULT_PARAMETERS
         */
        val DEFAULT_PARAMETERS = floatArrayOf(
            0.212f,     // w[0]: init_stability for Again
            1.2931f,    // w[1]: init_stability for Hard
            2.3065f,    // w[2]: init_stability for Good
            8.2956f,    // w[3]: init_stability for Easy
            6.4133f,    // w[4]: init_difficulty base
            0.8334f,    // w[5]: init_difficulty rating factor
            3.0194f,    // w[6]: next_difficulty delta factor
            0.001f,     // w[7]: mean_reversion weight
            1.8722f,    // w[8]: stability_after_success exp base
            0.1666f,    // w[9]: stability_after_success stability power
            0.796f,     // w[10]: stability_after_success retrievability factor
            1.4835f,    // w[11]: stability_after_failure base
            0.0614f,    // w[12]: stability_after_failure difficulty power
            0.2629f,    // w[13]: stability_after_failure stability power
            1.6483f,    // w[14]: stability_after_failure retrievability factor
            0.6014f,    // w[15]: hard_penalty
            1.8729f,    // w[16]: easy_bonus
            0.5425f,    // w[17]: short_term factor
            0.0912f,    // w[18]: short_term rating offset
            0.0658f,    // w[19]: short_term stability power
            0.1542f     // w[20]: decay (FSRS 6)
        )

        /** 稳定性范围 */
        const val S_MIN = 0.01f
        const val S_MAX = 36500f

        /** 难度范围 */
        const val D_MIN = 1f
        const val D_MAX = 10f

        /** 最大间隔 (天) */
        const val MAX_INTERVAL = 36500
    }

    private val w get() = parameters

    // ========== 核心公式 ==========

    /**
     * 幂律遗忘曲线
     *
     * R(t, S) = (1 + factor * t/S)^(-decay)
     *
     * 其中 factor = 0.9^(1/-decay) - 1
     *
     * @param elapsedDays 自上次复习以来的天数
     * @param stability 当前稳定性
     * @return 回忆概率 (0-1)
     */
    fun forgettingCurve(elapsedDays: Float, stability: Float): Float {
        val decay = w[20]
        val factor = 0.9f.pow(1f / -decay) - 1f
        return (elapsedDays / stability * factor + 1f).pow(-decay)
    }

    /**
     * 计算下一个最优间隔
     *
     * I(S, R) = S / factor * (R^(1/-decay) - 1)
     *
     * @param stability 当前稳定性
     * @param retention 目标留存率 (默认 0.9)
     * @return 最优间隔 (天)
     */
    fun nextInterval(stability: Float, retention: Float = desiredRetention): Float {
        val decay = w[20]
        val factor = 0.9f.pow(1f / -decay) - 1f
        return stability / factor * (retention.pow(1f / -decay) - 1f)
    }

    /**
     * 新卡初始稳定性
     *
     * S0(G) = w[G-1]
     */
    fun initStability(rating: FsrsRating): Float {
        return w[rating.value - 1].coerceIn(S_MIN, S_MAX)
    }

    /**
     * 新卡初始难度
     *
     * D0(G) = w[4] - exp(w[5] * (G - 1)) + 1
     */
    fun initDifficulty(rating: FsrsRating): Float {
        return (w[4] - exp(w[5] * (rating.value - 1f).toDouble()).toFloat() + 1f)
            .coerceIn(D_MIN, D_MAX)
    }

    /**
     * 成功后的新稳定性
     *
     * S'_r(D, S, R, G) = S * (exp(w[8]) * (11 - D) * S^(-w[9]) * (exp((1-R)*w[10]) - 1) * hard_penalty * easy_bonus + 1)
     */
    fun stabilityAfterSuccess(
        stability: Float,
        difficulty: Float,
        retrievability: Float,
        rating: FsrsRating
    ): Float {
        val hardPenalty = if (rating == FsrsRating.Hard) w[15] else 1f
        val easyBonus = if (rating == FsrsRating.Easy) w[16] else 1f

        val newS = stability * (
            exp(w[8].toDouble()).toFloat() *
            (11f - difficulty) *
            stability.pow(-w[9]) *
            (exp(((1f - retrievability) * w[10]).toDouble()).toFloat() - 1f) *
            hardPenalty *
            easyBonus + 1f
        )
        return newS.coerceIn(S_MIN, S_MAX)
    }

    /**
     * 失败后的新稳定性
     *
     * S'_f(D, S, R) = w[11] * D^(-w[12]) * ((S+1)^w[13] - 1) * exp((1-R)*w[14])
     * 下限: S / exp(w[17] * w[18])
     */
    fun stabilityAfterFailure(
        stability: Float,
        difficulty: Float,
        retrievability: Float
    ): Float {
        val newS = w[11] *
            difficulty.pow(-w[12]) *
            ((stability + 1f).pow(w[13]) - 1f) *
            exp(((1f - retrievability) * w[14]).toDouble()).toFloat()

        // 最小值约束: 失败后的稳定性不能低于 S / exp(w[17]*w[18])
        val minS = stability / exp((w[17] * w[18]).toDouble()).toFloat()

        return max(newS, minS).coerceIn(S_MIN, S_MAX)
    }

    /**
     * 短期稳定性 (用于 delta_t == 0 的情况，即同一天内的学习步骤)
     *
     * S'_st(S, G) = S * exp(w[17] * (G - 3 + w[18])) * S^(-w[19])
     * clamp >= 1 if G >= 2
     */
    fun stabilityShortTerm(stability: Float, rating: FsrsRating): Float {
        val sinc = exp((w[17] * (rating.value - 3f + w[18])).toDouble()).toFloat() *
            stability.pow(-w[19])

        val clampedSinc = if (rating.value >= 2) max(sinc, 1f) else sinc
        return (stability * clampedSinc).coerceIn(S_MIN, S_MAX)
    }

    /**
     * 计算下一个难度
     *
     * D'(D, G) = D + linear_damping(-w[6] * (G - 3), D)
     * 然后 mean reversion: w[7] * (D0(4) - D') + D'
     */
    fun nextDifficulty(difficulty: Float, rating: FsrsRating): Float {
        val deltaD = -w[6] * (rating.value - 3f)
        val linearDamped = deltaD * (10f - difficulty) / 9f
        val newD = difficulty + linearDamped

        // Mean reversion
        val d0Good = w[4] - exp(w[5] * (4f - 1f).toDouble()).toFloat() + 1f
        val reverted = w[7] * (d0Good - newD) + newD

        return reverted.coerceIn(D_MIN, D_MAX)
    }

    // ========== 高级 API ==========

    /**
     * 执行一步状态转换 (对齐 Anki 的 model.step)
     *
     * @param currentState 当前记忆状态 (null 表示新卡)
     * @param rating 评分
     * @param elapsedDays 距离上次复习的天数 (0 = 同一天/学习中)
     * @return 新的记忆状态
     */
    fun step(
        currentState: MemoryState?,
        rating: FsrsRating,
        elapsedDays: Float
    ): MemoryState {
        if (currentState == null || currentState.stability == 0f) {
            // 新卡: 使用初始值
            return MemoryState(
                stability = initStability(rating),
                difficulty = initDifficulty(rating)
            )
        }

        val s = currentState.stability.coerceIn(S_MIN, S_MAX)
        val d = currentState.difficulty.coerceIn(D_MIN, D_MAX)

        val newS: Float
        if (elapsedDays == 0f) {
            // 同一天内: 使用短期稳定性
            newS = stabilityShortTerm(s, rating)
        } else {
            // 跨天: 计算 retrievability 然后更新
            val r = forgettingCurve(elapsedDays, s)
            newS = if (rating == FsrsRating.Again) {
                stabilityAfterFailure(s, d, r)
            } else {
                stabilityAfterSuccess(s, d, r, rating)
            }
        }

        val newD = nextDifficulty(d, rating)

        return MemoryState(
            stability = newS.coerceIn(S_MIN, S_MAX),
            difficulty = newD.coerceIn(D_MIN, D_MAX)
        )
    }

    /**
     * 计算四个按钮的下一状态预览
     */
    fun nextStates(
        currentState: MemoryState?,
        elapsedDays: Float
    ): NextStates {
        val ratings = listOf(FsrsRating.Again, FsrsRating.Hard, FsrsRating.Good, FsrsRating.Easy)
        val states = ratings.map { rating ->
            val newState = step(currentState, rating, elapsedDays)
            val interval = nextInterval(newState.stability)
            ItemState(memory = newState, interval = interval)
        }
        return NextStates(
            again = states[0],
            hard = states[1],
            good = states[2],
            easy = states[3]
        )
    }

    /**
     * 计算间隔并取整 (用于实际调度)
     */
    fun nextIntervalDays(stability: Float): Int {
        val raw = nextInterval(stability)
        return raw.roundToInt().coerceIn(1, MAX_INTERVAL)
    }

    /**
     * 计算带抖动的间隔 (deterministic fuzz)
     *
     * 目的：减少到期日完全扎堆，提升真实复习分布体验。
     * 兼容策略：
     * - 小间隔不抖动，避免影响新学阶段节奏。
     * - 使用种子保证同一输入稳定可复现。
     */
    fun nextIntervalDaysWithFuzz(stability: Float, seed: Long): Int {
        val base = nextIntervalDays(stability)
        return fuzzIntervalDays(base, seed)
    }

    fun fuzzIntervalDays(baseInterval: Int, seed: Long): Int {
        if (baseInterval < 3) return baseInterval

        val span = when {
            baseInterval < 7 -> 1
            baseInterval < 30 -> max(1, (baseInterval * 0.08f).roundToInt())
            baseInterval < 90 -> max(2, (baseInterval * 0.12f).roundToInt())
            else -> max(4, (baseInterval * 0.15f).roundToInt())
        }

        val random = Random(seed xor (baseInterval.toLong() * 1103515245L + 12345L))
        val delta = random.nextInt(span * 2 + 1) - span
        return (baseInterval + delta).coerceIn(1, MAX_INTERVAL)
    }
}
