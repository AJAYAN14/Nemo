package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.model.ReviewForecast
import kotlinx.coroutines.flow.Flow

/**
 * 语法 Repository 接口
 * Domain层只定义接口，Data层实现
 *
 * 设计原则:
 * - 查询方法返回 Flow<T> (响应式数据流)
 * - 更新方法返回 Result<T> (明确成功/失败状态)
 */
interface GrammarRepository {

    // ========== 查询 ==========

    /**
     * 根据ID获取语法
     */
    fun getGrammarById(id: Int): Flow<Grammar?>

    /**
     * 获取所有语法
     */
    fun getAllGrammars(): Flow<List<Grammar>>

    /**
     * 获取指定等级的新语法（未学习且未跳过）
     * @param level JLPT等级 (N1-N5)
     * @param isRandom 是否随机抽取 (false=按ID顺序)
     */
    fun getNewGrammars(level: String, isRandom: Boolean = false): Flow<List<Grammar>>

    /**
     * 获取到期复习的语法数量
     */
    fun getDueGrammarsCount(today: Long): Flow<Int>

    /**
     * 获取跳过的语法
     */
    fun getSkippedGrammars(limit: Int): Flow<List<Grammar>>

    /**
     * 获取到期复习的语法
     * @param today 今天的Epoch Day
     */
    fun getDueGrammars(today: Long): Flow<List<Grammar>>

    /**
     * 获取今日首次学习的语法
     */
    fun getTodayLearnedGrammars(today: Long): Flow<List<Grammar>>

    /**
     * 获取今日复习过的语法
     */
    fun getTodayReviewedGrammars(today: Long): Flow<List<Grammar>>


    /**
     * 获取收藏的语法
     */
    fun getFavoriteGrammars(): Flow<List<Grammar>>

    /**
     * 获取所有已学习的语法
     */
    fun getAllLearnedGrammars(): Flow<List<Grammar>>

    /**
     * 获取指定等级的所有已学习语法
     */
    fun getAllLearnedGrammarsByLevel(level: String): Flow<List<Grammar>>

    /**
     * 获取所有已学习的语法总数
     */
    fun getLearnedGrammarCount(): Flow<Int>

    /**
     * 获取复习预测（未来7天）
     */
    fun getReviewForecast(startDate: Long, endDate: Long): Flow<List<ReviewForecast>>

    /**
     * 根据等级获取语法
     * @param levels 等级列表，例如 ["n5", "n4"]
     */
    fun getGrammarsByLevels(levels: List<String>): Flow<List<Grammar>>

    /**
     * 根据ID列表批量获取语法
     */
    suspend fun getGrammarsByIds(ids: List<Int>): List<Grammar>

    /**
     * 搜索语法
     * @param query 搜索关键词
     */
    fun searchGrammars(query: String): Flow<List<Grammar>>

    // ========== 等级查询 ==========

    fun getTodayLearnedGrammarLevels(todayEpochDay: Long): Flow<List<String>>

    fun getFavoriteGrammarLevels(): Flow<List<String>>

    fun getLearnedGrammarLevels(): Flow<List<String>>

    fun getTodayReviewedGrammarLevels(todayEpochDay: Long): Flow<List<String>>

    fun getWrongAnswerGrammarLevels(): Flow<List<String>>

    // ========== 更新 ==========

    /**
     * 更新语法（通常是SRS状态更新）
     */
    suspend fun updateGrammar(grammar: Grammar): Result<Unit>

    /**
     * 更新收藏状态
     */
    suspend fun updateFavoriteStatus(grammarId: Int, isFavorite: Boolean): Result<Unit>

    /**
     * 标记为跳过
     */
    suspend fun markAsSkipped(grammarId: Int): Result<Unit>

    /**
     * 取消跳过
     */
    suspend fun unmarkAsSkipped(grammarId: Int): Result<Unit>

    // ========== 批量操作 ==========

    /**
     * 重置所有学习进度
     */
    suspend fun resetAllProgress(): Result<Unit>

    /**
     * 清空所有收藏
     */
    suspend fun clearAllFavorites(): Result<Unit>
}
