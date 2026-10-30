package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.data.manager.CloudBackupManager
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.usecase.grammar.GetTodayLearnedGrammarsCountUseCase
import com.jian.nemo.core.domain.usecase.word.GetTodayLearnedWordsCountUseCase
import com.jian.nemo.feature.learning.domain.LearningSessionStatsManager
import com.jian.nemo.feature.learning.domain.LearningTtsManager
import com.jian.nemo.feature.learning.presentation.CardBadge
import com.jian.nemo.feature.learning.presentation.LearningMode
import com.jian.nemo.feature.learning.presentation.LearningStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 计时与会话结算域 delegate (系统设计 §3.2.M8 / US-7 / F7)
 *
 * 承接原 LearningViewModel：
 * - completeSession / syncTodayCounts / pauseTimer / resumeTimer / checkAndRestoreOrReset
 * - showAnswer（含冷却检查 + 自动朗读）
 * - toggleGrammarDetail
 *
 * 持有 LearningSessionStatsManager 实例（与 ViewModel 共享同一实例）。
 */
class StatsTimerDelegate(
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val getTodayLearnedWordsCountUseCase: GetTodayLearnedWordsCountUseCase,
    private val getTodayLearnedGrammarsCountUseCase: GetTodayLearnedGrammarsCountUseCase,
    private val cloudBackupManager: CloudBackupManager,
    private val learningTtsManager: LearningTtsManager,
    private val sessionState: LearningSessionState,
    private val stateUpdater: StateUpdater,
    private val scope: CoroutineScope,
    private val undoDelegate: UndoDelegate,
    private val queueNavigationDelegate: QueueNavigationDelegate,
    /** 与 ViewModel 共享同一 StatsManager 实例（保证统计状态一致） */
    private val sessionStatsManager: LearningSessionStatsManager,
) {

    // ===== 暴露给其他 delegate 的只读访问器 =====
    val sessionStartTime: Long get() = sessionStatsManager.sessionStartTime
    val maxCombo: Int get() = sessionStatsManager.maxCombo
    val sessionNewCount: Int get() = sessionStatsManager.sessionNewCount
    val sessionReviewCount: Int get() = sessionStatsManager.sessionReviewCount
    val sessionRelearnCount: Int get() = sessionStatsManager.sessionRelearnCount

    /** 评分时更新统计（被 RatingDelegate 调用） */
    fun onItemRated(mode: LearningMode, quality: Int, badge: CardBadge, itemId: Int, today: Long) {
        sessionStatsManager.onItemRated(mode, quality, badge, itemId, today)
    }

    /** 会话完成统计（仅返回时长，供 SessionLifecycleDelegate.Completed 分支调用） */
    fun completeSessionStats(mode: LearningMode, today: Long): Long =
        sessionStatsManager.completeSession(mode, today)

    /** 触发即时云端备份（供 SessionLifecycleDelegate.Completed 分支调用） */
    fun triggerCloudBackup() {
        scope.launch(Dispatchers.IO) {
            cloudBackupManager.performImmediateAutoBackup()
        }
    }

    /** 恢复计时器原始调用（供 SessionLifecycleDelegate 在加载会话时调用，无守卫） */
    fun resumeStatsTimer() {
        sessionStatsManager.resumeTimer()
    }

    /** 清空会话计时（退出学习时调用） */
    fun clearSessionTimer() {
        sessionStatsManager.clearSessionTimer()
    }

    // ===== 事件入口方法 =====

    /** 完成会话（原 VM.completeSession） */
    fun completeSession() {
        val today = sessionState.lockedDayOrToday()
        val totalDuration = sessionStatsManager.completeSession(stateUpdater.currentState.learningMode, today)

        stateUpdater.update {
            it.copy(
                status = LearningStatus.SessionCompleted,
                currentWord = null,
                currentGrammar = null,
                wordList = emptyList(),
                grammarList = emptyList(),
                currentIndex = 0,
                currentGrammarIndex = 0,
                showAnswerAvailableAt = 0L,
                waitingUntil = 0L,
                sessionDurationSeconds = totalDuration
            )
        }

        queueNavigationDelegate.clearSession()
        undoDelegate.syncUndoAvailability()
        syncTodayCounts()

        // 触发即时自动备份
        scope.launch(Dispatchers.IO) {
            cloudBackupManager.performImmediateAutoBackup()
        }

        println("会话完成！")
    }

    /** 从数据库安全同步今日去重的新学数与复习数（原 VM.syncTodayCounts） */
    fun syncTodayCounts() {
        scope.launch(Dispatchers.IO) {
            val today = sessionState.lockedDayOrToday()
            val isWordMode = stateUpdater.currentState.learningMode == LearningMode.Word

            val newCount = try {
                if (isWordMode) {
                    getTodayLearnedWordsCountUseCase()
                        .first { it !is Result.Loading }
                        .let { if (it is Result.Success) it.data else 0 }
                } else {
                    getTodayLearnedGrammarsCountUseCase()
                        .first { it !is Result.Loading }
                        .let { if (it is Result.Success) it.data else 0 }
                }
            } catch (e: Exception) {
                sessionStatsManager.sessionNewCount
            }

            val reviewCount = try {
                if (isWordMode) {
                    wordRepository.getTodayReviewedWords(today).first().size
                } else {
                    grammarRepository.getTodayReviewedGrammars(today).first().size
                }
            } catch (e: Exception) {
                sessionStatsManager.sessionReviewCount
            }

            stateUpdater.update {
                it.copy(
                    sessionNewCount = newCount,
                    sessionReviewCount = reviewCount,
                    sessionRelearnCount = sessionStatsManager.sessionRelearnCount,
                    sessionMaxCombo = sessionStatsManager.maxCombo
                )
            }
        }
    }

    /** 暂停学习计时器（原 VM.pauseTimer） */
    fun pauseTimer() {
        val today = sessionState.lockedDayOrToday()
        sessionStatsManager.pauseTimer(stateUpdater.currentState.learningMode, today)
    }

    /** 恢复学习计时器（事件入口，含守卫；原 VM.resumeTimer） */
    fun resumeTimer() {
        // 仅在被暂停了，且当前页面处于学习状态时才恢复
        if (sessionStatsManager.sessionStartTime == 0L && stateUpdater.currentState.status == LearningStatus.Learning) {
            checkAndRestoreOrReset()
            sessionStatsManager.resumeTimer()
        }
    }

    /** 检查当前学习日天数，执行今日学习时间持久化恢复或跨天重置清零（原 VM.checkAndRestoreOrReset） */
    fun checkAndRestoreOrReset() {
        val today = sessionState.lockedDayOrToday()
        sessionStatsManager.checkAndRestoreOrReset(stateUpdater.currentState.learningMode, today)
    }

    /** 显示答案（含冷却检查；原 VM.showAnswer） */
    fun showAnswer() {
        if (stateUpdater.currentState.status == LearningStatus.Processing) return

        val state = stateUpdater.currentState
        if (state.showAnswerAvailableAt > 0L && System.currentTimeMillis() + DebounceConstants.SHOW_ANSWER_GRACE_MS < state.showAnswerAvailableAt) {
            return
        }

        stateUpdater.update { it.copy(isAnswerShown = true, isCardFlipped = true, showAnswerAvailableAt = 0L) }

        // 自动朗读逻辑
        if (stateUpdater.currentState.isAutoAudioEnabled) {
            val cur = stateUpdater.currentState
            // 仅在单词模式下自动朗读
            if (cur.learningMode == LearningMode.Word) {
                cur.currentWord?.let { word ->
                    learningTtsManager.speakWord(word.hiragana, word.chinese)
                }
            }
        }

        // 保存当前会话状态（主要是为了记录 isAnswerShown = true）
        queueNavigationDelegate.saveCurrentSessionState()
    }

    /** 切换语法详情可见性（原 VM.toggleGrammarDetail） */
    fun toggleGrammarDetail() {
        if (stateUpdater.currentState.status != LearningStatus.Learning) return
        stateUpdater.update { it.copy(isGrammarDetailVisible = !it.isGrammarDetailVisible) }
    }
}
