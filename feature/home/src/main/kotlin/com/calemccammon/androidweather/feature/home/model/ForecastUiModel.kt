package com.calemccammon.androidweather.feature.home.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.calemccammon.androidweather.core.model.CurrentConditions
import com.calemccammon.androidweather.core.model.DailyForecast
import com.calemccammon.androidweather.core.model.Forecast
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.ui.UiText
import com.calemccammon.androidweather.core.ui.icon
import com.calemccammon.androidweather.core.ui.label
import com.calemccammon.androidweather.core.ui.speed
import com.calemccammon.androidweather.core.ui.spokenTemperature
import com.calemccammon.androidweather.core.ui.temperature
import com.calemccammon.androidweather.core.ui.uiText
import com.calemccammon.androidweather.feature.home.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.time.DayOfWeek
import kotlin.math.roundToInt
import com.calemccammon.androidweather.core.ui.R as CoreR

/**
 * The forecast as the screen shows it: every value already rounded, worded
 * and laid out, so the composables only place it.
 */
@Immutable
data class ForecastUiModel(
    val current: CurrentUiModel,
    val days: ImmutableList<DayUiModel>,
) {
    companion object {
        /** Translates a domain [Forecast] for display in [units]. */
        fun from(
            forecast: Forecast,
            units: Units,
        ): ForecastUiModel {
            // The whole week's range, so each day's bar shows where it sits in it.
            val weekLow = forecast.days.minOfOrNull { it.low } ?: 0.0
            val weekHigh = forecast.days.maxOfOrNull { it.high } ?: 0.0

            return ForecastUiModel(
                current = CurrentUiModel.from(forecast.current, units),
                days =
                    forecast.days
                        .map { DayUiModel.from(it, forecast.isToday(it), weekLow, weekHigh) }
                        .toImmutableList(),
            )
        }
    }
}

/** The "right now" block at the top of the forecast. */
@Immutable
data class CurrentUiModel(
    val icon: ImageVector,
    /** "34°F". The one place the scale is spelled out; the rest of the forecast is in the same one. */
    val temperature: UiText,
    /** "Overcast". */
    val condition: UiText,
    /** "Feels like 28°". */
    val feelsLike: UiText,
    /** "Wind 2 mph". */
    val wind: UiText,
    /** The whole block as one sentence for a screen reader. */
    val description: UiText,
) {
    companion object {
        fun from(
            current: CurrentConditions,
            units: Units,
        ): CurrentUiModel {
            val temperature = current.temperature.roundToInt()
            val feelsLike = current.feelsLike.roundToInt()
            val wind = current.windSpeed.roundToInt()
            val speed = units.speed(wind)
            val condition = current.condition.label

            return CurrentUiModel(
                icon = current.condition.icon(isDay = current.isDay),
                temperature = units.temperature(temperature),
                condition = condition,
                feelsLike = uiText(R.string.feature_home_feels_like, feelsLike),
                wind = uiText(R.string.feature_home_wind, speed),
                description =
                    uiText(
                        R.string.feature_home_current_description,
                        condition,
                        units.spokenTemperature(temperature),
                        feelsLike,
                        speed,
                    ),
            )
        }
    }
}

/** One row of the week. */
@Immutable
data class DayUiModel(
    /** "Today" for the day the forecast was made on, otherwise a short weekday: "Sat". */
    val dayLabel: UiText,
    val icon: ImageVector,
    /** "47%", or null when there is no chance worth showing. */
    val chance: UiText?,
    /** "30°". */
    val low: UiText,
    /** "68°". */
    val high: UiText,
    /**
     * Where this day's low and high fall within the week's range, each from 0
     * (the week's lowest) to 1 (its highest).
     */
    val barStart: Float,
    val barEnd: Float,
    /** The whole row as one sentence for a screen reader. */
    val description: UiText,
) {
    companion object {
        fun from(
            day: DailyForecast,
            isToday: Boolean,
            weekLow: Double,
            weekHigh: Double,
        ): DayUiModel {
            val low = day.low.roundToInt()
            val high = day.high.roundToInt()
            val chance = day.precipitationChance ?: 0
            val span = weekHigh - weekLow
            val today = uiText(R.string.feature_home_today)
            val dayName = if (isToday) today else uiText(day.date.dayOfWeek.fullName)
            val condition = day.condition.label

            return DayUiModel(
                dayLabel = if (isToday) today else uiText(day.date.dayOfWeek.shortName),
                icon = day.condition.icon(),
                chance = if (chance > 0) uiText(R.string.feature_home_precipitation_chance, chance) else null,
                low = uiText(CoreR.string.core_ui_temperature, low),
                high = uiText(CoreR.string.core_ui_temperature, high),
                // A week with one temperature throughout fills the whole bar.
                barStart = if (span <= 0) 0f else ((day.low - weekLow) / span).toFloat(),
                barEnd = if (span <= 0) 1f else ((day.high - weekLow) / span).toFloat(),
                description =
                    if (chance > 0) {
                        uiText(R.string.feature_home_day_description_with_chance, dayName, condition, low, high, chance)
                    } else {
                        uiText(R.string.feature_home_day_description, dayName, condition, low, high)
                    },
            )
        }

        private val DayOfWeek.shortName
            get() =
                when (this) {
                    DayOfWeek.MONDAY -> R.string.feature_home_day_short_monday
                    DayOfWeek.TUESDAY -> R.string.feature_home_day_short_tuesday
                    DayOfWeek.WEDNESDAY -> R.string.feature_home_day_short_wednesday
                    DayOfWeek.THURSDAY -> R.string.feature_home_day_short_thursday
                    DayOfWeek.FRIDAY -> R.string.feature_home_day_short_friday
                    DayOfWeek.SATURDAY -> R.string.feature_home_day_short_saturday
                    DayOfWeek.SUNDAY -> R.string.feature_home_day_short_sunday
                }

        private val DayOfWeek.fullName
            get() =
                when (this) {
                    DayOfWeek.MONDAY -> R.string.feature_home_day_monday
                    DayOfWeek.TUESDAY -> R.string.feature_home_day_tuesday
                    DayOfWeek.WEDNESDAY -> R.string.feature_home_day_wednesday
                    DayOfWeek.THURSDAY -> R.string.feature_home_day_thursday
                    DayOfWeek.FRIDAY -> R.string.feature_home_day_friday
                    DayOfWeek.SATURDAY -> R.string.feature_home_day_saturday
                    DayOfWeek.SUNDAY -> R.string.feature_home_day_sunday
                }
    }
}
