package com.calemccammon.androidweather.core.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** The weather right now at a place. */
data class CurrentConditions(
    /**
     * When these conditions were measured, on the place's own clock. It says
     * which calendar day is "today" there, whatever the device's clock says.
     */
    val observedAt: LocalDateTime,
    val temperature: Double,
    val feelsLike: Double,
    val windSpeed: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
)

/** One day of the weekly forecast. */
data class DailyForecast(
    /** The calendar day at the place. */
    val date: LocalDate,
    val low: Double,
    val high: Double,
    val condition: WeatherCondition,
    /** Highest chance of precipitation during the day, in percent. */
    val precipitationChance: Int? = null,
)

/**
 * Current conditions and the days ahead for one place.
 *
 * A forecast is fetched and kept in metric units: degrees Celsius and
 * kilometres an hour. [inUnits] gives the same forecast in another system, so
 * changing units never needs the network.
 */
data class Forecast(
    val current: CurrentConditions,
    val days: List<DailyForecast>,
) {
    /** This metric forecast with its temperatures and wind speed in [units]. */
    fun inUnits(units: Units): Forecast =
        when (units) {
            Units.Metric -> this
            Units.Imperial ->
                Forecast(
                    current =
                        current.copy(
                            temperature = fahrenheit(current.temperature),
                            feelsLike = fahrenheit(current.feelsLike),
                            windSpeed = current.windSpeed * MILES_PER_KILOMETRE,
                        ),
                    days = days.map { it.copy(low = fahrenheit(it.low), high = fahrenheit(it.high)) },
                )
        }

    private companion object {
        const val MILES_PER_KILOMETRE = 0.621371
        const val FAHRENHEIT_PER_CELSIUS = 1.8
        const val FREEZING_FAHRENHEIT = 32

        fun fahrenheit(celsius: Double): Double = celsius * FAHRENHEIT_PER_CELSIUS + FREEZING_FAHRENHEIT
    }
}

/** A forecast kept on the device, and when it was fetched. */
data class CachedForecast(
    val forecast: Forecast,
    val fetchedAt: Instant,
) {
    /**
     * The calendar day at the place at [now]: the day its conditions were
     * measured on, moved on by the time since the forecast was fetched. So a
     * forecast kept overnight calls the next day today, as the place does.
     */
    fun todayAt(now: Instant): LocalDate {
        val age = Duration.between(fetchedAt, now).coerceAtLeast(Duration.ZERO)
        return forecast.current.observedAt
            .plus(age)
            .toLocalDate()
    }
}
