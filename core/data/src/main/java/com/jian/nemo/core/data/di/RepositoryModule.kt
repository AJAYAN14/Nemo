package com.jian.nemo.core.data.di

import com.jian.nemo.core.data.repository.GrammarRepositoryImpl
import com.jian.nemo.core.data.repository.GrammarWrongAnswerRepositoryImpl
import com.jian.nemo.core.data.repository.SettingsRepositoryImpl
import com.jian.nemo.core.data.repository.SessionRepositoryImpl
import com.jian.nemo.core.data.repository.StudyRecordRepositoryImpl
import com.jian.nemo.core.data.repository.WordRepositoryImpl
import com.jian.nemo.core.data.repository.WrongAnswerRepositoryImpl
import com.jian.nemo.core.domain.repository.GrammarRepository
import com.jian.nemo.core.domain.repository.GrammarWrongAnswerRepository
import com.jian.nemo.core.domain.repository.SettingsRepository
import com.jian.nemo.core.domain.repository.SessionRepository
import com.jian.nemo.core.domain.repository.StudyRecordRepository
import com.jian.nemo.core.domain.repository.WordRepository
import com.jian.nemo.core.domain.repository.WrongAnswerRepository
import com.jian.nemo.core.data.repository.AuthRepositoryImpl
import com.jian.nemo.core.data.repository.AudioRepositoryImpl
import com.jian.nemo.core.domain.repository.AuthRepository
import com.jian.nemo.core.domain.repository.AudioRepository
import com.jian.nemo.core.domain.repository.ContentReportRepository
import com.jian.nemo.core.data.repository.ContentReportRepositoryImpl
import com.jian.nemo.core.data.repository.SyncRepositoryImpl
import com.jian.nemo.core.data.repository.ConfigRepositoryImpl
import com.jian.nemo.core.data.repository.ContentRepositoryImpl
import com.jian.nemo.core.data.repository.ContentUpdateApplierImpl
import com.jian.nemo.core.domain.repository.SyncRepository
import com.jian.nemo.core.domain.repository.ConfigRepository
import com.jian.nemo.core.domain.repository.ContentRepository
import com.jian.nemo.core.domain.repository.ContentUpdateApplier
import com.jian.nemo.core.domain.repository.AIWorkshopRepository
import com.jian.nemo.core.data.repository.AIWorkshopRepositoryImpl
import com.jian.nemo.core.data.manager.DataExportManager
import com.jian.nemo.core.domain.repository.DataExportRepository
import com.jian.nemo.core.domain.service.SyncService
import com.jian.nemo.core.domain.service.SyncManager
import com.jian.nemo.core.data.service.SyncServiceImpl
import com.jian.nemo.core.data.manager.SyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository 依赖注入模块
 *
 * 使用 @Binds 代替 @Provides（更高效）
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * 绑定Word Repository实现
     */
    @Binds
    @Singleton
    abstract fun bindWordRepository(
        impl: WordRepositoryImpl
    ): WordRepository

    /**
     * 绑定Grammar Repository实现
     */
    @Binds
    @Singleton
    abstract fun bindGrammarRepository(
        impl: GrammarRepositoryImpl
    ): GrammarRepository

    /**
     * 绑定StudyRecord Repository实现
     */
    @Binds
    @Singleton
    abstract fun bindStudyRecordRepository(
        impl: StudyRecordRepositoryImpl
    ): StudyRecordRepository

    /**
     * 绑定WrongAnswer Repository实现
     */
    @Binds
    @Singleton
    abstract fun bindWrongAnswerRepository(
        impl: WrongAnswerRepositoryImpl
    ): WrongAnswerRepository

    /**
     * 绑定GrammarWrongAnswer Repository实现
     */
    @Binds
    @Singleton
    abstract fun bindGrammarWrongAnswerRepository(
        impl: GrammarWrongAnswerRepositoryImpl
    ): GrammarWrongAnswerRepository

    /**
     * 绑定Settings Repository实现
     */
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    /**
     * 绑定Session Repository实现
     */
    @Binds
    @Singleton
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ): SessionRepository

    /**
     * 绑定Auth Repository实现
     */
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAudioRepository(
        impl: AudioRepositoryImpl
    ): AudioRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: SyncRepositoryImpl
    ): SyncRepository

    @Binds
    @Singleton
    abstract fun bindDataExportRepository(
        impl: DataExportManager
    ): DataExportRepository

    @Binds
    @Singleton
    abstract fun bindSyncService(
        impl: SyncServiceImpl
    ): SyncService

    @Binds
    @Singleton
    abstract fun bindSyncManager(
        impl: SyncScheduler
    ): SyncManager

    companion object {
    }

    @Binds
    @Singleton
    abstract fun bindGrammarTestRepository(
        impl: com.jian.nemo.core.data.repository.GrammarTestRepositoryImpl
    ): com.jian.nemo.core.domain.repository.GrammarTestRepository

    @Binds
    @Singleton
    abstract fun bindReviewLogRepository(
        impl: com.jian.nemo.core.data.repository.ReviewLogRepositoryImpl
    ): com.jian.nemo.core.domain.repository.ReviewLogRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(
        impl: ConfigRepositoryImpl
    ): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindContentRepository(
        impl: ContentRepositoryImpl
    ): ContentRepository

    @Binds
    @Singleton
    abstract fun bindContentUpdateApplier(
        impl: ContentUpdateApplierImpl
    ): ContentUpdateApplier

    @Binds
    @Singleton
    abstract fun bindFavoriteQuestionRepository(
        impl: com.jian.nemo.core.data.repository.FavoriteQuestionRepositoryImpl
    ): com.jian.nemo.core.domain.repository.FavoriteQuestionRepository

    @Binds
    @Singleton
    abstract fun bindContentReportRepository(
        impl: ContentReportRepositoryImpl
    ): ContentReportRepository

    @Binds
    @Singleton
    abstract fun bindAIWorkshopRepository(
        impl: AIWorkshopRepositoryImpl
    ): AIWorkshopRepository

}
