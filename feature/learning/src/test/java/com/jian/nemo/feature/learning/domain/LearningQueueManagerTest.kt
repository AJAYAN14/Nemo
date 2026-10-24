package com.jian.nemo.feature.learning.domain

import org.junit.Assert.*
import org.junit.Test

class LearningQueueManagerTest {

    private val manager = LearningQueueManager()

    @Test
    fun `insertAtRelativeOffset should insert item smoothly at relative offset position`() {
        // Given: [A, B, C, D, E, F, G], 当前操作卡片是 B (index 1), 相对后移 3 张
        val items = listOf("A", "B", "C", "D", "E", "F", "G")
        
        // When
        val result = manager.insertAtRelativeOffset(
            items = items,
            currentIndex = 1,
            itemToInsert = "B_updated",
            offset = 3
        )

        // Then: 移除 B 后为 [A, C, D, E, F, G]，在 1 + 3 = 4 处插入
        assertEquals(listOf("A", "C", "D", "E", "B_updated", "F", "G"), result)
    }

    @Test
    fun `insertAtRelativeOffset when offset exceeds remaining size should append to end`() {
        // Given: [A, B], 当前在 A (index 0), offset 5 (超出列表长度)
        val items = listOf("A", "B")

        // When
        val result = manager.insertAtRelativeOffset(
            items = items,
            currentIndex = 0,
            itemToInsert = "A_updated",
            offset = 5
        )

        // Then: 应该追加在末尾
        assertEquals(listOf("B", "A_updated"), result)
    }

    @Test
    fun `handleSkippedItemsOnRating when user skips first 3 items should move them to end`() {
        // Given: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9], 用户直接在 index 3 打分
        val items = (0..9).toList()

        // When
        val (rearranged, newIndex) = manager.handleSkippedItemsOnRating(items, currentIndex = 3)

        // Then: 0, 1, 2 顺延到队尾，3 变成新列表的 index 0
        assertEquals(listOf(3, 4, 5, 6, 7, 8, 9, 0, 1, 2), rearranged)
        assertEquals(0, newIndex)
    }

    @Test
    fun `handleSkippedItemsOnRating when at index 0 should do nothing`() {
        val items = listOf("A", "B", "C")
        val (rearranged, newIndex) = manager.handleSkippedItemsOnRating(items, currentIndex = 0)
        assertEquals(items, rearranged)
        assertEquals(0, newIndex)
    }

    @Test
    fun `selectNextItem on empty list should return Empty`() {
        val result = manager.selectNextItem<String>(emptyList(), preferredIndex = 0)
        assertTrue(result is QueueSelectionResult.Empty)
    }

    @Test
    fun `selectNextItem should honor preferredIndex safely`() {
        val items = listOf("A", "B", "C")
        val result = manager.selectNextItem(items, preferredIndex = 1)
        assertTrue(result is QueueSelectionResult.Next)
        assertEquals(1, (result as QueueSelectionResult.Next).index)
        assertEquals("B", result.item)

        // 越界时安全回落到最后一位
        val outOfBounds = manager.selectNextItem(items, preferredIndex = 10)
        assertEquals(2, (outOfBounds as QueueSelectionResult.Next).index)
        assertEquals("C", outOfBounds.item)
    }

    @Test
    fun `单张卡片在未来到期时应该返回 Wait 防秒弹`() {
        val items = listOf("A")
        val now = 1000L
        val dueTime = now + 10 * 60 * 1000L // 10 分钟后

        val result = manager.selectNextItem(
            items = items,
            getDueTime = { dueTime },
            now = now,
            learnAheadLimitMs = 20 * 60 * 1000L,
            preferredIndex = 0
        )

        assertTrue("单张卡片在未来时应触发 Wait 倒计时，防止原地秒弹", result is QueueSelectionResult.Wait)
        assertEquals(dueTime, (result as QueueSelectionResult.Wait).waitingUntil)
    }

    @Test
    fun `所有卡片在未来且超出提前学习容差时应该返回 Wait`() {
        val items = listOf("A", "B")
        val now = 1000L
        val dueTimes = mapOf("A" to now + 30 * 60 * 1000L, "B" to now + 40 * 60 * 1000L)

        val result = manager.selectNextItem(
            items = items,
            getDueTime = { dueTimes[it] ?: 0L },
            now = now,
            learnAheadLimitMs = 20 * 60 * 1000L, // 容差 20 分钟，卡片需要 30 分钟
            preferredIndex = 0
        )

        assertTrue(result is QueueSelectionResult.Wait)
        assertEquals(now + 30 * 60 * 1000L, (result as QueueSelectionResult.Wait).waitingUntil)
    }

    @Test
    fun `优先选择已到期的卡片`() {
        val items = listOf("A", "B", "C")
        val now = 1000L
        // A 和 C 在未来，B 已经就绪
        val dueTimes = mapOf("A" to now + 600000L, "B" to now - 1000L, "C" to now + 800000L)

        val result = manager.selectNextItem(
            items = items,
            getDueTime = { dueTimes[it] ?: 0L },
            now = now,
            learnAheadLimitMs = 20 * 60 * 1000L,
            preferredIndex = 0
        )

        assertTrue(result is QueueSelectionResult.Next)
        assertEquals(1, (result as QueueSelectionResult.Next).index)
        assertEquals("B", result.item)
    }
}

