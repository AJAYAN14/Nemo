package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.StudyRecord
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 单元测试: GetLearningStatsUseCase
 *
 * 验证获取学习统计数据的业务逻辑
 */
class GetLearningStatsUseCaseTest {

    private lateinit var studyRecordRepository: StudyRecordRepository
    private lateinit var wordRepository: WordRepository
    private lateinit var grammarRepository: GrammarRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var useCase: GetLearningStatsUseCase

    @Before
    fun setup() {
        studyRecordRepository = mockk()
        wordRepository = mockk()
        grammarRepository = mockk()
        settingsRepository = mockk()
        useCase = GetLearningStatsUseCase(
            studyRecordRepository,
            wordRepository,
            grammarRepository,
            settingsRepository
        )

        // Mock DateTimeUtils
        mockkObject(DateTimeUtils)
        every { settingsRepository.learningDayResetHourFlow } returns flowOf(4)
        every { DateTimeUtils.getLearningDay(4) } returns 100L
    }

    private fun setupDefaultMocks(
        todayRecord: StudyRecord? = null,
        dailyStreak: Int = 0,
        totalStudyDays: Int = 0,
        dueWords: Int = 0,
        dueGrammars: Int = 0,
        allRecords: List<StudyRecord> = emptyList(),
        todayLearnedWords: List<Word> = emptyList(),
        todayLearnedGrammars: List<Grammar> = emptyList(),
        allLearnedWords: List<Word> = emptyList(),
        allLearnedGrammars: List<Grammar> = emptyList(),
        wordDailyGoal: Int = 50,
        grammarDailyGoal: Int = 10
    ) {
        every { studyRecordRepository.getTodayRecord() } returns flowOf(todayRecord)
        every { settingsRepository.dailyStreakFlow } returns flowOf(dailyStreak)
        every { settingsRepository.totalStudyDaysFlow } returns flowOf(totalStudyDays)
        every { settingsRepository.dailyGoalFlow } returns flowOf(wordDailyGoal)
        every { settingsRepository.grammarDailyGoalFlow } returns flowOf(grammarDailyGoal)
        every { studyRecordRepository.getAllRecords() } returns flowOf(allRecords)
        every { studyRecordRepository.getTotalStudyDays() } returns flowOf(totalStudyDays)
        every { studyRecordRepository.getRecordsBetween(any(), any()) } returns flowOf(emptyList())
        every { wordRepository.getDueWordsCount(100L) } returns flowOf(dueWords)
        every { grammarRepository.getDueGrammarsCount(100L) } returns flowOf(dueGrammars)
        every { wordRepository.getTodayLearnedWords(100L) } returns flowOf(todayLearnedWords)
        every { grammarRepository.getTodayLearnedGrammars(100L) } returns flowOf(todayLearnedGrammars)
        every { wordRepository.getAllLearnedWords() } returns flowOf(allLearnedWords)
        every { grammarRepository.getAllLearnedGrammars() } returns flowOf(allLearnedGrammars)
        every { wordRepository.getAllWordsByLevel("N1") } returns flowOf(emptyList())
        every { wordRepository.getAllWordsByLevel("N2") } returns flowOf(emptyList())
        every { wordRepository.getAllWordsByLevel("N3") } returns flowOf(emptyList())
        every { wordRepository.getAllWordsByLevel("N4") } returns flowOf(emptyList())
        every { wordRepository.getAllWordsByLevel("N5") } returns flowOf(emptyList())
        every { grammarRepository.getAllGrammars() } returns flowOf(emptyList())
    }

    @Test
    fun `invoke should return learning statistics with all data`() = runTest {
        // Given: 完整的学习数据
        val todayRecord = StudyRecord(
            date = 100L,
            learnedWords = 10,
            reviewedWords = 15,
            learnedGrammars = 5,
            reviewedGrammars = 8,
            testCount = 2
        )

        val mockWords = List(10) { mockk<Word>() }
        val mockGrammars = List(5) { mockk<Grammar>() }
        val allLearnedWords = List(50) { mockk<Word>() }
        val allLearnedGrammars = List(20) { mockk<Grammar>() }

        setupDefaultMocks(
            todayRecord = todayRecord,
            dailyStreak = 7,
            totalStudyDays = 30,
            dueWords = 25,
            dueGrammars = 12,
            allRecords = (94L..100L).map { date ->
                StudyRecord(date, 1, 0, 0, 0, 0)
            },
            todayLearnedWords = mockWords,
            todayLearnedGrammars = mockGrammars,
            allLearnedWords = allLearnedWords,
            allLearnedGrammars = allLearnedGrammars
        )

        // When
        val result = useCase().first()

        // Then
        assertEquals(7, result.dailyStreak)
        assertEquals(30, result.totalStudyDays)
        assertEquals(10, result.todayLearnedWords)
        assertEquals(5, result.todayLearnedGrammars)
        assertEquals(15, result.todayReviewedWords)
        assertEquals(8, result.todayReviewedGrammars)
        assertEquals(50, result.masteredWords)
        assertEquals(20, result.masteredGrammars)
        assertEquals(25, result.dueWords)
        assertEquals(12, result.dueGrammars)
    }

    @Test
    fun `invoke should handle null today record`() = runTest {
        // Given: 今天没有学习记录
        setupDefaultMocks(
            todayRecord = null,
            dailyStreak = 0,
            totalStudyDays = 0,
            dueWords = 0,
            dueGrammars = 0
        )

        // When
        val result = useCase().first()

        // Then: 今日数据应该为0
        assertEquals(0, result.dailyStreak)
        assertEquals(0, result.totalStudyDays)
        assertEquals(0, result.todayLearnedWords)
        assertEquals(0, result.todayLearnedGrammars)
        assertEquals(0, result.todayReviewedWords)
        assertEquals(0, result.todayReviewedGrammars)
    }

    @Test
    fun `invoke should return mastered counts from repository`() = runTest {
        // Given
        val todayRecord = StudyRecord(
            date = 100L,
            learnedWords = 5,
            reviewedWords = 10,
            learnedGrammars = 3,
            reviewedGrammars = 4,
            testCount = 1
        )

        val allLearnedWords = List(100) { mockk<Word>() }
        val allLearnedGrammars = List(30) { mockk<Grammar>() }

        setupDefaultMocks(
            todayRecord = todayRecord,
            dailyStreak = 5,
            totalStudyDays = 15,
            dueWords = 10,
            dueGrammars = 5,
            allLearnedWords = allLearnedWords,
            allLearnedGrammars = allLearnedGrammars
        )

        // When
        val result = useCase().first()

        // Then: 已掌握数量从 repository 获取
        assertEquals(100, result.masteredWords)
        assertEquals(30, result.masteredGrammars)
    }

    @Test
    fun `invoke should handle high streak and study days`() = runTest {
        // Given: 长期学习的用户
        val todayRecord = StudyRecord(
            date = 100L,
            learnedWords = 20,
            reviewedWords = 30,
            learnedGrammars = 10,
            reviewedGrammars = 15,
            testCount = 3
        )

        val mockWords = List(20) { mockk<Word>() }
        val mockGrammars = List(10) { mockk<Grammar>() }

        setupDefaultMocks(
            todayRecord = todayRecord,
            dailyStreak = 365,
            totalStudyDays = 500,
            dueWords = 100,
            dueGrammars = 50,
            allRecords = (100L - 364L..100L).map { date ->
                StudyRecord(date, 1, 0, 0, 0, 0)
            },
            todayLearnedWords = mockWords,
            todayLearnedGrammars = mockGrammars
        )

        // When
        val result = useCase().first()

        // Then
        assertEquals(365, result.dailyStreak)
        assertEquals(500, result.totalStudyDays)
        assertEquals(20, result.todayLearnedWords)
        assertEquals(10, result.todayLearnedGrammars)
        assertEquals(30, result.todayReviewedWords)
        assertEquals(15, result.todayReviewedGrammars)
        assertEquals(100, result.dueWords)
        assertEquals(50, result.dueGrammars)
    }
}
