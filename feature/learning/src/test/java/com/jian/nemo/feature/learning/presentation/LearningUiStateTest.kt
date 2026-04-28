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
}
