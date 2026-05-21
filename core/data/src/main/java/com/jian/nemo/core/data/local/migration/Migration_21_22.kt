package com.jian.nemo.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移: v21 -> v22
 *
 * 变更：
 * - 创建新表 `ai_reading_history` 用于保存 AI 日语阅读历史以及用户答题状态
 */
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `ai_reading_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `title` TEXT NOT NULL, 
                `level` TEXT NOT NULL, 
                `content_raw` TEXT NOT NULL, 
                `translation` TEXT NOT NULL, 
                `vocabulary_json` TEXT NOT NULL, 
                `questions_json` TEXT NOT NULL, 
                `selected_answers_json` TEXT NOT NULL, 
                `is_submitted` INTEGER NOT NULL, 
                `created_at` INTEGER NOT NULL
            )
        """.trimIndent())
    }
}
