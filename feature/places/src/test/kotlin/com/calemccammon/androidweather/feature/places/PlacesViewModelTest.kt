package com.calemccammon.androidweather.feature.places

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.calemccammon.androidweather.core.domain.usecase.ObserveSavedPlaceForecasts
import com.calemccammon.androidweather.core.domain.usecase.ObserveUnits
import com.calemccammon.androidweather.core.domain.usecase.RefreshSavedPlaces
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.model.WeatherFailure
import com.calemccammon.androidweather.core.testing.FakeDeviceRepository
import com.calemccammon.androidweather.core.testing.FakeForecastRepository
import com.calemccammon.androidweather.core.testing.FakeSavedPlacesRepository
import com.calemccammon.androidweather.core.testing.FakeSettingsRepository
import com.calemccammon.androidweather.core.testing.MainDispatcherRule
import com.calemccammon.androidweather.core.testing.MutableClock
import com.calemccammon.androidweather.core.testing.Samples
import com.calemccammon.androidweather.core.ui.UiText
import com.calemccammon.androidweather.core.ui.asString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class PlacesViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources
    private val UiText.words get() = asString(resources)

    private val clock = MutableClock()
    private val forecasts = FakeForecastRepository(clock)
    private val settings = FakeSettingsRepository()
    private val saved = FakeSavedPlacesRepository(listOf(Samples.asheville, Samples.lisbon))

    private val viewModel by lazy {
        PlacesViewModel(
            observeUnits = ObserveUnits(settings, FakeDeviceRepository(country = "GB")),
            observeSavedPlaceForecasts = ObserveSavedPlaceForecasts(saved, forecasts),
            refreshSavedPlaces = RefreshSavedPlaces(saved, forecasts, settings, clock),
            savedPlaces = saved,
            settings = settings,
        )
    }

    private fun TestScope.shown(): PlacesState {
        advanceUntilIdle()
        return viewModel.state.value
    }

    private fun watching(test: suspend TestScope.() -> Unit) =
        runTest {
            val screen = launch { viewModel.state.collect() }
            test()
            screen.cancel()
        }

    @Test
    fun `before the saved places are read it does not claim there are none`() {
        assertEquals(PlacesState(ready = false), viewModel.state.value)
    }

    @Test
    fun `each saved place is a row, with the weather held for it if any`() =
        watching {
            forecasts.hold(Samples.lisbon, Samples.forecast(temperature = 17.6))

            val state = shown()

            assertEquals(true, state.ready)
            assertEquals(listOf("Asheville", "Lisbon"), state.places.map { it.place.title.words })
            assertNull(state.places[0].weather)
            val weather = state.places[1].weather
            assertEquals("18°C", weather?.temperature?.words)
            assertEquals("Overcast", weather?.condition?.words)
            assertEquals("18 degrees Celsius", weather?.spokenTemperature?.words)
        }

    @Test
    fun `rows follow the units chosen`() =
        watching {
            forecasts.hold(Samples.lisbon, Samples.forecast(temperature = 10.0))
            settings.setUnits(Units.Imperial)

            assertEquals(
                "50°F",
                shown()
                    .places[1]
                    .weather
                    ?.temperature
                    ?.words,
            )
        }

    @Test
    fun `with nothing saved the list is ready and empty`() =
        watching {
            saved.remove(Samples.asheville)
            saved.remove(Samples.lisbon)

            assertEquals(PlacesState(ready = true), shown())
        }

    @Test
    fun `opening a row makes its place the one the home screen shows`() =
        watching {
            val lisbon = shown().places[1].id

            viewModel.open(lisbon)
            advanceUntilIdle()

            assertEquals(Samples.lisbon, settings.current.selectedPlace)
        }

    @Test
    fun `removing a row is told once and can be undone once`() =
        watching {
            val asheville = shown().places[0].id

            viewModel.events.test {
                viewModel.remove(asheville)

                val removed = assertIs<PlacesEvent.Removed>(awaitItem())
                assertEquals("Asheville", removed.title.words)
                assertEquals(listOf("Lisbon"), shown().places.map { it.place.title.words })

                viewModel.undoRemove(removed.removal)
                assertEquals(listOf("Lisbon", "Asheville"), shown().places.map { it.place.title.words })

                // The same removal cannot be undone twice.
                viewModel.undoRemove(removed.removal)
                assertEquals(2, shown().places.size)
            }
        }

    @Test
    fun `with two places removed, undoing the first puts the first back`() =
        watching {
            val (asheville, lisbon) = shown().places.map { it.id }

            viewModel.events.test {
                viewModel.remove(asheville)
                val first = assertIs<PlacesEvent.Removed>(awaitItem())
                viewModel.remove(lisbon)
                val second = assertIs<PlacesEvent.Removed>(awaitItem())

                viewModel.undoRemove(first.removal)
                assertEquals(listOf("Asheville"), shown().places.map { it.place.title.words })

                viewModel.undoRemove(second.removal)
                assertEquals(listOf("Asheville", "Lisbon"), shown().places.map { it.place.title.words })
            }
        }

    @Test
    fun `a row that is not there is neither opened nor removed`() =
        watching {
            shown()

            viewModel.open(99)
            viewModel.remove(99)

            assertEquals(2, shown().places.size)
            assertNull(settings.current.selectedPlace)
        }

    @Test
    fun `pulling to refresh fetches every place and shows that it is doing so`() =
        watching {
            shown()
            forecasts.gate = CompletableDeferred()

            viewModel.refresh()
            assertEquals(true, shown().refreshing)

            // A second pull while the first is under way starts nothing more.
            viewModel.refresh()
            forecasts.gate?.complete(Unit)

            val state = shown()
            assertEquals(false, state.refreshing)
            assertEquals(listOf("10°C", "10°C"), state.places.map { it.weather?.temperature?.words })
            assertEquals(listOf(Samples.asheville, Samples.lisbon), forecasts.refreshed)
        }

    @Test
    fun `a refresh that failed is told once, with why`() =
        watching {
            shown()
            forecasts.failure = WeatherFailure.NoConnection

            viewModel.events.test {
                viewModel.refresh()

                val failed = assertIs<PlacesEvent.RefreshFailed>(awaitItem())
                assertEquals("Could not reach the weather service. Check your connection.", failed.reason.words)
            }
            assertEquals(false, shown().refreshing)
        }

    @Test
    fun `a failure that waiting will not mend is told too, and not blamed on the connection`() =
        watching {
            shown()
            forecasts.failure = WeatherFailure.BadResponse

            viewModel.events.test {
                viewModel.refresh()

                val failed = assertIs<PlacesEvent.RefreshFailed>(awaitItem())
                assertEquals("The weather service sent an answer this app could not read.", failed.reason.words)
            }
        }

    @Test
    fun `alerts are off until turned on, and the choice is remembered`() =
        watching {
            assertEquals(false, shown().alertsEnabled)

            viewModel.setAlertsEnabled(true)

            assertEquals(true, shown().alertsEnabled)
            assertEquals(true, settings.current.alertsEnabled)
        }
}
