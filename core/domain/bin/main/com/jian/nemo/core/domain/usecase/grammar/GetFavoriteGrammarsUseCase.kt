package com.jian.nemo.core.domain.usecase.grammar

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.common.ext.asResult
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取收藏的语法 Use Case
 *
 * 业务规则:
 * 1. 只返回isFavorite = true的语法
 * 2. 按ID排序
 *
 * 参考: GetFavoriteWordsUseCase
 */
class GetFavoriteGrammarsUseCase @Inject constructor(
    private val grammarRepository: GrammarRepository
) {
    operator fun invoke(): Flow<Result<List<Grammar>>> {
        return grammarRepository.getFavoriteGrammars()
            .asResult()
    }
}
