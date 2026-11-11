package com.jian.nemo.feature.statistics

import android.content.Context
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.LearningStats
import com.jian.nemo.core.domain.model.ReviewForecast
import com.jian.nemo.core.domain.model.StudyRecord
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingIconStyle
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.usecase.statistics.GetAllLearnedGrammarsUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetAllLearnedWordsUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetLearnedGrammarsForDateUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetLearnedWordsForDateUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetLearningStatsUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetRecentRecordsUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetReviewForecastUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetReviewedGrammarsForDateUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetReviewedWordsForDateUseCase
import com.jian.nemo.feature.statistics.model.StatisticDisplayItem
import com.jian.nemo.feature.statistics.model.StatisticSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * 今日统计预加载列表快照
 */
data class TodayListsSnapshot(
    val date: Long,
    val words: List<StatisticDisplayItem>,
    val grammars: List<StatisticDisplayItem>
)

/**
 * 历史统计预加载完整数据快照
 */
data class HistoricalStatisticsSnapshot(
    val learnedWords: List<StatisticDisplayItem>,
    val learnedGrammars: List<StatisticDisplayItem>,
    val wordTotalCountMap: Map<String, Int>,
    val grammarTotalCountMap: Map<String, Int>,
    val wordDailyGoal: Int,
    val grammarDailyGoal: Int,
    val wordPredictions: Map<String, LevelPredictionInfo>,
    val grammarPredictions: Map<String, LevelPredictionInfo>,
    val wordOverallPrediction: OverallPredictionSummary,
    val grammarOverallPrediction: OverallPredictionSummary
)

/**
 * 统计数据单例预加载缓存
 *
 * 在进度页进场动画稳定后于后台预热今日统计与历史统计数据，
 * 并以轻量防抖策略跟随底层数据库变化保鲜。
 */
@Singleton
class StatisticsPreloadCache @Inject constructor(
    private val getAllLearnedWordsUseCase: GetAllLearnedWordsUseCase,
    private val getAllLearnedGrammarsUseCase: GetAllLearnedGrammarsUseCase,
    private val getLearnedWordsForDateUseCase: GetLearnedWordsForDateUseCase,
    private val getLearnedGrammarsForDateUseCase: GetLearnedGrammarsForDateUseCase,
    private val getReviewedWordsForDateUseCase: GetReviewedWordsForDateUseCase,
    private val getReviewedGrammarsForDateUseCase: GetReviewedGrammarsForDateUseCase,
    private val getLearningStatsUseCase: GetLearningStatsUseCase,
    private val getRecentRecordsUseCase: GetRecentRecordsUseCase,
    private val getReviewForecastUseCase: GetReviewForecastUseCase,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private val isStarted = AtomicBoolean(false)
    private var warmupJob: Job? = null

    private val todayRefreshVersion = MutableStateFlow(0)

    private val _todayListsState = MutableStateFlow<TodayListsSnapshot?>(null)
    val todayListsState: StateFlow<TodayListsSnapshot?> = _todayListsState.asStateFlow()

    private val _learningStatsState = MutableStateFlow<LearningStats?>(null)
    val learningStatsState: StateFlow<LearningStats?> = _learningStatsState.asStateFlow()

    private val _recent30DaysState = MutableStateFlow<List<StudyRecord>>(emptyList())
    val recent30DaysState: StateFlow<List<StudyRecord>> = _recent30DaysState.asStateFlow()

    private val _recent7DaysState = MutableStateFlow<List<StudyRecord>>(emptyList())
    val recent7DaysState: StateFlow<List<StudyRecord>> = _recent7DaysState.asStateFlow()

    private val _reviewForecastState = MutableStateFlow<List<ReviewForecast>>(emptyList())
    val reviewForecastState: StateFlow<List<ReviewForecast>> = _reviewForecastState.asStateFlow()

    private val _historicalState = MutableStateFlow<HistoricalStatisticsSnapshot?>(null)
    val historicalState: StateFlow<HistoricalStatisticsSnapshot?> = _historicalState.asStateFlow()

    private val _settingIconStyleState = MutableStateFlow(SettingIconStyle.VIBRANT_SOLID)
    val settingIconStyleState: StateFlow<SettingIconStyle> = _settingIconStyleState.asStateFlow()

    /**
     * 错开页面切换动画后启动预加载
     */
    fun warmUpAfterDelay(delayMs: Long = 380L) {
        synchronized(lock) {
            if (isStarted.get() || warmupJob?.isActive == true) return
            warmupJob = scope.launch {
                delay(delayMs)
                startPipelinesOnce()
            }
        }
    }

    /**
     * 立即激活数据收集（跳过预热延迟）
     */
    fun ensureStartedNow() {
        synchronized(lock) {
            warmupJob?.cancel()
            warmupJob = null
        }
        startPipelinesOnce()
    }

    /**
     * 若逻辑今天日期已跨天切换，刷新当日预加载管道
     */
    fun refreshTodayIfDateChanged(actualToday: Long) {
        val cachedDate = _todayListsState.value?.date
        if (cachedDate != null && cachedDate != actualToday) {
            todayRefreshVersion.value += 1
        }
    }

    /**
     * 按指定日期实时查询并转换单词/语法展示列表
     */
    fun observeListsForDate(date: Long): Flow<Pair<List<StatisticDisplayItem>, List<StatisticDisplayItem>>> {
        return combine(
            getLearnedWordsForDateUseCase(date),
            getLearnedGrammarsForDateUseCase(date),
            getReviewedWordsForDateUseCase(date),
            getReviewedGrammarsForDateUseCase(date)
        ) { learnedWords, learnedGrammars, reviewedWords, reviewedGrammars ->
            val learnedWordIds = learnedWords.mapTo(HashSet()) { it.id }
            val learnedGrammarIds = learnedGrammars.mapTo(HashSet()) { it.id }

            val wordItems = learnedWords.map { it.toDisplayItem(StatisticSource.LEARNED) } +
                reviewedWords
                    .filterNot { it.id in learnedWordIds }
                    .map { it.toDisplayItem(StatisticSource.REVIEWED) }

            val grammarItems = learnedGrammars.map { it.toDisplayItem(StatisticSource.LEARNED) } +
                reviewedGrammars
                    .filterNot { it.id in learnedGrammarIds }
                    .map { it.toDisplayItem(StatisticSource.REVIEWED) }

            wordItems to grammarItems
        }.flowOn(Dispatchers.Default)
    }

    private fun startPipelinesOnce() {
        if (!isStarted.compareAndSet(false, true)) return
        scope.launch {
            settingsRepository.settingIconStyleFlow
                .catch { }
                .collect { _settingIconStyleState.value = it }
        }
        scope.launch { observeTodayPipeline() }
        scope.launch { observeHistoricalPipeline() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeTodayPipeline() {
        scope.launch {
            getLearningStatsUseCase()
                .catch { }
                .collect { stats ->
                    _learningStatsState.value = stats
                    refreshRecentRecords()
                }
        }

        scope.launch {
            getReviewForecastUseCase()
                .catch { }
                .collect { forecast ->
                    _reviewForecastState.value = forecast
                }
        }

        scope.launch {
            refreshRecentRecords()
        }

        scope.launch {
            combine(
                settingsRepository.learningDayResetHourFlow,
                todayRefreshVersion
            ) { resetHour, _ ->
                DateTimeUtils.getLearningDay(resetHour)
            }
                .distinctUntilChanged()
                .flatMapLatest { date ->
                    observeListsForDate(date).map { (words, grammars) ->
                        TodayListsSnapshot(date = date, words = words, grammars = grammars)
                    }
                }
                .debounceAfterFirst(200L)
                .catch { }
                .collect { snapshot ->
                    _todayListsState.value = snapshot
                }
        }
    }

    private suspend fun refreshRecentRecords() {
        try {
            val recent30 = getRecentRecordsUseCase(30)
            val recent7 = getRecentRecordsUseCase(7)
            _recent30DaysState.value = recent30
            _recent7DaysState.value = recent7
        } catch (_: Exception) {
        }
    }

    private suspend fun observeHistoricalPipeline() {
        combine(
            getAllLearnedWordsUseCase(),
            getAllLearnedGrammarsUseCase(),
            wordRepository.getWordTotalCountsByLevel(),
            grammarRepository.getGrammarTotalCountsByLevel(),
            settingsRepository.dailyGoalFlow,
            settingsRepository.grammarDailyGoalFlow,
            settingsRepository.wordLevelCompletionDatesFlow,
            settingsRepository.grammarLevelCompletionDatesFlow
        ) { flows ->
            @Suppress("UNCHECKED_CAST")
            RawHistoricalInputs(
                words = flows[0] as List<Word>,
                grammars = flows[1] as List<Grammar>,
                wordTotalCounts = flows[2] as Map<String, Int>,
                grammarTotalCounts = flows[3] as Map<String, Int>,
                wordDailyGoal = flows[4] as Int,
                grammarDailyGoal = flows[5] as Int,
                wordCompletionDates = flows[6] as Map<String, Long>,
                grammarCompletionDates = flows[7] as Map<String, Long>
            )
        }
            .debounceAfterFirst(250L)
            .map { buildHistoricalSnapshot(it) }
            .flowOn(Dispatchers.Default)
            .catch { }
            .collect { snapshot ->
                _historicalState.value = snapshot
            }
    }

    private fun buildHistoricalSnapshot(inputs: RawHistoricalInputs): HistoricalStatisticsSnapshot {
        val standardLevels = listOf("N5", "N4", "N3", "N2", "N1")
        val wordTotalMap = standardLevels.associateWith { level ->
            inputs.wordTotalCounts[level] ?: 0
        }
        val grammarTotalMap = inputs.grammarTotalCounts

        val wordLearnedMap = inputs.words.groupingBy { it.level.uppercase() }.eachCount()
        val grammarLearnedMap = inputs.grammars.groupingBy { it.grammarLevel.uppercase() }.eachCount()

        val wordFallbackDates = standardLevels.associateWith { level ->
            inputs.words.filter { it.level.equals(level, ignoreCase = true) }
                .mapNotNull { it.lastReviewedDate ?: it.firstLearnedDate }
                .maxOrNull() ?: 0L
        }
        val grammarFallbackDates = standardLevels.associateWith { level ->
            inputs.grammars.filter { it.grammarLevel.equals(level, ignoreCase = true) }
                .mapNotNull { it.lastReviewedDate ?: it.firstLearnedDate }
                .maxOrNull() ?: 0L
        }

        val (wordPredictions, wordOverall) = calculatePredictions(
            levels = standardLevels,
            learnedCountMap = wordLearnedMap,
            totalCountMap = wordTotalMap,
            recordedCompletionDates = inputs.wordCompletionDates,
            fallbackCompletionDates = wordFallbackDates,
            dailyGoal = inputs.wordDailyGoal
        ) { level, epochDay ->
            scope.launch {
                settingsRepository.setWordLevelCompletionDate(level, epochDay)
            }
        }

        val (grammarPredictions, grammarOverall) = calculatePredictions(
            levels = standardLevels,
            learnedCountMap = grammarLearnedMap,
            totalCountMap = grammarTotalMap,
            recordedCompletionDates = inputs.grammarCompletionDates,
            fallbackCompletionDates = grammarFallbackDates,
            dailyGoal = inputs.grammarDailyGoal
        ) { level, epochDay ->
            scope.launch {
                settingsRepository.setGrammarLevelCompletionDate(level, epochDay)
            }
        }

        val wordItems = inputs.words.map { it.toDisplayItem() }
        val grammarItems = inputs.grammars.map { it.toDisplayItem() }

        return HistoricalStatisticsSnapshot(
            learnedWords = wordItems,
            learnedGrammars = grammarItems,
            wordTotalCountMap = wordTotalMap,
            grammarTotalCountMap = grammarTotalMap,
            wordDailyGoal = inputs.wordDailyGoal,
            grammarDailyGoal = inputs.grammarDailyGoal,
            wordPredictions = wordPredictions,
            grammarPredictions = grammarPredictions,
            wordOverallPrediction = wordOverall,
            grammarOverallPrediction = grammarOverall
        )
    }

    private fun calculatePredictions(
        levels: List<String>,
        learnedCountMap: Map<String, Int>,
        totalCountMap: Map<String, Int>,
        recordedCompletionDates: Map<String, Long>,
        fallbackCompletionDates: Map<String, Long>,
        dailyGoal: Int,
        onLevelCompletedFirstTime: (String, Long) -> Unit
    ): Pair<Map<String, LevelPredictionInfo>, OverallPredictionSummary> {
        val effectiveDailyGoal = if (dailyGoal > 0) dailyGoal else 1
        val today = LocalDate.now()
        val todayEpochDay = today.toEpochDay()
        val formatter = DateTimeFormatter.ofPattern(context.getString(R.string.stats_history_date_format_ymd))

        val predictionMap = mutableMapOf<String, LevelPredictionInfo>()
        var cumulativeDays = 0
        var hasAnyUncompleted = false

        for (level in levels) {
            val learned = learnedCountMap[level] ?: 0
            val total = totalCountMap[level] ?: 0
            val isCompleted = total > 0 && learned >= total
            val remaining = (total - learned).coerceAtLeast(0)

            if (isCompleted) {
                var epochDay = recordedCompletionDates[level]
                if (epochDay == null) {
                    epochDay = fallbackCompletionDates[level]
                    if (epochDay != null && epochDay > 0) {
                        onLevelCompletedFirstTime(level, epochDay)
                    } else {
                        epochDay = todayEpochDay
                        onLevelCompletedFirstTime(level, epochDay)
                    }
                }

                val dateText = if (epochDay > 0) {
                    try {
                        context.getString(
                            R.string.stats_history_format_level_achieved_date,
                            LocalDate.ofEpochDay(epochDay).format(formatter)
                        )
                    } catch (_: Exception) {
                        context.getString(R.string.stats_history_label_level_achieved)
                    }
                } else {
                    context.getString(R.string.stats_history_label_level_achieved)
                }

                predictionMap[level] = LevelPredictionInfo(
                    level = level,
                    isCompleted = true,
                    completionDateText = dateText,
                    predictedDateText = null,
                    remainingDays = 0,
                    remainingCount = 0
                )
            } else {
                hasAnyUncompleted = true
                val daysNeeded = ceil(remaining.toDouble() / effectiveDailyGoal).toInt().coerceAtLeast(1)
                cumulativeDays += daysNeeded

                val predictedDate = today.plusDays(cumulativeDays.toLong())
                val predictedText = predictedDate.format(formatter)

                predictionMap[level] = LevelPredictionInfo(
                    level = level,
                    isCompleted = false,
                    completionDateText = null,
                    predictedDateText = predictedText,
                    remainingDays = cumulativeDays,
                    remainingCount = remaining
                )
            }
        }

        val overallSummary = OverallPredictionSummary(
            totalRemainingDays = cumulativeDays,
            estimatedCompletionDateText = if (hasAnyUncompleted) today.plusDays(cumulativeDays.toLong()).format(formatter) else null,
            isAllCompleted = !hasAnyUncompleted,
            dailyGoal = effectiveDailyGoal
        )

        return predictionMap to overallSummary
    }

    private fun Word.toDisplayItem(source: StatisticSource = StatisticSource.LEARNED): StatisticDisplayItem {
        return StatisticDisplayItem(
            id = id,
            japanese = japanese,
            hiragana = hiragana,
            chinese = chinese,
            level = level.uppercase(),
            source = source
        )
    }

    private fun Grammar.toDisplayItem(source: StatisticSource = StatisticSource.LEARNED): StatisticDisplayItem {
        return StatisticDisplayItem(
            id = id,
            japanese = grammar,
            hiragana = getFirstConjunction() ?: "",
            chinese = getFirstExplanation(),
            level = grammarLevel.uppercase(),
            source = source
        )
    }

    private fun <T> Flow<T>.debounceAfterFirst(waitMillis: Long): Flow<T> = channelFlow {
        var hasEmittedFirst = false
        collectLatest { value ->
            if (!hasEmittedFirst) {
                hasEmittedFirst = true
                send(value)
            } else {
                delay(waitMillis)
                send(value)
            }
        }
    }

    private data class RawHistoricalInputs(
        val words: List<Word>,
        val grammars: List<Grammar>,
        val wordTotalCounts: Map<String, Int>,
        val grammarTotalCounts: Map<String, Int>,
        val wordDailyGoal: Int,
        val grammarDailyGoal: Int,
        val wordCompletionDates: Map<String, Long>,
        val grammarCompletionDates: Map<String, Long>
    )
}
