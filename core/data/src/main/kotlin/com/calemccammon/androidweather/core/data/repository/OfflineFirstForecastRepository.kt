package com.calemccammon.androidweather.core.data.repository

import com.calemccammon.androidweather.core.data.key
import com.calemccammon.androidweather.core.data.toDayEntities
import com.calemccammon.androidweather.core.data.toEntity
import com.calemccammon.androidweather.core.data.toModel
import com.calemccammon.androidweather.core.database.dao.ForecastDao
import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.network.WeatherNetworkDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Forecasts with the database as the one source of truth.
 *
 * Nothing fetched is handed to a caller: [refresh] writes it to the database,
 * and [observe], which only reads the database, sees the change. So the
 * screen, the background refresh and anything else that asks all agree, and
 * the last forecast fetched is there with no connection.
 */
internal class OfflineFirstForecastRepository
    @Inject
    constructor(
        private val dao: ForecastDao,
        private val network: WeatherNetworkDataSource,
        private val clock: Clock,
    ) : ForecastRepository {
        override fun observe(place: Place): Flow<CachedForecast?> =
            dao
                .observe(place.key)
                // Room emits again when any forecast changes, not only this one.
                .distinctUntilChanged()
                .map { it?.toModel() }

        override suspend fun refresh(place: Place) {
            val forecast = network.forecast(place.latitude, Place.normalizeLongitude(place.longitude))
            dao.replace(forecast.toEntity(place.key, fetchedAt = clock.instant()), forecast.toDayEntities(place.key))
        }

        override suspend fun prune(
            before: Instant,
            keep: Collection<Place>,
        ) {
            dao.deleteUnsavedFetchedBefore(before, keep.map { it.key })
        }
    }
