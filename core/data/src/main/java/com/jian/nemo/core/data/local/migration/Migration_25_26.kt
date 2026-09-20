package com.jian.nemo.core.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `word_relations` (
                `id` INTEGER NOT NULL,
                `word_id` INTEGER NOT NULL,
                `related_word_id` INTEGER NOT NULL,
                `relation_type` TEXT NOT NULL,
                `note` TEXT,
                `created_at` TEXT,
                PRIMARY KEY(`id`)
            )
        """)
        database.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_word_relations_word_id`
            ON `word_relations` (`word_id`)
        """)
    }
}
