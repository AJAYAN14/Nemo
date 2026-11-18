package com.jian.nemo.feature.learning.di

import com.jian.nemo.feature.learning.data.preferences.CategoryLearningPreferences
import com.jian.nemo.feature.learning.data.preferences.CategoryLearningPreferencesImpl
import com.jian.nemo.feature.learning.data.preferences.DataStoreLearningSessionStatsStorage
import com.jian.nemo.feature.learning.data.preferences.LearningSessionStatsStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LearningModule {

    @Binds
    @Singleton
    abstract fun bindCategoryLearningPreferences(
        impl: CategoryLearningPreferencesImpl
    ): CategoryLearningPreferences

    @Binds
    @Singleton
    abstract fun bindLearningSessionStatsStorage(
        impl: DataStoreLearningSessionStatsStorage
    ): LearningSessionStatsStorage
}
