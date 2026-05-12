package com.jian.nemo.core.data.di

import android.content.Context
import androidx.room.Room
import com.jian.nemo.core.data.local.NemoDatabase
import com.jian.nemo.core.data.local.NemoDatabaseCallback
import com.jian.nemo.core.data.local.dao.*
import com.jian.nemo.core.data.local.migration.MIGRATION_2_3
import com.jian.nemo.core.data.local.migration.MIGRATION_11_12
import com.jian.nemo.core.data.local.migration.MIGRATION_12_13
import com.jian.nemo.core.data.local.migration.MIGRATION_14_15
import com.jian.nemo.core.data.local.migration.MIGRATION_17_18
import com.jian.nemo.core.data.local.migration.MIGRATION_20_21
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * 数据库依赖注入模块
 *
 * 提供：
 * - NemoDatabase（单例）
 * - 所有DAO实例
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 提供NemoDatabase实例
     *
     * ⚠️ 开发阶段配置：
     * - version = 1
     * - fallbackToDestructiveMigration()（允许破坏性迁移）
     */
    /**
     * 提供 Json 实例（用于数据导入）
     */
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    @Provides
    @Singleton
    fun provideNemoDatabase(
        @ApplicationContext context: Context,
        databaseCallback: NemoDatabaseCallback  // 注入回调
    ): NemoDatabase {
        return Room.databaseBuilder(
            context,
            NemoDatabase::class.java,
            NemoDatabase.DATABASE_NAME
        )
            .addCallback(databaseCallback)  // 添加回调
            .addMigrations(MIGRATION_2_3, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_14_15, MIGRATION_17_18, MIGRATION_20_21)
            .fallbackToDestructiveMigration() // 当找不到迁移路径时允许清空并重建数据库
            .build()
    }

    @Provides
    @Singleton
    fun provideWordDao(database: NemoDatabase): WordDao = database.wordDao()

    @Provides
    @Singleton
    fun provideGrammarDao(database: NemoDatabase): GrammarDao = database.grammarDao()

    @Provides
    @Singleton
    fun provideGrammarUsageDao(database: NemoDatabase): GrammarUsageDao = database.grammarUsageDao()

    @Provides
    @Singleton
    fun provideGrammarExampleDao(database: NemoDatabase): GrammarExampleDao = database.grammarExampleDao()

    @Provides
    @Singleton
    fun provideStudyRecordDao(database: NemoDatabase): StudyRecordDao = database.studyRecordDao()

    @Provides
    @Singleton
    fun provideTestRecordDao(database: NemoDatabase): TestRecordDao = database.testRecordDao()

    @Provides
    @Singleton
    fun provideWrongAnswerDao(database: NemoDatabase): WrongAnswerDao = database.wrongAnswerDao()

    @Provides
    @Singleton
    fun provideGrammarWrongAnswerDao(database: NemoDatabase): GrammarWrongAnswerDao = database.grammarWrongAnswerDao()

    @Provides
    @Singleton
    fun provideUserDao(database: NemoDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideSettingsDao(database: NemoDatabase): SettingsDao = database.settingsDao()

    @Provides
    @Singleton
    fun provideReviewLogDao(database: NemoDatabase): ReviewLogDao = database.reviewLogDao()

    @Provides
    @Singleton
    fun provideSyncMetadataDao(database: NemoDatabase): SyncMetadataDao = database.syncMetadataDao()

    @Provides
    @Singleton
    fun provideWordStudyStateDao(database: NemoDatabase): WordStudyStateDao = database.wordStudyStateDao()

    @Provides
    @Singleton
    fun provideGrammarStudyStateDao(database: NemoDatabase): GrammarStudyStateDao = database.grammarStudyStateDao()

    @Provides
    @Singleton
    fun provideFavoriteQuestionDao(database: NemoDatabase): FavoriteQuestionDao = database.favoriteQuestionDao()

    @Provides
    @Singleton
    fun provideAIExerciseDao(database: NemoDatabase): AIExerciseDao = database.aiExerciseDao()
}
