package com.jian.nemo.core.common.util

import kotlin.math.roundToInt

/**
 * SRS 时间格式化工具
 * 对齐 Anki 的时间显示逻辑：
 * 1. 支持秒(s)、分(m)、时(h)、天(d)、月(mo)、年(y)
 * 2. 按钮预览时：秒、分、天取整；小时、月、年保留一位小数（若为整数则不显示小数）
 */
object SrsTimeFormatter {

    private const val SECOND = 1L
    private const val MINUTE = 60 * SECOND
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR
    private const val YEAR = (365.25 * DAY).toLong() // 匹配 Anki 的平均年长度
    private const val MONTH = YEAR / 12

    /**
     * 将秒数格式化为 SRS 按钮预览字符串
     * @param seconds 总秒数
     */
    fun formatSrsInterval(seconds: Long): String {
        if (seconds <= 0) return "< 1m"

        val absSeconds = if (seconds < 0) -seconds else seconds
        
        return when {
            absSeconds < MINUTE -> "${absSeconds}s"
            absSeconds < HOUR -> {
                val mins = (absSeconds.toDouble() / MINUTE).roundToInt()
                "${mins}m"
            }
            absSeconds < DAY -> {
                val hours = absSeconds.toDouble() / HOUR
                formatWithDecimal(hours, "h")
            }
            absSeconds < MONTH -> {
                val days = (absSeconds.toDouble() / DAY).roundToInt()
                "${days}d"
            }
            absSeconds < YEAR -> {
                val months = absSeconds.toDouble() / MONTH
                formatWithDecimal(months, "mo")
            }
            else -> {
                val years = absSeconds.toDouble() / YEAR
                formatWithDecimal(years, "y")
            }
        }
    }

    /**
     * 格式化数字，如果是整数则不保留小数，否则保留一位
     */
    private fun formatWithDecimal(value: Double, unit: String): String {
        val rounded = (value * 10).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) {
            "${rounded.toInt()}$unit"
        } else {
            "$rounded$unit"
        }
    }
}
