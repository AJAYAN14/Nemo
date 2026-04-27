package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.ReviewForecast
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

/**
 * 获取复习预测 Use Case
 *
 * 获取未来7天的复习预测数据 (单词 + 语法)
 */
class GetReviewForecastUseCase @Inject constructor(
    private val wordRepository: WordRepository,
    private val grammarRepository: GrammarRepository,
    private val settingsRepository: SettingsRepository
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<ReviewForecast>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val today = DateTimeUtils.getLearningDay(resetHour)
            val endDate = today + 6 // 从今天起共7天

            val wordForecastFlow = wordRepository.getReviewForecast(today, endDate)
            val grammarForecastFlow = grammarRepository.getReviewForecast(today, endDate)

            combine(wordForecastFlow, grammarForecastFlow) { wordForecast, grammarForecast ->
                val resultMap = mutableMapOf<Long, ReviewForecast>()

                wordForecast.forEach { forecast ->
                    resultMap[forecast.date] = ReviewForecast(
                        date = forecast.date,
                        wordCount = forecast.wordCount,
                        grammarCount = 0
                    )
                }
                grammarForecast.forEach { forecast ->
                    val existing = resultMap[forecast.date]
                    if (existing != null) {
                        resultMap[forecast.date] = existing.copy(grammarCount = forecast.grammarCount)
                    } else {
                        resultMap[forecast.date] = ReviewForecast(
                            date = forecast.date,
                            wordCount = 0,
                            grammarCount = forecast.grammarCount
                        )
                    }
                }

                resultMap.values.sortedBy { it.date }
            }
        }
    }
}
