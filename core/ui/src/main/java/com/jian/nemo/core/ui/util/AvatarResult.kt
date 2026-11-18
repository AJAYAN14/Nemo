package com.jian.nemo.core.ui.util

import android.content.Context
import com.jian.nemo.core.domain.model.AvatarStorageFailure
import com.jian.nemo.core.ui.R
import java.util.Locale
import kotlin.math.ceil

/**
 * 头像操作结果
 * 使用 sealed class 提供详细的错误信息
 */
sealed class AvatarResult {
    /**
     * 操作成功
     * @param path 头像文件路径
     * @param message 成功消息
     */
    data class Success(
        val path: String,
        val message: String = ""
    ) : AvatarResult()

    /**
     * 操作失败
     */
    sealed class Error : AvatarResult() {
        abstract val message: String
        abstract val details: String?

        /**
         * 权限被拒绝
         */
        data class PermissionDenied(
            override val message: String = "",
            override val details: String? = null
        ) : Error()

        /**
         * 文件不存在
         */
        data class FileNotFound(
            val path: String,
            override val message: String = "",
            override val details: String? = null
        ) : Error()

        /**
         * 存储空间不足
         */
        data class StorageFull(
            val availableSpace: Long,
            val requiredSpace: Long,
            override val message: String = "",
            override val details: String? = null
        ) : Error()

        /**
         * 无效的图片文件
         */
        data class InvalidImage(
            val reason: String,
            override val message: String = "",
            override val details: String? = reason
        ) : Error()

        /**
         * 图片过大
         */
        data class ImageTooLarge(
            val actualSize: Long,
            val maxSize: Long,
            override val message: String = "",
            override val details: String? = null
        ) : Error()

        /**
         * 图片压缩失败
         */
        data class CompressionFailed(
            val exception: Exception,
            override val message: String = "",
            override val details: String? = exception.message
        ) : Error()

        /**
         * 文件保存失败
         */
        data class SaveFailed(
            val exception: Exception,
            override val message: String = "",
            override val details: String? = exception.message
        ) : Error()

        /**
         * 未知错误
         */
        data class Unknown(
            val exception: Exception,
            override val message: String = "",
            override val details: String? = exception.message
        ) : Error()
    }

    /**
     * 获取用户友好的错误消息（通过 Context 本地化）
     */
    fun getUserMessage(context: Context): String {
        return when (this) {
            is Success -> context.getString(R.string.core_ui_avatar_result_success)
            is Error.PermissionDenied -> context.getString(
                R.string.core_ui_avatar_result_format_with_details,
                context.getString(R.string.core_ui_avatar_result_permission_denied),
                context.getString(R.string.core_ui_avatar_result_permission_denied_details)
            )
            is Error.FileNotFound -> context.getString(
                R.string.core_ui_avatar_result_format_with_details,
                context.getString(R.string.core_ui_avatar_result_file_not_found),
                context.getString(R.string.core_ui_avatar_result_file_not_found_details)
            )
            is Error.StorageFull -> context.getString(
                R.string.core_ui_avatar_result_format_with_details,
                context.getString(R.string.core_ui_avatar_result_storage_full),
                context.getString(
                    R.string.core_ui_avatar_result_storage_full_details,
                    formatMegabytesCeil(requiredSpace)
                )
            )
            is Error.InvalidImage -> context.getString(
                R.string.core_ui_avatar_result_format_with_details,
                context.getString(R.string.core_ui_avatar_result_invalid_image),
                reason
            )
            is Error.ImageTooLarge -> context.getString(
                R.string.core_ui_avatar_result_format_with_details,
                context.getString(R.string.core_ui_avatar_result_image_too_large),
                context.getString(
                    R.string.core_ui_avatar_result_image_too_large_details,
                    formatMegabytesCeil(actualSize),
                    formatMegabytesCeil(maxSize)
                )
            )
            is Error.CompressionFailed -> context.getString(
                R.string.core_ui_avatar_result_format_with_details,
                context.getString(R.string.core_ui_avatar_result_compression_failed),
                details ?: ""
            )
            is Error.SaveFailed -> context.getString(
                R.string.core_ui_avatar_result_format_with_details,
                context.getString(R.string.core_ui_avatar_result_save_failed),
                details ?: ""
            )
            is Error.Unknown -> context.getString(
                R.string.core_ui_avatar_result_format_with_details,
                context.getString(R.string.core_ui_avatar_result_unknown),
                details ?: ""
            )
        }
    }

    /**
     * 获取用户友好的错误消息
     */
    fun getUserMessage(): String {
        return when (this) {
            is Success -> message
            is Error -> if (details != null) "$message：$details" else message
        }
    }

    /**
     * 判断是否成功
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * 判断是否失败
     */
    fun isError(): Boolean = this is Error

    /**
     * 获取路径（如果成功）
     */
    fun getPathOrNull(): String? {
        return when (this) {
            is Success -> path
            is Error -> null
        }
    }
}

/**
 * 将字节数向上取整至 0.1MB 粒度并格式化（整数不附带小数位）
 */
fun formatMegabytesCeil(bytes: Long): String {
    if (bytes <= 0L) return "0"
    val tenths = ceil(bytes.toDouble() * 10.0 / (1024.0 * 1024.0)).toLong()
    val whole = tenths / 10L
    val fraction = tenths % 10L
    return if (fraction == 0L) {
        whole.toString()
    } else {
        String.format(Locale.US, "%d.%d", whole, fraction)
    }
}

/**
 * 将领域层头像处理失败原因映射为本地化用户提示文案
 */
fun AvatarStorageFailure.toUserMessage(context: Context): String {
    return when (this) {
        AvatarStorageFailure.PermissionDenied -> context.getString(
            R.string.core_ui_avatar_result_format_with_details,
            context.getString(R.string.core_ui_avatar_result_permission_denied),
            context.getString(R.string.core_ui_avatar_result_permission_denied_details)
        )
        is AvatarStorageFailure.FileNotFound -> context.getString(
            R.string.core_ui_avatar_result_format_with_details,
            context.getString(R.string.core_ui_avatar_result_file_not_found),
            context.getString(R.string.core_ui_avatar_result_file_not_found_details)
        )
        is AvatarStorageFailure.StorageFull -> context.getString(
            R.string.core_ui_avatar_result_format_with_details,
            context.getString(R.string.core_ui_avatar_result_storage_full),
            context.getString(
                R.string.core_ui_avatar_result_storage_full_details,
                formatMegabytesCeil(requiredBytes)
            )
        )
        is AvatarStorageFailure.ImageTooLarge -> context.getString(
            R.string.core_ui_avatar_result_format_with_details,
            context.getString(R.string.core_ui_avatar_result_image_too_large),
            context.getString(
                R.string.core_ui_avatar_result_image_too_large_details,
                formatMegabytesCeil(actualBytes),
                formatMegabytesCeil(maxBytes)
            )
        )
        AvatarStorageFailure.InvalidImage -> context.getString(
            R.string.core_ui_avatar_result_format_with_details,
            context.getString(R.string.core_ui_avatar_result_invalid_image),
            context.getString(R.string.core_ui_avatar_result_invalid_image_details)
        )
        AvatarStorageFailure.PickerUnavailable -> context.getString(
            R.string.core_ui_avatar_result_picker_unavailable
        )
        AvatarStorageFailure.CameraUnavailable -> context.getString(
            R.string.core_ui_avatar_result_camera_unavailable
        )
        is AvatarStorageFailure.SaveFailed -> {
            val base = context.getString(R.string.core_ui_avatar_result_save_failed)
            if (detail.isNullOrBlank()) {
                base
            } else {
                context.getString(R.string.core_ui_avatar_result_format_with_details, base, detail)
            }
        }
        is AvatarStorageFailure.Unknown -> {
            val base = context.getString(R.string.core_ui_avatar_result_unknown)
            if (detail.isNullOrBlank()) {
                base
            } else {
                context.getString(R.string.core_ui_avatar_result_format_with_details, base, detail)
            }
        }
    }
}
