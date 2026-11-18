package com.jian.nemo.feature.learning.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.jian.nemo.core.common.di.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LearningStatsDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CategoryPreferencesDataStore

@Module
@InstallIn(SingletonComponent::class)
object LearningDataStoreModule {

    @Provides
    @Singleton
    @LearningStatsDataStore
    fun provideLearningStatsDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope appScope: CoroutineScope
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(
                SharedPreferencesMigration(
                    context = context,
                    sharedPreferencesName = "nemo_learning_time_prefs"
                )
            ),
            scope = appScope,
            produceFile = { context.preferencesDataStoreFile("nemo_learning_time_prefs") }
        )
    }

    @Provides
    @Singleton
    @CategoryPreferencesDataStore
    fun provideCategoryPreferencesDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope appScope: CoroutineScope
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(
                SharedPreferencesMigration(
                    context = context,
                    sharedPreferencesName = "category_learning_prefs"
                )
            ),
            scope = appScope,
            produceFile = { context.preferencesDataStoreFile("category_learning_prefs") }
        )
    }
}
