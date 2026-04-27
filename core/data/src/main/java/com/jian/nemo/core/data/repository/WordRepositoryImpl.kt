package com.jian.nemo.core.data.repository

import com.jian.nemo.core.common.Result
import com.jian.nemo.core.data.local.dao.*
import com.jian.nemo.core.data.local.entity.TestRecordEntity
import com.jian.nemo.core.data.local.entity.WordEntity
import com.jian.nemo.core.data.local.entity.WordStudyStateEntity
import com.jian.nemo.core.data.mapper.WordMapper
import com.jian.nemo.core.data.mapper.WordMapper.toDomainModel
import com.jian.nemo.core.data.mapper.WordMapper.toDomainModels
import com.jian.nemo.core.data.mapper.WordMapper.toStudyStateEntity
import com.jian.nemo.core.domain.model.ContentDelist.isDelisted
import com.jian.nemo.core.domain.model.PartOfSpeech
import com.jian.nemo.core.domain.model.Word
import com.jian.nemo.core.domain.model.ReviewForecast
import com.jian.nemo.core.domain.repository.WordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Word Repository 实现
 *
 * 职责:
 * 1. 调用DAO获取数据
 * 2. Entity → Domain Model转换
 * 3. 异常处理

 */
@Singleton
class WordRepositoryImpl @Inject constructor(
    private val wordDao: WordDao,
    private val wordStudyStateDao: WordStudyStateDao,
    private val testRecordDao: TestRecordDao
) : WordRepository {

    private suspend fun mapWithStudyState(entities: List<WordEntity>): List<Word> {
        if (entities.isEmpty()) return emptyList()

        val statesById = wordStudyStateDao
            .getStatesByIds(entities.map { it.id })
            .associateBy { it.wordId }

        return entities.map { entity ->
            WordMapper.toDomainModel(entity, statesById[entity.id])
        }
    }

    // ========== 查询实现 ==========

    override fun getWordById(id: Int): Flow<Word?> {
        return combine(
            wordDao.getById(id),
            wordStudyStateDao.getByWordIdFlow(id)
        ) { entity, state ->
            entity?.let { WordMapper.toDomainModel(it, state) }
        }
            .catch { e ->
                emit(null)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getNewWords(level: String, isRandom: Boolean): Flow<List<Word>> {
        val flow = if (isRandom) {
            wordDao.getNewWordsByLevelRandom(level)
        } else {
            wordDao.getNewWordsByLevel(level)
        }

        return flow
            .map { entities ->
                mapWithStudyState(entities).filter { w -> !w.isDelisted() }
            }
            .catch { e ->
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getDueWords(today: Long): Flow<List<Word>> {
        return wordDao.getDueWords(today)
            .map { entities ->
                mapWithStudyState(entities).filter { w -> !w.isDelisted() }
            }
            .catch { e ->
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getDueWordsCount(today: Long): Flow<Int> {
        return getDueWords(today).map { it.size }
    }

    override fun getTodayLearnedWords(today: Long): Flow<List<Word>> {
        return wordDao.getTodayLearnedWords(today)
            .map { entities ->
                entities.toDomainModels()
            }
            .catch { e ->
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getTodayReviewedWords(today: Long): Flow<List<Word>> {
        return wordDao.getTodayReviewedWords(today)
            .map { entities ->
                entities.toDomainModels()
            }
            .catch { e ->
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getFavoriteWords(): Flow<List<Word>> {
        return wordDao.getFavoriteWords()
            .map { it.toDomainModels() }
            .catch { e ->
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override suspend fun getWordsSortedByDueScore(levels: List<String>, limit: Int): List<Word> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            if (levels.isEmpty()) return@withContext emptyList()
            val entities = wordDao.getWordsSortedByNextReviewDate(levels, limit)
            entities.toDomainModels()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getSkippedWords(limit: Int): Flow<List<Word>> {

        return wordDao.getSkippedWords(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getAllWordsByLevel(level: String): Flow<List<Word>> {
        // 转换为大写以匹配数据库中的level字段（N1, N2, N3, N4, N5）
        val upperLevel = level.uppercase()

        return wordDao.getAllWordsByLevel(upperLevel).map { entities ->

            entities.map { it.toDomainModel() }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getAllLearnedWords(): Flow<List<Word>> {
        return wordDao.getAllLearnedWords()
            .map { it.toDomainModels() }
            .catch { e ->
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getAllLearnedWordsByLevel(level: String): Flow<List<Word>> {
        val upperLevel = level.uppercase()
        return wordDao.getLearnedWordsByLevel(upperLevel)
            .map { it.toDomainModels() }
            .catch { e ->
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getLearnedWordCount(): Flow<Int> {
        return wordDao.getLearnedWordCount()
            .catch { e ->
                emit(0)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getReviewForecast(startDate: Long, endDate: Long): Flow<List<ReviewForecast>> {
        return wordDao.getReviewForecast(startDate, endDate)
            .map { tuples ->
                tuples.map {
                    ReviewForecast(date = it.date, wordCount = it.count)
                }
            }
            .catch { e ->
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }


    override fun searchWords(query: String): Flow<List<Word>> {
        return wordDao.searchWords(query)
            .map { it.toDomainModels() }
            .catch { e ->
                emit(emptyList())
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getTodayTestCount(today: Long): Flow<Int> {
        return testRecordDao.getRecordsByDate(today)
            .map { it.size }
            .catch { e ->
                emit(0)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getTodayTestAccuracy(today: Long): Flow<Float> {
        return testRecordDao.getRecordsByDate(today)
            .map { records ->
                if (records.isEmpty()) return@map 0f
                val totalQuestions = records.sumOf { it.totalQuestions }
                val totalCorrect = records.sumOf { it.correctAnswers }
                if (totalQuestions > 0) totalCorrect.toFloat() / totalQuestions else 0f
            }
            .catch { e ->
                emit(0f)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getTotalTestCount(): Flow<Int> {
        return testRecordDao.getTotalTestCount()
            .catch { e ->
                emit(0)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getOverallAccuracy(): Flow<Float> {
        return kotlinx.coroutines.flow.combine(
            testRecordDao.getTotalCorrectAnswers(),
            testRecordDao.getTotalQuestions()
        ) { correct, total ->
            val c = correct ?: 0
            val t = total ?: 0
            if (t > 0) c.toFloat() / t else 0f
        }.catch { e ->

            emit(0f)
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getTotalQuestionCount(): Flow<Int> {
        return testRecordDao.getTotalQuestions()
            .map { it ?: 0 }
            .catch { e ->
                emit(0)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getTotalCorrectAnswerCount(): Flow<Int> {
        return testRecordDao.getTotalCorrectAnswers()
            .map { it ?: 0 }
            .catch { e ->
                emit(0)
            }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    override fun getTodayLearnedLevels(todayEpochDay: Long): Flow<List<String>> {
        return wordDao.getTodayLearnedLevels(todayEpochDay)
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getFavoriteLevels(): Flow<List<String>> {
        return wordDao.getFavoriteLevels()
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getLearnedLevels(): Flow<List<String>> {
        return wordDao.getLearnedLevels()
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getTodayReviewedLevels(todayEpochDay: Long): Flow<List<String>> {
        return wordDao.getTodayReviewedLevels(todayEpochDay)
            .catch { e ->
                emit(emptyList())
            }
    }

    override fun getWrongAnswerLevels(): Flow<List<String>> {
        return wordDao.getWrongAnswerLevels()
            .catch { e ->
                emit(emptyList())
            }
    }

    override suspend fun getLoanWords(): List<Word> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val allWords = wordDao.getAllWords().toDomainModels()
            allWords.filter { word ->
                val japanese = word.japanese
                val hiragana = word.hiragana
                
                // 1. expression (japanese) 包含英文字母
                val hasEnglish = japanese.contains(Regex("[a-zA-Z]"))
                
                // 用于去除干扰符号的正则
                val symbolRegex = Regex("[・〜ー\\s\\-()（）/]")
                
                // 2. expression (japanese) 去除符号后全为片假名
                val jCleaned = japanese.replace(symbolRegex, "")
                val isKatakanaJapanese = jCleaned.isNotEmpty() && jCleaned.all { ch ->
                    Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.KATAKANA
                }
                
                // 3. kana (hiragana) 去除符号后全为片假名
                val hCleaned = hiragana.replace(symbolRegex, "")
                val isKatakanaKana = hCleaned.isNotEmpty() && hCleaned.all { ch ->
                    Character.UnicodeBlock.of(ch) == Character.UnicodeBlock.KATAKANA
                }
                
                hasEnglish || isKatakanaJapanese || isKatakanaKana
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getWordsByPartOfSpeech(pos: PartOfSpeech): List<Word> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            if (pos == PartOfSpeech.LOAN_WORD) {
                return@withContext getLoanWords()
            }

            val entities = when (pos) {
                PartOfSpeech.VERB -> wordDao.getVerbs()
                PartOfSpeech.NOUN -> wordDao.getNouns()
                PartOfSpeech.ADJECTIVE -> wordDao.getAdjectives()
                PartOfSpeech.ADVERB -> wordDao.getAdverbs()
                PartOfSpeech.PARTICLE -> wordDao.getParticles()
                PartOfSpeech.CONJUNCTION -> wordDao.getConjunctions()
                PartOfSpeech.RENTAI -> wordDao.getRentai()
                PartOfSpeech.PREFIX -> wordDao.getPrefixes()
                PartOfSpeech.SUFFIX -> wordDao.getSuffixes()
                PartOfSpeech.INTERJECTION -> wordDao.getInterjections()
                PartOfSpeech.FIXED_EXPRESSION -> wordDao.getFixedExpressions()
                else -> emptyList()
            }
            entities.toDomainModels()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getWordsByIds(ids: List<Int>): List<Word> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            if (ids.isEmpty()) {
                emptyList()
            } else {
                mapWithStudyState(wordDao.getWordsByIds(ids))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ========== 更新实现 ==========

    override suspend fun updateWord(word: Word): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            // 强制刷新时间戳，确保同步系统能捕捉到改动
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            val stateEntity = word.toStudyStateEntity().copy(lastModifiedTime = now)

            wordStudyStateDao.insert(stateEntity)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateFavoriteStatus(
        wordId: Int,
        isFavorite: Boolean
    ): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        return@withContext try {
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            val rowsUpdated = wordStudyStateDao.updateFavoriteStatus(wordId, isFavorite, now)
            
            if (rowsUpdated == 0) {
                // 如果没有更新任何行，说明记录不存在，需要插入
                val newState = WordStudyStateEntity(
                    wordId = wordId,
                    isFavorite = isFavorite,
                    lastModifiedTime = now
                )
                wordStudyStateDao.insert(newState)

            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun markAsSkipped(wordId: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            wordStudyStateDao.updateSkipStatus(wordId, true, now)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun unmarkAsSkipped(wordId: Int): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            wordStudyStateDao.updateSkipStatus(wordId, false, now)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // ========== 批量操作 ==========

    override suspend fun resetAllProgress(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            wordStudyStateDao.resetAllProgress(now)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun clearAllFavorites(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val now = com.jian.nemo.core.common.util.DateTimeUtils.getCurrentCompensatedMillis()
            wordStudyStateDao.clearAllFavorites(now)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    override suspend fun saveTestRecord(record: com.jian.nemo.core.domain.model.TestRecord): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val entity = TestRecordEntity(
                id = record.id,
                date = record.date,
                totalQuestions = record.totalQuestions,
                correctAnswers = record.correctAnswers,
                testMode = record.testMode,
                timestamp = record.timestamp
            )
            testRecordDao.insert(entity)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
