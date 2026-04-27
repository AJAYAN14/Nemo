package com.jian.nemo.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.LearningStats
import com.jian.nemo.core.domain.model.StudyRecord
import com.jian.nemo.core.domain.usecase.statistics.GetLearningStatsUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetRecentRecordsUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetLearnedGrammarsForDateUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetLearnedWordsForDateUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetReviewedGrammarsForDateUseCase
import com.jian.nemo.core.domain.usecase.statistics.GetReviewedWordsForDateUseCase
import com.jian.nemo.core.domain.model.ReviewForecast
import com.jian.nemo.core.domain.usecase.statistics.GetReviewForecastUseCase
import com.jian.nemo.feature.statistics.model.StatisticDisplayItem
import com.jian.nemo.feature.statistics.model.StatisticSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 统计界面 ViewModel
 *
 * 管理学习统计数据的状态
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getLearningStatsUseCase: GetLearningStatsUseCase,
    private val getRecentRecordsUseCase: GetRecentRecordsUseCase,
    private val getLearnedWordsForDateUseCase: GetLearnedWordsForDateUseCase,
    private val getLearnedGrammarsForDateUseCase: GetLearnedGrammarsForDateUseCase,
    private val getReviewedWordsForDateUseCase: GetReviewedWordsForDateUseCase,
    private val getReviewedGrammarsForDateUseCase: GetReviewedGrammarsForDateUseCase,
    private val getReviewForecastUseCase: GetReviewForecastUseCase,
    private val settingsRepository: com.jian.nemo.core.domain.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
        observeSelectedDate()
        syncTodayDate()
    }

    /**
     * 同步今天的初始日期（基于重置时间）
     */
    private fun syncTodayDate() {
        viewModelScope.launch {
            val resetHour = settingsRepository.learningDayResetHourFlow.first()
            val today = DateTimeUtils.getLearningDay(resetHour)
            _uiState.update { it.copy(selectedDate = today) }
        }
    }

    /**
     * 加载统计数据
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // 加载学习统计 (现在是一个 Flow)
                launch {
                    getLearningStatsUseCase().collect { stats ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                stats = stats,
                                error = null
                            )
                        }
                    }
                }

                // 加载最近30天记录（用于趋势图）
                val recent30Days = getRecentRecordsUseCase(30)

                // 加载最近7天记录（用于周视图）
                val recent7Days = getRecentRecordsUseCase(7)

                 _uiState.update {
                    it.copy(
                        recentRecords = recent30Days,
                        weekRecords = recent7Days
                    )
                }

                // 加载复习预测
                launch {
                    getReviewForecastUseCase().collect { forecast ->
                        _uiState.update { it.copy(reviewForecast = forecast) }
                        // 更新选中日期的复习数量（如果选中了未来日期）
                        updateSelectedDateReviewCount(_uiState.value.selectedDate, forecast)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败: ${e.message}"
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeSelectedDate() {
        viewModelScope.launch {
            _uiState.map { it.selectedDate }
                .distinctUntilChanged()
                .flatMapLatest { date ->
                    combine(
                        getLearnedWordsForDateUseCase(date),
                        getLearnedGrammarsForDateUseCase(date),
                        getReviewedWordsForDateUseCase(date),
                        getReviewedGrammarsForDateUseCase(date)
                    ) { learnedWords, learnedGrammars, reviewedWords, reviewedGrammars ->
                        val learnedWordItems = learnedWords.map { word ->
                            StatisticDisplayItem(
                                id = word.id,
                                japanese = word.japanese,
                                hiragana = word.hiragana,
                                chinese = word.chinese,
                                level = word.level.uppercase(),
                                source = StatisticSource.LEARNED
                            )
                        }

                        val reviewedWordItems = reviewedWords
                            .filterNot { reviewed -> learnedWords.any { it.id == reviewed.id } }
                            .map { word ->
                                StatisticDisplayItem(
                                    id = word.id,
                                    japanese = word.japanese,
                                    hiragana = word.hiragana,
                                    chinese = word.chinese,
                                    level = word.level.uppercase(),
                                    source = StatisticSource.REVIEWED
                                )
                            }

                        val learnedGrammarItems = learnedGrammars.map { grammar ->
                            StatisticDisplayItem(
                                id = grammar.id,
                                japanese = grammar.grammar,
                                hiragana = grammar.getFirstConjunction() ?: "",
                                chinese = grammar.getFirstExplanation(),
                                level = grammar.grammarLevel.uppercase(),
                                source = StatisticSource.LEARNED
                            )
                        }

                        val reviewedGrammarItems = reviewedGrammars
                            .filterNot { reviewed -> learnedGrammars.any { it.id == reviewed.id } }
                            .map { grammar ->
                                StatisticDisplayItem(
                                    id = grammar.id,
                                    japanese = grammar.grammar,
                                    hiragana = grammar.getFirstConjunction() ?: "",
                                    chinese = grammar.getFirstExplanation(),
                                    level = grammar.grammarLevel.uppercase(),
                                    source = StatisticSource.REVIEWED
                                )
                            }

                        Triple(
                            date,
                            learnedWordItems + reviewedWordItems,
                            learnedGrammarItems + reviewedGrammarItems
                        )
                    }
                }
                .collect { (date, wordItems, grammarItems) ->
                    val forecast = _uiState.value.reviewForecast
                    updateSelectedDateReviewCount(date, forecast)

                    _uiState.update {
                        it.copy(
                            todaysWords = wordItems,
                            todaysGrammars = grammarItems
                        )
                    }
                }
        }
    }

    private fun updateSelectedDateReviewCount(date: Long, forecast: List<com.jian.nemo.core.domain.model.ReviewForecast>) {
        viewModelScope.launch {
            val resetHour = settingsRepository.learningDayResetHourFlow.first()
            val today = DateTimeUtils.getLearningDay(resetHour)
            val reviewCount = if (date == today) {
                // 今天: 使用实时待复习数量 (从 stats 中获取)
                val stats = _uiState.value.stats
                (stats?.dueWords ?: 0) + (stats?.dueGrammars ?: 0)
            } else if (date > today) {
                // 未来: 使用预测数据
                forecast.find { it.date == date }?.totalCount ?: 0
            } else {
                // 过去: 0
                0
            }

            _uiState.update { it.copy(selectedDateReviewCount = reviewCount) }
        }
    }

    /**
     * 选择日期
     */
    fun selectDate(date: Long) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    /**
     * 刷新统计数据
     */
    fun refresh() {
        loadStatistics()
        // 重新触发日期观察
        val currentDate = _uiState.value.selectedDate
        // 简单触发方式：设置相同日期不会触发 distinctUntilChanged，但我们主要刷新的是数据源
        // 由于 loadStatistics 重新加载了 forecast 和 stats，
        // 而 words/grammars 是通过 Flow 监听的，数据库变动会自动推送，所以不需要手动刷新
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 统计界面 UI 状态
 */
data class StatisticsUiState(
    /**
     * 是否正在加载
     */
    val isLoading: Boolean = false,

    /**
     * 学习统计数据
     */
    val stats: LearningStats? = null,

    /**
     * 最近30天的学习记录（用于热力图）
     */
    val recentRecords: List<StudyRecord> = emptyList(),

    /**
     * 最近7天的学习记录（用于趋势图）
     */
    val weekRecords: List<StudyRecord> = emptyList(),

    /**
     * 选中日期 (Epoch Day)
     */
    val selectedDate: Long = 0L,

    /**
     * 复习预测数据
     */
    val reviewForecast: List<com.jian.nemo.core.domain.model.ReviewForecast> = emptyList(),

    /**
     * 选中日期的待复习数量
     */
    val selectedDateReviewCount: Int = 0,

    /**
     * 今日学习的单词列表 (实际上是 selectedDate 的单词列表)
     */
    val todaysWords: List<StatisticDisplayItem> = emptyList(),

    /**
     * 今日学习的语法列表 (实际上是 selectedDate 的语法列表)
     */
    val todaysGrammars: List<StatisticDisplayItem> = emptyList(),

    /**
     * 错误消息
     */
    val error: String? = null
)
