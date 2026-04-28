package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.domain.model.WordDto
import com.jian.nemo.core.domain.model.GrammarDto

/**
 * 词库内容云更新
 *
 * 从 Supabase Storage 拉取 content_version 与 word/grammar JSON，供应用层合并到本地 DB。
 */
interface ContentRepository {

    /**
     * 获取云端当前词库版本号
     * @return 版本号，拉取失败返回 null
     */
    suspend fun getRemoteContentVersion(): Int?

    /**
     * 下载指定等级的单词 JSON 字符串
     * @param level N1～N5
     */
    suspend fun downloadWordJson(level: String): String?

    /**
     * 下载指定等级的语法 JSON 字符串
     * @param level N1～N5
     */
    suspend fun downloadGrammarJson(level: String): String?

    /**
     * [New] 从 Supabase 数据库拉取全量单词数据
     */
    suspend fun fetchAllRemoteWords(): List<WordDto>

    /**
     * 从 Supabase 数据库拉取全量语法数据
     */
    suspend fun fetchAllRemoteGrammars(): List<GrammarDto>

    /**
     * [New] 增量拉取自指定时间以来修改过的单词
     * @param timestamp ISO 8601 格式时间戳
     */
    suspend fun fetchWordsModifiedSince(timestamp: String): List<WordDto>

    /**
     * [New] 增量拉取自指定时间以来修改过的语法
     * @param timestamp ISO 8601 格式时间戳
     */
    suspend fun fetchGrammarsModifiedSince(timestamp: String): List<GrammarDto>
}
