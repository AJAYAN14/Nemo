package com.jian.nemo.feature.learning.presentation.review

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.GrammarUsage
import com.jian.nemo.core.domain.model.ReviewLog
import com.jian.nemo.core.domain.model.SrsItem
import com.jian.nemo.core.domain.model.SrsUpdateResult
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.ReviewLogRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import com.jian.nemo.core.domain.usecase.grammar.GetDueGrammarsUseCase
import com.jian.nemo.core.domain.usecase.grammar.UpdateGrammarUseCase
import com.jian.nemo.core.domain.usecase.review.ProcessReviewResultUseCase
import com.jian.nemo.core.domain.usecase.word.GetDueWordsUseCase
import com.jian.nemo.core.domain.usecase.word.UpdateWordUseCase
import com.jian.nemo.feature.learning.domain.LearningQueueManager
import com.jian.nemo.feature.learning.domain.LearningScheduler
import com.jian.nemo.feature.learning.domain.SrsIntervalPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelRegressionTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `again should keep penalized interval in requeued review item`() = runTest {
        val harness = buildHarness(learnAheadLimitMinutes = 20, penalizedInterval = 2)
        val vm = harness.viewModel

        awaitLoaded(vm)

        vm.rateItem(2)
        awaitNotProcessing(vm)

        val requeued = vm.uiState.value.reviewItems.first() as ReviewPreviewItem.WordItem
        assertEquals(2, requeued.word.interval)
        assertEquals(ReviewStatus.Reviewing, vm.uiState.value.status)
    }

    @Test
    fun `again should enter waiting when learnAhead limit is zero`() = runTest {
        val harness = buildHarness(learnAheadLimitMinutes = 0, penalizedInterval = 2)
        val vm = harness.viewModel

        awaitLoaded(vm)

        vm.rateItem(2)
        awaitNotProcessing(vm)

        assertEquals(ReviewStatus.Waiting, vm.uiState.value.status)
        assertTrue(vm.uiState.value.waitingUntil > System.currentTimeMillis())
    }

    @Test
    fun `due list should be globally mixed by due day`() = runTest {
        val dueWord = Word(
            id = 1,
            rawId = "1",
            japanese = "行く",
            hiragana = "いく",
            chinese = "去",
            level = "N5",
            repetitionCount = 3,
            stability = 5f,
            difficulty = 4f,
            interval = 10,
            nextReviewDate = 20L,
            lastReviewedDate = 0L,
            firstLearnedDate = 0L,
            lastModifiedTime = 0L
        )
        val dueGrammar = Grammar(
            id = 2,
            rawId = "2",
            grammar = "〜ている",
            grammarLevel = "N5",
            usages = listOf(
                GrammarUsage(
                    subtype = null,
                    connection = "Vて + いる",
                    explanation = "表示动作正在进行",
                    notes = null,
                    examples = emptyList()
                )
            ),
            repetitionCount = 2,
            stability = 4f,
            difficulty = 5f,
            interval = 6,
            nextReviewDate = 10L,
            lastReviewedDate = 0L,
            firstLearnedDate = 0L,
            lastModifiedTime = 0L
        )

        val harness = buildHarness(
            learnAheadLimitMinutes = 20,
            penalizedInterval = 2,
            dueWords = listOf(dueWord),
            dueGrammars = listOf(dueGrammar)
        )

        awaitLoaded(harness.viewModel)

        val current = harness.viewModel.uiState.value.currentItem
        assertTrue(current is ReviewPreviewItem.GrammarItem)
    }

    @Test
    fun `leech action bury_today should not skip item`() = runTest {
        val harness = buildHarness(
            learnAheadLimitMinutes = 20,
            penalizedInterval = 2,
            leechThreshold = 1,
            leechAction = "bury_today"
        )

        awaitLoaded(harness.viewModel)
        harness.viewModel.rateItem(2)
        awaitNotProcessing(harness.viewModel)

        val updatedWord = harness.latestWords().first()
        assertFalse(updatedWord.isSkipped)
        assertTrue((harness.viewModel.uiState.value.error ?: "").contains("暂埋"))
    }

    private suspend fun awaitLoaded(vm: ReviewViewModel) {
        repeat(100) {
            if (vm.uiState.value.status != ReviewStatus.Loading) return
            delay(10)
        }
        throw AssertionError("ReviewViewModel did not finish loading in time")
    }

    private suspend fun awaitNotProcessing(vm: ReviewViewModel) {
        repeat(100) {
            if (!vm.uiState.value.isProcessing) return
            delay(10)
        }
        throw AssertionError("ReviewViewModel stayed in processing state too long")
    }

    private data class Harness(
        val viewModel: ReviewViewModel,
        val latestWords: () -> List<Word>
    )

    private fun buildHarness(
        learnAheadLimitMinutes: Int,
        penalizedInterval: Int,
        dueWords: List<Word>? = null,
        dueGrammars: List<Grammar> = emptyList(),
        leechThreshold: Int = 5,
        leechAction: String = "skip"
    ): Harness {
        val defaultDueWord = Word(
            id = 1,
            rawId = "1",
            japanese = "行く",
            hiragana = "いく",
            chinese = "去",
            level = "N5",
            repetitionCount = 3,
            stability = 5f,
            difficulty = 4f,
            interval = 10,
            nextReviewDate = 1L,
            lastReviewedDate = 0L,
            firstLearnedDate = 0L,
            lastModifiedTime = 0L
        )
        val seedWords = dueWords ?: listOf(defaultDueWord)

        val settingsRepository = settingsRepositoryProxy(
            learnAheadLimitMinutes = learnAheadLimitMinutes,
            relearningSteps = "1 10",
            resetHour = 4,
            leechThreshold = leechThreshold,
            leechAction = leechAction
        )

        val wordStore = seedWords.toMutableList()
        val wordRepository = wordRepositoryProxy(wordStore)
        val grammarRepository = grammarRepositoryProxy(dueGrammars)
        val studyRecordRepository = noOpProxy<StudyRecordRepository>()
        val reviewLogRepository = noOpProxy<ReviewLogRepository>()
        val srsCalculator = FakeSrsCalculator(penalizedInterval)

        val getDueWordsUseCase = GetDueWordsUseCase(wordRepository, settingsRepository)
        val getDueGrammarsUseCase = GetDueGrammarsUseCase(grammarRepository, settingsRepository)
        val processReviewResultUseCase = ProcessReviewResultUseCase(
            wordRepository = wordRepository,
            grammarRepository = grammarRepository,
            settingsRepository = settingsRepository,
            studyRecordRepository = studyRecordRepository,
            reviewLogRepository = reviewLogRepository,
            srsCalculator = srsCalculator
        )

        val vm = ReviewViewModel(
            getDueWordsUseCase = getDueWordsUseCase,
            getDueGrammarsUseCase = getDueGrammarsUseCase,
            processReviewResultUseCase = processReviewResultUseCase,
            srsCalculator = srsCalculator,
            settingsRepository = settingsRepository,
            learningScheduler = LearningScheduler(),
            srsIntervalPreview = SrsIntervalPreview(srsCalculator),
            updateWordUseCase = UpdateWordUseCase(wordRepository),
            updateGrammarUseCase = UpdateGrammarUseCase(grammarRepository),
            studyRecordRepository = studyRecordRepository,
            learningQueueManager = LearningQueueManager()
        )

        return Harness(
            viewModel = vm,
            latestWords = { wordStore.toList() }
        )
    }

    private class FakeSrsCalculator(
        private val penalizedInterval: Int
    ) : SrsCalculator {
        override fun calculate(item: SrsItem, quality: Int, today: Long): SrsUpdateResult {
            val interval = if (quality < 3) penalizedInterval else (item.interval + 1)
            return SrsUpdateResult(
                repetitionCount = item.repetitionCount,
                stability = item.stability,
                difficulty = item.difficulty,
                interval = interval,
                nextReviewDate = today + interval,
                lastReviewedDate = today,
                firstLearnedDate = item.firstLearnedDate ?: today,
                type = item.type
            )
        }

        override fun forgettingCurve(elapsedDays: Float, stability: Float): Float {
            return 0.9f // 测试用固定值
        }
    }

    private fun settingsRepositoryProxy(
        learnAheadLimitMinutes: Int,
        relearningSteps: String,
        resetHour: Int,
        leechThreshold: Int,
        leechAction: String
    ): SettingsRepository {
        val handler = { methodName: String, _: Array<Any?>? ->
            when (methodName) {
                "getLearningDayResetHourFlow" -> flowOf(resetHour)
                "getRelearningStepsFlow" -> flowOf(relearningSteps)
                "getLearnAheadLimitFlow" -> flowOf(learnAheadLimitMinutes)
                "getLeechThresholdFlow" -> flowOf(leechThreshold)
                "getLeechActionFlow" -> flowOf(leechAction)
                "getWordLapsesFlow" -> flowOf(emptyMap<Int, Int>())
                "getGrammarLapsesFlow" -> flowOf(emptyMap<Int, Int>())
                "incrementWordLapse", "incrementGrammarLapse" -> Unit
                else -> defaultReturn(methodName)
            }
        }
        return proxy(handler)
    }

    private fun wordRepositoryProxy(dueWords: MutableList<Word>): WordRepository {

        val handler = { methodName: String, args: Array<Any?>? ->
            when (methodName) {
                "getDueWords" -> flowOf(dueWords)
                "updateWord" -> {
                    val updated = args?.firstOrNull() as Word
                    val index = dueWords.indexOfFirst { it.id == updated.id }
                    if (index >= 0) {
                        dueWords[index] = updated
                    } else {
                        dueWords.add(updated)
                    }
                    Result.Success(Unit)
                }
                else -> defaultReturn(methodName)
            }
        }

        return proxy(handler)
    }

    private fun grammarRepositoryProxy(initialDueGrammars: List<Grammar>): GrammarRepository {
        val handler = { methodName: String, _: Array<Any?>? ->
            when (methodName) {
                "getDueGrammars" -> flowOf(initialDueGrammars)
                "updateGrammar" -> Result.Success(Unit)
                else -> defaultReturn(methodName)
            }
        }
        return proxy(handler)
    }

    private inline fun <reified T> noOpProxy(): T {
        return proxy { methodName, _ ->
            when (methodName) {
                "insertLog" -> Unit
                "incrementReviewedWords", "incrementReviewedGrammars" -> Result.Success(Unit)
                else -> defaultReturn(methodName)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T> proxy(
        crossinline handler: (methodName: String, args: Array<Any?>?) -> Any?
    ): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java)
        ) { _, method, args ->
            handler(method.name, args)
        } as T
    }

    private fun defaultReturn(methodName: String): Any? {
        return when {
            methodName.startsWith("get") && methodName.endsWith("Flow") -> flowOf<Any?>(null)
            methodName.startsWith("is") && methodName.endsWith("Flow") -> flowOf(false)
            methodName.startsWith("set") -> Unit
            methodName.startsWith("add") -> Unit
            methodName.startsWith("clear") -> Unit
            methodName.startsWith("increment") -> Result.Success(Unit)
            methodName.startsWith("update") -> Result.Success(Unit)
            methodName.startsWith("mark") -> Result.Success(Unit)
            methodName.startsWith("unmark") -> Result.Success(Unit)
            methodName.startsWith("reset") -> Result.Success(Unit)
            methodName.startsWith("delete") -> Result.Success(Unit)
            methodName.startsWith("upsert") -> Result.Success(Unit)
            methodName.startsWith("insert") -> Unit
            else -> null
        }
    }
}
