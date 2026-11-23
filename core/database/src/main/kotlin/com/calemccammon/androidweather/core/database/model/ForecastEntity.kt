package com.calemccammon.androidweather.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The forecast held for one spot on the map: when it was fetched and the
 * conditions then. Its days are in [DailyForecastEntity]. Values are metric.
 */
@Entity(tableName = "forecasts")
data class ForecastEntity(
    @PrimaryKey @ColumnInfo(name = "place_key") val placeKey: String,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Instant,
    /** On the place's own clock. */
    @ColumnInfo(name = "observed_at") val observedAt: LocalDateTime,
    val temperature: Double,
    @ColumnInfo(name = "feels_like") val feelsLike: Double,
    @ColumnInfo(name = "wind_speed") val windSpeed: Double,
    /** The name of a `WeatherCondition`. */
    val condition: String,
    @ColumnInfo(name = "is_day") val isDay: Boolean,
)

/** One day of a forecast. Deleted with the forecast it belongs to. */
@Entity(
    tableName = "daily_forecasts",
    primaryKeys = ["place_key", "date"],
    foreignKeys = [
        ForeignKey(
            entity = ForecastEntity::class,
            parentColumns = ["place_key"],
            childColumns = ["place_key"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DailyForecastEntity(
    @ColumnInfo(name = "place_key") val placeKey: String,
    val date: LocalDate,
    val low: Double,
    val high: Double,
    /** The name of a `WeatherCondition`. */
    val condition: String,
    @ColumnInfo(name = "precipitation_chance") val precipitationChance: Int?,
)

/** A forecast with its days, read in one go. */
data class ForecastWithDays(
    @Embedded val forecast: ForecastEntity,
    @Relation(parentColumn = "place_key", entityColumn = "place_key")
    val days: List<DailyForecastEntity>,
)
