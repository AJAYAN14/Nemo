package com.jian.nemo.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移: v22 -> v23
 *
 * 变更：
 * - 修复单词表 `words` 中 `(japanese, level)` 的唯一索引约束，改为普通索引，以允许同形异义词（如 N5的 一日/いちにち 和 一日/ついたち）同时存在。
 */
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 删除旧的唯一索引
        database.execSQL("DROP INDEX IF EXISTS `index_words_japanese_level`")
        // 创建新的非唯一索引
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_words_japanese_level` ON `words` (`japanese`, `level`)")
    }
}
