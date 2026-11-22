package com.calemccammon.androidweather.core.domain.usecase

import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.WeatherException
import com.calemccammon.androidweather.core.model.WeatherFailure
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/** Brings the forecast the device holds for a place up to date. */
class RefreshForecast
    @Inject
    constructor(
        private val repository: ForecastRepository,
        private val clock: Clock,
    ) {
        /**
         * Fetches the forecast for [place], unless the one held is newer than
         * [FRESH_FOR] and [force] is false: opening a place twice in a few
         * minutes asks the service once.
         *
         * A point off the globe fails with [WeatherFailure.InvalidPlace]
         * without asking the repository.
         */
        suspend operator fun invoke(
            place: Place,
            force: Boolean = false,
        ) {
            if (!place.isOnGlobe) throw WeatherException(WeatherFailure.InvalidPlace)

            if (!force) {
                val held = repository.observe(place).first()
                val age = held?.let { Duration.between(it.fetchedAt, clock.instant()) }
                // A forecast dated in the future means the clock was moved; fetch.
                if (age != null && !age.isNegative && age < FRESH_FOR) return
            }
            repository.refresh(place)
        }

        companion object {
            /** How long a fetched forecast is shown without asking again. */
            val FRESH_FOR: Duration = Duration.ofMinutes(30)
        }
    }
