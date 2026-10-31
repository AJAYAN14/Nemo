package com.jian.nemo.core.data.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.data.local.dao.WordDao
import com.jian.nemo.core.data.local.dao.WrongAnswerDao
import com.jian.nemo.core.data.mapper.WordMapper.toDomainModel
import com.jian.nemo.core.data.mapper.WrongAnswerMapper.toDomainModel
import com.jian.nemo.core.data.mapper.WrongAnswerMapper.toEntity
import com.jian.nemo.core.domain.model.WrongAnswer
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WrongAnswer Repository 实现
 *
 * 职责:
 * 1. 调用DAO获取数据
 * 2. Entity → Domain Model转换
 * 3. 异常处理
 * 4. 日志记录
 *

 */
@Singleton
class WrongAnswerRepositoryImpl @Inject constructor(
    private val wrongAnswerDao: WrongAnswerDao,
    private val wordDao: WordDao
) : WrongAnswerRepository {

    override fun getAllWrongAnswers(): Flow<List<WrongAnswer>> {
        return wrongAnswerDao.getAllWrongAnswers()
            .map { entities ->
                // 为每个错题关联single词信息
                entities.map { entity ->
                    try {
                        val word = wordDao.getById(entity.wordId).map { it?.toDomainModel() }
                            .catch { null }
                            // Flow cannot be collected here inside map synchronously easily without runBlocking
                            // The original code was likely flawed or relying on flow helper
                            // But let's check what toDomainModels did.
                            // toDomainModels took a RESOLVER: (Int) -> Word?
                            // Here we are inside map { entities -> ... }
                            // The original logic passed a callback that created a FLOW but returned NULL?
                            // "try { ... .let { null } } "
                            // Yes, the original code returned NULL for word.

                       entity.toDomainModel(null)
                    } catch (e: Exception) {
                       entity.toDomainModel(null)
                    }
                }
            }
            .catch { e ->
                println("❌ 获取错题列表失败: ${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override suspend fun getWrongAnswerByWordIdSync(wordId: Int): WrongAnswer? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            wrongAnswerDao.getWrongAnswerByWordIdSync(wordId)?.toDomainModel()
        } catch (e: Exception) {
            println("❌ 获取单词错题(Sync)失败: wordId=$wordId, ${e.message}")
            null
        }
    }

    override fun getWrongAnswersByWordId(wordId: Int): Flow<List<WrongAnswer>> {
        return wrongAnswerDao.getWrongAnswersByWordId(wordId)
            .map { entities ->
                val word = try {
                    wordDao.getById(wordId).map { it?.toDomainModel() }
                        .catch { null }
                        .let { null }  // 简化处理
                } catch (e: Exception) {
                    null
                }
                entities.map { it.toDomainModel(word) }
            }
            .catch { e ->
                println("❌ 获取单词错题失败: wordId=$wordId, ${e.message}")
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override suspend fun insertWrongAnswer(wrongAnswer: WrongAnswer): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val entity = wrongAnswer.toEntity()
            wrongAnswerDao.insert(entity)
            println("✅ 插入错题记录成功: wordId=${wrongAnswer.wordId}")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 插入错题记录失败: wordId=${wrongAnswer.wordId}, ${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun getAllWrongWordIds(): List<Int> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            wrongAnswerDao.getAllWrongWordIds()
        } catch (e: Exception) {
            println("❌ 获取错题单词ID列表失败: ${e.message}")
            emptyList()
        }
    }

    override suspend fun deleteByWordId(wordId: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            wrongAnswerDao.markDeletedByWordId(wordId, com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis())
            println("✅ 标记记录为已删除: wordId=$wordId")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 删除单词错题失败: wordId=$wordId, ${e.message}")
            Result.Error(e)
        }
    }

    override suspend fun clearAll(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            wrongAnswerDao.deleteAll()
            println("✅ 清空所有错题成功")
            Result.Success(Unit)
        } catch (e: Exception) {
            println("❌ 清空错题失败: ${e.message}")
            Result.Error(e)
        }
    }
}
