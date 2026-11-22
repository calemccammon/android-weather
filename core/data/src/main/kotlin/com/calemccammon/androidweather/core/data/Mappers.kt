package com.calemccammon.androidweather.core.data

import com.calemccammon.androidweather.core.database.model.DailyForecastEntity
import com.calemccammon.androidweather.core.database.model.ForecastEntity
import com.calemccammon.androidweather.core.database.model.ForecastWithDays
import com.calemccammon.androidweather.core.database.model.SavedPlaceEntity
import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.CurrentConditions
import com.calemccammon.androidweather.core.model.DailyForecast
import com.calemccammon.androidweather.core.model.Forecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.SavedPlace
import com.calemccammon.androidweather.core.model.WeatherCondition
import java.time.Instant

/*
 * Between the app's models and the database's rows.
 */

internal fun Place.toEntity() =
    SavedPlaceEntity(
        placeKey = key,
        latitude = latitude,
        longitude = longitude,
        name = name,
        region = region,
        country = country,
    )

internal fun SavedPlaceEntity.toModel() = SavedPlace(id, Place(latitude, longitude, name, region, country))

internal fun Forecast.toEntity(
    placeKey: String,
    fetchedAt: Instant,
) = ForecastEntity(
    placeKey = placeKey,
    fetchedAt = fetchedAt,
    observedAt = current.observedAt,
    temperature = current.temperature,
    feelsLike = current.feelsLike,
    windSpeed = current.windSpeed,
    condition = current.condition.name,
    isDay = current.isDay,
)

internal fun Forecast.toDayEntities(placeKey: String) =
    days.map {
        DailyForecastEntity(
            placeKey = placeKey,
            date = it.date,
            low = it.low,
            high = it.high,
            condition = it.condition.name,
            precipitationChance = it.precipitationChance,
        )
    }

internal fun ForecastWithDays.toModel() =
    CachedForecast(
        forecast =
            Forecast(
                current =
                    CurrentConditions(
                        observedAt = forecast.observedAt,
                        temperature = forecast.temperature,
                        feelsLike = forecast.feelsLike,
                        windSpeed = forecast.windSpeed,
                        condition = condition(forecast.condition),
                        isDay = forecast.isDay,
                    ),
                days =
                    days.sortedBy { it.date }.map {
                        DailyForecast(
                            date = it.date,
                            low = it.low,
                            high = it.high,
                            condition = condition(it.condition),
                            precipitationChance = it.precipitationChance,
                        )
                    },
            ),
        fetchedAt = forecast.fetchedAt,
    )

/** A name this version does not know, written by a later one, reads as unknown weather. */
private fun condition(name: String): WeatherCondition =
    WeatherCondition.entries.firstOrNull { it.name == name } ?: WeatherCondition.Unknown
