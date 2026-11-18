package com.jian.nemo.core.domain.model

/**
 * 相机拍照目标信息
 *
 * @property filePath 本地临时图片绝对路径
 * @property contentUri 提供给系统相机写入的 FileProvider URI 字符串
 */
data class CameraCaptureTarget(
    val filePath: String,
    val contentUri: String
)

/**
 * 头像本地持久化结果
 */
sealed interface AvatarStorageResult {
    data class Success(val filePath: String) : AvatarStorageResult
    data class Failure(val reason: AvatarStorageFailure) : AvatarStorageResult
}

/**
 * 头像本地处理失败原因
 */
sealed interface AvatarStorageFailure {
    data object PermissionDenied : AvatarStorageFailure
    data class FileNotFound(val source: String) : AvatarStorageFailure
    data class StorageFull(val availableBytes: Long, val requiredBytes: Long) : AvatarStorageFailure
    data class ImageTooLarge(val actualBytes: Long, val maxBytes: Long) : AvatarStorageFailure
    data object InvalidImage : AvatarStorageFailure
    data object PickerUnavailable : AvatarStorageFailure
    data object CameraUnavailable : AvatarStorageFailure
    data class SaveFailed(val detail: String? = null) : AvatarStorageFailure
    data class Unknown(val detail: String? = null) : AvatarStorageFailure
}
