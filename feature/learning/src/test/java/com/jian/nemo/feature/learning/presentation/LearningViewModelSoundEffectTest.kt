package com.jian.nemo.feature.learning.presentation

import android.content.Context
import com.jian.nemo.core.data.manager.CloudBackupManager
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.LearningMode
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.AudioEffectType
import com.jian.nemo.core.domain.repository.ContentReportRepository
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.ReviewLogRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import com.jian.nemo.core.domain.usecase.audio.PlaySoundEffectUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetDueGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetLearningGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetNewGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.GetTodayLearnedGrammarsCountUseCase
import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetLearningStatsUseCase
import com.jian.nemo.core.domain.usecase.word.GetDueWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetLearningWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetNewWordsUseCase
import com.jian.nemo.core.domain.usecase.word.GetTodayLearnedWordsCountUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.feature.learning.domain.LearningQueueManager
import com.jian.nemo.feature.learning.domain.LearningScheduler
import com.jian.nemo.feature.learning.domain.LearningSessionPolicy
import com.jian.nemo.feature.learning.domain.LearningSessionStatsManager
import com.jian.nemo.feature.learning.domain.LearningTtsManager
import com.jian.nemo.feature.learning.domain.LearningUndoHelper
import com.jian.nemo.feature.learning.domain.SessionLoader
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearningViewModelSoundEffectTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var playSoundEffectUseCase: PlaySoundEffectUseCase
    private lateinit var viewModel: LearningViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        playSoundEffectUseCase = mockk(relaxed = true)

        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.isDarkModeFlow } returns emptyFlow()
        every { settingsRepository.settingIconStyleFlow } returns emptyFlow()
        every { settingsRepository.leechThresholdFlow } returns emptyFlow()
        every { settingsRepository.leechActionFlow } returns emptyFlow()
        every { settingsRepository.dailyGoalFlow } returns emptyFlow()
        every { settingsRepository.grammarDailyGoalFlow } returns emptyFlow()
        every { settingsRepository.wordLapsesFlow } returns emptyFlow()
        every { settingsRepository.grammarLapsesFlow } returns emptyFlow()
        every { settingsRepository.learningDayResetHourFlow } returns emptyFlow()
        every { settingsRepository.learningStepsFlow } returns emptyFlow()
        every { settingsRepository.relearningStepsFlow } returns emptyFlow()
        every { settingsRepository.isAutoPlayAudioEnabledFlow } returns emptyFlow()
        every { settingsRepository.isShowAnswerDelayEnabledFlow } returns emptyFlow()
        every { settingsRepository.showAnswerDelayMsFlow } returns emptyFlow()
        every { settingsRepository.isAutoRevealAnswerEnabledFlow } returns emptyFlow()
        every { settingsRepository.autoRevealAnswerMsFlow } returns emptyFlow()
        every { settingsRepository.isGrammarAutoRevealAnswerEnabledFlow } returns emptyFlow()
        every { settingsRepository.grammarAutoRevealAnswerMsFlow } returns emptyFlow()

        val getLearningStatsUseCase = mockk<GetLearningStatsUseCase>(relaxed = true)
        every { getLearningStatsUseCase.invoke() } returns emptyFlow()

        val learningTtsManager = mockk<LearningTtsManager>(relaxed = true)
        every { learningTtsManager.playingAudioId } returns mockk(relaxed = true)

        viewModel = LearningViewModel(
            getNewWordsUseCase = mockk<GetNewWordsUseCase>(relaxed = true),
            getDueWordsUseCase = mockk<GetDueWordsUseCase>(relaxed = true),
            getLearningWordsUseCase = mockk<GetLearningWordsUseCase>(relaxed = true),
            updateWordUseCase = mockk<UpdateWordUseCase>(relaxed = true),
            getTodayLearnedWordsCountUseCase = mockk<GetTodayLearnedWordsCountUseCase>(relaxed = true),
            getNewGrammarsUseCase = mockk<GetNewGrammarsUseCase>(relaxed = true),
            getDueGrammarsUseCase = mockk<GetDueGrammarsUseCase>(relaxed = true),
            getLearningGrammarsUseCase = mockk<GetLearningGrammarsUseCase>(relaxed = true),
            updateGrammarUseCase = mockk<UpdateGrammarUseCase>(relaxed = true),
            getTodayLearnedGrammarsCountUseCase = mockk<GetTodayLearnedGrammarsCountUseCase>(relaxed = true),
            settingsRepository = settingsRepository,
            studyRecordRepository = mockk<StudyRecordRepository>(relaxed = true),
            wordRepository = mockk<WordRepository>(relaxed = true),
            grammarRepository = mockk<GrammarRepository>(relaxed = true),
            srsCalculator = mockk<SrsCalculator>(relaxed = true),
            learningSessionPolicy = mockk<LearningSessionPolicy>(relaxed = true),
            srsIntervalPreview = mockk<SrsIntervalPreview>(relaxed = true),
            sessionLoader = mockk<SessionLoader>(relaxed = true),
            learningQueueManager = LearningQueueManager(),
            learningScheduler = mockk<LearningScheduler>(relaxed = true),
            learningUndoHelper = mockk<LearningUndoHelper>(relaxed = true),
            learningTtsManager = learningTtsManager,
            playSoundEffectUseCase = playSoundEffectUseCase,
            contentReportRepository = mockk<ContentReportRepository>(relaxed = true),
            cloudBackupManager = mockk<CloudBackupManager>(relaxed = true),
            reviewLogRepository = mockk<ReviewLogRepository>(relaxed = true),
            getLearningStatsUseCase = getLearningStatsUseCase,
            sessionStatsManager = mockk<LearningSessionStatsManager>(relaxed = true),
            context = mockk<Context>(relaxed = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `rate word plays mapped sound effects for all four SRS ratings`() {
        val word = sampleWord(1)
        val cases = listOf(
            1 to AudioEffectType.OTHER,
            3 to AudioEffectType.HARD,
            4 to AudioEffectType.GOOD,
            5 to AudioEffectType.EASY
        )

        cases.forEach { (quality, expectedType) ->
            viewModel.update {
                it.copy(
                    learningMode = LearningMode.Word,
                    status = LearningStatus.Learning,
                    wordList = listOf(word),
                    currentIndex = 0,
                    currentWord = word
                )
            }
            viewModel.onEvent(LearningEvent.RateWord(quality))
            verify(exactly = 1) { playSoundEffectUseCase.invoke(expectedType) }
        }
    }

    @Test
    fun `rate grammar plays sound effect when learning grammar item exists`() {
        val grammar = sampleGrammar(10)
        viewModel.update {
            it.copy(
                learningMode = LearningMode.Grammar,
                status = LearningStatus.Learning,
                grammarList = listOf(grammar),
                currentGrammarIndex = 0,
                currentGrammar = grammar
            )
        }

        viewModel.onEvent(LearningEvent.RateGrammar(4))

        verify(exactly = 1) { playSoundEffectUseCase.invoke(AudioEffectType.GOOD) }
    }

    @Test
    fun `rate events do not play sound when non-learning status or empty queue`() {
        val word = sampleWord(2)

        listOf(
            LearningStatus.Loading,
            LearningStatus.Idle,
            LearningStatus.Processing,
            LearningStatus.Waiting,
            LearningStatus.SessionCompleted,
            LearningStatus.Error
        ).forEach { nonLearningStatus ->
            viewModel.update {
                it.copy(
                    learningMode = LearningMode.Word,
                    status = nonLearningStatus,
                    wordList = listOf(word),
                    currentIndex = 0,
                    currentWord = word
                )
            }
            viewModel.onEvent(LearningEvent.RateWord(4))
        }

        viewModel.update {
            it.copy(
                learningMode = LearningMode.Word,
                status = LearningStatus.Learning,
                wordList = emptyList(),
                currentWord = null
            )
        }
        viewModel.onEvent(LearningEvent.RateWord(5))

        viewModel.update {
            it.copy(
                learningMode = LearningMode.Grammar,
                status = LearningStatus.Learning,
                grammarList = emptyList(),
                currentGrammar = null
            )
        }
        viewModel.onEvent(LearningEvent.RateGrammar(3))

        verify(exactly = 0) { playSoundEffectUseCase.invoke(any()) }
    }

    private fun sampleWord(id: Int) = Word(
        id = id,
        rawId = "w_$id",
        japanese = "日本",
        hiragana = "にほん",
        chinese = "日本",
        level = "N5"
    )

    private fun sampleGrammar(id: Int) = Grammar(
        id = id,
        rawId = "g_$id",
        grammar = "〜ている",
        grammarLevel = "N5",
        usages = emptyList()
    )
}
