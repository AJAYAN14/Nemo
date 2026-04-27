package com.jian.nemo.core.domain.repository

import com.jian.nemo.core.common.Result

/**
 * 内容报错 Repository 接口
 */
interface ContentReportRepository {
    /**
     * 报告内容错误
     *
     * @param itemId 条目 ID (单词或语法 ID)
     * @param itemType 条目类型 ("word" 或 "grammar")
     */
    suspend fun reportContentError(itemId: Int, itemType: String): Result<Unit>
}
