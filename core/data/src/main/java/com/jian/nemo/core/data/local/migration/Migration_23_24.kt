package com.jian.nemo.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库版本 23 -> 24 的迁移
 * - 为7个同步表增加 sync_status 字段，默认值为 "SYNCED"
 * - 新增 sync_audit_logs 表
 */
val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val tables = listOf(
            "word_study_states",
            "grammar_study_states",
            "study_records",
            "test_records",
            "wrong_answers",
            "grammar_wrong_answers",
            "favorite_questions"
        )
        
        // 给所有需要同步的表增加 sync_status 字段
        tables.forEach { tableName ->
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `sync_status` TEXT NOT NULL DEFAULT 'SYNCED'")
        }
        
        // 创建 sync_audit_logs 表
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_audit_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `timestamp` INTEGER NOT NULL, 
                `tableName` TEXT NOT NULL, 
                `operation` TEXT NOT NULL, 
                `recordCount` INTEGER NOT NULL, 
                `status` TEXT NOT NULL, 
                `errorMessage` TEXT, 
                `details` TEXT
            )
            """.trimIndent()
        )
    }
}
