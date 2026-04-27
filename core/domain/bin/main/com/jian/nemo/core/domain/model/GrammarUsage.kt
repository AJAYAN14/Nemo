package com.jian.nemo.core.domain.model

/**
 * 语法用法领域模型
 *
 * 代表一个语法点的具体用法
 * 例如："～あがる"有"完成"和"向上"两种用法
 */
data class GrammarUsage(
    /**
     * 用法子类型（可选）
     * 例如："完成"、"向上"、"强调"
     */
    val subtype: String?,

    /**
     * 接续方式
     * 例如："动词「ます形」＋あがる"
     */
    val connection: String,

    /**
     * 说明/解释
     */
    val explanation: String,

    /**
     * 注意事项（可选）
     */
    val notes: String?,

    /**
     * 例句列表
     */
    val examples: List<GrammarExample>
)
