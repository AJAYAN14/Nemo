package com.jian.nemo.core.ui.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * SRS 日期转换工具
 */
object SrsDateUtils {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 将下次复习的 Epoch Day 转换为用户友好的文本
     */
    fun formatNextReviewDate(nextReviewDay: Long): String {
        val today = LocalDate.now()
        val targetDate = LocalDate.ofEpochDay(nextReviewDay)

        val daysBetween = ChronoUnit.DAYS.between(today, targetDate)

        return if (daysBetween <= 0) {
            "今日待复习"
        } else {
            targetDate.format(formatter)
        }
    }

    /**
     * 判断是否已过期（今日待复习）
     */
    fun isOverdue(nextReviewDay: Long): Boolean {
        val today = LocalDate.now().toEpochDay()
        return nextReviewDay <= today
    }
}
