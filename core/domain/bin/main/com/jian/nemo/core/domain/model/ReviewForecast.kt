package com.jian.nemo.core.domain.model

/**
 * 复习预测模型
 *
 * 表示未来某一天预计需要复习的单词或语法数量
 */
data class ReviewForecast(
    /**
     * 日期 (Epoch Day)
     */
    val date: Long,

    /**
     * 预计复习单词数量
     */
    val wordCount: Int = 0,

    /**
     * 预计复习语法数量
     */
    val grammarCount: Int = 0
) {
    /**
     * 总计预计复习数量
     */
    val totalCount: Int get() = wordCount + grammarCount
}
