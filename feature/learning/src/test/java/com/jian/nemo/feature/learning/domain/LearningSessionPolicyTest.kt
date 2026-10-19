package com.jian.nemo.feature.learning.domain

import com.jian.nemo.core.domain.model.SrsItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 学习会话策略及四级切片数学模型单元测试
 */
class LearningSessionPolicyTest {

    private lateinit var policy: LearningSessionPolicy

    @Before
    fun setUp() {
        policy = LearningSessionPolicy()
    }

    private data class TestSrsItem(
        val id: Int,
        override val repetitionCount: Int = 1,
        override val stability: Float = 10f,
        override val difficulty: Float = 5f,
        override val interval: Int = 10,
        override val nextReviewDate: Long = 100L,
        override val lastReviewedDate: Long? = 90L,
        override val firstLearnedDate: Long? = 90L,
        override val type: Int = 2
    ) : SrsItem

    @Test
    fun testCalculateRetrievability_atStability_returnsApproximatelyNinetyPercent() {
        val today = 100L
        // 稳定性为 10 天，上次复习是 90 (经过 10 天)
        val item = TestSrsItem(
            id = 1,
            stability = 10f,
            lastReviewedDate = 90L,
            nextReviewDate = 100L,
            interval = 10
        )

        val r = policy.calculateRetrievability(item, today)
        // FSRS 定义：经过天数等于稳定性时，回忆概率为 0.9 (90%)
        assertEquals(0.9f, r, 0.01f)
    }

    @Test
    fun testCalculateRetrievability_zeroElapsed_returnsOne() {
        val today = 100L
        val item = TestSrsItem(
            id = 1,
            stability = 10f,
            lastReviewedDate = 100L,
            nextReviewDate = 110L,
            interval = 10
        )

        val r = policy.calculateRetrievability(item, today)
        assertEquals(1.0f, r, 0.001f)
    }

    @Test
    fun testCalculateRelativeOverdue() {
        val today = 120L // 逾期 20 天
        val item1 = TestSrsItem(
            id = 1,
            interval = 10,
            nextReviewDate = 100L
        )
        // 逾期 20 天 / 原间隔 10 天 = 2.0 (200% 偏离)
        val ro1 = policy.calculateRelativeOverdue(item1, today)
        assertEquals(2.0f, ro1, 0.001f)

        // 另一个原间隔 100 天，逾期 20 天 = 0.2
        val item2 = TestSrsItem(
            id = 2,
            interval = 100,
            nextReviewDate = 100L
        )
        val ro2 = policy.calculateRelativeOverdue(item2, today)
        assertEquals(0.2f, ro2, 0.001f)
    }

    @Test
    fun testIsItemStale_conditions() {
        val today = 200L

        // 场景 A: 满足逾期 >= 30 且 t >= 3 * I
        // interval=10, lastDate=100 (t=100), nextDate=110 (overdue=90 >= 30, t=100 >= 30) -> Stale
        val staleItem = TestSrsItem(
            id = 1,
            interval = 10,
            lastReviewedDate = 100L,
            nextReviewDate = 110L
        )
        assertTrue(policy.isItemStale(staleItem, today))

        // 场景 B: 逾期不足 30 天 (overdue = 20) -> Not Stale
        val notStaleShortOverdue = TestSrsItem(
            id = 2,
            interval = 10,
            lastReviewedDate = 170L,
            nextReviewDate = 180L
        )
        assertFalse(policy.isItemStale(notStaleShortOverdue, today))

        // 场景 C: 逾期 >= 30 天，但 t < 3 * I
        // interval=100, lastDate=60 (t=140), nextDate=160 (overdue=40 >= 30, 但 140 < 300) -> Not Stale
        val notStaleLargeInterval = TestSrsItem(
            id = 3,
            interval = 100,
            lastReviewedDate = 60L,
            nextReviewDate = 160L
        )
        assertFalse(policy.isItemStale(notStaleLargeInterval, today))
    }

    @Test
    fun testPartitionOverdueItems_completeAndMutuallyExclusive() {
        val today = 200L

        // 1. P3 休眠项: overdue >= 30 && t >= 3 * I
        val staleItem = TestSrsItem(
            id = 100,
            interval = 5,
            lastReviewedDate = 50L,   // t = 150 >= 15
            nextReviewDate = 55L,     // overdue = 145 >= 30
            stability = 5f
        )

        // 2. P0 临界抢救级: 0.6 <= R < 0.9
        // stability=20, lastReviewedDate=180 (t=20 -> R 约 0.9, 微超至 t=25 -> R 在 0.7~0.8)
        val p0ItemA = TestSrsItem(
            id = 1,
            interval = 20,
            lastReviewedDate = 175L,  // t = 25
            nextReviewDate = 195L,    // overdue = 5
            stability = 20f
        )
        val p0ItemB = TestSrsItem(
            id = 2,
            interval = 20,
            lastReviewedDate = 170L,  // t = 30 (R 会低于 p0ItemA)
            nextReviewDate = 190L,    // overdue = 10
            stability = 20f
        )

        // 3. P1 深度逾期级: R < 0.6 (但未达到 Stale 门槛，例如 t < 3*I 或 overdue < 30)
        // interval=15, lastDate=175 (t=25), nextDate=190 (overdue=10 < 30 -> 不进 P3)
        // stability=0.5f (FSRS 6 下 t/S=50，回忆率 R 约为 0.547 < 0.6，确切落入 P1)
        val p1ItemA = TestSrsItem(
            id = 3,
            interval = 15,
            lastReviewedDate = 175L,
            nextReviewDate = 190L, // overdue = 10, RO = 10 / 15 = 0.67
            stability = 0.5f
        )
        val p1ItemB = TestSrsItem(
            id = 4,
            interval = 10,
            lastReviewedDate = 175L,
            nextReviewDate = 185L, // overdue = 15, RO = 15 / 10 = 1.5 (RO 更大，应排在 p1ItemA 前面)
            stability = 0.5f
        )

        // 4. P2 常规当期级: R >= 0.9 (刚到期或长周期)
        val p2ItemA = TestSrsItem(
            id = 5,
            interval = 100,
            lastReviewedDate = 195L,  // t = 5
            nextReviewDate = 198L,    // 先到期
            stability = 100f          // R > 0.95
        )
        val p2ItemB = TestSrsItem(
            id = 6,
            interval = 100,
            lastReviewedDate = 195L,  // t = 5
            nextReviewDate = 200L,    // 后到期
            stability = 100f
        )

        val allItems = listOf(staleItem, p0ItemA, p0ItemB, p1ItemA, p1ItemB, p2ItemA, p2ItemB)
        val partition = policy.partitionOverdueItems(allItems, today)

        // 验证切片总数穷尽无遗漏
        assertEquals(7, partition.totalCount)
        assertEquals(6, partition.activeItems.size)

        // 验证 P3
        assertEquals(1, partition.p3Stale.size)
        assertEquals(100, partition.p3Stale.first().id)

        // 验证 P0 (按 R 升序，越接近 0.6 遗忘边缘越在前)
        assertEquals(2, partition.p0UrgentRescue.size)
        assertEquals(2, partition.p0UrgentRescue[0].id) // p0ItemB 遗忘更深，R 更低，排在前面
        assertEquals(1, partition.p0UrgentRescue[1].id)

        // 验证 P1 (按 RO 降序，偏离越严重越在前)
        assertEquals(2, partition.p1DeepOverdue.size)
        assertEquals(4, partition.p1DeepOverdue[0].id) // p1ItemB 的 RO = 1.5 > p1ItemA 的 0.67
        assertEquals(3, partition.p1DeepOverdue[1].id)

        // 验证 P2 (按 nextReviewDate 升序，FIFO)
        assertEquals(2, partition.p2RegularCurrent.size)
        assertEquals(5, partition.p2RegularCurrent[0].id) // nextReviewDate 198
        assertEquals(6, partition.p2RegularCurrent[1].id) // nextReviewDate 200
    }
}
