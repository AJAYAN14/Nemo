package com.jian.nemo.core.common.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 日期时间工具类
 * 统一处理 ISO 8601 格式的时间
 */
object DateTimeUtils {

    // 缓存服务器时间偏移量，避免频繁读取 SharedPrefs
    private var serverTimeOffset: Long = 0L

    /**
     * 设置服务器时间偏移
     * @param offset 偏移量（毫秒）
     */
    fun setServerTimeOffset(offset: Long) {
        serverTimeOffset = offset
    }

    /**
     * 获取经过补偿的当前时间戳
     * 用于多设备同步时的时间基准统一
     */
    fun getCurrentCompensatedMillis(): Long {
        return System.currentTimeMillis() + serverTimeOffset
    }

    private const val ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

    // UI 显示格式
    private const val DISPLAY_PATTERN_FULL = "yyyy/MM/dd HH:mm"

    /**
     * 解析 ISO 8601 格式的时间字符串
     * 支持格式: 2026-01-29T00:00:00.000Z 或 2026-01-29T00:00:00
     * @param timeString ISO 8601 时间字符串
     * @return Date 对象，解析失败返回 null
     */
    fun parseIso8601(timeString: String?): Date? {
        if (timeString.isNullOrEmpty()) return null
        return try {
            // 简单处理: 去掉毫秒和时区后缀，统一按 UTC 处理
            // 这种方式兼容性好，且对于同步时间戳已经足够
            val cleanTime = timeString.substringBefore('.').substringBefore('Z')
            val sdf = SimpleDateFormat(ISO_8601_PATTERN, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(cleanTime)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 将 Date 格式化为 ISO 8601 字符串 (用于发送给服务端)
     * @param date Date 对象
     * @return ISO 8601 字符串
     */
    fun formatIso8601(date: Date): String {
        val sdf = SimpleDateFormat(ISO_8601_PATTERN, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date) + "Z"
    }

    /**
     * 将 Date 格式化为用户友好的显示字符串
     * @param date Date 对象
     * @return 格式化后的字符串 (yyyy/MM/dd HH:mm)
     */
    fun formatDisplay(date: Date): String {
        val sdf = SimpleDateFormat(DISPLAY_PATTERN_FULL, Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * 获取经过补偿的今天的 Epoch Day (自 1970-01-01 起的天数)
     * 用于 SRS 算法计算
     *
     * @deprecated 请使用 getLearningDay(resetHour)，除非您确实需要物理上的 UTC 天数。
     */
    @Deprecated("使用 getLearningDay(resetHour) 以支持设置中的重置时间", ReplaceWith("getLearningDay(resetHour)"))
    fun getCurrentEpochDay(): Long {
        return getCurrentCompensatedMillis() / (1000 * 60 * 60 * 24)
    }

    @Deprecated("使用 getLearningDay(resetHour)", ReplaceWith("getLearningDay(resetHour)"))
    fun getTodayEpochDay(): Long = getCurrentEpochDay()

    /**
     * 获取当前的学习日 (Epoch Day)
     * 根据设置的每日重置时间计算，集成服务器补偿时间
     * @param resetHour 每日重置时间 (0-23)
     */
    fun getLearningDay(resetHour: Int): Long {
        return toLearningDay(getCurrentCompensatedMillis(), resetHour)
    }

    /**
     * 将指定时间戳转换为逻辑学习日 (Epoch Day)
     * @param millis 物理时间戳 (毫秒)
     * @param resetHour 每日重置时间 (0-23)
     */
    fun toLearningDay(millis: Long, resetHour: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis

        if (calendar.get(Calendar.HOUR_OF_DAY) < resetHour) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        // 使用 java.time API 进行安全的 EpochDay 转换，规避除法产生的时区偏移
        return LocalDate.of(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1, // Calendar 月份 0-11, LocalDate 1-12
            calendar.get(Calendar.DAY_OF_MONTH)
        ).toEpochDay()
    }

    /**
     * 将 Epoch Day 转换为 LocalDate
     */
    fun epochDayToLocalDate(epochDay: Long): java.time.LocalDate {
        return java.time.LocalDate.ofEpochDay(epochDay)
    }

    /**
     * 将 LocalDate 转换为 Epoch Day
     */
    fun localDateToEpochDay(localDate: java.time.LocalDate): Long {
        return localDate.toEpochDay()
    }

    /**
     * 将时间戳转换为 Epoch Day (物理零点)
     */
    fun timestampToEpochDay(millis: Long): Long {
        return millis / (1000 * 60 * 60 * 24)
    }

    /**
     * 计算两个 Epoch Day 之间的天数差
     */
    fun daysBetween(start: Long, end: Long): Int {
        return (end - start).toInt()
    }

    /**
     * 将 Epoch Day 格式化为字符串 (yyyy-MM-dd)
     */
    fun epochDayToString(epochDay: Long): String {
        return epochDayToLocalDate(epochDay).toString()
    }

    /**
     * 将 Epoch Day 格式化为用户友好的显示字符串 (MM月dd日)
     * 用于确保逻辑日期显示一致
     */
    fun formatEpochDayToDisplay(epochDay: Long): String {
        val localDate = epochDayToLocalDate(epochDay)
        return String.format(Locale.CHINA, "%02d月%02d日", localDate.monthValue, localDate.dayOfMonth)
    }
}
