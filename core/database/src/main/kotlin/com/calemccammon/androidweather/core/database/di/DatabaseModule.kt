package com.calemccammon.androidweather.core.database.di

import android.content.Context
import androidx.room.Room
import com.calemccammon.androidweather.core.database.WeatherDatabase
import com.calemccammon.androidweather.core.database.dao.ForecastDao
import com.calemccammon.androidweather.core.database.dao.SavedPlaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun database(
        @ApplicationContext context: Context,
    ): WeatherDatabase = Room.databaseBuilder(context, WeatherDatabase::class.java, WeatherDatabase.FILE_NAME).build()

    @Provides
    fun savedPlaceDao(database: WeatherDatabase): SavedPlaceDao = database.savedPlaceDao()

    @Provides
    fun forecastDao(database: WeatherDatabase): ForecastDao = database.forecastDao()
}
