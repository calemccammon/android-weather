package com.calemccammon.androidweather.core.domain.usecase

import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.model.WeatherException
import com.calemccammon.androidweather.core.model.WeatherFailure
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
         * One place failing does not stop the rest; the outcome says what
         * failed. Forecasts of places looked at in passing are forgotten after
         * [KEEP_UNSAVED_FOR], but never that of the selected place, however
         * old: with no connection it is all there is to show.
         */
        suspend operator fun invoke(): Outcome {
            val selected = settings.settings.first().selectedPlace
            val kept = places.places.first().map { it.place } + listOfNotNull(selected)
            val failures = mutableListOf<WeatherFailure>()
            // The selected place is often a saved one too.
            for (place in kept.distinctBy { it.latitude to it.longitude }) {
                try {
                    forecasts.refresh(place)
                } catch (e: WeatherException) {
                    failures += e.failure
                }
            }
            forecasts.prune(before = clock.instant() - KEEP_UNSAVED_FOR, keep = listOfNotNull(selected))
            return Outcome(failures)
        }

        /** How a refresh went: the failure of each place that could not be fetched. */
        data class Outcome(
            val failures: List<WeatherFailure> = emptyList(),
        ) {
            /** Whether at least one place failed in a way that may pass, such as no connection. */
            val isWorthRetrying: Boolean
                get() = failures.any { it.isTemporary }
        }

        companion object {
            /** How long the forecast of a place that is not saved is kept. */
            val KEEP_UNSAVED_FOR: Duration = Duration.ofDays(1)
        }
    }
