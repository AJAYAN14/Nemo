package com.jian.nemo.core.domain.usecase.statistics

import com.jian.nemo.core.common.util.DateTimeUtils
import com.jian.nemo.core.domain.model.StudyRecord
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 获取最近N天的学习记录 Use Case
 *
 * 用于绘制学习趋势图和日历热力图
 */
class GetRecentRecordsUseCase @Inject constructor(
    private val studyRecordRepository: StudyRecordRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(days: Int = 30): List<StudyRecord> {
        val resetHour = settingsRepository.learningDayResetHourFlow.first()
        val today = DateTimeUtils.getLearningDay(resetHour)
        val startDate = today - days + 1

        return studyRecordRepository.getRecordsBetween(startDate, today).first()
    }
}
