package com.jian.nemo.core.domain.repository

/**
 * 将云端词库 JSON 合并到本地 DB（按等级）
 *
 * 单词：按 (level, japanese) 匹配则 UPDATE，否则 INSERT。
 * 语法：按 id REPLACE 主表并重写 usages/examples。
 */
interface ContentUpdateApplier {

    /**
     * 将指定等级的单词 JSON 合并到本地
     * @return 本等级更新/插入的条数，失败返回 null
     */
    suspend fun applyWordsFromJson(level: String, jsonString: String): Int?

    /**
     * 将指定等级的语法 JSON 合并到本地
     * @return 本等级更新/插入的条数，失败返回 null
     */
    suspend fun applyGrammarsFromJson(level: String, jsonString: String): Int?
}
