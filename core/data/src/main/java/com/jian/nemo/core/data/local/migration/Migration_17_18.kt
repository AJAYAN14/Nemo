package com.jian.nemo.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移: v17 -> v18
 *
 * 变更：
 * - 为 word_study_states 表添加 type 字段 (0=New, 1=Learn, 2=Review, 3=Relearn)
 * - 为 grammar_study_states 表添加 type 字段 (0=New, 1=Learn, 2=Review, 3=Relearn)
 *
 * 策略：
 * 1. 使用 ALTER TABLE ADD COLUMN 添加新列，默认值为 0 (New)
 * 2. 针对已有学习记录（repetition_count > 0），初步设为 2 (Review) 以维持 UI 显示一致
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // word_study_states 添加 type 字段
        database.execSQL("ALTER TABLE word_study_states ADD COLUMN type INTEGER NOT NULL DEFAULT 0")
        // 将已学习过的卡片标记为复习状态 (简单初始化)
        database.execSQL("UPDATE word_study_states SET type = 2 WHERE repetition_count > 0")

        // grammar_study_states 添加 type 字段
        database.execSQL("ALTER TABLE grammar_study_states ADD COLUMN type INTEGER NOT NULL DEFAULT 0")
        // 将已学习过的卡片标记为复习状态 (简单初始化)
        database.execSQL("UPDATE grammar_study_states SET type = 2 WHERE repetition_count > 0")
    }
}
