package com.calemccammon.androidweather.feature.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.calemccammon.androidweather.core.model.DailyForecast
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.model.WeatherCondition
import com.calemccammon.androidweather.core.testing.Samples
import com.calemccammon.androidweather.core.ui.UiText
import com.calemccammon.androidweather.core.ui.asString
import com.calemccammon.androidweather.core.ui.icon
import com.calemccammon.androidweather.feature.home.model.ForecastUiModel
import com.calemccammon.androidweather.feature.home.model.UnitsUiModel
import com.calemccammon.androidweather.feature.home.model.updatedText
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class UiModelsTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val UiText.words get() = asString(resources)

    // The sample forecast at 0.4 degrees: feels like -1.6, wind 20; Wednesday -5 to 5
    // and overcast, Thursday -4 to 6 with rain at 41%, Friday -3 to 7 with rain at 42%.
    private val metric = ForecastUiModel.from(Samples.forecast(temperature = 0.4), Units.Metric)

    @Test
    fun `current conditions are rounded and worded in metric units`() {
        val current = metric.current

        assertEquals("0°C", current.temperature.words)
        assertEquals("Overcast", current.condition.words)
        assertEquals("Feels like -2°", current.feelsLike.words)
        assertEquals("Wind 20 km/h", current.wind.words)
        assertEquals(WeatherCondition.Overcast.icon(isDay = true), current.icon)
        assertEquals("Now: Overcast, 0 degrees Celsius, feels like -2, wind 20 km/h", current.description.words)
    }

    @Test
    fun `current conditions are worded in imperial units`() {
        val forecast = Samples.forecast(temperature = 10.0).inUnits(Units.Imperial)

        val current = ForecastUiModel.from(forecast, Units.Imperial).current

        assertEquals("50°F", current.temperature.words)
        assertEquals("Wind 12 mph", current.wind.words)
        assertEquals("Now: Overcast, 50 degrees Fahrenheit, feels like 46, wind 12 mph", current.description.words)
    }

    @Test
    fun `the day the forecast was made on is today and the rest are named`() {
        assertEquals(listOf("Today", "Thu", "Fri"), metric.days.map { it.dayLabel.words })
    }

    @Test
    fun `a day shows its rounded low and high and its chance of precipitation`() {
        val (today, thursday) = metric.days

        assertEquals(listOf("-5°", "5°"), listOf(today.low.words, today.high.words))
        assertNull(today.chance)
        assertEquals("41%", thursday.chance?.words)
        assertEquals(WeatherCondition.Rain.icon(), thursday.icon)
    }

    @Test
    fun `a day is one sentence for a screen reader`() {
        val (today, thursday) = metric.days

        assertEquals("Today: Overcast, low -5°, high 5°", today.description.words)
        assertEquals(
            "Thursday: Rain, low -4°, high 6°, 41% chance of precipitation",
            thursday.description.words,
        )
    }

    @Test
    fun `a day's bar shows where its range sits in the week's`() {
        // The week runs from -4.6 to 7.4, twelve degrees.
        val (today, _, friday) = metric.days

        assertEquals(0f, today.barStart)
        assertEquals(10f / 12f, today.barEnd, absoluteTolerance = 0.0001f)
        assertEquals(2f / 12f, friday.barStart, absoluteTolerance = 0.0001f)
        assertEquals(1f, friday.barEnd)
    }

    @Test
    fun `a week of one temperature fills every bar`() {
        val flat = DailyForecast(LocalDate.parse("2026-11-18"), low = 5.0, high = 5.0, WeatherCondition.Fog)

        val day = ForecastUiModel.from(Samples.forecast().copy(days = listOf(flat)), Units.Metric).days.single()

        assertEquals(0f to 1f, day.barStart to day.barEnd)
    }

    @Test
    fun `the units choices match the units`() {
        assertEquals(UnitsUiModel.Celsius, UnitsUiModel.from(Units.Metric))
        assertEquals(UnitsUiModel.Fahrenheit, UnitsUiModel.from(Units.Imperial))
        assertEquals(Units.entries, UnitsUiModel.entries.map { it.toDomain() })
    }

    @Test
    fun `a forecast's age is told in the largest unit that fits`() {
        val fetched = Instant.parse("2026-11-18T12:00:00Z")

        fun after(age: Duration) = updatedText(fetched, fetched + age).words

        assertEquals("Updated just now", after(Duration.ofSeconds(59)))
        assertEquals("Updated 1 minute ago", after(Duration.ofSeconds(60)))
        assertEquals("Updated 59 minutes ago", after(Duration.ofMinutes(59)))
        assertEquals("Updated 1 hour ago", after(Duration.ofMinutes(60)))
        assertEquals("Updated 23 hours ago", after(Duration.ofHours(23)))
        assertEquals("Updated 1 day ago", after(Duration.ofHours(24)))
        assertEquals("Updated 3 days ago", after(Duration.ofDays(3)))
        // The clock was moved back since.
        assertEquals("Updated just now", after(Duration.ofHours(-5)))
    }
}
