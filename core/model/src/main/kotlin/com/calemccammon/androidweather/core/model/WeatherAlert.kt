package com.calemccammon.androidweather.core.model

import java.time.LocalDate

/** Severe weather forecast at a saved place, today or tomorrow there. */
data class WeatherAlert(
    val place: Place,
    val date: LocalDate,
    val condition: WeatherCondition,
    /** Whether [date] is today at the place; otherwise it is tomorrow. */
    val isToday: Boolean,
) {
    /**
     * Tells this alert from every other: the same weather on the same day at
     * the same spot is one alert. It goes by where the place is, so a place
     * removed and saved again is not told of the same storm twice.
     */
    val id: String
        get() = "${place.latitude},${place.longitude}|$date|${condition.name}"
}
