package com.jian.nemo.feature.learning.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import com.jian.nemo.core.domain.usecase.grammar.GetDueGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.review.ProcessReviewResultUseCase
import com.jian.nemo.core.domain.usecase.word.GetDueWordsUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.feature.learning.domain.LearningQueueManager
import com.jian.nemo.feature.learning.domain.LearningScheduler
import com.jian.nemo.feature.learning.domain.QueueSelectionResult
import com.jian.nemo.feature.learning.domain.ScheduleResult
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.presentation.LearningItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import com.jian.nemo.core.data.manager.CloudBackupManager

/**
 * Review status
 */
enum class ReviewStatus {
    Loading,
    Reviewing,
    Waiting,
    SessionCompleted
}

/**
 * Review UI State
 */
data class ReviewUiState(
    val status: ReviewStatus = ReviewStatus.Loading,
    val reviewItems: List<ReviewPreviewItem> = emptyList(),
    val currentIndex: Int = 0,
    val currentItem: ReviewPreviewItem? = null,
    val isAnswerShown: Boolean = false,
    val isProcessing: Boolean = false,
    val totalCompleted: Int = 0,
    val error: String? = null,
    val waitingUntil: Long = 0L,

    // UI Helpers
    val ratingIntervals: Map<Int, String> = emptyMap()
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val getDueGrammarsUseCase: GetDueGrammarsUseCase,
    private val processReviewResultUseCase: ProcessReviewResultUseCase,
    private val srsCalculator: SrsCalculator,
    private val settingsRepository: SettingsRepository,
    private val learningScheduler: LearningScheduler,
    private val srsIntervalPreview: SrsIntervalPreview,
    private val updateWordUseCase: UpdateWordUseCase,
    private val updateGrammarUseCase: UpdateGrammarUseCase,
    private val studyRecordRepository: StudyRecordRepository,
    private val learningQueueManager: LearningQueueManager,
    private val cloudBackupManager: CloudBackupManager
) : ViewModel() {

    companion object {
        private const val LEECH_ACTION_SKIP = "skip"
        private const val LEECH_ACTION_BURY_TODAY = "bury_today"
    }

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    // ========== Relearning 内部状态 ==========

    /** 重学步进追踪 (ItemId -> StepIndex) */
    private val _relearningSteps = mutableMapOf<Int, Int>()

    /** 重学到期时间 (ItemId -> DueTime Epoch Millis) */
    private val _relearningDueTimes = mutableMapOf<Int, Long>()

    /** 重学步进配置 (分钟列表) */
    private var _relearningStepsConfig: List<Int> = listOf(1, 10)

    /** 会话锁定的学习日 */
    private var _sessionLockedDay: Long? = null
    private var _resetHour: Int = 4
    private var _learnAheadLimitMinutes: Int = 20
    private var _leechThreshold: Int = 5
    private var _leechAction: String = LEECH_ACTION_SKIP

    init {
        loadData()
        
        // 监听 Leech 设置变化
        viewModelScope.launch {
            settingsRepository.leechThresholdFlow.collect { threshold ->
                _leechThreshold = threshold.coerceAtLeast(1)
            }
        }
        
        viewModelScope.launch {
            settingsRepository.leechActionFlow.collect { action ->
                _leechAction = action
            }
        }

        // 监听完成状态以触发自动备份
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.map { it.status }
                .distinctUntilChanged()
                .filter { it == ReviewStatus.SessionCompleted }
                .collect {
                    cloudBackupManager.tryAutoBackup()
                }
        }
    }

    // ========== 数据加载 ==========

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(status = ReviewStatus.Loading) }

            try {
                // 0. 加载配置
                _resetHour = settingsRepository.learningDayResetHourFlow.first()
                _sessionLockedDay = DateTimeUtils.getLearningDay(_resetHour)
                _learnAheadLimitMinutes = settingsRepository.learnAheadLimitFlow.first().coerceAtLeast(0)
                _leechThreshold = settingsRepository.leechThresholdFlow.first().coerceAtLeast(1)
                _leechAction = settingsRepository.leechActionFlow.first()

                val relearningStepsStr = settingsRepository.relearningStepsFlow.first()
                _relearningStepsConfig = parseSteps(relearningStepsStr)

                // 1. Get Due Words
                val dueWordsResult = getDueWordsUseCase().first { it !is Result.Loading }
                val dueWords = if (dueWordsResult is Result.Success) dueWordsResult.data else emptyList()

                // 2. Get Due Grammars
                val dueGrammarsResult = getDueGrammarsUseCase().first { it !is Result.Loading }
                val dueGrammars = if (dueGrammarsResult is Result.Success) dueGrammarsResult.data else emptyList()

                // 3. 全局混排：按 due day 排序后统一调度，避免单词/语法分段处理
                val combinedList = (dueWords.map { ReviewPreviewItem.WordItem(it) } +
                    dueGrammars.map { ReviewPreviewItem.GrammarItem(it) })
                    .sortedWith(compareBy<ReviewPreviewItem> { it.dueDay }.thenBy { it.itemId })

                if (combinedList.isNotEmpty()) {
                    selectNext(combinedList, 0)
                } else {
                    _uiState.update {
                        it.copy(
                            status = ReviewStatus.SessionCompleted,
                            reviewItems = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(status = ReviewStatus.Reviewing, error = "加载失败: ${e.message}")
                }
            }
        }
    }

    // ========== UI 交互 ==========

    fun showAnswer() {
        _uiState.update { it.copy(isAnswerShown = true) }
    }

    /**
     * 手动从等待状态恢复
     */
    fun resumeFromWaiting() {
        _uiState.update { it.copy(waitingUntil = 0L) }
        selectNext(_uiState.value.reviewItems, _uiState.value.currentIndex)
    }

    // ========== 核心方法：评分 ==========

    /**
     * 评分 (对齐 Anki Relearning Steps)
     *
     * - quality < 3: Again → Lapse Penalty + 进入/重新进入 Relearning Steps
     * - quality 3-4 (重学中): Hard/Good → Step 流转
     * - quality 3-4 (正常复习): Hard/Good → 直接 SRS 更新
     * - quality 5: Easy → 直接毕业/SRS 更新
     */
    fun rateItem(quality: Int) {
        if (_uiState.value.isProcessing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            try {
                val currentItem = _uiState.value.currentItem ?: run {
                    _uiState.update { it.copy(isProcessing = false) }
                    return@launch
                }

                val itemId = currentItem.itemId
                val isRelearning = _relearningSteps.containsKey(itemId)

                when {
                    // Easy (5): 无论状态，直接毕业/SRS更新
                    quality == 5 -> {
                        _relearningSteps.remove(itemId)
                        _relearningDueTimes.remove(itemId)
                        processDirectSrs(currentItem, quality)
                    }

                    // Again (< 3): 失败 → 进入/重新进入 Relearning
                    quality < 3 -> {
                        handleFailure(currentItem, quality)
                    }

                    // Hard/Good (3-4)
                    else -> {
                        if (isRelearning) {
                            // 在重学中 → 用 LearningScheduler 处理 Step 流转
                            handleRelearningPass(currentItem, quality)
                        } else {
                            // 正常复习 → 直接 SRS 更新 + 移出
                            processDirectSrs(currentItem, quality)
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ 复习评分异常: ${e.message}")
                _uiState.update {
                    it.copy(isProcessing = false, error = "评分异常: ${e.message}")
                }
            }
        }
    }

    // ========== 失败处理 (Again) ==========

    /**
     * 处理失败 (Again)
     *
     * 1. Lapse Penalty → 更新 DB 中的 interval/stability (FSRS 惩罚)
     * 2. 用 LearningScheduler 调度 → Requeue 或 Leech
     */
    private suspend fun handleFailure(item: ReviewPreviewItem, quality: Int) {
        val itemId = item.itemId

        // 1. Lapse Penalty (更新 DB 中的 interval/stability)
        val penalizedItem = applyLapsePenalty(item, quality)

        // 2. 读取累计 lapse（跨会话），再写回 +1
        val currentLapseCount = when (item) {
            is ReviewPreviewItem.WordItem -> settingsRepository.wordLapsesFlow.first()[itemId] ?: 0
            is ReviewPreviewItem.GrammarItem -> settingsRepository.grammarLapsesFlow.first()[itemId] ?: 0
        }
        when (item) {
            is ReviewPreviewItem.WordItem -> settingsRepository.incrementWordLapse(itemId)
            is ReviewPreviewItem.GrammarItem -> settingsRepository.incrementGrammarLapse(itemId)
        }

        // 3. 用 LearningScheduler 调度
        val penalizedLearningItem = penalizedItem.toLearningItem(
            step = _relearningSteps[itemId] ?: 0,
            dueTime = _relearningDueTimes[itemId] ?: 0L
        )
        val result = learningScheduler.scheduleFailure(
            item = penalizedLearningItem,
            currentLapseCount = currentLapseCount,
            stepConfig = _relearningStepsConfig,
            leechThreshold = _leechThreshold
        )

        // 4. 处理调度结果
        handleScheduleResult(result)
    }

    // ========== 重学中通过 (Hard/Good) ==========

    /**
     * 处理重学中的通过 (Hard/Good)
     */
    private suspend fun handleRelearningPass(item: ReviewPreviewItem, quality: Int) {
        val itemId = item.itemId
        val currentStep = _relearningSteps[itemId] ?: 0

        val learningItem = item.toLearningItem(
            step = currentStep,
            dueTime = _relearningDueTimes[itemId] ?: 0L
        )
        val result = learningScheduler.schedulePass(
            item = learningItem,
            quality = quality,
            currentStep = currentStep,
            stepConfig = _relearningStepsConfig
        )

        handleScheduleResult(result)
    }

    // ========== 调度结果处理 ==========

    /**
     * 处理 LearningScheduler 的调度结果
     */
    private suspend fun handleScheduleResult(result: ScheduleResult) {
        when (result) {
            is ScheduleResult.Leech -> {
                println("🩸 复习钉子户 (Leech): ${result.item.displayName} (Lapses: ${result.totalLapses})")
                _relearningSteps.remove(result.item.id)
                _relearningDueTimes.remove(result.item.id)
                handleLeech(result.item)
            }

            is ScheduleResult.Graduate -> {
                println("🎓 重学毕业: ${result.item.displayName}")
                _relearningSteps.remove(result.item.id)
                _relearningDueTimes.remove(result.item.id)
                processRelearningGraduation(result.item)
            }

            is ScheduleResult.Requeue -> {
                val item = result.updatedItem
                _relearningSteps[item.id] = result.nextStepIndex
                _relearningDueTimes[item.id] = result.dueTime

                if (result.isLapse) {
                    println("❌ 复习失败 (Again): ${item.displayName}, Step ${result.nextStepIndex}, Due in ${(result.dueTime - System.currentTimeMillis()) / 1000}s")
                } else {
                    println("🔄 复习重学进阶: ${item.displayName}, Step ${result.nextStepIndex}")
                }

                // 重入队到末尾并前进
                reQueueToEnd(item.toReviewPreviewItem())
            }
        }
    }

    // ========== SRS 更新 ==========

    /**
     * 直接 SRS 更新 (正常复习通过 / Easy 直接毕业)
     *
     * 使用 ProcessReviewResultUseCase 完成完整的 SRS + 统计 + 日志
     */
    private suspend fun processDirectSrs(item: ReviewPreviewItem, quality: Int) {
        when (item) {
            is ReviewPreviewItem.WordItem -> {
                processReviewResultUseCase.processWord(item.word, quality)
            }
            is ReviewPreviewItem.GrammarItem -> {
                processReviewResultUseCase.processGrammar(item.grammar, quality)
            }
        }

        println("📝 复习SRS更新: ${item.displayName}, quality=$quality")

        _uiState.update {
            it.copy(totalCompleted = it.totalCompleted + 1)
        }

        removeCurrentAndMoveNext()
    }

    /**
     * 应用失败惩罚 (Lapse Penalty)
     *
     * 当用户点 Again 时，立即通过 FSRS 计算惩罚后的 interval/stability 并更新 DB。
     * 重学毕业时基于这个惩罚后的值调度。
     */
    private suspend fun applyLapsePenalty(item: ReviewPreviewItem, quality: Int): ReviewPreviewItem {
        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)

        val penalizedItem = when (item) {
            is ReviewPreviewItem.WordItem -> {
                val srsResult = srsCalculator.calculate(item.word, quality, today)
                val updatedWord = item.word.copy(
                    interval = srsResult.interval,
                    repetitionCount = srsResult.repetitionCount,
                    stability = srsResult.stability,
                    difficulty = srsResult.difficulty,
                    nextReviewDate = srsResult.nextReviewDate,
                    lastReviewedDate = srsResult.lastReviewedDate,
                    firstLearnedDate = srsResult.firstLearnedDate,
                    lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                )
                updateWordUseCase(updatedWord)
                ReviewPreviewItem.WordItem(updatedWord)
            }
            is ReviewPreviewItem.GrammarItem -> {
                val srsResult = srsCalculator.calculate(item.grammar, quality, today)
                val updatedGrammar = item.grammar.copy(
                    interval = srsResult.interval,
                    repetitionCount = srsResult.repetitionCount,
                    stability = srsResult.stability,
                    difficulty = srsResult.difficulty,
                    nextReviewDate = srsResult.nextReviewDate,
                    lastReviewedDate = srsResult.lastReviewedDate,
                    firstLearnedDate = srsResult.firstLearnedDate,
                    lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                )
                updateGrammarUseCase(updatedGrammar)
                ReviewPreviewItem.GrammarItem(updatedGrammar)
            }
        }

        println("⚡ Lapse Penalty 已应用: ${item.displayName}")
        return penalizedItem
    }

    /**
     * 重学毕业 (Relearning Graduation)
     *
     * 此时卡片的 interval 已在 Lapse 时被 FSRS 惩罚过。
     * 毕业时只需将其移回复习队列，设定 nextReviewDate = today + penalized_interval。
     */
    private suspend fun processRelearningGraduation(learningItem: LearningItem) {
        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)

        when (learningItem) {
            is LearningItem.WordItem -> {
                val word = learningItem.word
                val interval = word.interval.coerceAtLeast(1)
                val updatedWord = word.copy(
                    interval = interval,
                    repetitionCount = word.repetitionCount + 1,
                    lastReviewedDate = today,
                    nextReviewDate = today + interval,
                    lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                )
                updateWordUseCase(updatedWord)
                studyRecordRepository.incrementReviewedWords(1)
            }
            is LearningItem.GrammarItem -> {
                val grammar = learningItem.grammar
                val interval = grammar.interval.coerceAtLeast(1)
                val updatedGrammar = grammar.copy(
                    interval = interval,
                    repetitionCount = grammar.repetitionCount + 1,
                    lastReviewedDate = today,
                    nextReviewDate = today + interval,
                    lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                )
                updateGrammarUseCase(updatedGrammar)
                studyRecordRepository.incrementReviewedGrammars(1)
            }
        }

        println("🎓 重学毕业完成: ${learningItem.displayName}")

        _uiState.update {
            it.copy(totalCompleted = it.totalCompleted + 1)
        }

        removeCurrentAndMoveNext()
    }

    /**
     * 处理钉子户 (Leech)
     *
     * 累计失败达到阈值后，按配置执行 skip 或 bury_today
     */
    private suspend fun handleLeech(learningItem: LearningItem) {
        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
        val action = if (_leechAction == LEECH_ACTION_BURY_TODAY) LEECH_ACTION_BURY_TODAY else LEECH_ACTION_SKIP

        when (learningItem) {
            is LearningItem.WordItem -> {
                val updatedWord = if (action == LEECH_ACTION_BURY_TODAY) {
                    learningItem.word.copy(
                        nextReviewDate = today + 1,
                        lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                    )
                } else {
                    learningItem.word.copy(
                        isSkipped = true,
                        lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                    )
                }
                updateWordUseCase(updatedWord)
            }
            is LearningItem.GrammarItem -> {
                val updatedGrammar = if (action == LEECH_ACTION_BURY_TODAY) {
                    learningItem.grammar.copy(
                        nextReviewDate = today + 1,
                        lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                    )
                } else {
                    learningItem.grammar.copy(
                        isSkipped = true,
                        lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                    )
                }
                updateGrammarUseCase(updatedGrammar)
            }
        }

        _uiState.update {
            val message = if (action == LEECH_ACTION_BURY_TODAY) {
                "已暂埋钉子户到明天: ${learningItem.displayName}"
            } else {
                "已暂停钉子户: ${learningItem.displayName}"
            }
            it.copy(error = message)
        }

        removeCurrentAndMoveNext()
    }

    // ========== 队列操作 ==========

    /**
     * 重入队到末尾并前进
     *
     * 将当前项移除，追加到列表末尾，然后选择下一项。
     */
    private fun reQueueToEnd(requeuedItem: ReviewPreviewItem) {
        val state = _uiState.value
        val currentIndex = state.currentIndex

        val newList = state.reviewItems.toMutableList()
        if (currentIndex in newList.indices) {
            newList.removeAt(currentIndex)
        }
        // 追加到末尾
        newList.add(requeuedItem)

        selectNext(newList, currentIndex)
    }

    /**
     * 移除当前项并移到下一个 (毕业/直接通过时使用)
     */
    private fun removeCurrentAndMoveNext() {
        val state = _uiState.value
        val currentIndex = state.currentIndex

        val newList = state.reviewItems.toMutableList()
        if (currentIndex in newList.indices) {
            newList.removeAt(currentIndex)
        }

        if (newList.isEmpty()) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    status = ReviewStatus.SessionCompleted,
                    reviewItems = emptyList()
                )
            }
            return
        }

        selectNext(newList, currentIndex)
    }

    /**
     * 选择下一项并更新 UI (严格调度版)
     */
    private fun selectNext(newList: List<ReviewPreviewItem>, preferredIndex: Int) {
        if (newList.isEmpty()) {
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    status = ReviewStatus.SessionCompleted,
                    reviewItems = emptyList()
                )
            }
            return
        }

        // 调用严格的时间调度管理器
        val selection = learningQueueManager.selectNextItem(
            items = newList,
            getDueTime = { _relearningDueTimes[it.itemId] ?: 0L },
            now = System.currentTimeMillis(),
            learnAheadLimitMs = _learnAheadLimitMinutes * 60 * 1000L,
            preferredIndex = preferredIndex
        )

        when (selection) {
            is QueueSelectionResult.Next -> {
                val nextItem = selection.item
                val nextIndex = selection.index

                _uiState.update {
                    it.copy(
                        status = ReviewStatus.Reviewing,
                        isProcessing = false,
                        reviewItems = newList,
                        currentIndex = nextIndex,
                        currentItem = nextItem,
                        isAnswerShown = false,
                        waitingUntil = 0L,
                        ratingIntervals = calculateIntervalsSync(nextItem)
                    )
                }
            }
            is QueueSelectionResult.Wait -> {
                _uiState.update {
                    it.copy(
                        status = ReviewStatus.Waiting,
                        isProcessing = false,
                        reviewItems = newList,
                        waitingUntil = selection.waitingUntil
                    )
                }
            }
            is QueueSelectionResult.Empty -> {
                _uiState.update {
                    it.copy(
                        status = ReviewStatus.SessionCompleted,
                        isProcessing = false,
                        reviewItems = emptyList()
                    )
                }
            }
        }
    }

    // ========== 间隔预览 ==========

    /**
     * 同步计算间隔预览 (修复原版异步返回空 Map 的 Bug)
     *
     * 复用 SrsIntervalPreview，正确显示重学中卡片的步进时间
     */
    private fun calculateIntervalsSync(item: ReviewPreviewItem): Map<Int, String> {
        val today = _sessionLockedDay ?: DateTimeUtils.getLearningDay(_resetHour)
        val itemId = item.itemId

        return when (item) {
            is ReviewPreviewItem.WordItem -> srsIntervalPreview.calculate(
                item = item.word,
                itemId = itemId,
                steps = _relearningSteps,
                learningStepsConfig = listOf(1, 10), // 复习模块不使用新词学习步骤
                relearningStepsConfig = _relearningStepsConfig,
                today = today
            )
            is ReviewPreviewItem.GrammarItem -> srsIntervalPreview.calculate(
                item = item.grammar,
                itemId = itemId,
                steps = _relearningSteps,
                learningStepsConfig = listOf(1, 10),
                relearningStepsConfig = _relearningStepsConfig,
                today = today
            )
        }
    }

    // ========== 工具方法 ==========

    private fun parseSteps(stepsStr: String): List<Int> {
        return stepsStr.split(" ", ",")
            .mapNotNull { it.trim().toIntOrNull() }
            .ifEmpty { listOf(1, 10) }
    }
}

// ========== ReviewPreviewItem 辅助扩展 ==========

/** 获取项目 ID */
val ReviewPreviewItem.itemId: Int
    get() = when (this) {
        is ReviewPreviewItem.WordItem -> word.id
        is ReviewPreviewItem.GrammarItem -> grammar.id
    }

/** 获取显示名称 */
val ReviewPreviewItem.displayName: String
    get() = when (this) {
        is ReviewPreviewItem.WordItem -> word.japanese
        is ReviewPreviewItem.GrammarItem -> grammar.grammar
    }

/** 转换为 LearningItem (供 LearningScheduler 使用) */
fun ReviewPreviewItem.toLearningItem(step: Int = 0, dueTime: Long = 0L): LearningItem {
    return when (this) {
        is ReviewPreviewItem.WordItem -> LearningItem.WordItem(word, step, dueTime)
        is ReviewPreviewItem.GrammarItem -> LearningItem.GrammarItem(grammar, step, dueTime)
    }
}

fun LearningItem.toReviewPreviewItem(): ReviewPreviewItem {
    return when (this) {
        is LearningItem.WordItem -> ReviewPreviewItem.WordItem(word)
        is LearningItem.GrammarItem -> ReviewPreviewItem.GrammarItem(grammar)
    }
}

/** 获取到期学习日（用于全局混排） */
val ReviewPreviewItem.dueDay: Long
    get() = when (this) {
        is ReviewPreviewItem.WordItem -> word.nextReviewDate
        is ReviewPreviewItem.GrammarItem -> grammar.nextReviewDate
    }
