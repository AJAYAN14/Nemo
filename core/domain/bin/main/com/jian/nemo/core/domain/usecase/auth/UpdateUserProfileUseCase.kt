package com.jian.nemo.core.domain.usecase.auth

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import java.io.File
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun updateUsername(newUsername: String): Result<Unit> {
        return authRepository.updateUsername(newUsername)
    }

    suspend fun updateEmail(newEmail: String): Result<Unit> {
        return authRepository.updateEmail(newEmail)
    }

    suspend fun uploadAvatar(file: File): Result<String> {
        val result = authRepository.updateAvatar(file)
        if (result is Result.Success) {
            settingsRepository.setUserAvatarPath(file.absolutePath)
        }
        return result
    }

    suspend fun clearAvatar(): Result<Unit> {
        settingsRepository.clearUserAvatar()
        return authRepository.updateUserAvatarUrl(null)
    }

    suspend fun updateUserAvatarUrl(url: String): Result<Unit> {
        val result = authRepository.updateUserAvatarUrl(url)
        if (result is Result.Success) {
            // 对于预设头像，直接将协议字符串作为路径缓存
            settingsRepository.setUserAvatarPath(url)
        }
        return result
    }
}
