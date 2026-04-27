package com.jian.nemo.core.domain.repository

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
}
