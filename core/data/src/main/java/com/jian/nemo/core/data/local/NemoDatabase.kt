package com.jian.nemo.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jian.nemo.core.data.local.dao.*
import com.jian.nemo.core.data.local.entity.*

/**
 * Nemo 2.0 主数据库
 */
@Database(
    entities = [
        WordEntity::class,
        GrammarEntity::class,
        GrammarUsageEntity::class,
        GrammarExampleEntity::class,
        StudyRecordEntity::class,
        TestRecordEntity::class,
        WrongAnswerEntity::class,
        GrammarWrongAnswerEntity::class,
        UserEntity::class,
        SettingsEntity::class,
        ReviewLogEntity::class,
        SyncMetadataEntity::class,
        WordStudyStateEntity::class,
        GrammarStudyStateEntity::class,
        FavoriteQuestionEntity::class,
        AIExerciseEntity::class,
        AIReadingHistoryEntity::class
    ],
    version = 22,  // v22: 引入 AI 智能阅读历史表 ai_reading_history
    exportSchema = true
)
abstract class NemoDatabase : RoomDatabase() {

    abstract fun wordDao(): WordDao
    abstract fun grammarDao(): GrammarDao
    abstract fun grammarUsageDao(): GrammarUsageDao
    abstract fun grammarExampleDao(): GrammarExampleDao
    abstract fun studyRecordDao(): StudyRecordDao
    abstract fun testRecordDao(): TestRecordDao
    abstract fun wrongAnswerDao(): WrongAnswerDao
    abstract fun grammarWrongAnswerDao(): GrammarWrongAnswerDao
    abstract fun userDao(): UserDao
    abstract fun settingsDao(): SettingsDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    abstract fun wordStudyStateDao(): WordStudyStateDao
    abstract fun grammarStudyStateDao(): GrammarStudyStateDao
    abstract fun favoriteQuestionDao(): FavoriteQuestionDao
    abstract fun aiExerciseDao(): AIExerciseDao
    abstract fun aiReadingHistoryDao(): AIReadingHistoryDao

    companion object {
        const val DATABASE_NAME = "nemo_database"
    }
}
