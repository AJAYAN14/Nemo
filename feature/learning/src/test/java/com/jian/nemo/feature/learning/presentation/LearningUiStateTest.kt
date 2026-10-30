package com.jian.nemo.feature.learning.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningUiStateTest {

    @Test
    fun `daily goal met should not block when word card is pending`() {
        val state = LearningUiState(
            learningMode = LearningMode.Word,
            dailyGoal = 20,
            completedToday = 20,
            currentWord = com.jian.nemo.core.domain.model.Word(
                id = 1,
                rawId = "1",
                japanese = "行く",
                hiragana = "いく",
                chinese = "去",
                level = "N5"
            ),
            wordList = listOf(
                com.jian.nemo.core.domain.model.Word(
                    id = 1,
                    rawId = "1",
                    japanese = "行く",
                    hiragana = "いく",
                    chinese = "去",
                    level = "N5"
                )
            )
        )

        assertTrue(state.hasPendingItems)
        assertFalse(state.shouldShowDailyGoalMet)
    }

    @Test
    fun `daily goal met should show completion when no pending word items`() {
        val state = LearningUiState(
            learningMode = LearningMode.Word,
            dailyGoal = 20,
            completedToday = 20,
            currentWord = null,
            wordList = emptyList()
        )

        assertFalse(state.hasPendingItems)
        assertTrue(state.shouldShowDailyGoalMet)
    }

    @Test
    fun `daily goal not met should not show completion`() {
        val state = LearningUiState(
            learningMode = LearningMode.Word,
            dailyGoal = 20,
            completedToday = 13,
            currentWord = null,
            wordList = emptyList()
        )

        assertFalse(state.shouldShowDailyGoalMet)
    }

    @Test
    fun `daily goal met should not block when grammar card is pending`() {
        val state = LearningUiState(
            learningMode = LearningMode.Grammar,
            dailyGoal = 10,
            completedToday = 10,
            currentGrammar = com.jian.nemo.core.domain.model.Grammar(
                id = 1,
                rawId = "1",
                grammar = "〜ている",
                grammarLevel = "N5",
                usages = emptyList()
            ),
            grammarList = listOf(
                com.jian.nemo.core.domain.model.Grammar(
                    id = 1,
                    rawId = "1",
                    grammar = "〜ている",
                    grammarLevel = "N5",
                    usages = emptyList()
                )
            )
        )

        assertTrue(state.hasPendingItems)
        assertFalse(state.shouldShowDailyGoalMet)
    }

    @Test
    fun `sessionTotalCount should not double count relearned review words`() {
        // 用户场景：10词一组，4个新词，6个复习词，其中1个点了重来并攻克
        val state = LearningUiState(
            sessionNewCount = 4,
            sessionReviewCount = 6,
            sessionRelearnCount = 1
        )

        // 独立完成总词数必须为 10，严禁变为 11
        org.junit.Assert.assertEquals(10, state.sessionTotalCount)
        // 顺畅旧词巩固数为 6 - 1 = 5
        org.junit.Assert.assertEquals(5, state.smoothReviewCount)
    }

    @Test
    fun `sessionTotalCount edge cases check`() {
        // 纯新词
        val allNewState = LearningUiState(sessionNewCount = 10, sessionReviewCount = 0, sessionRelearnCount = 0)
        org.junit.Assert.assertEquals(10, allNewState.sessionTotalCount)
        org.junit.Assert.assertEquals(0, allNewState.smoothReviewCount)

        // 全部顺畅复习
        val allReviewState = LearningUiState(sessionNewCount = 0, sessionReviewCount = 10, sessionRelearnCount = 0)
        org.junit.Assert.assertEquals(10, allReviewState.sessionTotalCount)
        org.junit.Assert.assertEquals(10, allReviewState.smoothReviewCount)

        // 全部复习且全部攻克重学
        val allRelearnState = LearningUiState(sessionNewCount = 0, sessionReviewCount = 5, sessionRelearnCount = 5)
        org.junit.Assert.assertEquals(5, allRelearnState.sessionTotalCount)
        org.junit.Assert.assertEquals(0, allRelearnState.smoothReviewCount)
    }
}
