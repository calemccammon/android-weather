package com.calemccammon.androidweather.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.calemccammon.androidweather.core.datastore.StoredSettings
import com.calemccammon.androidweather.core.datastore.StoredSettingsSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    @Provides
    @Singleton
    fun settingsStore(
        @ApplicationContext context: Context,
    ): DataStore<StoredSettings> = settingsStore { context.dataStoreFile("settings.json") }
}

/** The settings store over the file [produceFile] names. A file that cannot be read is started afresh. */
fun settingsStore(produceFile: () -> File): DataStore<StoredSettings> =
    DataStoreFactory.create(
        serializer = StoredSettingsSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { StoredSettings() },
        produceFile = produceFile,
    )
