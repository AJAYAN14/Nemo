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
import com.jian.nemo.core.data.local.migration.MIGRATION_21_22
import com.jian.nemo.core.data.local.migration.MIGRATION_22_23
import com.jian.nemo.core.data.local.migration.MIGRATION_24_25
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * 鏁版嵁搴撲緷璧栨敞鍏ユā鍧?
 *
 * 鎻愪緵锛?
 * - NemoDatabase锛堝崟渚嬶級
 * - 鎵€鏈塂AO瀹炰緥
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 鎻愪緵NemoDatabase瀹炰緥
     *
     * 鈿狅笍 寮€鍙戦樁娈甸厤缃細
     * - version = 1
     * - fallbackToDestructiveMigration()锛堝厑璁哥牬鍧忔€ц縼绉伙級
     */
    /**
     * 鎻愪緵 Json 瀹炰緥锛堢敤浜庢暟鎹鍏ワ級
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
        databaseCallback: NemoDatabaseCallback  // 娉ㄥ叆鍥炶皟
    ): NemoDatabase {
        return Room.databaseBuilder(
            context,
            NemoDatabase::class.java,
            NemoDatabase.DATABASE_NAME
        )
            .addCallback(databaseCallback)  // 娣诲姞鍥炶皟
            .addMigrations(MIGRATION_2_3, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_14_15, MIGRATION_17_18, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_24_25)
            .fallbackToDestructiveMigration() // 褰撴壘涓嶅埌杩佺Щ璺緞鏃跺厑璁告竻绌哄苟閲嶅缓鏁版嵁搴?
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

    @Provides
    @Singleton
    fun provideAIReadingHistoryDao(database: NemoDatabase): AIReadingHistoryDao = database.aiReadingHistoryDao()
}
