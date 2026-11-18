package com.jian.nemo.feature.test.domain.orchestrator

import com.jian.nemo.core.domain.model.CardState
import com.jian.nemo.core.domain.model.CardType
import com.jian.nemo.core.domain.model.MatchableCard
import com.jian.nemo.core.domain.model.TestQuestion
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.AudioEffectType
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.feature.test.TestEffect
import com.jian.nemo.feature.test.TestUiState
import com.jian.nemo.feature.test.domain.handler.CardMatchingHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class CardMatchingOrchestratorSoundEffectTest {

    @Test
    fun `correct card pairing plays single CORRECT sound effect without vibration`() = runBlocking {
        val playedEffects = mutableListOf<AudioEffectType>()
        val emittedEffects = mutableListOf<TestEffect>()
        val orchestrator = createOrchestrator(playedEffects)

        val word1 = sampleWord(1, "猫", "ねこ", "猫")
        val word2 = sampleWord(2, "犬", "いぬ", "狗")
        val termCard = MatchableCard(id = 1, text = "猫", type = CardType.TERM, state = CardState.SELECTED)
        val definitionCard = MatchableCard(id = 1, text = "猫", type = CardType.DEFINITION, state = CardState.DEFAULT)
        var state = TestUiState(
            questions = listOf(TestQuestion.CardMatching(id = 1, pairs = listOf(word1, word2))),
            currentIndex = 0,
            termCards = listOf(termCard, MatchableCard(id = 2, text = "犬", type = CardType.TERM)),
            definitionCards = listOf(definitionCard, MatchableCard(id = 2, text = "狗", type = CardType.DEFINITION)),
            selectedCard = termCard
        )

        orchestrator.onCardSelected(
            scope = this,
            card = definitionCard,
            stateProvider = { state },
            stateUpdater = { reducer -> state = reducer(state) },
            effectEmitter = { effect -> emittedEffects.add(effect) },
            navigation = NoOpNavigation
        )

        delay(900L)

        assertEquals(listOf(AudioEffectType.CORRECT), playedEffects)
        assertTrue(emittedEffects.isEmpty())
    }

    @Test
    fun `incorrect card pairing plays single ERROR sound effect and emits single Vibrate`() = runBlocking {
        val playedEffects = mutableListOf<AudioEffectType>()
        val emittedEffects = mutableListOf<TestEffect>()
        val orchestrator = createOrchestrator(playedEffects)

        val word1 = sampleWord(1, "猫", "ねこ", "猫")
        val word2 = sampleWord(2, "犬", "いぬ", "狗")
        val termCard = MatchableCard(id = 1, text = "猫", type = CardType.TERM, state = CardState.SELECTED)
        val wrongDefinitionCard = MatchableCard(id = 2, text = "狗", type = CardType.DEFINITION, state = CardState.DEFAULT)
        var state = TestUiState(
            questions = listOf(TestQuestion.CardMatching(id = 1, pairs = listOf(word1, word2))),
            currentIndex = 0,
            termCards = listOf(termCard, MatchableCard(id = 2, text = "犬", type = CardType.TERM)),
            definitionCards = listOf(MatchableCard(id = 1, text = "猫", type = CardType.DEFINITION), wrongDefinitionCard),
            selectedCard = termCard
        )

        orchestrator.onCardSelected(
            scope = this,
            card = wrongDefinitionCard,
            stateProvider = { state },
            stateUpdater = { reducer -> state = reducer(state) },
            effectEmitter = { effect -> emittedEffects.add(effect) },
            navigation = NoOpNavigation
        )

        delay(1_600L)

        assertEquals(listOf(AudioEffectType.ERROR), playedEffects)
        assertEquals(listOf(TestEffect.Vibrate), emittedEffects)
    }

    private fun createOrchestrator(playedEffects: MutableList<AudioEffectType>): CardMatchingOrchestrator {
        val audioRepository = Proxy.newProxyInstance(
            AudioRepository::class.java.classLoader,
            arrayOf(AudioRepository::class.java)
        ) { _, method, args ->
            when (method.name) {
                "playSoundEffect" -> {
                    playedEffects.add(args[0] as AudioEffectType)
                    Unit
                }
                else -> Unit
            }
        } as AudioRepository

        val settingsRepository = Proxy.newProxyInstance(
            SettingsRepository::class.java.classLoader,
            arrayOf(SettingsRepository::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getTestAutoPlayAudioFlow" -> flowOf(false)
                "getTestAutoAdvanceFlow" -> flowOf(false)
                "getLearningDayResetHourFlow" -> flowOf(4)
                else -> flowOf(false)
            }
        } as SettingsRepository

        val wrongAnswerRepository = Proxy.newProxyInstance(
            WrongAnswerRepository::class.java.classLoader,
            arrayOf(WrongAnswerRepository::class.java)
        ) { _, _, _ -> Unit } as WrongAnswerRepository

        return CardMatchingOrchestrator(
            cardMatchingHandler = CardMatchingHandler(),
            settingsRepository = settingsRepository,
            wrongAnswerRepository = wrongAnswerRepository,
            audioRepository = audioRepository
        )
    }

    private fun sampleWord(id: Int, japanese: String, hiragana: String, chinese: String) = Word(
        id = id,
        rawId = "w_$id",
        japanese = japanese,
        hiragana = hiragana,
        chinese = chinese,
        level = "N5"
    )

    private object NoOpNavigation : CardMatchingNavigation {
        override fun nextQuestion() = Unit
        override fun finishTest() = Unit
    }
}
