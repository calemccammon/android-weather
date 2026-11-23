package com.calemccammon.androidweather.core.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.WeatherCondition
import com.calemccammon.androidweather.core.model.WeatherFailure
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class WordingTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val UiText.words get() = asString(resources)

    @Test
    fun `text is looked up with its gaps filled, other text included`() {
        assertEquals("Asheville", UiText.Verbatim("Asheville").words)
        assertEquals("12°C", uiText(R.string.core_ui_temperature_celsius, 12).words)
        assertEquals(
            "1.00°N, 2.00°W",
            uiText(
                R.string.core_ui_place_coordinates,
                uiText(R.string.core_ui_place_north, "1.00"),
                uiText(R.string.core_ui_place_west, "2.00"),
            ).words,
        )
    }

    @Test
    fun `every condition and every failure has its own wording`() {
        val conditions = WeatherCondition.entries.map { it.label.words }
        val failures = WeatherFailure.entries.map { it.message.words }

        assertEquals(conditions.size, conditions.toSet().size)
        assertEquals(failures.size, failures.toSet().size)
        assertEquals("Freezing drizzle", WeatherCondition.FreezingDrizzle.label.words)
        assertEquals("The weather service took too long to answer.", WeatherFailure.Timeout.message.words)
    }

    @Test
    fun `clear and partly cloudy skies are drawn differently by night`() {
        for (condition in listOf(WeatherCondition.Clear, WeatherCondition.MostlyClear, WeatherCondition.PartlyCloudy)) {
            assertEquals(false, condition.icon(isDay = true) == condition.icon(isDay = false), condition.name)
        }
        assertEquals(WeatherCondition.Rain.icon(isDay = true), WeatherCondition.Rain.icon(isDay = false))
    }

    @Test
    fun `a named place shows its name, and its region and country beneath`() {
        val place = PlaceUiModel.from(Place(35.6, -82.55, "Asheville", "North Carolina", "United States"))

        assertEquals("Asheville", place.title.words)
        assertEquals("North Carolina, United States", place.subtitle)
        assertEquals(35.6 to -82.55, place.latitude to place.longitude)
        assertEquals("United States", PlaceUiModel.from(Place(1.0, 2.0, "X", country = "United States")).subtitle)
    }

    @Test
    fun `a place with no name is called by its coordinates`() {
        fun title(
            latitude: Double,
            longitude: Double,
        ) = PlaceUiModel.from(Place(latitude, longitude)).title.words

        assertEquals("35.60°N, 82.55°W", title(35.6, -82.55))
        assertEquals("33.86°S, 151.21°E", title(-33.8599, 151.2051))
        assertEquals("0.05°N, 0.01°E", title(0.05, 0.005))
        // A hair south of the equator rounds to it, and is not "0.00°S".
        assertEquals("0.00°N, 0.00°E", title(-0.001, -0.001))
        assertEquals("", PlaceUiModel.from(Place(1.0, 2.0)).subtitle)
    }
}
