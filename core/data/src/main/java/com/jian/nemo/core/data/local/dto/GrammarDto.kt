package com.jian.nemo.core.data.local.dto

import kotlinx.serialization.Serializable

/**
 * 语法 JSON 数据传输对象（新格式）
 *
 * 基于新的分级语法数据结构

 */
@Serializable
data class GrammarDto(
    val id: String,                    // 新格式：字符串 ID（如 "N1_001"）
    val title: String,                 // 语法条目（原 grammarEntry）
    val usages: List<GrammarUsageDto>, // 用法列表（一个语法点可能有多个用法）
    val level: String,                 // 等级（N1-N5）
    val delisted: Boolean = false      // 是否已下架（不删 ID，用字段标记）
)

/**
 * 语法用法数据传输对象
 */
@Serializable
data class GrammarUsageDto(
    val subtype: String? = null,              // 用法子类型（可能为 null）
    val connection: String,            // 接续方式
    val explanation: String,           // 说明
    val examples: List<GrammarExampleDto>, // 例句列表
    val notes: String? = null                 // 注意事项（可能为 null）
)

/**
 * 语法例句数据传输对象
 */
@Serializable
data class GrammarExampleDto(
    val sentence: String,              // 例句
    val translation: String,           // 翻译
    val source: String? = null,               // 来源（可能为 null）
    val isDialog: Boolean              // 是否为对话
)
