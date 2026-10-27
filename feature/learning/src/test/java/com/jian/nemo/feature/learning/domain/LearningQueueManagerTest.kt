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
    fun `单张卡片在未来但处于提前学习容差内时应该对齐 Anki 返回 Next 允许提前温习`() {
        val items = listOf("A")
        val now = 1000L
        val dueTime = now + 10 * 60 * 1000L // 10 分钟后

        val result = manager.selectNextItem(
            items = items,
            getDueTime = { dueTime },
            now = now,
            learnAheadLimitMs = 20 * 60 * 1000L, // 容差 20 分钟 >= 10 分钟
            preferredIndex = 0
        )

        assertTrue("单张卡片在容差内应允许提前温习", result is QueueSelectionResult.Next)
        assertEquals(0, (result as QueueSelectionResult.Next).index)
        assertEquals("A", result.item)
    }

    @Test
    fun `单张卡片在未来且容差设为 0 时应该返回 Wait`() {
        val items = listOf("A")
        val now = 1000L
        val dueTime = now + 10 * 60 * 1000L // 10 分钟后

        val result = manager.selectNextItem(
            items = items,
            getDueTime = { dueTime },
            now = now,
            learnAheadLimitMs = 0L, // 容差为 0，严格等待
            preferredIndex = 0
        )

        assertTrue("容差为 0 时应触发 Wait 倒计时", result is QueueSelectionResult.Wait)
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

    @Test
    fun `reorderQueueAnkiStyle 点Again的卡片处于未来冷却中时应排在所有常规未学卡片之后`() {
        // Given: [A, B, C]，当前正在做 A (index 0)
        // B, C 是常规待学卡 (due = 0)
        // A 点了 Again，到期时间设定为 1 分钟后 (now + 60s)
        val items = listOf("A", "B", "C")
        val now = 10000L
        val dueMap = mutableMapOf("A" to now + 60_000L, "B" to 0L, "C" to 0L)

        // When
        val reordered = manager.reorderQueueAnkiStyle(
            items = items,
            currentIndex = 0,
            itemToRequeue = "A",
            getDueTime = { dueMap[it] ?: 0L },
            now = now
        )

        // Then: A 排在 B, C 之后，下一张顺理成章是 B
        assertEquals(listOf("B", "C", "A"), reordered)
    }

    @Test
    fun `reorderQueueAnkiStyle 当在学卡片时间到达时应优先插队在常规卡片之前`() {
        // Given: 列表中有 [B, C, A]，其中 A 是 60s 前点了 Again 的卡片
        // 经过 65s 后，用户刚做完 B，此时 A 的到期时间已过 (已成熟 due <= now)
        // C 仍然是常规未学卡 (due = 0)
        val items = listOf("B", "C", "A")
        val now = 100000L
        val dueMap = mapOf("B" to 0L, "C" to 0L, "A" to now - 5000L) // A 已到期 5 秒

        // When: B 毕业移出，对剩余列表排序
        val sorted = manager.sortAnkiStyle(
            items = listOf("C", "A"),
            getDueTime = { dueMap[it] ?: 0L },
            now = now
        )

        // Then: A 具有最高调度优先级，插队到 C 之前！
        assertEquals(listOf("A", "C"), sorted)
    }

    @Test
    fun `reorderQueueAnkiStyle 多张点Again卡片应严格按到期时间升序排列`() {
        // Given: 原列表中有 [A, C, B]，当前正在做 A (index 0)
        // A 点了 Again 到期时间是 10s 后，B 在先前点了 Again 到期时间是 60s 后，C 是常规未学卡 (due = 0)
        val items = listOf("A", "C", "B")
        val now = 10000L
        val dueMap = mapOf("A" to now + 10_000L, "B" to now + 60_000L, "C" to 0L)

        // When: A 点了 Again 重新入队
        val reordered = manager.reorderQueueAnkiStyle(
            items = items,
            currentIndex = 0,
            itemToRequeue = "A",
            getDueTime = { dueMap[it] ?: 0L },
            now = now
        )

        // Then: 常规卡 C 在前，冷却卡按 A -> B 升序排在后
        assertEquals(listOf("C", "A", "B"), reordered)
    }
}

