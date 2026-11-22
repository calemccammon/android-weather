package com.calemccammon.androidweather.feature.home.model

import com.calemccammon.androidweather.core.model.Units

/** The choices the units control offers. */
enum class UnitsUiModel {
    Celsius,
    Fahrenheit,
    ;

    fun toDomain(): Units =
        when (this) {
            Celsius -> Units.Metric
            Fahrenheit -> Units.Imperial
        }

    companion object {
        fun from(units: Units): UnitsUiModel =
            when (units) {
                Units.Metric -> Celsius
                Units.Imperial -> Fahrenheit
            }
    }
}
