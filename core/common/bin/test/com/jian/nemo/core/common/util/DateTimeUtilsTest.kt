package com.jian.nemo.core.common.util

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.Calendar

/**
 * Unit tests for DateTimeUtils - Focusing on Learning Day logic
 */
class DateTimeUtilsTest {

    @Test
    fun `toLearningDay should return previous day before reset hour`() {
        // Mock 2026-02-11 03:00 (Epoch Day 20495)
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.FEBRUARY, 11, 3, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val millis = calendar.timeInMillis
        val resetHour = 4

        val learningDay = DateTimeUtils.toLearningDay(millis, resetHour)
        // 2026-02-11 03:00 < 4:00 -> Previous day (2026-02-10)
        val expectedDay = LocalDate.of(2026, 2, 10).toEpochDay()

        assertEquals("3:00 AM should be considered previous day if reset is 4:00 AM", expectedDay, learningDay)
    }

    @Test
    fun `toLearningDay should return current day after or at reset hour`() {
        // Mock 2026-02-11 04:00
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.FEBRUARY, 11, 4, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val millis = calendar.timeInMillis
        val resetHour = 4

        val learningDay = DateTimeUtils.toLearningDay(millis, resetHour)
        // 2026-02-11 04:00 >= 4:00 -> Current day (2026-02-11)
        val expectedDay = LocalDate.of(2026, 2, 11).toEpochDay()

        assertEquals("4:00 AM should be considered new day if reset is 4:00 AM", expectedDay, learningDay)
    }

    @Test
    fun `toLearningDay should handle midnight correctly`() {
        // Mock 2026-02-11 00:00
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.FEBRUARY, 11, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val millis = calendar.timeInMillis
        val resetHour = 4

        val learningDay = DateTimeUtils.toLearningDay(millis, resetHour)
        // 0:00 AM < 4:00 -> Previous day (2026-02-10)
        val expectedDay = LocalDate.of(2026, 2, 10).toEpochDay()

        assertEquals("0:00 AM should be considered previous day if reset is 4:00 AM", expectedDay, learningDay)
    }

    @Test
    fun `toLearningDay should work with 0 reset hour (standard midnight)`() {
        // Mock 2026-02-11 00:01
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.FEBRUARY, 11, 0, 1, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val millis = calendar.timeInMillis
        val resetHour = 0

        val learningDay = DateTimeUtils.toLearningDay(millis, resetHour)
        // 0:01 AM >= 0:00 -> Current day (2026-02-11)
        val expectedDay = LocalDate.of(2026, 2, 11).toEpochDay()

        assertEquals("0:01 AM should be current day if reset is 0:00 AM", expectedDay, learningDay)
    }

    @Test
    fun `epochDayToLocalDate conversion should be accurate`() {
        val date = DateTimeUtils.epochDayToLocalDate(20454) // 2026-01-01
        assertEquals(LocalDate.of(2026, 1, 1), date)
    }

    @Test
    fun `daysBetween should calculate difference correctly`() {
        val start = 20454L // 2026-01-01
        val end = 20464L   // 2026-01-11
        assertEquals(10, DateTimeUtils.daysBetween(start, end))
    }
}
