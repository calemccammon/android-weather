package com.calemccammon.androidweather.feature.home

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.calemccammon.androidweather.core.domain.usecase.ObserveForecast
import com.calemccammon.androidweather.core.domain.usecase.ObserveUnits
import com.calemccammon.androidweather.core.domain.usecase.RefreshForecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.model.UserSettings
import com.calemccammon.androidweather.core.model.WeatherFailure
import com.calemccammon.androidweather.core.testing.FakeDeviceRepository
import com.calemccammon.androidweather.core.testing.FakeForecastRepository
import com.calemccammon.androidweather.core.testing.FakeLocationRepository
import com.calemccammon.androidweather.core.testing.FakeSavedPlacesRepository
import com.calemccammon.androidweather.core.testing.FakeSettingsRepository
import com.calemccammon.androidweather.core.testing.MainDispatcherRule
import com.calemccammon.androidweather.core.testing.MutableClock
import com.calemccammon.androidweather.core.testing.Samples
import com.calemccammon.androidweather.core.ui.UiText
import com.calemccammon.androidweather.core.ui.asString
import com.calemccammon.androidweather.feature.home.model.ForecastState
import com.calemccammon.androidweather.feature.home.model.HomeEvent
import com.calemccammon.androidweather.feature.home.model.HomeState
import com.calemccammon.androidweather.feature.home.model.UnitsUiModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val resources: Resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    private val UiText.words get() = asString(resources)

    private val clock = MutableClock()
    private val forecasts = FakeForecastRepository(clock)
    private val settings = FakeSettingsRepository()
    private val saved = FakeSavedPlacesRepository()
    private val location = FakeLocationRepository()
    private val device = FakeDeviceRepository(country = "GB")

    private val viewModel by lazy {
        HomeViewModel(
            observeUnits = ObserveUnits(settings, device),
            observeForecast = ObserveForecast(forecasts),
            refreshForecast = RefreshForecast(forecasts, clock),
            settings = settings,
            savedPlaces = saved,
            location = location,
        )
    }

    /** The state on screen, once everything under way has settled. */
    private fun TestScope.shown(): HomeState {
        advanceUntilIdle()
        return viewModel.state.value
    }

    /** Runs [test] with the screen watching, as it does while it is open. */
    private fun watching(test: suspend TestScope.() -> Unit) =
        runTest {
            val screen = launch { viewModel.state.collect() }
            test()
            screen.cancel()
        }

    @Test
    fun `with no place selected there is nothing to forecast`() =
        watching {
            assertEquals(HomeState(units = UnitsUiModel.Celsius), shown())
            assertEquals(emptyList(), forecasts.refreshed)
        }

    @Test
    fun `selecting a place remembers it, fetches its forecast and shows it`() =
        watching {
            viewModel.select(Samples.asheville)

            val state = shown()
            assertEquals("Asheville", state.place?.title?.words)
            assertEquals(false, state.saved)
            val loaded = assertIs<ForecastState.Loaded>(state.forecast)
            assertEquals("10°C", loaded.forecast.current.temperature.words)
            assertEquals(clock.now, loaded.fetchedAt)
            assertEquals(false, loaded.refreshing)
            assertNull(loaded.problem)
            assertEquals(Samples.asheville, settings.current.selectedPlace)
            assertEquals(listOf(Samples.asheville), forecasts.refreshed)
        }

    @Test
    fun `it opens on the place selected last time, from what the device holds`() =
        watching {
            settings.setSelectedPlace(Samples.lisbon)
            forecasts.hold(Samples.lisbon, Samples.forecast(temperature = 18.0))

            val state = shown()

            assertEquals("Lisbon", state.place?.title?.words)
            assertEquals(
                "18°C",
                assertIs<ForecastState.Loaded>(state.forecast)
                    .forecast.current.temperature.words,
            )
            // What is held is fresh, so the service is not asked.
            assertEquals(emptyList(), forecasts.refreshed)
        }

    @Test
    fun `while the first forecast for a place is on its way it says so`() =
        watching {
            forecasts.gate = CompletableDeferred()

            viewModel.select(Samples.asheville)

            assertEquals(ForecastState.Loading, shown().forecast)

            forecasts.gate?.complete(Unit)
            assertIs<ForecastState.Loaded>(shown().forecast)
        }

    @Test
    fun `with nothing held a failed fetch is worded, and another try fetches again`() =
        watching {
            forecasts.failure = WeatherFailure.NoConnection

            viewModel.select(Samples.asheville)

            val failed = assertIs<ForecastState.Failed>(shown().forecast)
            assertEquals("Could not reach the weather service. Check your connection.", failed.message.words)

            forecasts.failure = null
            viewModel.retryForecast()

            assertIs<ForecastState.Loaded>(shown().forecast)
            assertEquals(listOf(Samples.asheville, Samples.asheville), forecasts.refreshed)
        }

    @Test
    fun `an old forecast is shown while a newer one is fetched`() =
        watching {
            forecasts.hold(Samples.asheville, Samples.forecast(temperature = 5.0))
            val fetchedAt = clock.now
            clock.advance(Duration.ofHours(2))
            forecasts.onNetwork[Samples.asheville] = Samples.forecast(temperature = 9.0)
            forecasts.gate = CompletableDeferred()

            viewModel.select(Samples.asheville)

            val old = assertIs<ForecastState.Loaded>(shown().forecast)
            assertEquals("5°C", old.forecast.current.temperature.words)
            assertEquals(fetchedAt, old.fetchedAt)
            assertEquals(true, old.refreshing)

            forecasts.gate?.complete(Unit)

            val new = assertIs<ForecastState.Loaded>(shown().forecast)
            assertEquals("9°C", new.forecast.current.temperature.words)
            assertEquals(clock.now, new.fetchedAt)
            assertEquals(false, new.refreshing)
        }

    @Test
    fun `with no connection the forecast held stays on screen with word of the problem`() =
        watching {
            forecasts.hold(Samples.asheville, Samples.forecast(temperature = 5.0))
            clock.advance(Duration.ofHours(2))
            forecasts.failure = WeatherFailure.NoConnection

            viewModel.select(Samples.asheville)

            val loaded = assertIs<ForecastState.Loaded>(shown().forecast)
            assertEquals("5°C", loaded.forecast.current.temperature.words)
            assertEquals(false, loaded.refreshing)
            assertEquals("Could not reach the weather service. Check your connection.", loaded.problem?.words)

            forecasts.failure = null
            viewModel.retryForecast()

            assertNull(assertIs<ForecastState.Loaded>(shown().forecast).problem)
        }

    @Test
    fun `a forecast that arrives in the background reaches the open screen`() =
        watching {
            viewModel.select(Samples.asheville)
            shown()

            // As the background refresh would: written to what the device holds.
            forecasts.hold(Samples.asheville, Samples.forecast(temperature = 21.0))

            assertEquals(
                "21°C",
                assertIs<ForecastState.Loaded>(shown().forecast)
                    .forecast.current.temperature.words,
            )
        }

    @Test
    fun `word of a failed fetch goes once a newer forecast arrives another way`() =
        watching {
            forecasts.hold(Samples.asheville, Samples.forecast(temperature = 5.0))
            clock.advance(Duration.ofHours(2))
            forecasts.failure = WeatherFailure.NoConnection
            viewModel.select(Samples.asheville)
            assertEquals(true, assertIs<ForecastState.Loaded>(shown().forecast).problem != null)

            // As the background refresh would, a while later.
            clock.advance(Duration.ofHours(1))
            forecasts.hold(Samples.asheville, Samples.forecast(temperature = 9.0))

            val loaded = assertIs<ForecastState.Loaded>(shown().forecast)
            assertEquals("9°C", loaded.forecast.current.temperature.words)
            assertNull(loaded.problem)
        }

    @Test
    fun `coming back to the screen fetches again only once the forecast has grown old`() =
        watching {
            viewModel.select(Samples.asheville)
            shown()

            viewModel.refreshIfStale()
            shown()
            assertEquals(listOf(Samples.asheville), forecasts.refreshed)

            clock.advance(Duration.ofHours(2))
            viewModel.refreshIfStale()
            shown()
            assertEquals(listOf(Samples.asheville, Samples.asheville), forecasts.refreshed)
        }

    @Test
    fun `coming back to the screen does not restart a fetch already under way`() =
        watching {
            forecasts.gate = CompletableDeferred()
            viewModel.select(Samples.asheville)
            shown()

            viewModel.refreshIfStale()
            forecasts.gate?.complete(Unit)

            assertIs<ForecastState.Loaded>(shown().forecast)
            assertEquals(listOf(Samples.asheville), forecasts.refreshed)
        }

    @Test
    fun `selecting another place gives up the fetch for the first`() =
        watching {
            val gate = CompletableDeferred<Unit>()
            forecasts.gate = gate
            viewModel.select(Samples.asheville)
            shown()

            forecasts.gate = null
            viewModel.select(Samples.lisbon)

            assertEquals("Lisbon", shown().place?.title?.words)
            gate.complete(Unit)
            shown()
            // The first place's fetch was dropped, so nothing was kept for it.
            assertNull(forecasts.heldFor(Samples.asheville))
            assertIs<ForecastState.Loaded>(shown().forecast)
        }

    @Test
    fun `one place's failed fetch is not shown against the next place`() =
        watching {
            forecasts.failure = WeatherFailure.Unavailable
            viewModel.select(Samples.asheville)
            assertIs<ForecastState.Failed>(shown().forecast)

            forecasts.hold(Samples.lisbon)
            viewModel.select(Samples.lisbon)

            assertNull(assertIs<ForecastState.Loaded>(shown().forecast).problem)
        }

    @Test
    fun `a point picked off the map is selected with its longitude in range`() =
        watching {
            viewModel.selectPoint(10.0, 190.0)

            assertEquals("10.00°N, 170.00°W", shown().place?.title?.words)
            assertEquals(Place(10.0, -170.0), settings.current.selectedPlace)
        }

    @Test
    fun `a point off the globe is told as such without being asked about`() =
        watching {
            viewModel.selectPoint(95.0, 0.0)

            val failed = assertIs<ForecastState.Failed>(shown().forecast)
            assertEquals("That point is not on the map.", failed.message.words)
            assertEquals(emptyList(), forecasts.refreshed)
        }

    @Test
    fun `clearing forgets the place`() =
        watching {
            viewModel.select(Samples.asheville)
            shown()

            viewModel.clear()

            assertEquals(HomeState(units = UnitsUiModel.Celsius), shown())
            assertNull(settings.current.selectedPlace)
        }

    @Test
    fun `units start from the device's country and changing them fetches nothing`() =
        watching {
            device.country = "US"
            viewModel.select(Samples.asheville)
            assertEquals(UnitsUiModel.Fahrenheit, shown().units)
            assertEquals(
                "50°F",
                assertIs<ForecastState.Loaded>(shown().forecast)
                    .forecast.current.temperature.words,
            )

            viewModel.setUnits(UnitsUiModel.Celsius)

            val state = shown()
            assertEquals(UnitsUiModel.Celsius, state.units)
            assertEquals(
                "10°C",
                assertIs<ForecastState.Loaded>(state.forecast)
                    .forecast.current.temperature.words,
            )
            assertEquals(UserSettings(Units.Metric, Samples.asheville), settings.current)
            assertEquals(listOf(Samples.asheville), forecasts.refreshed)
        }

    @Test
    fun `the save button keeps the place and pressed again stops keeping it`() =
        watching {
            viewModel.select(Samples.asheville)
            shown()

            viewModel.toggleSaved()
            assertEquals(true, shown().saved)
            assertEquals(listOf(Samples.asheville), saved.current)

            viewModel.toggleSaved()
            assertEquals(false, shown().saved)
            assertEquals(emptyList(), saved.current)
        }

    @Test
    fun `the save button does nothing with no place selected`() =
        watching {
            viewModel.toggleSaved()

            shown()
            assertEquals(emptyList(), saved.current)
        }

    @Test
    fun `my location selects where the device is, and says so while it finds out`() =
        watching {
            location.gate = CompletableDeferred()

            viewModel.selectCurrentLocation()
            assertEquals(true, shown().locating)

            location.gate?.complete(Unit)

            val state = shown()
            assertEquals(false, state.locating)
            assertEquals("33.86°S, 151.21°E", state.place?.title?.words)
            assertIs<ForecastState.Loaded>(state.forecast)
        }

    @Test
    fun `when the device cannot say where it is, that is told once and the place is kept`() =
        watching {
            viewModel.select(Samples.asheville)
            shown()
            location.place = null

            viewModel.events.test {
                viewModel.selectCurrentLocation()

                assertEquals(HomeEvent.LocationUnavailable, awaitItem())
            }
            assertEquals(false, shown().locating)
            assertEquals(Samples.asheville, settings.current.selectedPlace)
        }
}
