package com.calemccammon.androidweather.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.calemccammon.androidweather.core.database.dao.ForecastDao
import com.calemccammon.androidweather.core.database.dao.SavedPlaceDao
import com.calemccammon.androidweather.core.database.model.DailyForecastEntity
import com.calemccammon.androidweather.core.database.model.ForecastEntity
import com.calemccammon.androidweather.core.database.model.SavedPlaceEntity

/**
 * Everything the app keeps on the device apart from settings: saved places
 * and the forecasts last fetched. The screen reads only from here.
 */
@Database(
    entities = [SavedPlaceEntity::class, ForecastEntity::class, DailyForecastEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun savedPlaceDao(): SavedPlaceDao

    abstract fun forecastDao(): ForecastDao

    companion object {
        const val FILE_NAME = "weather.db"
    }
}
