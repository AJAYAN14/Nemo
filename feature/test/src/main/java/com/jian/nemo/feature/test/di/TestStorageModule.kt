package com.jian.nemo.feature.test.di

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
annotation class VerbSessionDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ListeningSessionDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WordClozeSessionDataStore

@Module
@InstallIn(SingletonComponent::class)
object TestStorageModule {

    @Provides
    @Singleton
    @VerbSessionDataStore
    fun provideVerbSessionDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope appScope: CoroutineScope
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(
                SharedPreferencesMigration(
                    context = context,
                    sharedPreferencesName = "verb_ai_session"
                )
            ),
            scope = appScope,
            produceFile = { context.preferencesDataStoreFile("verb_ai_session") }
        )
    }

    @Provides
    @Singleton
    @ListeningSessionDataStore
    fun provideListeningSessionDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope appScope: CoroutineScope
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(
                SharedPreferencesMigration(
                    context = context,
                    sharedPreferencesName = "listening_local_session"
                )
            ),
            scope = appScope,
            produceFile = { context.preferencesDataStoreFile("listening_local_session") }
        )
    }

    @Provides
    @Singleton
    @WordClozeSessionDataStore
    fun provideWordClozeSessionDataStore(
        @ApplicationContext context: Context,
        @ApplicationScope appScope: CoroutineScope
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            migrations = listOf(
                SharedPreferencesMigration(
                    context = context,
                    sharedPreferencesName = "word_cloze_session"
                )
            ),
            scope = appScope,
            produceFile = { context.preferencesDataStoreFile("word_cloze_session") }
        )
    }
}
