package com.jian.nemo.core.ui.util

import android.content.Context
import com.jian.nemo.core.ui.R

/**
 * 头像操作结果
 * 使用sealed class提供详细的错误信息
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
                context.getString(R.string.core_ui_avatar_result_storage_full_details, requiredSpace / 1024 / 1024)
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
                    actualSize / 1024 / 1024,
                    maxSize / 1024 / 1024
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
