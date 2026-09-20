package com.jian.nemo.core.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WordRelationDto(
    val id: Long,
    @SerialName("word_id") val wordId: Int,
    @SerialName("related_word_id") val relatedWordId: Int,
    @SerialName("relation_type") val relationType: String,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
