package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.model.ReviewForecast
import com.jian.nemo.core.domain.model.TestRecord
import kotlinx.coroutines.flow.Flow

/**
 * 单词 Repository 接口
 *
 * Domain层只定义接口，Data层实现
 *
 * 设计原则:
 * - 查询方法返回 Flow<T> (响应式数据流)
 * - 更新方法返回 Result<T> (明确成功/失败状态)
 */
interface WordRepository {

    // ========== 查询 ==========

    /**
     * 根据ID获取单词
     */
    fun getWordById(id: Int): Flow<Word?>

    /**
     * 获取指定等级的新单词（未学习且未跳过）
     * @param level JLPT等级 (N1-N5)
     * @param isRandom 是否随机抽取 (false=按ID顺序)
     */
    fun getNewWords(level: String, isRandom: Boolean = false): Flow<List<Word>>

    /**
     * 获取到期复习的单词
     * @param today 今天的Epoch Day
     */
    fun getDueWords(today: Long): Flow<List<Word>>

    /**
     * 获取到期复习的单词数量
     */
    fun getDueWordsCount(today: Long): Flow<Int>

    /**
     * 获取今日首次学习的单词
     */
    fun getTodayLearnedWords(today: Long): Flow<List<Word>>

    /**
     * 获取今日复习过的单词
     */
    fun getTodayReviewedWords(today: Long): Flow<List<Word>>

    /**
     * 获取收藏的单词
     */
    fun getFavoriteWords(): Flow<List<Word>>

    /**
     * 获取跳过的单词
     */
    fun getSkippedWords(limit: Int): Flow<List<Word>>

    /**
     * 获取指定等级的所有单词（包括已学和未学）
     * @param level JLPT等级 (如 "N5", "N4" 等)
     */
    fun getAllWordsByLevel(level: String): Flow<List<Word>>

    /**
     * 获取所有已学习的单词
     */
    fun getAllLearnedWords(): Flow<List<Word>>

    /**
     * 获取指定等级的所有已学习单词
     */
    fun getAllLearnedWordsByLevel(level: String): Flow<List<Word>>

    /**
     * 获取所有已学习的单词总数
     */
    fun getLearnedWordCount(): Flow<Int>

    /**
     * 获取复习预测（未来7天）
     */
    fun getReviewForecast(startDate: Long, endDate: Long): Flow<List<ReviewForecast>>


    /**
     * 根据词性分类获取单词
     */
    suspend fun getWordsByPartOfSpeech(pos: PartOfSpeech): List<Word>

    /**
     * 根据ID列表批量获取单词
     */
    suspend fun getWordsByIds(ids: List<Int>): List<Word>

    /**
     * 搜索单词
     * @param query 搜索关键词（日文、中文、假名）
     */
    /**
     * 搜索单词
     * @param query 搜索关键词（日文、中文、假名）
     */
    fun searchWords(query: String): Flow<List<Word>>

    /**
     * 获取按复习紧迫度排序的单词（Adaptive Strategy）
     */
    suspend fun getWordsSortedByDueScore(levels: List<String>, limit: Int): List<Word>

    /**
     * 获取所有外来语（片假名单词）
     */
    suspend fun getLoanWords(): List<Word>

    // ========== 测试统计 ==========

    /**
     * 获取今日测试次数
     */
    fun getTodayTestCount(today: Long): Flow<Int>

    /**
     * 获取今日测试正确率 (0.0 - 1.0)
     */
    fun getTodayTestAccuracy(today: Long): Flow<Float>

    /**
     * 获取历史总测试次数
     */
    fun getTotalTestCount(): Flow<Int>

    /**
     * 获取历史总正确率 (0.0 - 1.0)
     */
    fun getOverallAccuracy(): Flow<Float>

    /**
     * 获取总题目数（用于计算错题率）
     */
    fun getTotalQuestionCount(): Flow<Int>

    /**
     * 获取总正确题数（用于计算错题率）
     */
    fun getTotalCorrectAnswerCount(): Flow<Int>

    // ========== 等级查询 ==========

    fun getTodayLearnedLevels(todayEpochDay: Long): Flow<List<String>>

    fun getFavoriteLevels(): Flow<List<String>>

    fun getLearnedLevels(): Flow<List<String>>

    fun getTodayReviewedLevels(todayEpochDay: Long): Flow<List<String>>

    fun getWrongAnswerLevels(): Flow<List<String>>

    // ========== 更新 ==========

    /**
     * 更新单词（通常是SRS状态更新）
     */
    suspend fun updateWord(word: Word): Result<Unit>

    /**
     * 更新收藏状态
     */
    suspend fun updateFavoriteStatus(wordId: Int, isFavorite: Boolean): Result<Unit>

    /**
     * 标记为跳过
     */
    suspend fun markAsSkipped(wordId: Int): Result<Unit>

    /**
     * 取消跳过
     */
    suspend fun unmarkAsSkipped(wordId: Int): Result<Unit>

    // ========== 批量操作 ==========

    /**
     * 重置所有学习进度
     */
    suspend fun resetAllProgress(): Result<Unit>

    /**
     * 清空所有收藏
     */
    suspend fun clearAllFavorites(): Result<Unit>

    /**
     * 保存测试记录
     */
    suspend fun saveTestRecord(record: TestRecord): Result<Unit>
}
