package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.WrongAnswer
import kotlinx.coroutines.flow.Flow

/**
 * 单词错题Repository接口
 *
 * Domain层只定义接口，Data层实现
 * 参考：错误处理规范.md
 *
 * 设计原则:
 * - 查询方法返回 Flow<T> (响应式数据流)
 * - 更新方法返回 Result<T> (明确成功/失败状态)
 */
interface WrongAnswerRepository {

    /**
     * 获取所有错题记录（按时间倒序）
     */
    fun getAllWrongAnswers(): Flow<List<WrongAnswer>>

    /**
     * 获取指定单词的错题记录
     */
    fun getWrongAnswersByWordId(wordId: Int): Flow<List<WrongAnswer>>

    /**
     * 获取所有错题单词的ID列表
     */
    suspend fun getAllWrongWordIds(): List<Int>

    /**
     * 插入错题记录
     */
    suspend fun insertWrongAnswer(wrongAnswer: WrongAnswer): Result<Unit>

    /**
     * 删除指定单词的错题记录
     */
    suspend fun deleteByWordId(wordId: Int): Result<Unit>

    /**
     * 删除所有错题记录
     */
    /**
     * 获取指定单词的错题记录 (同步)
     */
    suspend fun getWrongAnswerByWordIdSync(wordId: Int): WrongAnswer?

    /**
     * 删除所有错题记录
     */
    suspend fun clearAll(): Result<Unit>
}
