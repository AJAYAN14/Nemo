package com.jian.nemo.feature.learning.presentation.delegate

import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.feature.learning.presentation.LearningItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 卡片操作域 delegate (系统设计 §3.2.M7 / US-6 / F6)
 *
 * 承接原 LearningViewModel：
 * - handleLeech（bury_today / skip 二分支，被 RatingDelegate.handleScheduleResult.Leech 调用）
 * - suspendCurrentItem（用户手动暂停）
 * - buryCurrentItem（今日暂缓）
 * - suspendItem（执行暂停逻辑）
 *
 * 第二阶段合并 X5（buryCurrentItem / handleLeech bury_today 逻辑重复）。
 */
class CardOpsDelegate(
    private val updateWordUseCase: UpdateWordUseCase,
    private val updateGrammarUseCase: UpdateGrammarUseCase,
    private val sessionState: LearningSessionState,
    private val stateUpdater: StateUpdater,
    private val scope: CoroutineScope,
    private val queueNavigationDelegate: QueueNavigationDelegate,
) {

    /** 处理钉子户（Leech，原 VM.handleLeech） */
    suspend fun handleLeech(item: LearningItem, totalLapses: Int) {
        println("钉子户 (Leech): ${item.displayName} (Lapses: $totalLapses, Action: ${sessionState.leechAction})")

        if (sessionState.leechAction == "bury_today") {
            buryItem(item, "已埋藏顽固卡片: ${item.displayName} (今日不再出现，明日自动回归)")
        } else {
            // 提示用户
            stateUpdater.update {
                it.copy(error = "已冻结顽固卡片: ${item.displayName} (累计遗忘 $totalLapses 次，已移出复习)")
            }

            // 执行暂停逻辑
            suspendItem(item)
        }
    }

    /** 暂停当前项（用户手动，原 VM.suspendCurrentItem） */
    fun suspendCurrentItem() {
        val currentItem = stateUpdater.currentState.currentLearningItem() ?: return
        println("手动暂停: ${currentItem.displayName}")

        scope.launch {
            suspendItem(currentItem)

            // 提示
            stateUpdater.update {
                it.copy(error = "已暂停: ${currentItem.displayName}")
            }
        }
    }

    /** 今日暂缓此项（Bury，原 VM.buryCurrentItem，F15/US-15） */
    fun buryCurrentItem() {
        val currentItem = stateUpdater.currentState.currentLearningItem() ?: return
        println("今日暂缓 (Bury): ${currentItem.displayName}")

        scope.launch {
            buryItem(currentItem, "已暂缓: ${currentItem.displayName} (今日不再出现)")
        }
    }

    /** 公共卡片暂缓/埋藏实现（F15 / US-15 抽取公共方法） */
    private suspend fun buryItem(item: LearningItem, message: String) {
        // 1. 持久化暂停状态 (buriedUntilDay)
        val today = sessionState.lockedDayOrToday()
        try {
            when (item) {
                is LearningItem.WordItem -> {
                    val updatedWord = item.word.copy(buriedUntilDay = today)
                    updateWordUseCase(updatedWord)
                    println("[持久化] 单词已埋藏/暂缓: ${item.displayName} (Until: $today)")
                }
                is LearningItem.GrammarItem -> {
                    val updatedGrammar = item.grammar.copy(buriedUntilDay = today)
                    updateGrammarUseCase(updatedGrammar)
                    println("[持久化] 语法已埋藏/暂缓: ${item.displayName} (Until: $today)")
                }
            }
        } catch (e: Exception) {
            println("[持久化] 埋藏/暂缓失败: ${e.message}")
        }

        // 2. 从各类内存状态中移除
        sessionState.learningSteps.remove(item.id)
        sessionState.learningDueTimes.remove(item.id)

        // 3. 提示
        stateUpdater.update {
            it.copy(error = message)
        }

        // 4. 移出队列并前进
        queueNavigationDelegate.removeCurrentAndMoveNext()
    }

    /** 执行暂停逻辑（原 VM.suspendItem） */
    private suspend fun suspendItem(item: LearningItem) {
        // 1. 持久化暂停状态
        try {
            when (item) {
                is LearningItem.WordItem -> {
                    val updatedWord = item.word.copy(isSkipped = true)
                    updateWordUseCase(updatedWord)
                    println("[持久化] 单词已暂停: ${item.displayName}")
                }
                is LearningItem.GrammarItem -> {
                    val updatedGrammar = item.grammar.copy(isSkipped = true)
                    updateGrammarUseCase(updatedGrammar)
                    println("[持久化] 语法已暂停: ${item.displayName}")
                }
            }
        } catch (e: Exception) {
            println("[持久化] 暂停失败: ${e.message}")
            // 即使持久化失败，也继续在内存中移除，避免阻塞用户
        }

        // 2. 从各类状态中移除
        sessionState.learningSteps.remove(item.id)
        sessionState.learningDueTimes.remove(item.id)

        // 3. 移出队列并前进
        queueNavigationDelegate.removeCurrentAndMoveNext()
    }
}
