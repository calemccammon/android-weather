package com.calemccammon.androidweather.core.network

import com.calemccammon.androidweather.core.model.Forecast
import com.calemccammon.androidweather.core.model.Place

/**
 * Places and forecasts from the weather service.
 *
 * Both calls fail with a
 * [com.calemccammon.androidweather.core.model.WeatherException] and nothing else.
 */
interface WeatherNetworkDataSource {
    /** Places whose name matches [query], best match first. */
    suspend fun searchPlaces(query: String): List<Place>

    /** Current conditions and the days ahead at a point, in metric units. */
    suspend fun forecast(
        latitude: Double,
        longitude: Double,
    ): Forecast
}
