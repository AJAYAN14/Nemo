package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.WordRelation
import kotlinx.coroutines.flow.Flow

interface WordRelationRepository {
    /**
     * 响应式查询本地关系数据
     */
    fun getRelationsByWordId(wordId: Int): Flow<List<WordRelation>>
}
