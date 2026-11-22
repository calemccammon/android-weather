package com.calemccammon.androidweather.core.domain.usecase

import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.model.WeatherException
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/**
 * Fetches the forecast of every saved place. This is what the background
 * refresh runs.
 */
class RefreshSavedPlaces
    @Inject
    constructor(
        private val places: SavedPlacesRepository,
        private val forecasts: ForecastRepository,
        private val clock: Clock,
    ) {
        /**
         * Forecasts of places looked at in passing are forgotten after
         * [KEEP_UNSAVED_FOR]. One place failing does not stop the rest. The outcome says whether
         * running again later is worth it.
         */
        suspend operator fun invoke(): Outcome {
            var worthRetrying = false
            for (saved in places.places.first()) {
                try {
                    forecasts.refresh(saved.place)
                } catch (e: WeatherException) {
                    if (e.failure.isTemporary) worthRetrying = true
                }
            }
            forecasts.prune(before = clock.instant() - KEEP_UNSAVED_FOR)
            return if (worthRetrying) Outcome.RetryLater else Outcome.Done
        }

        enum class Outcome {
            /** Every place was refreshed, or failed in a way that waiting will not mend. */
            Done,

            /** At least one place failed in a way that may pass, such as no connection. */
            RetryLater,
        }

        companion object {
            /** How long the forecast of a place that is not saved is kept. */
            val KEEP_UNSAVED_FOR: Duration = Duration.ofDays(1)
        }
    }
