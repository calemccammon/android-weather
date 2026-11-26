package com.calemccammon.androidweather.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ModelsTest {
    @Test
    fun `a longitude from a map dragged round the world is brought back into range`() {
        assertEquals(-170.0, Place.normalizeLongitude(190.0))
        assertEquals(170.0, Place.normalizeLongitude(-190.0))
        assertEquals(10.0, Place.normalizeLongitude(370.0))
        assertEquals(-180.0, Place.normalizeLongitude(180.0))
        assertEquals(-82.55, Place.normalizeLongitude(-82.55))
    }

    @Test
    fun `a point picked off the map has no name and a longitude in range`() {
        assertEquals(Place(latitude = 10.0, longitude = -170.0), Place.fromCoordinates(10.0, 190.0))
    }

    @Test
    fun `a point is on the globe unless its latitude is out of range or a coordinate is no number`() {
        assertTrue(Place(90.0, 180.0).isOnGlobe)
        assertTrue(Place(-90.0, 720.0).isOnGlobe)
        assertFalse(Place(90.1, 0.0).isOnGlobe)
        assertFalse(Place(Double.NaN, 0.0).isOnGlobe)
        assertFalse(Place(0.0, Double.NaN).isOnGlobe)
        assertFalse(Place(0.0, Double.POSITIVE_INFINITY).isOnGlobe)
    }

    @Test
    fun `only three countries default to imperial units`() {
        assertEquals(Units.Imperial, Units.forCountry("US"))
        assertEquals(Units.Imperial, Units.forCountry("LR"))
        assertEquals(Units.Imperial, Units.forCountry("MM"))
        assertEquals(Units.Metric, Units.forCountry("GB"))
        assertEquals(Units.Metric, Units.forCountry(null))
    }

    private val forecast =
        Forecast(
            current =
                CurrentConditions(
                    observedAt = LocalDateTime.parse("2026-11-18T23:30"),
                    temperature = 10.0,
                    feelsLike = 0.0,
                    windSpeed = 100.0,
                    condition = WeatherCondition.Rain,
                    isDay = false,
                ),
            days =
                listOf(
                    DailyForecast(LocalDate.parse("2026-11-18"), low = -40.0, high = 20.0, WeatherCondition.Rain, 80),
                    DailyForecast(LocalDate.parse("2026-11-19"), low = 5.0, high = 15.0, WeatherCondition.Clear),
                ),
        )

    @Test
    fun `a forecast in metric units is the forecast as fetched`() {
        assertSame(forecast, forecast.inUnits(Units.Metric))
    }

    @Test
    fun `a forecast in imperial units has its temperatures and wind converted and nothing else`() {
        val imperial = forecast.inUnits(Units.Imperial)

        assertEquals(50.0, imperial.current.temperature)
        assertEquals(32.0, imperial.current.feelsLike)
        assertEquals(62.1371, imperial.current.windSpeed, absoluteTolerance = 0.0001)
        assertEquals(listOf(-40.0 to 68.0, 41.0 to 59.0), imperial.days.map { it.low to it.high })
        assertEquals(
            forecast.current.copy(temperature = 0.0, feelsLike = 0.0, windSpeed = 0.0),
            imperial.current.copy(temperature = 0.0, feelsLike = 0.0, windSpeed = 0.0),
        )
        assertEquals(forecast.days.map { it.precipitationChance }, imperial.days.map { it.precipitationChance })
        assertEquals(forecast.days.map { it.date }, imperial.days.map { it.date })
    }

    @Test
    fun `failures that may pass are told from those that will not`() {
        val temporary = WeatherFailure.entries.filter { it.isTemporary }

        assertEquals(listOf(WeatherFailure.Timeout, WeatherFailure.NoConnection, WeatherFailure.Unavailable), temporary)
    }

    @Test
    fun `a weather exception's message carries its kind and any detail`() {
        assertEquals("Timeout", WeatherException(WeatherFailure.Timeout).message)
        assertEquals("Rejected: bad latitude", WeatherException(WeatherFailure.Rejected, "bad latitude").message)
    }

    @Test
    fun `today at a place moves on as the forecast held for it ages`() {
        // Fetched at noon UTC, when it was 21:00 at the place.
        val fetchedAt = Instant.parse("2026-11-18T12:00:00Z")
        val current =
            CurrentConditions(
                observedAt = LocalDateTime.parse("2026-11-18T21:00"),
                temperature = 10.0,
                feelsLike = 8.0,
                windSpeed = 5.0,
                condition = WeatherCondition.Clear,
                isDay = false,
            )
        val held = CachedForecast(Forecast(current, days = emptyList()), fetchedAt)

        assertEquals(LocalDate.parse("2026-11-18"), held.todayAt(fetchedAt))
        assertEquals(LocalDate.parse("2026-11-18"), held.todayAt(fetchedAt.plusSeconds(2 * 3600)))
        // Past midnight at the place: the next day, whatever the date is in UTC.
        assertEquals(LocalDate.parse("2026-11-19"), held.todayAt(fetchedAt.plusSeconds(4 * 3600)))
        assertEquals(LocalDate.parse("2026-11-20"), held.todayAt(fetchedAt.plusSeconds(28 * 3600)))
        // A clock set back is no reason to go back a day.
        assertEquals(LocalDate.parse("2026-11-18"), held.todayAt(fetchedAt.minusSeconds(48 * 3600)))
    }
}
