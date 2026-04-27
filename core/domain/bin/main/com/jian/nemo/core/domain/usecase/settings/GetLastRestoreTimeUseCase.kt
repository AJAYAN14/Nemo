package com.jian.nemo.core.domain.usecase.settings

import com.jian.nemo.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLastRestoreTimeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<Long> {
        return settingsRepository.lastRestoreTimeFlow
    }
}
