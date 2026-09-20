package com.jian.nemo.core.domain.model

data class WordRelation(
    val id: Long,
    val wordId: Int,
    val relatedWordId: Int,
    val relationType: String,
    val note: String?
)

data class RelatedWordItem(
    val relation: WordRelation,
    val relatedWord: Word
)
