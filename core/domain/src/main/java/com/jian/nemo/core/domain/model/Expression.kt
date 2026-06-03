package com.jian.nemo.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 词法表达例句数据模型 (完全对齐 Word Dto 规范)
 */
@Serializable
data class ExpressionExample(
    @SerialName("ja")
    val ja: String,
    
    @SerialName("zh")
    val zh: String
)

/**
 * 词法表达词条核心模型 (Entity)
 * id 已升级为带分类前缀的字符ID，实现了永久的解耦容错
 */
@Serializable
data class Expression(
    /**
     * 全局唯一且不可变的字符串 ID (如 "col_0001")，用于 Supabase 迁移与本地双向同步
     */
    @SerialName("id")
    val id: String,
    
    @SerialName("level")
    val level: String,
    
    @SerialName("japanese")
    val japanese: String,
    
    @SerialName("furigana")
    val furigana: String,
    
    @SerialName("clozeShow")
    val clozeShow: String,
    
    @SerialName("clozeAnswers")
    val clozeAnswers: List<String>,
    
    @SerialName("chinese")
    val chinese: String,
    
    @SerialName("tip")
    val tip: String = "",
    
    @SerialName("synonyms")
    val synonyms: List<String> = emptyList(),
    
    @SerialName("examples")
    val examples: List<ExpressionExample> = emptyList()
)

/**
 * 词法表达分类主体模型 (Category)
 * 对应 expressions 目录下 collocation.json 等 6 大分类的顶级结构
 */
@Serializable
data class ExpressionCategory(
    @SerialName("levelId")
    val levelId: Int,
    
    @SerialName("categoryKey")
    val categoryKey: String,
    
    @SerialName("levelName")
    val levelName: String,
    
    @SerialName("levelEnglish")
    val levelEnglish: String = "",
    
    @SerialName("definition")
    val definition: String = "",
    
    @SerialName("features")
    val features: String = "",
    
    @SerialName("formula")
    val formula: String = "",
    
    @SerialName("items")
    val items: List<Expression> = emptyList()
)
