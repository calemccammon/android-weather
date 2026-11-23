package com.calemccammon.androidweather.core.domain.usecase

import com.calemccammon.androidweather.core.domain.repository.AlertNotifier
import com.calemccammon.androidweather.core.domain.repository.ForecastRepository
import com.calemccammon.androidweather.core.domain.repository.SavedPlacesRepository
import com.calemccammon.androidweather.core.domain.repository.SettingsRepository
import com.calemccammon.androidweather.core.model.SavedPlace
import com.calemccammon.androidweather.core.model.WeatherAlert
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Looks through the forecasts held for the saved places for severe weather
 * the user has not been told of yet, and tells them.
 */
class TellNewWeatherAlerts
    @Inject
    constructor(
        private val places: SavedPlacesRepository,
        private val forecasts: ForecastRepository,
        private val settings: SettingsRepository,
        private val notifier: AlertNotifier,
    ) {
        /**
         * Tells of severe weather today or tomorrow at each saved place, less
         * what has been told already. Does nothing when the user has not asked
         * for alerts. Returns what was told.
         *
         * An alert is recorded as told only once it has been shown. If the
         * system would not show it, it is still new the next time.
         */
        suspend operator fun invoke(): List<WeatherAlert> {
            val current = settings.settings.first()
            if (!current.alertsEnabled) return emptyList()

            val standing = places.places.first().flatMap { alertsAt(it) }
            val new = standing.filterNot { it.id in current.notifiedAlerts }
            val shown = new.isEmpty() || notifier.show(new)

            // Only alerts still in the forecast are remembered, so the record
            // does not grow: yesterday's drop out by themselves.
            val told = standing.map { it.id }.filter { shown || it in current.notifiedAlerts }
            settings.setNotifiedAlerts(told.toSet())
            return if (shown) new else emptyList()
        }

        private suspend fun alertsAt(saved: SavedPlace): List<WeatherAlert> {
            val forecast = forecasts.observe(saved.place).first()?.forecast ?: return emptyList()
            // Today is the day at the place, not on the device.
            val today = forecast.current.observedAt.toLocalDate()
            return forecast.days
                .filter { it.condition.isSevere && (it.date == today || it.date == today.plusDays(1)) }
                .map { WeatherAlert(saved.place, it.date, it.condition, isToday = it.date == today) }
        }
    }
