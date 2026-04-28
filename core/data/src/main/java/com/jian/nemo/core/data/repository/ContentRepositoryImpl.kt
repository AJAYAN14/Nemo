package com.jian.nemo.core.data.repository

import android.util.Log
import com.jian.nemo.core.domain.repository.ContentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import com.jian.nemo.core.domain.model.WordDto
import com.jian.nemo.core.domain.model.GrammarDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 词库云更新：从 Supabase Storage 拉取 content_version 与 word/grammar JSON
 *
 * 约定：桶名 [CONTENT_BUCKET]，文件 content_version.json、word/N1.json～N5.json、grammar/N1.json～N5.json
 */
@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val json: Json
) : ContentRepository {

    override suspend fun getRemoteContentVersion(): Int? = withContext(Dispatchers.IO) {
        try {
            // 从 sync_meta 表获取内容版本
            val meta = supabase.postgrest["sync_meta"]
                .select()
                .decodeSingleOrNull<ContentMetaDto>()
            meta?.contentVersion
        } catch (e: Exception) {
            Log.w(TAG, "getRemoteContentVersion failed: ${e.message}")
            null
        }
    }

    override suspend fun downloadWordJson(level: String): String? = withContext(Dispatchers.IO) {
        try {
            val path = "word/$level.json"
            val url = supabase.storage.from(CONTENT_BUCKET).publicUrl(path)
            URL(url).openStream().use { it.readBytes().decodeToString() }
        } catch (e: Exception) {
            Log.w(TAG, "downloadWordJson($level) failed: ${e.message}")
            null
        }
    }

    override suspend fun downloadGrammarJson(level: String): String? = withContext(Dispatchers.IO) {
        try {
            val path = "grammar/$level.json"
            val url = supabase.storage.from(CONTENT_BUCKET).publicUrl(path)
            URL(url).openStream().use { it.readBytes().decodeToString() }
        } catch (e: Exception) {
            Log.w(TAG, "downloadGrammarJson($level) failed: ${e.message}")
            null
        }
    }

    override suspend fun fetchAllRemoteWords(): List<WordDto> = withContext(Dispatchers.IO) {
        val allWords = mutableListOf<WordDto>()
        var offset = 0L
        val pageSize = 1000L
        try {
            while (true) {
                val batch = supabase.postgrest["dictionary_words"]
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("is_delisted", false)
                        }
                        order("id", Order.ASCENDING)
                        range(offset, offset + pageSize - 1)
                    }.decodeList<WordDto>()

                if (batch.isEmpty()) break
                
                // 诊断日志：捕获染色数据，验证数据源是否包含修改
                batch.find { it.hiragana.contains("14安卓") }?.let {
                    Log.w(TAG, "🎯 捕获到目标单词: ID=${it.id}, Japanese=${it.japanese}, Hiragana=${it.hiragana}")
                }

                allWords.addAll(batch)
                Log.d(TAG, "已拉取单词批次: ${batch.size} 条 (总计: ${allWords.size})")
                if (batch.size < pageSize) break
                offset += pageSize
            }
            allWords
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllRemoteWords failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun fetchAllRemoteGrammars(): List<GrammarDto> = withContext(Dispatchers.IO) {
        val allGrammars = mutableListOf<GrammarDto>()
        var offset = 0L
        val pageSize = 1000L
        try {
            while (true) {
                val batch = supabase.postgrest["dictionary_grammars"]
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("is_delisted", false)
                        }
                        order("id", Order.ASCENDING)
                        range(offset, offset + pageSize - 1)
                    }.decodeList<GrammarDto>()

                if (batch.isEmpty()) break
                allGrammars.addAll(batch)
                Log.d(TAG, "已拉取语法批次: ${batch.size} 条 (总计: ${allGrammars.size})")
                if (batch.size < pageSize) break
                offset += pageSize
            }
            allGrammars
        } catch (e: Exception) {
            Log.e(TAG, "fetchAllRemoteGrammars failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun fetchWordsModifiedSince(timestamp: String): List<WordDto> = withContext(Dispatchers.IO) {
        val allWords = mutableListOf<WordDto>()
        var offset = 0L
        val pageSize = 1000L
        try {
            while (true) {
                val batch = supabase.postgrest["dictionary_words"]
                    .select(columns = Columns.ALL) {
                        filter {
                            gt("updated_at", timestamp)
                        }
                        order("updated_at", Order.ASCENDING)
                        range(offset, offset + pageSize - 1)
                    }.decodeList<WordDto>()

                if (batch.isEmpty()) break
                allWords.addAll(batch)
                if (batch.size < pageSize) break
                offset += pageSize
            }
            allWords
        } catch (e: Exception) {
            Log.e(TAG, "fetchWordsModifiedSince failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun fetchGrammarsModifiedSince(timestamp: String): List<GrammarDto> = withContext(Dispatchers.IO) {
        val allGrammars = mutableListOf<GrammarDto>()
        var offset = 0L
        val pageSize = 1000L
        try {
            while (true) {
                val batch = supabase.postgrest["dictionary_grammars"]
                    .select(columns = Columns.ALL) {
                        filter {
                            gt("updated_at", timestamp)
                        }
                        order("updated_at", Order.ASCENDING)
                        range(offset, offset + pageSize - 1)
                    }.decodeList<GrammarDto>()

                if (batch.isEmpty()) break
                allGrammars.addAll(batch)
                if (batch.size < pageSize) break
                offset += pageSize
            }
            allGrammars
        } catch (e: Exception) {
            Log.e(TAG, "fetchGrammarsModifiedSince failed: ${e.message}", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "ContentRepository"
        const val CONTENT_BUCKET = "content"
        const val CONTENT_VERSION_FILE = "content_version.json"
    }
}

@Serializable
private data class ContentMetaDto(
    @SerialName("content_version") val contentVersion: Int
)
