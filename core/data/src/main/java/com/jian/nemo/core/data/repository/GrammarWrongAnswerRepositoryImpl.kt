package com.jian.nemo.core.data.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.data.local.dao.GrammarDao
import com.jian.nemo.core.data.local.dao.GrammarWrongAnswerDao
import com.jian.nemo.core.data.mapper.toDomainModel
import com.jian.nemo.core.data.mapper.toEntity
import com.jian.nemo.core.domain.model.GrammarWrongAnswer
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GrammarWrongAnswer Repository 实现
 *
 * 职责:
 * 1. 调用DAO获取数据
 * 2. Entity → Domain Model转换
 * 3. 异常处理
 * 4. 日志记录
 *

 */
@Singleton
class GrammarWrongAnswerRepositoryImpl @Inject constructor(
    private val grammarWrongAnswerDao: GrammarWrongAnswerDao,
    private val grammarDao: GrammarDao
) : GrammarWrongAnswerRepository {

    override fun getAllWrongAnswers(): Flow<List<GrammarWrongAnswer>> {
        return grammarWrongAnswerDao.getAllWrongAnswers()
            .map { entities ->
                val grammarIds = entities.map { it.grammarId }.distinct()
                val grammarMap = if (grammarIds.isNotEmpty()) {
                    grammarDao.getGrammarsByIds(grammarIds).associateBy { it.id }
                } else {
                    emptyMap()
                }

                entities.map { entity ->
                    val grammarEntity = grammarMap[entity.grammarId]
                    // 使用已有的 GrammarMapper 进行转换
                    // 需要将 GrammarEntity 包装为 GrammarWithUsages
                    val grammarDomain = grammarEntity?.let {
                        with(com.jian.nemo.core.data.mapper.GrammarMapper) {
                            com.jian.nemo.core.data.local.entity.relations.GrammarWithUsages(
                                grammar = it,
                                usages = emptyList(),
                                state = null
                            ).toDomainModel()
                        }
                    }

                    // 直接使用顶层扩展函数
                    entity.toDomainModel(grammarDomain)
                }
            }
            .catch { e ->
                println("❌ 获取语法错题列表失败: ${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override suspend fun getWrongAnswerByGrammarIdSync(grammarId: Int): GrammarWrongAnswer? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            grammarWrongAnswerDao.getWrongAnswerByGrammarIdSync(grammarId)?.toDomainModel()
        } catch (e: Exception) {
            println("❌ 获取语法错题(Sync)失败: grammarId=$grammarId, ${e.message}")
            null
        }
    }

    override fun getWrongAnswersByGrammarId(grammarId: Int): Flow<List<GrammarWrongAnswer>> {
        return grammarWrongAnswerDao.getWrongAnswersByGrammarId(grammarId)
            .map { entities ->
                entities.map { it.toDomainModel(null) }
            }
            .catch { e ->
                println("❌ 获取语法错题失败: grammarId=$grammarId, ${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override suspend fun getAllWrongGrammarIds(): List<Int> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            grammarWrongAnswerDao.getAllWrongGrammarIds()
        } catch (e: Exception) {
            println("❌ 获取错题语法ID列表失败: ${e.message}")
            emptyList()
        }
    }

    override suspend fun deleteByGrammarId(grammarId: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            grammarWrongAnswerDao.markDeletedByGrammarId(grammarId, com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis())
            println("✅ 标记记录为已删除: grammarId=$grammarId")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 删除语法错题失败: grammarId=$grammarId, ${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun clearAll(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            grammarWrongAnswerDao.deleteAll()
            println("✅ 清空所有语法错题成功")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 清空语法错题失败: ${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun insertWrongAnswer(wrongAnswer: GrammarWrongAnswer): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val entity = wrongAnswer.toEntity()
            grammarWrongAnswerDao.insert(entity)
            // println("✅ 插入语法错题成功: grammarId=${wrongAnswer.grammarId}")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 插入语法错题失败: ${e.message}")
            Result.Error(e)
        }
    }
}
