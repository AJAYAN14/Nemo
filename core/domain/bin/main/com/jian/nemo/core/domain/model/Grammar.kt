package com.jian.nemo.core.domain.model

/**
 * 语法领域模型（重构版）
 *
 * 支持一个语法点有多个用法（usages）
 *
 * 特点:
 * 1. 纯Kotlin，不依赖Android框架
 * 2. 嵌套结构：Grammar -> Usages -> Examples
 * 3. 包含业务逻辑方法和向后兼容方法
 */
data class Grammar(
    val id: Int,

    // ========== 核心内容 ==========
    /**
     * 语法条目
     */
    val grammar: String,

    /**
     * 语法等级 (N1-N5)
     */
    val grammarLevel: String,

    /**
     * 是否已下架（不参与新词/复习队列）
     */
    val isDelisted: Boolean = false,

    /**
     * 用法列表
     * 一个语法点可能有多个用法
     */
    val usages: List<GrammarUsage>,

    // ========== SRS 复习字段 (FSRS 6) ==========
    override val repetitionCount: Int = 0,
    override val interval: Int = 0,
    override val stability: Float = 0f,
    override val difficulty: Float = 0f,
    override val nextReviewDate: Long = 0,
    override val lastReviewedDate: Long? = null,
    override val firstLearnedDate: Long? = null,

    // ========== 用户交互字段 ==========
    val isFavorite: Boolean = false,
    val isSkipped: Boolean = false,
    val buriedUntilDay: Long = 0,

    // ========== 元数据 ==========
    val lastModifiedTime: Long = System.currentTimeMillis()
) : SrsItem {

    /**
     * 是否已学习
     */
    val isLearned: Boolean
        get() = repetitionCount > 0

    /**
     * 是否到期复习
     * @param today 今天的Epoch Day
     */
    fun isDueForReview(today: Long): Boolean {
        return isLearned && nextReviewDate <= today && !isSkipped
    }

    // ========== 向后兼容方法（用于不需要修改的旧代码）==========

    /**
     * 获取第一个用法的接续方式
     * 兼容旧代码
     */
    fun getFirstConjunction(): String? = usages.firstOrNull()?.connection

    /**
     * 获取所有接续方式
     * 兼容旧代码
     */
    fun getAllConjunctions(): List<String> {
        return usages.map { it.connection }.distinct()
    }

    /**
     * 获取第一个用法的说明
     * 兼容旧代码
     */
    fun getFirstExplanation(): String = usages.firstOrNull()?.explanation ?: ""

    /**
     * 获取所有例句（扁平化）
     * 兼容旧代码
     * @return 例句和翻译的配对列表
     */
    fun getAllExamples(): List<Pair<String, String>> {
        return usages.flatMap { usage ->
            usage.examples.map { example ->
                example.sentence to example.translation
            }
        }
    }

    /**
     * 获取第一个用法的注意事项
     * 兼容旧代码
     */
    fun getFirstAttention(): String? = usages.firstOrNull()?.notes
}
