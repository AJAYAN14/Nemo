package com.jian.nemo.core.data.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.data.local.dao.*
import com.jian.nemo.core.data.mapper.GrammarMapper.toDomainModel
import com.jian.nemo.core.data.mapper.GrammarMapper.toDomainModels
import com.jian.nemo.core.data.mapper.GrammarMapper.toStudyStateEntity
import com.jian.nemo.core.domain.model.ContentDelist.isDelisted
import com.jian.nemo.core.domain.model.Grammar
import com.jian.nemo.core.domain.repository.GrammarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Grammar Repository 实现
 *
 * 参考: _reference/old-nemo/.../GrammarDao.kt
 *
 * 职责:
 * 1. 调用GrammarDao获取数据
 * 2. Entity → Domain Model转换
 * 3. 异常处理
 * 4. 日志记录
 */
@Singleton
class GrammarRepositoryImpl @Inject constructor(
    private val grammarDao: GrammarDao,
    private val grammarStudyStateDao: GrammarStudyStateDao
) : GrammarRepository {

    // ========== 查询实现 ==========

    override fun getGrammarById(id: Int): Flow<Grammar?> {
        return grammarDao.getGrammarWithUsages(id)
            .map { it?.toDomainModel() }
            .catch { e ->
                println("❌ 获取语法失败: id=$id, error=${e.message}")
                emit(null)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getAllGrammars(): Flow<List<Grammar>> {
        return grammarDao.getAllGrammarsWithUsages()
            .map { it.toDomainModels() }
            .catch { e ->
                println("❌ 获取所有语法失败: error=${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getNewGrammars(level: String, isRandom: Boolean): Flow<List<Grammar>> {
        val flow = if (isRandom) {
            grammarDao.getNewGrammarsByLevelWithUsagesRandom(level)
        } else {
            grammarDao.getNewGrammarsByLevelWithUsages(level)
        }

        return flow
            .map { it.toDomainModels().filter { g -> !g.isDelisted() } }
            .catch { e ->
                println("❌ 获取新语法失败: level=$level, random=$isRandom, error=${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getDueGrammars(today: Long): Flow<List<Grammar>> {
        return grammarDao.getDueGrammarsWithUsages(today)
            .map { it.toDomainModels().filter { g -> !g.isDelisted() } }
            .catch { e ->
                println("❌ 获取到期语法失败: today=$today, error=${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getDueGrammarsCount(today: Long): Flow<Int> {
        return getDueGrammars(today).map { it.size }
    }

    override fun getSkippedGrammars(limit: Int): Flow<List<Grammar>> {
        return grammarDao.getSkippedGrammarsWithUsages(limit)
            .map { it.toDomainModels() }
            .catch { e ->
                println("❌ 获取跳过语法失败: error=${e.message}")
                emit(emptyList())
            }
    }

    override fun getTodayLearnedGrammars(today: Long): Flow<List<Grammar>> {
        return grammarDao.getTodayLearnedGrammarsWithUsages(today)
            .map { it.toDomainModels() }
            .catch { e ->
                println("❌ 获取今日学习语法失败: error=${e.message}")
                emit(emptyList())
            }
    }

    override fun getTodayReviewedGrammars(today: Long): Flow<List<Grammar>> {
        return grammarDao.getTodayReviewedGrammarsWithUsages(today)
            .map { it.toDomainModels() }
            .catch { e ->
                println("❌ 获取今日复习语法失败: error=${e.message}")
                emit(emptyList())
            }
    }

    override fun getFavoriteGrammars(): Flow<List<Grammar>> {
        return grammarDao.getFavoriteGrammarsWithUsages()
            .map { it.toDomainModels() }
            .catch { e ->
                println("❌ 获取收藏语法失败: error=${e.message}")
                emit(emptyList())
            }
    }

    override fun getAllLearnedGrammars(): Flow<List<Grammar>> {
        return grammarDao.getAllLearnedGrammarsWithUsages()
            .map { it.toDomainModels() }
            .catch { e ->
                println("❌ 获取已学习语法失败: error=${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getAllLearnedGrammarsByLevel(level: String): Flow<List<Grammar>> {
        val upperLevel = level.uppercase()
        return grammarDao.getLearnedGrammarsByLevelWithUsages(upperLevel)
            .map { it.toDomainModels() }
            .catch { e ->
                println("❌ 获取等级${upperLevel}已学语法失败: error=${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getLearnedGrammarCount(): Flow<Int> {
        return grammarDao.getLearnedGrammarCount()
            .catch { e ->
                println("❌ 获取已学语法总数失败: error=${e.message}")
                emit(0)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getReviewForecast(startDate: Long, endDate: Long): Flow<List<com.jian.nemo.core.domain.model.ReviewForecast>> {
        return grammarDao.getReviewForecast(startDate, endDate)
            .map { tuples ->
                tuples.map {
                    com.jian.nemo.core.domain.model.ReviewForecast(date = it.date, grammarCount = it.count)
                }
            }
            .catch { e ->
                println("❌ 获取语法复习预测失败: error=${e.message}")
                emit(emptyList())
            }
    }

    override fun getTodayLearnedGrammarLevels(today: Long): Flow<List<String>> {
        return grammarDao.getTodayLearnedGrammarLevels(today)
            .catch { e ->
                println("❌ 获取今日学习语法等级失败: today=$today, error=${e.message}")
                emit(emptyList())
            }
    }

    override fun getFavoriteGrammarLevels(): Flow<List<String>> {
         return grammarDao.getFavoriteGrammarLevels()
            .catch { e ->
                println("❌ 获取收藏语法等级失败: error=${e.message}")
                emit(emptyList())
            }
    }

    override fun getLearnedGrammarLevels(): Flow<List<String>> {
         return grammarDao.getLearnedGrammarLevels()
            .catch { e ->
                println("❌ 获取已学习语法等级失败: error=${e.message}")
                emit(emptyList())
            }
    }

    override fun getTodayReviewedGrammarLevels(today: Long): Flow<List<String>> {
         return grammarDao.getTodayReviewedGrammarLevels(today)
            .catch { e ->
                println("❌ 获取今日复习语法等级失败: today=$today, error=${e.message}")
                emit(emptyList())
            }
    }

    override fun getWrongAnswerGrammarLevels(): Flow<List<String>> {
         return grammarDao.getWrongAnswerGrammarLevels()
            .catch { e ->
                println("❌ 获取错题语法等级失败: error=${e.message}")
                emit(emptyList())
            }
    }

    override fun getGrammarsByLevels(levels: List<String>): Flow<List<Grammar>> {
        println("[GrammarRepository] 获取等级 $levels 的所有语法")
        return grammarDao.getGrammarsByLevelsWithUsages(levels)
            .map { entities ->
                println("[GrammarRepository] 查询到 ${entities.size} 个语法")
                entities.toDomainModels()
            }
            .catch { e ->
                println("❌ 按等级获取语法失败: levels=$levels, error=${e.message}")
                emit(emptyList())
            }
    }

    override suspend fun getGrammarsByIds(ids: List<Int>): List<Grammar> {
        return try {
            if (ids.isEmpty()) {
                emptyList()
            } else {
                grammarDao.getGrammarsByIdsWithUsages(ids).map { it.toDomainModel() }
            }
        } catch (e: Exception) {
            println("❌ 批量获取语法失败: ids.size=${ids.size}, error=${e.message}")
            emptyList()
        }
    }

    override fun searchGrammars(query: String): Flow<List<Grammar>> {
        return grammarDao.searchGrammarsWithUsages(query)
            .map { it.toDomainModels() }
            .catch { e ->
                println("❌ 搜索语法失败: query=$query, error=${e.message}")
                emit(emptyList())
            }
    }

    // ========== 更新实现 ==========

    override suspend fun updateGrammar(grammar: Grammar): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // 🎯 强制刷新时间戳
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            val stateEntity = grammar.toStudyStateEntity().copy(lastModifiedTime = now)

            grammarStudyStateDao.insert(stateEntity)
            println("✅ 语法学习状态更新成功: ${grammar.grammar}, lastModified=$now")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 语法学习状态更新失败: ${grammar.grammar}, error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun updateFavoriteStatus(
        grammarId: Int,
        isFavorite: Boolean
    ): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            grammarStudyStateDao.updateFavoriteStatus(grammarId, isFavorite, now)
            println("✅ 语法收藏状态更新: grammarId=$grammarId, isFavorite=$isFavorite")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 语法收藏状态更新失败: error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun markAsSkipped(grammarId: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            grammarStudyStateDao.updateSkipStatus(grammarId, true, now)
            println("✅ 语法已跳过: grammarId=$grammarId")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 标记跳过失败: error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun unmarkAsSkipped(grammarId: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            grammarStudyStateDao.updateSkipStatus(grammarId, false, now)
            println("✅ 取消跳过: grammarId=$grammarId")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 取消跳过失败: error=${e.message}")
            Result.Error(e)
        }
    }

    // ========== 批量操作 ==========

    override suspend fun resetAllProgress(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            grammarStudyStateDao.resetAllProgress(now)
            println("⚠️ 所有语法进度已重置")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 重置进度失败: error=${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun clearAllFavorites(): Result<Unit> {
        return try {
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            grammarStudyStateDao.clearAllFavorites(now)
            println("✅ 所有语法收藏已清空")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 清空收藏失败: error=${e.message}")
            Result.Error(e)
        }
    }
}
