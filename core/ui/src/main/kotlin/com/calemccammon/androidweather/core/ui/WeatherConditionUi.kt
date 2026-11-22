package com.calemccammon.androidweather.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.calemccammon.androidweather.core.model.WeatherCondition

/*
 * How each weather condition is worded and drawn.
 */

val WeatherCondition.label: UiText
    get() =
        uiText(
            when (this) {
                WeatherCondition.Clear -> R.string.core_ui_condition_clear
                WeatherCondition.MostlyClear -> R.string.core_ui_condition_mostly_clear
                WeatherCondition.PartlyCloudy -> R.string.core_ui_condition_partly_cloudy
                WeatherCondition.Overcast -> R.string.core_ui_condition_overcast
                WeatherCondition.Fog -> R.string.core_ui_condition_fog
                WeatherCondition.Drizzle -> R.string.core_ui_condition_drizzle
                WeatherCondition.FreezingDrizzle -> R.string.core_ui_condition_freezing_drizzle
                WeatherCondition.Rain -> R.string.core_ui_condition_rain
                WeatherCondition.FreezingRain -> R.string.core_ui_condition_freezing_rain
                WeatherCondition.Snow -> R.string.core_ui_condition_snow
                WeatherCondition.RainShowers -> R.string.core_ui_condition_rain_showers
                WeatherCondition.SnowShowers -> R.string.core_ui_condition_snow_showers
                WeatherCondition.Thunderstorm -> R.string.core_ui_condition_thunderstorm
                WeatherCondition.Unknown -> R.string.core_ui_condition_unknown
            },
        )

fun WeatherCondition.icon(isDay: Boolean = true): ImageVector =
    when (this) {
        WeatherCondition.Clear, WeatherCondition.MostlyClear ->
            if (isDay) Icons.Filled.WbSunny else Icons.Filled.NightlightRound
        WeatherCondition.PartlyCloudy -> if (isDay) Icons.Filled.WbCloudy else Icons.Filled.NightsStay
        WeatherCondition.Overcast -> Icons.Filled.Cloud
        WeatherCondition.Fog -> Icons.Filled.Dehaze
        WeatherCondition.Drizzle, WeatherCondition.FreezingDrizzle -> Icons.Filled.Grain
        WeatherCondition.Rain, WeatherCondition.FreezingRain -> Icons.Filled.WaterDrop
        WeatherCondition.RainShowers -> Icons.Filled.Umbrella
        WeatherCondition.Snow, WeatherCondition.SnowShowers -> Icons.Filled.AcUnit
        WeatherCondition.Thunderstorm -> Icons.Filled.Thunderstorm
        WeatherCondition.Unknown -> Icons.AutoMirrored.Filled.HelpOutline
    }
