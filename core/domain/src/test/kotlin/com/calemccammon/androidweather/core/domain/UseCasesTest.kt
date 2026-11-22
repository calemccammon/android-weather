package com.calemccammon.androidweather.core.domain

import app.cash.turbine.test
import com.calemccammon.androidweather.core.domain.usecase.ObserveForecast
import com.calemccammon.androidweather.core.domain.usecase.ObserveSavedPlaceForecasts
import com.calemccammon.androidweather.core.domain.usecase.ObserveUnits
import com.calemccammon.androidweather.core.domain.usecase.RefreshForecast
import com.calemccammon.androidweather.core.domain.usecase.RefreshSavedPlaces
import com.calemccammon.androidweather.core.domain.usecase.SearchPlaces
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.Units
import com.calemccammon.androidweather.core.model.UserSettings
import com.calemccammon.androidweather.core.model.WeatherException
import com.calemccammon.androidweather.core.model.WeatherFailure
import com.calemccammon.androidweather.core.testing.FakeDeviceRepository
import com.calemccammon.androidweather.core.testing.FakeForecastRepository
import com.calemccammon.androidweather.core.testing.FakePlaceSearchRepository
import com.calemccammon.androidweather.core.testing.FakeSavedPlacesRepository
import com.calemccammon.androidweather.core.testing.FakeSettingsRepository
import com.calemccammon.androidweather.core.testing.MutableClock
import com.calemccammon.androidweather.core.testing.Samples
import kotlinx.coroutines.test.runTest
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UseCasesTest {
    private val clock = MutableClock()
    private val forecasts = FakeForecastRepository(clock)
    private val saved = FakeSavedPlacesRepository()
    private val offGlobe = Place(latitude = 91.0, longitude = 0.0)

    // Searching

    @Test
    fun `a search is trimmed, and one too short is not sent`() =
        runTest {
            val places = FakePlaceSearchRepository().apply { results = listOf(Samples.asheville) }
            val search = SearchPlaces(places)

            assertEquals(emptyList(), search(" a "))
            assertEquals(listOf(Samples.asheville), search("  ashe "))
            assertEquals(listOf("ashe"), places.queries)
        }

    // Units

    @Test
    fun `units are the user's choice, and the device's country's until they choose`() =
        runTest {
            val settings = FakeSettingsRepository()
            val units = ObserveUnits(settings, FakeDeviceRepository(country = "US"))

            units().test {
                assertEquals(Units.Imperial, awaitItem())
                settings.setUnits(Units.Metric)
                assertEquals(Units.Metric, awaitItem())
                // A change to some other setting says nothing new about units.
                settings.setSelectedPlace(Samples.lisbon)
                expectNoEvents()
            }
            assertEquals(UserSettings(Units.Metric, Samples.lisbon), settings.current)
        }

    // Watching a forecast

    @Test
    fun `the forecast held is shown in the units asked for, and again when it changes`() =
        runTest {
            ObserveForecast(forecasts)(Samples.asheville, Units.Imperial).test {
                assertNull(awaitItem())

                forecasts.hold(Samples.asheville, Samples.forecast(temperature = 10.0))
                val shown = awaitItem()
                assertEquals(50.0, shown?.forecast?.current?.temperature)
                assertEquals(clock.now, shown?.fetchedAt)

                forecasts.hold(Samples.asheville, Samples.forecast(temperature = 20.0))
                assertEquals(68.0, awaitItem()?.forecast?.current?.temperature)
            }
        }

    @Test
    fun `a point off the globe has no forecast`() =
        runTest {
            ObserveForecast(forecasts)(offGlobe, Units.Metric).test {
                assertNull(awaitItem())
                awaitComplete()
            }
        }

    // Refreshing a forecast

    private val refresh = RefreshForecast(forecasts, clock)

    @Test
    fun `a place with nothing held is fetched`() =
        runTest {
            refresh(Samples.asheville)

            assertEquals(listOf(Samples.asheville), forecasts.refreshed)
        }

    @Test
    fun `a forecast is not fetched again until it is half an hour old`() =
        runTest {
            forecasts.hold(Samples.asheville)

            clock.advance(RefreshForecast.FRESH_FOR - Duration.ofSeconds(1))
            refresh(Samples.asheville)
            assertEquals(emptyList(), forecasts.refreshed)

            clock.advance(Duration.ofSeconds(1))
            refresh(Samples.asheville)
            assertEquals(listOf(Samples.asheville), forecasts.refreshed)
        }

    @Test
    fun `a fresh forecast is fetched again when asked outright`() =
        runTest {
            forecasts.hold(Samples.asheville)

            refresh(Samples.asheville, force = true)

            assertEquals(listOf(Samples.asheville), forecasts.refreshed)
        }

    @Test
    fun `a forecast dated in the future is fetched again`() =
        runTest {
            forecasts.hold(Samples.asheville, fetchedAt = clock.now + Duration.ofHours(2))

            refresh(Samples.asheville)

            assertEquals(listOf(Samples.asheville), forecasts.refreshed)
        }

    @Test
    fun `a point off the globe fails without being asked about`() =
        runTest {
            val thrown = assertFailsWith<WeatherException> { refresh(offGlobe) }

            assertEquals(WeatherFailure.InvalidPlace, thrown.failure)
            assertEquals(emptyList(), forecasts.refreshed)
        }

    @Test
    fun `a failed fetch is passed on`() =
        runTest {
            forecasts.failure = WeatherFailure.Unavailable

            val thrown = assertFailsWith<WeatherException> { refresh(Samples.asheville) }

            assertEquals(WeatherFailure.Unavailable, thrown.failure)
        }

    // Saved places

    @Test
    fun `saved places are listed in order, each with the forecast held for it`() =
        runTest {
            ObserveSavedPlaceForecasts(saved, forecasts)(Units.Imperial).test {
                assertEquals(emptyList(), awaitItem())

                saved.save(Samples.asheville)
                assertEquals(listOf(Samples.asheville to null), awaitItem().map { it.saved.place to it.cached })

                forecasts.hold(Samples.asheville, Samples.forecast(temperature = 10.0))
                assertEquals(
                    50.0,
                    awaitItem()
                        .single()
                        .cached
                        ?.forecast
                        ?.current
                        ?.temperature,
                )

                saved.save(Samples.lisbon)
                assertEquals(listOf(Samples.asheville, Samples.lisbon), expectMostRecentItem().map { it.saved.place })

                saved.remove(Samples.asheville)
                assertEquals(listOf(Samples.lisbon), expectMostRecentItem().map { it.saved.place })
            }
        }

    private val refreshSaved = RefreshSavedPlaces(saved, forecasts, clock)

    @Test
    fun `the background refresh fetches every saved place and forgets old unsaved forecasts`() =
        runTest {
            saved.save(Samples.asheville)
            saved.save(Samples.lisbon)

            val outcome = refreshSaved()

            assertEquals(RefreshSavedPlaces.Outcome.Done, outcome)
            assertEquals(listOf(Samples.asheville, Samples.lisbon), forecasts.refreshed)
            assertEquals(listOf(clock.now - RefreshSavedPlaces.KEEP_UNSAVED_FOR), forecasts.pruned)
        }

    @Test
    fun `one place failing does not stop the rest, and a passing failure is worth another try`() =
        runTest {
            saved.save(Samples.asheville)
            saved.save(Samples.lisbon)
            forecasts.failure = WeatherFailure.NoConnection

            val outcome = refreshSaved()

            assertEquals(RefreshSavedPlaces.Outcome.RetryLater, outcome)
            assertEquals(listOf(Samples.asheville, Samples.lisbon), forecasts.refreshed)
            assertEquals(1, forecasts.pruned.size)
        }

    @Test
    fun `a failure that waiting will not mend is not tried again`() =
        runTest {
            saved.save(Samples.asheville)
            forecasts.failure = WeatherFailure.BadResponse

            assertEquals(RefreshSavedPlaces.Outcome.Done, refreshSaved())
        }

    @Test
    fun `with no saved places the refresh has nothing to fetch`() =
        runTest {
            assertEquals(RefreshSavedPlaces.Outcome.Done, refreshSaved())
            assertEquals(emptyList(), forecasts.refreshed)
        }
}
