package com.calemccammon.androidweather.core.ui

import com.calemccammon.androidweather.core.model.Units

/*
 * How a temperature or a speed is worded in each system of units.
 */

/** "12°C" or "54°F". */
fun Units.temperature(degrees: Int): UiText =
    uiText(
        when (this) {
            Units.Metric -> R.string.core_ui_temperature_celsius
            Units.Imperial -> R.string.core_ui_temperature_fahrenheit
        },
        degrees,
    )

/** "12 degrees Celsius", as a screen reader says it. */
fun Units.spokenTemperature(degrees: Int): UiText =
    UiText.Plural(
        when (this) {
            Units.Metric -> R.plurals.core_ui_spoken_celsius
            Units.Imperial -> R.plurals.core_ui_spoken_fahrenheit
        },
        degrees,
    )

/** "20 km/h" or "12 mph". */
fun Units.speed(value: Int): UiText =
    uiText(
        when (this) {
            Units.Metric -> R.string.core_ui_speed_kmh
            Units.Imperial -> R.string.core_ui_speed_mph
        },
        value,
    )
