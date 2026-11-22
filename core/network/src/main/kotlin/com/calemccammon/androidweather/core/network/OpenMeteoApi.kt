package com.calemccammon.androidweather.core.network

import com.calemccammon.androidweather.core.network.model.ForecastDto
import com.calemccammon.androidweather.core.network.model.PlaceSearchDto
import retrofit2.http.GET
import retrofit2.http.Query

/** https://open-meteo.com/en/docs/geocoding-api */
internal interface GeocodingApi {
    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): PlaceSearchDto
}

/** https://open-meteo.com/en/docs */
internal interface ForecastApi {
    // No units are asked for: the service's defaults are the metric ones.
    @Suppress("LongParameterList") // One for each thing the service is told.
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("forecast_days") days: Int,
        @Query("current") current: String = "temperature_2m,apparent_temperature,weather_code,wind_speed_10m,is_day",
        @Query("daily") daily: String =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max",
        @Query("timezone") timezone: String = "auto",
    ): ForecastDto
}
