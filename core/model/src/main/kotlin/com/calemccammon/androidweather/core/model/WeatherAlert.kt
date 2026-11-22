package com.calemccammon.androidweather.core.model

import java.time.LocalDate

/** Severe weather forecast at a saved place, today or tomorrow there. */
data class WeatherAlert(
    val savedPlaceId: Long,
    val place: Place,
    val date: LocalDate,
    val condition: WeatherCondition,
    /** Whether [date] is today at the place; otherwise it is tomorrow. */
    val isToday: Boolean,
) {
    /** Tells this alert from every other: the same weather on the same day at the same place is one alert. */
    val id: String
        get() = "$savedPlaceId|$date|${condition.name}"
}
