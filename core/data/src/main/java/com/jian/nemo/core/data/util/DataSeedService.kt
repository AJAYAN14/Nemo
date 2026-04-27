package com.jian.nemo.core.data.util

import android.content.Context
import android.util.Log
import com.jian.nemo.core.data.local.NemoDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 数据填充服务
 *
 * 负责检查数据库中的基础数据（单词、语法）是否完整，并在必要时从 assets 导入。
 * 此逻辑从 NemoDatabaseCallback 中提取，以便在以下场景复用：
 * 1. 数据库首次打开时（onOpen 回调）
 * 2. 用户登录/注册成功后（clearAllTables 后数据需要重新填充）
 */
@Singleton
class DataSeedService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val database: Provider<NemoDatabase>,
    private val dataExportManager: Provider<com.jian.nemo.core.data.manager.DataExportManager>
) {

    companion object {
        private const val TAG = "DataSeedService"
    }

    private val seedMutex = Mutex()

    /**
     * 确保基础数据已填充到数据库中。
     *
     * 检查单词表和语法表的数据量，根据条件执行导入：
     * - 单词：总是执行智能同步 (Smart Sync)
     * - 语法：仅在数据不完整时导入（数量为 0 或等级不足 5 个）
     *
     * 此方法是幂等的，可安全重复调用。
     */
    suspend fun ensureDataSeeded() = seedMutex.withLock {
        try {
            val db = database.get()
            val wordDao = db.wordDao()
            val grammarDao = db.grammarDao()

            // 检查单词数据量
            val wordCount = wordDao.getWordCount()
            Log.i(TAG, "📊 当前单词数量: $wordCount")

            // 检查语法数据量
            val grammarCount = grammarDao.getGrammarCount()
            val grammarLevelCount = grammarDao.getGrammarLevelCount()
            Log.i(TAG, "📊 当前语法数量: $grammarCount, 等级数: $grammarLevelCount")

            // 判断语法是否需要导入
            val needImportGrammars = grammarCount == 0 || grammarLevelCount < 5
            if (needImportGrammars) {
                Log.i(TAG, "⚠️ 语法数据不完整 (count=$grammarCount, levels=$grammarLevelCount)，将触发导入")
            }

            // 执行数据导入
            val importer = DataImporter(context, json)

            // 单词：总是智能同步
            Log.i(TAG, "📖 正在同步单词数据 (Smart Sync)...")
            importer.importWords(wordDao)

            // 语法：按需导入
            if (needImportGrammars) {
                Log.i(TAG, "📖 正在导入语法数据...")
                importer.importGrammars(
                    grammarDao = grammarDao,
                    grammarUsageDao = db.grammarUsageDao(),
                    grammarExampleDao = db.grammarExampleDao()
                )
            }

            Log.i(TAG, "📖 正在检查并修复冗余重复数据...")
            dataExportManager.get().repairDataDuplicates()

            Log.i(TAG, "🎉 数据填充与修复完成")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 数据填充失败: ${e.message}", e)
        }
    }
}
