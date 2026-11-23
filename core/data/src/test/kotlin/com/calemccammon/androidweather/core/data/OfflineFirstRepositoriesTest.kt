package com.calemccammon.androidweather.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.calemccammon.androidweather.core.data.repository.OfflineFirstForecastRepository
import com.calemccammon.androidweather.core.data.repository.RoomSavedPlacesRepository
import com.calemccammon.androidweather.core.database.WeatherDatabase
import com.calemccammon.androidweather.core.model.CachedForecast
import com.calemccammon.androidweather.core.model.Forecast
import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.WeatherCondition
import com.calemccammon.androidweather.core.model.WeatherException
import com.calemccammon.androidweather.core.model.WeatherFailure
import com.calemccammon.androidweather.core.network.WeatherNetworkDataSource
import com.calemccammon.androidweather.core.testing.MutableClock
import com.calemccammon.androidweather.core.testing.Samples
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** The repositories over a real database held in memory and a stand-in for the network. */
@RunWith(RobolectricTestRunner::class)
class OfflineFirstRepositoriesTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WeatherDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val clock = MutableClock()
    private val network = FakeNetwork()
    private val forecasts = OfflineFirstForecastRepository(database.forecastDao(), network, clock)
    private val saved = RoomSavedPlacesRepository(database.savedPlaceDao())

    @After
    fun close() = database.close()

    private class FakeNetwork : WeatherNetworkDataSource {
        var forecast: Forecast = Samples.forecast()
        var failure: WeatherFailure? = null
        val asked = mutableListOf<Pair<Double, Double>>()

        override suspend fun searchPlaces(query: String): List<Place> = emptyList()

        override suspend fun forecast(
            latitude: Double,
            longitude: Double,
        ): Forecast {
            asked += latitude to longitude
            failure?.let { throw WeatherException(it) }
            return forecast
        }
    }

    // Forecasts

    @Test
    fun `nothing is held, and nothing fetched, until a refresh`() =
        runTest {
            assertNull(forecasts.observe(Samples.asheville).first())
            assertEquals(emptyList(), network.asked)
        }

    @Test
    fun `a refresh is written to the database and reaches whoever is watching`() =
        runTest {
            forecasts.observe(Samples.asheville).test {
                assertNull(awaitItem())

                forecasts.refresh(Samples.asheville)

                assertEquals(CachedForecast(Samples.forecast(), fetchedAt = clock.now), awaitItem())
            }
            assertEquals(listOf(35.6 to -82.55), network.asked)
        }

    @Test
    fun `a failed refresh leaves what was held and passes the failure on`() =
        runTest {
            forecasts.refresh(Samples.asheville)
            val held = forecasts.observe(Samples.asheville).first()
            network.failure = WeatherFailure.NoConnection
            clock.advance(Duration.ofHours(5))

            val thrown = assertFailsWith<WeatherException> { forecasts.refresh(Samples.asheville) }

            assertEquals(WeatherFailure.NoConnection, thrown.failure)
            assertEquals(held, forecasts.observe(Samples.asheville).first())
        }

    @Test
    fun `a newer forecast replaces the old one and its time`() =
        runTest {
            forecasts.refresh(Samples.asheville)
            clock.advance(Duration.ofHours(1))
            network.forecast = Samples.forecast(temperature = 25.0, days = 1)

            forecasts.refresh(Samples.asheville)

            assertEquals(
                CachedForecast(Samples.forecast(temperature = 25.0, days = 1), clock.now),
                forecasts.observe(Samples.asheville).first(),
            )
        }

    @Test
    fun `a watcher of one place hears nothing when another place's forecast changes`() =
        runTest {
            forecasts.refresh(Samples.asheville)

            forecasts.observe(Samples.asheville).test {
                awaitItem()
                forecasts.refresh(Samples.lisbon)
                // The database tells its watchers a moment after a write, so
                // "nothing came" is shown by what comes next: after a change
                // to this place, the very next thing heard is that change.
                network.forecast = Samples.forecast(temperature = 30.0)
                forecasts.refresh(Samples.asheville)
                assertEquals(30.0, awaitItem()?.forecast?.current?.temperature)
            }
        }

    @Test
    fun `a forecast is filed under the spot, whatever the place is called`() =
        runTest {
            forecasts.refresh(Samples.asheville)

            // The same spot picked off the map, a hair away and a whole turn round.
            val picked = Place(latitude = 35.6004, longitude = -82.5496 + 360)

            assertEquals(Samples.forecast(), forecasts.observe(picked).first()?.forecast)
        }

    @Test
    fun `the service is asked with the longitude in range`() =
        runTest {
            forecasts.refresh(Place(latitude = 10.0, longitude = 190.0))

            assertEquals(listOf(10.0 to -170.0), network.asked)
        }

    @Test
    fun `pruning forgets old forecasts of unsaved places and keeps saved ones`() =
        runTest {
            saved.save(Samples.asheville)
            forecasts.refresh(Samples.asheville)
            forecasts.refresh(Samples.lisbon)
            clock.advance(Duration.ofDays(2))
            forecasts.refresh(Samples.point)

            forecasts.prune(before = clock.now - Duration.ofDays(1))

            assertEquals(Samples.forecast(), forecasts.observe(Samples.asheville).first()?.forecast)
            assertNull(forecasts.observe(Samples.lisbon).first())
            assertEquals(Samples.forecast(), forecasts.observe(Samples.point).first()?.forecast)
        }

    @Test
    fun `pruning keeps the places it is told to, however old and whether saved or not`() =
        runTest {
            forecasts.refresh(Samples.lisbon)
            forecasts.refresh(Samples.point)
            clock.advance(Duration.ofDays(5))

            // The same spot under another name is the same place.
            forecasts.prune(before = clock.now, keep = listOf(Samples.lisbon.copy(name = null)))

            assertEquals(Samples.forecast(), forecasts.observe(Samples.lisbon).first()?.forecast)
            assertNull(forecasts.observe(Samples.point).first())
        }

    @Test
    fun `a condition this version does not know reads as unknown`() =
        runTest {
            forecasts.refresh(Samples.asheville)
            database.openHelper.writableDatabase.execSQL("UPDATE forecasts SET condition = 'Sandstorm'")

            val held = forecasts.observe(Samples.asheville).first()

            assertEquals(WeatherCondition.Unknown, held?.forecast?.current?.condition)
        }

    // Saved places

    @Test
    fun `saved places are kept in order, once each, and can be removed`() =
        runTest {
            saved.save(Samples.lisbon)
            saved.save(Samples.asheville)
            saved.save(Samples.lisbon)

            assertEquals(listOf(Samples.lisbon, Samples.asheville), saved.places.first().map { it.place })
            assertEquals(listOf(1L, 2L), saved.places.first().map { it.id })

            saved.remove(Samples.lisbon)

            assertEquals(listOf(Samples.asheville), saved.places.first().map { it.place })
        }

    @Test
    fun `whether a place is saved goes by its spot on the map`() =
        runTest {
            saved.save(Samples.asheville)

            assertEquals(true, saved.isSaved(Place(35.6, -82.55)).first())
            assertEquals(false, saved.isSaved(Place(35.7, -82.55)).first())
        }

    @Test
    fun `spots are told apart to a hundredth of a degree`() {
        assertEquals("3560,-8255", Samples.asheville.key)
        assertEquals("3560,-8255", Place(35.6049, -82.5451).key)
        assertEquals("3561,-8255", Place(35.6051, -82.55).key)
        assertEquals("-3386,15121", Samples.point.key)
        assertEquals(Place(0.0, 190.0).key, Place(0.0, -170.0).key)
    }
}
