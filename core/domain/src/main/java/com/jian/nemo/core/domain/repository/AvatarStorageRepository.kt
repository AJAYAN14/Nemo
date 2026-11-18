package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.AvatarStorageResult
import com.jian.nemo.core.domain.model.CameraCaptureTarget

/**
 * 头像本地文件存储仓储
 *
 * 所有挂起方法均保证主线程安全（Main-safe），内部自动切换至 IO 调度器执行。
 */
interface AvatarStorageRepository {

    /**
     * 将外部内容 URI 对应的图片安全拷贝并校验后落盘至应用内部存储
     */
    suspend fun saveAvatarFromUri(uriString: String): AvatarStorageResult

    /**
     * 在后台线程预备相机拍照所需的临时文件与共享 URI
     */
    suspend fun prepareCameraCaptureTarget(): Result<CameraCaptureTarget>

    /**
     * 校验相机拍摄结果并将其持久化迁移至应用内部存储，完成后清理临时文件
     */
    suspend fun finalizeCameraCapture(tempFilePath: String): AvatarStorageResult

    /**
     * 清理取消或失败场景下遗留的相机临时文件
     */
    suspend fun cleanupTempCapture(tempFilePath: String)
}
