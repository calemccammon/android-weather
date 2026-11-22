package com.calemccammon.androidweather.core.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

/** How dates and times are written to a column: a moment as milliseconds, the rest as ISO 8601 text. */
internal class Converters {
    @TypeConverter
    fun instantToMillis(instant: Instant): Long = instant.toEpochMilli()

    @TypeConverter
    fun millisToInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)

    @TypeConverter
    fun dateToText(date: LocalDate): String = date.toString()

    @TypeConverter
    fun textToDate(text: String): LocalDate = LocalDate.parse(text)

    @TypeConverter
    fun dateTimeToText(dateTime: LocalDateTime): String = dateTime.toString()

    @TypeConverter
    fun textToDateTime(text: String): LocalDateTime = LocalDateTime.parse(text)
}
