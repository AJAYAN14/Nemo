package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.DictionarySyncResult

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DictionarySyncManager {
    /** 词库当前是否正在同步中 */
    val isSyncingFlow: StateFlow<Boolean>

    /** 词库是否已就绪（数据量充足且未处于同步锁定状态） */
    val isDictionaryReadyFlow: Flow<Boolean>

    /** 挂起检查词库是否已准备就绪 */
    suspend fun isDictionaryReady(): Boolean

    suspend fun performDictionarySync(
        force: Boolean = false,
        forceIncremental: Boolean = false
    ): DictionarySyncResult
}

