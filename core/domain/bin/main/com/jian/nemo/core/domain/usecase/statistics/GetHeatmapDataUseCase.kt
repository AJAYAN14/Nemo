package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * 获取热力图数据 UseCase
 *
 * 职责:
 * 1. 拉取过去365天的数据
 * 2. 补全缺失的日期 (填充0)
 * 3. 计算热力等级 (0-4)
 */
class GetHeatmapDataUseCase @Inject constructor(
    private val studyRecordRepository: StudyRecordRepository,
    private val settingsRepository: SettingsRepository
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<HeatmapDay>> {
        return settingsRepository.learningDayResetHourFlow.flatMapLatest { resetHour ->
            val endEpoch = DateTimeUtils.getLearningDay(resetHour)
            val endDate = LocalDate.ofEpochDay(endEpoch)
            val startDate = endDate.minusDays(364)
            val startEpoch = startDate.toEpochDay()

            studyRecordRepository.getDailyActivityCounts(startEpoch, endEpoch)
                .map { countsMap ->
                    val resultList = mutableListOf<HeatmapDay>()

                    // Generate all dates in range
                    var currentDate = startDate
                    while (!currentDate.isAfter(endDate)) {
                        val epochDay = currentDate.toEpochDay()
                        val count = countsMap[epochDay] ?: 0
                        val level = calculateLevel(count)

                        resultList.add(HeatmapDay(epochDay, count, level))
                        currentDate = currentDate.plusDays(1)
                    }

                    resultList
                }
        }
    }

    private fun calculateLevel(count: Int): Int {
        return when {
            count == 0 -> 0
            count <= 10 -> 1
            count <= 30 -> 2
            count <= 60 -> 3
            else -> 4
        }
    }
}

data class HeatmapDay(
    val date: Long, // Epoch Day
    val count: Int,
    val level: Int // 0-4
)
