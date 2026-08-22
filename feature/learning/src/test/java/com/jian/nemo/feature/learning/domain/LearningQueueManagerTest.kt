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
}

