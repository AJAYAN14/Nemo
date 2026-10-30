package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.presentation.LearningItem
import com.jian.nemo.feature.learning.presentation.LearningUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 学习会话共享状态容器 (系统设计 §4.2.2 内部状态字段归属)
 *
 * 承载原 LearningViewModel 的跨 delegate 内部状态：
 * - _learningSteps / _learningDueTimes：Rating / SessionLifecycle / QueueNavigation / CardOps / Undo 共享
 * - _lapseCounts：Rating 读写 + Undo 恢复（MutableStateFlow 并发安全）
 * - _requeuedItems：Rating 读写 + Undo / SessionLifecycle 恢复
 * - _sessionLockedDay：会话锁定的学习日（零点跨天保护）
 * - ratingJob / navigationJob / loadingJob：防抖 Job
 *
 * 由 ViewModel 创建唯一实例，构造注入各 delegate（共享同一引用）。
 * 全部在主线程（viewModelScope）访问，无跨线程竞争。
 */
class LearningSessionState {

    /** 学习阶段追踪 (ItemId -> StepIndex) */
    val learningSteps = mutableMapOf<Int, Int>()

    /** Anki-Mode: 到期时间记录 (ID -> DueTime Epoch Millis) */
    val learningDueTimes = mutableMapOf<Int, Long>()

    /** 失败次数追踪 (用于钉子户检测)，使用 StateFlow 确保并发安全 */
    val lapseCounts = MutableStateFlow<Map<Int, Int>>(emptyMap())

    /** 重入队标记 (用于红标显示) */
    val requeuedItems = mutableSetOf<Int>()

    /** 会话锁定的学习日 (用于零点跨天保护) */
    var sessionLockedDay: Long? = null

    /** 学习日重置时间 (小时)，SettingsDelegate 维护 */
    var resetHour: Int = 4

    /** Leech 阈值，SettingsDelegate 维护 */
    var leechThreshold: Int = 5

    /** Leech 处置动作 (bury_today / skip)，SettingsDelegate 维护 */
    var leechAction: String = "skip"

    /** 学习步进配置 (分钟列表)，SettingsDelegate 维护 */
    var learningStepsConfig: List<Int> = listOf(1, 10)

    /** 重学步进配置 (分钟列表)，SettingsDelegate 维护 */
    var relearningStepsConfig: List<Int> = listOf(1, 10)

    /** 提前学习限制 (毫秒)，SettingsDelegate 维护 */
    var learnAheadLimitMs: Long = 20 * 60 * 1000L

    /** 显示答案等待时长 (毫秒)，SettingsDelegate 维护 */
    var showAnswerDelayMs: Long = 5000L

    /** 评分防抖 Job (RatingDelegate 持有) */
    var ratingJob: Job? = null

    /** 导航防抖 Job (QueueNavigationDelegate 持有) */
    var navigationJob: Job? = null

    /** 会话加载 Job (SessionLifecycleDelegate 持有) */
    var loadingJob: Job? = null

    /** 当前会话锁定的学习日（兜底实时计算），零点跨天保护统一入口 */
    fun lockedDayOrToday(): Long =
        sessionLockedDay ?: DateTimeUtils.getLearningDay(resetHour)
}

/** 防抖/容差常量基线 (系统设计 §3.5.3) */
object DebounceConstants {
    /** 导航防抖延迟 (ms) */
    const val NAVIGATION_DEBOUNCE_MS = 400L

    /** 评分防抖延迟 (ms) */
    const val RATING_DEBOUNCE_MS = 100L

    /** 点击边界容差，避免倒计时到点仍被拦截 */
    const val SHOW_ANSWER_GRACE_MS = 150L
}

/**
 * 从 UiState 构建当前 LearningItem（原 ViewModel.getCurrentItem，
 * RatingDelegate / SettingsDelegate / UndoDelegate / CardOpsDelegate 共用）
 */
fun LearningUiState.currentLearningItem(): LearningItem? = when (learningMode) {
    is com.jian.nemo.feature.learning.presentation.LearningMode.Word ->
        currentWord?.let { LearningItem.WordItem(it) }
    is com.jian.nemo.feature.learning.presentation.LearningMode.Grammar ->
        currentGrammar?.let { LearningItem.GrammarItem(it) }
}

/**
 * 计算 quality -> 间隔文本预览（原 ViewModel.calculateRatingIntervals，
 * RatingDelegate / SessionLifecycleDelegate / QueueNavigationDelegate / UndoDelegate 共用）
 */
fun calculateRatingIntervals(
    item: LearningItem?,
    preview: SrsIntervalPreview,
    sessionState: LearningSessionState
): Map<Int, String> {
    if (item == null) return emptyMap()
    val today = sessionState.lockedDayOrToday()
    return when (item) {
        is LearningItem.WordItem -> preview.calculate(
            item.word, item.id,
            sessionState.learningSteps,
            sessionState.learningStepsConfig,
            sessionState.relearningStepsConfig,
            today
        )
        is LearningItem.GrammarItem -> preview.calculate(
            item.grammar, item.id,
            sessionState.learningSteps,
            sessionState.learningStepsConfig,
            sessionState.relearningStepsConfig,
            today
        )
    }
}
