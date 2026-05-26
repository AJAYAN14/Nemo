package com.jian.nemo.feature.test.domain.orchestrator

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.CardState
import com.jian.nemo.core.domain.model.CardType
import com.jian.nemo.core.domain.model.FeedbackPanelState
import com.jian.nemo.core.domain.model.MatchableCard
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.domain.model.WrongAnswer
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.feature.test.TestEffect
import com.jian.nemo.feature.test.TestUiState
import com.jian.nemo.feature.test.domain.handler.CardMatchingHandler
import com.jian.nemo.feature.test.domain.handler.CardMatchingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigation actions for Card Matching flow
 */
interface CardMatchingNavigation {
    fun nextQuestion()
    fun finishTest()
}

/**
 * Orchestrates the Card Matching flow (Selection -> Delay -> Pair -> Feedback -> Navigation)
 */
@Singleton
class CardMatchingOrchestrator @Inject constructor(
    private val cardMatchingHandler: CardMatchingHandler,
    private val settingsRepository: SettingsRepository,
    private val wrongAnswerRepository: WrongAnswerRepository,
    private val audioRepository: com.jian.nemo.core.domain.repository.AudioRepository
) {

    private var currentJob: Job? = null
    private var lastClickTime = 0L
    private val debounceTime = 300L

    /**
     * Cancel any ongoing card matching operation
     * Careful: Only call this when navigating away (exit/cleared), not during normal interaction
     */
    fun cancelCurrentJob() {
        currentJob?.cancel()
        currentJob = null
    }

    /**
     * Handle card selection with full async flow
     */
    fun onCardSelected(
        scope: CoroutineScope,
        card: MatchableCard,
        stateProvider: () -> TestUiState,
        stateUpdater: ((TestUiState) -> TestUiState) -> Unit,
        effectEmitter: suspend (TestEffect) -> Unit,
        navigation: CardMatchingNavigation
    ) {
        val currentState = stateProvider()

        // 1. Deadlock Protection: If board is locked (animation playing), ignore inputs and DO NOT cancel job
        if (currentState.isBoardLocked) {
            return
        }

        // 2. Time-based Debounce: detailed double-tap prevention
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < debounceTime) {
            return
        }
        lastClickTime = currentTime

        // Cancel previous job to prevent race conditions (only safe because we checked isBoardLocked)
        currentJob?.cancel()

        currentJob = scope.launch {
            // Re-fetch state inside coroutine to be safe, though we just checked it
            val state = stateProvider()
            val result = cardMatchingHandler.handleCardSelection(
                card = card,
                currentlySelected = state.selectedCard,
                isBoardLocked = state.isBoardLocked
            )

            when (result) {
                is CardMatchingResult.Ignored -> { /* No-op */ }

                is CardMatchingResult.Selected -> {
                    stateUpdater { state ->
                        val updatedState = updateCardState(state, card.id, card.type, CardState.SELECTED)
                        updatedState.copy(selectedCard = card)
                    }
                }

                is CardMatchingResult.Deselected -> {
                    stateUpdater { state ->
                        val updatedState = updateCardState(state, card.id, card.type, CardState.DEFAULT)
                        updatedState.copy(selectedCard = null)
                    }
                }

                is CardMatchingResult.SameColumnReplaced -> {
                    stateUpdater { state ->
                        var tempState = updateCardState(state, result.oldCard.id, result.oldCard.type, CardState.DEFAULT)
                        tempState = updateCardState(tempState, result.newCard.id, result.newCard.type, CardState.SELECTED)
                        tempState.copy(selectedCard = result.newCard)
                    }
                }

                is CardMatchingResult.PairingAttempt -> {
                    handleCardPairing(
                        firstCard = result.firstCard,
                        secondCard = result.secondCard,
                        stateProvider = stateProvider,
                        stateUpdater = stateUpdater,
                        effectEmitter = effectEmitter,
                        navigation = navigation
                    )
                }
            }
        }
    }

    private suspend fun handleCardPairing(
        firstCard: MatchableCard,
        secondCard: MatchableCard,
        stateProvider: () -> TestUiState,
        stateUpdater: ((TestUiState) -> TestUiState) -> Unit,
        effectEmitter: suspend (TestEffect) -> Unit,
        navigation: CardMatchingNavigation
    ) {
        // 1. Lock board immediately
        stateUpdater {
            var s = it.copy(isBoardLocked = true)
            s = updateCardState(s, secondCard.id, secondCard.type, CardState.SELECTED)
            s
        }

        // 2. Wait for visual feedback
        delay(300)

        // 3. Verify pairing
        val validation = cardMatchingHandler.validatePairing(firstCard, secondCard)
        val state = stateProvider()
        val currentQuestion = state.currentQuestion as? TestQuestion.CardMatching
        val totalPairs = currentQuestion?.pairs?.size ?: 0

        if (validation.isCorrect) {
            handleCorrectPairing(firstCard, secondCard, stateProvider, stateUpdater, effectEmitter, navigation, currentQuestion, totalPairs)
        } else {
            handleIncorrectPairing(firstCard, secondCard, stateProvider, stateUpdater, effectEmitter, navigation, currentQuestion)
        }
    }

    private suspend fun handleCorrectPairing(
        firstCard: MatchableCard,
        secondCard: MatchableCard,
        stateProvider: () -> TestUiState,
        stateUpdater: ((TestUiState) -> TestUiState) -> Unit,
        effectEmitter: suspend (TestEffect) -> Unit,
        navigation: CardMatchingNavigation,
        currentQuestion: TestQuestion.CardMatching?,
        totalPairs: Int
    ) {
        // 1. Show CORRECT state
        stateUpdater { state ->
            var s = updateCardState(state, firstCard.id, firstCard.type, CardState.CORRECT)
            s = updateCardState(s, secondCard.id, secondCard.type, CardState.CORRECT)
            s.copy(matchedPairsCount = state.matchedPairsCount + 1)
        }

        effectEmitter(TestEffect.PlaySound(isCorrect = true))

        // 自动朗读配对成功的单词假名
        val pairedWord = currentQuestion?.pairs?.find { it.id == firstCard.id }
        if (pairedWord != null) {
            if (settingsRepository.testAutoPlayAudioFlow.first()) {
                audioRepository.playTts(pairedWord.hiragana)
            }
        }

        delay(400)

        // 2. Show MATCHED state (Disappear/Grey out)
        stateUpdater { state ->
            var s = updateCardState(state, firstCard.id, firstCard.type, CardState.MATCHED)
            s = updateCardState(s, secondCard.id, secondCard.type, CardState.MATCHED)
            s.copy(
                selectedCard = null,
                isBoardLocked = false
            )
        }

        // 3. Check completion
        // Need to refetch state to get latest matchedPairsCount
        val newState = stateProvider()
        if (cardMatchingHandler.isAllMatched(newState.matchedPairsCount, totalPairs) && currentQuestion != null) {
            // Mark question as answered/correct
            stateUpdater { state ->
                val updatedQuestion = currentQuestion.copy(
                    isAnswered = true,
                    isCorrect = true
                )
                val updatedQuestions = state.questions.toMutableList()
                if (state.currentIndex in updatedQuestions.indices) {
                   updatedQuestions[state.currentIndex] = updatedQuestion
                }

                state.copy(
                    questions = updatedQuestions,
                    feedbackPanelState = FeedbackPanelState.COMPLETE
                )
            }

            handleCardMatchingComplete(stateProvider, stateUpdater, navigation)
        }
    }

    private suspend fun handleIncorrectPairing(
        firstCard: MatchableCard,
        secondCard: MatchableCard,
        stateProvider: () -> TestUiState,
        stateUpdater: ((TestUiState) -> TestUiState) -> Unit,
        effectEmitter: suspend (TestEffect) -> Unit,
        navigation: CardMatchingNavigation,
        currentQuestion: TestQuestion.CardMatching?
    ) {
        // 1. Show INCORRECT state
        stateUpdater { state ->
            var s = updateCardState(state, firstCard.id, firstCard.type, CardState.INCORRECT)
            s = updateCardState(s, secondCard.id, secondCard.type, CardState.INCORRECT)
            s.copy(cardMatchingWrongCount = state.cardMatchingWrongCount + 1)
        }

        effectEmitter(TestEffect.PlaySound(isCorrect = false))
        effectEmitter(TestEffect.Vibrate)

        delay(1200)

        // Refetch state for wrong count
        val newState = stateProvider()

        if (currentQuestion != null && cardMatchingHandler.isFailure(newState.cardMatchingWrongCount)) {
            handleCardMatchingFailure(currentQuestion, stateProvider, stateUpdater, navigation)
        } else {
            // Reset to default
            stateUpdater { state ->
                var s = updateCardState(state, firstCard.id, firstCard.type, CardState.DEFAULT)
                s = updateCardState(s, secondCard.id, secondCard.type, CardState.DEFAULT)
                s.copy(
                    selectedCard = null,
                    isBoardLocked = false,
                    feedbackPanelState = FeedbackPanelState.INCORRECT
                )
            }

            delay(500)
            stateUpdater { it.copy(feedbackPanelState = FeedbackPanelState.HIDDEN) }
        }
    }

    private suspend fun handleCardMatchingComplete(
        stateProvider: () -> TestUiState,
        stateUpdater: ((TestUiState) -> TestUiState) -> Unit,
        navigation: CardMatchingNavigation
    ) {
        val autoAdvance = settingsRepository.testAutoAdvanceFlow.first()

        if (autoAdvance) {
            stateUpdater { it.copy(isAutoAdvancing = true) }
            delay(1500)

            val state = stateProvider()
            if (state.isLastQuestion) {
                navigation.finishTest()
            } else {
                navigation.nextQuestion()
            }

            stateUpdater { it.copy(isAutoAdvancing = false) }
        }
    }

    private suspend fun handleCardMatchingFailure(
        currentQuestion: TestQuestion.CardMatching,
        stateProvider: () -> TestUiState,
        stateUpdater: ((TestUiState) -> TestUiState) -> Unit,
        navigation: CardMatchingNavigation
    ) {
        val updatedQuestion = currentQuestion.copy(
            isAnswered = true,
            isCorrect = false
        )

        stateUpdater { state ->
            val updatedQuestions = state.questions.toMutableList()
             if (state.currentIndex in updatedQuestions.indices) {
                updatedQuestions[state.currentIndex] = updatedQuestion
            }
            state.copy(
                questions = updatedQuestions,
                feedbackPanelState = FeedbackPanelState.INCORRECT
            )
        }

        // Save wrong answers
        val resetHour = settingsRepository.learningDayResetHourFlow.first()
        val today = DateTimeUtils.getLearningDay(resetHour)

        currentQuestion.pairs.forEach { word ->
            wrongAnswerRepository.insertWrongAnswer(
                WrongAnswer(
                    id = 0,
                    wordId = word.id,
                    word = null,
                    testMode = "card_matching",
                    userAnswer = "",
                    correctAnswer = word.chinese,
                    timestamp = today
                )
            )
        }

        delay(1500)

        val state = stateProvider()
        if (state.isLastQuestion) {
            navigation.finishTest()
        } else {
            navigation.nextQuestion()
        }
    }

    private fun updateCardState(
        state: TestUiState,
        cardId: Int,
        cardType: CardType,
        newState: CardState
    ): TestUiState {
        return if (cardType == CardType.DEFINITION) {
            val updatedCards = state.definitionCards.map { card ->
                if (card.id == cardId && card.type == cardType) {
                    card.copy(state = newState)
                } else {
                    card
                }
            }
            state.copy(definitionCards = updatedCards)
        } else {
            val updatedCards = state.termCards.map { card ->
                if (card.id == cardId && card.type == cardType) {
                    card.copy(state = newState)
                } else {
                    card
                }
            }
            state.copy(termCards = updatedCards)
        }
    }
}
