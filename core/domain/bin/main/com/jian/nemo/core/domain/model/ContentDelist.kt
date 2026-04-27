package com.jian.nemo.core.domain.model

/**
 * 内容下架策略（方案一：ID 永不删除）
 *
 * 词条不在 JSON 中物理删除，用字段 [Word.isDelisted] / [Grammar.isDelisted] 标记。
 * 已下架词条不进入新词/复习队列。
 */
object ContentDelist {

    /**
     * 单词是否已下架（委托给模型字段）
     */
    fun Word.isDelisted(): Boolean = this.isDelisted

    /**
     * 语法是否已下架（委托给模型字段）
     */
    fun Grammar.isDelisted(): Boolean = this.isDelisted
}
