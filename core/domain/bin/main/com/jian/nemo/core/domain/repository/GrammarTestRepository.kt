package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.GrammarTestQuestion
import com.jian.nemo.core.common.Result

interface GrammarTestRepository {
    suspend fun loadQuestionsByLevel(level: String): Result<List<GrammarTestQuestion>>
}
