package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.GrammarWrongAnswer
import kotlinx.coroutines.flow.Flow

/**
 * 语法错题Repository接口
 *
 * Domain层只定义接口，Data层实现
 * 参考：错误处理规范.md
 *
 * 设计原则:
 * - 查询方法返回 Flow<T> (响应式数据流)
 * - 更新方法返回 Result<T> (明确成功/失败状态)
 */
interface GrammarWrongAnswerRepository {

    /**
     * 获取所有语法错题记录（按时间倒序）
     */
    fun getAllWrongAnswers(): Flow<List<GrammarWrongAnswer>>

    /**
     * 获取指定语法的错题记录
     */
    fun getWrongAnswersByGrammarId(grammarId: Int): Flow<List<GrammarWrongAnswer>>

    /**
     * 获取所有错题语法的ID列表
     */
    suspend fun getAllWrongGrammarIds(): List<Int>

    /**
     * 删除指定语法的错题记录
     */
    suspend fun deleteByGrammarId(grammarId: Int): Result<Unit>

    /**
     * 删除所有语法错题记录
     */
    suspend fun clearAll(): Result<Unit>

    /**
     * 获取指定语法的错题记录 (同步)
     */
    suspend fun getWrongAnswerByGrammarIdSync(grammarId: Int): GrammarWrongAnswer?

    /**
     * 插入错题记录
     */
    suspend fun insertWrongAnswer(wrongAnswer: GrammarWrongAnswer): Result<Unit>
}
