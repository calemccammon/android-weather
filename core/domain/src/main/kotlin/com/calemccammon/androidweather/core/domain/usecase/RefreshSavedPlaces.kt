package com.calemccammon.androidweather.core.domain.usecase

import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.model.WeatherException
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/**
 * Fetches the forecast of every saved place, and of the selected place, which
 * the home screen and the widget show. This is what the background refresh runs.
 */
class RefreshSavedPlaces
    @Inject
    constructor(
        private val places: SavedPlacesRepository,
        private val forecasts: ForecastRepository,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        /**
         * One place failing does not stop the rest; the outcome says whether
         * running again later is worth it. Forecasts of places looked at in
         * passing are forgotten after [KEEP_UNSAVED_FOR].
         */
        suspend operator fun invoke(): Outcome {
            var worthRetrying = false
            val kept = places.places.first().map { it.place } + listOfNotNull(settings.settings.first().selectedPlace)
            // The selected place is often a saved one too.
            for (place in kept.distinctBy { it.latitude to it.longitude }) {
                try {
                    forecasts.refresh(place)
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
