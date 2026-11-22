package com.calemccammon.androidweather.core.testing

import com.calemccammon.androidweather.core.model.CurrentConditions
import com.calemccammon.androidweather.core.model.DailyForecast
import com.calemccammon.androidweather.core.model.Forecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.WeatherCondition
import java.time.LocalDate
import java.time.LocalDateTime

/** Places and forecasts for tests to use where the particulars do not matter. */
object Samples {
    val asheville =
        Place(
            latitude = 35.6,
            longitude = -82.55,
            name = "Asheville",
            region = "North Carolina",
            country = "United States",
        )

    val lisbon = Place(latitude = 38.72, longitude = -9.13, name = "Lisbon", region = "Lisbon", country = "Portugal")

    /** A point picked off the map: no name. */
    val point = Place(latitude = -33.86, longitude = 151.21)

    /** A metric forecast of [days] days, starting on a Wednesday at [temperature] degrees. */
    fun forecast(
        temperature: Double = 10.0,
        days: Int = 3,
    ) = Forecast(
        current =
            CurrentConditions(
                observedAt = LocalDateTime.parse("2026-11-18T09:00"),
                temperature = temperature,
                feelsLike = temperature - 2,
                windSpeed = 20.0,
                condition = WeatherCondition.Overcast,
                isDay = true,
            ),
        days =
            List(days) { i ->
                DailyForecast(
                    date = LocalDate.parse("2026-11-18").plusDays(i.toLong()),
                    low = temperature - 5 + i,
                    high = temperature + 5 + i,
                    condition = if (i == 0) WeatherCondition.Overcast else WeatherCondition.Rain,
                    precipitationChance = if (i == 0) null else 40 + i,
                )
            },
    )
}
