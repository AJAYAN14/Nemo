package com.jian.nemo.core.data.repository

import android.util.Log
import com.jian.nemo.core.data.local.dao.GrammarExampleDao
import com.jian.nemo.core.data.local.dao.GrammarUsageDao
import com.jian.nemo.core.data.local.dao.WordDao
import com.jian.nemo.core.data.util.toEntity
import com.jian.nemo.core.data.util.toGrammarEntity
import com.jian.nemo.core.data.util.toUsageEntities
import com.jian.nemo.core.data.util.toExampleEntities
import com.jian.nemo.core.data.local.dao.GrammarDao
import com.jian.nemo.core.data.local.dto.WordDto as LocalWordDto
import com.jian.nemo.core.data.local.dto.GrammarDto as LocalGrammarDto
import com.jian.nemo.core.domain.model.WordDto as NetworkWordDto
import com.jian.nemo.core.domain.model.GrammarDto as NetworkGrammarDto
import com.jian.nemo.core.domain.repository.ContentUpdateApplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 将云端词库 JSON 合并到本地 DB
 *
 * 单词：按 (level, japanese) 匹配则 UPDATE 内容，否则 INSERT。
 * 语法：按 id REPLACE 主表，并重写该 grammar 的 usages/examples。
 */
@Singleton
class ContentUpdateApplierImpl @Inject constructor(
    private val wordDao: WordDao,
    private val grammarDao: GrammarDao,
    private val grammarUsageDao: GrammarUsageDao,
    private val grammarExampleDao: GrammarExampleDao,
    private val json: Json
) : ContentUpdateApplier {

    override suspend fun applyWordsFromJson(level: String, jsonString: String): Int? =
        withContext(Dispatchers.IO) {
            try {
                val dtos = json.decodeFromString<List<LocalWordDto>>(jsonString)
                var updated = 0
                val jsonIds = dtos.map { it.japanese } // WordDto 用 japanese 作为内容唯一标识
                dtos.forEach { dto ->
                    val existing = wordDao.getWordByLevelAndJapanese(level, dto.japanese)
                    if (existing != null) {
                        val merged = existing.copy(
                            chinese = dto.chinese,
                            hiragana = dto.hiragana,
                            pos = dto.pos,
                            example1 = dto.examples.getOrNull(0)?.japanese,
                            gloss1 = dto.examples.getOrNull(0)?.chinese,
                            example2 = dto.examples.getOrNull(1)?.japanese,
                            gloss2 = dto.examples.getOrNull(1)?.chinese,
                            example3 = dto.examples.getOrNull(2)?.japanese,
                            gloss3 = dto.examples.getOrNull(2)?.chinese,
                            isDelisted = dto.delisted
                        )
                        wordDao.update(merged)
                        updated++
                    } else {
                        wordDao.insert(dto.toEntity())
                        updated++
                    }
                }

                // 【核心修复】标记在本等级下，但不在本次 JSON 中的词条为已下架 (Ghost Delisting)
                val delistedCount = wordDao.markMissingAsDelisted(level, jsonIds)
                Log.d(TAG, "applyWordsFromJson($level): $updated updated/inserted, $delistedCount ghost-delisted")
                updated
            } catch (e: Exception) {
                Log.e(TAG, "applyWordsFromJson($level) failed", e)
                null
            }
        }

    override suspend fun applyGrammarsFromJson(level: String, jsonString: String): Int? =
        withContext(Dispatchers.IO) {
            try {
                val dtos = json.decodeFromString<List<LocalGrammarDto>>(jsonString)
                var count = 0
                val jsonIntIds = mutableListOf<Int>()
                dtos.forEach { dto ->
                    val grammarEntity = dto.toGrammarEntity()
                    grammarDao.upsertAll(listOf(grammarEntity))
                    val grammarId = grammarEntity.id
                    jsonIntIds.add(grammarId)

                    grammarUsageDao.deleteByGrammarId(grammarId)
                    val usageEntities = dto.toUsageEntities()
                    val usageIds = grammarUsageDao.insertAll(usageEntities)
                    val exampleEntities = dto.toExampleEntities(usageIds)
                    grammarExampleDao.insertAll(exampleEntities)
                    count++
                }

                // 【核心修复】标记在本等级下，但不在本次 JSON 中的语法为已下架
                val delistedCount = grammarDao.markMissingAsDelisted(level, jsonIntIds)
                Log.d(TAG, "applyGrammarsFromJson($level): $count items, $delistedCount ghost-delisted")
                count
            } catch (e: Exception) {
                Log.e(TAG, "applyGrammarsFromJson($level) failed", e)
                null
            }
        }

    override suspend fun applyAllWords(words: List<NetworkWordDto>) {
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "applyAllWords: processing ${words.size} words")
                // 采用分批插入策略
                words.chunked(500).forEach { batch ->
                    val entities = batch.map { it.toEntity() }
                    wordDao.upsertAll(entities)
                }
                Log.i(TAG, "applyAllWords: success")
            } catch (e: Exception) {
                Log.e(TAG, "applyAllWords failed", e)
            }
        }
    }

    override suspend fun applyAllGrammars(grammars: List<NetworkGrammarDto>) {
        withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "applyAllGrammars: processing ${grammars.size} grammars")
                grammars.forEach { dto ->
                    // 1. 插入主表 (Upsert)
                    val grammarEntity = dto.toGrammarEntity()
                    grammarDao.upsertAll(listOf(grammarEntity))
                    val grammarId = grammarEntity.id

                    // 2. 重写用法和例句 (先删后增，保证数据最新且不重复)
                    grammarUsageDao.deleteByGrammarId(grammarId)
                    val usageEntities = dto.toUsageEntities()
                    if (usageEntities.isNotEmpty()) {
                        val usageIds = grammarUsageDao.insertAll(usageEntities)
                        val exampleEntities = dto.toExampleEntities(usageIds)
                        if (exampleEntities.isNotEmpty()) {
                            grammarExampleDao.insertAll(exampleEntities)
                        }
                    }
                }
                Log.i(TAG, "applyAllGrammars: success")
            } catch (e: Exception) {
                Log.e(TAG, "applyAllGrammars failed", e)
            }
        }
    }
    companion object {
        private const val TAG = "ContentUpdateApplier"
    }
}
