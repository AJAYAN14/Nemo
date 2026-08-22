package com.jian.nemo.feature.learning.domain

import javax.inject.Inject

/**
 * 队列选择结果
 */
sealed class QueueSelectionResult<out T> {
    /** 选中了下一个项目 */
    data class Next<out T>(val index: Int, val item: T) : QueueSelectionResult<T>()

    /** 需要等待 (保留兼容性) */
    data class Wait<out T>(val waitingUntil: Long) : QueueSelectionResult<T>()

    /** 队列为空 (会话完成) */
    data object Empty : QueueSelectionResult<Nothing>()
}

/**
 * 学习队列管理器
 *
 * 负责从学习队列中进行平滑单向调度：
 * 1. 相对位置动态插入 (Relative Offset Insertion): 生疏词后移 3~5 张，中间步后移 8~12 张。
 * 2. 跳过卡片顺延 (Push Skipped to End): 若用户跳过前几张直接评分，未评分卡片顺延到队尾补漏。
 * 3. 单向平滑流 (Linear Smooth Progression): 视图永远单向流动，消除突兀跳跃与倒退。
 */
class LearningQueueManager @Inject constructor() {

    /**
     * 处理跳过卡片的顺延
     *
     * 如果用户在 currentIndex > 0 处进行评分，说明跳过了 0 until currentIndex 的卡片。
     * 将跳过的卡片自动挪到列表末尾，当前正在操作的卡片顺延成为新列表的第 0 项。
     *
     * @return Pair(重排后的列表, 当前项在新列表中的索引 0)
     */
    fun <T> handleSkippedItemsOnRating(items: List<T>, currentIndex: Int): Pair<List<T>, Int> {
        if (items.isEmpty() || currentIndex <= 0 || currentIndex >= items.size) {
            return items to currentIndex.coerceAtLeast(0).coerceAtMost((items.size - 1).coerceAtLeast(0))
        }

        val currentAndAfter = items.subList(currentIndex, items.size)
        val skippedBefore = items.subList(0, currentIndex)
        val rearranged = ArrayList<T>(items.size).apply {
            addAll(currentAndAfter)
            addAll(skippedBefore)
        }

        return rearranged to 0
    }

    /**
     * 相对位置动态插入 (用于 Requeue：生疏、困难、在学中间步)
     *
     * 先从当前列表中移除当前操作项，然后将更新后的项插入到 (currentIndex + offset) 的位置。
     *
     * @param items 当前列表
     * @param currentIndex 当前操作项索引
     * @param itemToInsert 准备插入的卡片
     * @param offset 相对卡片张数偏移量 (例如 3~5)
     * @return 插入后的新列表
     */
    fun <T> insertAtRelativeOffset(
        items: List<T>,
        currentIndex: Int,
        itemToInsert: T,
        offset: Int
    ): List<T> {
        val mutableList = items.toMutableList()
        if (currentIndex in mutableList.indices) {
            mutableList.removeAt(currentIndex)
        }

        // 计算插入位置：以当前位置为基准向后偏移 offset 张
        val targetIndex = (currentIndex + offset).coerceIn(0, mutableList.size)
        mutableList.add(targetIndex, itemToInsert)
        return mutableList
    }

    /**
     * 从列表中移除当前项 (用于 Graduate 毕业)
     *
     * @param items 当前列表
     * @param currentIndex 当前操作项索引
     * @return 移除后的新列表
     */
    fun <T> removeCurrent(items: List<T>, currentIndex: Int): List<T> {
        val mutableList = items.toMutableList()
        if (currentIndex in mutableList.indices) {
            mutableList.removeAt(currentIndex)
        }
        return mutableList
    }

    /**
     * 选择下一个要展示的项目 (严格单向平滑选择)
     *
     * @param items 候选列表
     * @param preferredIndex 首选索引 (当前位置)
     */
    fun <T> selectNextItem(
        items: List<T>,
        preferredIndex: Int = 0
    ): QueueSelectionResult<T> {
        if (items.isEmpty()) {
            return QueueSelectionResult.Empty
        }

        val validIndex = preferredIndex.coerceIn(0, items.size - 1)
        return QueueSelectionResult.Next(validIndex, items[validIndex])
    }

    /**
     * 兼容旧接口的重载
     */
    fun <T> selectNextItem(
        items: List<T>,
        getDueTime: (T) -> Long,
        now: Long,
        learnAheadLimitMs: Long,
        preferredIndex: Int = 0
    ): QueueSelectionResult<T> {
        return selectNextItem(items, preferredIndex)
    }
}

