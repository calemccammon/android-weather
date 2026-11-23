package com.calemccammon.androidweather.core.data.di

import com.calemccammon.androidweather.core.data.repository.AndroidDeviceRepository
import com.calemccammon.androidweather.core.data.repository.DataStoreSettingsRepository
import com.calemccammon.androidweather.core.data.repository.FusedLocationRepository
import com.calemccammon.androidweather.core.data.repository.NetworkPlaceSearchRepository
import com.calemccammon.androidweather.core.data.repository.OfflineFirstForecastRepository
import com.calemccammon.androidweather.core.data.repository.RoomSavedPlacesRepository
import com.calemccammon.androidweather.core.domain.repository.DeviceRepository
import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.domain.repository.LocationRepository
import com.calemccammon.androidweather.core.domain.repository.PlaceSearchRepository
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * The one place that knows which implementation stands behind each of the
 * domain's interfaces. Everything else asks for the interface.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    @Singleton
    abstract fun forecasts(repository: OfflineFirstForecastRepository): ForecastRepository

    @Binds
    @Singleton
    abstract fun savedPlaces(repository: RoomSavedPlacesRepository): SavedPlacesRepository

    @Binds
    @Singleton
    abstract fun settings(repository: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun placeSearch(repository: NetworkPlaceSearchRepository): PlaceSearchRepository

    @Binds
    abstract fun location(repository: FusedLocationRepository): LocationRepository

    @Binds
    abstract fun device(repository: AndroidDeviceRepository): DeviceRepository

    companion object {
        @Provides
        fun clock(): Clock = Clock.systemUTC()
    }
}
