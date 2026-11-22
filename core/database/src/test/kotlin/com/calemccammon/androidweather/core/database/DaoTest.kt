package com.calemccammon.androidweather.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.calemccammon.androidweather.core.database.model.DailyForecastEntity
import com.calemccammon.androidweather.core.database.model.ForecastEntity
import com.calemccammon.androidweather.core.database.model.SavedPlaceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The queries, run against a real SQLite database held in memory. */
@RunWith(RobolectricTestRunner::class)
class DaoTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), WeatherDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val places = database.savedPlaceDao()
    private val forecasts = database.forecastDao()

    @After
    fun close() = database.close()

    private fun place(
        key: String,
        name: String? = key,
    ) = SavedPlaceEntity(placeKey = key, latitude = 1.0, longitude = 2.0, name = name, region = null, country = null)

    private fun forecast(
        key: String,
        fetchedAt: Instant = Instant.parse("2026-11-18T12:00:00Z"),
        temperature: Double = 10.0,
    ) = ForecastEntity(
        placeKey = key,
        fetchedAt = fetchedAt,
        observedAt = LocalDateTime.parse("2026-11-18T09:00"),
        temperature = temperature,
        feelsLike = 8.0,
        windSpeed = 20.0,
        condition = "Rain",
        isDay = true,
    )

    private fun day(
        key: String,
        date: String,
        chance: Int? = 40,
    ) = DailyForecastEntity(key, LocalDate.parse(date), low = 1.0, high = 9.0, condition = "Rain", chance)

    // Saved places

    @Test
    fun `saved places are listed oldest first, and a spot saved twice is kept once`() =
        runTest {
            places.insert(place("b"))
            places.insert(place("a"))
            places.insert(place("b", name = "again"))

            assertEquals(listOf("b", "a"), places.observeAll().first().map { it.name })
        }

    @Test
    fun `whether a spot is saved follows saving and removing`() =
        runTest {
            places.observeIsSaved("a").test {
                assertEquals(false, awaitItem())
                places.insert(place("a"))
                assertEquals(true, awaitItem())
                places.delete("a")
                assertEquals(false, awaitItem())
            }
        }

    // Forecasts

    @Test
    fun `a forecast is read back whole, with what was written`() =
        runTest {
            assertNull(forecasts.observe("a").first())

            forecasts.replace(forecast("a"), listOf(day("a", "2026-11-18"), day("a", "2026-11-19", chance = null)))

            val held = forecasts.observe("a").first()
            assertEquals(forecast("a"), held?.forecast)
            assertEquals(listOf(day("a", "2026-11-18"), day("a", "2026-11-19", chance = null)), held?.days)
        }

    @Test
    fun `a newer forecast takes the place of the old one, days and all`() =
        runTest {
            forecasts.replace(forecast("a"), listOf(day("a", "2026-11-18"), day("a", "2026-11-19")))
            forecasts.replace(forecast("b"), listOf(day("b", "2026-11-18")))

            forecasts.replace(forecast("a", temperature = 20.0), listOf(day("a", "2026-11-19"), day("a", "2026-11-20")))

            val held = forecasts.observe("a").first()
            assertEquals(20.0, held?.forecast?.temperature)
            assertEquals(listOf("2026-11-19", "2026-11-20"), held?.days?.map { it.date.toString() })
            // Another spot's forecast is left alone.
            assertEquals(
                1,
                forecasts
                    .observe("b")
                    .first()
                    ?.days
                    ?.size,
            )
        }

    @Test
    fun `a watcher sees a forecast arrive`() =
        runTest {
            forecasts.observe("a").test {
                assertNull(awaitItem())
                forecasts.replace(forecast("a"), listOf(day("a", "2026-11-18")))
                assertEquals(10.0, awaitItem()?.forecast?.temperature)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `pruning drops old forecasts of unsaved spots, with their days, and nothing else`() =
        runTest {
            val cutOff = Instant.parse("2026-11-18T12:00:00Z")
            places.insert(place("saved"))
            forecasts.replace(
                forecast("saved", fetchedAt = cutOff.minusSeconds(60)),
                listOf(day("saved", "2026-11-18")),
            )
            forecasts.replace(forecast("old", fetchedAt = cutOff.minusSeconds(1)), listOf(day("old", "2026-11-18")))
            forecasts.replace(forecast("new", fetchedAt = cutOff), listOf(day("new", "2026-11-18")))

            val dropped = forecasts.deleteUnsavedFetchedBefore(cutOff)

            assertEquals(1, dropped)
            assertNull(forecasts.observe("old").first())
            assertEquals(
                1,
                forecasts
                    .observe("saved")
                    .first()
                    ?.days
                    ?.size,
            )
            assertEquals(
                1,
                forecasts
                    .observe("new")
                    .first()
                    ?.days
                    ?.size,
            )
            val daysLeft =
                database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM daily_forecasts").use {
                    it.moveToFirst()
                    it.getInt(0)
                }
            assertEquals(2, daysLeft)
        }
}
