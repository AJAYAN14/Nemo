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
     * 按 Anki 优先级排布卡片列表 (已到期在学词 > 常规词 > 冷却中在学词)
     */
    fun <T> sortAnkiStyle(
        items: List<T>,
        getDueTime: (T) -> Long,
        now: Long = System.currentTimeMillis()
    ): List<T> {
        if (items.size <= 1) return items

        val dueLearning = mutableListOf<T>()
        val regular = mutableListOf<T>()
        val futureLearning = mutableListOf<T>()

        for (item in items) {
            val due = getDueTime(item)
            when {
                due in 1..now -> dueLearning.add(item)
                due > now -> futureLearning.add(item)
                else -> regular.add(item)
            }
        }

        dueLearning.sortBy { getDueTime(it) }
        futureLearning.sortBy { getDueTime(it) }

        return ArrayList<T>(items.size).apply {
            addAll(dueLearning)
            addAll(regular)
            addAll(futureLearning)
        }
    }

    /**
     * 对齐 Anki 的卡片重入队与重排算法 (Anki Sched Queue Reordering):
     *
     * 当卡片评分后需要留在队列中 (Again 重来、Hard 困难、Good 中间步) 时：
     * 1. 从原列表中移除正在操作的卡片 (位于 currentIndex 处)。
     * 2. 如果除当前项外无其它卡片，直接返回包含本卡片的单元素列表。
     * 3. 划分剩余原有卡片：
     *    - 优先级 1 (已到期的在学卡片)：dueTime > 0 且 dueTime <= now，按 dueTime 升序排在最前；
     *    - 优先级 2 (常规未开始待学卡片)：未在学的新词或未评级复习词 (dueTime == 0L)，保持原有先后顺序；
     *    - 优先级 3 (冷却中的在学卡片)：处于分钟级未来倒计时中的卡片 (dueTime > now)，按 dueTime 升序排在末尾。
     * 4. 插入当前操作的这顶卡片 (itemToRequeue)：
     *    - 若当前卡片已到期 (dueTime <= now)：插入优先级 1 中；
     *    - 若当前卡片在未来 (dueTime > now)：严格对齐 Anki test_learn_collapsedV2 规范，
     *      只要队列中还有其它卡片在等候，当前卡片排在已有等候卡片之后，绝对防止刚点完 Again 连续两次展示同一张卡片。
     */
    fun <T> reorderQueueAnkiStyle(
        items: List<T>,
        currentIndex: Int,
        itemToRequeue: T,
        getDueTime: (T) -> Long,
        now: Long = System.currentTimeMillis()
    ): List<T> {
        val remaining = items.toMutableList()
        if (currentIndex in remaining.indices) {
            remaining.removeAt(currentIndex)
        }
        if (remaining.isEmpty()) {
            return listOf(itemToRequeue)
        }

        val dueLearning = mutableListOf<T>()
        val regular = mutableListOf<T>()
        val futureLearning = mutableListOf<T>()

        for (item in remaining) {
            val due = getDueTime(item)
            when {
                due in 1..now -> dueLearning.add(item)
                due > now -> futureLearning.add(item)
                else -> regular.add(item)
            }
        }

        dueLearning.sortBy { getDueTime(it) }
        futureLearning.sortBy { getDueTime(it) }

        val currentDue = getDueTime(itemToRequeue)
        if (currentDue in 1..now) {
            dueLearning.add(itemToRequeue)
        } else {
            futureLearning.add(itemToRequeue)
        }

        dueLearning.sortBy { getDueTime(it) }
        futureLearning.sortBy { getDueTime(it) }

        val result = ArrayList<T>(items.size).apply {
            addAll(dueLearning)
            addAll(regular)
            addAll(futureLearning)
        }

        // 防瞬间重复 (Anti-immediate repeat, 对齐 Anki SchedulerTest.test_learn_collapsedV2):
        // 如果刚操作的卡片排在了第 0 项，但队列中明明还有其他卡片在等候，
        // 则将其后移，优先展示队列中的下一张卡片，避免用户刚点完操作立刻又看到同一张卡片。
        if (result.isNotEmpty() && result[0] == itemToRequeue && result.size > 1) {
            result.removeAt(0)
            var inserted = false
            for (i in result.indices) {
                if (getDueTime(result[i]) > currentDue) {
                    result.add(i.coerceAtLeast(1), itemToRequeue)
                    inserted = true
                    break
                }
            }
            if (!inserted) {
                result.add(itemToRequeue)
            }
        }

        return result
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
     * 选择下一个要展示的项目 (时间感知与单向平滑调度)
     *
     * 1. 优先从当前位置 (preferredIndex) 开始寻找已到期的卡片 (dueTime <= now 或未设置 dueTime 的新词 0L)。
     * 2. 若队列中所有卡片都在未来 (dueTime > now)：
     *    - 找到最早到期的卡片及其等待时间 (waitMs = minDueTime - now)。
     *    - 若 waitMs > learnAheadLimitMs 或 learnAheadLimitMs <= 0 (超出提前学习容差或禁止提前学习)，触发 Wait(minDueTime)。
     *    - 若 waitMs <= learnAheadLimitMs，对齐 Anki 规范，允许提前学习，选择最早到期的卡片。
     * 3. 若列表为空，返回 Empty。
     */
    fun <T> selectNextItem(
        items: List<T>,
        getDueTime: (T) -> Long,
        now: Long = System.currentTimeMillis(),
        learnAheadLimitMs: Long = 20 * 60 * 1000L,
        preferredIndex: Int = 0
    ): QueueSelectionResult<T> {
        if (items.isEmpty()) {
            return QueueSelectionResult.Empty
        }

        val count = items.size

        // 1. 优先寻找已经就绪的卡片 (dueTime <= now 或 0L)
        // 从 preferredIndex 开始向前线性扫描一圈，保证平滑单向流动
        val startIndex = preferredIndex.coerceIn(0, count - 1)
        for (i in 0 until count) {
            val idx = (startIndex + i) % count
            val due = getDueTime(items[idx])
            if (due <= now || due == 0L) {
                return QueueSelectionResult.Next(idx, items[idx])
            }
        }

        // 2. 队列中所有卡片都在未来 (due > now)
        var earliestIndex = 0
        var minDueTime = Long.MAX_VALUE
        for (i in items.indices) {
            val due = getDueTime(items[i])
            if (due < minDueTime) {
                minDueTime = due
                earliestIndex = i
            }
        }

        val waitMs = minDueTime - now
        // 超出提前复习容差，或设置了禁止提前学习 (limit <= 0)，进入等待倒计时
        if (learnAheadLimitMs <= 0 || waitMs > learnAheadLimitMs) {
            return QueueSelectionResult.Wait(minDueTime)
        }

        // 在提前复习容差内 (waitMs <= learnAheadLimitMs)，对齐 Anki 规范允许提前复习最早到期项
        return QueueSelectionResult.Next(earliestIndex, items[earliestIndex])
    }

    /**
     * 基础重载 (仅按索引选择，用于测试或无需时间判定的轻量场景)
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
}

