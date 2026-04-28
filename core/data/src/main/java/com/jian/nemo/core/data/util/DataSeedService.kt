package com.jian.nemo.core.data.util

import android.content.Context
import android.util.Log
import com.jian.nemo.core.data.local.NemoDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import com.jian.nemo.core.domain.repository.SettingsRepository
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
 */
@Singleton
class DataSeedService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val database: Provider<NemoDatabase>,
    private val settingsRepository: Provider<SettingsRepository>,
    private val dataExportManager: Provider<com.jian.nemo.core.data.manager.DataExportManager>
) {

    companion object {
        private const val TAG = "DataSeedService"
    }

    private val seedMutex = Mutex()

    /**
     * 确保基础数据已填充到数据库中。
     *
     * 策略变更：
     * - 如果本地已有云端版本号 (lastContentVersion > 0)，说明已进入云同步时代。
     * - 为了防止本地 JSON 覆盖云端修改，此时跳过本地 Assets 导入。
     */
    suspend fun ensureDataSeeded() = seedMutex.withLock {
        try {
            val db = database.get()
            val wordDao = db.wordDao()
            val grammarDao = db.grammarDao()

            // 1. 检查当前本地版本和数据量 (通过 Provider 延迟获取 repository)
            val repo = settingsRepository.get()
            val lastVersion = repo.getLastContentVersion()
            val wordCount = wordDao.getWordCount()
            val grammarCount = grammarDao.getGrammarCount()
            val grammarLevelCount = grammarDao.getGrammarLevelCount()

            Log.i(TAG, "📊 数据状态检查: LocalVersion=$lastVersion, WordCount=$wordCount, GrammarCount=$grammarCount")

            // 2. 如果已经有云端版本号且数据库不为空，跳过本地同步
            if (lastVersion > 0 && wordCount > 0) {
                Log.i(TAG, "✅ 已进入云同步模式 (V$lastVersion)，跳过本地 JSON 智能同步。词库更新将由 SupabaseSyncManager 接管。")
                return@withLock
            }

            // 3. 判断是否需要导入（仅在首次安装或数据丢失时）
            val needImportGrammars = grammarCount == 0 || grammarLevelCount < 5
            if (needImportGrammars) {
                Log.i(TAG, "⚠️ 数据不完整，将从 Assets 导入初始种子数据")
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
