package com.jian.nemo.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 v2 → v3
 *
 * 主要变更：
 * 1. 创建 grammar_usages 表
 * 2. 创建 grammar_examples 表
 * 3. 重构 grammars 表（移除 conjunction、example、explanation、attention 字段）
 * 4. 清空旧的语法数据（建议重新导入）
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ========== 1. 创建新表 ==========

        // 创建 grammar_usages 表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS grammar_usages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                grammar_id INTEGER NOT NULL,
                subtype TEXT,
                connection TEXT NOT NULL,
                explanation TEXT NOT NULL,
                notes TEXT,
                usage_order INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(grammar_id) REFERENCES grammars(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE INDEX index_grammar_usages_grammar_id ON grammar_usages(grammar_id)
        """)

        // 创建 grammar_examples 表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS grammar_examples (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                usage_id INTEGER NOT NULL,
                sentence TEXT NOT NULL,
                translation TEXT NOT NULL,
                source TEXT,
                is_dialog INTEGER NOT NULL DEFAULT 0,
                example_order INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(usage_id) REFERENCES grammar_usages(id) ON DELETE CASCADE
            )
        """)

        database.execSQL("""
            CREATE INDEX index_grammar_examples_usage_id ON grammar_examples(usage_id)
        """)

        // ========== 2. 清空旧的语法数据 ==========
        // 建议：清空旧数据，重新导入新JSON（因为数据结构完全改变）
        database.execSQL("DELETE FROM grammars")

        // ========== 3. 重构 grammars 表 ==========
        // SQLite 不支持直接删除列，需要重建表

        // 3.1 创建新表
        database.execSQL("""
            CREATE TABLE grammars_new (
                id INTEGER PRIMARY KEY NOT NULL,
                grammar TEXT NOT NULL,
                grammar_level TEXT NOT NULL,
                repetitionCount INTEGER NOT NULL DEFAULT 0,
                interval INTEGER NOT NULL DEFAULT 0,
                easinessFactor REAL NOT NULL DEFAULT 2.5,
                nextReviewDate INTEGER NOT NULL DEFAULT 0,
                lastReviewedDate INTEGER,
                firstLearnedDate INTEGER,
                isFavorite INTEGER NOT NULL DEFAULT 0,
                isSkipped INTEGER NOT NULL DEFAULT 0,
                lastModifiedTime INTEGER NOT NULL
            )
        """)

        // 3.2 迁移数据
        database.execSQL("""
            INSERT INTO grammars_new (
                id, grammar, grammar_level, repetitionCount, interval,
                easinessFactor, nextReviewDate, lastReviewedDate,
                firstLearnedDate, isFavorite, isSkipped, lastModifiedTime
            )
            SELECT
                id, grammar, grammar_level, repetitionCount, interval,
                easinessFactor, nextReviewDate, lastReviewedDate,
                firstLearnedDate, isFavorite, isSkipped, lastModifiedTime
            FROM grammars
        """)

        // 3.3 删除旧表
        database.execSQL("DROP TABLE grammars")

        // 3.4 重命名新表
        database.execSQL("ALTER TABLE grammars_new RENAME TO grammars")

        // 3.5 重建索引
        database.execSQL("""
            CREATE INDEX index_grammars_grammar_level ON grammars(grammar_level)
        """)
        database.execSQL("""
            CREATE INDEX index_grammars_nextReviewDate ON grammars(nextReviewDate)
        """)
    }
}
