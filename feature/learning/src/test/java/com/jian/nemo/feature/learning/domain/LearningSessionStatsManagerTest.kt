package com.jian.nemo.feature.learning.domain

import com.jian.nemo.core.domain.model.LearningMode
import com.jian.nemo.feature.learning.data.preferences.LearningSessionStatsStorage
import com.jian.nemo.feature.learning.data.preferences.RawStatsData
import com.jian.nemo.feature.learning.presentation.CardBadge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearningSessionStatsManagerTest {

    private class FakeStorage(
        var rawData: RawStatsData = RawStatsData()
    ) : LearningSessionStatsStorage {
        var resetCount = 0
        var savedElapsedTimes = mutableListOf<Triple<LearningMode, Long, Long>>()
        var savedModeStats = mutableListOf<String>()

        override suspend fun loadStats(): RawStatsData = rawData

        override suspend fun resetAllStats(today: Long) {
            resetCount++
            rawData = RawStatsData(lastResetDay = today)
        }

        override suspend fun saveElapsedTime(mode: LearningMode, today: Long, elapsedTime: Long) {
            savedElapsedTimes.add(Triple(mode, today, elapsedTime))
            rawData = if (mode == LearningMode.Word) {
                rawData.copy(lastResetDay = today, wordElapsedTime = elapsedTime)
            } else {
                rawData.copy(lastResetDay = today, grammarElapsedTime = elapsedTime)
            }
        }

        override suspend fun saveModeStats(
            mode: LearningMode,
            today: Long,
            currentCombo: Int,
            maxCombo: Int,
            newCount: Int,
            reviewCount: Int,
            relearnCount: Int,
            relearnIds: String
        ) {
            savedModeStats.add("$mode:$today:$newCount:$reviewCount:$currentCombo:$maxCombo")
            rawData = if (mode == LearningMode.Word) {
                rawData.copy(
                    lastResetDay = today,
                    wordCurrentCombo = currentCombo,
                    wordMaxCombo = maxCombo,
                    wordSessionNewCount = newCount,
                    wordSessionReviewCount = reviewCount,
                    wordSessionRelearnCount = relearnCount,
                    wordRelearnIds = relearnIds
                )
            } else {
                rawData.copy(
                    lastResetDay = today,
                    grammarCurrentCombo = currentCombo,
                    grammarMaxCombo = maxCombo,
                    grammarSessionNewCount = newCount,
                    grammarSessionReviewCount = reviewCount,
                    grammarSessionRelearnCount = relearnCount,
                    grammarRelearnIds = relearnIds
                )
            }
        }

        var savedAllStats = mutableListOf<String>()

        override suspend fun saveAllStats(
            mode: LearningMode,
            today: Long,
            elapsedTime: Long,
            currentCombo: Int,
            maxCombo: Int,
            newCount: Int,
            reviewCount: Int,
            relearnCount: Int,
            relearnIds: String
        ) {
            savedAllStats.add("$mode:$today:$elapsedTime:$newCount:$reviewCount:$currentCombo:$maxCombo")
            rawData = if (mode == LearningMode.Word) {
                rawData.copy(
                    lastResetDay = today,
                    wordElapsedTime = elapsedTime,
                    wordCurrentCombo = currentCombo,
                    wordMaxCombo = maxCombo,
                    wordSessionNewCount = newCount,
                    wordSessionReviewCount = reviewCount,
                    wordSessionRelearnCount = relearnCount,
                    wordRelearnIds = relearnIds
                )
            } else {
                rawData.copy(
                    lastResetDay = today,
                    grammarElapsedTime = elapsedTime,
                    grammarCurrentCombo = currentCombo,
                    grammarMaxCombo = maxCombo,
                    grammarSessionNewCount = newCount,
                    grammarSessionReviewCount = reviewCount,
                    grammarSessionRelearnCount = relearnCount,
                    grammarRelearnIds = relearnIds
                )
            }
        }
    }

    @Test
    fun `未 hydrate 时暂停不落盘空数据覆盖存储`() = runTest {
        val storage = FakeStorage(
            rawData = RawStatsData(lastResetDay = 100L, wordElapsedTime = 600L)
        )
        val manager = LearningSessionStatsManager(
            storage = storage,
            appScope = this,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        assertFalse(manager.isHydrated)
        manager.pauseTimer(LearningMode.Word, today = 100L)
        advanceTimeBy(1000)

        // 未经历 checkAndRestoreOrReset，不向 storage 落盘
        assertEquals(0, storage.savedElapsedTimes.size)
        assertEquals(600L, storage.rawData.wordElapsedTime)
    }

    @Test
    fun `跨天时全量清空内存态并触发 storage resetAllStats`() = runTest {
        val storage = FakeStorage(
            rawData = RawStatsData(
                lastResetDay = 100L,
                wordElapsedTime = 500L,
                wordSessionNewCount = 10,
                wordMaxCombo = 5
            )
        )
        val manager = LearningSessionStatsManager(
            storage = storage,
            appScope = this,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        manager.checkAndRestoreOrReset(LearningMode.Word, today = 101L)

        assertTrue(manager.isHydrated)
        assertEquals(0L, manager.wordElapsedTime)
        assertEquals(0, manager.sessionNewCount)
        assertEquals(0, manager.maxCombo)
        assertEquals(1, storage.resetCount)
        assertEquals(101L, storage.rawData.lastResetDay)
    }

    @Test
    fun `同天时安全恢复内存态且双模式互不覆盖`() = runTest {
        val storage = FakeStorage(
            rawData = RawStatsData(
                lastResetDay = 100L,
                wordElapsedTime = 300L,
                grammarElapsedTime = 200L,
                wordCurrentCombo = 4,
                wordMaxCombo = 7,
                wordSessionNewCount = 15,
                wordSessionReviewCount = 20,
                grammarSessionNewCount = 5,
                wordRelearnIds = "1,2,3"
            )
        )
        val manager = LearningSessionStatsManager(
            storage = storage,
            appScope = this,
            ioDispatcher = StandardTestDispatcher(testScheduler)
        )

        manager.checkAndRestoreOrReset(LearningMode.Word, today = 100L)

        assertTrue(manager.isHydrated)
        assertEquals(300L, manager.wordElapsedTime)
        assertEquals(200L, manager.grammarElapsedTime)
        assertEquals(4, manager.currentCombo)
        assertEquals(7, manager.maxCombo)
        assertEquals(15, manager.sessionNewCount)
        assertEquals(20, manager.sessionReviewCount)
        assertEquals(3, manager.sessionRelearnCount)
        assertEquals(0, storage.resetCount)
    }

    @Test
    fun `评分卡片时 400ms 防抖合并高频持久化`() = runTest {
        val storage = FakeStorage()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val manager = LearningSessionStatsManager(
            storage = storage,
            appScope = this,
            ioDispatcher = testDispatcher
        )

        manager.checkAndRestoreOrReset(LearningMode.Word, today = 100L)

        // 连续快速评分 3 次
        manager.onItemRated(LearningMode.Word, quality = 4, badge = CardBadge.NEW, itemId = 10, today = 100L)
        manager.onItemRated(LearningMode.Word, quality = 4, badge = CardBadge.NEW, itemId = 11, today = 100L)
        manager.onItemRated(LearningMode.Word, quality = 4, badge = CardBadge.NEW, itemId = 12, today = 100L)

        // 内存态已即时生效
        assertEquals(3, manager.sessionNewCount)
        assertEquals(3, manager.currentCombo)
        assertEquals(3, manager.maxCombo)

        // 此时 200ms 防抖未到期，storage 尚未调用
        advanceTimeBy(200)
        assertEquals(0, storage.savedModeStats.size)

        // 再过 250ms (总计 450ms)，防抖触发，合并为 1 次写入
        advanceTimeBy(250)
        assertEquals(1, storage.savedModeStats.size)
        assertTrue(storage.savedModeStats[0].contains("Word:100:3:0:3:3"))
    }

    @Test
    fun `评分后立即暂停在400ms防抖内立即通过saveAllStats落盘杜绝丢评分与连击`() = runTest {
        val storage = FakeStorage()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val manager = LearningSessionStatsManager(
            storage = storage,
            appScope = this,
            ioDispatcher = testDispatcher
        )

        manager.checkAndRestoreOrReset(LearningMode.Word, today = 100L)
        manager.resumeTimer()

        // 评分一次后仅过 50ms 即退后台暂停
        manager.onItemRated(LearningMode.Word, quality = 4, badge = CardBadge.NEW, itemId = 10, today = 100L)
        advanceTimeBy(50)
        manager.pauseTimer(LearningMode.Word, today = 100L)
        advanceTimeBy(100)

        // 防抖任务虽被取消，但最新评分与连击已随 saveAllStats 立即落盘
        assertEquals(1, storage.savedAllStats.size)
        assertTrue(storage.savedAllStats[0].contains("Word:100:"))
        // 验证 rawData 已持久化新学数 1 与连击数 1
        assertEquals(1, storage.rawData.wordSessionNewCount)
        assertEquals(1, storage.rawData.wordMaxCombo)
        assertEquals(1, storage.rawData.wordCurrentCombo)
    }

    @Test
    fun `双模式在内存中严格隔离，Word模式计数不污染Grammar模式查询`() = runTest {
        val storage = FakeStorage()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val manager = LearningSessionStatsManager(
            storage = storage,
            appScope = this,
            ioDispatcher = testDispatcher
        )

        manager.checkAndRestoreOrReset(LearningMode.Word, today = 100L)

        // 在 Word 模式下学习
        manager.onItemRated(LearningMode.Word, quality = 4, badge = CardBadge.NEW, itemId = 10, today = 100L)
        manager.onItemRated(LearningMode.Word, quality = 4, badge = CardBadge.NEW, itemId = 11, today = 100L)

        val wordStats = manager.getCurrentStats(LearningMode.Word)
        assertEquals(2, wordStats.newCount)
        assertEquals(2, wordStats.maxCombo)

        // 此时查询 Grammar 模式战报，必须为独立状态 0，绝不受 Word 污染
        val grammarStats = manager.getCurrentStats(LearningMode.Grammar)
        assertEquals(0, grammarStats.newCount)
        assertEquals(0, grammarStats.maxCombo)
    }
}
