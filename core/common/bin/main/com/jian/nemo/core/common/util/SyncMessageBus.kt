package com.jian.nemo.core.common.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全局同步消息总线
 * 用于跨进程/组件分发同步结果通知
 */
@Singleton
class SyncMessageBus @Inject constructor() {
    private val _syncEvents = MutableSharedFlow<SyncEvent>(extraBufferCapacity = 1)
    val syncEvents = _syncEvents.asSharedFlow()

    suspend fun emit(event: SyncEvent) {
        _syncEvents.emit(event)
    }

    fun tryEmit(event: SyncEvent) {
        _syncEvents.tryEmit(event)
    }
}

sealed class SyncEvent {
    data class Success(val message: String = "数据同步成功") : SyncEvent()
    data class Error(val message: String) : SyncEvent()
}
