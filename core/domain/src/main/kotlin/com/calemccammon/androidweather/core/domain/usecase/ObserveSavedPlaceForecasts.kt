package com.calemccammon.androidweather.core.domain.usecase

import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.SavedPlace
import com.calemccammon.androidweather.core.model.Units
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** A saved place and the forecast the device holds for it, if any. */
data class SavedPlaceForecast(
    val saved: SavedPlace,
    val cached: CachedForecast?,
)

/** Watches the saved places, each with the forecast held for it. */
class ObserveSavedPlaceForecasts
    @Inject
    constructor(
        private val places: SavedPlacesRepository,
        private val forecasts: ForecastRepository,
    ) {
        /** Every saved place, oldest first, with its forecast in [units]. */
        @OptIn(ExperimentalCoroutinesApi::class)
        operator fun invoke(units: Units): Flow<List<SavedPlaceForecast>> =
            places.places.flatMapLatest { saved ->
                if (saved.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        saved.map { place ->
                            forecasts.observe(place.place).map { cached ->
                                SavedPlaceForecast(place, cached?.copy(forecast = cached.forecast.inUnits(units)))
                            }
                        },
                    ) { it.toList() }
                }
            }
    }
