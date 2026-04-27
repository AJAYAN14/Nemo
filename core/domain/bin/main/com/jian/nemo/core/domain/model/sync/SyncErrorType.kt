package com.jian.nemo.core.domain.model.sync

enum class SyncErrorType {
    NO_CLOUD_DATA,      // 云端无数据 (可安全继续推送)
    NETWORK_ERROR,      // 网络错误 (必须中止推送)
    DATA_ERROR,         // 数据错误 (视情况而定)
    UNKNOWN             // 未知错误
}
