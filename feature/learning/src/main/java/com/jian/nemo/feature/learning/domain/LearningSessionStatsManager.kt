package com.jian.nemo.feature.learning.domain

import android.content.Context
import com.jian.nemo.feature.learning.presentation.CardBadge
import com.jian.nemo.feature.learning.presentation.LearningMode

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
class LearningSessionStatsManager(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("nemo_learning_time_prefs", Context.MODE_PRIVATE)

    var sessionStartTime: Long = 0L
        private set

    var wordElapsedTime: Long = 0L
        private set

    var grammarElapsedTime: Long = 0L
        private set

    var currentCombo: Int = 0
        private set

    var maxCombo: Int = 0
        private set

    var sessionNewCount: Int = 0
        private set

    var sessionReviewCount: Int = 0
        private set

    var sessionRelearnCount: Int = 0
        private set

    /**
     * 检查今日日期，恢复本地持久化的今日战报或执行跨天重置
     */
    fun checkAndRestoreOrReset(mode: LearningMode, today: Long) {
        val lastSavedDay = sharedPrefs.getLong("last_reset_day", 0L)
        val prefix = if (mode == LearningMode.Word) "word_" else "grammar_"

        if (lastSavedDay != today) {
            // 跨天：执行全局清零
            wordElapsedTime = 0L
            grammarElapsedTime = 0L
            currentCombo = 0
            maxCombo = 0
            sessionNewCount = 0
            sessionReviewCount = 0
            sessionRelearnCount = 0

            sharedPrefs.edit()
                .putLong("last_reset_day", today)
                .putLong("word_elapsed_time", 0L)
                .putLong("grammar_elapsed_time", 0L)
                .putInt("word_current_combo", 0)
                .putInt("word_max_combo", 0)
                .putInt("word_session_new_count", 0)
                .putInt("word_session_review_count", 0)
                .putInt("word_session_relearn_count", 0)
                .putInt("grammar_current_combo", 0)
                .putInt("grammar_max_combo", 0)
                .putInt("grammar_session_new_count", 0)
                .putInt("grammar_session_review_count", 0)
                .putInt("grammar_session_relearn_count", 0)
                .apply()
            println("StatsManager: 跨越重置学习日，清零全部累计战报数据")
        } else {
            // 同一天：安全恢复数据
            wordElapsedTime = sharedPrefs.getLong("word_elapsed_time", 0L)
            grammarElapsedTime = sharedPrefs.getLong("grammar_elapsed_time", 0L)

            currentCombo = sharedPrefs.getInt("${prefix}current_combo", 0)
            maxCombo = sharedPrefs.getInt("${prefix}max_combo", 0)
            sessionNewCount = sharedPrefs.getInt("${prefix}session_new_count", 0)
            sessionReviewCount = sharedPrefs.getInt("${prefix}session_review_count", 0)
            sessionRelearnCount = sharedPrefs.getInt("${prefix}session_relearn_count", 0)

            println("StatsManager: 恢复同学习日数据 [模式: $mode, 已学时间(词/法): $wordElapsedTime/$grammarElapsedTime, 新:$sessionNewCount, 复:$sessionReviewCount, 重:$sessionRelearnCount, Combo:$maxCombo]")
        }
    }

    /**
     * 开始/恢复计时
     */
    fun resumeTimer() {
        if (sessionStartTime == 0L) {
            sessionStartTime = System.currentTimeMillis()
        }
    }

    /**
     * 暂停计时并将用时写入本地
     */
    fun pauseTimer(mode: LearningMode, today: Long) {
        if (sessionStartTime > 0) {
            val elapsed = (System.currentTimeMillis() - sessionStartTime) / 1000
            if (mode == LearningMode.Word) {
                wordElapsedTime += elapsed
                sharedPrefs.edit()
                    .putLong("last_reset_day", today)
                    .putLong("word_elapsed_time", wordElapsedTime)
                    .apply()
            } else {
                grammarElapsedTime += elapsed
                sharedPrefs.edit()
                    .putLong("last_reset_day", today)
                    .putLong("grammar_elapsed_time", grammarElapsedTime)
                    .apply()
            }
            sessionStartTime = 0L
        }
    }

    /**
     * 每次评分卡片时，更新战报统计并实时持久化
     */
    fun onItemRated(mode: LearningMode, quality: Int, badge: CardBadge, today: Long) {
        // 1. 统计学习数量分类
        when (badge) {
            CardBadge.NEW -> sessionNewCount++
            CardBadge.REVIEW -> sessionReviewCount++
            CardBadge.LEARNING, CardBadge.RELEARN -> sessionRelearnCount++
        }

        // 2. 统计连击数
        if (quality >= 3) {
            currentCombo++
            maxCombo = maxOf(maxCombo, currentCombo)
        } else {
            currentCombo = 0
        }

        // 3. 实时写入 SharedPreferences
        val prefix = if (mode == LearningMode.Word) "word_" else "grammar_"
        sharedPrefs.edit()
            .putLong("last_reset_day", today)
            .putInt("${prefix}current_combo", currentCombo)
            .putInt("${prefix}max_combo", maxCombo)
            .putInt("${prefix}session_new_count", sessionNewCount)
            .putInt("${prefix}session_review_count", sessionReviewCount)
            .putInt("${prefix}session_relearn_count", sessionRelearnCount)
            .apply()

        println("StatsManager: 用户评分(quality=$quality, badge=$badge) -> 战报更新: 新学=$sessionNewCount, 复习=$sessionReviewCount, 重学=$sessionRelearnCount, 连击=$currentCombo/$maxCombo")
    }

    /**
     * 完成会话结算，归档本次总已学时间并清空开始计时器，返回今日总时间秒数
     */
    fun completeSession(mode: LearningMode, today: Long): Long {
        val currentSessionDuration = if (sessionStartTime > 0) (System.currentTimeMillis() - sessionStartTime) / 1000 else 0L
        val totalDuration = if (mode == LearningMode.Word) {
            wordElapsedTime += currentSessionDuration
            sharedPrefs.edit()
                .putLong("last_reset_day", today)
                .putLong("word_elapsed_time", wordElapsedTime)
                .apply()
            wordElapsedTime
        } else {
            grammarElapsedTime += currentSessionDuration
            sharedPrefs.edit()
                .putLong("last_reset_day", today)
                .putLong("grammar_elapsed_time", grammarElapsedTime)
                .apply()
            grammarElapsedTime
        }
        sessionStartTime = 0L
        return totalDuration
    }

    /**
     * 获取当前战报的快照
     */
    fun getCurrentStats(mode: LearningMode): SessionStats {
        val currentSessionDuration = if (sessionStartTime > 0) (System.currentTimeMillis() - sessionStartTime) / 1000 else 0L
        val elapsed = if (mode == LearningMode.Word) {
            wordElapsedTime + currentSessionDuration
        } else {
            grammarElapsedTime + currentSessionDuration
        }
        return SessionStats(
            elapsedTime = elapsed,
            maxCombo = maxCombo,
            newCount = sessionNewCount,
            reviewCount = sessionReviewCount,
            relearnCount = sessionRelearnCount
        )
    }

    /**
     * 重置计时变量（用于退出时仅置空本次计时，不抹掉持久化总计）
     */
    fun clearSessionTimer() {
        sessionStartTime = 0L
    }
}
