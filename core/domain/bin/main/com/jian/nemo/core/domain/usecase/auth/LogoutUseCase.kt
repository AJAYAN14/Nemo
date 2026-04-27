package com.jian.nemo.core.domain.usecase.auth

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        settingsRepository.clearUserAvatar()
        return authRepository.logout()
    }
}
