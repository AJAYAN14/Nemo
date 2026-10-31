package com.jian.nemo.core.data.local.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 例句 JSON 数据传输对象
 *
 * 用于解析新数据格式中的嵌套例句结构
 */
@Serializable
data class ExampleDto(
    /**
     * 日语例句
     */
    @SerialName("ja")
    val japanese: String,

    /**
     * 中文翻译
     */
    @SerialName("zh")
    val chinese: String
)

/**
 * 单词 JSON 数据传输对象
 *

 * 用于从 assets 文件解析 JSON
 */
@Serializable
data class WordDto(
    /**
     * 原始ID（字符串格式，如 "N5_0001"）
     */
    @SerialName("id")
    val rawId: String,

    /**
     * JLPT等级（如 "N5"）
     */
    @SerialName("level")
    val level: String,

    /**
     * 日语原文
     */
    @SerialName("expression")
    val japanese: String,

    /**
     * 假名读音
     */
    @SerialName("kana")
    val hiragana: String,

    /**
     * 中文意思
     */
    @SerialName("meaning")
    val chinese: String,

    /**
     * 词性
     */
    @SerialName("pos")
    val pos: String? = null,

    /**
     * 例句列表
     */
    @SerialName("examples")
    val examples: List<ExampleDto> = emptyList(),

    /**
     * 是否已下架（JSON 中可写 "delisted": true，不删 ID）
     */
    @SerialName("delisted")
    val delisted: Boolean = false
)
