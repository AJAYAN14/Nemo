package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.DictionarySyncResult

interface DictionarySyncManager {
    suspend fun performDictionarySync(
        force: Boolean = false,
        forceIncremental: Boolean = false
    ): DictionarySyncResult
}
