package com.jian.nemo.core.domain.usecase.sync

import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.repository.SyncRepository
import com.jian.nemo.core.domain.model.SyncProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 从云端恢复数据 UseCase
 *
 * 始终执行全量镜像恢复：清空本地数据 → 从云端全量拉取。
 * 确认对话框已在 UI 层前置处理，此处不再做预检查。
 */
class RestoreDataUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<SyncProgress> = flow {
        try {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                emit(SyncProgress.Failed("未登录"))
                return@flow
            }

            // 执行全量镜像恢复：清空本地 → 从云端拉取全部数据
            syncRepository.performRestore(user.id).collect { emit(it) }
        } catch (e: Exception) {
            emit(SyncProgress.Failed("恢复失败: ${e.message}"))
        }
    }
}
