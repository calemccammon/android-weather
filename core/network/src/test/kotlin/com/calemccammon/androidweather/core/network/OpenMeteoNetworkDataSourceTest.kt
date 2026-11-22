package com.calemccammon.androidweather.core.network

import com.calemccammon.androidweather.core.model.Place
import com.calemccammon.androidweather.core.model.WeatherCondition
import com.calemccammon.androidweather.core.model.WeatherException
import com.calemccammon.androidweather.core.model.WeatherFailure
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Before
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** The data source against a local server that answers as Open-Meteo would. */
class OpenMeteoNetworkDataSourceTest {
    private val server = MockWebServer()
    private lateinit var source: WeatherNetworkDataSource

    @Before
    fun start() {
        server.start()
        val url = server.url("/").toString()
        source =
            OpenMeteoNetworkDataSource(
                client = OpenMeteoNetworkDataSource.httpClient(timeout = Duration.ofMillis(500)),
                urls = OpenMeteoUrls(geocoding = url, forecast = url),
            )
    }

    @After
    fun stop() = server.close()

    private fun answer(
        body: String,
        code: Int = 200,
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .body(body)
            .build(),
    )

    private suspend fun failure(ask: suspend () -> Unit): WeatherException = assertFailsWith<WeatherException> { ask() }

    // Places

    @Test
    fun `a search asks for the name and reads the places found`() =
        runTest {
            answer(
                """
                {"results": [
                  {"id": 1, "name": "Asheville", "latitude": 35.6, "longitude": -82.55,
                   "admin1": "North Carolina", "country": "United States", "elevation": 650.0},
                  {"name": "Somewhere", "latitude": 1.0, "longitude": 2.0}
                ]}
                """,
            )

            val places = source.searchPlaces("ashe ville")

            assertEquals(
                listOf(
                    Place(35.6, -82.55, "Asheville", "North Carolina", "United States"),
                    Place(1.0, 2.0, "Somewhere"),
                ),
                places,
            )
            val asked = server.takeRequest().url
            assertEquals("/v1/search", asked.encodedPath)
            assertEquals("ashe ville", asked.queryParameter("name"))
            assertEquals("8", asked.queryParameter("count"))
        }

    @Test
    fun `a search that finds nothing has no results key at all`() =
        runTest {
            answer("""{"generationtime_ms": 0.5}""")

            assertEquals(emptyList(), source.searchPlaces("zzzz"))
        }

    // Forecasts

    private val forecastBody =
        """
        {
          "latitude": 35.6, "timezone": "America/New_York",
          "current": {"time": "2026-11-18T09:15", "temperature_2m": 2.4, "apparent_temperature": -0.6,
                      "weather_code": 3, "wind_speed_10m": 11.2, "is_day": 1},
          "daily": {
            "time": ["2026-11-18", "2026-11-19", "2026-11-20"],
            "weather_code": [3, 61, null],
            "temperature_2m_max": [8.1, 12.0, null],
            "temperature_2m_min": [-1.0, 4.5, 3.0],
            "precipitation_probability_max": [null, 46.6, 10]
          }
        }
        """

    @Test
    fun `a forecast asks for a point in metric units and reads conditions and days`() =
        runTest {
            answer(forecastBody)

            val forecast = source.forecast(35.6, -82.55)

            assertEquals(LocalDateTime.parse("2026-11-18T09:15"), forecast.current.observedAt)
            assertEquals(2.4, forecast.current.temperature)
            assertEquals(-0.6, forecast.current.feelsLike)
            assertEquals(11.2, forecast.current.windSpeed)
            assertEquals(WeatherCondition.Overcast, forecast.current.condition)
            assertEquals(true, forecast.current.isDay)

            // The third day has no high, so it is left out.
            assertEquals(listOf("2026-11-18", "2026-11-19").map(LocalDate::parse), forecast.days.map { it.date })
            assertEquals(listOf(-1.0 to 8.1, 4.5 to 12.0), forecast.days.map { it.low to it.high })
            assertEquals(listOf(null, 47), forecast.days.map { it.precipitationChance })
            assertEquals(WeatherCondition.Rain, forecast.days[1].condition)

            val asked = server.takeRequest().url
            assertEquals("/v1/forecast", asked.encodedPath)
            assertEquals("35.6", asked.queryParameter("latitude"))
            assertEquals("-82.55", asked.queryParameter("longitude"))
            assertEquals("7", asked.queryParameter("forecast_days"))
            assertEquals("auto", asked.queryParameter("timezone"))
            // The service's default units are the metric ones, so none are named.
            assertNull(asked.queryParameter("temperature_unit"))
            assertNull(asked.queryParameter("wind_speed_unit"))
        }

    @Test
    fun `a forecast with no chance of precipitation column is read without one`() =
        runTest {
            answer(forecastBody.replace(Regex(""""precipitation_probability_max": \[[^]]*]"""), """"x": 1"""))

            assertEquals(listOf(null, null), source.forecast(0.0, 0.0).days.map { it.precipitationChance })
        }

    // What can go wrong

    @Test
    fun `an answer that is not json is a bad response`() =
        runTest {
            answer("<html>hello</html>")

            assertEquals(WeatherFailure.BadResponse, failure { source.forecast(0.0, 0.0) }.failure)
        }

    @Test
    fun `an answer of the wrong shape is a bad response`() =
        runTest {
            answer("""{"current": {"time": "2026-11-18T09:15"}}""")

            assertEquals(WeatherFailure.BadResponse, failure { source.forecast(0.0, 0.0) }.failure)
        }

    @Test
    fun `a date that is not one is a bad response`() =
        runTest {
            answer(forecastBody.replace("2026-11-19", "tomorrow"))

            assertEquals(WeatherFailure.BadResponse, failure { source.forecast(0.0, 0.0) }.failure)
        }

    @Test
    fun `columns of different lengths are a bad response`() =
        runTest {
            answer(forecastBody.replace("[3, 61, null]", "[3, 61]"))

            assertEquals(WeatherFailure.BadResponse, failure { source.forecast(0.0, 0.0) }.failure)
        }

    @Test
    fun `a refusal carries the service's reason`() =
        runTest {
            answer("""{"error": true, "reason": "Latitude must be in range of -90 to 90"}""", code = 400)

            val thrown = failure { source.forecast(100.0, 0.0) }

            assertEquals(WeatherFailure.Rejected, thrown.failure)
            assertEquals("Latitude must be in range of -90 to 90", thrown.detail)
        }

    @Test
    fun `a refusal with no readable reason carries its status`() =
        runTest {
            answer("nope", code = 404)

            val thrown = failure { source.searchPlaces("ashe") }

            assertEquals(WeatherFailure.Rejected, thrown.failure)
            assertEquals("HTTP 404", thrown.detail)
        }

    @Test
    fun `the service's own trouble and too many requests are both unavailable`() =
        runTest {
            answer("<html>Bad gateway</html>", code = 502)
            answer("""{"error": true, "reason": "Too many requests"}""", code = 429)

            assertEquals(WeatherFailure.Unavailable, failure { source.forecast(0.0, 0.0) }.failure)
            assertEquals(WeatherFailure.Unavailable, failure { source.forecast(0.0, 0.0) }.failure)
        }

    @Test
    fun `no answer in time is a timeout`() =
        runTest {
            server.enqueue(MockResponse.Builder().onResponseStart(SocketEffect.Stall).build())

            assertEquals(WeatherFailure.Timeout, failure { source.forecast(0.0, 0.0) }.failure)
        }

    @Test
    fun `a server that cannot be reached is no connection`() =
        runTest {
            server.close()

            assertEquals(WeatherFailure.NoConnection, failure { source.searchPlaces("ashe") }.failure)
        }
}
