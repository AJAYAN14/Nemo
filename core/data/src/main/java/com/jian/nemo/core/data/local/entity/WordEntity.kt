package com.jian.nemo.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * - 数据库列名保持下划线格式(与旧项目一致)，Kotlin属性使用驼峰命名
 * - 使用 @ColumnInfo 映射数据库列名，如: example_1 → example1
 */
@Entity(
    tableName = "words",
    indices = [
        Index(value = ["level"]),
        Index(value = ["pos"]),
        Index(value = ["japanese", "level"], unique = true)
    ]
)
data class WordEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int,

    // ========== 核心内容字段 ==========
    /**
     * 日语原文
     */
    val japanese: String,

    /**
     * 假名读音
     */
    val hiragana: String,

    /**
     * 中文意思
     */
    val chinese: String,

    /**
     * JLPT等级 (N1-N5)
     */
    val level: String,

    /**
     * 词性 (Part of Speech)
     * 用于12种词性分类，如: "名", "自動1", "他動2", "イ形", "副" 等
     */
    val pos: String? = null,

    // ========== 例句字段（最多3个）==========
    @ColumnInfo(name = "example_1")
    val example1: String? = null,

    @ColumnInfo(name = "gloss_1")
    val gloss1: String? = null,

    @ColumnInfo(name = "example_2")
    val example2: String? = null,

    @ColumnInfo(name = "gloss_2")
    val gloss2: String? = null,

    @ColumnInfo(name = "example_3")
    val example3: String? = null,

    @ColumnInfo(name = "gloss_3")
    val gloss3: String? = null,

    /**
     * 是否已下架（方案一：ID 永不删除，用字段标记）
     * true 则不参与新词/复习队列
     */
    @ColumnInfo(name = "is_delisted")
    val isDelisted: Boolean = false
)
