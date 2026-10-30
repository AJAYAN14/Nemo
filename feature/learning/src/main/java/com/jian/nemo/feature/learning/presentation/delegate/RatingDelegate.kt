package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.ReviewLogRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.feature.learning.domain.LearningQueueManager
import com.jian.nemo.feature.learning.domain.LearningScheduler
import com.jian.nemo.feature.learning.domain.ScheduleResult
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.presentation.CardBadge
import com.jian.nemo.feature.learning.presentation.LearningItem
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.LearningStatus
import com.jian.nemo.feature.learning.presentation.cardBadge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 评分管线域 delegate (系统设计 §3.2.M4 / US-3 / F3)
 *
 * 承接原 LearningViewModel 评分核心链路：
 * - rate：防抖入口 + 按 quality 分支调度（5 熟词速通 / ≤2 忘记 / 3-4 掌握）
 * - applyLapsePenalty：纯内存计算失败惩罚
 * - processRelearningGraduation：重学毕业
 * - handleScheduleResult：Leech / Graduate / Requeue 三分支
 * - processSrsUpdate：SRS 更新与毕业
 * - persistRatingItemAsync：后台异步持久化
 *
 * 第二阶段合并 X4（processSrsUpdate / processRelearningGraduation）。
 */
class RatingDelegate(
    private val learningQueueManager: LearningQueueManager,
    private val learningScheduler: LearningScheduler,
    private val srsCalculator: SrsCalculator,
    private val srsIntervalPreview: SrsIntervalPreview,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val updateWordUseCase: UpdateWordUseCase,
    private val updateGrammarUseCase: UpdateGrammarUseCase,
    private val settingsRepository: SettingsRepository,
    private val studyRecordRepository: StudyRecordRepository,
    private val reviewLogRepository: ReviewLogRepository,
    private val sessionState: LearningSessionState,
    private val stateUpdater: StateUpdater,
    private val scope: CoroutineScope,
    private val undoDelegate: UndoDelegate,
    private val statsTimerDelegate: StatsTimerDelegate,
    private val queueNavigationDelegate: QueueNavigationDelegate,
    private val cardOpsDelegate: CardOpsDelegate,
) {
    private var ratingJob
        get() = sessionState.ratingJob
        set(value) { sessionState.ratingJob = value }

    /** 评分入口（统一处理 Word 和 Grammar，原 VM.rate） */
    fun rate(quality: Int) {
        // 防抖
        if (stateUpdater.currentState.status == LearningStatus.Processing) return

        ratingJob?.cancel()
        ratingJob = scope.launch {
            // 保存撤销快照
            undoDelegate.saveUndoSnapshot()

            stateUpdater.update { it.copy(status = LearningStatus.Processing) }

            try {
                // 1. 处理用户跳过卡片的自动顺延 (0 until currentIndex 的卡片移至末尾)
                val state = stateUpdater.currentState
                val mode = state.learningMode
                var currentItem: LearningItem? = null

                if (mode == LearningMode.Word) {
                    val (rearrangedList, newIndex) = learningQueueManager.handleSkippedItemsOnRating(state.wordList, state.currentIndex)
                    if (newIndex != state.currentIndex || rearrangedList != state.wordList) {
                        stateUpdater.update {
                            it.copy(
                                wordList = rearrangedList,
                                currentIndex = newIndex,
                                currentWord = rearrangedList.getOrNull(newIndex)
                            )
                        }
                    }
                    val word = rearrangedList.getOrNull(newIndex) ?: run {
                        stateUpdater.update { it.copy(status = LearningStatus.Learning) }
                        return@launch
                    }
                    currentItem = LearningItem.WordItem(
                        word = word,
                        step = sessionState.learningSteps[word.id] ?: 0,
                        dueTime = sessionState.learningDueTimes[word.id] ?: 0L
                    )
                } else {
                    val (rearrangedList, newIndex) = learningQueueManager.handleSkippedItemsOnRating(state.grammarList, state.currentGrammarIndex)
                    if (newIndex != state.currentGrammarIndex || rearrangedList != state.grammarList) {
                        stateUpdater.update {
                            it.copy(
                                grammarList = rearrangedList,
                                currentGrammarIndex = newIndex,
                                currentGrammar = rearrangedList.getOrNull(newIndex)
                            )
                        }
                    }
                    val grammar = rearrangedList.getOrNull(newIndex) ?: run {
                        stateUpdater.update { it.copy(status = LearningStatus.Learning) }
                        return@launch
                    }
                    currentItem = LearningItem.GrammarItem(
                        grammar = grammar,
                        step = sessionState.learningSteps[grammar.id] ?: 0,
                        dueTime = sessionState.learningDueTimes[grammar.id] ?: 0L
                    )
                }

                // 委托给 StatsManager 进行计数统计与实时持久化
                val today = sessionState.lockedDayOrToday()
                statsTimerDelegate.onItemRated(stateUpdater.currentState.learningMode, quality, currentItem!!.cardBadge, currentItem.id, today)

                val isNew = currentItem.isNew
                val currentStep = sessionState.learningSteps[currentItem.id]
                val isLearning = isNew || currentStep != null

                when {
                    // 熟词速通 (评分 = 5)
                    quality == 5 -> {
                        println("熟词速通: ${currentItem.displayName}")
                        sessionState.learningSteps.remove(currentItem.id)
                        processSrsUpdate(currentItem, quality, isLapse = false)
                    }

                    // 忘记 (评分 < 3)
                    quality < 3 -> {
                        // 1. 先应用惩罚 (更新数据库中的 Interval/EF)，确保下次毕业时基于惩罚后的值计算
                        val penalizedItem = applyLapsePenalty(currentItem, quality) ?: currentItem

                        // 2. 调度失败流程 (进入 Learning Queue)
                        val currentLapseCount = sessionState.lapseCounts.value[currentItem.id] ?: 0

                        // 新卡 Again 应该使用学习步长，复习卡 Again 使用重学步长
                        val config = if (currentItem.isNew) sessionState.learningStepsConfig else sessionState.relearningStepsConfig

                        val result = learningScheduler.scheduleFailure(
                            penalizedItem, // 使用更新后的 item
                            currentLapseCount,
                            config,
                            sessionState.leechThreshold
                        )
                        handleScheduleResult(result)
                    }

                    // 掌握 (评分 3-4)
                    else -> {
                        if (isLearning) {
                            // Use Relearning Config if it's NOT a New Card (i.e. has Repetitions)
                            val config = if (isNew) sessionState.learningStepsConfig else sessionState.relearningStepsConfig
                            val result = learningScheduler.schedulePass(
                                currentItem,
                                quality,
                                currentStep ?: 0,
                                config
                            )
                            handleScheduleResult(result)
                        } else {
                            processSrsUpdate(currentItem, quality, isLapse = false)
                        }
                    }
                }

                // 评分防抖
                delay(DebounceConstants.RATING_DEBOUNCE_MS)

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                println("评分异常: ${e.message}")
                stateUpdater.update {
                    it.copy(
                        status = LearningStatus.Error,
                        error = "评分异常: ${e.message}"
                    )
                }
            }
        }
    }

    /** 应用失败惩罚 (Lapse Penalty，纯内存计算，原 VM.applyLapsePenalty) */
    private fun applyLapsePenalty(item: LearningItem, quality: Int): LearningItem {
        val today = sessionState.lockedDayOrToday()

        return when (item) {
            is LearningItem.WordItem -> {
                val srsResult = srsCalculator.calculate(item.word, quality, today)
                val updatedWord = item.word.copy(
                    type = 3, // 🎯 标记为重学 (Relearn)
                    interval = srsResult.interval,
                    repetitionCount = srsResult.repetitionCount,
                    stability = srsResult.stability,
                    difficulty = srsResult.difficulty,
                    nextReviewDate = srsResult.nextReviewDate,
                    lastReviewedDate = srsResult.lastReviewedDate,
                    firstLearnedDate = srsResult.firstLearnedDate,
                    lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                )
                item.copy(word = updatedWord)
            }
            is LearningItem.GrammarItem -> {
                val srsResult = srsCalculator.calculate(item.grammar, quality, today)
                val updatedGrammar = item.grammar.copy(
                    type = 3, // 🎯 标记为重学 (Relearn)
                    interval = srsResult.interval,
                    repetitionCount = srsResult.repetitionCount,
                    stability = srsResult.stability,
                    difficulty = srsResult.difficulty,
                    nextReviewDate = srsResult.nextReviewDate,
                    lastReviewedDate = srsResult.lastReviewedDate,
                    firstLearnedDate = srsResult.firstLearnedDate,
                    lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                )
                item.copy(grammar = updatedGrammar)
            }
        }
    }

    /** 处理重学毕业 (Re-learning Graduation，原 VM.processRelearningGraduation) */
    private fun processRelearningGraduation(item: LearningItem) {
        val today = sessionState.lockedDayOrToday()
        val isNew = item.isNew

        val updatedItem = when (item) {
            is LearningItem.WordItem -> {
                val word = item.word
                val interval = word.interval.coerceAtLeast(1)
                val updatedWord = word.copy(
                    type = 2, // 🎯 毕业进入复习 (Review)
                    interval = interval,
                    repetitionCount = word.repetitionCount + 1,
                    lastReviewedDate = today,
                    nextReviewDate = today + interval,
                    lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                )
                println("重学毕业: id=${updatedWord.id}, stability=${updatedWord.stability}, next_interval=${updatedWord.interval}d")
                item.copy(word = updatedWord)
            }
            is LearningItem.GrammarItem -> {
                val grammar = item.grammar
                val interval = grammar.interval.coerceAtLeast(1)
                val updatedGrammar = grammar.copy(
                    type = 2, // 🎯 毕业进入复习 (Review)
                    interval = interval,
                    repetitionCount = grammar.repetitionCount + 1,
                    lastReviewedDate = today,
                    nextReviewDate = today + interval,
                    lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                )
                println("重学毕业: id=${updatedGrammar.id}, stability=${updatedGrammar.stability}, next_interval=${updatedGrammar.interval}d")
                item.copy(grammar = updatedGrammar)
            }
        }

        processGraduation(updatedItem, isNew = isNew, isLapse = false, quality = 3)
    }

    /** 统一毕业处理逻辑（F14 / US-14 抽取公共方法） */
    private fun processGraduation(
        updatedItem: LearningItem,
        isNew: Boolean,
        isLapse: Boolean,
        quality: Int
    ) {
        // 🎯 乐观更新 UI：立即计次并切卡
        stateUpdater.update {
            it.copy(
                completedThisSession = it.completedThisSession + 1,
                completedToday = if (isNew) it.completedToday + 1 else it.completedToday,
                sessionProcessedCount = it.sessionProcessedCount + 1
            )
        }
        queueNavigationDelegate.removeCurrentAndMoveNext()

        // 🎯 异步安全持久化写入数据库
        persistRatingItemAsync(
            updatedItem,
            isGraduation = true,
            isNew = isNew,
            isLapse = isLapse,
            quality = quality
        )
    }

    /** 处理调度结果（Leech/Graduate/Requeue 三分支，原 VM.handleScheduleResult） */
    private suspend fun handleScheduleResult(result: ScheduleResult) {
        when (result) {
            is ScheduleResult.Leech -> cardOpsDelegate.handleLeech(result.item, result.totalLapses)
            is ScheduleResult.Graduate -> {
                println("毕业 (Graduated): ${result.item.displayName}")
                sessionState.learningSteps.remove(result.item.id)

                // 区分: 新卡毕业 vs 重学毕业 (Re-learning Graduation)
                if (!result.item.isNew && result.quality < 5) {
                    processRelearningGraduation(result.item)
                } else {
                    processSrsUpdate(result.item, result.quality, isLapse = false)
                }
            }
            is ScheduleResult.Requeue -> {
                val item = result.updatedItem
                sessionState.learningSteps[item.id] = result.nextStepIndex

                val updatedItem = when (stateUpdater.currentState.learningMode) {
                    LearningMode.Word -> {
                        if (item is LearningItem.WordItem) {
                            val updatedWord = item.word.copy(type = item.type)
                            item.copy(word = updatedWord)
                        } else item
                    }
                    LearningMode.Grammar -> {
                        if (item is LearningItem.GrammarItem) {
                            val updatedGrammar = item.grammar.copy(type = item.type)
                            item.copy(grammar = updatedGrammar)
                        } else item
                    }
                }

                if (result.isLapse) {
                    println("失败 (Again): ${updatedItem.displayName}, Step ${result.nextStepIndex}, 到期时间: ${result.dueTime}")
                } else {
                    if (result.nextStepIndex == sessionState.learningSteps[updatedItem.id]) {
                        println("困难 (Hard): ${updatedItem.displayName}, Keep Step ${result.nextStepIndex}, 到期时间: ${result.dueTime}")
                    } else {
                        println("进阶 (Good): ${updatedItem.displayName} (Step -> ${result.nextStepIndex}), 到期时间: ${result.dueTime}")
                    }
                }

                // 🎯 乐观更新 UI：立即累加处理步数并重新排队切词
                stateUpdater.update {
                    it.copy(sessionProcessedCount = it.sessionProcessedCount + 1)
                }
                queueNavigationDelegate.reQueueAtRelativeOffset(updatedItem)

                // 🎯 异步安全持久化写入数据库
                persistRatingItemAsync(
                    updatedItem,
                    isGraduation = false,
                    isNew = updatedItem.isNew,
                    isLapse = result.isLapse,
                    quality = 1
                )
            }
        }
    }

    /** SRS 更新（乐观切卡与异步持久化，原 VM.processSrsUpdate） */
    private fun processSrsUpdate(item: LearningItem, quality: Int, isLapse: Boolean) {
        val today = sessionState.lockedDayOrToday()
        val isNew = item.isNew

        val updatedItem = when (item) {
            is LearningItem.WordItem -> {
                val word = item.word
                val srsResult = srsCalculator.calculate(word, quality, today)
                val updatedWord = word.copy(
                    type = 2, // 🎯 毕业进入复习 (Review)
                    interval = srsResult.interval,
                    repetitionCount = srsResult.repetitionCount,
                    stability = srsResult.stability,
                    difficulty = srsResult.difficulty,
                    nextReviewDate = srsResult.nextReviewDate,
                    lastReviewedDate = srsResult.lastReviewedDate,
                    firstLearnedDate = srsResult.firstLearnedDate,
                    lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                )
                println("更新单词: id=${updatedWord.id}, rep=${updatedWord.repetitionCount}, interval=${updatedWord.interval}d")
                item.copy(word = updatedWord)
            }

            is LearningItem.GrammarItem -> {
                val grammar = item.grammar
                val srsResult = srsCalculator.calculate(grammar, quality, today)
                val updatedGrammar = grammar.copy(
                    type = 2, // 🎯 毕业进入复习 (Review)
                    interval = srsResult.interval,
                    repetitionCount = srsResult.repetitionCount,
                    stability = srsResult.stability,
                    difficulty = srsResult.difficulty,
                    nextReviewDate = srsResult.nextReviewDate,
                    lastReviewedDate = srsResult.lastReviewedDate,
                    firstLearnedDate = srsResult.firstLearnedDate,
                    lastModifiedTime = DateTimeUtils.getCurrentCompensatedMillis()
                )
                println("更新语法: id=${updatedGrammar.id}, rep=${updatedGrammar.repetitionCount}, interval=${updatedGrammar.interval}d")
                item.copy(grammar = updatedGrammar)
            }
        }

        processGraduation(updatedItem, isNew = isNew, isLapse = isLapse, quality = quality)
    }

    /** 后台异步安全持久化评分结果与学习统计（原 VM.persistRatingItemAsync） */
    private fun persistRatingItemAsync(
        updatedItem: LearningItem,
        isGraduation: Boolean,
        isNew: Boolean,
        isLapse: Boolean,
        quality: Int
    ) {
        scope.launch(Dispatchers.IO) {
            val today = sessionState.lockedDayOrToday()
            try {
                when (updatedItem) {
                    is LearningItem.WordItem -> {
                        val word = updatedItem.word
                        val result = updateWordUseCase(word)
                        if (result is Result.Error) {
                            println("后台保存单词失败: ${result.exception.message}")
                            return@launch
                        }

                        if (isLapse) {
                            settingsRepository.incrementWordLapse(word.id)
                        }

                        if (isGraduation) {
                            if (isNew) studyRecordRepository.incrementLearnedWords(1)
                            else studyRecordRepository.incrementReviewedWords(1)

                            if (!isNew) {
                                settingsRepository.addTodayTestedWordId(word.id)
                                if (quality <= 2) {
                                    settingsRepository.addTodayWrongWordId(word.id)
                                }
                                try {
                                    val lastDate = word.lastReviewedDate
                                    val actualInterval = if (lastDate != null && lastDate > 0L) {
                                        (today - lastDate).toInt()
                                    } else {
                                        0
                                    }
                                    reviewLogRepository.insertLog(
                                        com.jian.nemo.core.domain.model.ReviewLog(
                                            itemId = word.id,
                                            itemType = "word",
                                            reviewDate = DateTimeUtils.getCurrentCompensatedMillis(),
                                            intervalDays = actualInterval,
                                            rating = quality
                                        )
                                    )
                                } catch (e: Exception) {
                                    println("记录复习日志失败: ${e.message}")
                                }
                                srsCalculator.notifyReviewCompleted()
                            }
                            statsTimerDelegate.syncTodayCounts()
                        }
                    }

                    is LearningItem.GrammarItem -> {
                        val grammar = updatedItem.grammar
                        val result = updateGrammarUseCase(grammar)
                        if (result is Result.Error) {
                            println("后台保存语法失败: ${result.exception.message}")
                            return@launch
                        }

                        if (isLapse) {
                            settingsRepository.incrementGrammarLapse(grammar.id)
                        }

                        if (isGraduation) {
                            if (isNew) studyRecordRepository.incrementLearnedGrammars(1)
                            else studyRecordRepository.incrementReviewedGrammars(1)

                            if (!isNew) {
                                settingsRepository.addTodayTestedGrammarId(grammar.id)
                                if (quality <= 2) {
                                    settingsRepository.addTodayWrongGrammarId(grammar.id)
                                }
                                try {
                                    val lastDate = grammar.lastReviewedDate
                                    val actualInterval = if (lastDate != null && lastDate > 0L) {
                                        (today - lastDate).toInt()
                                    } else {
                                        0
                                    }
                                    reviewLogRepository.insertLog(
                                        com.jian.nemo.core.domain.model.ReviewLog(
                                            itemId = grammar.id,
                                            itemType = "grammar",
                                            reviewDate = DateTimeUtils.getCurrentCompensatedMillis(),
                                            intervalDays = actualInterval,
                                            rating = quality
                                        )
                                    )
                                } catch (e: Exception) {
                                    println("记录复习日志失败: ${e.message}")
                                }
                                srsCalculator.notifyReviewCompleted()
                            }
                            statsTimerDelegate.syncTodayCounts()
                        }
                    }
                }
            } catch (e: Exception) {
                println("后台持久化评分失败: ${e.message}")
            }
        }
    }
}
