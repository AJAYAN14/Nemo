package com.jian.nemo.core.domain.algorithm

import com.jian.nemo.core.domain.model.SrsItem
import com.jian.nemo.core.domain.repository.ReviewLogRepository
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SrsCalculatorImplTest {

    private lateinit var calculator: SrsCalculatorImpl
    private lateinit var reviewLogRepository: ReviewLogRepository

    @Before
    fun setup() {
        reviewLogRepository = mockk(relaxed = true)
        coEvery { reviewLogRepository.getRecentLogs(any()) } returns emptyList()
        calculator = SrsCalculatorImpl(reviewLogRepository)
    }

    // ========== FSRS 基础功能测试 ==========

    @Test
    fun `首次学习 质量4(Good) 应该毕业并设置间隔`() {
        // Given: 未学习的单词
        val item = createNewItem()

        // When: 第一次毕业，质量为4 (Good -> FSRS Rating 3)
        val result = calculator.calculate(item, quality = 4, today = 100)

        // Then:
        assertEquals(1, result.repetitionCount)
        assertTrue("Interval ${result.interval} should be > 0", result.interval > 0)
        assertEquals(100L + result.interval, result.nextReviewDate)
        assertTrue("Stability should be initialized > 0", result.stability > 0f)
        assertTrue("Difficulty should be initialized > 0", result.difficulty > 0f)
    }

    @Test
    fun `首次学习 质量5(Easy) 应该给更长的间隔`() {
        // Given: 未学习的单词
        val item1 = createNewItem()
        val item2 = createNewItem()

        // When:
        val resultGood = calculator.calculate(item1, quality = 4, today = 100)
        val resultEasy = calculator.calculate(item2, quality = 5, today = 100)

        // Then:
        assertTrue("Easy interval > Good interval", resultEasy.interval > resultGood.interval)
        assertTrue("Easy stability > Good stability", resultEasy.stability > resultGood.stability)
    }

    @Test
    fun `首次学习 质量2(Again) 应该失败且重复次数不增加`() {
        val item = createNewItem()
        
        // When: quality = 2 (Again -> FSRS Rating 1)
        val result = calculator.calculate(item, quality = 2, today = 100)

        // Then:
        assertEquals(0, result.repetitionCount)
        assertEquals(0L, result.nextReviewDate) // 新卡失败不设置下次复习日期
        assertTrue(result.stability > 0f)
    }

    @Test
    fun `复习阶段_质量4_间隔应该增加`() {
        // Given: 已毕业1次的单词
        val item = createItemWithRepetition(
            repetitionCount = 1,
            stability = 3.0f,
            difficulty = 5.0f,
            interval = 3,
            nextReviewDate = 100
        )

        // When: 复习，质量4 (Good)
        val result = calculator.calculate(item, quality = 4, today = 100)

        // Then:
        assertEquals(2, result.repetitionCount)
        assertTrue("New interval ${result.interval} should be > 3", result.interval > 3)
        assertTrue("Stability should increase", result.stability > 3.0f)
    }

    @Test
    fun `复习阶段_质量2忘记_应该重新适应`() {
        // Given: 已复习3次，间隔10天
        val item = createItemWithRepetition(
            repetitionCount = 3,
            stability = 10.0f,
            difficulty = 5.0f,
            interval = 10,
            nextReviewDate = 100,
            lastReviewedDate = 90
        )

        // When: 忘记 (Quality = 2 -> Again)
        val result = calculator.calculate(item, quality = 2, today = 100)

        // Then:
        assertEquals(3, result.repetitionCount) // 失败不增加次数
        assertEquals(100L + result.interval, result.nextReviewDate)
        assertTrue("Interval should stay positive", result.interval > 0)
        assertTrue("Stability should stay positive", result.stability > 0f)
    }

    // ========== 延迟复习测试 ==========

    @Test
    fun `延迟且记得(Q=4) 应该给予稳定性奖励`() {
        // Given: 间隔10天，但延迟了10天（总共20天没复习）
        val itemDelayed = createItemWithRepetition(
            repetitionCount = 4,
            stability = 10.0f,
            difficulty = 5.0f,
            interval = 10,
            nextReviewDate = 90, // Today is 100, so delay = 10
            lastReviewedDate = 80 // elapsed = 20
        )
        
        val itemOnTime = createItemWithRepetition(
            repetitionCount = 4,
            stability = 10.0f,
            difficulty = 5.0f,
            interval = 10,
            nextReviewDate = 100, // No delay
            lastReviewedDate = 90 // elapsed = 10
        )

        // When
        val resultDelayed = calculator.calculate(itemDelayed, quality = 4, today = 100)
        val resultOnTime = calculator.calculate(itemOnTime, quality = 4, today = 100)

        // Then: FSRS 延迟且记得会增加更多 stability
        assertTrue("Delayed review should boost stability more", resultDelayed.stability > resultOnTime.stability)
    }

    // ========== 辅助方法 ==========

    private fun createNewItem(): TestSrsItem {
        return TestSrsItem(
            repetitionCount = 0,
            stability = 0f,
            difficulty = 0f,
            interval = 0,
            nextReviewDate = 0,
            lastReviewedDate = null,
            firstLearnedDate = null
        )
    }

    private fun createItemWithRepetition(
        repetitionCount: Int,
        stability: Float,
        difficulty: Float,
        interval: Int = 1,
        nextReviewDate: Long = 100,
        lastReviewedDate: Long = 90
    ): TestSrsItem {
        return TestSrsItem(
            repetitionCount = repetitionCount,
            stability = stability,
            difficulty = difficulty,
            interval = interval,
            nextReviewDate = nextReviewDate,
            lastReviewedDate = lastReviewedDate,
            firstLearnedDate = 80
        )
    }

    private data class TestSrsItem(
        override val repetitionCount: Int,
        override val stability: Float,
        override val difficulty: Float,
        override val interval: Int,
        override val nextReviewDate: Long,
        override val lastReviewedDate: Long?,
        override val firstLearnedDate: Long?
    ) : SrsItem
}
