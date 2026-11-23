package com.calemccammon.androidweather.core.network

import com.calemccammon.androidweather.core.model.WeatherCondition
import com.calemccammon.androidweather.core.network.model.weatherConditionFromWmo
import kotlin.test.Test
import kotlin.test.assertEquals

class WmoCodeTest {
    @Test
    fun `every code in Open-Meteo's table has a condition`() {
        val table =
            mapOf(
                WeatherCondition.Clear to listOf(0),
                WeatherCondition.MostlyClear to listOf(1),
                WeatherCondition.PartlyCloudy to listOf(2),
                WeatherCondition.Overcast to listOf(3),
                WeatherCondition.Fog to listOf(45, 48),
                WeatherCondition.Drizzle to listOf(51, 53, 55),
                WeatherCondition.FreezingDrizzle to listOf(56, 57),
                WeatherCondition.Rain to listOf(61, 63, 65),
                WeatherCondition.FreezingRain to listOf(66, 67),
                WeatherCondition.Snow to listOf(71, 73, 75, 77),
                WeatherCondition.RainShowers to listOf(80, 81, 82),
                WeatherCondition.SnowShowers to listOf(85, 86),
                WeatherCondition.Thunderstorm to listOf(95, 96, 99),
            )

        for ((condition, codes) in table) {
            for (code in codes) assertEquals(condition, weatherConditionFromWmo(code), "code $code")
        }
    }

    @Test
    fun `a code outside the table, or none, is unknown`() {
        assertEquals(WeatherCondition.Unknown, weatherConditionFromWmo(4))
        assertEquals(WeatherCondition.Unknown, weatherConditionFromWmo(100))
        assertEquals(WeatherCondition.Unknown, weatherConditionFromWmo(null))
    }
}
