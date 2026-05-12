package com.jian.nemo.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移: v20 -> v21
 *
 * 变更：
 * - 为 ai_exercises 表添加 grammar_point 字段 (TEXT, 可空)
 *   用于语法专项模式下记录练习所针对的语法点名称
 * - 为 ai_exercises 表添加 usage_id 字段 (INTEGER, 可空)
 *   用于追溯语法专项模式下具体的用法分支 ID
 */
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE ai_exercises ADD COLUMN grammar_point TEXT")
        database.execSQL("ALTER TABLE ai_exercises ADD COLUMN usage_id INTEGER")
    }
}
