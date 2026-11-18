package com.jian.nemo.feature.learning.domain

import android.content.Context
import com.jian.nemo.core.common.di.ApplicationScope
import com.jian.nemo.core.common.network.Dispatcher
import com.jian.nemo.core.common.network.NemoDispatchers
import com.jian.nemo.core.domain.model.LearningMode
import com.jian.nemo.feature.learning.data.preferences.LearningSessionStatsStorage
import com.jian.nemo.feature.learning.data.preferences.SharedPreferencesLearningSessionStatsStorage
import com.jian.nemo.feature.learning.presentation.CardBadge
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 战报统计快照数据结构
 */
data class SessionStats(
    val elapsedTime: Long,
    val maxCombo: Int,
    val newCount: Int,
    val reviewCount: Int,
    val relearnCount: Int
)

/**
 * 学习会话统计与本地持久化逻辑管理器
 */
@Singleton
class LearningSessionStatsManager @Inject constructor(
    private val storage: LearningSessionStatsStorage,
    @ApplicationScope private val appScope: CoroutineScope,
    @Dispatcher(NemoDispatchers.IO) private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor(context: Context) : this(
        storage = SharedPreferencesLearningSessionStatsStorage(context),
        appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        ioDispatcher = Dispatchers.IO
    )

    private val stateLock = Any()

    @Volatile
    var isHydrated: Boolean = false
        private set

    @Volatile
    private var lastSavedDay: Long = 0L

    private val relearnWordIds = mutableSetOf<Int>()
    private val relearnGrammarIds = mutableSetOf<Int>()

    @Volatile
    var sessionStartTime: Long = 0L
        private set

    @Volatile
    var wordElapsedTime: Long = 0L
        private set

    @Volatile
    var grammarElapsedTime: Long = 0L
        private set

    @Volatile
    var wordCurrentCombo: Int = 0
        private set

    @Volatile
    var wordMaxCombo: Int = 0
        private set

    @Volatile
    var wordSessionNewCount: Int = 0
        private set

    @Volatile
    var wordSessionReviewCount: Int = 0
        private set

    @Volatile
    var grammarCurrentCombo: Int = 0
        private set

    @Volatile
    var grammarMaxCombo: Int = 0
        private set

    @Volatile
    var grammarSessionNewCount: Int = 0
        private set

    @Volatile
    var grammarSessionReviewCount: Int = 0
        private set

    @Volatile
    var activeMode: LearningMode = LearningMode.Word
        private set

    val currentCombo: Int
        get() = if (activeMode == LearningMode.Word) wordCurrentCombo else grammarCurrentCombo

    val maxCombo: Int
        get() = if (activeMode == LearningMode.Word) wordMaxCombo else grammarMaxCombo

    val sessionNewCount: Int
        get() = if (activeMode == LearningMode.Word) wordSessionNewCount else grammarSessionNewCount

    val sessionReviewCount: Int
        get() = if (activeMode == LearningMode.Word) wordSessionReviewCount else grammarSessionReviewCount

    val sessionRelearnCount: Int
        get() = if (activeMode == LearningMode.Word) relearnWordIds.size else relearnGrammarIds.size

    private var debounceSaveJob: Job? = null

    /**
     * 检查今日日期，恢复本地持久化的今日战报或执行跨天重置
     */
    suspend fun checkAndRestoreOrReset(mode: LearningMode, today: Long) {
        val raw = storage.loadStats()

        synchronized(stateLock) {
            activeMode = mode
            lastSavedDay = raw.lastResetDay

            if (raw.lastResetDay != today) {
                wordElapsedTime = 0L
                grammarElapsedTime = 0L
                wordCurrentCombo = 0
                wordMaxCombo = 0
                wordSessionNewCount = 0
                wordSessionReviewCount = 0
                grammarCurrentCombo = 0
                grammarMaxCombo = 0
                grammarSessionNewCount = 0
                grammarSessionReviewCount = 0
                relearnWordIds.clear()
                relearnGrammarIds.clear()
                isHydrated = true
            } else {
                wordElapsedTime = raw.wordElapsedTime
                grammarElapsedTime = raw.grammarElapsedTime

                wordCurrentCombo = raw.wordCurrentCombo
                wordMaxCombo = raw.wordMaxCombo
                wordSessionNewCount = raw.wordSessionNewCount
                wordSessionReviewCount = raw.wordSessionReviewCount

                grammarCurrentCombo = raw.grammarCurrentCombo
                grammarMaxCombo = raw.grammarMaxCombo
                grammarSessionNewCount = raw.grammarSessionNewCount
                grammarSessionReviewCount = raw.grammarSessionReviewCount

                relearnWordIds.clear()
                if (raw.wordRelearnIds.isNotEmpty()) {
                    raw.wordRelearnIds.split(",")
                        .mapNotNull { it.toIntOrNull() }
                        .forEach { relearnWordIds.add(it) }
                }

                relearnGrammarIds.clear()
                if (raw.grammarRelearnIds.isNotEmpty()) {
                    raw.grammarRelearnIds.split(",")
                        .mapNotNull { it.toIntOrNull() }
                        .forEach { relearnGrammarIds.add(it) }
                }

                isHydrated = true
            }
        }

        if (raw.lastResetDay != today) {
            storage.resetAllStats(today)
        }
    }

    /**
     * 开始/恢复计时
     */
    fun resumeTimer() {
        synchronized(stateLock) {
            if (sessionStartTime == 0L) {
                sessionStartTime = System.currentTimeMillis()
            }
        }
    }

    /**
     * 暂停计时并将用时与当前全部战报原子冲刷持久化（防抖取消时立即保存，杜绝丢统计）
     */
    fun pauseTimer(mode: LearningMode, today: Long) {
        val totalElapsed: Long
        val currentComboSnapshot: Int
        val maxComboSnapshot: Int
        val sessionNewCountSnapshot: Int
        val sessionReviewCountSnapshot: Int
        val sessionRelearnCountSnapshot: Int
        val relearnIdsSnapshot: String

        synchronized(stateLock) {
            activeMode = mode
            debounceSaveJob?.cancel()
            debounceSaveJob = null

            if (sessionStartTime > 0L) {
                val elapsed = (System.currentTimeMillis() - sessionStartTime) / 1000
                if (mode == LearningMode.Word) {
                    wordElapsedTime += elapsed
                } else {
                    grammarElapsedTime += elapsed
                }
                sessionStartTime = 0L
            }
            totalElapsed = if (mode == LearningMode.Word) wordElapsedTime else grammarElapsedTime

            if (mode == LearningMode.Word) {
                currentComboSnapshot = wordCurrentCombo
                maxComboSnapshot = wordMaxCombo
                sessionNewCountSnapshot = wordSessionNewCount
                sessionReviewCountSnapshot = wordSessionReviewCount
                sessionRelearnCountSnapshot = relearnWordIds.size
                relearnIdsSnapshot = relearnWordIds.joinToString(",")
            } else {
                currentComboSnapshot = grammarCurrentCombo
                maxComboSnapshot = grammarMaxCombo
                sessionNewCountSnapshot = grammarSessionNewCount
                sessionReviewCountSnapshot = grammarSessionReviewCount
                sessionRelearnCountSnapshot = relearnGrammarIds.size
                relearnIdsSnapshot = relearnGrammarIds.joinToString(",")
            }
        }

        if (isHydrated) {
            appScope.launch(NonCancellable + ioDispatcher) {
                storage.saveAllStats(
                    mode = mode,
                    today = today,
                    elapsedTime = totalElapsed,
                    currentCombo = currentComboSnapshot,
                    maxCombo = maxComboSnapshot,
                    newCount = sessionNewCountSnapshot,
                    reviewCount = sessionReviewCountSnapshot,
                    relearnCount = sessionRelearnCountSnapshot,
                    relearnIds = relearnIdsSnapshot
                )
            }
        }
    }

    /**
     * 每次评分卡片时，更新战报统计并实时持久化（400ms 防抖）
     */
    fun onItemRated(mode: LearningMode, quality: Int, badge: CardBadge, itemId: Int = 0, today: Long) {
        val currentComboSnapshot: Int
        val maxComboSnapshot: Int
        val sessionNewCountSnapshot: Int
        val sessionReviewCountSnapshot: Int
        val sessionRelearnCountSnapshot: Int
        val relearnIdsSnapshot: String

        synchronized(stateLock) {
            activeMode = mode
            if (mode == LearningMode.Word) {
                when (badge) {
                    CardBadge.NEW -> wordSessionNewCount++
                    CardBadge.REVIEW -> wordSessionReviewCount++
                    CardBadge.LEARNING, CardBadge.RELEARN -> { /* 由数据库去重逻辑处理 */ }
                }

                if (badge == CardBadge.REVIEW && quality < 3 && itemId != 0) {
                    relearnWordIds.add(itemId)
                }

                if (quality >= 3) {
                    wordCurrentCombo++
                    wordMaxCombo = maxOf(wordMaxCombo, wordCurrentCombo)
                } else {
                    wordCurrentCombo = 0
                }

                currentComboSnapshot = wordCurrentCombo
                maxComboSnapshot = wordMaxCombo
                sessionNewCountSnapshot = wordSessionNewCount
                sessionReviewCountSnapshot = wordSessionReviewCount
                sessionRelearnCountSnapshot = relearnWordIds.size
                relearnIdsSnapshot = relearnWordIds.joinToString(",")
            } else {
                when (badge) {
                    CardBadge.NEW -> grammarSessionNewCount++
                    CardBadge.REVIEW -> grammarSessionReviewCount++
                    CardBadge.LEARNING, CardBadge.RELEARN -> { /* 由数据库去重逻辑处理 */ }
                }

                if (badge == CardBadge.REVIEW && quality < 3 && itemId != 0) {
                    relearnGrammarIds.add(itemId)
                }

                if (quality >= 3) {
                    grammarCurrentCombo++
                    grammarMaxCombo = maxOf(grammarMaxCombo, grammarCurrentCombo)
                } else {
                    grammarCurrentCombo = 0
                }

                currentComboSnapshot = grammarCurrentCombo
                maxComboSnapshot = grammarMaxCombo
                sessionNewCountSnapshot = grammarSessionNewCount
                sessionReviewCountSnapshot = grammarSessionReviewCount
                sessionRelearnCountSnapshot = relearnGrammarIds.size
                relearnIdsSnapshot = relearnGrammarIds.joinToString(",")
            }

            debounceSaveJob?.cancel()
            debounceSaveJob = appScope.launch(ioDispatcher) {
                delay(400)
                if (isHydrated) {
                    storage.saveModeStats(
                        mode = mode,
                        today = today,
                        currentCombo = currentComboSnapshot,
                        maxCombo = maxComboSnapshot,
                        newCount = sessionNewCountSnapshot,
                        reviewCount = sessionReviewCountSnapshot,
                        relearnCount = sessionRelearnCountSnapshot,
                        relearnIds = relearnIdsSnapshot
                    )
                }
            }
        }
    }

    /**
     * 完成会话结算，归档本次总已学时间并清空开始计时器，返回今日总时间秒数（原子落盘全部指标）
     */
    fun completeSession(mode: LearningMode, today: Long): Long {
        val totalDuration: Long
        val currentComboSnapshot: Int
        val maxComboSnapshot: Int
        val sessionNewCountSnapshot: Int
        val sessionReviewCountSnapshot: Int
        val sessionRelearnCountSnapshot: Int
        val relearnIdsSnapshot: String

        synchronized(stateLock) {
            activeMode = mode
            debounceSaveJob?.cancel()
            debounceSaveJob = null

            if (sessionStartTime > 0L) {
                val currentSessionDuration = (System.currentTimeMillis() - sessionStartTime) / 1000
                if (mode == LearningMode.Word) {
                    wordElapsedTime += currentSessionDuration
                } else {
                    grammarElapsedTime += currentSessionDuration
                }
                sessionStartTime = 0L
            }
            totalDuration = if (mode == LearningMode.Word) wordElapsedTime else grammarElapsedTime

            if (mode == LearningMode.Word) {
                currentComboSnapshot = wordCurrentCombo
                maxComboSnapshot = wordMaxCombo
                sessionNewCountSnapshot = wordSessionNewCount
                sessionReviewCountSnapshot = wordSessionReviewCount
                sessionRelearnCountSnapshot = relearnWordIds.size
                relearnIdsSnapshot = relearnWordIds.joinToString(",")
            } else {
                currentComboSnapshot = grammarCurrentCombo
                maxComboSnapshot = grammarMaxCombo
                sessionNewCountSnapshot = grammarSessionNewCount
                sessionReviewCountSnapshot = grammarSessionReviewCount
                sessionRelearnCountSnapshot = relearnGrammarIds.size
                relearnIdsSnapshot = relearnGrammarIds.joinToString(",")
            }
        }

        if (isHydrated) {
            appScope.launch(NonCancellable + ioDispatcher) {
                storage.saveAllStats(
                    mode = mode,
                    today = today,
                    elapsedTime = totalDuration,
                    currentCombo = currentComboSnapshot,
                    maxCombo = maxComboSnapshot,
                    newCount = sessionNewCountSnapshot,
                    reviewCount = sessionReviewCountSnapshot,
                    relearnCount = sessionRelearnCountSnapshot,
                    relearnIds = relearnIdsSnapshot
                )
            }
        }
        return totalDuration
    }

    /**
     * 获取当前战报的快照（严格按指定模式隔离返回，防止跨模式污染）
     */
    fun getCurrentStats(mode: LearningMode): SessionStats {
        synchronized(stateLock) {
            val currentSessionDuration = if (sessionStartTime > 0L && activeMode == mode) {
                (System.currentTimeMillis() - sessionStartTime) / 1000
            } else 0L
            val elapsed = if (mode == LearningMode.Word) {
                wordElapsedTime + currentSessionDuration
            } else {
                grammarElapsedTime + currentSessionDuration
            }
            val combo = if (mode == LearningMode.Word) wordMaxCombo else grammarMaxCombo
            val newCount = if (mode == LearningMode.Word) wordSessionNewCount else grammarSessionNewCount
            val reviewCount = if (mode == LearningMode.Word) wordSessionReviewCount else grammarSessionReviewCount
            val relearnCount = if (mode == LearningMode.Word) relearnWordIds.size else relearnGrammarIds.size
            return SessionStats(
                elapsedTime = elapsed,
                maxCombo = combo,
                newCount = newCount,
                reviewCount = reviewCount,
                relearnCount = relearnCount
            )
        }
    }

    /**
     * 重置计时变量（用于退出时仅置空本次计时，不抹掉持久化总计）
     */
    fun clearSessionTimer() {
        synchronized(stateLock) {
            sessionStartTime = 0L
        }
    }
}
