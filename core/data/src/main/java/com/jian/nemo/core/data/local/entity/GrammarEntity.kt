package com.jian.nemo.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 语法实体（重构版）
 *
 * 对应表: grammars
 *
 * 重构说明:
 * - 移除了 conjunction1-4 字段（迁移到 GrammarUsageEntity.connection）
 * - 移除了 example1-3, translation1-3 字段（迁移到 GrammarExampleEntity）
 * - 移除了 explanation 字段（迁移到 GrammarUsageEntity.explanation）
 * - 移除了 attention 字段（迁移到 GrammarUsageEntity.notes）
 * - 保留核心字段，进度字段已分离到 GrammarStudyStateEntity
 *
 * 数据库版本: v3+
 */
@Entity(
    tableName = "grammars",
    indices = [
        Index(value = ["grammar_level"]),
        Index(value = ["raw_id"], unique = true)
    ]
)
data class GrammarEntity(
    @PrimaryKey
    val id: Int,

    /**
     * 后端唯一标识符 (例如: N1_001)
     */
    @ColumnInfo(name = "raw_id")
    val rawId: String,

    // ========== 核心内容字段 ==========
    /**
     * 语法条目
     */
    val grammar: String,

    /**
     * 语法等级 (N1-N5)
     */
    @ColumnInfo(name = "grammar_level")
    val grammarLevel: String,

    /**
     * 是否已下架（方案一：ID 永不删除，用字段标记）
     */
    @ColumnInfo(name = "is_delisted")
    val isDelisted: Boolean = false
)
