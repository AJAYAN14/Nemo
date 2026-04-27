package com.jian.nemo.core.common.error

/**
 * 同步和恢复错误类型
 * 用于区分不同类型的错误，提供更好的错误处理和用户提示
 */
sealed class SyncError {
    /**
     * 网络相关错误
     */
    sealed class NetworkError : SyncError() {
        object Timeout : NetworkError()
        object NoConnection : NetworkError()
        object ServerError : NetworkError()
        data class Unknown(val message: String, val throwable: Throwable? = null) : NetworkError()
    }

    /**
     * 权限相关错误
     */
    sealed class PermissionError : SyncError() {
        object NotLoggedIn : PermissionError()
        object AccessDenied : PermissionError()
        data class Unknown(val message: String) : PermissionError()
    }

    /**
     * 数据格式错误
     */
    sealed class DataFormatError : SyncError() {
        object InvalidJson : DataFormatError()
        object MissingRequiredField : DataFormatError()
        object VersionMismatch : DataFormatError()
        data class Unknown(val message: String) : DataFormatError()
    }

    /**
     * 数据库操作错误
     */
    sealed class DatabaseError : SyncError() {
        object TransactionFailed : DatabaseError()
        object DataCorrupted : DatabaseError()
        object ValidationFailed : DatabaseError()
        data class Unknown(val message: String, val throwable: Throwable? = null) : DatabaseError()
    }

    /**
     * 未知错误
     */
    data class UnknownError(
        val message: String,
        val throwable: Throwable? = null
    ) : SyncError()

    /**
     * 获取错误消息标识（由调用方根据上下文处理本地化）
     */
    fun getMessageKey(): String {
        return when (this) {
            is NetworkError.Timeout -> "error_network_timeout"
            is NetworkError.NoConnection -> "error_network_no_connection"
            is NetworkError.ServerError -> "error_server_error"
            is NetworkError.Unknown -> "error_network_unknown"

            is PermissionError.NotLoggedIn -> "error_auth_not_logged_in"
            is PermissionError.AccessDenied -> "error_auth_access_denied"
            is PermissionError.Unknown -> "error_auth_unknown"

            is DataFormatError.InvalidJson -> "error_data_invalid_json"
            is DataFormatError.MissingRequiredField -> "error_data_missing_field"
            is DataFormatError.VersionMismatch -> "error_data_version_mismatch"
            is DataFormatError.Unknown -> "error_data_unknown"

            is DatabaseError.TransactionFailed -> "error_db_transaction_failed"
            is DatabaseError.DataCorrupted -> "error_db_corrupted"
            is DatabaseError.ValidationFailed -> "error_db_validation_failed"
            is DatabaseError.Unknown -> "error_db_unknown"

            is UnknownError -> "error_unknown"
        }
    }

    /**
     * 获取用户友好的错误消息
     * @deprecated 请改用 getMessageKey() 并由 UI 层处理多语言。
     */
    @Deprecated("Use getMessageKey()", ReplaceWith("getMessageKey()"))
    fun getUserFriendlyMessage(): String {
        return when (this) {
            is NetworkError.Timeout -> "网络请求超时，请检查网络连接后重试"
            is NetworkError.NoConnection -> "无法连接到服务器，请检查网络设置"
            is NetworkError.ServerError -> "服务器错误，请稍后重试"
            is NetworkError.Unknown -> "网络错误: ${this.message}"

            is PermissionError.NotLoggedIn -> "请先登录账号"
            is PermissionError.AccessDenied -> "没有访问权限，请检查账号状态"
            is PermissionError.Unknown -> "权限错误: ${this.message}"

            is DataFormatError.InvalidJson -> "同步数据格式错误，无法恢复"
            is DataFormatError.MissingRequiredField -> "同步数据不完整，缺少必要字段"
            is DataFormatError.VersionMismatch -> "数据版本不兼容，请更新应用"
            is DataFormatError.Unknown -> "数据格式错误: ${this.message}"

            is DatabaseError.TransactionFailed -> "数据恢复失败，请重试"
            is DatabaseError.DataCorrupted -> "数据验证失败，同步文件可能已损坏"
            is DatabaseError.ValidationFailed -> "恢复后的数据验证失败"
            is DatabaseError.Unknown -> "数据库错误: ${this.message}"

            is UnknownError -> "未知错误: ${this.message}"
        }
    }
}
