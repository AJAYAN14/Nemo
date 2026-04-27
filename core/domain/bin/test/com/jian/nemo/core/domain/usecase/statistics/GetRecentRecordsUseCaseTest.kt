package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.StudyRecord
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 单元测试: GetRecentRecordsUseCase
 *
 * 验证获取最近学习记录的业务逻辑
 */
class GetRecentRecordsUseCaseTest {

    private lateinit var studyRecordRepository: StudyRecordRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: GetRecentRecordsUseCase

    @Before
    fun setup() {
        studyRecordRepository = mockk()
        settingsRepository = mockk()
        useCase = GetRecentRecordsUseCase(studyRecordRepository, settingsRepository)

        // Mock DateTimeUtils
        mockkObject(DateTimeUtils)
        every { settingsRepository.learningDayResetHourFlow } returns flowOf(4)
        every { DateTimeUtils.getLearningDay(4) } returns 100L
    }

    @Test
    fun `invoke should return recent study records with default 30 days`() = runTest {
        // Given: 最近30天的记录
        val recentRecords = listOf(
            createStudyRecord(date = 100L, learnedWords = 10),
            createStudyRecord(date = 99L, learnedWords = 8),
            createStudyRecord(date = 98L, learnedWords = 12),
            createStudyRecord(date = 80L, learnedWords = 5),
            createStudyRecord(date = 71L, learnedWords = 7) // 30天前开始
        )

        every { studyRecordRepository.getRecordsBetween(71L, 100L) } returns flowOf(recentRecords)

        // When: 使用默认参数
        val result = useCase()

        // Then
        assertEquals(5, result.size)
        assertEquals(100L, result[0].date)
        assertEquals(71L, result[4].date)
    }

    @Test
    fun `invoke should return records for custom days parameter`() = runTest {
        // Given: 最近7天的记录
        val sevenDaysRecords = listOf(
            createStudyRecord(date = 100L, learnedWords = 10),
            createStudyRecord(date = 99L, learnedWords = 8),
            createStudyRecord(date = 98L, learnedWords = 12),
            createStudyRecord(date = 97L, learnedWords = 6),
            createStudyRecord(date = 96L, learnedWords = 9),
            createStudyRecord(date = 95L, learnedWords = 11),
            createStudyRecord(date = 94L, learnedWords = 7)
        )

        every { studyRecordRepository.getRecordsBetween(94L, 100L) } returns flowOf(sevenDaysRecords)

        // When: 指定7天
        val result = useCase(days = 7)

        // Then
        assertEquals(7, result.size)
        assertEquals(100L, result[0].date)
        assertEquals(94L, result[6].date)
    }

    @Test
    fun `invoke should handle empty records`() = runTest {
        // Given: 没有学习记录
        every { studyRecordRepository.getRecordsBetween(71L, 100L) } returns flowOf(emptyList())

        // When
        val result = useCase(days = 30)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `invoke should calculate correct start date for given days`() = runTest {
        // Given
        val records = listOf(
            createStudyRecord(date = 100L, learnedWords = 5),
            createStudyRecord(date = 90L, learnedWords = 3),
            createStudyRecord(date = 86L, learnedWords = 4) // 15天前开始(100 - 15 + 1 = 86)
        )

        every { studyRecordRepository.getRecordsBetween(86L, 100L) } returns flowOf(records)

        // When: 获取15天的记录
        val result = useCase(days = 15)

        // Then: 验证日期范围
        assertEquals(3, result.size)
        assertTrue(result.all { it.date >= 86L && it.date <= 100L })
    }

    @Test
    fun `invoke should handle single day request`() = runTest {
        // Given: 只要今天的记录
        val todayRecord = listOf(
            createStudyRecord(date = 100L, learnedWords = 10)
        )

        every { studyRecordRepository.getRecordsBetween(100L, 100L) } returns flowOf(todayRecord)

        // When
        val result = useCase(days = 1)

        // Then
        assertEquals(1, result.size)
        assertEquals(100L, result[0].date)
    }

    @Test
    fun `invoke should handle records with varying activity levels`() = runTest {
        // Given: 不同活跃度的记录
        val mixedRecords = listOf(
            createStudyRecord(date = 100L, learnedWords = 50, reviewedWords = 100), // 高活跃
            createStudyRecord(date = 99L, learnedWords = 0, reviewedWords = 0), // 无活动
            createStudyRecord(date = 98L, learnedWords = 5, reviewedWords = 10), // 低活跃
            createStudyRecord(date = 97L, learnedWords = 20, reviewedWords = 30) // 中等活跃
        )

        every { studyRecordRepository.getRecordsBetween(97L, 100L) } returns flowOf(mixedRecords)

        // When
        val result = useCase(days = 4)

        // Then: 应该返回所有记录,包括无活动的
        assertEquals(4, result.size)
        assertEquals(50, result[0].learnedWords)
        assertEquals(0, result[1].learnedWords)
        assertEquals(5, result[2].learnedWords)
        assertEquals(20, result[3].learnedWords)
    }

    private fun createStudyRecord(
        date: Long,
        learnedWords: Int = 0,
        reviewedWords: Int = 0,
        learnedGrammars: Int = 0,
        reviewedGrammars: Int = 0
    ) = StudyRecord(
        date = date,
        learnedWords = learnedWords,
        reviewedWords = reviewedWords,
        learnedGrammars = learnedGrammars,
        reviewedGrammars = reviewedGrammars,
        testCount = 0
    )
}
