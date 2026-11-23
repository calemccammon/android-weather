package com.calemccammon.androidweather.core.domain.usecase

import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.Units
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Watches the forecast the device holds for a place. */
class ObserveForecast
    @Inject
    constructor(
        private val repository: ForecastRepository,
    ) {
        /**
         * The forecast held for [place], in [units]; null while there is none.
         * A point off the globe has none, and the repository is not asked.
         */
        operator fun invoke(
            place: Place,
            units: Units,
        ): Flow<CachedForecast?> {
            if (!place.isOnGlobe) return flowOf(null)
            return repository
                .observe(place)
                .map { cached -> cached?.copy(forecast = cached.forecast.inUnits(units)) }
        }
    }
