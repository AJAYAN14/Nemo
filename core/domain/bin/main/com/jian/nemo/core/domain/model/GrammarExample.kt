package com.jian.nemo.core.domain.model

/**
 * 语法例句领域模型
 */
data class GrammarExample(
    /**
     * 例句（日文）
     * 包含假名标注，格式：汉字[假名]
     */
    val sentence: String,

    /**
     * 翻译（中文）
     */
    val translation: String,

    /**
     * 来源（可选）
     * 例如："2010年12月真题"
     */
    val source: String?,

    /**
     * 是否为对话
     */
    val isDialog: Boolean
)
