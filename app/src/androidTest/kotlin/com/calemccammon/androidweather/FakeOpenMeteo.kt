package com.calemccammon.androidweather

import com.calemccammon.androidweather.core.network.OpenMeteoUrls
import com.calemccammon.androidweather.core.network.di.OpenMeteoUrlsModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.rules.ExternalResource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A server on the device that answers as Open-Meteo would, for tests that run
 * the whole app. Everything from the HTTP client inwards is the shipped code.
 *
 * It knows two places, Asheville and Lisbon, and gives each a forecast a
 * test can change.
 */
class FakeOpenMeteo : ExternalResource() {
    private val server = MockWebServer()

    /** While true, every request fails as if the service were down. */
    @Volatile
    var down = false

    /** The WMO code of today's and tomorrow's weather at Lisbon. 95 is a thunderstorm. */
    @Volatile
    var lisbonCode = 0

    /** The path and query of every request, in order. */
    val requests = CopyOnWriteArrayList<String>()

    override fun before() {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = answer(request)
            }
        server.start()
        // Read here, off the main thread, for the app to pick up when it is built.
        url = server.url("/").toString()
    }

    override fun after() = server.close()

    private fun answer(request: RecordedRequest): MockResponse {
        val asked = request.url
        requests += "${asked.encodedPath}?${asked.encodedQuery}"

        val body =
            when (asked.encodedPath) {
                "/v1/search" -> if (asked.queryParameter("name").orEmpty().startsWith("zz")) "{}" else PLACES
                "/v1/forecast" -> {
                    val lisbon = asked.queryParameter("latitude")?.startsWith("38") == true
                    if (lisbon) forecast(temperature = 18, code = lisbonCode) else forecast(temperature = 7, code = 3)
                }
                else -> null
            }
        return when {
            down -> MockResponse.Builder().code(SERVICE_UNAVAILABLE).build()
            body == null -> MockResponse.Builder().code(NOT_FOUND).build()
            else -> MockResponse.Builder().body(body).build()
        }
    }

    /** A week from today, so "today" in the app is the first row. */
    private fun forecast(
        temperature: Int,
        code: Int,
    ): String {
        val today = LocalDate.now()
        val days = (0L until DAYS).map { today.plusDays(it) }
        return """
            {
              "current": {"time": "${LocalDateTime.now().withSecond(0).withNano(0)}",
                          "temperature_2m": $temperature.0, "apparent_temperature": ${temperature - 2}.0,
                          "weather_code": $code, "wind_speed_10m": 10.0, "is_day": 1},
              "daily": {
                "time": [${days.joinToString { "\"$it\"" }}],
                "weather_code": [${days.joinToString { "$code" }}],
                "temperature_2m_max": [${days.indices.joinToString { "${temperature + 4 + it}.0" }}],
                "temperature_2m_min": [${days.indices.joinToString { "${temperature - 4 + it}.0" }}],
                "precipitation_probability_max": [${days.indices.joinToString { "${it * 10}" }}]
              }
            }
            """.trimIndent()
    }

    companion object {
        /** The running server's address; the app is pointed at it by [FakeOpenMeteoModule]. */
        @Volatile
        var url: String = "http://localhost:1/"
            private set

        private const val DAYS = 7
        private const val SERVICE_UNAVAILABLE = 503
        private const val NOT_FOUND = 404

        private const val PLACES =
            """
            {"results": [
              {"name": "Asheville", "latitude": 35.6, "longitude": -82.55,
               "admin1": "North Carolina", "country": "United States"},
              {"name": "Lisbon", "latitude": 38.72, "longitude": -9.13, "admin1": "Lisbon", "country": "Portugal"}
            ]}
            """
    }
}

/** Points the app at [FakeOpenMeteo] in place of the real service. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [OpenMeteoUrlsModule::class])
object FakeOpenMeteoModule {
    @Provides
    fun openMeteoUrls(): OpenMeteoUrls = OpenMeteoUrls(geocoding = FakeOpenMeteo.url, forecast = FakeOpenMeteo.url)
}
