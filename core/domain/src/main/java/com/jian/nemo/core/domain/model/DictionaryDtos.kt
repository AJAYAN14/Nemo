package com.jian.nemo.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 词库同步结果
 */
data class DictionarySyncResult(
    val updatedWords: Int = 0,
    val updatedGrammars: Int = 0,
    val isFullSync: Boolean = false,
    val localVersion: Int = 0,
    val remoteVersion: Int = 0
)

@Serializable
data class WordDto(
    @SerialName("id") val id: Int,
    @SerialName("japanese") val japanese: String,
    @SerialName("hiragana") val hiragana: String,
    @SerialName("chinese") val chinese: String,
    @SerialName("level") val level: String,
    @SerialName("pos") val pos: String? = null,
    @SerialName("example_1") val example1: String? = null,
    @SerialName("gloss_1") val gloss1: String? = null,
    @SerialName("example_2") val example2: String? = null,
    @SerialName("gloss_2") val gloss2: String? = null,
    @SerialName("example_3") val example3: String? = null,
    @SerialName("gloss_3") val gloss3: String? = null,
    @SerialName("is_delisted") val isDelisted: Boolean = false,
    @SerialName("raw_id") val rawId: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class GrammarDto(
    @SerialName("id") val id: Int,
    @SerialName("raw_id") val rawId: String? = null,
    @SerialName("level") val level: String,
    @SerialName("title") val title: String,
    @SerialName("content") val content: GrammarContentDto,
    @SerialName("is_delisted") val isDelisted: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class GrammarContentDto(
    @SerialName("usages") val usages: List<GrammarUsageDto>
)

@Serializable
data class GrammarUsageDto(
    @SerialName("subtype") val subtype: String? = null,
    @SerialName("connection") val connection: String,
    @SerialName("explanation") val explanation: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("examples") val examples: List<GrammarExampleDto> = emptyList()
)

@Serializable
data class GrammarExampleDto(
    @SerialName("sentence") val sentence: String,
    @SerialName("translation") val translation: String,
    @SerialName("source") val source: String? = null,
    @SerialName("isDialog") val isDialog: Boolean = false
)
