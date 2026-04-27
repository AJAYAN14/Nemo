package com.jian.nemo.core.domain.usecase.settings

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.SettingsRepository
import javax.inject.Inject

class RepairDataUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<String> {
        return try {
            val count = settingsRepository.repairLocalData()
            if (count > 0) {
                Result.Success("修复成功，已清理 $count 条重复数据")
            } else {
                Result.Success("数据正常，无须修复")
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
