package com.jian.nemo.feature.statistics.presentation.curve

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jian.nemo.core.domain.algorithm.FsrsAlgorithm
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.service.SrsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 时间范围选项
 */
enum class CurveTimeRange(val days: Int, val label: String) {
    SHORT(7, "短期 (7天)"),
    MEDIUM(30, "中期 (30天)"),
    LONG(90, "中长期 (90天)"),
    EXTENDED(365, "长期 (365天)")
}

/**
 * 遗忘曲线界面 ViewModel
 *
 * 两条曲线都使用 SrsCalculator.forgettingCurve()（含个性化参数微调）：
 * - 标准曲线：S = FSRS 默认初始 stability（Good 评分 w[2] ≈ 2.3 天）— 代表未经优化的基准
 * - 用户曲线：S = 用户所有已学项目的平均 stability — 代表经过复习强化后的实际记忆保持
 *
 * 支持动态切换横坐标时间范围（7/30/90/365天）
 */
@HiltViewModel
class ForgettingCurveViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val srsCalculator: SrsCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgettingCurveUiState())
    val uiState: StateFlow<ForgettingCurveUiState> = _uiState.asStateFlow()

    /** 缓存用户平均 stability，切换范围时复用 */
    private var cachedUserStability: Float? = null
    private var dataLoaded = false

    init {
        loadCurveData()
    }

    /**
     * 切换时间范围
     */
    fun setTimeRange(range: CurveTimeRange) {
        _uiState.update { it.copy(selectedRange = range) }
        if (dataLoaded) {
            val curveData = generateCurveData(cachedUserStability, range.days)
            _uiState.update { it.copy(curveData = curveData) }
        }
    }

    private fun loadCurveData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            combine(
                wordRepository.getAllLearnedWords(),
                grammarRepository.getAllLearnedGrammars()
            ) { words, grammars ->
                val allStabilities = mutableListOf<Float>()

                words.forEach { word ->
                    if (word.stability > 0f) {
                        allStabilities.add(word.stability)
                    }
                }
                grammars.forEach { grammar ->
                    if (grammar.stability > 0f) {
                        allStabilities.add(grammar.stability)
                    }
                }

                if (allStabilities.isNotEmpty()) {
                    allStabilities.sort()
                    val size = allStabilities.size
                    if (size % 2 == 0) {
                        (allStabilities[size / 2 - 1] + allStabilities[size / 2]) / 2f
                    } else {
                        allStabilities[size / 2]
                    }
                } else {
                    null
                }
            }.collect { userStability ->
                cachedUserStability = userStability
                dataLoaded = true

                val currentRange = _uiState.value.selectedRange
                val curveData = generateCurveData(userStability, currentRange.days)

                _uiState.update {
                    it.copy(
                        curveData = curveData,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * 生成遗忘曲线数据
     *
     * 使用 SrsCalculator.forgettingCurve()（含个性化参数）计算
     */
    private fun generateCurveData(userStability: Float?, maxDays: Int): ForgettingCurveData {
        val standardStability = FsrsAlgorithm.DEFAULT_PARAMETERS[2] // ≈ 2.3065

        val standardCurve = generateCurvePoints(standardStability, maxDays)

        val userCurve = if (userStability != null && userStability > 0f) {
            generateCurvePoints(userStability, maxDays)
        } else {
            emptyList()
        }

        return ForgettingCurveData(
            standardCurve = standardCurve,
            userCurve = userCurve
        )
    }

    /**
     * 根据给定 stability 和时间范围生成曲线数据点（每天一个点）
     *
     * 通过 SrsCalculator 代理调用，使用个性化后的 FSRS 参数
     */
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

/**
 * 遗忘曲线界面 UI 状态
 */
data class ForgettingCurveUiState(
    val curveData: ForgettingCurveData? = null,
    val isLoading: Boolean = false,
    val selectedRange: CurveTimeRange = CurveTimeRange.MEDIUM
)
