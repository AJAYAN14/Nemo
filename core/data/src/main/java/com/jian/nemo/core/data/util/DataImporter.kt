package com.jian.nemo.core.data.util

import android.content.Context
import android.util.Log
import com.jian.nemo.core.data.local.dao.GrammarDao
import com.jian.nemo.core.data.local.dao.GrammarUsageDao
import com.jian.nemo.core.data.local.dao.GrammarExampleDao
import com.jian.nemo.core.data.local.dao.WordDao
import com.jian.nemo.core.data.local.dto.GrammarDto
import com.jian.nemo.core.data.local.dto.WordDto
import com.jian.nemo.core.data.local.entity.GrammarEntity
import com.jian.nemo.core.data.local.entity.GrammarUsageEntity
import com.jian.nemo.core.data.local.entity.GrammarExampleEntity
import com.jian.nemo.core.data.local.entity.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 数据导入工具
 * 负责从 assets 读取 JSON 并导入数据库
 */
class DataImporter(
    private val context: Context,
    private val json: Json
) {
    companion object {
        private const val TAG = "DataImporter"

        /**
         * 从字符串 ID 提取数字 ID
         *
         * 转换规则：
         * - "N1_001" -> 1001
         * - "N2_050" -> 2050
         * - "N5_123" -> 5123
         */
        fun extractNumericId(id: String): Int {
            val parts = id.split("_")
            val level = parts[0].substring(1).toInt() // "N1" -> 1
            val num = parts[1].toInt()                // "001" -> 1
            return level * 10000 + num
        }
    }
    /**
     * 导入所有单词数据 (智能同步版)
     *
     * 策略:
     * 1.不仅是导入，而是与现有数据库进行 "Sync"
     * 2.对比 Key: (level, japanese)
     * 3.如果存在: 更新字段 (保留 ID, 从而保留学习记录)
     * 4.如果不存在: 插入新记录
     * 5.如果 DB 有但 JSON 无: 标记为下架 (Soft Delete)
     */
    suspend fun importWords(wordDao: WordDao) = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "🚀 开始执行单词智能同步 (Smart Sync)...")
            
            // 1. 读取所有 JSON 数据
            val levels = listOf("N1", "N2", "N3", "N4", "N5")
            val jsonWords = mutableListOf<WordDto>()
            levels.forEach { level ->
                val words = readWordsFromAssets("word/$level.json")
                jsonWords.addAll(words)
            }
            Log.i(TAG, "📋 JSON 数据读取完毕: ${jsonWords.size} 条")

            // [FIX] 预处理 JSON 数据: 去重
            // 防止 JSON 本身含有违反唯一索引 (japanese, level) 的重复项
            // 策略: 后读取的覆盖先读取的 (或者保留第一个)，这里使用 map 以 key 覆盖为准
            val uniqueJsonMap = jsonWords.associateBy { "${it.level}_${it.japanese}" }
            val deduplicatedJsonWords = uniqueJsonMap.values
            if (jsonWords.size != deduplicatedJsonWords.size) {
                Log.w(TAG, "⚠️ JSON 数据存在重复，已去重: ${jsonWords.size} -> ${deduplicatedJsonWords.size}")
            }

            // 2. 读取所有数据库数据 (用于比对)
            val dbWords = wordDao.getAllWordsSync()
            // Key: level_japanese (唯一业务键)
            // 注意: 如果 DB 中已经有重复 (v13及以前)，associateBy 会丢弃部分数据。
            // 但我们在 v14 升级时，Unique Index 的创建应该已经强制处理了重复 (或导致重建)。
            val dbMap = dbWords.associateBy { "${it.level}_${it.japanese}" }
            Log.i(TAG, "🗄️ 数据库现有数据: ${dbWords.size} 条")

            val toInsert = mutableListOf<WordEntity>()
            val toUpdate = mutableListOf<WordEntity>()
            val jsonKeys = uniqueJsonMap.keys

            // 3. 遍历 JSON (去重后) 进行对比 (Insert / Update)
            deduplicatedJsonWords.forEach { dto ->
                // 确保 DTO 里的 level 也是 "N1" 格式 (大写)
                val key = "${dto.level}_${dto.japanese}"
                
                val existingEntity = dbMap[key]
                val newEntityData = dto.toEntity() // ID=0

                if (existingEntity != null) {
                    // [MATCH] 检查是否需要更新
                    // 我们比较除 ID 和 isDelisted 之外的字段
                    // 注意: toEntity() 默认 isDelisted=false(或json中的值), 但 db 中可能是 true (如果之前下架过现在又上架了)
                    // 所以逻辑是: 只要 JSON 里有，isDelisted 就应该强制为 false (复活)
                    
                    val mergedEntity = existingEntity.copy(
                        hiragana = newEntityData.hiragana,
                        chinese = newEntityData.chinese,
                        pos = newEntityData.pos,
                        example1 = newEntityData.example1,
                        gloss1 = newEntityData.gloss1,
                        example2 = newEntityData.example2,
                        gloss2 = newEntityData.gloss2,
                        example3 = newEntityData.example3,
                        gloss3 = newEntityData.gloss3,
                        // 强制复活: 如果 JSON 里有, 且 JSON 没说 delisted, 那么就是 false
                        isDelisted = newEntityData.isDelisted 
                    )

                    // 简单对比: 如果内容不同，加入更新队列
                    if (mergedEntity != existingEntity) {
                        toUpdate.add(mergedEntity)
                    }
                } else {
                    // [NEW]
                    toInsert.add(newEntityData)
                }
            }

            // 4. 找出不在 JSON 中的数据 (Dictionary Delist / Mark as Delisted)
            // 策略: DB 有，JSON 无 -> 标记 isDelisted = 1
            val toDelistIds = dbWords.filter { entity ->
                val key = "${entity.level}_${entity.japanese}"
                !jsonKeys.contains(key) && !entity.isDelisted
            }.map { it.id }

            // 5. 执行批量操作
            if (toInsert.isNotEmpty()) {
                Log.i(TAG, "📥 新增单词: ${toInsert.size} 条")
                // 使用分批插入防止 SQL 参数过多 (虽然 Room 也会处理，但手动分批更稳)
                toInsert.chunked(500).forEach { batch ->
                    wordDao.insertAll(batch)
                }
            } else {
                Log.i(TAG, "📥 新增单词: 0 条")
            }

            if (toUpdate.isNotEmpty()) {
                Log.i(TAG, "🔄 更新单词: ${toUpdate.size} 条 (ID 不变)")
                toUpdate.chunked(500).forEach { batch ->
                    wordDao.updateAll(batch)
                }
            } else {
                Log.i(TAG, "🔄 更新单词: 0 条")
            }

            if (toDelistIds.isNotEmpty()) {
                Log.i(TAG, "🗑️ 下架单词: ${toDelistIds.size} 条 (标记为 delisted)")
                toDelistIds.chunked(500).forEach { batchIds ->
                    wordDao.markAsDelisted(batchIds)
                }
            } else {
                Log.i(TAG, "🗑️ 下架单词: 0 条")
            }

            Log.i(TAG, "🎉 单词同步完成!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 单词同步失败: ${e.message}", e)
            throw e
        }
    }

    /**
     * 导入所有语法数据（重构版）
     *
     * 支持多表插入：GrammarEntity + GrammarUsageEntity + GrammarExampleEntity
     */
    suspend fun importGrammars(
        grammarDao: GrammarDao,
        grammarUsageDao: GrammarUsageDao,
        grammarExampleDao: GrammarExampleDao
    ) = withContext(Dispatchers.IO) {
        try {
            Log.e(TAG, "!!! 📖 开始导入语法...")
            val levels = listOf("N1", "N2", "N3", "N4", "N5")
            var totalCount = 0
            var totalUsages = 0
            var totalExamples = 0

            levels.forEach { level ->
                Log.e(TAG, "!!! 📖 准备读取 grammar/$level.json")
                val grammars = try {
                    readGrammarsFromAssets("grammar/$level.json")
                } catch (e: Exception) {
                    Log.e(TAG, "!!! ❌ 读取 $level 语法文件失败", e)
                    throw e
                }
                Log.e(TAG, "!!! ✅ $level 语法文件解析成功，共 ${grammars.size} 条")

                grammars.forEachIndexed { index, dto ->
                    Log.d(TAG, "  处理语法 ${index + 1}/${grammars.size}: ${dto.title} (ID: ${dto.id})")

                    // 1. 插入主表
                    val grammarEntity = dto.toGrammarEntity()
                    grammarDao.insertAll(listOf(grammarEntity))
                    Log.d(TAG, "    ✓ 主表插入成功")

                    // 2. 插入用法表
                    val usageEntities = dto.toUsageEntities()
                    Log.d(TAG, "    → 插入 ${usageEntities.size} 个用法")
                    val usageIds = grammarUsageDao.insertAll(usageEntities)
                    totalUsages += usageEntities.size

                    // 3. 插入例句表
                    val exampleEntities = dto.toExampleEntities(usageIds)
                    val exampleCount = exampleEntities.size
                    Log.d(TAG, "    → 插入 $exampleCount 个例句")
                    grammarExampleDao.insertAll(exampleEntities)
                    totalExamples += exampleCount
                }

                totalCount += grammars.size
                Log.e(TAG, "!!! ✅ $level 语法导入完成，共 ${grammars.size} 个")
            }

            Log.e(TAG, "!!! 🎉 所有语法导入完成，共 $totalCount 个语法，$totalUsages 个用法，$totalExamples 个例句")
        } catch (e: Exception) {
            Log.e(TAG, "!!! ❌ 语法导入失败: ${e.message}", e)
            e.printStackTrace()
            throw e
        }
    }

    /**
     * 从 assets 读取单词 JSON
     */
    private fun readWordsFromAssets(fileName: String): List<WordDto> {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.use { it.readText() }
        return json.decodeFromString(jsonString)
    }

    /**
     * 从 assets 读取语法 JSON
     */
    private fun readGrammarsFromAssets(fileName: String): List<GrammarDto> {
        val inputStream = context.assets.open(fileName)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val jsonString = reader.use { it.readText() }
        return json.decodeFromString(jsonString)
    }
}

/**
 * WordDto 转 WordEntity 扩展函数
 *
 * 新数据格式特点:
 * - level 直接从 JSON 读取，无需从文件名推断
 * - examples 是嵌套数组，需提取前3个
 */
fun WordDto.toEntity(): WordEntity {
    return WordEntity(
        id = DataImporter.extractNumericId(rawId),
        japanese = japanese,
        hiragana = hiragana,
        chinese = chinese,
        level = level,
        pos = pos,
        example1 = examples.getOrNull(0)?.japanese,
        gloss1 = examples.getOrNull(0)?.chinese,
        example2 = examples.getOrNull(1)?.japanese,
        gloss2 = examples.getOrNull(1)?.chinese,
        example3 = examples.getOrNull(2)?.japanese,
        gloss3 = examples.getOrNull(2)?.chinese,
        isDelisted = delisted
    )
}

/**
 * DTO → Entity 映射函数（重构版）
 */

/**
 * 将 GrammarDto 转换为 GrammarEntity（仅主表）
 */
fun GrammarDto.toGrammarEntity(): GrammarEntity {
    return GrammarEntity(
        id = DataImporter.extractNumericId(id),
        grammar = title,
        grammarLevel = level.uppercase(),
        isDelisted = delisted
    )
}

/**
 * 将 GrammarDto 的 usages 转换为 GrammarUsageEntity 列表
 */
fun GrammarDto.toUsageEntities(): List<GrammarUsageEntity> {
    val grammarId = DataImporter.extractNumericId(id)
    return usages.mapIndexed { index, usage ->
        GrammarUsageEntity(
            grammarId = grammarId,
            subtype = usage.subtype,
            connection = usage.connection,
            explanation = usage.explanation,
            notes = usage.notes,
            usageOrder = index
        )
    }
}

/**
 * 将 GrammarDto 的 examples 转换为 GrammarExampleEntity 列表
 * @param usageIds 插入 usage 后返回的 ID 列表
 */
fun GrammarDto.toExampleEntities(usageIds: List<Long>): List<GrammarExampleEntity> {
    val result = mutableListOf<GrammarExampleEntity>()

    usages.forEachIndexed { usageIndex, usage ->
        val usageId = usageIds[usageIndex].toInt()
        usage.examples.forEachIndexed { exampleIndex, example ->
            result.add(
                GrammarExampleEntity(
                    usageId = usageId,
                    sentence = example.sentence,
                    translation = example.translation,
                    source = example.source,
                    isDialog = example.isDialog,
                    exampleOrder = exampleIndex
                )
            )
        }
    }

    return result
}
