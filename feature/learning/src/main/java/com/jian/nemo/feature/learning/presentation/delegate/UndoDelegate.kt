package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.feature.learning.domain.LearningUndoHelper
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import com.jian.nemo.feature.learning.domain.UndoSnapshot
import com.jian.nemo.feature.learning.presentation.LearningItem
import com.jian.nemo.feature.learning.presentation.LearningStatus
import com.jian.nemo.feature.learning.presentation.SlideDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 撤销管理域 delegate (系统设计 §3.2.M6 / US-5 / F5)
 *
 * 承接原 LearningViewModel：
 * - saveUndoSnapshot：评分前保存快照（被 RatingDelegate 调用）
 * - syncUndoAvailability：同步 canUndo 状态
 * - undo：恢复上一张卡片（数据库 + 内存 Map + UI 状态）
 *
 * 单步撤销（保留最后一次快照）。
 */
class UndoDelegate(
    private val learningUndoHelper: LearningUndoHelper,
    private val updateWordUseCase: UpdateWordUseCase,
    private val updateGrammarUseCase: UpdateGrammarUseCase,
    private val studyRecordRepository: StudyRecordRepository,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val srsIntervalPreview: SrsIntervalPreview,
    private val sessionState: LearningSessionState,
    private val stateUpdater: StateUpdater,
    private val scope: CoroutineScope,
    private val queueNavigationDelegate: QueueNavigationDelegate,
) {

    /** 保存评分前快照（原 VM.saveUndoSnapshot） */
    fun saveUndoSnapshot() {
        val state = stateUpdater.currentState
        val currentItem = state.currentLearningItem() ?: return

        val snapshot = UndoSnapshot(
            item = currentItem,
            originalWord = (currentItem as? LearningItem.WordItem)?.word,
            originalGrammar = (currentItem as? LearningItem.GrammarItem)?.grammar,
            wordList = state.wordList.toList(),
            grammarList = state.grammarList.toList(),
            currentIndex = state.currentIndex,
            currentGrammarIndex = state.currentGrammarIndex,
            learningSteps = sessionState.learningSteps.toMap(),
            lapseCounts = sessionState.lapseCounts.value.toMap(),
            requeuedItems = sessionState.requeuedItems.toSet(),
            learningDueTimes = sessionState.learningDueTimes.toMap(),
            completedToday = state.completedToday,
            completedThisSession = state.completedThisSession,
            sessionProcessedCount = state.sessionProcessedCount,
            wasNew = currentItem.isNew
        )

        learningUndoHelper.saveSnapshot(snapshot)
        syncUndoAvailability()

        println("快照已保存: ${currentItem.displayName}")
    }

    /** 同步撤销可用性（单一真相源: LearningUndoHelper） */
    fun syncUndoAvailability() {
        val canUndo = learningUndoHelper.canUndo()
        stateUpdater.update { it.copy(canUndo = canUndo) }
    }

    /** 清空撤销快照（供 SessionLifecycleDelegate 在 startLearning/exitLearning 调用） */
    fun clearSnapshots() {
        learningUndoHelper.clear()
        syncUndoAvailability()
    }

    /** 撤销上一次评分（原 VM.undo） */
    fun undo() {
        if (stateUpdater.currentState.status == LearningStatus.Processing) return

        val snapshot = learningUndoHelper.popSnapshot() ?: run {
            syncUndoAvailability()
            return
        }
        // 快照已被消费，立即刷新可撤销状态，避免快速重复点击。
        syncUndoAvailability()

        scope.launch {
            try {
                stateUpdater.update { it.copy(status = LearningStatus.Processing) }

                // 恢复数据库状态
                when (snapshot.item) {
                    is LearningItem.WordItem -> {
                        snapshot.originalWord?.let { updateWordUseCase(it) }
                        if (snapshot.wasNew) {
                            studyRecordRepository.incrementLearnedWords(-1)
                        } else {
                            studyRecordRepository.incrementReviewedWords(-1)
                        }
                    }
                    is LearningItem.GrammarItem -> {
                        snapshot.originalGrammar?.let { updateGrammarUseCase(it) }
                        if (snapshot.wasNew) {
                            studyRecordRepository.incrementLearnedGrammars(-1)
                        } else {
                            studyRecordRepository.incrementReviewedGrammars(-1)
                        }
                    }
                }

                // 恢复 delegate 内部状态
                sessionState.learningSteps.clear()
                sessionState.learningSteps.putAll(snapshot.learningSteps)
                sessionState.lapseCounts.value = snapshot.lapseCounts
                sessionState.requeuedItems.clear()
                sessionState.requeuedItems.addAll(snapshot.requeuedItems)
                sessionState.learningDueTimes.clear()
                sessionState.learningDueTimes.putAll(snapshot.learningDueTimes)

                // 恢复 UI 状态
                @Suppress("UNUSED_VARIABLE")
                val mode = stateUpdater.currentState.learningMode
                stateUpdater.update {
                    it.copy(
                        status = LearningStatus.Learning,
                        wordList = snapshot.wordList,
                        grammarList = snapshot.grammarList,
                        currentIndex = snapshot.currentIndex,
                        currentGrammarIndex = snapshot.currentGrammarIndex,
                        currentWord = snapshot.originalWord,
                        currentGrammar = snapshot.originalGrammar,
                        completedToday = snapshot.completedToday,
                        completedThisSession = snapshot.completedThisSession,
                        sessionProcessedCount = snapshot.sessionProcessedCount,
                        isAnswerShown = true,  // 撤销后显示答案面
                        isCardFlipped = true,
                        ratingIntervals = calculateRatingIntervals(snapshot.item, srsIntervalPreview, sessionState),
                        slideDirection = SlideDirection.BACKWARD
                    )
                }
                queueNavigationDelegate.updateQueueBadgeCounts()
                syncUndoAvailability()

                // 保存会话状态
                queueNavigationDelegate.saveCurrentSessionState()

                println("撤销成功: ${snapshot.item.displayName}")

            } catch (e: Exception) {
                println("撤销失败: ${e.message}")
                stateUpdater.update {
                    it.copy(
                        status = LearningStatus.Learning,
                        error = "撤销失败: ${e.message}"
                    )
                }
                syncUndoAvailability()
            }
        }
    }
}
