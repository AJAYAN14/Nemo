package com.jian.nemo.core.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.jian.nemo.core.ui.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * SRS 日期转换工具
 */
object SrsDateUtils {

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 基于 Context 将下次复习的 Epoch Day 转换为用户友好的文本
     */
    fun formatNextReviewDate(context: Context, nextReviewDay: Long): String {
        val today = LocalDate.now()
        val targetDate = LocalDate.ofEpochDay(nextReviewDay)

        val daysBetween = ChronoUnit.DAYS.between(today, targetDate)

        return if (daysBetween <= 0) {
            context.getString(R.string.core_ui_srs_status_due_today)
        } else {
            targetDate.format(formatter)
        }
    }

    /**
     * Composable 环境下便捷获取下次复习日期文案
     */
    @Composable
    fun formatNextReviewDate(nextReviewDay: Long): String {
        return formatNextReviewDate(LocalContext.current, nextReviewDay)
    }

    /**
     * 判断是否已过期（今日待复习）
     */
    fun isOverdue(nextReviewDay: Long): Boolean {
        val today = LocalDate.now().toEpochDay()
        return nextReviewDay <= today
    }
}
