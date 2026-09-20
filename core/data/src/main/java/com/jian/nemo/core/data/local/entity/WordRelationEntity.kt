package com.jian.nemo.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "word_relations",
    indices = [Index(value = ["word_id"])]
)
data class WordRelationEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "word_id")
    val wordId: Int,
    @ColumnInfo(name = "related_word_id")
    val relatedWordId: Int,
    @ColumnInfo(name = "relation_type")
    val relationType: String,
    val note: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String? = null
)
