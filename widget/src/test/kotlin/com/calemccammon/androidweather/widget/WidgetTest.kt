package com.calemccammon.androidweather.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.calemccammon.androidweather.core.domain.usecase.ObserveForecast
import com.calemccammon.androidweather.core.domain.usecase.ObserveUnits
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.testing.FakeDeviceRepository
import com.calemccammon.androidweather.core.testing.FakeForecastRepository
import com.calemccammon.androidweather.core.testing.FakeSettingsRepository
import com.calemccammon.androidweather.core.testing.MutableClock
import com.calemccammon.androidweather.core.testing.Samples
import com.calemccammon.androidweather.core.ui.UiText
import com.calemccammon.androidweather.core.ui.asString
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class WidgetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val UiText.words get() = asString(context.resources)

    private val clock = MutableClock()
    private val forecasts = FakeForecastRepository(clock)
    private val settings = FakeSettingsRepository()
    private val observe =
        ObserveWidgetState(
            settings,
            ObserveUnits(settings, FakeDeviceRepository(country = "GB")),
            ObserveForecast(forecasts),
            clock,
        )

    // What the widget shows

    @Test
    fun `the widget follows the selected place, its forecast and the units`() =
        runTest {
            observe().test {
                assertEquals(WidgetState.NoPlace, awaitItem())

                settings.setSelectedPlace(Samples.asheville)
                assertEquals("Asheville", assertIs<WidgetState.Waiting>(awaitItem()).place.words)

                forecasts.hold(Samples.asheville, Samples.forecast(temperature = 10.0))
                val weather = assertIs<WidgetState.Weather>(awaitItem())
                assertEquals("Asheville", weather.place.words)
                assertEquals("10°C", weather.temperature.words)
                assertEquals("Overcast", weather.condition.words)
                assertEquals("5° / 15°", weather.range?.words)
                assertEquals("Asheville: Overcast, 10 degrees Celsius", weather.spoken.words)

                settings.setUnits(Units.Imperial)
                assertEquals("50°F", assertIs<WidgetState.Weather>(awaitItem()).temperature.words)

                settings.setSelectedPlace(null)
                assertEquals(WidgetState.NoPlace, awaitItem())
            }
        }

    @Test
    fun `a change that shows the same thing is not a change`() =
        runTest {
            settings.setSelectedPlace(Samples.asheville)
            forecasts.hold(Samples.asheville)

            observe().test {
                awaitItem()
                // The same forecast fetched again, and a setting the widget does not show.
                forecasts.hold(Samples.asheville)
                settings.setAlertsEnabled(true)
                expectNoEvents()
            }
        }

    @Test
    fun `a forecast kept since yesterday shows today's range, not yesterday's`() =
        runTest {
            settings.setSelectedPlace(Samples.asheville)
            forecasts.hold(Samples.asheville, Samples.forecast())
            clock.advance(Duration.ofDays(1))

            observe().test {
                assertEquals("6° / 16°", assertIs<WidgetState.Weather>(awaitItem()).range?.words)
            }
        }

    @Test
    fun `a forecast with no day for today has no range`() =
        runTest {
            settings.setSelectedPlace(Samples.asheville)
            forecasts.hold(Samples.asheville, Samples.forecast(days = 0))

            observe().test {
                assertNull(assertIs<WidgetState.Weather>(awaitItem()).range)
            }
        }

    // How it is drawn

    private val weather =
        WidgetState.Weather(
            place = UiText.Verbatim("Asheville"),
            temperature = UiText.Verbatim("10°C"),
            condition = UiText.Verbatim("Overcast"),
            range = UiText.Verbatim("5° / 15°"),
            spoken = UiText.Verbatim("Asheville: Overcast, 10 degrees Celsius"),
        )

    private fun draw(
        state: WidgetState,
        size: DpSize,
        check: androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest.() -> Unit,
    ) = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(size)
        provideComposable { WidgetContent(state, onClick = null) }
        check()
    }

    @Test
    fun `with room it shows the place, temperature, condition and today's range`() =
        draw(weather, WeatherWidget.roomySize) {
            onNode(hasText("Asheville")).assertExists()
            onNode(hasText("10°C")).assertExists()
            onNode(hasText("Overcast")).assertExists()
            onNode(hasText("5° / 15°")).assertExists()
            onNode(hasContentDescription("Asheville: Overcast, 10 degrees Celsius")).assertExists()
        }

    @Test
    fun `at its smallest it shows the temperature and the place only`() =
        draw(weather, WeatherWidget.compactSize) {
            onNode(hasText("10°C")).assertExists()
            onNode(hasText("Asheville")).assertExists()
            onNode(hasText("Overcast")).assertDoesNotExist()
        }

    @Test
    fun `with no place it says to open the app`() =
        draw(WidgetState.NoPlace, WeatherWidget.roomySize) {
            onNode(hasText("Open Weather to choose a place")).assertExists()
        }

    @Test
    fun `with a place and no forecast yet it says it is waiting`() =
        draw(WidgetState.Waiting(UiText.Verbatim("Asheville")), WeatherWidget.roomySize) {
            onNode(hasText("Asheville")).assertExists()
            onNode(hasText("Waiting for the forecast")).assertExists()
        }
}
