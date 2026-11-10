package com.jian.nemo.feature.statistics.presentation.curve

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.feature.statistics.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.jian.nemo.core.domain.algorithm.FsrsAlgorithm
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.ReviewLogRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt
import com.jian.nemo.core.domain.repository.SettingIconStyle

/**
 * 时间范围选项
 */
enum class CurveTimeRange(val days: Int, @StringRes val labelRes: Int) {
    SHORT(7, R.string.stats_curve_range_7_days),
    MEDIUM(30, R.string.stats_curve_range_30_days),
    LONG(90, R.string.stats_curve_range_90_days),
    EXTENDED(365, R.string.stats_curve_range_365_days);

    val label: String
        @Composable
        get() = stringResource(labelRes)
}

/**
 * 遗忘曲线界面 UI 状态
 */
data class ForgettingCurveUiState(
    val curveData: ForgettingCurveData? = null,
    val isLoading: Boolean = false,
    val selectedRange: CurveTimeRange = CurveTimeRange.MEDIUM,
    val selectedStage: CurveStage = CurveStage.COMPOSITE,
    val settingIconStyle: SettingIconStyle = SettingIconStyle.SOFT_TRANSLUCENT
)

/**
 * 遗忘曲线界面 ViewModel
 *
 * 支持：
 * 1. 动态切换横坐标时间跨度（7/30/90/365天）
 * 2. 四级 FSRS 互斥记忆分层切换（全库综合 / 初识新词 / 发展巩固 / 深度稳固 / 永久长效）
 * 3. 三元自适应记忆警戒线与临界天数智能推演（目标保留率 × 实际回忆率 × 动态警戒线）
 */
@HiltViewModel
class ForgettingCurveViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository,
    private val reviewLogRepository: ReviewLogRepository,
    private val srsCalculator: SrsCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgettingCurveUiState())
    val uiState: StateFlow<ForgettingCurveUiState> = _uiState.asStateFlow()

    private var cachedStageStats: List<StageStat> = emptyList()
    private var cachedStageMedians: Map<CurveStage, Float?> = emptyMap()
    private var currentTargetRetention: Float = 0.90f
    private var currentActualRetention: Float? = null
    private var dataLoaded = false

    init {
        loadCurveData()
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingIconStyleFlow.collect { style ->
                _uiState.update { it.copy(settingIconStyle = style) }
            }
        }
    }

    /**
     * 切换横坐标时间范围
     */
    fun setTimeRange(range: CurveTimeRange) {
        _uiState.update { it.copy(selectedRange = range) }
        if (dataLoaded) {
            refreshCurveData()
        }
    }

    /**
     * 切换记忆分层阶段
     */
    fun setStage(stage: CurveStage) {
        val stat = cachedStageStats.find { it.stage == stage }
        if (stat != null && !stat.isAvailable && stage != CurveStage.COMPOSITE) {
            // 当前阶段暂无词汇样本，安全忽略
            return
        }
        _uiState.update { it.copy(selectedStage = stage) }
        if (dataLoaded) {
            refreshCurveData()
        }
    }

    /**
     * 自适应动态警戒线计算公式：
     * 动态警戒线 = 目标保留率 + 0.5 * (目标保留率 - 实际回忆率)
     * 钳位区间：[0.75f, 0.95f]
     */
    private fun calculateDynamicAlertThreshold(targetRetention: Float, actualRetention: Float?): Float {
        if (actualRetention == null) return targetRetention
        val delta = targetRetention - actualRetention
        val rawThreshold = targetRetention + (0.5f * delta)
        return rawThreshold.coerceIn(0.75f, 0.95f)
    }

    private fun refreshCurveData() {
        val currentRange = _uiState.value.selectedRange
        val currentStage = _uiState.value.selectedStage
        val stageStability = cachedStageMedians[currentStage]
        val dynamicThreshold = calculateDynamicAlertThreshold(currentTargetRetention, currentActualRetention)
        val curveData = generateCurveData(
            userStability = stageStability,
            maxDays = currentRange.days,
            activeStage = currentStage,
            stages = cachedStageStats,
            alertThreshold = dynamicThreshold,
            actualRetention = currentActualRetention,
            targetRetention = currentTargetRetention
        )
        _uiState.update { it.copy(curveData = curveData) }
    }

    private fun loadCurveData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                wordRepository.getAllLearnedWords(),
                grammarRepository.getAllLearnedGrammars(),
                settingsRepository.targetRetentionFlow
            ) { words, grammars, targetRetention ->
                val actual = reviewLogRepository.getRecentSuccessRate(200)
                Triple(words to grammars, targetRetention, actual)
            }.collect { (items, targetRetention, actual) ->
                val (words, grammars) = items
                currentTargetRetention = targetRetention
                currentActualRetention = actual

                val (medians, stats) = withContext(Dispatchers.Default) {
                    val allStabilities = mutableListOf<Float>()

                    words.forEach { word ->
                        if (word.stability > 0f) allStabilities.add(word.stability)
                    }
                    grammars.forEach { grammar ->
                        if (grammar.stability > 0f) allStabilities.add(grammar.stability)
                    }

                    // 四级分桶计算
                    val youngEarly = allStabilities.filter { it < 3.0f }
                    val youngDev = allStabilities.filter { it in 3.0f..<21.0f }
                    val mature = allStabilities.filter { it in 21.0f..<90.0f }
                    val expert = allStabilities.filter { it >= 90.0f }

                    val medians = mapOf(
                        CurveStage.COMPOSITE to calculateMedian(allStabilities),
                        CurveStage.YOUNG_EARLY to calculateMedian(youngEarly),
                        CurveStage.YOUNG_DEVELOPING to calculateMedian(youngDev),
                        CurveStage.MATURE to calculateMedian(mature),
                        CurveStage.EXPERT to calculateMedian(expert)
                    )

                    val stats = listOf(
                        StageStat(CurveStage.COMPOSITE, allStabilities.size, medians[CurveStage.COMPOSITE] ?: 0f, allStabilities.isNotEmpty()),
                        StageStat(CurveStage.YOUNG_EARLY, youngEarly.size, medians[CurveStage.YOUNG_EARLY] ?: 0f, youngEarly.isNotEmpty()),
                        StageStat(CurveStage.YOUNG_DEVELOPING, youngDev.size, medians[CurveStage.YOUNG_DEVELOPING] ?: 0f, youngDev.isNotEmpty()),
                        StageStat(CurveStage.MATURE, mature.size, medians[CurveStage.MATURE] ?: 0f, mature.isNotEmpty()),
                        StageStat(CurveStage.EXPERT, expert.size, medians[CurveStage.EXPERT] ?: 0f, expert.isNotEmpty())
                    )

                    medians to stats
                }

                cachedStageMedians = medians
                cachedStageStats = stats
                dataLoaded = true

                val currentRange = _uiState.value.selectedRange
                val currentStage = _uiState.value.selectedStage
                val stageStability = medians[currentStage]
                val dynamicThreshold = calculateDynamicAlertThreshold(targetRetention, actual)

                val curveData = generateCurveData(
                    userStability = stageStability,
                    maxDays = currentRange.days,
                    activeStage = currentStage,
                    stages = stats,
                    alertThreshold = dynamicThreshold,
                    actualRetention = actual,
                    targetRetention = targetRetention
                )

                _uiState.update {
                    it.copy(
                        curveData = curveData,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun calculateMedian(list: List<Float>): Float? {
        if (list.isEmpty()) return null
        val sorted = list.sorted()
        val size = sorted.size
        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2f
        } else {
            sorted[size / 2]
        }
    }

    private fun generateCurveData(
        userStability: Float?,
        maxDays: Int,
        activeStage: CurveStage,
        stages: List<StageStat>,
        alertThreshold: Float = 0.90f,
        actualRetention: Float? = null,
        targetRetention: Float = 0.90f
    ): ForgettingCurveData {
        val standardStability = FsrsAlgorithm.DEFAULT_PARAMETERS[2] // ≈ 2.3065
        val standardCurve = generateCurvePoints(standardStability, maxDays)

        val userCurve = if (userStability != null && userStability > 0f) {
            generateCurvePoints(userStability, maxDays)
        } else {
            emptyList()
        }

        val alertInsight = generateAlertInsight(
            stage = activeStage,
            stability = userStability,
            alertThreshold = alertThreshold,
            actualRetention = actualRetention,
            targetRetention = targetRetention
        )

        return ForgettingCurveData(
            standardCurve = standardCurve,
            userCurve = userCurve,
            activeStage = activeStage,
            stages = stages,
            alertInsight = alertInsight,
            alertThreshold = alertThreshold,
            actualRetention = actualRetention,
            targetRetention = targetRetention
        )
    }

    private fun generateAlertInsight(
        stage: CurveStage,
        stability: Float?,
        alertThreshold: Float,
        actualRetention: Float?,
        targetRetention: Float
    ): AlertInsight? {
        if (stability == null || stability <= 0f) return null
        // 临界跌破天数：利用 FSRS 公式反解到达动态警戒线的确切天数
        val days = srsCalculator.nextInterval(stability, alertThreshold).roundToInt().coerceAtLeast(1)
        val thresholdPercent = (alertThreshold * 100).roundToInt()

        return when (stage) {
            CurveStage.YOUNG_EARLY -> AlertInsight(
                level = AlertLevel.CRITICAL,
                title = context.getString(R.string.stats_curve_title_fragile),
                message = context.getString(R.string.stats_curve_msg_fragile, days, thresholdPercent),
                criticalDays = days,
                color = CurveStage.YOUNG_EARLY.themeColor,
                alertThreshold = alertThreshold,
                actualRetention = actualRetention,
                targetRetention = targetRetention
            )
            CurveStage.YOUNG_DEVELOPING -> AlertInsight(
                level = AlertLevel.WARNING,
                title = context.getString(R.string.stats_curve_title_consolidation),
                message = context.getString(R.string.stats_curve_msg_consolidation, days, thresholdPercent),
                criticalDays = days,
                color = CurveStage.YOUNG_DEVELOPING.themeColor,
                alertThreshold = alertThreshold,
                actualRetention = actualRetention,
                targetRetention = targetRetention
            )
            CurveStage.MATURE -> AlertInsight(
                level = AlertLevel.SAFE,
                title = context.getString(R.string.stats_curve_title_solid),
                message = context.getString(R.string.stats_curve_msg_solid, days, thresholdPercent),
                criticalDays = days,
                color = CurveStage.MATURE.themeColor,
                alertThreshold = alertThreshold,
                actualRetention = actualRetention,
                targetRetention = targetRetention
            )
            CurveStage.EXPERT -> AlertInsight(
                level = AlertLevel.SAFE,
                title = context.getString(R.string.stats_curve_title_permanent),
                message = context.getString(R.string.stats_curve_msg_permanent, thresholdPercent, days),
                criticalDays = days,
                color = CurveStage.EXPERT.themeColor,
                alertThreshold = alertThreshold,
                actualRetention = actualRetention,
                targetRetention = targetRetention
            )
            CurveStage.COMPOSITE -> AlertInsight(
                level = if (days < 10) AlertLevel.WARNING else AlertLevel.SAFE,
                title = context.getString(R.string.stats_curve_title_deduction),
                message = context.getString(R.string.stats_curve_msg_deduction, thresholdPercent, days),
                criticalDays = days,
                color = CurveStage.COMPOSITE.themeColor,
                alertThreshold = alertThreshold,
                actualRetention = actualRetention,
                targetRetention = targetRetention
            )
        }
    }

    private fun generateCurvePoints(stability: Float, maxDays: Int): List<CurvePoint> {
        return (0..maxDays).map { day ->
            CurvePoint(
                dayIndex = day,
                retentionRate = if (day == 0) 1f
                else srsCalculator.forgettingCurve(day.toFloat(), stability)
            )
        }
    }
}
